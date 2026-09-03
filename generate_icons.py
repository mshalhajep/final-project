#!/usr/bin/env python3
"""Generates legacy launcher icons (ic_launcher.png / ic_launcher_round.png) for the
LocalConnect brand: indigo->cyan gradient, telemetry arcs and a glowing central node.
Pure Python PNG writer (no PIL dependency)."""
import struct, zlib, math, os

RES = os.path.join(os.path.dirname(__file__), "app", "src", "main", "res")

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

C0 = (0x63, 0x66, 0xF1)  # indigo
C1 = (0x4F, 0x46, 0xE5)  # deep indigo
C2 = (0x0E, 0xA5, 0xE9)  # cyan


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def bg_color(t):
    if t < 0.45:
        return lerp(C0, C1, t / 0.45)
    return lerp(C1, C2, (t - 0.45) / 0.55)


def smooth_alpha(dist, half_width):
    if dist <= half_width - 0.75:
        return 1.0
    if dist >= half_width + 0.75:
        return 0.0
    return 0.5 - (dist - half_width) / 1.5 * 0.5


def in_arc(angle, lo, hi):
    # Wrap-around safe angular range check, angle/lo/hi in degrees
    span = (hi - lo) % 360.0
    return ((angle - lo) % 360.0) <= span


def render(size, round_mask):
    ss = 2  # 2x2 supersampling
    big = size * ss
    img = bytearray(big * big * 4)
    cx = cy = big / 2.0
    scale = big * 0.72 / 108.0  # emblem occupies 72% of the canvas

    # geometry in viewport units (108x108)
    node_r = 7.5
    glow_r = 12.5
    inner_r = 19.0
    outer_r = 29.0
    node_w = 2.25
    glow_w = 1.0
    inner_w = 2.25
    outer_w = 2.0
    end_r = 2.8
    ends = [(39, 40), (69, 40), (39, 68), (69, 68)]

    for y in range(big):
        for x in range(big):
            t = (x + y) / (2.0 * big)
            r, g, b = bg_color(t)

            # map pixel -> viewport coords
            vx = (x - cx) / scale + 54.0
            vy = (y - cy) / scale + 54.0
            dx = vx - 54.0
            dy = vy - 54.0
            dist = math.hypot(dx, dy)
            ang = math.degrees(math.atan2(dy, dx)) % 360.0

            alpha = 0.0
            # central node
            alpha = max(alpha, smooth_alpha(dist, node_r))
            # glow ring (40% opacity)
            ring = smooth_alpha(abs(dist - glow_r), glow_w) * 0.40
            alpha = max(alpha, ring)
            # inner arcs
            if in_arc(ang, 140, 220) or in_arc(ang, -40, 40):
                alpha = max(alpha, smooth_alpha(abs(dist - inner_r), inner_w))
            # outer arcs (70% opacity)
            if in_arc(ang, 140, 220) or in_arc(ang, -40, 40):
                alpha = max(alpha, smooth_alpha(abs(dist - outer_r), outer_w) * 0.70)
            # arc end nodes
            for ex, ey in ends:
                ed = math.hypot(vx - ex, vy - ey)
                alpha = max(alpha, smooth_alpha(ed, end_r))

            cov = max(0.0, min(1.0, alpha))
            # composite white emblem over the opaque gradient background
            i = (y * big + x) * 4
            img[i] = int(r + (255 - r) * cov)
            img[i + 1] = int(g + (255 - g) * cov)
            img[i + 2] = int(b + (255 - b) * cov)
            img[i + 3] = 255

    # downsample 2x2 (simple box average of composited pixels)
    out = bytearray(size * size * 4)
    for y in range(size):
        for x in range(size):
            sr = sg = sb = sa = 0
            for oy in range(ss):
                for ox in range(ss):
                    i = ((y * ss + oy) * big + (x * ss + ox)) * 4
                    sr += img[i]
                    sg += img[i + 1]
                    sb += img[i + 2]
                    sa += img[i + 3]
            n = ss * ss
            j = (y * size + x) * 4
            out[j] = sr // n
            out[j + 1] = sg // n
            out[j + 2] = sb // n
            out[j + 3] = sa // n

    if round_mask:
        # circular mask with 1px AA
        c = size / 2.0
        for y in range(size):
            for x in range(size):
                j = (y * size + x) * 4
                d = math.hypot(x + 0.5 - c, y + 0.5 - c)
                if d > c:
                    out[j + 3] = 0
                elif d > c - 1.0:
                    factor = c - d
                    out[j + 3] = int(out[j + 3] * factor)
    return bytes(out)


def write_png(path, size, rgba):
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = bytearray()
    stride = size * 4
    for y in range(size):
        raw.append(0)  # filter: none
        raw += rgba[y * stride:(y + 1) * stride]
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def main():
    for folder, size in DENSITIES.items():
        target = os.path.join(RES, folder)
        os.makedirs(target, exist_ok=True)
        # remove legacy default robot webp icons
        for name in ("ic_launcher.webp", "ic_launcher_round.webp"):
            p = os.path.join(target, name)
            if os.path.exists(p):
                os.remove(p)
        write_png(os.path.join(target, "ic_launcher.png"), size, render(size, False))
        write_png(os.path.join(target, "ic_launcher_round.png"), size, render(size, True))
        print(f"generated {folder}: {size}px")


if __name__ == "__main__":
    main()
