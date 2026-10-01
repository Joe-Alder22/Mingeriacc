package mingeriacc;

import java.util.HashMap;
import java.util.Map;

/**
 * Item icons, drawn in code. Tools and weapons are built from simple shapes
 * (lines, arcs, discs) shaded with a five-colour ramp and then outlined, so a
 * new material only needs a new ramp.
 */
final class ItemArt {
    private ItemArt() {}

    // colour ramps: outline, dark, mid, light, highlight
    static final int[] COPPER = {0x4a2410, Pal.COP_D, Pal.COP, Pal.COP_L, 0xffd2a8};
    static final int[] IRON = {0x26262e, 0x55555f, 0x86868f, 0xb4b4bc, 0xe2e2e8};
    static final int[] SILVER = {0x3e4452, 0x8a92a2, 0xbcc4d0, 0xe2e8f0, 0xffffff};
    static final int[] GOLD = {0x4e3806, 0xa07818, 0xdcb42e, 0xf6dc6c, 0xfff6c0};
    static final int[] WOODR = {0x2e1c0e, Pal.WOOD_D, Pal.WOOD, Pal.WOOD_L, 0xd09a62};
    static final int[] PLATINUM = {0x3a4250, 0x98a8bc, 0xc8d6e6, 0xe8f0f8, 0xffffff};
    static final int[] SHADOW = {0x1a1230, 0x3a2a6a, 0x6a4ac0, 0x9a80e8, 0xd0c0ff};
    static final int[] MOLTEN = {0x3a0a04, 0x9a2008, 0xe05010, 0xff9a30, 0xffe080};
    static final int[] MINING = {0x4a3a10, 0x9a7a20, 0xd8b040, 0xf0d870, 0xfff4c0};
    static final int[] GRASSR = {0x1a3a0c, 0x3a7a1c, 0x5aa82c, 0x86cc40, 0xc0f080};
    static final int[] ARCANE = {0x1e1036, 0x42227a, 0x6a3ab0, 0x9a6ad8, 0xd0b0ff};
    static final int[] ICER = {0x1e3a5a, Pal.ICE_D, Pal.ICE, Pal.ICE_L, 0xf4fcff};

    private static Sprite ramped(int[] m, String... rows) {
        return Sprite.ascii(map('o', m[0], 'd', m[1], 'm', m[2], 'l', m[3], 'h', m[4]), rows);
    }

    static Sprite helmet(int[] m) {
        return ramped(m,
                "...oooooo...",
                "..ohhllllmo.",
                ".ohllmmmmmdo",
                ".olmmmmmmddo",
                ".ommmmmmmddo",
                ".odmoooooddo",
                ".odo.....odo",
                ".oo......oo.");
    }

    static Sprite chainmail(int[] m) {
        return ramped(m,
                "..oo....oo..",
                ".ohlo..oldo.",
                "ohllmooommdo",
                "olmmmmmmmmdo",
                "olmlmlmlmmdo",
                "ommmmmmmmmdo",
                ".ommmlmmmdo.",
                ".olmmmmmmdo.",
                ".ommmmmmmdo.",
                ".oddddddddo.",
                "..oooooooo..");
    }

    static Sprite greaves(int[] m) {
        return ramped(m,
                ".oooooooooo.",
                ".ohllmmmmdo.",
                ".olmmdommdo.",
                ".olmdo.omdo.",
                ".olmdo.olmo.",
                ".olmdo.olmo.",
                ".olmdo.olmo.",
                "oolmdo.olmoo",
                "ohlmddoolmdo",
                "oooooo.ooooo");
    }

    private static Sprite bucket(int top, int topL) {
        Map<Character, Integer> c = map('o', 0x26262e, 'd', 0x55555f, 'm', 0x86868f, 'l', 0xb4b4bc, 'h', 0xe2e2e8,
                't', top, 'T', topL);
        return Sprite.ascii(c,
                "..oooooo..",
                ".o......o.",
                "o.oooooo.o",
                "ohTTTTTtdo",
                "ohlttttmdo",
                ".olmmmmdo.",
                ".olmmmmdo.",
                ".olmmmmdo.",
                "..oddddo..",
                "..oooooo..");
    }

    private static final int HANDLE_D = 0x5a3a20, HANDLE = 0x8a5a30, HANDLE_L = 0xb07c48;

    // ---- painter -----------------------------------------------------------

    private static double segDist(double px, double py, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0;
        double t = ((px - x0) * dx + (py - y0) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double qx = x0 + dx * t - px, qy = y0 + dy * t - py;
        return Math.sqrt(qx * qx + qy * qy);
    }

    /** Signed side of a point relative to a line (positive = left of the direction). */
    private static double side(double px, double py, double x0, double y0, double x1, double y1) {
        return ((x1 - x0) * (py - y0) - (y1 - y0) * (px - x0)) / Math.hypot(x1 - x0, y1 - y0);
    }

    /** A thick shaded line (handle, blade): the lit side is up-left. */
    private static void bar(Sprite s, double x0, double y0, double x1, double y1, double thick, int dark, int mid, int light) {
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double px = x + 0.5, py = y + 0.5;
                if (segDist(px, py, x0, y0, x1, y1) > thick / 2) continue;
                double sd = side(px, py, x0, y0, x1, y1);
                s.set(x, y, sd > 0.35 ? light : sd < -0.35 ? dark : mid);
            }
    }

    private static void handle(Sprite s, double x0, double y0, double x1, double y1) {
        bar(s, x0, y0, x1, y1, 2.2, HANDLE_D, HANDLE, HANDLE_L);
    }

    // ---- tools --------------------------------------------------------------

