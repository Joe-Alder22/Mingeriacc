package mingeriacc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Pixel buffer. Everything is drawn here, and the finished image is scaled to
 * the window by a whole-number factor so the pixels stay sharp.
 *
 * The world layer uses {@link #mask}: written pixels get the mask's top byte,
 * so empty (0) pixels can be told apart from drawn black ones.
 */
final class Screen {
    int w, h;
    int[] px;
    BufferedImage img;
    /** OR'ed into every written pixel (0 for the main screen). */
    int mask;

    Screen(int w, int h) {
        resize(w, h);
    }

    void resize(int nw, int nh) {
        if (img != null && nw == w && nh == h) return;
        w = Math.max(1, nw);
        h = Math.max(1, nh);
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
    }

    void clear(int c) {
        java.util.Arrays.fill(px, c);
    }

    void pset(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < w && y < h) px[x + y * w] = (c & 0xffffff) | mask;
    }

    void pblend(int x, int y, int c, int a) {
        if (x >= 0 && y >= 0 && x < w && y < h) {
            int i = x + y * w;
            px[i] = Pal.blend(px[i], c, a) | mask;
        }
    }

    /** Additive light: a = 0..256. */
    void padd(int x, int y, int c, int a) {
        if (x >= 0 && y >= 0 && x < w && y < h) {
            int i = x + y * w;
            px[i] = Pal.add(px[i], c, a) | mask;
        }
    }

    void fill(int x, int y, int fw, int fh, int c) {
        int x0 = Math.max(0, x), y0 = Math.max(0, y);
        int x1 = Math.min(w, x + fw), y1 = Math.min(h, y + fh);
        c = (c & 0xffffff) | mask;
        for (int yy = y0; yy < y1; yy++) {
            int row = yy * w;
            for (int xx = x0; xx < x1; xx++) px[row + xx] = c;
        }
    }

    void fillA(int x, int y, int fw, int fh, int c, int a) {
        int x0 = Math.max(0, x), y0 = Math.max(0, y);
        int x1 = Math.min(w, x + fw), y1 = Math.min(h, y + fh);
        for (int yy = y0; yy < y1; yy++) {
            int row = yy * w;
            for (int xx = x0; xx < x1; xx++) px[row + xx] = Pal.blend(px[row + xx], c, a) | mask;
        }
    }

    /** Translucent rectangle with the corner pixels cut off. */
    void fillRound(int x, int y, int fw, int fh, int c, int a) {
        fillA(x + 1, y, fw - 2, 1, c, a);
        fillA(x, y + 1, fw, fh - 2, c, a);
        fillA(x + 1, y + fh - 1, fw - 2, 1, c, a);
    }

    /** Vertical gradient from c0 (top) to c1 (bottom). */
    void vgradient(int x, int y, int fw, int fh, int c0, int c1) {
        for (int i = 0; i < fh; i++) fill(x, y + i, fw, 1, Pal.lerp(c0, c1, fh <= 1 ? 0 : i / (double) (fh - 1)));
    }

    /** Darkens an area (s = 0..256). */
    void darken(int x, int y, int fw, int fh, int s) {
        int x0 = Math.max(0, x), y0 = Math.max(0, y);
        int x1 = Math.min(w, x + fw), y1 = Math.min(h, y + fh);
        for (int yy = y0; yy < y1; yy++) {
            int row = yy * w;
            for (int xx = x0; xx < x1; xx++) px[row + xx] = Pal.shade(px[row + xx], s) | mask;
        }
    }

    void rect(int x, int y, int rw, int rh, int c) {
        fill(x, y, rw, 1, c);
        fill(x, y + rh - 1, rw, 1, c);
        fill(x, y, 1, rh, c);
        fill(x + rw - 1, y, 1, rh, c);
    }

    /** Frame with the corner pixels left out. */
    void rectRound(int x, int y, int rw, int rh, int c) {
        fill(x + 1, y, rw - 2, 1, c);
        fill(x + 1, y + rh - 1, rw - 2, 1, c);
        fill(x, y + 1, 1, rh - 2, c);
        fill(x + rw - 1, y + 1, 1, rh - 2, c);
    }

    /** UI panel: translucent background with a soft frame. */
    void panel(int x, int y, int pw, int ph) {
        fillRound(x, y, pw, ph, Pal.UI_BG, 220);
        fillA(x + 2, y + 1, pw - 4, 1, Pal.WHITE, 30);
        rectRound(x, y, pw, ph, Pal.UI_EDGE);
    }

    /** Soft round glow, added to the image (a = strength 0..256 at the centre). */
    void glow(double cx, double cy, double r, int c, int a) {
        int x0 = (int) Math.floor(cx - r), x1 = (int) Math.ceil(cx + r);
        int y0 = (int) Math.floor(cy - r), y1 = (int) Math.ceil(cy + r);
        double r2 = r * r;
        for (int y = Math.max(0, y0); y <= Math.min(h - 1, y1); y++)
            for (int x = Math.max(0, x0); x <= Math.min(w - 1, x1); x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d2 = dx * dx + dy * dy;
                if (d2 >= r2) continue;
                double f = 1 - d2 / r2;
                int i = x + y * w;
                px[i] = Pal.add(px[i], c, (int) (a * f * f)) | mask;
            }
    }

    /** Filled circle blended with alpha a (0..256). */
    void disc(double cx, double cy, double r, int c, int a) {
        int x0 = (int) Math.floor(cx - r), x1 = (int) Math.ceil(cx + r);
        int y0 = (int) Math.floor(cy - r), y1 = (int) Math.ceil(cy + r);
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > r + 0.5) continue;
                int aa = d > r - 0.5 ? (int) (a * (r + 0.5 - d)) : a;
                if (aa >= 256) pset(x, y, c);
                else pblend(x, y, c, aa);
            }
    }

    // ---- sprites ---------------------------------------------------------

    private void put(int i, int c, int shadeOrLight, boolean isLight) {
        int a = c >>> 24;
        int col = isLight ? Pal.mulLight(c, shadeOrLight) : Pal.shade(c, shadeOrLight);
        if (a >= 250) px[i] = col | mask;
        else px[i] = Pal.blend(px[i], col, a + 1) | mask;
    }

    /** Draws an image; shade 0..256 darkens it, flip mirrors it horizontally. */
    void draw(Sprite s, int x, int y, boolean flip, int shade) {
        for (int sy = 0; sy < s.h; sy++) {
            int dy = y + sy;
            if (dy < 0 || dy >= h) continue;
            for (int sx = 0; sx < s.w; sx++) {
                int c = s.p[(flip ? s.w - 1 - sx : sx) + sy * s.w];
                if (c == 0) continue;
                int dx = x + sx;
                if (dx < 0 || dx >= w) continue;
                put(dx + dy * w, c, shade, false);
            }
        }
    }

    void draw(Sprite s, int x, int y) {
        draw(s, x, y, false, 256);
    }

    /** Draws an image multiplied by a light colour (0xRRGGBB, 0xffffff = unchanged). */
    void drawLit(Sprite s, int x, int y, boolean flip, int light) {
        for (int sy = 0; sy < s.h; sy++) {
            int dy = y + sy;
            if (dy < 0 || dy >= h) continue;
            for (int sx = 0; sx < s.w; sx++) {
                int c = s.p[(flip ? s.w - 1 - sx : sx) + sy * s.w];
                if (c == 0) continue;
                int dx = x + sx;
                if (dx < 0 || dx >= w) continue;
                put(dx + dy * w, c, light, true);
            }
        }
    }

    /**
     * Draws an image lit, with overall alpha (0..256) and an optional flash
     * towards flashColor (flash 0..256).
     */
    void drawFx(Sprite s, int x, int y, boolean flip, int light, int alpha, int flashColor, int flash) {
        for (int sy = 0; sy < s.h; sy++) {
            int dy = y + sy;
            if (dy < 0 || dy >= h) continue;
            for (int sx = 0; sx < s.w; sx++) {
                int c = s.p[(flip ? s.w - 1 - sx : sx) + sy * s.w];
                if (c == 0) continue;
                int dx = x + sx;
                if (dx < 0 || dx >= w) continue;
                int col = Pal.mulLight(c, light);
                if (flash > 0) col = Pal.blend(col, flashColor, flash);
                int a = ((c >>> 24) + 1) * alpha >> 8;
                int i = dx + dy * w;
                px[i] = (a >= 250 ? col : Pal.blend(px[i], col, a)) | mask;
            }
        }
    }

    /** Draws an image in a single colour (e.g. a shadow or outline). */
    void drawSilhouette(Sprite s, int x, int y, int color) {
        for (int sy = 0; sy < s.h; sy++)
            for (int sx = 0; sx < s.w; sx++)
                if (s.p[sx + sy * s.w] != 0) pset(x + sx, y + sy, color);
    }

    /**
     * Rotated drawing. (pivX, pivY) is the pivot in image coordinates and
     * (dx, dy) is where it lands on screen. flip mirrors the whole result.
     * light multiplies the colours (0xffffff = unchanged).
     */
    void drawRotated(Sprite s, double pivX, double pivY, double dx, double dy,
                     double angle, boolean flip, int light) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double r = 0;
        double[][] corners = {{0, 0}, {s.w, 0}, {0, s.h}, {s.w, s.h}};
        for (double[] c : corners) r = Math.max(r, Math.hypot(c[0] - pivX, c[1] - pivY));
        int ri = (int) Math.ceil(r) + 1;
        int cx = (int) Math.floor(dx), cy = (int) Math.floor(dy);
        for (int yy = cy - ri; yy <= cy + ri; yy++) {
            if (yy < 0 || yy >= h) continue;
            for (int xx = cx - ri; xx <= cx + ri; xx++) {
                if (xx < 0 || xx >= w) continue;
                double ox = xx + 0.5 - dx, oy = yy + 0.5 - dy;
                if (flip) ox = -ox;
                double sx = cos * ox + sin * oy + pivX;
                double sy = -sin * ox + cos * oy + pivY;
                int ix = (int) Math.floor(sx), iy = (int) Math.floor(sy);
                if (ix < 0 || iy < 0 || ix >= s.w || iy >= s.h) continue;
                int c = s.p[ix + iy * s.w];
                if (c == 0) continue;
                put(xx + yy * w, c, light, true);
            }
        }
    }

    /** A one-pixel line blended with alpha a (0..256). */
    void line(double x0, double y0, double x1, double y1, int c, int a) {
        int steps = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.floor(x0 + (x1 - x0) * t), y = (int) Math.floor(y0 + (y1 - y0) * t);
            if (a >= 256) pset(x, y, c);
            else pblend(x, y, c, a);
        }
    }

    /** Thick line at the given angle (0 = straight down), used for limbs. */
    void limb(double x0, double y0, double angle, double len, int thick, int color) {
        double dx = Math.sin(angle), dy = Math.cos(angle);
        int steps = (int) Math.ceil(len * 2);
        for (int i = 0; i <= steps; i++) {
            double t = len * i / steps;
            int x = (int) Math.floor(x0 + dx * t - (thick - 1) / 2.0);
            int y = (int) Math.floor(y0 + dy * t);
            fill(x, y, thick, thick > 1 ? 2 : 1, color);
        }
    }

    // ---- text ----------------------------------------------------------

    void text(String t, int x, int y, int c) {
        Font.draw(this, t, x, y, c);
    }

    /** Text with a dark shadow (readable on any background). */
    void textShadow(String t, int x, int y, int c) {
        Font.draw(this, t, x + 1, y + 1, Pal.BLACK);
        Font.draw(this, t, x, y, c);
    }

    /** Text with an outline. */
    void textOutline(String t, int x, int y, int c) {
        textOutline(t, x, y, c, Pal.BLACK);
    }

    void textOutline(String t, int x, int y, int c, int outline) {
        for (int oy = -1; oy <= 1; oy++)
            for (int ox = -1; ox <= 1; ox++)
                if (ox != 0 || oy != 0) Font.draw(this, t, x + ox, y + oy, outline);
        Font.draw(this, t, x, y, c);
    }

    void textCenter(String t, int cx, int y, int c) {
        textShadow(t, cx - Font.width(t) / 2, y, c);
    }

    /** Large text (each font pixel is n x n) with an outline and a soft shadow. */
    void textBig(String t, int x, int y, int n, int c, int shadow) {
        Screen tmp = new Screen(Font.width(t) + 2, 12);
        tmp.clear(0xff00ff);
        Font.draw(tmp, t, 1, 2, 0xffffff);
        for (int pass = 0; pass < 3; pass++) {
            for (int yy = 0; yy < tmp.h; yy++)
                for (int xx = 0; xx < tmp.w; xx++) {
                    if (tmp.px[xx + yy * tmp.w] != 0xffffff) continue;
                    int bx = x + (xx - 1) * n, by = y + (yy - 2) * n;
                    if (pass == 0 && shadow >= 0) fillA(bx + n / 2 + 1, by + n / 2 + 1, n, n, shadow, 120);
                    else if (pass == 1 && shadow >= 0) fill(bx - 1, by - 1, n + 2, n + 2, shadow);
                    else if (pass == 2) {
                        for (int k = 0; k < n; k++)
                            fill(bx, by + k, n, 1, Pal.lerp(Pal.lerp(c, 0xffffff, 0.3), Pal.shade(c, 200),
                                    ((yy - 2) * n + k) / (7.0 * n)));
                    }
                }
        }
    }
}
