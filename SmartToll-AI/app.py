import os
import re
import gc
import base64
import pathlib
from pathlib import Path
from flask import Flask, request, jsonify
import cv2
import numpy as np

# Safe patch for pathlib.Path.exists to avoid Windows permission issues on .git/commondir
_orig_exists = pathlib.Path.exists
def _safe_exists(self):
    try:
        return _orig_exists(self)
    except Exception:
        return False
pathlib.Path.exists = _safe_exists

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

# Bundled lightweight English recognizer model directory (~15MB only)
MODELS_DIR = BASE_DIR / "models"
USER_NET_DIR = BASE_DIR / "models" / "user_net"
MODELS_DIR.mkdir(parents=True, exist_ok=True)
USER_NET_DIR.mkdir(parents=True, exist_ok=True)

# Lazy-loaded singletons to avoid high memory startup spikes on cloud containers
_model = None
_reader = None

def get_model():
    global _model
    if _model is None:
        import torch
        from ultralytics import YOLO
        print("Loading YOLO model from:", MODEL_PATH)
        _model = YOLO(str(MODEL_PATH))
    return _model

def get_reader():
    global _reader
    if _reader is None:
        import torch
        import easyocr
        
        has_local_weights = (MODELS_DIR / "english_g2.pth").exists()
        print(f"Loading EasyOCR Recognizer (detector=False, local_cache={has_local_weights})...")
        
        # detector=False disables the 80MB CRAFT text detector, saving >200MB RAM
        # and staying safely below Render's 512MB RAM ceiling!
        _reader = easyocr.Reader(
            ["en"],
            gpu=False,
            detector=False,
            recognizer=True,
            model_storage_directory=str(MODELS_DIR),
            user_network_directory=str(USER_NET_DIR),
            download_enabled=not has_local_weights
        )
    return _reader

# Background model warmup to ensure instant sub-second inference on first request
def _prewarm_models():
    try:
        print("Pre-warming YOLO and EasyOCR models and JIT kernels in background...")
        m = get_model()
        r = get_reader()
        dummy = np.zeros((64, 64, 3), dtype=np.uint8)
        import torch
        with torch.no_grad():
            m.predict(source=dummy, imgsz=416, verbose=False)
            r.recognize(dummy, horizontal_list=[[0, 64, 0, 64]], free_list=[])
        print("Pre-warming complete! JIT kernels compiled and ready for instant ANPR inference.")
    except Exception as e:
        print("Model pre-warm warning:", e)

import threading
threading.Thread(target=_prewarm_models, daemon=True).start()


