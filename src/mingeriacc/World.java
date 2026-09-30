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
    int sizeIndex;
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
