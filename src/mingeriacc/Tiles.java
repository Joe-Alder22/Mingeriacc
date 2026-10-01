package mingeriacc;

/**
 * Tile and wall definitions as parallel arrays indexed by ID. New IDs are
 * always added at the end: saves store raw IDs. Textures are made in TileArt.
 */
final class Tiles {
    private Tiles() {}

    static final int T = 8; // tile size in pixels
    static final int TORCH_LIGHT = 0xffe6bc;

    // Foreground tiles
    static final int AIR = 0, DIRT = 1, GRASS = 2, STONE = 3, SAND = 4, CLAY = 5,
            COPPER = 6, IRON = 7, SILVER = 8, GOLD = 9, PLANKS = 10, TRUNK = 11,
            LEAVES = 12, TORCH = 13, PLATFORM = 14, TUFT = 15, FLOWER = 16, SAPLING = 17,
            WORKBENCH = 18, FURNACE = 19, ANVIL = 20, LIFE_CRYSTAL = 21, MUSHROOM = 22,
            GLASS = 23, GRAY_BRICK = 24, RED_BRICK = 25, POT = 26,
            MUD = 27, JUNGLE_GRASS = 28, EBONSTONE = 29, CORRUPT_GRASS = 30, EBONSAND = 31, ASH = 32,
            HELLSTONE = 33, OBSIDIAN = 34, DEMONITE = 35, MAHOGANY_TRUNK = 36, MAHOGANY_LEAVES = 37,
            EBON_TRUNK = 38, EBON_LEAVES = 39, VINE = 40, JUNGLE_PLANT = 41, CORRUPT_PLANT = 42,
            JUNGLE_SPORE = 43, MAHOGANY_PLANKS = 44, EBON_PLANKS = 45, CHEST = 46, DOOR_CLOSED = 47,
            DOOR_OPEN = 48, TABLE = 49, CHAIR = 50, BED = 51, OBSIDIAN_BRICK = 52, HELLSTONE_BRICK = 53,
            GOLD_CHEST = 54,
            RUBY = 55, SAPPHIRE = 56, EMERALD = 57, TOPAZ = 58,
            GREEN_MOSS = 59, BROWN_MOSS = 60, RED_MOSS = 61, BLUE_MOSS = 62, PURPLE_MOSS = 63, MOSS_PLANT = 64,
            SNOW = 65, ICE = 66, BOREAL_TRUNK = 67, BOREAL_LEAVES = 68, BOREAL_PLANKS = 69, ICE_CHEST = 70;
    static final int COUNT = 71;

    // Walls (the _N walls are natural ones made by world generation: enemies may spawn on them)
    static final int W_NONE = 0, W_DIRT = 1, W_STONE = 2, W_WOOD = 3, W_DIRT_N = 4, W_STONE_N = 5,
            W_GLASS = 6, W_GRAY_BRICK = 7, W_RED_BRICK = 8, W_MUD_N = 9, W_EBON_N = 10, W_MAHOGANY = 11,
            W_OBSIDIAN_BRICK = 12, W_HELLSTONE_BRICK = 13, W_SNOW_N = 14, W_ICE_N = 15, W_BOREAL = 16;
    static final int WALL_COUNT = 17;

    // Tool types
    static final int TOOL_NONE = 0, TOOL_PICK = 1, TOOL_AXE = 2, TOOL_HAMMER = 3;

    // Dig sounds
    static final int SND_SOFT = 0, SND_HARD = 1, SND_WOOD = 2, SND_GLASS = 3, SND_METAL = 4, SND_PLANT = 5;

