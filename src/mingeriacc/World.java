package mingeriacc;

/** The world's tiles, walls and time. */
final class World {
    final int w, h;
    final byte[] tiles;
    final byte[] walls;
    /** Extra per-tile data: furniture cell offsets and flags. */
    final byte[] meta;
    /** For each column, the first row that blocks sunlight. */
    final int[] skyTop;
    /** Liquid amount per tile (0..255) and its type (Liquids.WATER / LAVA). */
    final byte[] liquid, lqType;
    final Liquids liquids;
    /** Storage chests by the index of their top-left tile. */
    final java.util.HashMap<Integer, Chest> chests = new java.util.HashMap<>();
    int underworld;   // top row of the underworld (h = none)

    String name = "World";
    long seed;
    /** Identifies the world in character files (where each character was and where its bed is). */
    long id;
    int sizeIndex;
    boolean downedEye;            // the Eye of Cthulhu has been defeated
    boolean bloodMoon, slimeRain; // events going on
    int slimeRainKills;
    int surfaceLevel; // below this row the backdrop is cave, not sky
    int rockLevel;    // top of the stone layer
    double time = 7.5; // time of day in hours 0..24
    int day = 1;
    int spawnX, spawnY; // tile coordinates (at the feet)

    World(int w, int h) {
        this.w = w;
        this.h = h;
        tiles = new byte[w * h];
        walls = new byte[w * h];
        meta = new byte[w * h];
        skyTop = new int[w];
        liquid = new byte[w * h];
        lqType = new byte[w * h];
        underworld = h;
        liquids = new Liquids(this);
    }

    /** A chest's contents. */
    static final class Chest {
        static final int SIZE = 40;
        final int x, y;
        final int[] id = new int[SIZE], count = new int[SIZE];

        Chest(int x, int y) {
            this.x = x;
            this.y = y;
        }

        boolean isEmpty() {
            for (int i : id) if (i != 0) return false;
            return true;
        }

        /** Puts items into the first free slots; returns what did not fit. */
        int add(int item, int n) {
            int max = Items.MAX_STACK[item];
            for (int i = 0; i < SIZE && n > 0; i++)
                if (id[i] == item && count[i] < max) {
                    int put = Math.min(n, max - count[i]);
                    count[i] += put;
                    n -= put;
                }
            for (int i = 0; i < SIZE && n > 0; i++)
                if (id[i] == 0) {
                    int put = Math.min(n, max);
                    id[i] = item;
                    count[i] = put;
                    n -= put;
                }
            return n;
        }
    }

    Chest chestAt(int x, int y) {
        if (!Tiles.isChest(tile(x, y))) return null;
        return chests.get(originX(x, y) + originY(x, y) * w);
    }

    int liquidAt(int x, int y) {
        if (!inside(x, y)) return 0;
        return liquid[x + y * w] & 0xff;
    }

    int liquidType(int x, int y) {
        if (!inside(x, y)) return 0;
        return lqType[x + y * w];
    }

    boolean isLava(int x, int y) {
        return liquidAt(x, y) > 0 && liquidType(x, y) == Liquids.LAVA;
    }

    boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    int tile(int x, int y) {
        if (x < 0 || x >= w || y >= h) return Tiles.STONE;
        if (y < 0) return Tiles.AIR;
        return tiles[x + y * w] & 0xff;
    }

    int wall(int x, int y) {
        if (!inside(x, y)) return Tiles.W_NONE;
        return walls[x + y * w] & 0xff;
    }

    int meta(int x, int y) {
        if (!inside(x, y)) return 0;
        return meta[x + y * w] & 0xff;
    }

    boolean solid(int x, int y) {
        return Tiles.SOLID[tile(x, y)];
    }

    /** Whether the tile can be stood on from above but passed from below. */
    boolean isPlatform(int x, int y) {
        int t = tile(x, y);
        if (t == Tiles.PLATFORM) return true;
        return Tiles.PLAT_TOP[t] && ((meta(x, y) >> 3) & 7) == 0;
    }

    void setRaw(int x, int y, int t) {
        tiles[x + y * w] = (byte) t;
    }

    void set(int x, int y, int t) {
        if (!inside(x, y)) return;
        int i = x + y * w;
        tiles[i] = (byte) t;
        meta[i] = 0;
        if (Tiles.SOLID[t]) liquid[i] = 0;
        updateSky(x);
        liquids.activateAround(x, y);
    }

