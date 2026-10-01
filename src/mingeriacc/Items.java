package mingeriacc;

/**
 * Item definitions as parallel arrays indexed by ID. New IDs are always added
 * at the end: saves store raw IDs. Icons are drawn in ItemArt.
 */
final class Items {
    private Items() {}

    static final int NONE = 0, DIRT = 1, STONE = 2, SAND = 3, CLAY = 4, COPPER_ORE = 5,
            IRON_ORE = 6, SILVER_ORE = 7, GOLD_ORE = 8, WOOD = 9, TORCH = 10, PLATFORM = 11,
            ACORN = 12, DIRT_WALL = 13, STONE_WALL = 14, WOOD_WALL = 15, COPPER_PICK = 16,
            COPPER_AXE = 17, WOOD_HAMMER = 18,
            WORKBENCH = 19, FURNACE = 20, IRON_ANVIL = 21,
            COPPER_BAR = 22, IRON_BAR = 23, SILVER_BAR = 24, GOLD_BAR = 25,
            GEL = 26, LENS = 27,
            COPPER_COIN = 28, SILVER_COIN = 29, GOLD_COIN = 30, PLATINUM_COIN = 31,
            WOOD_SWORD = 32, COPPER_SHORTSWORD = 33, COPPER_SWORD = 34, IRON_SWORD = 35,
            SILVER_SWORD = 36, GOLD_SWORD = 37,
            WOOD_BOW = 38, COPPER_BOW = 39, IRON_BOW = 40, SILVER_BOW = 41, GOLD_BOW = 42, WOOD_ARROW = 43,
            IRON_PICK = 44, SILVER_PICK = 45, GOLD_PICK = 46,
            IRON_AXE = 47, SILVER_AXE = 48, GOLD_AXE = 49,
            COPPER_HAMMER = 50, IRON_HAMMER = 51, SILVER_HAMMER = 52, GOLD_HAMMER = 53,
            LIFE_CRYSTAL = 54, MUSHROOM = 55, HEALING_POTION = 56, BOTTLE = 57, GLASS = 58,
            GRAY_BRICK = 59, RED_BRICK = 60, GLASS_WALL = 61, GRAY_BRICK_WALL = 62, RED_BRICK_WALL = 63,
            MUD = 64, EBONSTONE = 65, EBONSAND = 66, ASH = 67, HELLSTONE = 68, OBSIDIAN = 69, DEMONITE_ORE = 70,
            DEMONITE_BAR = 71, HELLSTONE_BAR = 72, MAHOGANY = 73, EBONWOOD = 74, VINE = 75, JUNGLE_SPORES = 76,
            STINGER = 77, ROTTEN_CHUNK = 78, CHEST = 79, GOLD_CHEST = 80, DOOR = 81, TABLE = 82, CHAIR = 83,
            BED = 84, OBSIDIAN_BRICK = 85, HELLSTONE_BRICK = 86, EMPTY_BUCKET = 87, WATER_BUCKET = 88,
            LAVA_BUCKET = 89, NIGHTMARE_PICK = 90, WAR_AXE = 91, THE_BREAKER = 92, LIGHTS_BANE = 93,
            DEMON_BOW = 94, MOLTEN_PICK = 95, FIERY_GREATSWORD = 96, MOLTEN_FURY = 97, BLADE_OF_GRASS = 98,
            FLAMING_ARROW = 99, COPPER_HELMET = 100, COPPER_MAIL = 101, COPPER_GREAVES = 102,
            IRON_HELMET = 103, IRON_MAIL = 104, IRON_GREAVES = 105, SILVER_HELMET = 106, SILVER_MAIL = 107,
            SILVER_GREAVES = 108, GOLD_HELMET = 109, GOLD_MAIL = 110, GOLD_GREAVES = 111, SHADOW_HELMET = 112,
            SHADOW_MAIL = 113, SHADOW_GREAVES = 114, MOLTEN_HELMET = 115, MOLTEN_MAIL = 116,
            MOLTEN_GREAVES = 117, MINING_HELMET = 118, HERMES_BOOTS = 119, CLOUD_BOTTLE = 120,
            BAND_REGEN = 121, HORSESHOE = 122, AGLET = 123, BALLOON = 124, ANKLET = 125, OBSIDIAN_SKULL = 126,
            FLIPPER = 127, MAHOGANY_WALL = 128, OBSIDIAN_BRICK_WALL = 129, HELLSTONE_BRICK_WALL = 130,
            RUBY = 131, SAPPHIRE = 132, EMERALD = 133, TOPAZ = 134,
            FLAME_STAFF = 135, TIDE_STAFF = 136, QUAKE_STAFF = 137, STORM_STAFF = 138,
            FALLEN_STAR = 139, MANA_CRYSTAL = 140, MANA_POTION = 141, BAND_STARPOWER = 142, ARCANE_HAT = 143,
            MAGIC_MIRROR = 144, SUSPICIOUS_EYE = 145, UNHOLY_ARROW = 146, EYE_SHIELD = 147,
            SNOW_BLOCK = 148, ICE_BLOCK = 149, BOREAL_WOOD = 150, BOREAL_WALL = 151, ICE_BLADE = 152,
            ICE_SKATES = 153, BLIZZARD_BOTTLE = 154, ICE_CHEST = 155,
            GRASS_SEEDS = 156, JUNGLE_SEEDS = 157, PURIFICATION_POWDER = 158, HEART = 159, MANA_STAR = 160;
    static final int COUNT = 161;

