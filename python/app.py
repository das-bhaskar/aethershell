# app.py

from flask import Flask, request, jsonify
from renderer import draw_frame
from analyzer import analyze_with_gemini
import os

app = Flask(__name__)

# Output folder for PNGs (mapped to your Java static folder)
OUTPUT_DIR = "../hub/src/main/resources/static/output"
os.makedirs(OUTPUT_DIR, exist_ok=True)


@app.route('/process_vram', methods=['POST'])
def process():
    data = request.json
    if not data:
        return jsonify({"error": "No data received"}), 400
        
    session_id = data.get('session_id')
    frames = data.get('frames')  # List of coordinate strings from Java

    if not session_id or not frames:
        return jsonify({"error": "Missing session_id or frames"}), 400

    try:
        # 1. Render all frames → returns list of PNG filenames
        filenames = draw_frame(session_id, frames, OUTPUT_DIR, scale=8)

        if not filenames:
            return jsonify({"error": "No frames rendered"}), 500

        # 2. Pick ONE image for AI analysis (last frame)
        image_path = os.path.join(OUTPUT_DIR, filenames[-1])

        # 3. Send to Gemini and get single-letter interpretation
        ai_description = analyze_with_gemini(image_path)

        # 4. Return result to Java
        return jsonify({
            "status": "success",
            "ai_description": ai_description,
            "images": filenames
        }), 200

    except Exception as e:
        print(f"Server Error: {e}")
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    # Use port 5001 to match your worker setup
    app.run(port=5001)