    static final String[] NAME = new String[COUNT];
    static final boolean[] SOLID = new boolean[COUNT];
    static final int[] HP = new int[COUNT];
    static final int[] TOOL = new int[COUNT];            // which tool mines it
    static final int[] MIN_PICK = new int[COUNT];        // pickaxe power needed
    static final boolean[] FRAGILE = new boolean[COUNT]; // breaks from any swing
    static final int[] DROP = new int[COUNT];            // dropped item
    static final int[] LIGHT = new int[COUNT];           // light emitted 0..15
    static final int[] LIGHT_COLOR = new int[COUNT];     // colour of that light
    static final int[] COLOR = new int[COUNT];           // particles etc.
    static final int[] EDGE = new int[COUNT];            // edge line colour
    static final boolean[] NEEDS_GROUND = new boolean[COUNT]; // needs a tile below it
    static final boolean[] TRANSPARENT = new boolean[COUNT];  // solid, but lets light through (glass)
    static final int[] SOUND = new int[COUNT];
    static final boolean[] BLEED_SRC = new boolean[COUNT];    // soft ground that bleeds into neighbours
    static final boolean[] BLEED_DST = new boolean[COUNT];    // hard ground that soft ground bleeds into
    static final boolean[] PLACE_OVER = new boolean[COUNT];   // can be replaced by placing a tile
    /** Moss colours (highlight, light, mid, dark) for moss-covered stone, else null. */
    static final int[][] MOSS = new int[COUNT][];

    // Furniture: tiles made of several cells. The meta byte of each cell holds
    // its offset inside the object: dx | dy << 3 | flags << 6.
    static final int[] FURN_W = new int[COUNT], FURN_H = new int[COUNT];
    static final boolean[] PLAT_TOP = new boolean[COUNT];     // the top can be stood on

    static final int[] WALL_HP = new int[WALL_COUNT];
    static final int[] WALL_DROP = new int[WALL_COUNT];
    static final int[] WALL_COLOR = new int[WALL_COUNT];
    static final boolean[] WALL_NATURAL = new boolean[WALL_COUNT];
    static final String[] WALL_NAME = new String[WALL_COUNT];

    // Textures (filled in by TileArt)
    static Sprite[][] tex = new Sprite[COUNT][];
    static Sprite[] macro = new Sprite[COUNT];       // world-aligned texture, or null
    static Sprite[][] furn = new Sprite[COUNT][];    // whole-object images (animation frames)
    static Sprite[][] furnGlow = new Sprite[COUNT][]; // parts that glow (drawn unlit)
    static Sprite[][] wallTex = new Sprite[WALL_COUNT][];
    static Sprite[] caveBack = new Sprite[5];        // parallax cave backdrops (dirt, stone, jungle, corruption, snow)
    static Sprite[][] torchFlame;
    static Sprite torchStick;
    static Sprite[] trunkBase, trunkBaseMaho, trunkBaseEbon, trunkBaseBoreal;
    static Sprite[][] mossPlant = new Sprite[5][];   // moss tufts in each moss colour
    static Sprite[] cracks;

    private static void def(int id, String name, boolean solid, int hp, int tool, int drop,
                            int color, int edge, int sound) {
        NAME[id] = name;
        SOLID[id] = solid;
        HP[id] = hp;
        TOOL[id] = tool;
        DROP[id] = drop;
        COLOR[id] = color;
        EDGE[id] = edge;
        SOUND[id] = sound;
    }

    private static void light(int id, int level, int color) {
        LIGHT[id] = level;
        LIGHT_COLOR[id] = color;
    }

    private static void furniture(int id, int w, int h) {
        FURN_W[id] = w;
        FURN_H[id] = h;
    }

    private static void wall(int id, String name, int hp, int drop, int color, boolean natural) {
        WALL_NAME[id] = name;
        WALL_HP[id] = hp;
        WALL_DROP[id] = drop;
        WALL_COLOR[id] = color;
        WALL_NATURAL[id] = natural;
    }