    // Item kinds
    static final int K_MATERIAL = 0, K_TILE = 1, K_WALL = 2, K_TOOL = 3, K_WEAPON = 4, K_AMMO = 5,
            K_CONSUMABLE = 6, K_COIN = 7, K_ARMOR = 8, K_ACCESSORY = 9, K_BUCKET = 10,
            K_SEED = 11,  // turns the aimed tile into PLACE (grass seeds)
            K_USE = 12,   // special use (mirror, summoning, powder): Game.useSpecial
            K_PICKUP = 13; // used up at once when picked up (hearts, mana stars)

    // Armour slots
    static final int HEAD = 0, BODY = 1, LEGS = 2;

    // Weapon effects on hit
    static final int FX_NONE = 0, FX_FIRE = 1, FX_POISON = 2, FX_FROST = 3;

    // Use styles
    static final int S_SWING = 0, S_STAB = 1, S_SHOOT = 2, S_EAT = 3, S_HOLDUP = 4, S_CAST = 5;

    // Ammo classes
    static final int A_NONE = 0, A_ARROW = 1;

    static final String[] NAME = new String[COUNT];
    static final String[] DESC = new String[COUNT];
    static final int[] KIND = new int[COUNT];
    static final int[] MAX_STACK = new int[COUNT];
    static final int[] PLACE = new int[COUNT];     // tile or wall it places
    static final int[] TOOL = new int[COUNT];      // tool type
    static final int[] POWER = new int[COUNT];     // tool power
    static final int[] USE_TIME = new int[COUNT];  // use time in frames
    static final int[] STYLE = new int[COUNT];
    static final int[] DAMAGE = new int[COUNT];
    static final double[] KNOCK = new double[COUNT];
    static final int[] CRIT = new int[COUNT];      // critical hit chance in percent
    static final int[] AMMO = new int[COUNT];      // ammo class of this item
    static final int[] USE_AMMO = new int[COUNT];  // ammo class a weapon shoots
    static final int[] PROJ = new int[COUNT];      // projectile an ammo item becomes
    static final double[] SHOOT_SPEED = new double[COUNT];
    static final int[] VALUE = new int[COUNT];     // in copper coins
    static final int[] HEAL = new int[COUNT];
    static final int[] RARITY = new int[COUNT];
    static final int[] SLOT = new int[COUNT];      // armour slot
    static final int[] DEFENSE = new int[COUNT];
    static final int[] EFFECT = new int[COUNT];    // debuff a weapon or ammo inflicts
    static final int[] MANA = new int[COUNT];      // mana a magic weapon uses
    static final int[] PIERCE = new int[COUNT];    // enemies an ammo's projectile passes through
    static final int[][] ARMOR_RAMP = new int[COUNT][]; // armour colours for drawing
    static final Sprite[] ICON = new Sprite[COUNT];
    static final Sprite[] SMALL = new Sprite[COUNT]; // item lying on the ground

    private static void def(int id, String name, String desc, int kind, int stack, int place, int useTime, int value) {
        NAME[id] = name;
        DESC[id] = desc;
        KIND[id] = kind;
        MAX_STACK[id] = stack;
        PLACE[id] = place;
        USE_TIME[id] = useTime;
        VALUE[id] = value;
        STYLE[id] = S_SWING;
    }

    private static void tool(int id, String name, int tool, int power, int useTime, int damage, double knock, int value) {
        String desc = tool == Tiles.TOOL_PICK ? "Mines dirt, stone and ores" : tool == Tiles.TOOL_AXE ? "Chops down trees"
                : "Breaks background walls";
        def(id, name, desc, K_TOOL, 1, 0, useTime, value);
        TOOL[id] = tool;
        POWER[id] = power;
        DAMAGE[id] = damage;
        KNOCK[id] = knock;
        CRIT[id] = 4;
    }