    static Sprite pickaxe(int[] m) {
        Sprite s = new Sprite(13, 13);
        handle(s, 1.5, 11.5, 7.8, 5.2);
        double cx = 1.0, cy = 12.0;
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double dx = x + 0.5 - cx, dy = cy - (y + 0.5);
                double r = Math.hypot(dx, dy), a = Math.toDegrees(Math.atan2(dy, dx));
                if (a < 8 || a > 82) continue;
                double mid = Math.abs(a - 45) / 37.0;   // 0 at the middle, 1 at the tips
                double half = 1.6 - mid * 0.9;
                double d = r - 10.2;
                if (Math.abs(d) > half) continue;
                int c = d > half * 0.45 ? m[3] : d < -half * 0.45 ? m[1] : m[2];
                if (d > half * 0.45 && mid < 0.35) c = m[4];
                s.set(x, y, c);
            }
        return s.outlined(m[0]);
    }

    static Sprite axe(int[] m) {
        Sprite s = new Sprite(13, 13);
        handle(s, 1.5, 11.5, 9.5, 1.8);
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double px = x + 0.5, py = y + 0.5;
                double dx = px - 5.6, dy = py - 3.6;
                double d = Math.hypot(dx * 0.9, dy * 1.1);
                if (d > 4.2) continue;
                if (side(px, py, 1.5, 11.5, 9.5, 1.8) < 0.6) continue;
                int c = d > 3.3 ? m[4] : d > 2.4 ? m[3] : d > 1.4 ? m[2] : m[1];
                s.set(x, y, c);
            }
        return s.outlined(m[0]);
    }

    static Sprite hammer(int[] m, boolean wooden) {
        Sprite s = new Sprite(13, 13);
        handle(s, 1.5, 11.5, 7.5, 5.5);
        double hx = 8.3, hy = 4.7;       // head centre
        double ux = 0.7071, uy = 0.7071; // long axis (perpendicular to the handle)
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double px = x + 0.5 - hx, py = y + 0.5 - hy;
                double a = px * ux + py * uy, b = -px * uy + py * ux;
                if (Math.abs(a) > 4.3 || Math.abs(b) > 2.1) continue;
                int c = b < -1.1 ? m[3] : b > 1.1 ? m[1] : m[2];
                if (Math.abs(a) > 3.6) c = wooden ? m[1] : m[3];
                if (b < -1.1 && a < -1.5) c = m[4];
                s.set(x, y, c);
            }
        return s.outlined(m[0]);
    }

    static Sprite broadsword(int[] m, int guard, int guardD, int grip) {
        Sprite s = new Sprite(14, 14);
        // blade
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double px = x + 0.5, py = y + 0.5;
                double t = ((px - 4.5) * 0.7071 - (py - 9.5) * 0.7071);   // along the blade
                double n = ((px - 4.5) * 0.7071 + (py - 9.5) * 0.7071);   // across
                double len = 11.0;
                if (t < 0 || t > len) continue;
                double half = t > len - 2.5 ? (len - t) / 2.5 * 1.5 : 1.5;
                if (Math.abs(n) > half) continue;
                int c = n < -0.5 ? m[3] : n > 0.6 ? m[1] : m[2];
                if (Math.abs(n) < 0.35 && t > 1.5 && t < len - 2) c = m[4];
                s.set(x, y, c);
            }
        // guard across the blade
        bar(s, 2.3, 7.6, 6.4, 11.7, 2.0, guardD, guard, Pal.lerp(guard, 0xffffff, 0.35));
        // grip and pommel
        bar(s, 1.4, 12.6, 3.8, 10.2, 1.8, Pal.shade(grip, 170), grip, Pal.lerp(grip, 0xffffff, 0.25));
        s.set(0, 13, guardD);
        s.set(1, 13, guard);
        s.set(0, 12, guard);
        return s.outlined(m[0]);
    }

    static Sprite shortsword(int[] m) {
        Sprite s = new Sprite(10, 10);
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double px = x + 0.5, py = y + 0.5;
                double t = ((px - 3.2) * 0.7071 - (py - 6.8) * 0.7071);
                double n = ((px - 3.2) * 0.7071 + (py - 6.8) * 0.7071);
                double len = 7.2;
                if (t < 0 || t > len) continue;
                double half = t > len - 1.8 ? (len - t) / 1.8 * 1.2 : 1.2;
                if (Math.abs(n) > half) continue;
                int c = n < -0.4 ? m[3] : n > 0.5 ? m[1] : m[2];
                if (Math.abs(n) < 0.3 && t > 1 && t < len - 1.5) c = m[4];
                s.set(x, y, c);
            }
        bar(s, 1.4, 5.2, 4.8, 8.6, 1.6, m[1], m[2], m[3]);
        bar(s, 0.8, 9.2, 2.6, 7.4, 1.6, HANDLE_D, HANDLE, HANDLE_L);
        return s.outlined(m[0]);
    }

    /** Bow drawn upright with the limb bulging to the right; the grip is at the middle. */
    static Sprite bow(int[] m) {
        Sprite s = new Sprite(9, 16);
        double cx = 0.5, cy = 8.0, R = 7.6;
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double r = Math.hypot(dx * 1.05, dy);
                if (dx < 0) continue;
                double d = r - R;
                if (Math.abs(d) > 1.2) continue;
                int c = d > 0.4 ? m[3] : d < -0.4 ? m[1] : m[2];
                if (Math.abs(dy) < 1.5) c = 0x5a3a20; // grip wrap
                s.set(x, y, c);
            }
        for (int y = 1; y < 15; y++) s.set(1, y, 0xd8d8e0); // string
        return s.outlined(m[0]);
    }

    static Sprite arrow(int shaft, int head) {
        Sprite s = new Sprite(12, 12);
        bar(s, 1.5, 10.5, 9.0, 3.0, 1.3, Pal.shade(shaft, 180), shaft, Pal.lerp(shaft, 0xffffff, 0.3));
        for (int y = 0; y < 5; y++)
            for (int x = 7; x < 12; x++) {
                double px = x + 0.5, py = y + 0.5;
                double t = (px - 8.2) * 0.7071 - (py - 3.8) * 0.7071;
                double n = (px - 8.2) * 0.7071 + (py - 3.8) * 0.7071;
                if (t < 0 || t > 3.2 || Math.abs(n) > (3.2 - t) * 0.6) continue;
                s.set(x, y, n < 0 ? Pal.lerp(head, 0xffffff, 0.35) : head);
            }
        s.set(0, 9, 0xe8e8f0);
        s.set(1, 8, 0xe8e8f0);
        s.set(2, 11, 0xc84040);
        s.set(3, 10, 0xc84040);
        s.set(1, 11, 0xa02828);
        return s.outlined(0x1e1410);
    }

    static Sprite bar(int[] m) {
        Map<Character, Integer> c = map('o', m[0], 'd', m[1], 'm', m[2], 'l', m[3], 'h', m[4]);
        return Sprite.ascii(c,
                "...oooooooo.",
                "..ohhhllllo.",
                ".olllllllldo",
                "ommmmmmmmddo",
                "ommmmmmmmddo",
                "odddddddddo.",
                "ooooooooooo.");
    }

    static Sprite coin(int[] m) {
        Sprite s = new Sprite(8, 8);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                double dx = x + 0.5 - 4, dy = y + 0.5 - 4;
                double d = Math.hypot(dx, dy);
                if (d > 3.9) continue;
                int c;
                if (d > 3.0) c = m[0];
                else if (dx + dy < -2.2) c = m[4];
                else if (dx + dy < 0) c = m[3];
                else if (dx + dy > 2.5) c = m[1];
                else c = m[2];
                s.set(x, y, c);
            }
        s.set(4, 3, m[1]);
        s.set(4, 4, m[1]);
        return s;
    }

    private static Sprite gel() {
        Sprite s = new Sprite(10, 8);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 10; x++) {
                double dx = (x + 0.5 - 5) / 4.6, dy = (y + 0.5 - 5.2) / 3.0;
                if (y + 0.5 < 5.2) dy = (y + 0.5 - 5.2) / 4.2;
                double d = dx * dx + dy * dy;
                if (d > 1) continue;
                int a = d > 0.7 ? 230 : 185;
                s.setA(x, y, d > 0.7 ? 0x2a60c0 : Pal.GEL, a);
            }
        s.setA(3, 2, 0xffffff, 230);
        s.setA(2, 3, Pal.GEL_L, 230);
        s.setA(4, 2, Pal.GEL_L, 200);
        return s;
    }

    private static Sprite lens() {
        Sprite s = new Sprite(8, 8);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                double d = Math.hypot(x + 0.5 - 4, y + 0.5 - 4);
                if (d > 3.8) continue;
                int c = d > 3.0 ? 0x5a6a86 : d > 1.9 ? 0xc8dcef : d > 1.0 ? 0x4a78b8 : 0x101828;
                s.set(x, y, c);
            }
        s.set(2, 2, 0xffffff);
        s.set(3, 2, 0xeaf4ff);
        return s;
    }

    private static Sprite bottle(int liquid, int liquidL) {
        Sprite s = new Sprite(8, 11);
        int glass = 0x9ac4e0, glassD = 0x5a7ea0, cork = 0x9a6a3a;
        s.set(3, 0, cork);
        s.set(4, 0, cork);
        for (int y = 1; y < 3; y++) {
            s.set(2, y, glassD);
            s.set(5, y, glassD);
            s.setA(3, y, glass, 120);
            s.setA(4, y, glass, 120);
        }
        for (int y = 3; y < 11; y++)
            for (int x = 0; x < 8; x++) {
                double dx = (x + 0.5 - 4) / 3.9, dy = (y + 0.5 - 7) / 4.0;
                double d = dx * dx + dy * dy;
                if (d > 1) continue;
                boolean edge = d > 0.62;
                if (edge) s.set(x, y, glassD);
                else if (liquid != 0 && y >= 6) s.set(x, y, y == 6 ? liquidL : liquid);
                else s.setA(x, y, glass, 110);
            }
        s.setA(2, 5, 0xffffff, 220);
        s.setA(2, 6, 0xffffff, 160);
        return s;
    }

    private static Sprite blockIcon(Sprite tex) {
        Sprite s = tex.copy();
        s.clear(0, 0);
        s.clear(7, 0);
        s.clear(0, 7);
        s.clear(7, 7);
        return s;
    }

    private static Sprite oreIcon(int dark, int mid, int light) {
        Map<Character, Integer> m = map('d', dark, 'm', mid, 'l', light, 'o', Pal.shade(dark, 140));
        return Sprite.ascii(m,
                "...oo.....",
                "..olmoo...",
                ".olmmmdo..",
                "olmmmmmdo.",
                "ommmlmmmdo",
                "ommmmmmddo",
                ".odmmmddo.",
                "..odddoo..",
                "...ooo....");
    }

    private static Sprite wallIcon(int wl, int frame) {
        Sprite s = new Sprite(8, 8);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                boolean edge = x == 0 || y == 0 || x == 7 || y == 7;
                s.set(x, y, edge ? frame : Tiles.wallTexel(wl, x + 2, y + 3));
            }
        return s;
    }

    private static Map<Character, Integer> map(Object... kv) {
        Map<Character, Integer> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((Character) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    private static Sprite shrink(Sprite s, int size) {
        Sprite o = new Sprite(size, size);
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                o.p[x + y * size] = s.get(x * s.w / size, y * s.h / size);
        o.clear(0, 0);
        o.clear(size - 1, 0);
        o.clear(0, size - 1);
        o.clear(size - 1, size - 1);
        return o;
    }

    // ---- build ----------------------------------------------------------------

    static void build() {
        Sprite[] I = Items.ICON;
        I[Items.DIRT] = blockIcon(TileArt.sample(Tiles.DIRT));
        I[Items.STONE] = blockIcon(Tiles.tex[Tiles.STONE][0]);
        I[Items.SAND] = blockIcon(TileArt.sample(Tiles.SAND));
        I[Items.CLAY] = blockIcon(TileArt.sample(Tiles.CLAY));
        I[Items.COPPER_ORE] = oreIcon(Pal.COP_D, Pal.COP, Pal.COP_L);
        I[Items.IRON_ORE] = oreIcon(Pal.IRON_D, Pal.IRON, Pal.IRON_L);
        I[Items.SILVER_ORE] = oreIcon(Pal.SILV_D, Pal.SILV, Pal.SILV_L);
        I[Items.GOLD_ORE] = oreIcon(Pal.GOLD_D, Pal.GOLD, Pal.GOLD_L);
        I[Items.GLASS] = blockIcon(glassBlock());
        I[Items.GRAY_BRICK] = blockIcon(TileArt.sample(Tiles.GRAY_BRICK));
        I[Items.RED_BRICK] = blockIcon(TileArt.sample(Tiles.RED_BRICK));

        Map<Character, Integer> wood = map('d', Pal.WOOD_D, 'w', Pal.WOOD, 'l', Pal.WOOD_L,
                'b', Pal.BARK_D, 'B', Pal.BARK, 'r', 0xc8965e);
        I[Items.WOOD] = Sprite.ascii(wood,
                "..........",
                ".bBBBBBBb.",
                "bBwwrwwwlb",
                "bwlwwdwwwb",
                "bwwwwwwlwb",
                "bwdwwlwwwb",
                "bwwwlwwdwb",
                "bBwwwwwwBb",
                ".bBBBBBBb.",
                "..........");

        Sprite torch = new Sprite(6, 10);
        for (int y = 4; y < 10; y++) {
            torch.set(2, y, Pal.WOOD_L);
            torch.set(3, y, Pal.WOOD_D);
        }
        String[] fl = {"..y...", ".yYy..", ".yWYo.", "..oo.."};
        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 6; x++) {
                char c = fl[y].charAt(x);
                int col = c == 'W' ? 0xfffbe8 : c == 'Y' ? Pal.FLAME_L : c == 'y' ? Pal.FLAME : c == 'o' ? Pal.FLAME_R : -1;
                if (col >= 0) torch.set(x, y, col);
            }
        I[Items.TORCH] = torch;

        I[Items.PLATFORM] = Sprite.ascii(wood,
                "..........",
                "..........",
                "..........",
                "llllllllll",
                "wwwdwwwwdw",
                "dddddddddd",
                ".d......d.",
                "..........");

        Map<Character, Integer> acorn = map('c', Pal.BARK_D, 'C', Pal.BARK, 'n', Pal.WOOD_L,
                'N', Pal.COP_L, 'k', Pal.WOOD);
        I[Items.ACORN] = Sprite.ascii(acorn,
                "...c...",
                ".cCCCc.",
                "cCCCCCc",
                ".kNnnk.",
                ".knnNk.",
                "..knk..",
                "...k...");

        I[Items.DIRT_WALL] = wallIcon(Tiles.W_DIRT, Pal.DIRT_L);
        I[Items.STONE_WALL] = wallIcon(Tiles.W_STONE, Pal.STONE_L);
        I[Items.WOOD_WALL] = wallIcon(Tiles.W_WOOD, Pal.WOOD_L);
        I[Items.GLASS_WALL] = wallIcon(Tiles.W_GLASS, Pal.GLASS);
        I[Items.GRAY_BRICK_WALL] = wallIcon(Tiles.W_GRAY_BRICK, Pal.BRICK_G_L);
        I[Items.RED_BRICK_WALL] = wallIcon(Tiles.W_RED_BRICK, Pal.BRICK_R_L);

        I[Items.COPPER_PICK] = pickaxe(COPPER);
        I[Items.IRON_PICK] = pickaxe(IRON);
        I[Items.SILVER_PICK] = pickaxe(SILVER);
        I[Items.GOLD_PICK] = pickaxe(GOLD);
        I[Items.COPPER_AXE] = axe(COPPER);
        I[Items.IRON_AXE] = axe(IRON);
        I[Items.SILVER_AXE] = axe(SILVER);
        I[Items.GOLD_AXE] = axe(GOLD);
        I[Items.WOOD_HAMMER] = hammer(WOODR, true);
        I[Items.COPPER_HAMMER] = hammer(COPPER, false);
        I[Items.IRON_HAMMER] = hammer(IRON, false);
        I[Items.SILVER_HAMMER] = hammer(SILVER, false);
        I[Items.GOLD_HAMMER] = hammer(GOLD, false);

        I[Items.WOOD_SWORD] = broadsword(WOODR, Pal.WOOD_D, 0x3e2614, Pal.WOOD);
        I[Items.COPPER_SHORTSWORD] = shortsword(COPPER);
        I[Items.COPPER_SWORD] = broadsword(COPPER, COPPER[1], COPPER[0], 0x6a4a3a);
        I[Items.IRON_SWORD] = broadsword(IRON, IRON[1], IRON[0], 0x5a3a2a);
        I[Items.SILVER_SWORD] = broadsword(SILVER, SILVER[1], SILVER[0], 0x3a4a7a);
        I[Items.GOLD_SWORD] = broadsword(GOLD, GOLD[1], GOLD[0], 0x7a2a2a);

        I[Items.WOOD_BOW] = bow(WOODR);
        I[Items.COPPER_BOW] = bow(COPPER);
        I[Items.IRON_BOW] = bow(IRON);
        I[Items.SILVER_BOW] = bow(SILVER);
        I[Items.GOLD_BOW] = bow(GOLD);
        I[Items.WOOD_ARROW] = arrow(Pal.WOOD, 0x9a9aa8);

        I[Items.COPPER_BAR] = bar(COPPER);
        I[Items.IRON_BAR] = bar(IRON);
        I[Items.SILVER_BAR] = bar(SILVER);
        I[Items.GOLD_BAR] = bar(GOLD);
        I[Items.COPPER_COIN] = coin(COPPER);
        I[Items.SILVER_COIN] = coin(SILVER);
        I[Items.GOLD_COIN] = coin(GOLD);
        I[Items.PLATINUM_COIN] = coin(PLATINUM);

        I[Items.GEL] = gel();
        I[Items.LENS] = lens();
        I[Items.MUSHROOM] = Tiles.tex[Tiles.MUSHROOM][0].crop(0, 2, 8, 6);
        I[Items.HEALING_POTION] = bottle(0xd02a3a, 0xff6a78);
        I[Items.BOTTLE] = bottle(0, 0);
        I[Items.LIFE_CRYSTAL] = Tiles.furn[Tiles.LIFE_CRYSTAL][0].crop(1, 1, 14, 10);

        I[Items.WORKBENCH] = Tiles.furn[Tiles.WORKBENCH][0];
        I[Items.FURNACE] = Tiles.furn[Tiles.FURNACE][0].fit(15);
        I[Items.IRON_ANVIL] = Tiles.furn[Tiles.ANVIL][0];
        buildV3();
        buildV4();

        for (int i = 1; i < Items.COUNT; i++) {
            if (I[i] == null) I[i] = new Sprite(8, 8);
            Items.SMALL[i] = I[i].w > 10 || I[i].h > 10 ? I[i].fit(10) : I[i];
        }
        // dropped blocks are small cubes
        for (int id : new int[]{Items.DIRT, Items.STONE, Items.SAND, Items.CLAY, Items.GLASS, Items.GRAY_BRICK,
                Items.RED_BRICK, Items.DIRT_WALL, Items.STONE_WALL, Items.WOOD_WALL, Items.GLASS_WALL,
                Items.GRAY_BRICK_WALL, Items.RED_BRICK_WALL, Items.MUD, Items.EBONSTONE, Items.EBONSAND, Items.ASH,
                Items.OBSIDIAN, Items.OBSIDIAN_BRICK, Items.HELLSTONE_BRICK, Items.MAHOGANY_WALL, Items.OBSIDIAN_BRICK_WALL,
                Items.HELLSTONE_BRICK_WALL, Items.SNOW_BLOCK, Items.ICE_BLOCK, Items.BOREAL_WALL}) {
            Items.SMALL[id] = shrink(I[id], 6);
        }
        for (int id : new int[]{Items.COPPER_COIN, Items.SILVER_COIN, Items.GOLD_COIN, Items.PLATINUM_COIN})
            Items.SMALL[id] = I[id];
    }

    /** Icons for the items of version 0.3. */
    private static void buildV3() {
        Sprite[] I = Items.ICON;
        I[Items.MUD] = blockIcon(TileArt.sample(Tiles.MUD));
        I[Items.EBONSTONE] = blockIcon(Tiles.tex[Tiles.EBONSTONE][0]);
        I[Items.EBONSAND] = blockIcon(TileArt.sample(Tiles.EBONSAND));
        I[Items.ASH] = blockIcon(TileArt.sample(Tiles.ASH));
        I[Items.OBSIDIAN] = blockIcon(Tiles.tex[Tiles.OBSIDIAN][0]);
        I[Items.OBSIDIAN_BRICK] = blockIcon(TileArt.sample(Tiles.OBSIDIAN_BRICK));
        I[Items.HELLSTONE_BRICK] = blockIcon(TileArt.sample(Tiles.HELLSTONE_BRICK));
        I[Items.HELLSTONE] = oreIcon(Pal.HELL_D, Pal.HELL, Pal.HELL_L);
        I[Items.DEMONITE_ORE] = oreIcon(Pal.DEMO_D, Pal.DEMO, Pal.DEMO_L);
        I[Items.DEMONITE_BAR] = bar(SHADOW);
        I[Items.HELLSTONE_BAR] = bar(MOLTEN);
        I[Items.MAHOGANY] = I[Items.WOOD].recolor(Pal.WOOD_D, Pal.MAHO_D, Pal.WOOD, Pal.MAHO, Pal.WOOD_L, Pal.MAHO_L,
                0xc8965e, 0xd07a60);
        I[Items.EBONWOOD] = I[Items.WOOD].recolor(Pal.WOOD_D, 0x3a2e4a, Pal.WOOD, 0x5a4a6a, Pal.WOOD_L, 0x7a6a8a,
                0xc8965e, 0x9a8aaa, Pal.BARK, 0x3a3048, Pal.BARK_D, 0x221a2c);
        Map<Character, Integer> g = map('g', Pal.JGRASS, 'G', Pal.JGRASS_D, 'l', Pal.JGRASS_L);
        I[Items.VINE] = Sprite.ascii(g, "..lG....", "..gG....", "...gl...", "...Gg...", "..lg....", "..gG.l..",
                "...gGg..", "...Gg...", "..lg....", "..G.....");
        I[Items.JUNGLE_SPORES] = Sprite.ascii(map('a', 0x9aff70, 'b', 0x5ac040, 'c', 0xe0ffc0),
                "..a.....", ".aca..a.", "..a..aca", "b....ba.", ".b.a....", "..aca.b.", "...a....");
        I[Items.STINGER] = Sprite.ascii(map('k', 0x1a1a14, 'y', 0xe8c030, 'w', 0xfff0a0),
                "........k", ".......k.", "......yk.", ".....yk..", "....wy...", "...yk....", "..yk.....", ".yw......",
                "kk.......");
        I[Items.ROTTEN_CHUNK] = Sprite.ascii(map('d', 0x3a2a30, 'm', 0x6a4a52, 'l', 0x8a6a6a, 'p', 0x6a5a8a),
                "..dddd..", ".dmmlmd.", "dmlmmpmd", "dmmpmmmd", "dmmmmlmd", ".dmpmmd.", "..dddd..");
        I[Items.CHEST] = Tiles.furn[Tiles.CHEST][0].fit(14);
        I[Items.GOLD_CHEST] = Tiles.furn[Tiles.GOLD_CHEST][0].fit(14);
        I[Items.DOOR] = Tiles.furn[Tiles.DOOR_CLOSED][0].fit(15);
        I[Items.TABLE] = Tiles.furn[Tiles.TABLE][0].fit(15);
        I[Items.CHAIR] = Tiles.furn[Tiles.CHAIR][0];
        I[Items.BED] = Tiles.furn[Tiles.BED][0].fit(15);
        I[Items.EMPTY_BUCKET] = bucket(0x3a3a44, 0x4a4a54);
        I[Items.WATER_BUCKET] = bucket(Pal.WATER, Pal.WATER_L);
        I[Items.LAVA_BUCKET] = bucket(Pal.LAVA, Pal.LAVA_L);
        I[Items.NIGHTMARE_PICK] = pickaxe(SHADOW);
        I[Items.WAR_AXE] = axe(SHADOW);
        I[Items.THE_BREAKER] = hammer(SHADOW, false);
        I[Items.LIGHTS_BANE] = broadsword(SHADOW, SHADOW[1], SHADOW[0], 0x4a2a5a);
        I[Items.DEMON_BOW] = bow(SHADOW);
        I[Items.MOLTEN_PICK] = pickaxe(MOLTEN);
        I[Items.FIERY_GREATSWORD] = broadsword(MOLTEN, 0x3a3a44, 0x1a1a20, 0x5a2a1a);
        I[Items.MOLTEN_FURY] = bow(MOLTEN);
        I[Items.BLADE_OF_GRASS] = broadsword(GRASSR, 0x7a5a2a, 0x3a2a14, 0x4a3a1a);
        I[Items.FLAMING_ARROW] = arrow(Pal.WOOD, 0xff8a20);
        int[][] ramps = {COPPER, IRON, SILVER, GOLD, SHADOW, MOLTEN};
        for (int k = 0; k < 6; k++) {
            I[Items.COPPER_HELMET + k * 3] = helmet(ramps[k]);
            I[Items.COPPER_MAIL + k * 3] = chainmail(ramps[k]);
            I[Items.COPPER_GREAVES + k * 3] = greaves(ramps[k]);
        }
        Sprite mh = helmet(MINING).copy();
        mh.set(9, 3, 0xffffff);
        mh.set(10, 3, 0xfff4c0);
        mh.set(9, 4, 0xfff4c0);
        I[Items.MINING_HELMET] = mh;
        I[Items.HERMES_BOOTS] = Sprite.ascii(map('o', 0x2a2a3a, 'w', 0xf0f0f8, 'b', 0x8ab0e8, 'B', 0x4a70b8, 'g', 0xe8c030),
                "......ww..", ".....wwbw.", "..oooowbw.", "..obbbow..", "..obbbo...", "..obBbo...", ".obbbbooo.",
                "obbbbbbbbo", "oBBBBBBBBo", "oooggoooo.");
        Sprite cloud = bottle(0, 0).copy();
        for (int y = 5; y < 10; y++)
            for (int x = 1; x < 7; x++) if ((x + y) % 3 != 0 && cloud.get(x, y) != 0) cloud.set(x, y, 0xf4f8ff);
        I[Items.CLOUD_BOTTLE] = cloud;
        I[Items.BAND_REGEN] = ring(0xd02a3a, 0xff8a90, 0x6a1018);
        I[Items.HORSESHOE] = Sprite.ascii(map('o', GOLD[0], 'd', GOLD[1], 'm', GOLD[2], 'l', GOLD[3]),
                ".oo....oo.", "olmo..omdo", "olmo..omdo", "olmo..omdo", "olmo..omdo", "olmdooomdo",
                ".olmmmmdo.", "..oooooo..");
        I[Items.AGLET] = Sprite.ascii(map('o', 0x3a3a44, 'm', 0xb4b4bc, 'l', 0xe8e8f0, 'w', 0xc8a078),
                ".......ww.", "......ww..", ".....ww...", "....ww....", "...oo.....", "..olo.....", ".olmo.....",
                ".omo......", "..o.......");
        I[Items.BALLOON] = Sprite.ascii(map('o', 0x6a0a10, 'r', 0xe02a30, 'l', 0xff8a8a, 's', 0xd8d8d8),
                "..oooo..", ".orlrro.", "orllrrro", "orlrrrro", "orrrrrro", ".orrrro.", "..oooo..", "...os...",
                "....s...", "...s....", "....s...");
        I[Items.ANKLET] = ring(0x4aa02a, 0xa0e070, 0x1a4a10);
        I[Items.OBSIDIAN_SKULL] = Sprite.ascii(map('o', 0x0a0612, 'd', Pal.OBS, 'm', Pal.OBS_L, 'l', 0x7a6a9a, 'k', 0x000000),
                "..oooooo..", ".olllmmdo.", "olmmmmmmdo", "omkkmmkkdo", "omkkmmkkdo", "ommmkkmmdo", ".oddmmddo.",
                "..odmdmo..", "..oooooo..");
        I[Items.FLIPPER] = Sprite.ascii(map('o', 0x0a2a4a, 'b', 0x3a8ae0, 'l', 0x8ac8ff),
                "...oo.....", "..olbo....", "..olbbo...", "..obbbbo..", ".obbbbbbo.", ".olbbbbbbo", "olbbbbbbbo",
                "obobobobo.", "o.o.o.o...");
        I[Items.MAHOGANY_WALL] = wallIcon(Tiles.W_MAHOGANY, Pal.MAHO_L);
        I[Items.OBSIDIAN_BRICK_WALL] = wallIcon(Tiles.W_OBSIDIAN_BRICK, Pal.OBS_L);
        I[Items.HELLSTONE_BRICK_WALL] = wallIcon(Tiles.W_HELLSTONE_BRICK, Pal.HELL);
    }

    // ---- version 0.4 ------------------------------------------------------------

    /** Colour ramp (outline, dark, mid, light, highlight) around a base colour. */
    static int[] ramp(int c) {
        return new int[]{Pal.shade(c, 80), Pal.shade(c, 160), c, Pal.lerp(c, 0xffffff, 0.35), Pal.lerp(c, 0xffffff, 0.75)};
    }

    private static Sprite gem(int c) {
        return ramped(ramp(c),
                "..oooo..",
                ".ohhlmo.",
                "ohllmmdo",
                "olmmmmdo",
                ".odmmdo.",
                "..oddo..",
                "...oo...");
    }

    /** A staff: a rod with a gem in a claw at the top. */
    private static Sprite staff(int[] metal, int gemColor) {
        Sprite s = new Sprite(13, 13);
        bar(s, 1.0, 12.0, 8.2, 4.8, 2.0, metal[1], metal[2], metal[3]);
        int[] g = ramp(gemColor);
        for (int y = 0; y < 13; y++)
            for (int x = 0; x < 13; x++) {
                double dx = x + 0.5 - 9.6, dy = y + 0.5 - 3.4;
                double d = Math.hypot(dx, dy);
                if (d > 2.7) continue;
                s.set(x, y, d > 2.0 ? g[1] : dx + dy < -1.2 ? g[4] : dx + dy < 0.4 ? g[3] : g[2]);
            }
        // claw around the gem
        s.set(7, 4, metal[3]);
        s.set(6, 3, metal[2]);
        s.set(9, 7, metal[1]);
        s.set(10, 7, metal[2]);
        s.set(8, 6, metal[2]);
        return s.outlined(metal[0]);
    }

    private static Sprite starIcon(int c, int light, int outline) {
        Sprite s = new Sprite(11, 11);
        for (int y = 0; y < 11; y++)
            for (int x = 0; x < 11; x++) {
                double dx = x + 0.5 - 5.5, dy = y + 0.5 - 5.8;
                double d = Math.hypot(dx, dy);
                double a = Math.atan2(dy, dx) + Math.PI / 2;
                double edge = 5.2 * (0.42 + 0.58 * Math.pow(Math.abs(Math.cos(a * 2.5)), 2.2));
                if (d > edge) continue;
                s.set(x, y, d < 1.6 ? 0xffffff : dx + dy < -0.5 ? light : c);
            }
        return s.outlined(outline);
    }

    private static void buildV4() {
        Sprite[] I = Items.ICON;
        int[] gems = {Pal.RUBY, Pal.SAPPHIRE, Pal.EMERALD, Pal.TOPAZ};
        for (int k = 0; k < 4; k++) I[Items.RUBY + k] = gem(gems[k]);
        I[Items.FLAME_STAFF] = staff(COPPER, Pal.RUBY);
        I[Items.TIDE_STAFF] = staff(IRON, Pal.SAPPHIRE);
        I[Items.QUAKE_STAFF] = staff(SILVER, Pal.EMERALD);
        I[Items.STORM_STAFF] = staff(GOLD, Pal.TOPAZ);
        I[Items.FALLEN_STAR] = starIcon(0xffd84a, 0xfff6b0, 0x8a5a10);
        I[Items.MANA_CRYSTAL] = starIcon(Pal.MANA, Pal.MANA_L, Pal.MANA_D);
        I[Items.MANA_POTION] = bottle(0x2a5ae0, 0x7aa8ff);
        Sprite band = ring(0x3a6af0, 0x9ac0ff, 0x1a2a7a).copy();
        band.set(4, 1, 0xffffff);
        band.set(5, 1, 0xfff07a);
        band.set(4, 0, 0xfff07a);
        I[Items.BAND_STARPOWER] = band;
        I[Items.ARCANE_HAT] = Sprite.ascii(map('o', ARCANE[0], 'd', ARCANE[1], 'm', ARCANE[2], 'l', ARCANE[3],
                'h', ARCANE[4], 'y', 0xe8c030),
                "........oo..",
                ".......ohdo.",
                "......ohmo..",
                ".....olmdo..",
                "....olmmdo..",
                "...olmmmdo..",
                "...oyyyyyo..",
                "..olmmmmmdo.",
                ".olmmmmmmmdo",
                "oooooooooooo");
        Sprite mirror = Sprite.ascii(map('o', 0x3e4452, 'f', SILVER[2], 'F', SILVER[3], 'g', 0x9ad8f0, 'G', 0xe0f8ff,
                'd', 0x5a8ab0, 'h', HANDLE),
                "..oooo..",
                ".ofFFfo.",
                "ofGGgdfo",
                "oFGgggfo",
                "ofggddfo",
                "ofgdddfo",
                ".ofddfo.",
                "..offo..",
                "...oho..",
                "...oho..",
                "...oho..",
                "....o...");
        I[Items.MAGIC_MIRROR] = mirror;
        I[Items.SUSPICIOUS_EYE] = Sprite.ascii(map('o', 0x5a1a1a, 'w', 0xece4e0, 'v', 0xd06060, 'b', 0x3a6ab0,
                'k', 0x101018, 'W', 0xffffff),
                "...oooo...",
                ".oowwwvoo.",
                "owvwbbbwwo",
                "owwbkkbbvo",
                "ovwbkWbbwo",
                "owwbbbbwwo",
                ".oowvwwoo.",
                "...oooo...");
        I[Items.UNHOLY_ARROW] = arrow(0x5a4a7a, 0xb070ff);
        Sprite shield = Sprite.ascii(map('o', 0x2a1a10, 'r', 0x8a5a30, 'R', 0xb07c48, 'w', 0xece4e0, 'b', 0xa02828,
                'k', 0x101018, 'i', 0x3a6ab0),
                "..ooooooo..",
                ".oRRRRRRro.",
                "oRrrrrrrrro",
                "oRrwwwwwrro",
                "oRwwiiiwwro",
                "oRwikkkiwro",
                "oRwwiiiwwro",
                "oRrwwbwwrro",
                ".oRrrrrrro.",
                "..oRrrrro..",
                "...oRrro...",
                "....ooo....");
        I[Items.EYE_SHIELD] = shield;
        I[Items.SNOW_BLOCK] = blockIcon(TileArt.sample(Tiles.SNOW));
        I[Items.ICE_BLOCK] = blockIcon(TileArt.sample(Tiles.ICE));
        I[Items.BOREAL_WOOD] = I[Items.WOOD].recolor(Pal.WOOD_D, Pal.BOREAL_D, Pal.WOOD, Pal.BOREAL, Pal.WOOD_L, Pal.BOREAL_L,
                0xc8965e, 0xc8b490, Pal.BARK, 0x5a4c3c, Pal.BARK_D, 0x3a3026);
        I[Items.BOREAL_WALL] = wallIcon(Tiles.W_BOREAL, Pal.BOREAL_L);
        I[Items.ICE_BLADE] = broadsword(ICER, 0x3a6ab0, 0x1a2a5a, 0x2a3a6a);
        I[Items.ICE_SKATES] = Sprite.ascii(map('o', 0x2a2a3a, 'b', 0x8a5a30, 'B', 0xb07c48, 'l', 0xf0f0f8, 's', 0xc8d8e8,
                'S', 0xffffff),
                "..oooo....",
                "..obBo....",
                "..obBo....",
                "..obBbo...",
                "..obbBoo..",
                ".obbbbbbo.",
                "oBbbbbbbbo",
                "oooooooooo",
                "...l...l..",
                "SssssssssS");
        Sprite bliz = bottle(0, 0).copy();
        for (int y = 4; y < 10; y++)
            for (int x = 1; x < 7; x++)
                if (bliz.get(x, y) != 0 && (x * 2 + y) % 4 != 0) bliz.set(x, y, (x + y) % 3 == 0 ? 0xffffff : 0xb8dcf8);
        I[Items.BLIZZARD_BOTTLE] = bliz;
        I[Items.ICE_CHEST] = Tiles.furn[Tiles.ICE_CHEST][0].fit(14);
        I[Items.GRASS_SEEDS] = seedBag(0xc8a878, Pal.GRASS, Pal.GRASS_L);
        I[Items.JUNGLE_SEEDS] = seedBag(0xb89868, Pal.JGRASS, Pal.JGRASS_L);
        I[Items.PURIFICATION_POWDER] = Sprite.ascii(map('o', 0x3a3a4a, 'p', 0xe8e0d0, 'P', 0xffffff, 'd', 0xb8b0a0,
                'c', 0x6ad8ff, 'r', 0x8a6a4a),
                "c......c.",
                "...rr....",
                "..orro.c.",
                ".oppPpo..",
                "oppPpppo.",
                "opPppdpo.",
                "oppppddo.",
                ".oddddo..",
                "..oooo...");
        I[Items.HEART] = Sprite.ascii(map('X', Pal.UI_HEART_D, 'h', Pal.UI_HEART_L, 'l', Pal.UI_HEART, 'm', 0xb82838),
                ".XX.XX.",
                "XhlXllX",
                "XllllmX",
                ".XlllX.",
                "..XmX..",
                "...X...");
        I[Items.MANA_STAR] = starIcon(Pal.MANA, Pal.MANA_L, Pal.MANA_D).fit(9);
    }

    private static Sprite seedBag(int bag, int leaf, int leafL) {
        return Sprite.ascii(map('o', Pal.shade(bag, 110), 'b', bag, 'B', Pal.lerp(bag, 0xffffff, 0.3), 'd', Pal.shade(bag, 170),
                'g', leaf, 'G', leafL, 'k', 0x4a3a20),
                "...G.g...",
                "..gGg....",
                "...okko..",
                "..obbbo..",
                ".oBbbbdo.",
                "oBbbkbbdo",
                "oBbbbbbdo",
                "obbkbbbdo",
                ".odddddo.",
                "..ooooo..");
    }

    private static Sprite ring(int c, int l, int d) {
        Sprite s = new Sprite(10, 10);
        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 10; x++) {
                double dx = (x + 0.5 - 5) / 4.6, dy = (y + 0.5 - 5) / 3.4;
                double r = Math.hypot(dx, dy);
                if (r > 1 || r < 0.55) continue;
                s.set(x, y, r > 0.9 ? d : dy < 0 && dx < 0.3 ? l : c);
            }
        s.set(4, 1, 0xffffff);
        return s;
    }

    private static Sprite glassBlock() {
        Sprite s = new Sprite(8, 8);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                int c = (x == 0 || y == 0 || x == 7 || y == 7) ? 0x6a9ab8 : 0xb8dcf0;
                if (x + y == 5 || x + y == 6) c = 0xf0faff;
                s.set(x, y, c);
            }
        return s;
    }

    /** Coin image for an animation frame (spinning): frame 0..3. */
    static Sprite coinFrame(int item, int frame) {
        Sprite c = Items.ICON[item];
        int f = frame & 3;
        if (f == 0) return c;
        int w = f == 2 ? 2 : 5;
        Sprite s = new Sprite(8, 8);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < w; x++) s.p[(4 - w / 2 + x) + y * 8] = c.get(x * 8 / w, y);
        return s;
    }
}
