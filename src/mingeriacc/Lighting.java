package mingeriacc;

/**
 * Tile lighting, computed for the visible area plus a margin. Sunlight and
 * light from torches (and other glowing things) are separate channels with
 * levels 0..15. Block light also carries a colour, taken from the brightest
 * source that reaches a tile, so torches stay warm and crystals pink without
 * the hue shifting with distance. Light loses one level per tile through air
 * and three through solid tiles.
 */
final class Lighting {
    static final int MAX = 15;
    static final int MARGIN = 16;

    int x0, y0, lw, lh;
    byte[] sky = new byte[0], block = new byte[0];
    int[] tint = new int[0];
    private final int[][] buckets = new int[MAX + 1][];
    private final int[] counts = new int[MAX + 1];
    private byte[] cost = new byte[0];

    /** Brightness factor per level (0..256). */
    static final int[] SHADE = new int[MAX + 1];

    static {
        for (int i = 0; i <= MAX; i++) {
            double t = i / (double) MAX;
            SHADE[i] = (int) Math.round(256 * Math.pow(t, 0.95));
        }
    }

    Lighting() {
        for (int i = 0; i <= MAX; i++) buckets[i] = new int[1024];
    }

    private void push(int lvl, int idx) {
        int[] b = buckets[lvl];
        if (counts[lvl] == b.length) {
            b = java.util.Arrays.copyOf(b, b.length * 2);
            buckets[lvl] = b;
        }
        b[counts[lvl]++] = idx;
    }

    /**
     * Computes light for the tile area [tx0..tx1] x [ty0..ty1].
     * extra: additional lights as {x, y, level, colour, ...}.
     */
    void compute(World w, int tx0, int ty0, int tx1, int ty1, int skyLevel, int[] extra, int extraCount) {
        x0 = Math.max(0, tx0 - MARGIN);
        y0 = Math.max(0, ty0 - MARGIN);
        int x1 = Math.min(w.w - 1, tx1 + MARGIN);
        int y1 = Math.min(w.h - 1, ty1 + MARGIN);
        lw = Math.max(1, x1 - x0 + 1);
        lh = Math.max(1, y1 - y0 + 1);
        int n = lw * lh;
        if (sky.length < n) {
            sky = new byte[n];
            block = new byte[n];
            tint = new int[n];
            cost = new byte[n];
        }

        byte[] tiles = w.tiles;
        int W = w.w;
        for (int y = 0; y < lh; y++) {
            int wy = y + y0;
            int row = wy * W;
            for (int x = 0; x < lw; x++) {
                int wx = x + x0;
                int i = x + y * lw;
                int t = tiles[row + wx] & 0xff;
                cost[i] = (byte) (Tiles.blocksLight(t) ? 3 : 1);
                sky[i] = (byte) (wy < w.skyTop[wx] ? skyLevel : 0);
                block[i] = (byte) Tiles.LIGHT[t];
                tint[i] = Tiles.LIGHT_COLOR[t];
                if (wy >= w.underworld && !Tiles.SOLID[t] && block[i] < 8) {
                    // the underworld glows faintly everywhere
                    block[i] = 8;
                    tint[i] = 0xff8a5a;
                }
                if (w.liquid[row + wx] != 0 && w.lqType[row + wx] == Liquids.LAVA) {
                    block[i] = 12;
                    tint[i] = 0xff7a34;
                }
            }
        }
        for (int k = 0; k < extraCount; k++) {
            int ex = extra[k * 4] - x0, ey = extra[k * 4 + 1] - y0;
            if (ex < 0 || ey < 0 || ex >= lw || ey >= lh) continue;
            int i = ex + ey * lw;
            if (extra[k * 4 + 2] > block[i]) {
                block[i] = (byte) extra[k * 4 + 2];
                tint[i] = extra[k * 4 + 3];
            }
        }
        spread(sky, null, n);
        spread(block, tint, n);
    }

    /** Spreads one channel outwards from its seeds (bucket queue, brightest first). */
    private void spread(byte[] level, int[] color, int n) {
        java.util.Arrays.fill(counts, 0);
        for (int i = 0; i < n; i++) if (level[i] > 0) push(level[i], i);
        for (int lv = MAX; lv >= 1; lv--) {
            for (int k = 0; k < counts[lv]; k++) {
                int i = buckets[lv][k];
                if (level[i] != lv) continue;
                int lx = i % lw, ly = i / lw;
                int nl = lv - cost[i];
                if (nl <= 0) continue;
                if (lx > 0) light(level, color, i, i - 1, nl);
                if (lx < lw - 1) light(level, color, i, i + 1, nl);
                if (ly > 0) light(level, color, i, i - lw, nl);
                if (ly < lh - 1) light(level, color, i, i + lw, nl);
            }
        }
    }

    private void light(byte[] level, int[] color, int from, int to, int nl) {
        if (level[to] >= nl) return;
        level[to] = (byte) nl;
        if (color != null) color[to] = color[from];
        push(nl, to);
    }

    /** Overall brightness level 0..15 of a tile. */
    int get(int x, int y) {
        x -= x0;
        y -= y0;
        if (x < 0 || y < 0 || x >= lw || y >= lh) return 0;
        int i = x + y * lw;
        return Math.max(sky[i], block[i]);
    }

    /** Light colour of a tile as 0xRRGGBB (255 = full): sunlight tinted by skyTint plus block light. */
    int color(int x, int y, int skyTint) {
        x -= x0;
        y -= y0;
        if (x < 0 || y < 0 || x >= lw || y >= lh) return 0;
        int i = x + y * lw;
        int s = SHADE[sky[i]], b = SHADE[block[i]];
        int c = tint[i];
        int r = (s * (skyTint >> 16 & 255) + b * (c >> 16 & 255)) >> 8;
        int g = (s * (skyTint >> 8 & 255) + b * (c >> 8 & 255)) >> 8;
        int bl = (s * (skyTint & 255) + b * (c & 255)) >> 8;
        return (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, bl);
    }
}
