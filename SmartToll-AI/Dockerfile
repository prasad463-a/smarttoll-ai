FROM python:3.11-slim

WORKDIR /app

# Install system dependencies for OpenCV, PyTorch OpenMP, and EasyOCR
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Install lightweight CPU-only PyTorch
RUN pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Pre-cache EasyOCR model in image
RUN python -c "import easyocr; easyocr.Reader(['en'])"

ENV PORT=10000
EXPOSE 10000

CMD ["python", "app.py"]
