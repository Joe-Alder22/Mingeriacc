package mingeriacc;

/**
 * The game's colours. All graphics are drawn with these colours and blends of
 * them; lighting multiplies them per pixel.
 */
final class Pal {
    private Pal() {}

    // Neutrals
    static final int BLACK = 0x0b0a12;
    static final int DARK = 0x1a1826;
    static final int GRAY_D = 0x3a3a4c;
    static final int GRAY = 0x6a6a7e;
    static final int GRAY_L = 0x9c9cb0;
    static final int WHITE = 0xf2f2f6;

    // Dirt
    static final int DIRT_DD = 0x38241a;
    static final int DIRT_D = 0x4f3224;
    static final int DIRT = 0x6f4a34;
    static final int DIRT_L = 0x8c6246;

    // Grass
    static final int GRASS_D = 0x2c6a30;
    static final int GRASS = 0x44933c;
    static final int GRASS_L = 0x6cb84c;
    static final int GRASS_HL = 0x96d468;

    // Stone
    static final int STONE_D = 0x3e3e4a;
    static final int STONE = 0x5f6070;
    static final int STONE_L = 0x858898;

    // Sand
    static final int SAND_D = 0xae905a;
    static final int SAND = 0xd0b47c;
    static final int SAND_L = 0xe8d29e;

    // Clay
    static final int CLAY_D = 0x86422f;
    static final int CLAY = 0xa95d42;
    static final int CLAY_L = 0xc77b5a;

    // Ores and metals
    static final int COP_D = 0x9a4a24;
    static final int COP = 0xd0703a;
    static final int COP_L = 0xf0a060;
    static final int IRON_D = 0x7c6660;
    static final int IRON = 0xb49c94;
    static final int IRON_L = 0xdcc8c0;
    static final int SILV_D = 0x8890a0;
    static final int SILV = 0xc8d0dc;
    static final int SILV_L = 0xf4f8ff;
    static final int GOLD_D = 0xa07818;
    static final int GOLD = 0xe8c030;
    static final int GOLD_L = 0xfff080;

    // Wood
    static final int WOOD_D = 0x5a3a20;
    static final int WOOD = 0x8a5a30;
    static final int WOOD_L = 0xb07c48;
    static final int BARK_D = 0x3e2a1a;
    static final int BARK = 0x6a4a2c;
    static final int BARK_L = 0x86603a;

    // Leaves
    static final int LEAF_D = 0x1e4a28;
    static final int LEAF = 0x2e7636;
    static final int LEAF_L = 0x4c9c44;
    static final int LEAF_HL = 0x78c05a;

    // Flame
    static final int FLAME_R = 0xe05020;
    static final int FLAME = 0xffb030;
    static final int FLAME_L = 0xfff0a0;

    // Flowers and small plants
    static final int RED = 0xd84848;
    static final int YELLOW = 0xf0d848;
    static final int BLUE = 0x6a8ef0;
    static final int PINK = 0xe89ad0;

    // Bricks and glass
    static final int BRICK_G_D = 0x4a4a58;
    static final int BRICK_G = 0x707282;
    static final int BRICK_G_L = 0x9294a4;
    static final int BRICK_R_D = 0x6a2a22;
    static final int BRICK_R = 0x9a4030;
    static final int BRICK_R_L = 0xc0604a;
    static final int GLASS = 0xa8d4f0;
    static final int GLASS_L = 0xe8f6ff;

    // Jungle
    static final int MUD_D = 0x3a2828;
    static final int MUD = 0x563e3a;
    static final int MUD_L = 0x6e5450;
    static final int JGRASS_D = 0x3a7a1c;
    static final int JGRASS = 0x5aa82c;
    static final int JGRASS_L = 0x86cc40;
    static final int MAHO_D = 0x5a2620;
    static final int MAHO = 0x8a3e30;
    static final int MAHO_L = 0xb05c46;

    // Corruption
    static final int EBON_D = 0x322a44;
    static final int EBON = 0x524566;
    static final int EBON_L = 0x76688e;
    static final int CGRASS_D = 0x46386a;
    static final int CGRASS = 0x6a589e;
    static final int CGRASS_L = 0x9280c6;
    static final int DEMO_D = 0x3a2a6a;
    static final int DEMO = 0x6a4ac0;
    static final int DEMO_L = 0x9a80e8;

    // Underworld
    static final int ASH_D = 0x2c2826;
    static final int ASH = 0x46403c;
    static final int ASH_L = 0x625852;
    static final int HELL_D = 0x6a1a0a;
    static final int HELL = 0xc0381a;
    static final int HELL_L = 0xff7a30;
    static final int OBS_D = 0x140e1e;
    static final int OBS = 0x2a2040;
    static final int OBS_L = 0x4a3a6a;

    // Liquids
    static final int WATER = 0x2860c8;
    static final int WATER_L = 0x6aa8f0;
    static final int LAVA_D = 0xb02008;
    static final int LAVA = 0xf05a10;
    static final int LAVA_L = 0xffb040;

    // Life crystal, gel, lens
    static final int CRYSTAL_D = 0x8a1a3a;
    static final int CRYSTAL = 0xe0406a;
    static final int CRYSTAL_L = 0xff9ab8;
    static final int GEL = 0x4a90e8;
    static final int GEL_L = 0x9ac8ff;

