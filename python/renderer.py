import math
from PIL import Image, ImageDraw
import os

def draw_frame(session_id, frames, output_dir, scale=8):
    generated_files = []
    orig_w, orig_h = 256, 192
    canvas_size = (orig_w * scale, orig_h * scale)

    for i, coords_string in enumerate(frames):
        img = Image.new("RGBA", canvas_size, (255, 255, 255, 0))
        draw = ImageDraw.Draw(img)
        points = []
        
        # Track boundaries to crop the "floating" empty space
        min_x, min_y = canvas_size[0], canvas_size[1]
        max_x, max_y = 0, 0

        for pair in coords_string.split(';'):
            pair = pair.strip()
            if ',' in pair:
                try:
                    x, y = map(int, pair.split(','))
                    if x <= 1 and y <= 1: continue
                    px, py = x * scale, y * scale
                    points.append((px, py))
                    # Update bounds
                    min_x, min_y = min(min_x, px), min(min_y, py)
                    max_x, max_y = max(max_x, px), max(max_y, py)
                except (ValueError, TypeError):
                    continue

        if len(points) > 1:
            for j in range(len(points) - 1):
                p1, p2 = points[j], points[j+1]
                dist = math.sqrt((p2[0] - p1[0])**2 + (p2[1] - p1[1])**2)
                if dist < (30 * scale):
                    draw.line([p1, p2], fill="black", width=scale, joint="curve")

        # CROP FIX: Remove empty space so feet touch the ground
        if points:
            padding = scale * 2
            left = max(0, min_x - padding)
            top = max(0, min_y - padding)
            right = min(canvas_size[0], max_x + padding)
            bottom = min(canvas_size[1], max_y + padding)
            img = img.crop((left, top, right, bottom))

        filename = f"{session_id}_frame_{i}.png"
        full_path = os.path.join(output_dir, filename)
        img.save(full_path, "PNG")
        generated_files.append(filename)

    return generated_files