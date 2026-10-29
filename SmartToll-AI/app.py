import os
import re
import base64
from pathlib import Path
from flask import Flask, request, jsonify
import cv2
import numpy as np

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


def extract_vehicle_number(text):
    if not text:
        return None
    cleaned = re.sub(r"[^A-Z0-9]", "", text.upper())
    # Remove common plate noise prefix like IND / 1ND / IO
    cleaned = re.sub(r"^(IND|1ND|IO|I)", "", cleaned)

    # 1. Standard Indian vehicle number: AP55A1234, MH12DE1433, DL01A1234, KA041234
    m = re.search(r"[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}", cleaned)
    if m:
        return m.group(0)

    # 2. Bharat Series: 22BH1234AA
    m = re.search(r"[0-9]{2}BH[0-9]{4}[A-Z]{1,2}", cleaned)
    if m:
        return m.group(0)

    # 3. Generic alphanumeric candidate (5 to 11 chars with both letters and digits)
    if 5 <= len(cleaned) <= 12:
        has_letters = bool(re.search(r"[A-Z]", cleaned))
        has_digits = bool(re.search(r"[0-9]", cleaned))
        if has_letters and has_digits:
            return cleaned

    return None


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
    image = None
    data = request.get_json(silent=True) or {}

    # 1. Base64 decoded image (ideal for cloud microservices)
    if "imageBase64" in data and data["imageBase64"]:
        try:
            b64_str = data["imageBase64"]
            if "," in b64_str:
                b64_str = b64_str.split(",", 1)[1]
            img_bytes = base64.b64decode(b64_str)
            nparr = np.frombuffer(img_bytes, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        except Exception as e:
            print("Failed to decode imageBase64:", e)

    # 2. Direct multipart/form-data file upload
    if image is None and "image" in request.files:
        try:
            file = request.files["image"]
            img_bytes = file.read()
            nparr = np.frombuffer(img_bytes, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        except Exception as e:
            print("Failed to read uploaded image:", e)

    # 3. Local disk imagePath (for local testing)
    if image is None:
        image_path = data.get("imagePath")
        if image_path and os.path.exists(image_path):
            image = cv2.imread(image_path)

    if image is None:
        return jsonify({
            "vehicleDetected": False,
            "vehicleNumber": None,
            "message": "Could not read image (no valid image data provided)"
        }), 400

    # Get models (lazy loaded)
    model = get_model()
    reader = get_reader()

    detected_plates = []

    # Step 1: Detect license plates using YOLO
    try:
        results = model.predict(source=image, conf=0.20, verbose=False)
        for result in results:
            for box in result.boxes:
                x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                # Add padding around bounding box to capture edges
                h, w = image.shape[:2]
                px1 = max(0, x1 - 5)
                py1 = max(0, y1 - 5)
                px2 = min(w, x2 + 5)
                py2 = min(h, y2 + 5)
                plate = image[py1:py2, px1:px2]

                if plate.size == 0:
                    continue

                detections = reader.readtext(plate)
                for detection in detections:
                    raw_text = detection[1]
                    plate_num = extract_vehicle_number(raw_text)
                    print(f"YOLO Box OCR candidate: '{raw_text}' -> '{plate_num}'")
                    if plate_num:
                        detected_plates.append(plate_num)
    except Exception as e:
        print("YOLO prediction error:", e)

    if detected_plates:
        # Return first valid detected plate
        return jsonify({
            "vehicleNumber": detected_plates[0],
            "vehicleDetected": True,
            "message": "Number plate detected successfully"
        })

    # Step 2: Fallback to full-image OCR if YOLO didn't find a bounding box
    print("YOLO found no valid plate. Falling back to full-image OCR scan...")
    try:
        full_detections = reader.readtext(image)
        for detection in full_detections:
            raw_text = detection[1]
            plate_num = extract_vehicle_number(raw_text)
            print(f"Full-Image OCR candidate: '{raw_text}' -> '{plate_num}'")
            if plate_num:
                return jsonify({
                    "vehicleNumber": plate_num,
                    "vehicleDetected": True,
                    "message": "Number plate detected via image scan"
                })
    except Exception as e:
        print("Full-image OCR error:", e)

    return jsonify({
        "vehicleNumber": None,
        "vehicleDetected": False,
        "message": "Number plate not detected"
    })


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
