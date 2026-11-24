import os
import cv2
import numpy as np
from PIL import Image

def main():
    print("Starting DayFlow Favicon Generation...")

    # Source image path
    src_path = r"C:\Users\georg\.gemini\antigravity\brain\f8f289f8-f2c0-49c1-b5ad-b9b5fb8737cb\.user_uploaded\media_1795533270241_281ff2d8.png"
    img = Image.open(src_path).convert("RGBA")
    print(f"Loaded source image: {img.size}")

    # Crop to emblem region: x in [106, 206], y in [114, 208]
    # In original: xmin=107, xmax=205, ymin=114, ymax=207
    crop = img.crop((106, 114, 206, 208))
    arr = np.array(crop).astype(np.float32)
    h, w, _ = arr.shape
    print(f"Emblem crop size: {w}x{h}")

    # Calculate distance from pure white (255, 255, 255)
    # The background and checkmark negative space in the card are pure white (255,255,255)
    max_diff = np.max(255.0 - arr[:, :, :3], axis=2)

    # Smooth alpha ramp for anti-aliasing
    alpha = np.clip(max_diff / 45.0, 0.0, 1.0)

    # Reconstruct un-premultiplied foreground colors
    fg = np.zeros_like(arr[:, :, :3])
    mask = alpha > 0.005
    for c in range(3):
        fg[mask, c] = np.clip((arr[mask, c] - 255.0 * (1.0 - alpha[mask])) / alpha[mask], 0.0, 255.0)

    # Clean emblem with true alpha channel
    clean_rgba = np.dstack([fg, alpha * 255.0]).astype(np.uint8)
    clean_img = Image.fromarray(clean_rgba)

    # Save intermediate high-quality raw transparent emblem
    os.makedirs("docs/assets", exist_ok=True)
    clean_img.save("docs/assets/emblem_transparent_clean.png")
    print("Saved docs/assets/emblem_transparent_clean.png")

    # Center emblem into a square with balanced 6% padding
    max_dim = max(w, h)
    sq_size = int(max_dim * 1.15) # 15% breathing room for perfect favicon scale
    sq_canvas = Image.new("RGBA", (sq_size, sq_size), (0, 0, 0, 0))
    offset_x = (sq_size - w) // 2
    offset_y = (sq_size - h) // 2
    sq_canvas.paste(clean_img, (offset_x, offset_y), clean_img)

    # Target directory: public/
    os.makedirs("public", exist_ok=True)

    # High quality Lanczos resampled outputs
    sizes = {
        "public/favicon-16x16.png": 16,
        "public/favicon-32x32.png": 32,
        "public/favicon-48x48.png": 48,
        "public/favicon.png": 192,
        "public/apple-touch-icon.png": 180,
        "public/android-chrome-192x192.png": 192,
        "public/android-chrome-512x512.png": 512,
    }

    # Generate high-res 1024x1024 master
    master_1024 = sq_canvas.resize((1024, 1024), Image.Resampling.LANCZOS)

    for path, sz in sizes.items():
        resized = master_1024.resize((sz, sz), Image.Resampling.LANCZOS)
        resized.save(path)
        print(f"Generated {path} ({sz}x{sz})")

    # Generate standard multi-resolution favicon.ico
    ico_sizes = [(16, 16), (32, 32), (48, 48), (64, 64)]
    ico_images = [master_1024.resize(sz, Image.Resampling.LANCZOS) for sz in ico_sizes]
    ico_images[0].save("public/favicon.ico", format="ICO", sizes=ico_sizes)
    print("Generated public/favicon.ico")

    # Android Mipmap generation
    # DayFlow dark background color: #07120F
    dark_bg_color = (7, 18, 15, 255)

    android_mipmap_configs = [
        ("mipmap-mdpi", 48, 108),
        ("mipmap-hdpi", 72, 162),
        ("mipmap-xhdpi", 96, 216),
        ("mipmap-xxhdpi", 144, 324),
        ("mipmap-xxxhdpi", 192, 432),
    ]

    res_base = r"android\app\src\main\res"
    if os.path.exists(res_base):
        for folder, launcher_sz, fg_sz in android_mipmap_configs:
            target_dir = os.path.join(res_base, folder)
            os.makedirs(target_dir, exist_ok=True)

            # 1. ic_launcher.png (square with rounded corners on dark bg)
            # Create dark bg canvas
            bg_launcher = Image.new("RGBA", (launcher_sz, launcher_sz), dark_bg_color)
            # Paste emblem scaled to ~75%
            emblem_sz = int(launcher_sz * 0.75)
            emblem_resized = master_1024.resize((emblem_sz, emblem_sz), Image.Resampling.LANCZOS)
            offset = (launcher_sz - emblem_sz) // 2
            bg_launcher.paste(emblem_resized, (offset, offset), emblem_resized)
            bg_launcher.save(os.path.join(target_dir, "ic_launcher.png"))

            # 2. ic_launcher_round.png
            bg_launcher.save(os.path.join(target_dir, "ic_launcher_round.png"))

            # 3. ic_launcher_foreground.png (transparent background, adaptive safe zone ~60%)
            fg_canvas = Image.new("RGBA", (fg_sz, fg_sz), (0, 0, 0, 0))
            fg_emblem_sz = int(fg_sz * 0.60)
            fg_emblem_resized = master_1024.resize((fg_emblem_sz, fg_emblem_sz), Image.Resampling.LANCZOS)
            fg_offset = (fg_sz - fg_emblem_sz) // 2
            fg_canvas.paste(fg_emblem_resized, (fg_offset, fg_offset), fg_emblem_resized)
            fg_canvas.save(os.path.join(target_dir, "ic_launcher_foreground.png"))

            print(f"Updated Android {folder} launcher icons ({launcher_sz}x{launcher_sz}, fg: {fg_sz}x{fg_sz})")

        # Update ic_launcher_background.xml
        bg_xml_path = os.path.join(res_base, "values", "ic_launcher_background.xml")
        if os.path.exists(bg_xml_path):
            with open(bg_xml_path, "w", encoding="utf-8") as f:
                f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#07120F</color>\n</resources>\n')
            print("Updated ic_launcher_background.xml to #07120F")

    # Now create the vector SVG for public/favicon.svg
    # Let's create an exact, beautiful scalable vector SVG
    # using exact coordinates and SVG linear gradient
    svg_content = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 110 110" fill="none">
  <defs>
    <linearGradient id="dayflow-dcheck-grad" x1="5%" y1="5%" x2="95%" y2="85%">
      <stop offset="0%" stop-color="#38E1E5" />
      <stop offset="42%" stop-color="#29D67A" />
      <stop offset="100%" stop-color="#F0DF4B" />
    </linearGradient>
    <filter id="subtle-glow" x="-10%" y="-10%" width="120%" height="120%">
      <feDropShadow dx="0" dy="1" stdDeviation="1.5" flood-color="#29D67A" flood-opacity="0.25" />
    </filter>
  </defs>
  <!-- DAYFLOW D-Check Emblem -->
  <g transform="translate(5, 7)">
    <path fill="url(#dayflow-dcheck-grad)" fill-rule="evenodd" d="
      M 2 1
      C 1 1 0 2 0 4
      L 0 90
      C 0 92 1 93 3 93
      L 56 93
      C 61 93 64 91 68 90
      C 77 85 81 82 84 79
      C 89 73 91 69 94 63
      C 95 60 96 57 96 51
      C 97 50 97 45 96 37
      C 95 36 95 30 96 27
      C 98 24 99 21 99 15
      C 98 14 97 15 96 16
      L 85 28
      L 80 32
      L 75 39
      L 66 49
      L 51 65
      L 39 77
      L 22 77
      C 19 77 18 75 18 73
      L 18 36
      C 18 34 20 34 22 34
      L 34 34
      C 35 34 36 35 36 37
      L 36 54
      L 43 49
      L 55 36
      L 64 26
      L 73 16
      C 74 13 74 9 73 7
      C 70 4 65 2 62 1
      L 56 0
      Z
    " />
  </g>
</svg>
'''
    with open("public/favicon.svg", "w", encoding="utf-8") as f:
        f.write(svg_content)
    print("Generated public/favicon.svg")

if __name__ == "__main__":
    main()
