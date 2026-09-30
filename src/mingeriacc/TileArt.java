package mingeriacc;

import java.util.Map;
import java.util.Random;

/** Tile, wall and furniture textures, all generated in code. */
final class TileArt {
    private TileArt() {}

    private static final int T = Tiles.T;

    // ---- helpers --------------------------------------------------------

    /** Tileable value noise, values 0..1. cells must divide size. */
    static double[] tnoise(Random r, int size, int cells, int octaves) {
        double[] out = new double[size * size];
        double amp = 1;
        int c = cells;
        for (int o = 0; o < octaves && c <= size; o++) {
            double[] lat = new double[c * c];
            for (int i = 0; i < lat.length; i++) lat[i] = r.nextDouble();
            for (int y = 0; y < size; y++) {
                double fy = y * c / (double) size;
                int iy = (int) fy;
                double ty = smooth(fy - iy);
                int iy1 = (iy + 1) % c;
                for (int x = 0; x < size; x++) {
                    double fx = x * c / (double) size;
                    int ix = (int) fx;
                    double tx = smooth(fx - ix);
                    int ix1 = (ix + 1) % c;
                    double a = lat[ix + iy * c], b = lat[ix1 + iy * c];
                    double d = lat[ix + iy1 * c], e = lat[ix1 + iy1 * c];
                    double v = (a + (b - a) * tx) * (1 - ty) + (d + (e - d) * tx) * ty;
                    out[x + y * size] += v * amp;
                }
            }
            amp *= 0.5;
            c *= 2;
        }
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double v : out) { min = Math.min(min, v); max = Math.max(max, v); }
        for (int i = 0; i < out.length; i++) out[i] = (out[i] - min) / Math.max(1e-9, max - min);
        return out;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static Sprite darker(Sprite s, double k) {
        Sprite o = new Sprite(s.w, s.h);
        for (int i = 0; i < s.p.length; i++)
            if (s.p[i] != 0) o.p[i] = (s.p[i] & 0xff000000) | Pal.shade(s.p[i], (int) (256 * k));
        return o;
    }

    // ---- build -----------------------------------------------------------

    static void build() {
        Random r = new Random(1234);

        Tiles.macro[Tiles.DIRT] = dirtMacro(r);
        Tiles.macro[Tiles.GRASS] = Tiles.macro[Tiles.DIRT];
        Tiles.macro[Tiles.SAND] = sandMacro(r);
        Tiles.macro[Tiles.CLAY] = clayMacro(r);
        Tiles.macro[Tiles.PLANKS] = planksMacro(r, Pal.WOOD_D, Pal.WOOD, Pal.WOOD_L, 0x4a2e18);
        Tiles.macro[Tiles.GRAY_BRICK] = brickMacro(r, 0x34343e, Pal.BRICK_G_D, Pal.BRICK_G, Pal.BRICK_G_L);
        Tiles.macro[Tiles.RED_BRICK] = brickMacro(r, 0x4a2a24, Pal.BRICK_R_D, Pal.BRICK_R, Pal.BRICK_R_L);

        Tiles.tex[Tiles.STONE] = new Sprite[4];
        for (int i = 0; i < 4; i++) Tiles.tex[Tiles.STONE][i] = stoneTex(r, Pal.STONE, Pal.STONE_D, Pal.STONE_L);
        Tiles.tex[Tiles.COPPER] = oreTex(r, Pal.COP_D, Pal.COP, Pal.COP_L);
        Tiles.tex[Tiles.IRON] = oreTex(r, Pal.IRON_D, Pal.IRON, Pal.IRON_L);
        Tiles.tex[Tiles.SILVER] = oreTex(r, Pal.SILV_D, Pal.SILV, Pal.SILV_L);
        Tiles.tex[Tiles.GOLD] = oreTex(r, Pal.GOLD_D, Pal.GOLD, Pal.GOLD_L);
        Tiles.tex[Tiles.GLASS] = glassTex();

        trunk(r);
        Tiles.tex[Tiles.LEAVES] = leaves(r, Pal.LEAF_D, Pal.LEAF, Pal.LEAF_L, Pal.LEAF_HL);
        torch();
        platform();
        plants(r);

        Tiles.furn[Tiles.WORKBENCH] = new Sprite[]{workbench()};
        furnace(r);
        Tiles.furn[Tiles.ANVIL] = new Sprite[]{anvil()};
        lifeCrystal();
        Tiles.furn[Tiles.POT] = new Sprite[]{pot(r, Pal.CLAY_D, Pal.CLAY, Pal.CLAY_L), pot(r, 0x5a4a3a, 0x7a6a56, 0x9a8a74),
                pot(r, 0x4a4a6a, 0x6a6a8a, 0x8e8eaa)};

        walls(r);
        cracks();
        caves(r);
        buildV3(r);
    }

    // ---- version 0.3: biomes and furniture ------------------------------------