    // Player
    static final int SKIN = 0xf0b890;
    static final int SKIN_D = 0xc88c68;
    static final int HAIR = 0x5a3418;
    static final int HAIR_D = 0x3e2210;
    static final int SHIRT = 0x3a6ec8;
    static final int SHIRT_D = 0x2a4e96;
    static final int PANTS = 0x4a3a2a;
    static final int PANTS_D = 0x33281c;
    static final int SHOES = 0x2a2020;

    // User interface
    static final int UI_BG = 0x1a2548;
    static final int UI_BG2 = 0x2c3c70;
    static final int UI_EDGE = 0x4d68a8;
    static final int UI_EDGE_L = 0x8ea8e0;
    static final int UI_SEL = 0xf5d36a;
    static final int UI_TEXT = 0xf0f0f6;
    static final int UI_DIM = 0x98a0bc;
    static final int UI_GOOD = 0x8ee08a;
    static final int UI_BAD = 0xf07a6a;
    static final int UI_HEART = 0xe8384a;
    static final int UI_HEART_L = 0xff8a90;
    static final int UI_HEART_D = 0x7a1424;
    static final int UI_DAMAGE = 0xff9a3a;
    static final int UI_CRIT = 0xff5a1a;
    static final int UI_HURT = 0xff4a4a;
    static final int UI_HEAL = 0x5aff7a;

    // Item rarity colours (name colours)
    static final int[] RARITY = {0xf0f0f6, 0x9696ff, 0x96ff96, 0xffc896, 0xff9696, 0xff96ff};

    // Sky key colours (top, horizon)
    static final int SKY_DAY_TOP = 0x3f7fd6;
    static final int SKY_DAY_BOT = 0xa6d2f2;
    static final int SKY_DAWN_TOP = 0x535596;
    static final int SKY_DAWN_BOT = 0xf2a678;
    static final int SKY_DUSK_TOP = 0x3a2e6c;
    static final int SKY_DUSK_BOT = 0xea8864;
    static final int SKY_NIGHT_TOP = 0x050818;
    static final int SKY_NIGHT_BOT = 0x14224a;

    // Background landscape (far to near)
    static final int HILL_FAR = 0x86aed6;
    static final int HILL_MID = 0x5a9480;
    static final int HILL_NEAR = 0x3a7248;
    static final int DUNE_FAR = 0xd8c4a0;
    static final int DUNE_NEAR = 0xc8a06a;

    // Cave backdrop
    static final int CAVE_DIRT = 0x3a271c;
    static final int CAVE_STONE = 0x2c2c38;

    // ---- helpers --------------------------------------------------------

    static int r(int c) { return (c >> 16) & 255; }
    static int g(int c) { return (c >> 8) & 255; }
    static int b(int c) { return c & 255; }

    static int rgb(int r, int g, int b) {
        if (r < 0) r = 0; else if (r > 255) r = 255;
        if (g < 0) g = 0; else if (g > 255) g = 255;
        if (b < 0) b = 0; else if (b > 255) b = 255;
        return (r << 16) | (g << 8) | b;
    }

    /** Linear blend, t = 0..1. */
    static int lerp(int a, int b, double t) {
        if (t <= 0) return a & 0xffffff;
        if (t >= 1) return b & 0xffffff;
        return rgb((int) (r(a) + (r(b) - r(a)) * t),
                   (int) (g(a) + (g(b) - g(a)) * t),
                   (int) (b(a) + (b(b) - b(a)) * t));
    }

    /** Darken: s = 0..256 (256 = full brightness). */
    static int shade(int c, int s) {
        if (s >= 256) return c & 0xffffff;
        return (((c >> 16 & 255) * s >> 8) << 16)
             | (((c >> 8 & 255) * s >> 8) << 8)
             | ((c & 255) * s >> 8);
    }

    /** Multiplies each channel separately (0..256 each). */
    static int mul(int c, int lr, int lg, int lb) {
        return (((c >> 16 & 255) * lr >> 8) << 16)
             | (((c >> 8 & 255) * lg >> 8) << 8)
             | ((c & 255) * lb >> 8);
    }

    /** Multiplies by a colour used as a light (0xRRGGBB, 255 = full). */
    static int mulLight(int c, int light) {
        return mul(c, (light >> 16 & 255) + 1, (light >> 8 & 255) + 1, (light & 255) + 1);
    }

    /** Additive blend with clamping; a = 0..256. */
    static int add(int dst, int src, int a) {
        int r = (dst >> 16 & 255) + ((src >> 16 & 255) * a >> 8);
        int g = (dst >> 8 & 255) + ((src >> 8 & 255) * a >> 8);
        int b = (dst & 255) + ((src & 255) * a >> 8);
        return ((r > 255 ? 255 : r) << 16) | ((g > 255 ? 255 : g) << 8) | (b > 255 ? 255 : b);
    }

    /** Alpha blend: a = 0..256. */
    static int blend(int dst, int src, int a) {
        if (a >= 256) return src & 0xffffff;
        if (a <= 0) return dst & 0xffffff;
        int ia = 256 - a;
        return ((((src >> 16 & 255) * a + (dst >> 16 & 255) * ia) >> 8) << 16)
             | ((((src >> 8 & 255) * a + (dst >> 8 & 255) * ia) >> 8) << 8)
             | (((src & 255) * a + (dst & 255) * ia) >> 8);
    }

    /** Brightens (t > 0) or darkens (t < 0) towards white or black. */
    static int tone(int c, double t) {
        return t >= 0 ? lerp(c, 0xffffff, t) : lerp(c, 0, -t);
    }
}
