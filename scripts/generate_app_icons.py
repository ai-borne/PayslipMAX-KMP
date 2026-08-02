#!/usr/bin/env python3
import os
import sys
from PIL import Image

def generate_icons():
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    src_icon_path = os.path.join(
        project_root,
        "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"
    )

    if not os.path.exists(src_icon_path):
        print(f"Error: Source icon not found at {src_icon_path}")
        sys.exit(1)

    print(f"Loading source icon from: {src_icon_path}")
    orig_img = Image.open(src_icon_path).convert("RGB")
    w, h = orig_img.size

    # Source background & foreground color reference
    r_bg, g_bg, b_bg = 52, 109, 241
    denom = (255 - r_bg) + (255 - g_bg) + (255 - b_bg)

    # 1. Extract alpha mask for logo
    mask = Image.new("L", (w, h))
    mask_pixels = mask.load()
    orig_pixels = orig_img.load()

    for y in range(h):
        for x in range(w):
            r, g, b = orig_pixels[x, y]
            t = ((r - r_bg) + (g - g_bg) + (b - b_bg)) / denom
            t_int = max(0, min(255, int(t * 255.0)))
            mask_pixels[x, y] = t_int

    # 2. Generate iOS Dark Icon (app-icon-dark-1024.png)
    # Background: Dark slate #181A20
    dark_bg_color = (24, 26, 32)
    dark_fg_color = (255, 255, 255)
    
    dark_img = Image.new("RGB", (w, h), dark_bg_color)
    dark_fg = Image.new("RGB", (w, h), dark_fg_color)
    dark_img.paste(dark_fg, (0, 0), mask)

    ios_appicon_dir = os.path.join(project_root, "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
    dark_icon_path = os.path.join(ios_appicon_dir, "app-icon-dark-1024.png")
    dark_img.save(dark_icon_path, "PNG")
    print(f"Saved iOS Dark Icon: {dark_icon_path}")

    # 3. Generate iOS Tinted Icon (app-icon-tinted-1024.png)
    # Background: #1A1A1A, Grayscale Logo: #E5E5E5
    tinted_bg_color = (26, 26, 26)
    tinted_fg_color = (229, 229, 229)

    tinted_img = Image.new("RGB", (w, h), tinted_bg_color)
    tinted_fg = Image.new("RGB", (w, h), tinted_fg_color)
    tinted_img.paste(tinted_fg, (0, 0), mask)

    tinted_icon_path = os.path.join(ios_appicon_dir, "app-icon-tinted-1024.png")
    tinted_img.save(tinted_icon_path, "PNG")
    print(f"Saved iOS Tinted Icon: {tinted_icon_path}")

    # 4. Generate Android Monochrome Adaptive Icons (ic_launcher_monochrome.png)
    # Canvas sizes for adaptive icons (108dp canvas with 72dp safe zone -> 2/3 scaling)
    android_res_dir = os.path.join(project_root, "composeApp/src/androidMain/res")
    density_map = {
        "mipmap-mdpi": 108,
        "mipmap-hdpi": 162,
        "mipmap-xhdpi": 216,
        "mipmap-xxhdpi": 324,
        "mipmap-xxxhdpi": 432,
    }

    # Full logo artwork in RGBA (transparent background, white logo)
    logo_rgba = Image.new("RGBA", (w, h), (255, 255, 255, 0))
    white_solid = Image.new("RGBA", (w, h), (255, 255, 255, 255))
    logo_rgba.paste(white_solid, (0, 0), mask)

    for folder, canvas_size in density_map.items():
        folder_path = os.path.join(android_res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)

        # 72dp safe zone in 108dp canvas means safe zone size is 2/3 of canvas_size
        safe_size = int(canvas_size * (72.0 / 108.0))
        margin = (canvas_size - safe_size) // 2

        scaled_logo = logo_rgba.resize((safe_size, safe_size), Image.Resampling.LANCZOS)
        
        canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
        canvas.paste(scaled_logo, (margin, margin), scaled_logo)

        mono_out_path = os.path.join(folder_path, "ic_launcher_monochrome.png")
        canvas.save(mono_out_path, "PNG")
        print(f"Saved Android Monochrome ({folder}): {mono_out_path}")

    print("All app icon assets generated successfully!")

if __name__ == "__main__":
    generate_icons()
