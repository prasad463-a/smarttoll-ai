import os
import re
from pathlib import Path
from flask import Flask, request, jsonify
import cv2

app = Flask(__name__)
try:
    from flask_cors import CORS
    CORS(app)
except ImportError:
    pass

BASE_DIR = Path(__file__).resolve().parent

# Trained YOLO model path
MODEL_PATH = (
    BASE_DIR / "runs" / "detect" / "smarttoll_test"
    / "weights" / "best.pt"
)

# Lazy-loaded singletons to prevent high memory startup spikes on cloud containers
_model = None
_reader = None

def get_model():
    global _model
    if _model is None:
        import torch
        torch.set_num_threads(1)
        from ultralytics import YOLO
        print("Loading YOLO model from:", MODEL_PATH)
        _model = YOLO(str(MODEL_PATH))
    return _model

def get_reader():
    global _reader
    if _reader is None:
        import torch
        torch.set_num_threads(1)
        import easyocr
        print("Loading EasyOCR Reader...")
        _reader = easyocr.Reader(["en"], gpu=False)
    return _reader


@app.route("/", methods=["GET"])
def health_check():
    return jsonify({
        "status": "UP",
        "service": "SmartToll AI ANPR Service",
        "yoloModel": str(MODEL_PATH.name),
        "version": "1.0.0"
    })


@app.route("/anpr/process", methods=["POST"])
def process_image():
    data = request.get_json(silent=True) or {}
    image_path = data.get("imagePath")

    # Support direct file upload via multipart/form-data
    if not image_path and "image" in request.files:
        uploaded_file = request.files["image"]
        temp_dest = BASE_DIR / "temp_upload.jpg"
        uploaded_file.save(str(temp_dest))
        image_path = str(temp_dest)

    if not image_path:
        return jsonify({
            "vehicleDetected": False,
            "vehicleNumber": None,
            "message": "Image path or image file is missing"
        }), 400

    image = cv2.imread(image_path)

    if image is None:
        return jsonify({
            "vehicleDetected": False,
            "vehicleNumber": None,
            "message": "Could not read image file"
        }), 400

    # Get models (lazy loaded)
    model = get_model()
    reader = get_reader()

    # Detect license plates using YOLO
    results = model.predict(
        source=image,
        conf=0.25,
        verbose=False
    )

    for result in results:
        for box in result.boxes:
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
            plate = image[y1:y2, x1:x2]

            if plate.size == 0:
                continue

            # Read text from the cropped plate
            detections = reader.readtext(plate)

            for detection in detections:
                text = detection[1].upper()
                cleaned_text = re.sub(r"[^A-Z0-9]", "", text)
                print("OCR candidate:", cleaned_text)

                # Indian vehicle number format
                if re.match(r"^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$", cleaned_text):
                    return jsonify({
                        "vehicleNumber": cleaned_text,
                        "vehicleDetected": True,
                        "message": "Number plate detected successfully"
                    })

    return jsonify({
        "vehicleNumber": None,
        "vehicleDetected": False,
        "message": "Number plate not detected"
    })


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