    /** A noise texture from a ramp of colours (dark to light), 64 x 64. */
    private static Sprite noiseMacro(Random r, int... ramp) {
        int n = 64;
        double[] a = tnoise(r, n, 8, 3), b = tnoise(r, n, 16, 2);
        Sprite s = new Sprite(n, n);
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double v = a[x + y * n] * 0.65 + b[x + y * n] * 0.35 + (r.nextDouble() - 0.5) * 0.08;
                int i = v < 0.3 ? 0 : v < 0.68 ? 1 : 2;
                s.set(x, y, ramp[Math.min(ramp.length - 1, i + (ramp.length > 3 ? 1 : 0))]);
            }
        for (int k = 0; k < 80; k++) s.set(r.nextInt(n), r.nextInt(n), ramp[0]);
        return s;
    }

    private static Sprite[] recolorAll(Sprite[] src, int... pairs) {
        Sprite[] o = new Sprite[src.length];
        for (int i = 0; i < src.length; i++) o[i] = src[i].recolor(pairs);
        return o;
    }

    private static void buildV3(Random r) {
        Tiles.macro[Tiles.MUD] = noiseMacro(r, 0x2c1e1e, Pal.MUD_D, Pal.MUD, Pal.MUD_L);
        Tiles.macro[Tiles.JUNGLE_GRASS] = Tiles.macro[Tiles.MUD];
        Tiles.macro[Tiles.CORRUPT_GRASS] = Tiles.macro[Tiles.DIRT];
        Tiles.macro[Tiles.ASH] = noiseMacro(r, 0x1e1a1a, Pal.ASH_D, Pal.ASH, Pal.ASH_L);
        Tiles.macro[Tiles.EBONSAND] = Tiles.macro[Tiles.SAND].recolor(Pal.SAND_D, 0x4a4260, Pal.SAND, 0x6a6280,
                Pal.SAND_L, 0x8a82a0);
        Tiles.macro[Tiles.MAHOGANY_PLANKS] = planksMacro(r, Pal.MAHO_D, Pal.MAHO, Pal.MAHO_L, 0x3a1812);
        Tiles.macro[Tiles.EBON_PLANKS] = planksMacro(r, 0x3a2e4a, 0x5a4a6a, 0x7a6a8a, 0x221a2c);
        Tiles.macro[Tiles.OBSIDIAN_BRICK] = brickMacro(r, 0x06040a, Pal.OBS_D, Pal.OBS, Pal.OBS_L);
        Tiles.macro[Tiles.HELLSTONE_BRICK] = brickMacro(r, 0x1a0804, 0x4a1206, Pal.HELL_D, 0x8a2a10);

        Tiles.tex[Tiles.EBONSTONE] = new Sprite[4];
        for (int i = 0; i < 4; i++) Tiles.tex[Tiles.EBONSTONE][i] = stoneTex(r, Pal.EBON, Pal.EBON_D, Pal.EBON_L);
        Tiles.tex[Tiles.DEMONITE] = oreTex(r, Pal.DEMO_D, Pal.DEMO, Pal.DEMO_L, Pal.EBON, Pal.EBON_D, Pal.EBON_L);
        Tiles.tex[Tiles.HELLSTONE] = oreTex(r, Pal.HELL_D, Pal.HELL, Pal.HELL_L, 0x3a2622, 0x241614, 0x4e3630);
        Tiles.tex[Tiles.OBSIDIAN] = new Sprite[4];
        for (int i = 0; i < 4; i++) {
            Sprite s = stoneTex(r, Pal.OBS, Pal.OBS_D, Pal.OBS_L);
            s.set(r.nextInt(8), r.nextInt(8), 0x9a8ac8);
            Tiles.tex[Tiles.OBSIDIAN][i] = s;
        }

        Sprite[] trunk = Tiles.tex[Tiles.TRUNK];
        int[] mahoPairs = {Pal.BARK_D, 0x3e1a14, Pal.BARK, Pal.MAHO_D, Pal.BARK_L, Pal.MAHO, 0x5e422a, 0x6e3226};
        int[] ebonPairs = {Pal.BARK_D, 0x221a2c, Pal.BARK, 0x3a3048, Pal.BARK_L, 0x564866, 0x5e422a, 0x2e2638};
        int mid = Pal.lerp(Pal.BARK, Pal.BARK_D, 0.5);
        mahoPairs[6] = mid;
        ebonPairs[6] = mid;
        Tiles.tex[Tiles.MAHOGANY_TRUNK] = recolorAll(trunk, mahoPairs);
        Tiles.tex[Tiles.EBON_TRUNK] = recolorAll(trunk, ebonPairs);
        Tiles.trunkBaseMaho = recolorAll(Tiles.trunkBase, mahoPairs);
        Tiles.trunkBaseEbon = recolorAll(Tiles.trunkBase, ebonPairs);
        Tiles.tex[Tiles.MAHOGANY_LEAVES] = leaves(r, 0x2a5a14, Pal.JGRASS_D, Pal.JGRASS, Pal.JGRASS_L);
        Tiles.tex[Tiles.EBON_LEAVES] = leaves(r, 0x2e2448, Pal.CGRASS_D, Pal.CGRASS, Pal.CGRASS_L);

        String[][] fronds = {
            {"...L....", "..LGL...", ".LG.GL..", "..G.G...", "L.G.G.L.", "GLG.GLG.", ".GGgGG..", "gGGgGGg."},
            {"......L.", ".L...LG.", ".GL.LG..", "..GLG...", "L..G..L.", "GL.G.LG.", ".GgGgG..", "gGgGGGg."},
            {"........", "..L..L..", ".LGLLGL.", "..GGGG..", ".L.GG.L.", ".GLGGLG.", "..gGGg..", ".gGgGgG."},
            {".L......", "LGL...L.", ".G...LGL", ".GL...G.", "..G.LG..", "..GLG...", ".gGGg...", "gGgGGg.."},
        };
        Tiles.tex[Tiles.JUNGLE_PLANT] = new Sprite[4];
        for (int i = 0; i < 4; i++) Tiles.tex[Tiles.JUNGLE_PLANT][i] = ascii8(fronds[i], Pal.JGRASS_L, Pal.JGRASS, Pal.JGRASS_D, 0);
        Tiles.tex[Tiles.CORRUPT_PLANT] = new Sprite[4];
        String[][] thorns = {
            {"........", "........", "....L...", "..L.G...", "..G.GL..", ".GgGG...", "..gGgG..", ".gGgGGg."},
            {"........", "........", "........", ".L....L.", ".G..L.G.", ".gG.GgG.", "..GgGg..", ".gGGgGg."},
            {"........", "...L....", "...G.L..", "L..G.G..", "G.gGgG..", ".GgG.G.L", "..gGgGGg", ".gGgGgG."},
            {"........", "........", "......L.", ".L....G.", ".GL..gG.", "..G.gG..", ".gGGg...", "gGgGGgG."},
        };
        for (int i = 0; i < 4; i++) Tiles.tex[Tiles.CORRUPT_PLANT][i] = ascii8(thorns[i], Pal.CGRASS_L, Pal.CGRASS, Pal.CGRASS_D, 0);
        Tiles.tex[Tiles.VINE] = new Sprite[4];
        for (int i = 0; i < 4; i++) {
            Sprite s = new Sprite(T, T);
            int x = 3 + (i & 1);
            for (int y = 0; y < T; y++) {
                int xx = x + (int) Math.round(Math.sin((y + i * 2) * 0.8) * 0.8);
                s.set(xx, y, (y + i) % 3 == 0 ? Pal.JGRASS_L : Pal.JGRASS_D);
                if ((y + i) % 4 == 1) s.set(xx + 1, y, Pal.JGRASS);
                if ((y + i) % 4 == 3) s.set(xx - 1, y, Pal.JGRASS);
            }
            Tiles.tex[Tiles.VINE][i] = s;
        }
        Tiles.tex[Tiles.JUNGLE_SPORE] = new Sprite[]{Sprite.ascii(map('g', Pal.JGRASS_D, 'G', Pal.JGRASS, 'a', 0x9aff70, 'w', 0xe0ffc0),
                "........", "........", "...a....", "..awa...", "...a.a..", "...G.wa.", "..gG.G..", "...GG...")};

        // furniture
        Map2 wood = new Map2('k', 0x2a1a0e, 'd', Pal.WOOD_D, 'w', Pal.WOOD, 'l', Pal.WOOD_L, 'i', 0x5a5a66,
                'I', 0x9a9aa8, 'y', Pal.GOLD);
        String[] chest = {
            "................",
            "..kkkkkkkkkkkk..",
            ".klllllllllllldk",
            ".kIwwwwwwwwwwIdk",
            ".kIwwwwwwwwwwIdk",
            ".kIddddddddddIdk",
            ".kkkkkkyykkkkkkk",
            ".kIlllkIIklllIdk",
            ".kIwwwkyykwwwIdk",
            ".kIwwwwwwwwwwIdk",
            ".kIwwwwwwwwwwIdk",
            ".kIwwwwwwwwwwIdk",
            ".kIddddddddddIdk",
            ".kiiiiiiiiiiiiik",
            ".kkkkkkkkkkkkkkk",
            "................",
        };
        Tiles.furn[Tiles.CHEST] = new Sprite[]{wood.ascii(chest)};
        Map2 gold = new Map2('k', 0x4e3806, 'd', 0xa07818, 'w', 0xdcb42e, 'l', 0xf6dc6c, 'i', 0x7a5a10,
                'I', 0xfff0a0, 'y', 0x6a4a3a);
        Tiles.furn[Tiles.GOLD_CHEST] = new Sprite[]{gold.ascii(chest)};
        String[] door = {
            "kkkkkkkk", "kllllldk", "klwwwwdk", "klwddwdk", "klwddwdk", "klwwwwdk", "klwwwwdk", "klwwwwdk",
            "kldddddk", "klwwwwdk", "klwwwwdk", "klwwwyIk", "klwwwwdk", "klwddwdk", "klwddwdk", "klwwwwdk",
            "klwwwwdk", "kldddddk", "klwwwwdk", "klwddwdk", "klwddwdk", "klwwwwdk", "klddddkk", "kkkkkkkk",
        };
        Tiles.furn[Tiles.DOOR_CLOSED] = new Sprite[]{wood.ascii(door)};
        String[] open = new String[24];
        for (int y = 0; y < 24; y++) {
            String edge = y == 0 || y == 23 ? "kk" : "kd";
            open[y] = edge + "..............";
        }
        Sprite op = wood.ascii(open);
        for (int y = 1; y < 23; y++) op.set(1, y, y % 6 == 0 ? Pal.WOOD_D : Pal.WOOD_L);
        Tiles.furn[Tiles.DOOR_OPEN] = new Sprite[]{op};
        Tiles.furn[Tiles.TABLE] = new Sprite[]{wood.ascii(
                "........................",
                "........................",
                "........................",
                "........................",
                "........................",
                "........................",
                "........................",
                "........................",
                "kkkkkkkkkkkkkkkkkkkkkkkk",
                "llllllllllllllllllllllll",
                "wwwwwwwdwwwwwwwwwdwwwwww",
                "kddddddddddddddddddddddk",
                ".kwdk..............kwdk.",
                ".kwdk..............kwdk.",
                ".kwdk..............kwdk.",
                ".kkkk..............kkkk.")};
        Tiles.furn[Tiles.CHAIR] = new Sprite[]{wood.ascii(
                ".kk.....", ".klk....", ".kwk....", ".kwk....", ".klk....", ".kwk....", ".kwk....", ".kwk....",
                ".kwkkkkk", ".klllllk", ".kddddwk", ".kwk.kwk", ".kwk.kwk", ".kwk.kwk", ".kwk.kwk", ".kk..kk.")};
        Map2 bed = new Map2('k', 0x2a1a0e, 'd', Pal.WOOD_D, 'w', Pal.WOOD, 'l', Pal.WOOD_L, 'r', 0xb83a3a,
                'R', 0xe06060, 'q', 0x8a2626, 'p', 0xf0f0f8, 'P', 0xc8c8d8);
        Tiles.furn[Tiles.BED] = new Sprite[]{bed.ascii(
                "kkk.............................",
                "klk.............................",
                "kwk.............................",
                "kwk.............................",
                "kwk.............................",
                "kwkppppP........................",
                "kwkpppPPkkkkkkkkkkkkkkkkkkkkkkk.",
                "kwkPPPPkRRRRRRRRRRRRRRRRRRRRRRRk",
                "kwkkkkkkrRrrrrrRrrrrrrRrrrrrrrrk",
                "kwlllllllllllllllllllllllllllllk",
                "kwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwk",
                "kddddddddddddddddddddddddddddddk",
                "kwk..........................kwk",
                "kwk..........................kwk",
                "kwk..........................kwk",
                "kkk..........................kkk")};

        // walls
        Tiles.wallTex[Tiles.W_MUD_N] = new Sprite[]{roughWall(r, 0x1c1212, 0x342424, 0x100a0a)};
        Tiles.wallTex[Tiles.W_EBON_N] = new Sprite[]{roughWall(r, 0x1a1424, 0x30283e, 0x0e0a14)};
        Sprite mw = planksMacro(r, 0x3e1a14, 0x5a2a20, 0x6e3428, 0x2a100c);
        Sprite mw2 = new Sprite(16, 16);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) mw2.p[x + y * 16] = mw.p[y + x * 16]; // vertical boards
        Tiles.wallTex[Tiles.W_MAHOGANY] = new Sprite[]{mw2};
        Tiles.wallTex[Tiles.W_OBSIDIAN_BRICK] = new Sprite[]{tile16(brickMacro(r, 0x040208, 0x0e0a16, 0x1a1428, 0x26203a))};
        Tiles.wallTex[Tiles.W_HELLSTONE_BRICK] = new Sprite[]{tile16(brickMacro(r, 0x140604, 0x2a0c06, 0x401208, 0x561a0c))};

        // cave backdrops for the jungle and the corruption
        Tiles.caveBack[2] = Tiles.caveBack[0].recolor();
        Tiles.caveBack[3] = Tiles.caveBack[1].recolor();
        for (int i = 0; i < 64 * 64; i++) {
            int a = Tiles.caveBack[0].p[i], b = Tiles.caveBack[1].p[i];
            Tiles.caveBack[2].p[i] = 0xff000000 | Pal.mul(a, 230, 250, 180);
            Tiles.caveBack[3].p[i] = 0xff000000 | Pal.mul(b, 225, 195, 256);
        }
    }

    /** Small helper for ASCII images with a colour map. */
    private static final class Map2 {
        final Map<Character, Integer> m;

        Map2(Object... kv) {
            m = map(kv);
        }

        Sprite ascii(String... rows) {
            return Sprite.ascii(m, rows);
        }
    }


    // ---- ground ----------------------------------------------------------

    private static Sprite dirtMacro(Random r) {
        int n = 64;
        double[] a = tnoise(r, n, 8, 3), b = tnoise(r, n, 16, 2);
        Sprite s = new Sprite(n, n);
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double v = a[x + y * n] * 0.65 + b[x + y * n] * 0.35 + (r.nextDouble() - 0.5) * 0.08;
                int c = v < 0.3 ? Pal.DIRT_D : v < 0.68 ? Pal.DIRT : Pal.DIRT_L;
                s.set(x, y, c);
            }
        // small stones and specks
        for (int k = 0; k < 26; k++) {
            int x = r.nextInt(n), y = r.nextInt(n);
            s.set(x, y, 0x9a7a60);
            s.set((x + 1) % n, y, 0x86664e);
            s.set(x, (y + 1) % n, Pal.DIRT_DD);
            s.set((x + 1) % n, (y + 1) % n, Pal.DIRT_DD);
        }
        for (int k = 0; k < 90; k++) s.set(r.nextInt(n), r.nextInt(n), Pal.DIRT_DD);
        return s;
    }

    private static Sprite sandMacro(Random r) {
        int n = 32;
        double[] a = tnoise(r, n, 4, 2);
        Sprite s = new Sprite(n, n);
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double w = Math.sin((y + a[x + y * n] * 5) * Math.PI * 2 / 8.0);
                int c = w > 0.75 ? Pal.SAND_L : w < -0.8 ? Pal.SAND_D : Pal.SAND;
                s.set(x, y, c);
            }
        for (int k = 0; k < 30; k++) s.set(r.nextInt(n), r.nextInt(n), r.nextBoolean() ? Pal.SAND_D : Pal.SAND_L);
        return s;
    }

    private static Sprite clayMacro(Random r) {
        int n = 32;
        double[] a = tnoise(r, n, 4, 2);
        Sprite s = new Sprite(n, n);
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double band = Math.sin((y + a[x + y * n] * 6) * Math.PI * 2 / 16.0);
                int c = band > 0.55 ? Pal.CLAY_L : band < -0.6 ? Pal.CLAY_D : Pal.CLAY;
                s.set(x, y, c);
            }
        for (int k = 0; k < 20; k++) s.set(r.nextInt(n), r.nextInt(n), Pal.CLAY_D);
        return s;
    }

    static Sprite planksMacro(Random r, int dark, int mid, int light, int gap) {
        int n = 16;
        Sprite s = new Sprite(n, n);
        for (int band = 0; band < 4; band++) {
            int seam = (band * 7 + 3) % n;
            int seam2 = (seam + 8 + (band & 1) * 3) % n;
            for (int yy = 0; yy < 4; yy++) {
                int y = band * 4 + yy;
                for (int x = 0; x < n; x++) {
                    int c = yy == 0 ? light : yy == 3 ? gap : mid;
                    if (yy == 2 && r.nextInt(5) == 0) c = dark;
                    if (yy == 1 && r.nextInt(9) == 0) c = Pal.lerp(mid, light, 0.5);
                    if ((x == seam || x == seam2) && yy != 3) c = gap;
                    if ((x == (seam + 1) % n || x == (seam2 + 1) % n) && yy == 1) c = light;
                    s.set(x, y, c);
                }
            }
        }
        return s;
    }

    static Sprite brickMacro(Random r, int mortar, int dark, int mid, int light) {
        Sprite s = new Sprite(16, 8);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 16; x++) {
                int row = y / 4, yy = y % 4;
                int off = row == 0 ? 0 : 4;
                int bx = Math.floorMod(x - off, 8);
                int c;
                if (yy == 3 || bx == 7) c = mortar;
                else if (yy == 0 || bx == 0) c = light;
                else if (yy == 2 || bx == 6) c = dark;
                else c = mid;
                if (c == mid && r.nextInt(7) == 0) c = Pal.lerp(mid, dark, 0.5);
                s.set(x, y, c);
            }
        return s;
    }

    static Sprite stoneTex(Random r, int base, int dark, int light) {
        Sprite s = new Sprite(T, T);
        for (int y = 0; y < T; y++)
            for (int x = 0; x < T; x++) {
                double v = r.nextDouble();
                s.set(x, y, v < 0.1 ? dark : v > 0.9 ? light : base);
            }
        // shadow and highlight edges of a boulder
        int bx = r.nextInt(3), by = r.nextInt(3), bw = 3 + r.nextInt(3), bh = 3 + r.nextInt(2);
        for (int x = bx; x < bx + bw; x++) {
            s.set(x, by, light);
            s.set(x, by + bh, dark);
        }
        for (int y = by; y <= by + bh; y++) s.set(bx + bw, y, dark);
        return s;
    }

    static Sprite[] oreTex(Random r, int dark, int mid, int light) {
        return oreTex(r, dark, mid, light, Pal.STONE, Pal.STONE_D, Pal.STONE_L);
    }

    static Sprite[] oreTex(Random r, int dark, int mid, int light, int sBase, int sDark, int sLight) {
        Sprite[] out = new Sprite[4];
        for (int i = 0; i < 4; i++) {
            Sprite s = stoneTex(r, sBase, sDark, sLight);
            int n = 3 + r.nextInt(2);
            for (int k = 0; k < n; k++) {
                int x = r.nextInt(6), y = r.nextInt(6);
                s.set(x, y, mid);
                s.set(x + 1, y, light);
                s.set(x, y + 1, dark);
                s.set(x + 1, y + 1, mid);
                if (r.nextBoolean()) s.set(x + 2, y + 1, dark);
            }
            out[i] = s;
        }
        return out;
    }

    private static Sprite[] glassTex() {
        Sprite[] out = new Sprite[4];
        for (int i = 0; i < 4; i++) {
            Sprite s = new Sprite(T, T);
            for (int y = 0; y < T; y++)
                for (int x = 0; x < T; x++) s.setA(x, y, Pal.GLASS, 60);
            int d = i * 2;
            for (int k = 0; k < T; k++) {
                int x = k, y = (T - 1 - k + d) % T;
                if (y < T - 1 - k + d - 0) s.setA(x, y, Pal.GLASS_L, 130);
            }
            s.setA(1, 1, Pal.GLASS_L, 200);
            s.setA(2, 1, Pal.GLASS_L, 150);
            s.setA(1, 2, Pal.GLASS_L, 150);
            out[i] = s;
        }
        return out;
    }

    // ---- trees and plants --------------------------------------------------

    private static void trunk(Random r) {
        Tiles.tex[Tiles.TRUNK] = new Sprite[4];
        Tiles.trunkBase = new Sprite[4];
        for (int i = 0; i < 4; i++) {
            Sprite s = new Sprite(T, T);
            for (int y = 0; y < T; y++)
                for (int x = 1; x < 7; x++) {
                    int c = Pal.BARK;
                    if (x == 1) c = Pal.BARK_D;
                    else if (x == 6) c = Pal.BARK_D;
                    else if (x == 2) c = Pal.BARK_L;
                    else if (x == 5) c = Pal.lerp(Pal.BARK, Pal.BARK_D, 0.5);
                    if ((x == 3 || x == 4) && ((y + i * 3) % 5 == 0 || (x == 4 && (y + i * 2) % 7 == 3))) c = Pal.BARK_D;
                    s.set(x, y, c);
                }
            Tiles.tex[Tiles.TRUNK][i] = s;
            Sprite b = s.copy();
            // roots
            b.set(0, 6, Pal.BARK_D);
            b.set(0, 7, Pal.BARK);
            b.set(7, 6, Pal.BARK_D);
            b.set(7, 7, Pal.BARK_D);
            b.set(1, 7, Pal.BARK_L);
            Tiles.trunkBase[i] = b;
        }
    }

    static Sprite[] leaves(Random r, int d, int m, int l, int hl) {
        Sprite[] out = new Sprite[4];
        double[] n = tnoise(r, 16, 4, 2);
        for (int i = 0; i < 4; i++) {
            Sprite s = new Sprite(T, T);
            int ox = (i & 1) * 8, oy = (i >> 1) * 8;
            for (int y = 0; y < T; y++)
                for (int x = 0; x < T; x++) {
                    double v = n[(x + ox) + (y + oy) * 16] + (r.nextDouble() - 0.5) * 0.25;
                    s.set(x, y, v < 0.3 ? d : v < 0.58 ? m : v < 0.8 ? l : hl);
                }
            out[i] = s;
        }
        return out;
    }

    private static void torch() {
        Sprite stick = new Sprite(T, T);
        for (int y = 3; y < T; y++) {
            stick.set(3, y, Pal.WOOD_L);
            stick.set(4, y, Pal.WOOD_D);
        }
        Tiles.torchStick = stick;
        Tiles.tex[Tiles.TORCH] = new Sprite[]{stick};
        String[][] flames = {
            {"..y...", ".yYy..", ".yWYo.", "..oo.."},
            {"...y..", "..yYy.", ".oYWy.", "..oo.."},
            {"..y...", ".yYy..", ".yWYy.", "..oo.."},
        };
        Tiles.torchFlame = new Sprite[3][];
        for (int f = 0; f < 3; f++) {
            Sprite s = new Sprite(T, T);
            for (int y = 0; y < 4; y++)
                for (int x = 0; x < 6; x++) {
                    char c = flames[f][y].charAt(x);
                    int col = c == 'W' ? 0xfffbe8 : c == 'Y' ? Pal.FLAME_L : c == 'y' ? Pal.FLAME : c == 'o' ? Pal.FLAME_R : -1;
                    if (col >= 0) s.set(x + 1, y, col);
                }
            Tiles.torchFlame[f] = new Sprite[]{s};
        }
    }

    private static void platform() {
        Tiles.tex[Tiles.PLATFORM] = new Sprite[4];
        for (int i = 0; i < 4; i++) {
            Sprite s = new Sprite(T, T);
            for (int x = 0; x < T; x++) {
                s.set(x, 0, Pal.WOOD_L);
                s.set(x, 1, Pal.WOOD);
                s.set(x, 2, Pal.WOOD_D);
            }
            s.set((i * 3 + 1) % T, 1, Pal.WOOD_D);
            s.set(1, 3, Pal.WOOD_D);
            s.set(6, 3, Pal.WOOD_D);
            s.set(1, 4, 0x4a2e18);
            s.set(6, 4, 0x4a2e18);
            Tiles.tex[Tiles.PLATFORM][i] = s;
        }
    }

    private static Sprite ascii8(String[] rows, int light, int mid, int dark, int extra) {
        Sprite s = new Sprite(T, T);
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                int col = c == 'L' ? light : c == 'G' ? mid : c == 'g' ? dark : c == 'P' || c == 'c' ? extra : -1;
                if (col >= 0) s.set(x, y, col);
            }
        return s;
    }

    static Sprite[] tufts(int l, int m, int d) {
        String[][] tufts = {
            {"........", "........", "........", "........", "....L...", ".L..G.L.", ".G.GG.G.", "GgGgGGgG"},
            {"........", "........", "........", "..L.....", "..G..L..", ".LG..G..", ".GgG.Gg.", "gGGgGGgG"},
            {"........", "........", "........", "........", "........", "......L.", ".L.L.GG.", "gGGGgGGg"},
            {"........", "........", "........", "......L.", ".L....G.", ".G..L.G.", "GG.GGgGG", "gGgGGGgG"},
        };
        Sprite[] out = new Sprite[4];
        for (int i = 0; i < 4; i++) out[i] = ascii8(tufts[i], l, m, d, 0);
        return out;
    }

    private static void plants(Random r) {
        Tiles.tex[Tiles.TUFT] = tufts(Pal.GRASS_L, Pal.GRASS, Pal.GRASS_D);

        Tiles.tex[Tiles.FLOWER] = new Sprite[4];
        int[] petals = {Pal.RED, Pal.YELLOW, Pal.BLUE, Pal.PINK};
        String[] flower = {"........", "...P....", "..PcP...", "...P....", "...G....", "...GL...", "..LG....", "...G...."};
        for (int i = 0; i < 4; i++) {
            Sprite s = ascii8(flower, Pal.GRASS_L, Pal.GRASS, Pal.GRASS_D, petals[i]);
            s.set(3, 2, i == 1 ? Pal.WOOD_L : Pal.YELLOW);
            s.set(2, 1, Pal.lerp(petals[i], 0xffffff, 0.35));
            Tiles.tex[Tiles.FLOWER][i] = s;
        }

        String[] sap = {"........", "........", "..L.....", ".LGL.L..", "..GLLG..", "...BG...", "...B....", "...B...."};
        Sprite sapling = ascii8(sap, Pal.LEAF_L, Pal.LEAF, Pal.LEAF_D, 0);
        for (int y = 5; y < 8; y++) sapling.set(3, y, Pal.BARK);
        Tiles.tex[Tiles.SAPLING] = new Sprite[]{sapling};

        java.util.Map<Character, Integer> mc = new java.util.HashMap<>();
        mc.put('w', 0xf0e4cc);
        mc.put('c', 0xd8c4a4);
        mc.put('d', 0xa88c6c);
        mc.put('s', 0xe8dcc8);
        mc.put('b', 0x8a6a4a);
        Tiles.tex[Tiles.MUSHROOM] = new Sprite[]{
            Sprite.ascii(mc, "........", "........", "..www...", ".wcbcw..", "wccccbw.", ".dddddd.", "...ss...", "...ss..."),
        };
    }

    // ---- furniture -------------------------------------------------------

    private static java.util.Map<Character, Integer> map(Object... kv) {
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((Character) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    private static Sprite workbench() {
        return Sprite.ascii(map('l', Pal.WOOD_L, 'w', Pal.WOOD, 'd', Pal.WOOD_D, 'D', 0x3e2614, 'g', 0x8a8a9a),
                "llllllllllllllll",
                "wwwwwwwdwwwwwwww",
                "dddddddddddddddd",
                ".wD..........wD.",
                ".wD..........wD.",
                ".wDwwwwwwwwwwwD.",
                ".wD..........wD.",
                ".wD..........wD.");
    }

    private static void furnace(Random r) {
        java.util.Map<Character, Integer> m = map('k', 0x34343e, 'd', Pal.BRICK_G_D, 's', Pal.BRICK_G, 'l', Pal.BRICK_G_L,
                'x', 0x1a1210, 'h', 0x2a1a14, 'p', 0x5a5a66);
        String[] rows = {
            "........pp..............",
            "........pp..............",
            "......klllllllk.........",
            "....klllsssssllllk......",
            "...klsssdsssdssssslk....",
            "..klssdssssssssdsssk....",
            "..ksssssssssssssssssk...",
            ".klsdssshxxxxhssssdsk...",
            ".ksssssxxxxxxxxsssssk...",
            ".kssdsshxxxxxxhsdsssk...",
            ".ksssssxxxxxxxxssssslk..",
            ".kssssshxxxxxxhssssssk..",
            ".kddddddddddddddddddddk.",
            ".klllllllllllllllllllk..",
            ".kssssssssssssssssssssk.",
            "kkkkkkkkkkkkkkkkkkkkkkkk",
        };
        Sprite base = Sprite.ascii(m, rows);
        // brick seams
        for (int y = 3; y < 12; y++)
            for (int x = 0; x < 24; x++) {
                int c = base.get(x, y) & 0xffffff;
                if (c == (Pal.BRICK_G & 0xffffff) && ((y % 3 == 0) || ((x + (y / 3) * 3) % 6 == 0))) base.set(x, y, Pal.BRICK_G_D);
            }
        Tiles.furn[Tiles.FURNACE] = new Sprite[]{base};
        Tiles.furnGlow[Tiles.FURNACE] = new Sprite[4];
        for (int f = 0; f < 4; f++) {
            Sprite g = new Sprite(24, 16);
            Random fr = new Random(40 + f);
            for (int y = 7; y < 12; y++)
                for (int x = 8; x < 16; x++) {
                    if ((base.get(x, y) & 0xffffff) != 0x1a1210) continue;
                    double hgt = (11 - y) / 4.0;
                    double v = fr.nextDouble() - hgt * 0.8;
                    int c = v > 0.25 ? Pal.FLAME_L : v > -0.15 ? Pal.FLAME : v > -0.45 ? Pal.FLAME_R : -1;
                    if (c >= 0) g.set(x, y, c);
                }
            Tiles.furnGlow[Tiles.FURNACE][f] = g;
        }
    }

    private static Sprite anvil() {
        return Sprite.ascii(map('k', 0x22222c, 'd', 0x3e3e4c, 'm', 0x5c5c6c, 'l', 0x8a8a9c, 'h', 0xb4b4c4),
                "..khhhhhhhhhhhk.",
                "kklllllllllllmk.",
                ".kkddmmmmmmddkk.",
                ".....kmmmmk.....",
                ".....kmmdmk.....",
                "....kmmmmmmk....",
                "...kllmmmmmdk...",
                "...kkkkkkkkkk...");
    }

    private static void lifeCrystal() {
        java.util.Map<Character, Integer> m = map('k', 0x4a0a20, 'd', Pal.CRYSTAL_D, 'm', Pal.CRYSTAL, 'l', Pal.CRYSTAL_L,
                'w', 0xffe0ea, 's', Pal.STONE_D, 'S', Pal.STONE, 'L', Pal.STONE_L);
        Sprite s = Sprite.ascii(m,
                "................",
                "...kkk....kkk...",
                "..kmlwk..kmmmk..",
                ".kmlwlmkkmmmmdk.",
                ".kmllmmmmmmmmdk.",
                ".kmlmmmmmmmmddk.",
                "..kmmmmmmmmddk..",
                "...kmmmmmmddk...",
                "....kmmmmddk....",
                ".....kmmddk.....",
                "......kmdk......",
                ".....LSkkSS.....",
                "...LSSSSSSSSs...",
                "..LSSSsSSSSSSs..",
                ".LSSSSSSSsSSSSs.",
                "sssssssssssssss.");
        Tiles.furn[Tiles.LIFE_CRYSTAL] = new Sprite[]{s};
        Sprite g = new Sprite(16, 16);
        g.set(4, 2, Pal.CRYSTAL_L);
        g.set(5, 2, 0xffe0ea);
        g.set(4, 3, 0xffe0ea);
        g.set(3, 4, Pal.CRYSTAL_L);
        Tiles.furnGlow[Tiles.LIFE_CRYSTAL] = new Sprite[]{g};
    }

    private static Sprite pot(Random r, int d, int m, int l) {
        java.util.Map<Character, Integer> cm = map('k', Pal.shade(d, 150), 'd', d, 'm', m, 'l', l, 'b', Pal.shade(d, 190));
        return Sprite.ascii(cm,
                "................",
                ".....kkkkkk.....",
                ".....kllmmk.....",
                "......kmmk......",
                "....kkmmmmkk....",
                "...kmlmmmmmdk...",
                "..kmlmmmmmmmdk..",
                "..kbbbbbbbbbbk..",
                ".kmlmmmmmmmmmdk.",
                ".kmlmmmmmmmmmdk.",
                ".kbbbbbbbbbbbbk.",
                ".kmlmmmmmmmmddk.",
                "..kmmmmmmmmmdk..",
                "..kdmmmmmmmddk..",
                "...kddddddddk...",
                "....kkkkkkkk....");
    }

    // ---- walls, cracks, caves ------------------------------------------------

    private static void walls(Random r) {
        Tiles.wallTex[Tiles.W_NONE] = new Sprite[]{new Sprite(16, 16)};
        // dirt: darkened dirt with soft cracks
        Sprite dirt = new Sprite(16, 16);
        double[] n = tnoise(r, 16, 4, 2);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                double v = n[x + y * 16];
                dirt.set(x, y, v < 0.35 ? 0x2c1c14 : v < 0.7 ? 0x3a261b : 0x463023);
            }
        Tiles.wallTex[Tiles.W_DIRT] = new Sprite[]{dirt};
        Tiles.wallTex[Tiles.W_DIRT_N] = new Sprite[]{roughWall(r, 0x21150e, 0x3a2719, 0x160e09)};
        Tiles.wallTex[Tiles.W_STONE_N] = new Sprite[]{roughWall(r, 0x1d1d25, 0x363643, 0x121218)};

        // stone: irregular blocks
        Sprite stone = new Sprite(16, 16);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int row = y / 4, yy = y % 4;
                int off = (row & 1) * 5;
                int bx = Math.floorMod(x - off, 8);
                int c = 0x3a3a48;
                if (yy == 3 || bx == 7) c = 0x262632;
                else if (yy == 0) c = 0x464656;
                else if (r.nextInt(9) == 0) c = 0x42424f;
                stone.set(x, y, c);
            }
        Tiles.wallTex[Tiles.W_STONE] = new Sprite[]{stone};

        // wood: vertical boards
        Sprite wood = new Sprite(16, 16);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = 0x5a3a22;
                int bx = x % 4;
                if (bx == 3) c = 0x3a2414;
                else if (bx == 0) c = 0x6a4628;
                if (y == ((x / 4) * 5 + 3) % 16 && bx != 3) c = 0x3a2414;
                if (bx == 1 && r.nextInt(8) == 0) c = 0x4e321e;
                wood.set(x, y, c);
            }
        Tiles.wallTex[Tiles.W_WOOD] = new Sprite[]{wood};

        Sprite glass = new Sprite(16, 16);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int c = (x % 8 == 0 || y % 8 == 0) ? 0x3a4a5a : 0x28405a;
                if ((x % 8) + (y % 8) == 6 && x % 8 != 0) c = 0x4a6a8a;
                glass.set(x, y, c);
            }
        Tiles.wallTex[Tiles.W_GLASS] = new Sprite[]{glass};

        Sprite gb = brickMacro(r, 0x22222a, 0x34343e, 0x44444f, 0x50505c);
        Tiles.wallTex[Tiles.W_GRAY_BRICK] = new Sprite[]{tile16(gb)};
        Sprite rb = brickMacro(r, 0x2a1612, 0x3e1e18, 0x582a20, 0x6a3428);
        Tiles.wallTex[Tiles.W_RED_BRICK] = new Sprite[]{tile16(rb)};
    }

    /** Natural cave wall: rough, uneven rock or earth with dark cracks (64 x 64, tileable). */
    private static Sprite roughWall(Random r, int dark, int light, int crack) {
        int n = 64;
        double[] a = tnoise(r, n, 4, 4), b = tnoise(r, n, 8, 3);
        Sprite s = new Sprite(n, n);
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double v = a[x + y * n] * 0.6 + b[x + y * n] * 0.4;
                // lit from above: compare with the value one pixel up
                double up = a[x + ((y + n - 1) % n) * n] * 0.6 + b[x + ((y + n - 1) % n) * n] * 0.4;
                double shade = Math.max(-1, Math.min(1, (v - up) * 12));
                int c = Pal.lerp(dark, light, Math.max(0, Math.min(1, v * 0.9 + shade * 0.15)));
                if (Math.abs(b[x + y * n] - 0.5) < 0.018) c = crack;
                s.set(x, y, c);
            }
        return s;
    }

    /** Repeats a 16x8 texture to 16x16. */
    private static Sprite tile16(Sprite s) {
        Sprite o = new Sprite(16, 16);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) o.p[x + y * 16] = s.get(x, y % s.h);
        return o;
    }

    private static void cracks() {
        Tiles.cracks = new Sprite[3];
        String[][] cr = {
            {"........", "........", "...X....", "...XX...", "....X...", "........", "........", "........"},
            {"........", ".X......", "..X..X..", "...XX...", "...X.X..", "..X.....", "..X.....", "........"},
            {"X.....X.", ".X...X..", "..X.XX..", "...XX...", "..XX.X..", ".X...XX.", "X.X....X", "..X....."},
        };
        for (int i = 0; i < 3; i++) {
            Sprite s = new Sprite(T, T);
            for (int y = 0; y < T; y++)
                for (int x = 0; x < T; x++)
                    if (cr[i][y].charAt(x) == 'X') s.setA(x, y, Pal.BLACK, 190);
            Tiles.cracks[i] = s;
        }
    }

    private static void caves(Random r) {
        int n = 64;
        // dirt caves: soft earthy blotches with roots
        double[] a = tnoise(r, n, 4, 4);
        Sprite d = new Sprite(n, n);
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double v = a[x + y * n];
                int c = Pal.lerp(0x1e140e, 0x3a281c, v);
                if (v > 0.52 && v < 0.56) c = 0x16100a;
                d.set(x, y, c);
            }
        for (int k = 0; k < 6; k++) {
            double x = r.nextInt(n), y = r.nextInt(n), ang = Math.PI / 2 + (r.nextDouble() - 0.5);
            for (int s = 0; s < 14; s++) {
                d.set(Math.floorMod((int) x, n), Math.floorMod((int) y, n), 0x2a1c12);
                x += Math.cos(ang);
                y += Math.sin(ang);
                ang += (r.nextDouble() - 0.5) * 0.8;
            }
        }
        // stone caves: layered rock with cracks
        double[] b = tnoise(r, n, 4, 4), c2 = tnoise(r, n, 8, 2);
        Sprite st = new Sprite(n, n);
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double v = b[x + y * n];
                double layer = Math.sin((y + c2[x + y * n] * 10) * Math.PI * 2 / 32.0);
                int c = Pal.lerp(0x16161e, 0x30303c, v * 0.75 + (layer + 1) * 0.125);
                if (Math.abs(v - 0.5) < 0.015) c = 0x101016;
                st.set(x, y, c);
            }
        Tiles.caveBack[0] = d;
        Tiles.caveBack[1] = st;
    }

    /** An 8x8 sample of a tile's look (for item icons). */
    static Sprite sample(int t) {
        Sprite s = new Sprite(T, T);
        for (int y = 0; y < T; y++)
            for (int x = 0; x < T; x++) s.p[x + y * T] = Tiles.texel(t, 0, x, y, x + 3, y + 5);
        return s;
    }

    static Sprite darkened(Sprite s, double k) {
        return darker(s, k);
    }
}
