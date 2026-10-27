
from pathlib import Path
from ultralytics import YOLO

BASE_DIR = Path(__file__).resolve().parent

MODEL_PATH = BASE_DIR / "runs" / "detect" / "smarttoll_test" / "weights" / "best.pt"
IMAGE_PATH = BASE_DIR / "images" / "car.jpg"

model = YOLO(str(MODEL_PATH))

results = model.predict(
    source=str(IMAGE_PATH),
    save=True,
    conf=0.25
)

print("Detection completed!")
print("Check the runs/detect/predict folder for the result image.") 