    private static void sword(int id, String name, int damage, double knock, int useTime, int value) {
        def(id, name, "", K_WEAPON, 1, 0, useTime, value);
        DAMAGE[id] = damage;
        KNOCK[id] = knock;
        CRIT[id] = 4;
    }

    private static void bow(int id, String name, int damage, int useTime, double speed, int value) {
        def(id, name, "Shoots arrows", K_WEAPON, 1, 0, useTime, value);
        STYLE[id] = S_SHOOT;
        DAMAGE[id] = damage;
        KNOCK[id] = 0.5;
        CRIT[id] = 4;
        USE_AMMO[id] = A_ARROW;
        SHOOT_SPEED[id] = speed;
    }

    /** A magic staff: held towards the mouse, uses mana. proj 0 = lightning (Magic.lightning). */
    private static void staff(int id, String name, String desc, int damage, int mana, int useTime, double speed,
                              int proj, int value, int rarity) {
        def(id, name, desc, K_WEAPON, 1, 0, useTime, value);
        STYLE[id] = S_CAST;
        DAMAGE[id] = damage;
        MANA[id] = mana;
        KNOCK[id] = 3;
        CRIT[id] = 4;
        SHOOT_SPEED[id] = speed;
        PROJ[id] = proj;
        RARITY[id] = rarity;
    }

    private static void armor(int first, String metal, int[] ramp, int def0, int def1, int def2, int value, int rarity) {
        String[] parts = {" Helmet", " Chainmail", " Greaves"};
        int[] def = {def0, def1, def2};
        for (int i = 0; i < 3; i++) {
            int id = first + i;
            def(id, metal + parts[i], "", K_ARMOR, 1, 0, 0, value * (i == 1 ? 5 : 4) / 4);
            SLOT[id] = i;
            DEFENSE[id] = def[i];
            ARMOR_RAMP[id] = ramp;
            RARITY[id] = rarity;
        }
    }

    private static void accessory(int id, String name, String desc, int value) {
        def(id, name, desc, K_ACCESSORY, 1, 0, 0, value);
        RARITY[id] = 2;
    }

