#!/usr/bin/env python3
from __future__ import annotations

import math
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "store-listing"
SOURCE = OUT / "source"

BACKDROP = SOURCE / "oops-dumbbell-cereal-backdrop.png"
ICON = ROOT / "assets" / "app-icons" / "oops-all-prs-icon-master.png"

FONT_REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"
FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
FONT_BLACK = "/System/Library/Fonts/Supplemental/Arial Black.ttf"


@dataclass(frozen=True)
class Shot:
    slug: str
    headline: str
    subhead: str
    source: str
    bg: tuple[int, int, int]
    accent: tuple[int, int, int]
    screenshot_align: str = "bottom"


SHOTS = [
    Shot(
        "turn-workouts-into-prs",
        "Turn workouts into PRs",
        "Log sets fast and see records the moment they happen.",
        "specs/011-milestone-ux-hardening/validation/screenshots/core-14-second-set-logged.png",
        (8, 14, 23),
        (90, 226, 242),
    ),
    Shot(
        "know-your-next-target",
        "Know the next target",
        "Previous values stay close, so every set has context.",
        "specs/018-previous-workout-values/validation/screenshots/pixel-9-pro-weighted-previous-values.png",
        (19, 22, 34),
        (255, 192, 54),
    ),
    Shot(
        "recent-prs-at-a-glance",
        "Recent PRs at a glance",
        "Track your latest bests without digging through logs.",
        "specs/011-milestone-ux-hardening/validation/screenshots/continuity-20-progress-final.png",
        (10, 26, 31),
        (77, 214, 164),
    ),
    Shot(
        "history-that-helps",
        "History that helps",
        "Review completed workouts and save the good ones as templates.",
        "specs/011-milestone-ux-hardening/validation/screenshots/continuity-14-history-summary-final.png",
        (18, 15, 29),
        (246, 111, 146),
    ),
    Shot(
        "templates-without-friction",
        "Templates without friction",
        "Start from routines, or jump into a blank workout.",
        "specs/011-milestone-ux-hardening/validation/screenshots/continuity-18-train-template-final.png",
        (9, 16, 31),
        (80, 177, 255),
    ),
    Shot(
        "local-first-data",
        "Your data stays yours",
        "Local-first logging with straightforward CSV exports.",
        "specs/011-milestone-ux-hardening/validation/screenshots/continuity-21-profile-final.png",
        (15, 21, 22),
        (255, 151, 62),
    ),
]


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size)


def cover(im: Image.Image, size: tuple[int, int]) -> Image.Image:
    return ImageOps.fit(im.convert("RGB"), size, method=Image.Resampling.LANCZOS, centering=(0.5, 0.5))


def contain(im: Image.Image, size: tuple[int, int]) -> Image.Image:
    out = im.copy().convert("RGBA")
    out.thumbnail(size, Image.Resampling.LANCZOS)
    return out


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size[0], size[1]), radius=radius, fill=255)
    return mask


def rounded_paste(base: Image.Image, im: Image.Image, xy: tuple[int, int], radius: int, shadow: bool = True) -> None:
    im = im.convert("RGBA")
    if shadow:
        shadow_layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
        shadow_draw = ImageDraw.Draw(shadow_layer)
        x, y = xy
        shadow_draw.rounded_rectangle(
            (x + 14, y + 20, x + im.width + 14, y + im.height + 20),
            radius=radius,
            fill=(0, 0, 0, 120),
        )
        base.alpha_composite(shadow_layer.filter(ImageFilter.GaussianBlur(28)))
    mask = rounded_mask(im.size, radius)
    base.paste(im, xy, mask)


def draw_wrapped(
    draw: ImageDraw.ImageDraw,
    text: str,
    xy: tuple[int, int],
    max_width: int,
    fnt: ImageFont.FreeTypeFont,
    fill: tuple[int, int, int] | tuple[int, int, int, int],
    line_spacing: int,
) -> int:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if draw.textbbox((0, 0), candidate, font=fnt)[2] <= max_width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)

    x, y = xy
    for line in lines:
        draw.text((x, y), line, font=fnt, fill=fill)
        y += fnt.size + line_spacing
    return y


