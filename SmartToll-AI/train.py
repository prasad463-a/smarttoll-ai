from pathlib import Path
from ultralytics import YOLO

BASE_DIR = Path(__file__).resolve().parent
DATA_YAML = BASE_DIR / "dataset" / "data.yaml"

model = YOLO("yolo11n.pt")

model.train(
    data=str(DATA_YAML),
    epochs=3,
    imgsz=320,
    batch=4,
    workers=0,
    device="cpu",
    fraction=0.05,
    name="smarttoll_test"
)