import os
from pathlib import Path
from flask import Flask, request, jsonify
from flask_cors import CORS
from ultralytics import YOLO
import easyocr
import cv2
import re

app = Flask(__name__)
CORS(app)

BASE_DIR = Path(__file__).resolve().parent

# Load trained YOLO model
MODEL_PATH = (
    BASE_DIR / "runs" / "detect" / "smarttoll_test"
    / "weights" / "best.pt"
)

model = YOLO(str(MODEL_PATH))

# Load EasyOCR
reader = easyocr.Reader(["en"])


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

    # Also support direct file upload via multipart/form-data
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

    # Detect license plates using YOLO
    results = model.predict(
        source=image,
        conf=0.25,
        verbose=False
    )

    for result in results:
        for box in result.boxes:
            # Get bounding box coordinates
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())

            # Crop the license plate
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