def make_background(size: tuple[int, int], shot: Shot, idx: int) -> Image.Image:
    w, h = size
    base = Image.new("RGBA", size, shot.bg + (255,))
    draw = ImageDraw.Draw(base)

    # Soft non-monochrome color fields.
    fields = [
        (shot.accent, (int(w * 0.72), int(h * 0.10)), int(w * 0.55)),
        ((255, 211, 67), (int(w * 0.18), int(h * 0.22)), int(w * 0.42)),
        ((255, 92, 136), (int(w * 0.92), int(h * 0.72)), int(w * 0.48)),
    ]
    for color, center, radius in fields:
        layer = Image.new("RGBA", size, (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        cx, cy = center
        ld.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=color + (46,))
        base.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius // 3)))

    if BACKDROP.exists():
        cereal = cover(Image.open(BACKDROP), size).convert("RGBA")
        cereal = cereal.filter(ImageFilter.GaussianBlur(2 if h > 2200 else 1))
        tint = Image.new("RGBA", size, (0, 0, 0, 96 if h > 2200 else 62))
        cereal.alpha_composite(tint)
        cereal.putalpha(90 if h > 2200 else 105)
        base.alpha_composite(cereal)

    # Branded diagonal energy stripe.
    stripe = Image.new("RGBA", size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(stripe)
    for offset, color in enumerate([shot.accent, (255, 211, 67), (255, 92, 136)]):
        y0 = int(h * 0.12 + offset * h * 0.045)
        sd.polygon(
            [(-w * 0.1, y0), (w * 1.15, y0 + int(h * 0.12)), (w * 1.15, y0 + int(h * 0.15)), (-w * 0.1, y0 + int(h * 0.03))],
            fill=color + (35,),
        )
    base.alpha_composite(stripe)

    return base


def render_phone_shot(size: tuple[int, int], shot: Shot, index: int, out_path: Path) -> None:
    w, h = size
    base = make_background(size, shot, index)
    draw = ImageDraw.Draw(base)

    margin = int(w * 0.075)
    top = int(h * 0.06)
    label_font = font(FONT_BOLD, int(w * 0.032))
    headline_font = font(FONT_BLACK, int(w * 0.078))
    sub_font = font(FONT_REGULAR, int(w * 0.036))

    brand = "OOPS ALL PRS"
    brand_box = draw.textbbox((0, 0), brand, font=label_font)
    pill_pad_x = int(w * 0.026)
    pill_w = brand_box[2] - brand_box[0] + pill_pad_x * 2
    pill_h = int(w * 0.06)
    draw.rounded_rectangle(
        (margin, top, margin + pill_w, top + pill_h),
        radius=pill_h // 2,
        fill=(7, 16, 23, 218),
        outline=shot.accent + (185,),
        width=max(2, w // 400),
    )
    text_y = top + (pill_h - (brand_box[3] - brand_box[1])) // 2 - brand_box[1]
    draw.text((margin + pill_pad_x, text_y), brand, font=label_font, fill=(245, 252, 255, 245))

    text_top = top + int(h * 0.11)
    text_bottom = draw_wrapped(draw, shot.headline, (margin, text_top), int(w * 0.86), headline_font, (255, 255, 255, 255), int(w * 0.008))
    draw_wrapped(draw, shot.subhead, (margin, text_bottom + int(h * 0.012)), int(w * 0.82), sub_font, (217, 231, 246, 230), int(w * 0.006))

    src = Image.open(ROOT / shot.source).convert("RGBA")
    # Crop away status/nav bars less aggressively for honesty; then frame like a live phone capture.
    src = src.crop((0, int(src.height * 0.018), src.width, int(src.height * 0.985)))
    phone_w = int(w * 0.72)
    phone_h = int(phone_w * src.height / src.width)
    if phone_h > int(h * 0.62):
        phone_h = int(h * 0.62)
        phone_w = int(phone_h * src.width / src.height)
    screenshot = src.resize((phone_w, phone_h), Image.Resampling.LANCZOS)

    frame_pad = int(w * 0.018)
    frame = Image.new("RGBA", (phone_w + frame_pad * 2, phone_h + frame_pad * 2), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame)
    radius = int(w * 0.065)
    fd.rounded_rectangle((0, 0, frame.width, frame.height), radius=radius, fill=(10, 12, 20, 255), outline=shot.accent + (170,), width=max(3, w // 180))
    rounded_paste(frame, screenshot, (frame_pad, frame_pad), max(12, radius - frame_pad), shadow=False)

    x = (w - frame.width) // 2
    y = int(h * 0.36)
    rounded_paste(base, frame, (x, y), radius, shadow=True)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    base.convert("RGB").save(out_path, quality=96)


def render_feature(out_path: Path) -> None:
    w, h = (1024, 500)
    base = cover(Image.open(BACKDROP), (w, h)).convert("RGBA") if BACKDROP.exists() else Image.new("RGBA", (w, h), (9, 16, 31, 255))
    overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.rectangle((0, 0, w, h), fill=(5, 8, 14, 82))
    od.rectangle((0, 0, int(w * 0.57), h), fill=(5, 8, 14, 96))
    base.alpha_composite(overlay)
    draw = ImageDraw.Draw(base)

    icon = Image.open(ICON).convert("RGBA").resize((104, 104), Image.Resampling.LANCZOS)
    rounded_paste(base, icon, (58, 62), 22, shadow=True)

    draw.text((58, 188), "Oops All PRs", font=font(FONT_BLACK, 64), fill=(255, 255, 255, 255))
    draw_wrapped(
        draw,
        "A fast workout logger for chasing personal records.",
        (62, 270),
        460,
        font(FONT_BOLD, 29),
        (224, 240, 249, 238),
        8,
    )
    draw.rounded_rectangle((62, 378, 320, 432), radius=27, fill=(88, 223, 242, 238))
    draw.text((92, 391), "Log. Lift. PR.", font=font(FONT_BOLD, 24), fill=(7, 16, 23, 255))

    phone_sources = [
        ROOT / "specs/011-milestone-ux-hardening/validation/screenshots/core-14-second-set-logged.png",
        ROOT / "specs/011-milestone-ux-hardening/validation/screenshots/continuity-20-progress-final.png",
    ]
    for i, p in enumerate(phone_sources):
        shot = Image.open(p).convert("RGBA").crop((0, 50, 1280, 2760))
        ph = 420
        pw = int(ph * shot.width / shot.height)
        shot = shot.resize((pw, ph), Image.Resampling.LANCZOS)
        frame = Image.new("RGBA", (pw + 16, ph + 16), (0, 0, 0, 0))
        ImageDraw.Draw(frame).rounded_rectangle((0, 0, frame.width, frame.height), radius=36, fill=(8, 10, 18, 255), outline=(88, 223, 242, 190), width=3)
        rounded_paste(frame, shot, (8, 8), 29, shadow=False)
        rounded_paste(base, frame, (650 + i * 108, 48 + i * 28), 38, shadow=True)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    base.convert("RGB").save(out_path, quality=96)


def main() -> None:
    SOURCE.mkdir(parents=True, exist_ok=True)

    render_feature(OUT / "google-play" / "feature-graphic-1024x500.png")

    for idx, shot in enumerate(SHOTS, 1):
        render_phone_shot((1080, 1920), shot, idx, OUT / "google-play" / "screenshots" / f"{idx:02d}-{shot.slug}-1080x1920.png")
        render_phone_shot((1320, 2868), shot, idx, OUT / "app-store" / "iphone-6-9" / f"{idx:02d}-{shot.slug}-1320x2868.png")

    summary = OUT / "README.md"
    summary.write_text(
        "# Store Listing Assets\n\n"
        "Generated store-listing artwork for Oops All PRs.\n\n"
        "## Google Play\n\n"
        "- `google-play/feature-graphic-1024x500.png`\n"
        "- `google-play/screenshots/*.png` at 1080x1920\n\n"
        "## App Store\n\n"
        "- `app-store/iphone-6-9/*.png` at 1320x2868\n\n"
        "## Source\n\n"
        "- `source/oops-dumbbell-cereal-backdrop.png`: AI-generated dumbbell-cereal backdrop used for compositing.\n"
        "- Screenshots are sourced from validation captures under `specs/*/validation/screenshots`.\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