    static {
        def(AIR, "Air", false, 0, TOOL_NONE, 0, 0, 0, SND_SOFT);
        def(DIRT, "Dirt", true, 40, TOOL_PICK, Items.DIRT, Pal.DIRT, Pal.DIRT_DD, SND_SOFT);
        def(GRASS, "Grass", true, 40, TOOL_PICK, Items.DIRT, Pal.GRASS, Pal.DIRT_DD, SND_SOFT);
        def(STONE, "Stone", true, 80, TOOL_PICK, Items.STONE, Pal.STONE, 0x2e2e38, SND_HARD);
        def(SAND, "Sand", true, 35, TOOL_PICK, Items.SAND, Pal.SAND, 0x8a7046, SND_SOFT);
        def(CLAY, "Clay", true, 45, TOOL_PICK, Items.CLAY, Pal.CLAY, 0x6a3024, SND_SOFT);
        def(COPPER, "Copper Ore", true, 100, TOOL_PICK, Items.COPPER_ORE, Pal.COP, 0x2e2e38, SND_HARD);
        def(IRON, "Iron Ore", true, 110, TOOL_PICK, Items.IRON_ORE, Pal.IRON, 0x2e2e38, SND_HARD);
        def(SILVER, "Silver Ore", true, 120, TOOL_PICK, Items.SILVER_ORE, Pal.SILV, 0x2e2e38, SND_HARD);
        def(GOLD, "Gold Ore", true, 140, TOOL_PICK, Items.GOLD_ORE, Pal.GOLD, 0x2e2e38, SND_HARD);
        def(PLANKS, "Wood Planks", true, 50, TOOL_PICK, Items.WOOD, Pal.WOOD, 0x3e2614, SND_WOOD);
        def(TRUNK, "Tree Trunk", false, 110, TOOL_AXE, Items.WOOD, Pal.BARK, 0, SND_WOOD);
        def(LEAVES, "Leaves", false, 1, TOOL_NONE, 0, Pal.LEAF, 0, SND_PLANT);
        def(TORCH, "Torch", false, 1, TOOL_PICK, Items.TORCH, Pal.FLAME, 0, SND_WOOD);
        def(PLATFORM, "Wood Platform", false, 20, TOOL_PICK, Items.PLATFORM, Pal.WOOD, 0, SND_WOOD);
        def(TUFT, "Tall Grass", false, 1, TOOL_NONE, 0, Pal.GRASS, 0, SND_PLANT);
        def(FLOWER, "Flower", false, 1, TOOL_NONE, 0, Pal.YELLOW, 0, SND_PLANT);
        def(SAPLING, "Sapling", false, 1, TOOL_NONE, Items.ACORN, Pal.LEAF_L, 0, SND_PLANT);
        def(WORKBENCH, "Work Bench", false, 30, TOOL_PICK, Items.WORKBENCH, Pal.WOOD, 0, SND_WOOD);
        def(FURNACE, "Furnace", false, 60, TOOL_PICK, Items.FURNACE, Pal.STONE, 0, SND_HARD);
        def(ANVIL, "Iron Anvil", false, 60, TOOL_PICK, Items.IRON_ANVIL, Pal.GRAY, 0, SND_METAL);
        def(LIFE_CRYSTAL, "Life Crystal", false, 150, TOOL_PICK, Items.LIFE_CRYSTAL, Pal.CRYSTAL, 0, SND_GLASS);
        def(MUSHROOM, "Mushroom", false, 1, TOOL_NONE, Items.MUSHROOM, 0xd8c8b0, 0, SND_PLANT);
        def(GLASS, "Glass", true, 20, TOOL_PICK, Items.GLASS, Pal.GLASS, 0x6a9ab8, SND_GLASS);
        def(GRAY_BRICK, "Gray Brick", true, 90, TOOL_PICK, Items.GRAY_BRICK, Pal.BRICK_G, 0x34343e, SND_HARD);
        def(RED_BRICK, "Red Brick", true, 90, TOOL_PICK, Items.RED_BRICK, Pal.BRICK_R, 0x4a1c16, SND_HARD);
        def(POT, "Pot", false, 1, TOOL_NONE, 0, Pal.CLAY, 0, SND_GLASS);
        def(MUD, "Mud", true, 40, TOOL_PICK, Items.MUD, Pal.MUD, 0x2a1c1c, SND_SOFT);
        def(JUNGLE_GRASS, "Jungle Grass", true, 40, TOOL_PICK, Items.MUD, Pal.JGRASS, 0x2a1c1c, SND_SOFT);
        def(EBONSTONE, "Ebonstone", true, 150, TOOL_PICK, Items.EBONSTONE, Pal.EBON, 0x241c34, SND_HARD);
        def(CORRUPT_GRASS, "Corrupt Grass", true, 40, TOOL_PICK, Items.DIRT, Pal.CGRASS, Pal.DIRT_DD, SND_SOFT);
        def(EBONSAND, "Ebonsand", true, 40, TOOL_PICK, Items.EBONSAND, 0x6a6280, 0x3a3448, SND_SOFT);
        def(ASH, "Ash", true, 40, TOOL_PICK, Items.ASH, Pal.ASH, 0x201c1c, SND_SOFT);
        def(HELLSTONE, "Hellstone", true, 250, TOOL_PICK, Items.HELLSTONE, Pal.HELL, 0x2a0e08, SND_HARD);
        def(OBSIDIAN, "Obsidian", true, 200, TOOL_PICK, Items.OBSIDIAN, Pal.OBS, 0x0a0612, SND_HARD);
        def(DEMONITE, "Demonite Ore", true, 180, TOOL_PICK, Items.DEMONITE_ORE, Pal.DEMO, 0x241c34, SND_HARD);
        def(MAHOGANY_TRUNK, "Rich Mahogany", false, 110, TOOL_AXE, Items.MAHOGANY, Pal.MAHO, 0, SND_WOOD);
        def(MAHOGANY_LEAVES, "Leaves", false, 1, TOOL_NONE, 0, Pal.JGRASS, 0, SND_PLANT);
        def(EBON_TRUNK, "Ebonwood Tree", false, 110, TOOL_AXE, Items.EBONWOOD, 0x4a3a5a, 0, SND_WOOD);
        def(EBON_LEAVES, "Leaves", false, 1, TOOL_NONE, 0, Pal.CGRASS, 0, SND_PLANT);
        def(VINE, "Vine", false, 1, TOOL_NONE, Items.VINE, Pal.JGRASS, 0, SND_PLANT);
        def(JUNGLE_PLANT, "Jungle Plant", false, 1, TOOL_NONE, 0, Pal.JGRASS, 0, SND_PLANT);
        def(CORRUPT_PLANT, "Corrupt Plant", false, 1, TOOL_NONE, 0, Pal.CGRASS, 0, SND_PLANT);
        def(JUNGLE_SPORE, "Jungle Spores", false, 1, TOOL_NONE, Items.JUNGLE_SPORES, 0x9ae060, 0, SND_PLANT);
        def(MAHOGANY_PLANKS, "Rich Mahogany Planks", true, 50, TOOL_PICK, Items.MAHOGANY, Pal.MAHO, 0x3a1812, SND_WOOD);
        def(EBON_PLANKS, "Ebonwood Planks", true, 50, TOOL_PICK, Items.EBONWOOD, 0x5a4a6a, 0x221a2c, SND_WOOD);
        def(CHEST, "Chest", false, 40, TOOL_PICK, Items.CHEST, Pal.WOOD, 0, SND_WOOD);
        def(DOOR_CLOSED, "Door", true, 40, TOOL_PICK, Items.DOOR, Pal.WOOD, 0, SND_WOOD);
        def(DOOR_OPEN, "Door", false, 40, TOOL_PICK, Items.DOOR, Pal.WOOD, 0, SND_WOOD);
        def(TABLE, "Table", false, 30, TOOL_PICK, Items.TABLE, Pal.WOOD, 0, SND_WOOD);
        def(CHAIR, "Chair", false, 30, TOOL_PICK, Items.CHAIR, Pal.WOOD, 0, SND_WOOD);
        def(BED, "Bed", false, 30, TOOL_PICK, Items.BED, Pal.WOOD, 0, SND_WOOD);
        def(OBSIDIAN_BRICK, "Obsidian Brick", true, 200, TOOL_PICK, Items.OBSIDIAN_BRICK, Pal.OBS, 0x0a0612, SND_HARD);
        def(HELLSTONE_BRICK, "Hellstone Brick", true, 150, TOOL_PICK, Items.HELLSTONE_BRICK, Pal.HELL_D, 0x2a0e08, SND_HARD);
        def(GOLD_CHEST, "Gold Chest", false, 60, TOOL_PICK, Items.GOLD_CHEST, Pal.GOLD, 0, SND_METAL);
        // version 0.4: gems, moss, snow
        def(RUBY, "Ruby", true, 120, TOOL_PICK, Items.RUBY, Pal.RUBY, 0x2e2e38, SND_GLASS);
        def(SAPPHIRE, "Sapphire", true, 120, TOOL_PICK, Items.SAPPHIRE, Pal.SAPPHIRE, 0x2e2e38, SND_GLASS);
        def(EMERALD, "Emerald", true, 120, TOOL_PICK, Items.EMERALD, Pal.EMERALD, 0x2e2e38, SND_GLASS);
        def(TOPAZ, "Topaz", true, 120, TOOL_PICK, Items.TOPAZ, Pal.TOPAZ, 0x2e2e38, SND_GLASS);
        String[] mossNames = {"Green Moss", "Brown Moss", "Red Moss", "Blue Moss", "Purple Moss"};
        int[][] mossRamps = {
            {0x9af070, 0x6ac848, 0x46a034, 0x2a7024}, {0xd8b070, 0xb08848, 0x8a6434, 0x5e4024},
            {0xff8a7a, 0xe05048, 0xb03034, 0x7a1a24}, {0x9ad8ff, 0x5aa8f0, 0x3a78d0, 0x24509a},
            {0xe0a0ff, 0xb070f0, 0x8a48d0, 0x5a2a9a}};
        for (int k = 0; k < 5; k++) {
            int t = GREEN_MOSS + k;
            def(t, mossNames[k], true, 80, TOOL_PICK, Items.STONE, mossRamps[k][2], 0x2e2e38, SND_HARD);
            MOSS[t] = mossRamps[k];
        }
        def(MOSS_PLANT, "Moss", false, 1, TOOL_NONE, 0, 0x6ac848, 0, SND_PLANT);
        def(SNOW, "Snow", true, 35, TOOL_PICK, Items.SNOW_BLOCK, Pal.SNOW, 0x8a9ab8, SND_SOFT);
        def(ICE, "Ice", true, 60, TOOL_PICK, Items.ICE_BLOCK, Pal.ICE, 0x4a7aa8, SND_GLASS);
        def(BOREAL_TRUNK, "Boreal Tree", false, 110, TOOL_AXE, Items.BOREAL_WOOD, Pal.BOREAL, 0, SND_WOOD);
        def(BOREAL_LEAVES, "Leaves", false, 1, TOOL_NONE, 0, 0x2a5a4a, 0, SND_PLANT);
        def(BOREAL_PLANKS, "Boreal Planks", true, 50, TOOL_PICK, Items.BOREAL_WOOD, Pal.BOREAL_L, 0x3e3226, SND_WOOD);
        def(ICE_CHEST, "Frozen Chest", false, 50, TOOL_PICK, Items.ICE_CHEST, Pal.ICE, 0, SND_GLASS);
        MIN_PICK[EBONSTONE] = 45;
        MIN_PICK[DEMONITE] = 55;
        MIN_PICK[OBSIDIAN] = 55;
        MIN_PICK[OBSIDIAN_BRICK] = 55;
        MIN_PICK[HELLSTONE] = 65;

        FRAGILE[TORCH] = true;
        FRAGILE[TUFT] = true;
        FRAGILE[FLOWER] = true;
        FRAGILE[SAPLING] = true;
        FRAGILE[MUSHROOM] = true;
        FRAGILE[POT] = true;
        NEEDS_GROUND[TUFT] = true;
        NEEDS_GROUND[FLOWER] = true;
        NEEDS_GROUND[SAPLING] = true;
        NEEDS_GROUND[MUSHROOM] = true;
        for (int t : new int[]{VINE, JUNGLE_PLANT, CORRUPT_PLANT, JUNGLE_SPORE}) FRAGILE[t] = true;
        NEEDS_GROUND[JUNGLE_PLANT] = true;
        NEEDS_GROUND[CORRUPT_PLANT] = true;
        NEEDS_GROUND[JUNGLE_SPORE] = true;
        FRAGILE[MOSS_PLANT] = true;
        NEEDS_GROUND[MOSS_PLANT] = true;
        PLACE_OVER[MOSS_PLANT] = true;
        TRANSPARENT[GLASS] = true;
        PLACE_OVER[JUNGLE_PLANT] = true;
        PLACE_OVER[CORRUPT_PLANT] = true;
        PLACE_OVER[AIR] = true;
        PLACE_OVER[TUFT] = true;
        PLACE_OVER[FLOWER] = true;

        light(TORCH, 15, TORCH_LIGHT);
        light(FURNACE, 13, 0xffb878);
        light(LIFE_CRYSTAL, 9, 0xff6a9a);
        light(HELLSTONE, 7, 0xff5a20);
        light(JUNGLE_SPORE, 6, 0x9aff70);
        light(DEMONITE, 4, 0x9a70ff);
        light(RUBY, 4, 0xff5a6a);
        light(SAPPHIRE, 4, 0x5a8aff);
        light(EMERALD, 4, 0x5aff8a);
        light(TOPAZ, 4, 0xffc84a);

        furniture(CHEST, 2, 2);
        furniture(GOLD_CHEST, 2, 2);
        furniture(ICE_CHEST, 2, 2);
        furniture(DOOR_CLOSED, 1, 3);
        furniture(DOOR_OPEN, 2, 3);
        furniture(TABLE, 3, 2);
        furniture(CHAIR, 1, 2);
        furniture(BED, 4, 2);
        PLAT_TOP[TABLE] = true;
        furniture(WORKBENCH, 2, 1);
        furniture(FURNACE, 3, 2);
        furniture(ANVIL, 2, 1);
        furniture(LIFE_CRYSTAL, 2, 2);
        furniture(POT, 2, 2);
        PLAT_TOP[WORKBENCH] = true;
        PLAT_TOP[ANVIL] = true;

        for (int t : new int[]{DIRT, GRASS, MUD, JUNGLE_GRASS, CORRUPT_GRASS, ASH, SNOW}) BLEED_SRC[t] = true;
        for (int t : new int[]{STONE, COPPER, IRON, SILVER, GOLD, CLAY, SAND, EBONSTONE, DEMONITE, HELLSTONE, EBONSAND,
                RUBY, SAPPHIRE, EMERALD, TOPAZ, ICE, GREEN_MOSS, BROWN_MOSS, RED_MOSS, BLUE_MOSS, PURPLE_MOSS})
            BLEED_DST[t] = true;

        wall(W_NONE, "", 0, 0, 0, false);
        wall(W_DIRT, "Dirt Wall", 30, Items.DIRT_WALL, Pal.DIRT_D, false);
        wall(W_STONE, "Stone Wall", 40, Items.STONE_WALL, Pal.STONE_D, false);
        wall(W_WOOD, "Wood Wall", 30, Items.WOOD_WALL, Pal.WOOD_D, false);
        wall(W_DIRT_N, "Dirt Wall", 30, Items.DIRT_WALL, Pal.DIRT_D, true);
        wall(W_STONE_N, "Stone Wall", 40, Items.STONE_WALL, Pal.STONE_D, true);
        wall(W_GLASS, "Glass Wall", 20, Items.GLASS_WALL, Pal.GLASS, false);
        wall(W_GRAY_BRICK, "Gray Brick Wall", 45, Items.GRAY_BRICK_WALL, Pal.BRICK_G_D, false);
        wall(W_RED_BRICK, "Red Brick Wall", 45, Items.RED_BRICK_WALL, Pal.BRICK_R_D, false);
        wall(W_MUD_N, "Mud Wall", 30, 0, Pal.MUD_D, true);
        wall(W_EBON_N, "Ebonstone Wall", 40, 0, Pal.EBON_D, true);
        wall(W_MAHOGANY, "Rich Mahogany Wall", 30, Items.MAHOGANY_WALL, Pal.MAHO_D, false);
        wall(W_OBSIDIAN_BRICK, "Obsidian Brick Wall", 60, Items.OBSIDIAN_BRICK_WALL, Pal.OBS_D, false);
        wall(W_HELLSTONE_BRICK, "Hellstone Brick Wall", 60, Items.HELLSTONE_BRICK_WALL, Pal.HELL_D, false);
        wall(W_SNOW_N, "Snow Wall", 30, 0, 0x8a98b4, true);
        wall(W_ICE_N, "Ice Wall", 40, 0, 0x5a7aa0, true);
        wall(W_BOREAL, "Boreal Wood Wall", 30, Items.BOREAL_WALL, Pal.BOREAL_D, false);

        TileArt.build();
    }

