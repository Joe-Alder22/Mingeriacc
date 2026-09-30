package mingeriacc;

import java.util.Map;

/**
 * A small pixel image. Pixel 0 = transparent, otherwise alpha << 24 | rgb
 * (alpha 255 = opaque). Images are drawn in code, so no image files are needed.
 */
final class Sprite {
    final int w, h;
    final int[] p;

    Sprite(int w, int h) {
        this.w = w;
        this.h = h;
        this.p = new int[w * h];
    }

    void set(int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < w && y < h) p[x + y * w] = 0xff000000 | rgb;
    }

    /** Sets a pixel with an alpha 0..255. */
    void setA(int x, int y, int rgb, int a) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        if (a <= 0) p[x + y * w] = 0;
        else p[x + y * w] = (Math.min(255, a) << 24) | (rgb & 0xffffff);
    }

    void clear(int x, int y) {
        if (x >= 0 && y >= 0 && x < w && y < h) p[x + y * w] = 0;
    }

    int get(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return 0;
        return p[x + y * w];
    }

    boolean opaque(int x, int y) {
        return get(x, y) != 0;
    }

    Sprite copy() {
        Sprite s = new Sprite(w, h);
        System.arraycopy(p, 0, s.p, 0, p.length);
        return s;
    }

    /** Builds an image from strings: each character is a colour (dot = transparent). */
    static Sprite ascii(Map<Character, Integer> colors, String... rows) {
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        Sprite s = new Sprite(w, rows.length);
        for (int y = 0; y < rows.length; y++) {
            String r = rows[y];
            for (int x = 0; x < r.length(); x++) {
                Integer c = colors.get(r.charAt(x));
                if (c != null) s.set(x, y, c);
            }
        }
        return s;
    }

    /** Half-size copy (every other pixel), e.g. for dropped items. */
    Sprite half() {
        Sprite s = new Sprite((w + 1) / 2, (h + 1) / 2);
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                int c = get(x * 2, y * 2);
                if (c == 0) c = get(x * 2 + 1, y * 2 + 1);
                if (c == 0) c = get(x * 2 + 1, y * 2);
                s.p[x + y * s.w] = c;
            }
        return s;
    }

    /** Mirrored copy. */
    Sprite flipped() {
        Sprite s = new Sprite(w, h);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) s.p[(w - 1 - x) + y * w] = p[x + y * w];
        return s;
    }

    /** Copy with a 1-pixel outline of the given colour around opaque pixels (grows by 2). */
    Sprite outlined(int color) {
        Sprite s = new Sprite(w + 2, h + 2);
        for (int y = -1; y <= h; y++)
            for (int x = -1; x <= w; x++) {
                if (opaque(x, y)) {
                    s.p[(x + 1) + (y + 1) * s.w] = p[x + y * w];
                } else if (opaque(x - 1, y) || opaque(x + 1, y) || opaque(x, y - 1) || opaque(x, y + 1)) {
                    s.set(x + 1, y + 1, color);
                }
            }
        return s;
    }

    /** Copy where every opaque pixel is multiplied by a light colour. */
    Sprite tinted(int light) {
        Sprite s = new Sprite(w, h);
        for (int i = 0; i < p.length; i++) {
            if (p[i] == 0) continue;
            s.p[i] = (p[i] & 0xff000000) | Pal.mulLight(p[i], light);
        }
        return s;
    }

    /** Copy with colours replaced: pairs of {from, to}. */
    Sprite recolor(int... pairs) {
        Sprite s = copy();
        for (int i = 0; i < s.p.length; i++) {
            if (s.p[i] == 0) continue;
            int c = s.p[i] & 0xffffff;
            for (int k = 0; k < pairs.length; k += 2)
                if (c == (pairs[k] & 0xffffff)) {
                    s.p[i] = (s.p[i] & 0xff000000) | (pairs[k + 1] & 0xffffff);
                    break;
                }
        }
        return s;
    }

    /** Part of the image. */
    Sprite crop(int x0, int y0, int cw, int ch) {
        Sprite s = new Sprite(cw, ch);
        for (int y = 0; y < ch; y++)
            for (int x = 0; x < cw; x++) s.p[x + y * cw] = get(x0 + x, y0 + y);
        return s;
    }

    /** Scaled to fit within max x max pixels (nearest neighbour, never enlarged). */
    Sprite fit(int max) {
        if (w <= max && h <= max) return this;
        double k = Math.min(max / (double) w, max / (double) h);
        int nw = Math.max(1, (int) Math.round(w * k)), nh = Math.max(1, (int) Math.round(h * k));
        Sprite s = new Sprite(nw, nh);
        for (int y = 0; y < nh; y++)
            for (int x = 0; x < nw; x++) s.p[x + y * nw] = get((int) ((x + 0.5) / k), (int) ((y + 0.5) / k));
        return s;
    }
}
