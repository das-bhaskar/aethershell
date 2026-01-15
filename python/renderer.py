import math
from PIL import Image, ImageDraw
import os

def draw_frame(session_id, frames, output_dir, scale=8):
    """
    Processes a list of coordinate strings and saves PNGs.
    Returns a list of the filenames created.
    """
    generated_files = []
    
    # NDS Original Resolution
    orig_w, orig_h = 256, 192
    canvas_size = (orig_w * scale, orig_h * scale)

    for i, coords_string in enumerate(frames):
        # Create Transparent Canvas
        img = Image.new("RGBA", canvas_size, (255, 255, 255, 0))
        draw = ImageDraw.Draw(img)

        points = []
        # Split string by semicolon to get "x,y" pairs
        for pair in coords_string.split(';'):
            pair = pair.strip()
            if ',' in pair:
                try:
                    x, y = map(int, pair.split(','))
                    # Filter hardware junk
                    if x <= 1 and y <= 1: 
                        continue
                    points.append((x * scale, y * scale))
                except (ValueError, TypeError):
                    continue

        # Draw lines only if points are close together (Jump Detection)
        if len(points) > 1:
            for j in range(len(points) - 1):
                p1 = points[j]
                p2 = points[j+1]

                # Distance calculation
                dist = math.sqrt((p2[0] - p1[0])**2 + (p2[1] - p1[1])**2)

                # If distance is small, it's a real stroke. 
                # If large, the pen was lifted.
                if dist < (30 * scale):
                    draw.line([p1, p2], fill="black", width=scale, joint="curve")

        # Save and track the file
        filename = f"{session_id}_frame_{i}.png"
        full_path = os.path.join(output_dir, filename)
        img.save(full_path, "PNG")
        generated_files.append(filename)

    return generated_files