    void setMeta(int x, int y, int m) {
        if (inside(x, y)) meta[x + y * w] = (byte) m;
    }

    void setWall(int x, int y, int wl) {
        if (!inside(x, y)) return;
        walls[x + y * w] = (byte) wl;
        updateSky(x);
    }

    void updateSky(int x) {
        int y = 0;
        while (y < h) {
            int i = x + y * w;
            int t = tiles[i] & 0xff;
            if ((Tiles.SOLID[t] && !Tiles.TRANSPARENT[t]) || walls[i] != 0) break;
            y++;
        }
        skyTop[x] = y;
    }

    void updateAllSky() {
        for (int x = 0; x < w; x++) updateSky(x);
    }

    /** Whether a straight line between two points (world pixels) passes no solid tile. */
    boolean lineClear(double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 2.0);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            if (solid((int) Math.floor((x0 + dx * t) / Tiles.T), (int) Math.floor((y0 + dy * t) / Tiles.T))) return false;
        }
        return true;
    }

    // ---- trees ----------------------------------------------------------------

    /** Removes the whole tree that has a trunk at (x, y), without drops. Returns the trunk length. */
    int removeTree(int x, int y) {
        int base = y, top = y;
        while (Tiles.isTrunk(tile(x, base + 1))) base++;
        while (Tiles.isTrunk(tile(x, top - 1))) top--;
        for (int ty = top; ty <= base; ty++) {
            set(x, ty, Tiles.AIR);
            if (Tiles.isLeaves(tile(x - 1, ty))) set(x - 1, ty, Tiles.AIR);
            if (Tiles.isLeaves(tile(x + 1, ty))) set(x + 1, ty, Tiles.AIR);
        }
        java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
        stack.push(new int[]{x, top - 1});
        int guard = 0;
        while (!stack.isEmpty() && guard++ < 600) {
            int[] p = stack.pop();
            if (!Tiles.isLeaves(tile(p[0], p[1]))) continue;
            if (Math.abs(p[0] - x) > 5 || p[1] < top - 12 || p[1] > base) continue;
            set(p[0], p[1], Tiles.AIR);
            stack.push(new int[]{p[0] + 1, p[1]});
            stack.push(new int[]{p[0] - 1, p[1]});
            stack.push(new int[]{p[0], p[1] + 1});
            stack.push(new int[]{p[0], p[1] - 1});
        }
        return base - top + 1;
    }

    /** Removes trees whose trunk does not stand on solid ground. Returns how many were removed. */
    int removeFloatingTrees() {
        int n = 0;
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h - 1; y++) {
                int i = x + y * w;
                if (!Tiles.isTrunk(tiles[i] & 0xff)) continue;
                int below = tiles[i + w] & 0xff;
                if (Tiles.isTrunk(below) || Tiles.SOLID[below]) continue;
                removeTree(x, y);
                n++;
            }
        return n;
    }

    /** Topmost solid tile in a column (surface height). */
    int surfaceAt(int x) {
        x = Math.max(0, Math.min(w - 1, x));
        for (int y = 0; y < h; y++) if (Tiles.SOLID[tiles[x + y * w] & 0xff]) return y;
        return h;
    }

    // ---- furniture ------------------------------------------------------------

    /** Left column of the furniture object covering (x, y). */
    int originX(int x, int y) {
        return x - (meta(x, y) & 7);
    }

    /** Top row of the furniture object covering (x, y). */
    int originY(int x, int y) {
        return y - ((meta(x, y) >> 3) & 7);
    }

    /** Places a furniture object with its top-left cell at (ox, oy). */
    void placeFurniture(int t, int ox, int oy, int flags) {
        for (int dy = 0; dy < Tiles.FURN_H[t]; dy++)
            for (int dx = 0; dx < Tiles.FURN_W[t]; dx++) {
                set(ox + dx, oy + dy, t);
                setMeta(ox + dx, oy + dy, dx | dy << 3 | flags << 6);
            }
    }

    /** Removes the furniture object covering (x, y). Returns its type (or AIR). */
    int removeFurniture(int x, int y) {
        int t = tile(x, y);
        if (!Tiles.isFurniture(t)) return Tiles.AIR;
        int ox = originX(x, y), oy = originY(x, y);
        for (int dy = 0; dy < Tiles.FURN_H[t]; dy++)
            for (int dx = 0; dx < Tiles.FURN_W[t]; dx++)
                if (tile(ox + dx, oy + dy) == t) set(ox + dx, oy + dy, Tiles.AIR);
        return t;
    }
}
