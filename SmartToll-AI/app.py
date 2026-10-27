# from flask import Flask, request, jsonify
# import easyocr
# import re

# app = Flask(__name__)

# # Create EasyOCR reader
# reader = easyocr.Reader(['en'])


# @app.route("/anpr/process", methods=["POST"])
# def process_image():

#     data = request.get_json()

#     image_path = data.get("imagePath")

#     print("Received image:", image_path)

#     # Read text from image
#     result = reader.readtext(image_path)

#     print("OCR Result:", result)

#     # Find vehicle number
#     vehicle_number = None

#     for detection in result:

#         text = detection[1].upper()

#         # Remove spaces and special characters
#         cleaned_text = re.sub(r'[^A-Z0-9]', '', text)

#         print("Checking:", cleaned_text)

#         # Indian vehicle number pattern
#         if re.match(
#             r'^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$',
#             cleaned_text
#         ):
#             vehicle_number = cleaned_text
#             break

#     # Vehicle number found
#     if vehicle_number:

#         return jsonify({
#             "vehicleNumber": vehicle_number,
#             "vehicleDetected": True,
#             "message": "Vehicle number detected successfully"
#         })

#     # Vehicle number not found
#     return jsonify({
#         "vehicleNumber": None,
#         "vehicleDetected": False,
#         "message": "Vehicle number not detected"
#     })


# if __name__ == "__main__":
#     app.run(port=5000)


from pathlib import Path
from flask import Flask, request, jsonify
from ultralytics import YOLO
import easyocr
import cv2
import re

app = Flask(__name__)

BASE_DIR = Path(__file__).resolve().parent

# Load your trained YOLO model
MODEL_PATH = (
    BASE_DIR / "runs" / "detect" / "smarttoll_test"
    / "weights" / "best.pt"
)

model = YOLO(str(MODEL_PATH))

# Load EasyOCR
reader = easyocr.Reader(["en"])


@app.route("/anpr/process", methods=["POST"])
def process_image():

    data = request.get_json()
    image_path = data.get("imagePath")

    if not image_path:
        return jsonify({
            "vehicleDetected": False,
            "vehicleNumber": None,
            "message": "Image path is missing"
        }), 400

    image = cv2.imread(image_path)

    if image is None:
        return jsonify({
            "vehicleDetected": False,
            "vehicleNumber": None,
            "message": "Could not read image"
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
            x1, y1, x2, y2 = map(
                int, box.xyxy[0].tolist()
            )

            # Crop the license plate
            plate = image[y1:y2, x1:x2]

            if plate.size == 0:
                continue

            # Read text from the cropped plate
            detections = reader.readtext(plate)

            for detection in detections:

                text = detection[1].upper()

                cleaned_text = re.sub(
                    r"[^A-Z0-9]", "", text
                )

                print("OCR:", cleaned_text)

                # Indian vehicle number format
                if re.match(
                    r"^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$",
                    cleaned_text
                ):
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
    app.run(port=5000)