    static boolean isFurniture(int t) {
        return FURN_W[t] > 0;
    }

    static boolean isTrunk(int t) {
        return t == TRUNK || t == MAHOGANY_TRUNK || t == EBON_TRUNK || t == BOREAL_TRUNK;
    }

    static boolean isLeaves(int t) {
        return t == LEAVES || t == MAHOGANY_LEAVES || t == EBON_LEAVES || t == BOREAL_LEAVES;
    }

    /** Leaves that belong to a trunk type. */
    static int leavesOf(int trunk) {
        return trunk == MAHOGANY_TRUNK ? MAHOGANY_LEAVES : trunk == EBON_TRUNK ? EBON_LEAVES
                : trunk == BOREAL_TRUNK ? BOREAL_LEAVES : LEAVES;
    }

    static boolean isGrass(int t) {
        return t == GRASS || t == JUNGLE_GRASS || t == CORRUPT_GRASS;
    }

    /** Ground a tree can grow on (grass, or snow for boreal trees). */
    static boolean isTreeGround(int t) {
        return isGrass(t) || t == SNOW;
    }

    static boolean isMoss(int t) {
        return MOSS[t] != null;
    }

    static boolean isGem(int t) {
        return t >= RUBY && t <= TOPAZ;
    }

    /** Plain or moss-covered stone (moss grows on it, gems and ores replace it). */
    static boolean isStone(int t) {
        return t == STONE || isMoss(t);
    }

    static boolean isChest(int t) {
        return t == CHEST || t == GOLD_CHEST || t == ICE_CHEST;
    }

    static boolean isDoor(int t) {
        return t == DOOR_CLOSED || t == DOOR_OPEN;
    }

    /** Whether the tile gives light to a house (torches, furnaces...). */
    static boolean isLightSource(int t) {
        return LIGHT[t] >= 9;
    }

    /** Whether light is dimmed strongly by this tile. */
    static boolean blocksLight(int t) {
        return SOLID[t] && !TRANSPARENT[t];
    }

    /** Colour of a tile pixel: (x, y) inside the tile, (wx, wy) in world pixels. */
    static int texel(int t, int variant, int x, int y, int wx, int wy) {
        Sprite m = macro[t];
        if (m != null) return m.p[(wx & (m.w - 1)) + (wy & (m.h - 1)) * m.w];
        Sprite[] v = tex[t];
        return v[variant & (v.length - 1)].p[x + y * T];
    }

    /** Colour of a wall pixel (walls are world-aligned 16x16 textures). */
    static int wallTexel(int wl, int wx, int wy) {
        Sprite s = wallTex[wl][0];
        return s.p[(wx & (s.w - 1)) + (wy & (s.h - 1)) * s.w];
    }
}