    static {
        def(NONE, "", "", K_MATERIAL, 0, 0, 0, 0);
        def(DIRT, "Dirt", "Can be placed", K_TILE, 999, Tiles.DIRT, 8, 0);
        def(STONE, "Stone", "Can be placed", K_TILE, 999, Tiles.STONE, 8, 0);
        def(SAND, "Sand", "Can be placed. Smelts into glass", K_TILE, 999, Tiles.SAND, 8, 0);
        def(CLAY, "Clay", "Can be placed. Fires into red bricks", K_TILE, 999, Tiles.CLAY, 8, 0);
        def(COPPER_ORE, "Copper Ore", "Smelt into bars at a furnace", K_TILE, 999, Tiles.COPPER, 8, 50);
        def(IRON_ORE, "Iron Ore", "Smelt into bars at a furnace", K_TILE, 999, Tiles.IRON, 8, 100);
        def(SILVER_ORE, "Silver Ore", "Smelt into bars at a furnace", K_TILE, 999, Tiles.SILVER, 8, 150);
        def(GOLD_ORE, "Gold Ore", "Smelt into bars at a furnace", K_TILE, 999, Tiles.GOLD, 8, 300);
        def(WOOD, "Wood", "Building material. Places as planks", K_TILE, 999, Tiles.PLANKS, 8, 0);
        def(TORCH, "Torch", "Gives light. Hold it or place it", K_TILE, 999, Tiles.TORCH, 8, 50);
        def(PLATFORM, "Wood Platform", "Jump through from below. S = drop down", K_TILE, 999, Tiles.PLATFORM, 8, 0);
        def(ACORN, "Acorn", "Plant on grass to grow a tree", K_TILE, 999, Tiles.SAPLING, 12, 0);
        def(DIRT_WALL, "Dirt Wall", "Background wall", K_WALL, 999, Tiles.W_DIRT, 7, 0);
        def(STONE_WALL, "Stone Wall", "Background wall", K_WALL, 999, Tiles.W_STONE, 7, 0);
        def(WOOD_WALL, "Wood Wall", "Background wall", K_WALL, 999, Tiles.W_WOOD, 7, 0);
        tool(COPPER_PICK, "Copper Pickaxe", Tiles.TOOL_PICK, 35, 16, 4, 2, 100);
        tool(COPPER_AXE, "Copper Axe", Tiles.TOOL_AXE, 40, 20, 5, 4.5, 80);
        tool(WOOD_HAMMER, "Wooden Hammer", Tiles.TOOL_HAMMER, 25, 18, 2, 5.5, 10);

        def(WORKBENCH, "Work Bench", "Used for basic crafting", K_TILE, 99, Tiles.WORKBENCH, 14, 30);
        def(FURNACE, "Furnace", "Used for smelting ore", K_TILE, 99, Tiles.FURNACE, 14, 300);
        def(IRON_ANVIL, "Iron Anvil", "Used to craft items from metal bars", K_TILE, 99, Tiles.ANVIL, 14, 1500);
        def(COPPER_BAR, "Copper Bar", "Crafted into tools at an anvil", K_MATERIAL, 999, 0, 0, 150);
        def(IRON_BAR, "Iron Bar", "Crafted into tools at an anvil", K_MATERIAL, 999, 0, 0, 300);
        def(SILVER_BAR, "Silver Bar", "Crafted into tools at an anvil", K_MATERIAL, 999, 0, 0, 600);
        def(GOLD_BAR, "Gold Bar", "Crafted into tools at an anvil", K_MATERIAL, 999, 0, 0, 1200);
        def(GEL, "Gel", "Both tasty and flammable", K_MATERIAL, 999, 0, 0, 5);
        def(LENS, "Lens", "It stares back at you", K_MATERIAL, 99, 0, 0, 100);
        def(COPPER_COIN, "Copper Coin", "100 make a silver coin", K_COIN, 100, 0, 0, 1);
        def(SILVER_COIN, "Silver Coin", "100 make a gold coin", K_COIN, 100, 0, 0, 100);
        def(GOLD_COIN, "Gold Coin", "100 make a platinum coin", K_COIN, 100, 0, 0, 10000);
        def(PLATINUM_COIN, "Platinum Coin", "Very shiny", K_COIN, 999, 0, 0, 1000000);

        sword(WOOD_SWORD, "Wooden Sword", 7, 5, 25, 100);
        sword(COPPER_SHORTSWORD, "Copper Shortsword", 5, 4, 13, 70);
        STYLE[COPPER_SHORTSWORD] = S_STAB;
        DESC[COPPER_SHORTSWORD] = "Stabs towards the mouse";
        sword(COPPER_SWORD, "Copper Broadsword", 8, 5, 23, 450);
        sword(IRON_SWORD, "Iron Broadsword", 10, 5.5, 21, 1000);
        sword(SILVER_SWORD, "Silver Broadsword", 11, 6, 21, 1800);
        sword(GOLD_SWORD, "Gold Broadsword", 13, 6, 20, 3000);

        bow(WOOD_BOW, "Wooden Bow", 4, 28, 4.2, 100);
        bow(COPPER_BOW, "Copper Bow", 6, 27, 4.4, 350);
        bow(IRON_BOW, "Iron Bow", 8, 26, 4.6, 800);
        bow(SILVER_BOW, "Silver Bow", 9, 25, 4.8, 1400);
        bow(GOLD_BOW, "Gold Bow", 11, 24, 5.0, 2400);
        def(WOOD_ARROW, "Wooden Arrow", "Ammunition for bows", K_AMMO, 999, 0, 0, 5);
        AMMO[WOOD_ARROW] = A_ARROW;
        DAMAGE[WOOD_ARROW] = 5;
        PROJ[WOOD_ARROW] = Projectile.WOOD_ARROW;
        SHOOT_SPEED[WOOD_ARROW] = 1.5;

        tool(IRON_PICK, "Iron Pickaxe", Tiles.TOOL_PICK, 40, 15, 5, 2, 500);
        tool(SILVER_PICK, "Silver Pickaxe", Tiles.TOOL_PICK, 45, 14, 6, 2, 1000);
        tool(GOLD_PICK, "Gold Pickaxe", Tiles.TOOL_PICK, 55, 13, 6, 2, 2000);
        tool(IRON_AXE, "Iron Axe", Tiles.TOOL_AXE, 45, 19, 6, 4.5, 400);
        tool(SILVER_AXE, "Silver Axe", Tiles.TOOL_AXE, 50, 18, 7, 4.5, 800);
        tool(GOLD_AXE, "Gold Axe", Tiles.TOOL_AXE, 55, 17, 8, 4.5, 1600);
        tool(COPPER_HAMMER, "Copper Hammer", Tiles.TOOL_HAMMER, 35, 17, 4, 5.5, 80);
        tool(IRON_HAMMER, "Iron Hammer", Tiles.TOOL_HAMMER, 40, 16, 7, 5.5, 400);
        tool(SILVER_HAMMER, "Silver Hammer", Tiles.TOOL_HAMMER, 45, 15, 8, 5.5, 800);
        tool(GOLD_HAMMER, "Gold Hammer", Tiles.TOOL_HAMMER, 55, 14, 9, 5.5, 1600);

        def(LIFE_CRYSTAL, "Life Crystal", "Permanently increases maximum life by 20", K_CONSUMABLE, 99, 0, 30, 7500);
        STYLE[LIFE_CRYSTAL] = S_HOLDUP;
        RARITY[LIFE_CRYSTAL] = 2;
        def(MUSHROOM, "Mushroom", "Heals 15 life", K_CONSUMABLE, 99, 0, 17, 25);
        STYLE[MUSHROOM] = S_EAT;
        HEAL[MUSHROOM] = 15;
        def(HEALING_POTION, "Lesser Healing Potion", "Heals 50 life. H = quick heal", K_CONSUMABLE, 30, 0, 17, 300);
        STYLE[HEALING_POTION] = S_EAT;
        HEAL[HEALING_POTION] = 50;
        def(BOTTLE, "Bottle", "Used for making potions", K_MATERIAL, 99, 0, 0, 20);
        def(GLASS, "Glass", "Can be placed. Light shines through", K_TILE, 999, Tiles.GLASS, 8, 0);
        def(GRAY_BRICK, "Gray Brick", "Sturdy building block", K_TILE, 999, Tiles.GRAY_BRICK, 8, 0);
        def(RED_BRICK, "Red Brick", "Sturdy building block", K_TILE, 999, Tiles.RED_BRICK, 8, 0);
        def(GLASS_WALL, "Glass Wall", "Background wall", K_WALL, 999, Tiles.W_GLASS, 7, 0);
        def(GRAY_BRICK_WALL, "Gray Brick Wall", "Background wall", K_WALL, 999, Tiles.W_GRAY_BRICK, 7, 0);
        def(RED_BRICK_WALL, "Red Brick Wall", "Background wall", K_WALL, 999, Tiles.W_RED_BRICK, 7, 0);

        def(MUD, "Mud", "Can be placed", K_TILE, 999, Tiles.MUD, 8, 0);
        def(EBONSTONE, "Ebonstone Block", "Can be placed", K_TILE, 999, Tiles.EBONSTONE, 8, 0);
        def(EBONSAND, "Ebonsand Block", "Can be placed", K_TILE, 999, Tiles.EBONSAND, 8, 0);
        def(ASH, "Ash Block", "Can be placed", K_TILE, 999, Tiles.ASH, 8, 0);
        def(HELLSTONE, "Hellstone", "Hot to the touch", K_TILE, 999, Tiles.HELLSTONE, 8, 2000);
        def(OBSIDIAN, "Obsidian", "Forms where water meets lava", K_TILE, 999, Tiles.OBSIDIAN, 8, 500);
        def(DEMONITE_ORE, "Demonite Ore", "Pulsing with dark energy", K_TILE, 999, Tiles.DEMONITE, 8, 1000);
        def(DEMONITE_BAR, "Demonite Bar", "Crafted into shadow gear at an anvil", K_MATERIAL, 999, 0, 0, 4000);
        def(HELLSTONE_BAR, "Hellstone Bar", "Crafted into molten gear at an anvil", K_MATERIAL, 999, 0, 0, 8000);
        def(MAHOGANY, "Rich Mahogany", "Jungle wood. Works as wood in recipes", K_TILE, 999, Tiles.MAHOGANY_PLANKS, 8, 0);
        def(EBONWOOD, "Ebonwood", "Corrupt wood. Works as wood in recipes", K_TILE, 999, Tiles.EBON_PLANKS, 8, 0);
        def(VINE, "Vine", "Jungle material", K_MATERIAL, 99, 0, 0, 100);
        def(JUNGLE_SPORES, "Jungle Spores", "Glowing jungle material", K_MATERIAL, 99, 0, 0, 200);
        def(STINGER, "Stinger", "Dropped by hornets", K_MATERIAL, 99, 0, 0, 200);
        def(ROTTEN_CHUNK, "Rotten Chunk", "Smells awful", K_MATERIAL, 99, 0, 0, 50);
        def(CHEST, "Chest", "Stores items. Right-click to open", K_TILE, 99, Tiles.CHEST, 14, 500);
        def(GOLD_CHEST, "Gold Chest", "Stores items. Right-click to open", K_TILE, 99, Tiles.GOLD_CHEST, 14, 5000);
        def(DOOR, "Wooden Door", "Right-click to open or close", K_TILE, 99, Tiles.DOOR_CLOSED, 14, 200);
        def(TABLE, "Wooden Table", "Furniture for houses", K_TILE, 99, Tiles.TABLE, 14, 300);
        def(CHAIR, "Wooden Chair", "Furniture for houses. Right-click to check the room", K_TILE, 99, Tiles.CHAIR, 14, 150);
        def(BED, "Bed", "Right-click to set your spawn point", K_TILE, 99, Tiles.BED, 14, 2000);
        def(OBSIDIAN_BRICK, "Obsidian Brick", "Very sturdy", K_TILE, 999, Tiles.OBSIDIAN_BRICK, 8, 0);
        def(HELLSTONE_BRICK, "Hellstone Brick", "Warm building block", K_TILE, 999, Tiles.HELLSTONE_BRICK, 8, 0);
        def(EMPTY_BUCKET, "Empty Bucket", "Scoops up water or lava", K_BUCKET, 99, 0, 15, 300);
        def(WATER_BUCKET, "Water Bucket", "Pours out water", K_BUCKET, 99, 0, 15, 300);
        def(LAVA_BUCKET, "Lava Bucket", "Pours out lava", K_BUCKET, 99, 0, 15, 300);
        tool(NIGHTMARE_PICK, "Nightmare Pickaxe", Tiles.TOOL_PICK, 65, 13, 9, 3, 15000);
        tool(WAR_AXE, "War Axe of the Night", Tiles.TOOL_AXE, 65, 17, 17, 6, 12000);
        tool(THE_BREAKER, "The Breaker", Tiles.TOOL_HAMMER, 70, 16, 24, 6, 12000);
        tool(MOLTEN_PICK, "Molten Pickaxe", Tiles.TOOL_PICK, 100, 12, 12, 2, 30000);
        sword(LIGHTS_BANE, "Light's Bane", 17, 5, 20, 13500);
        sword(BLADE_OF_GRASS, "Blade of Grass", 28, 3, 30, 27000);
        DESC[BLADE_OF_GRASS] = "May poison enemies";
        EFFECT[BLADE_OF_GRASS] = FX_POISON;
        sword(FIERY_GREATSWORD, "Fiery Greatsword", 36, 6.5, 34, 27000);
        DESC[FIERY_GREATSWORD] = "Sets enemies on fire";
        EFFECT[FIERY_GREATSWORD] = FX_FIRE;
        bow(DEMON_BOW, "Demon Bow", 14, 23, 5.4, 9000);
        bow(MOLTEN_FURY, "Molten Fury", 29, 22, 6.0, 27000);
        DESC[MOLTEN_FURY] = "Arrows it shoots catch fire";
        EFFECT[MOLTEN_FURY] = FX_FIRE;
        def(FLAMING_ARROW, "Flaming Arrow", "Sets enemies on fire", K_AMMO, 999, 0, 0, 10);
        AMMO[FLAMING_ARROW] = A_ARROW;
        DAMAGE[FLAMING_ARROW] = 7;
        PROJ[FLAMING_ARROW] = Projectile.FLAMING_ARROW;
        SHOOT_SPEED[FLAMING_ARROW] = 1.8;
        EFFECT[FLAMING_ARROW] = FX_FIRE;
        for (int id : new int[]{NIGHTMARE_PICK, WAR_AXE, THE_BREAKER, LIGHTS_BANE, DEMON_BOW}) RARITY[id] = 1;
        for (int id : new int[]{MOLTEN_PICK, FIERY_GREATSWORD, MOLTEN_FURY, BLADE_OF_GRASS}) RARITY[id] = 3;

        armor(COPPER_HELMET, "Copper", ItemArt.COPPER, 1, 2, 1, 1000, 0);
        armor(IRON_HELMET, "Iron", ItemArt.IRON, 2, 3, 2, 2000, 0);
        armor(SILVER_HELMET, "Silver", ItemArt.SILVER, 3, 4, 3, 3500, 1);
        armor(GOLD_HELMET, "Gold", ItemArt.GOLD, 4, 5, 4, 6000, 1);
        armor(SHADOW_HELMET, "Shadow", ItemArt.SHADOW, 6, 7, 6, 12000, 1);
        armor(MOLTEN_HELMET, "Molten", ItemArt.MOLTEN, 8, 9, 8, 25000, 3);
        NAME[SHADOW_MAIL] = "Shadow Scalemail";
        def(MINING_HELMET, "Mining Helmet", "Gives light when worn", K_ARMOR, 1, 0, 0, 20000);
        SLOT[MINING_HELMET] = HEAD;
        DEFENSE[MINING_HELMET] = 1;
        ARMOR_RAMP[MINING_HELMET] = ItemArt.MINING;
        accessory(HERMES_BOOTS, "Hermes Boots", "Run much faster after a moment", 5000);
        accessory(CLOUD_BOTTLE, "Cloud in a Bottle", "Allows a double jump", 5000);
        accessory(BAND_REGEN, "Band of Regeneration", "Slowly regenerates life", 5000);
        accessory(HORSESHOE, "Lucky Horseshoe", "No fall damage", 5000);
        accessory(AGLET, "Aglet", "5% faster movement", 2000);
        accessory(BALLOON, "Shiny Red Balloon", "Jump higher", 5000);
        accessory(ANKLET, "Anklet of the Wind", "10% faster movement", 5000);
        accessory(OBSIDIAN_SKULL, "Obsidian Skull", "Immune to burning", 5000);
        accessory(FLIPPER, "Flipper", "Swim freely in water", 5000);
        def(MAHOGANY_WALL, "Rich Mahogany Wall", "Background wall", K_WALL, 999, Tiles.W_MAHOGANY, 7, 0);
        def(OBSIDIAN_BRICK_WALL, "Obsidian Brick Wall", "Background wall", K_WALL, 999, Tiles.W_OBSIDIAN_BRICK, 7, 0);
        def(HELLSTONE_BRICK_WALL, "Hellstone Brick Wall", "Background wall", K_WALL, 999, Tiles.W_HELLSTONE_BRICK, 7, 0);

        for (int id : new int[]{COPPER_BAR, IRON_BAR, SILVER_BAR, GOLD_BAR, IRON_ANVIL, FURNACE}) RARITY[id] = 0;
        for (int id : new int[]{GOLD_SWORD, GOLD_BOW, GOLD_PICK, GOLD_AXE, GOLD_HAMMER, SILVER_SWORD, SILVER_BOW})
            RARITY[id] = 1;
        defineV4();

        ItemArt.build();
    }