def extract_vehicle_number(text):
    if not text:
        return None
    cleaned = re.sub(r"[^A-Z0-9]", "", text.upper())
    # Remove common plate noise prefix like IND / 1ND / IO / CARPLATE / D
    cleaned = re.sub(r"^(CARPLATE|PLATE|IND|1ND|IO|I|D|WO)", "", cleaned)

    # 1. Standard Indian vehicle number: AP55A1234, MH12DE1433, DL01A1234, KA041234
    m = re.search(r"[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}", cleaned)
    if m:
        return m.group(0)

    # 2. Bharat Series: 22BH1234AA
    m = re.search(r"[0-9]{2}BH[0-9]{4}[A-Z]{1,2}", cleaned)
    if m:
        return m.group(0)

    # 3. Generic alphanumeric candidate (5 to 12 chars with both letters and digits)
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
        "ocrMode": "RecognizerOnly (Low-Memory)",
        "version": "1.1.0"
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

    import torch

    # Downscale very large mobile photos (>1280px) to conserve RAM
    h, w = image.shape[:2]
    max_dim = max(h, w)
    if max_dim > 1280:
        scale = 1280.0 / max_dim
        image = cv2.resize(image, (int(w * scale), int(h * scale)))

    # Get models (lazy loaded)
    model = get_model()
    reader = get_reader()

    detected_plates = []

    # Step 1: Detect license plates using YOLO with no_grad
    try:
        with torch.no_grad():
            results = model.predict(source=image, conf=0.18, imgsz=416, verbose=False)

        h_img, w_img = image.shape[:2]
        
        # Collect and rank detected boxes by confidence score descending
        all_boxes = []
        for result in results:
            for box in result.boxes:
                all_boxes.append(box)
        all_boxes.sort(key=lambda b: float(b.conf[0]) if b.conf is not None else 0.0, reverse=True)

        if all_boxes:
            box = all_boxes[0]  # Prioritize the single highest-confidence YOLO bounding box
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
            # Add slight padding around bounding box to capture plate edges
            px1 = max(0, x1 - 8)
            py1 = max(0, y1 - 8)
            px2 = min(w_img, x2 + 8)
            py2 = min(h_img, y2 + 8)
            plate = image[py1:py2, px1:px2]

            if plate.size > 0:
                hp, wp = plate.shape[:2]
                # Batch full plate and bottom half (for 2-line plates) in ONE single call!
                slices = [
                    [0, wp, 0, hp],
                    [0, wp, int(hp * 0.35), hp]
                ]

                with torch.no_grad():
                    rec_results = reader.recognize(plate, horizontal_list=slices, free_list=[])
                for item in rec_results:
                    raw_text = item[1]
                    plate_num = extract_vehicle_number(raw_text)
                    if plate_num and plate_num not in detected_plates:
                        print(f"Detected candidate: '{raw_text}' -> '{plate_num}'")
                        detected_plates.append(plate_num)

        # Fallback to 2nd box only if 1st box yielded no candidates
        if not detected_plates and len(all_boxes) > 1:
            box = all_boxes[1]
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
            plate = image[max(0, y1 - 8):min(h_img, y2 + 8), max(0, x1 - 8):min(w_img, x2 + 8)]
            if plate.size > 0:
                hp, wp = plate.shape[:2]
                with torch.no_grad():
                    rec_results = reader.recognize(plate, horizontal_list=[[0, wp, 0, hp]], free_list=[])
                for item in rec_results:
                    plate_num = extract_vehicle_number(item[1])
                    if plate_num and plate_num not in detected_plates:
                        detected_plates.append(plate_num)

    except Exception as e:
        print("YOLO/OCR prediction error:", e)

    # Free temporary torch tensors
    gc.collect()

    if detected_plates:
        # Prioritize standard Indian plates matching ^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$
        std_plates = [p for p in detected_plates if re.match(r"^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$", p)]
        final_plate = std_plates[0] if std_plates else detected_plates[0]

        print(f"Successfully identified vehicle plate: {final_plate}")
        return jsonify({
            "vehicleNumber": final_plate,
            "vehicleDetected": True,
            "message": "Number plate detected successfully"
        })

    # Step 2: Fallback scan for close-up plate images (center/bottom slices)
    print("YOLO found no valid plate. Running close-up plate fallback scan...")
    try:
        h_img, w_img = image.shape[:2]
        # Crop center region to avoid recognizing entire background
        cy1, cy2 = int(h_img * 0.2), int(h_img * 0.8)
        cx1, cx2 = int(w_img * 0.1), int(w_img * 0.9)
        crop = image[cy1:cy2, cx1:cx2]
        hc, wc = crop.shape[:2]

        fallback_slices = [
            [[0, wc, 0, hc]],
            [[0, wc, int(hc * 0.35), hc]]
        ]
        for s in fallback_slices:
            with torch.no_grad():
                rec_results = reader.recognize(crop, horizontal_list=s, free_list=[])
            for item in rec_results:
                raw_text = item[1]
                plate_num = extract_vehicle_number(raw_text)
                if plate_num:
                    print(f"Fallback detected: '{raw_text}' -> '{plate_num}'")
                    return jsonify({
                        "vehicleNumber": plate_num,
                        "vehicleDetected": True,
                        "message": "Number plate detected via image scan"
                    })
    except Exception as e:
        print("Fallback scan error:", e)

    return jsonify({
        "vehicleNumber": None,
        "vehicleDetected": False,
        "message": "Number plate not detected"
    })


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
