"""Convert a white-background generated icon into a transparent 64px game sprite."""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage


def remove_background(
    image: Image.Image, tolerance: float, remove_enclosed: bool
) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.uint8).copy()
    rgb = rgba[:, :, :3].astype(np.int16)

    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    background_color = np.median(border, axis=0)
    distance = np.sqrt(np.sum((rgb - background_color) ** 2, axis=2))
    near_background = distance <= tolerance

    seeds = np.zeros(near_background.shape, dtype=bool)
    seeds[0, :] = near_background[0, :]
    seeds[-1, :] = near_background[-1, :]
    seeds[:, 0] = near_background[:, 0]
    seeds[:, -1] = near_background[:, -1]
    background = (
        near_background
        if remove_enclosed
        else ndimage.binary_propagation(seeds, mask=near_background)
    )
    rgba[background, 3] = 0
    return Image.fromarray(rgba, "RGBA")


def fit_sprite(image: Image.Image, size: int, padding: int) -> Image.Image:
    alpha = image.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("Background removal left no visible pixels")

    cropped = image.crop(bounds)
    available = size - 2 * padding
    scale = min(available / cropped.width, available / cropped.height)
    resized_size = (
        max(1, round(cropped.width * scale)),
        max(1, round(cropped.height * scale)),
    )
    resized = cropped.resize(resized_size, Image.Resampling.LANCZOS)

    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    position = ((size - resized.width) // 2, (size - resized.height) // 2)
    canvas.alpha_composite(resized, position)
    return canvas


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--size", type=int, default=64)
    parser.add_argument("--padding", type=int, default=3)
    parser.add_argument("--tolerance", type=float, default=80.0)
    parser.add_argument(
        "--remove-enclosed",
        action="store_true",
        help="Also clear white background trapped inside loops and handles",
    )
    args = parser.parse_args()

    with Image.open(args.source) as source:
        transparent = remove_background(
            source, args.tolerance, args.remove_enclosed
        )
        sprite = fit_sprite(transparent, args.size, args.padding)

    args.destination.parent.mkdir(parents=True, exist_ok=True)
    sprite.save(args.destination, format="PNG", optimize=True)


if __name__ == "__main__":
    main()