    /** Items of version 0.4: gems, magic, the first boss, snow and the dryad's goods. */
    private static void defineV4() {
        String[] gems = {"Ruby", "Sapphire", "Emerald", "Topaz"};
        String[] gemUse = {"Fire", "Water", "Earth", "Lightning"};
        for (int k = 0; k < 4; k++) {
            def(RUBY + k, gems[k], "A precious gem. Holds the power of " + gemUse[k].toLowerCase(), K_MATERIAL, 99, 0, 0, 1500);
            RARITY[RUBY + k] = 1;
        }
        staff(FLAME_STAFF, "Flame Staff", "Shoots a bolt of fire that burns enemies", 11, 4, 24, 4.2,
                Projectile.FLAME_BOLT, 4000, 1);
        EFFECT[FLAME_STAFF] = FX_FIRE;
        staff(TIDE_STAFF, "Tide Staff", "Shoots a water bolt that bounces off walls", 15, 6, 22, 3.6,
                Projectile.WATER_BOLT, 7000, 1);
        staff(QUAKE_STAFF, "Quake Staff", "Hurls a boulder that shatters into shards", 24, 9, 30, 4.0,
                Projectile.ROCK, 11000, 2);
        KNOCK[QUAKE_STAFF] = 6;
        staff(STORM_STAFF, "Storm Staff", "Lightning strikes the enemy nearest the mouse and jumps on", 21, 10, 28, 0,
                0, 16000, 2);
        def(FALLEN_STAR, "Fallen Star", "Fades away at sunrise. Five make a mana crystal", K_MATERIAL, 99, 0, 0, 500);
        def(MANA_CRYSTAL, "Mana Crystal", "Permanently increases maximum mana by 20", K_CONSUMABLE, 99, 0, 30, 2500);
        STYLE[MANA_CRYSTAL] = S_HOLDUP;
        RARITY[MANA_CRYSTAL] = 2;
        def(MANA_POTION, "Lesser Mana Potion", "Restores 50 mana. Drunk by itself when you run out", K_CONSUMABLE,
                30, 0, 17, 250);
        STYLE[MANA_POTION] = S_EAT;
        accessory(BAND_STARPOWER, "Band of Starpower", "Increases maximum mana by 20", 15000);
        def(ARCANE_HAT, "Arcane Hat", "10% more magic damage, 20 more maximum mana", K_ARMOR, 1, 0, 0, 20000);
        SLOT[ARCANE_HAT] = HEAD;
        DEFENSE[ARCANE_HAT] = 2;
        ARMOR_RAMP[ARCANE_HAT] = ItemArt.ARCANE;
        RARITY[ARCANE_HAT] = 1;
        def(MAGIC_MIRROR, "Magic Mirror", "Gaze into the mirror to return home", K_USE, 1, 0, 60, 30000);
        STYLE[MAGIC_MIRROR] = S_HOLDUP;
        RARITY[MAGIC_MIRROR] = 1;
        def(SUSPICIOUS_EYE, "Suspicious Looking Eye", "Summons the Eye of Cthulhu. Works only at night", K_USE, 20, 0,
                45, 0);
        STYLE[SUSPICIOUS_EYE] = S_HOLDUP;
        RARITY[SUSPICIOUS_EYE] = 1;
        def(UNHOLY_ARROW, "Unholy Arrow", "Pierces through enemies", K_AMMO, 999, 0, 0, 40);
        AMMO[UNHOLY_ARROW] = A_ARROW;
        DAMAGE[UNHOLY_ARROW] = 8;
        PROJ[UNHOLY_ARROW] = Projectile.UNHOLY_ARROW;
        SHOOT_SPEED[UNHOLY_ARROW] = 1.7;
        PIERCE[UNHOLY_ARROW] = 3;
        RARITY[UNHOLY_ARROW] = 1;
        accessory(EYE_SHIELD, "Shield of the Eye", "Double tap left or right to dash into enemies. +2 defense", 20000);
        DEFENSE[EYE_SHIELD] = 2;
        RARITY[EYE_SHIELD] = 3;

        def(SNOW_BLOCK, "Snow Block", "Can be placed", K_TILE, 999, Tiles.SNOW, 8, 0);
        def(ICE_BLOCK, "Ice Block", "Can be placed. Slippery", K_TILE, 999, Tiles.ICE, 8, 0);
        def(BOREAL_WOOD, "Boreal Wood", "Snowy wood. Works as wood in recipes", K_TILE, 999, Tiles.BOREAL_PLANKS, 8, 0);
        def(BOREAL_WALL, "Boreal Wood Wall", "Background wall", K_WALL, 999, Tiles.W_BOREAL, 7, 0);
        sword(ICE_BLADE, "Ice Blade", 17, 4.5, 22, 10000);
        DESC[ICE_BLADE] = "Shoots an icy bolt that chills enemies";
        EFFECT[ICE_BLADE] = FX_FROST;
        PROJ[ICE_BLADE] = Projectile.FROST_BOLT;
        SHOOT_SPEED[ICE_BLADE] = 3.8;
        RARITY[ICE_BLADE] = 1;
        accessory(ICE_SKATES, "Ice Skates", "Grip and speed on ice", 5000);
        accessory(BLIZZARD_BOTTLE, "Blizzard in a Bottle", "Allows a strong double jump", 6000);
        def(ICE_CHEST, "Frozen Chest", "Stores items. Right-click to open", K_TILE, 99, Tiles.ICE_CHEST, 14, 5000);
        def(GRASS_SEEDS, "Grass Seeds", "Plant on dirt to grow grass", K_SEED, 99, Tiles.GRASS, 15, 20);
        def(JUNGLE_SEEDS, "Jungle Grass Seeds", "Plant on mud to grow jungle grass", K_SEED, 99, Tiles.JUNGLE_GRASS, 15, 150);
        def(PURIFICATION_POWDER, "Purification Powder", "Cleanses the corruption around the mouse", K_USE, 99, 0, 20, 75);
        STYLE[PURIFICATION_POWDER] = S_HOLDUP;
        def(HEART, "Heart", "Heals 20 life", K_PICKUP, 1, 0, 0, 0);
        def(MANA_STAR, "Star", "Restores 50 mana", K_PICKUP, 1, 0, 0, 0);
    }

    static boolean isMagic(int id) {
        return MANA[id] > 0;
    }

    static boolean isTool(int id) {
        return KIND[id] == K_TOOL;
    }

    static boolean isWeapon(int id) {
        return KIND[id] == K_WEAPON || KIND[id] == K_TOOL;
    }

    static boolean isCoin(int id) {
        return KIND[id] == K_COIN;
    }

    static boolean isWood(int id) {
        return id == WOOD || id == MAHOGANY || id == EBONWOOD || id == BOREAL_WOOD;
    }

    /** Sell price in copper coins. */
    static int sellPrice(int id) {
        return VALUE[id] / 5;
    }

    /** Coins as text, e.g. "1 gold 20 silver". */
    static String money(long copper) {
        if (copper <= 0) return "0 copper";
        long p = copper / 1000000, g = copper / 10000 % 100, s = copper / 100 % 100, c = copper % 100;
        StringBuilder sb = new StringBuilder();
        if (p > 0) sb.append(p).append(" platinum ");
        if (g > 0) sb.append(g).append(" gold ");
        if (s > 0) sb.append(s).append(" silver ");
        if (c > 0) sb.append(c).append(" copper");
        return sb.toString().trim();
    }
}
