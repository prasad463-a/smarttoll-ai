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


INDIAN_STATES = (
    "AP", "TS", "TG", "MH", "DL", "KA", "TN", "KL", "UP", "MP", "GJ", "RJ", "HR",
    "PB", "WB", "OD", "JH", "CH", "BR", "AS", "UK", "HP", "GA", "JK", "LA", "PY",
    "TR", "MN", "ML", "NL", "MZ", "SK", "AR", "AN", "DD", "DN", "LD", "BH"
)

def clean_and_normalize_plate(raw_text):
    if not raw_text:
        return (None, 0)
    
    text = raw_text.upper().strip()
    text = re.sub(r"[^A-Z0-9]", "", text)
    if not text:
        return (None, 0)

    # 1. Standard Indian pattern WITH explicit series letters: (STATE)(1-2 DIGITS)(1-3 LETTERS)(4 DIGITS)
    # e.g. AP 55 A 9791, DL 01 AB 1234
    for state in INDIAN_STATES:
        pattern = r"(" + state + r")([0-9OISZB]{1,2})([A-Z]{1,3})([0-9OISZB]{4})"
        m = re.search(pattern, text)
        if m:
            s, rto, series, num = m.groups()
            def to_digits(d):
                return d.replace("O", "0").replace("I", "1").replace("S", "5").replace("Z", "2").replace("B", "8")
            rto = to_digits(rto)
            num = to_digits(num)
            return (f"{s}{rto}{series}{num}", 110)

    # 2. Standard Indian pattern where series letter was misread as a digit (e.g. 'A' misread as '4'):
    for state in INDIAN_STATES:
        pattern = r"(" + state + r")([0-9OISZB]{2})([A-Z0-9]{1,2})([0-9OISZB]{4})"
        m = re.search(pattern, text)
        if m:
            s, rto, series, num = m.groups()
            def to_digits(d):
                return d.replace("O", "0").replace("I", "1").replace("S", "5").replace("Z", "2").replace("B", "8")
            def to_letters(l):
                return l.replace("4", "A").replace("0", "O").replace("8", "B").replace("1", "I").replace("5", "S").replace("2", "Z")
            rto = to_digits(rto)
            series = to_letters(series)
            num = to_digits(num)
            if series.isalpha():
                return (f"{s}{rto}{series}{num}", 100)

    # 3. Bharat Series format: e.g. 22BH1234AA
    m = re.search(r"([0-9]{2}BH[0-9]{4}[A-Z]{1,2})", text)
    if m:
        return (m.group(1), 95)

    # 4. Old-style Indian plate without series letter (STATE + 1-2 digits + 4 digits at end):
    for state in INDIAN_STATES:
        pattern = r"(" + state + r")([0-9]{1,2})([0-9]{4})$"
        m = re.search(pattern, text)
        if m:
            s, rto, num = m.groups()
            return (f"{s}{rto}{num}", 80)

    # 5. Generic standard 2-letter prefix format with letters and digits:
    m = re.search(r"([A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4})", text)
    if m:
        return (m.group(1), 70)

    # 6. Fallback alphanumeric candidate (must have at least 2 letters and 4 digits):
    cleaned = re.sub(r"^(IND|1ND|IC|ID|IO|WO|CARPLATE|PLATE)", "", text)
    if 7 <= len(cleaned) <= 10:
        letters = re.findall(r"[A-Z]", cleaned)
        digits = re.findall(r"[0-9]", cleaned)
        if len(letters) >= 2 and len(digits) >= 4:
            return (cleaned, 35)

    return (None, 0)


def extract_vehicle_number(text):
    plate, _ = clean_and_normalize_plate(text)
    return plate


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

    # Downscale very large photos (>800px) for maximum CPU inference speed
    h, w = image.shape[:2]
    max_dim = max(h, w)
    if max_dim > 800:
        scale = 800.0 / max_dim
        image = cv2.resize(image, (int(w * scale), int(h * scale)))

    # Get models (lazy loaded)
    model = get_model()
    reader = get_reader()

    candidates = []

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

        # Evaluate up to the top 2 detected boxes with confidence > 0.18
        for box in all_boxes[:2]:
            box_conf = float(box.conf[0]) if box.conf is not None else 0.5
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
            px1 = max(0, x1 - 6)
            py1 = max(0, y1 - 6)
            px2 = min(w_img, x2 + 6)
            py2 = min(h_img, y2 + 6)
            plate = image[py1:py2, px1:px2]

            hp, wp = plate.shape[:2]
            if hp < 8 or wp < 15:
                continue

            # Upscale plate crop so character strokes are clear and not distorted by downsampling
            scale = max(1.0, 100.0 / hp)
            target_w = int(wp * scale)
            target_h = int(hp * scale)
            plate_scaled = cv2.resize(plate, (target_w, target_h), interpolation=cv2.INTER_CUBIC)

            # Slices: 1) Full plate, 2) Bottom 65% for 2-line plates, 3) Right 85% to skip left HSRP badge
            slices = [
                [0, target_w, 0, target_h],
                [0, target_w, int(target_h * 0.35), target_h],
                [int(target_w * 0.15), target_w, 0, target_h]
            ]

            with torch.no_grad():
                rec_results = reader.recognize(plate_scaled, horizontal_list=slices, free_list=[])

            for item in rec_results:
                raw_text = item[1]
                ocr_conf = float(item[2])
                plate_str, priority = clean_and_normalize_plate(raw_text)
                if plate_str:
                    score = priority + (ocr_conf * 10.0) + (box_conf * 5.0)
                    print(f"Candidate: '{plate_str}' | Score: {round(score, 2)} | Raw: '{raw_text}' | Conf: {round(ocr_conf, 3)}")
                    candidates.append((plate_str, score))

            # If we found a top-tier Indian plate (priority 110), stop further box scans to save CPU
            if any(c[1] >= 110 for c in candidates):
                break

    except Exception as e:
        print("YOLO/OCR prediction error:", e)

    # Free temporary torch tensors
    gc.collect()

    if candidates:
        candidates.sort(key=lambda c: c[1], reverse=True)
        final_plate = candidates[0][0]
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
        cy1, cy2 = int(h_img * 0.15), int(h_img * 0.85)
        cx1, cx2 = int(w_img * 0.08), int(w_img * 0.92)
        crop = image[cy1:cy2, cx1:cx2]
        hc, wc = crop.shape[:2]

        fallback_slices = [
            [0, wc, 0, hc],
            [0, wc, int(hc * 0.35), hc],
            [int(wc * 0.15), wc, 0, hc]
        ]
        with torch.no_grad():
            rec_results = reader.recognize(crop, horizontal_list=fallback_slices, free_list=[])
        for item in rec_results:
            raw_text = item[1]
            ocr_conf = float(item[2])
            plate_str, priority = clean_and_normalize_plate(raw_text)
            if plate_str:
                score = priority + (ocr_conf * 10.0)
                candidates.append((plate_str, score))

        if candidates:
            candidates.sort(key=lambda c: c[1], reverse=True)
            final_plate = candidates[0][0]
            print(f"Fallback detected plate: {final_plate}")
            return jsonify({
                "vehicleNumber": final_plate,
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
