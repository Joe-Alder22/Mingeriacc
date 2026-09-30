package mingeriacc;

/**
 * Crafting recipes. A recipe may need a crafting station (a tile) near the
 * player; station 0 means crafting by hand.
 */
final class Recipes {
    private Recipes() {}

    static final int HAND = 0;

    static final class Recipe {
        final int result, amount, station;
        final int[] items, counts;

        Recipe(int result, int amount, int station, int... ingredients) {
            this.result = result;
            this.amount = amount;
            this.station = station;
            int n = ingredients.length / 2;
            items = new int[n];
            counts = new int[n];
            for (int i = 0; i < n; i++) {
                items[i] = ingredients[i * 2];
                counts[i] = ingredients[i * 2 + 1];
            }
        }

        boolean canCraft(Inventory inv) {
            for (int i = 0; i < items.length; i++)
                if (have(inv, items[i]) < counts[i]) return false;
            return true;
        }

        /** Whether the player has at least one of the ingredients. */
        boolean hasAny(Inventory inv) {
            for (int item : items) if (have(inv, item) > 0) return true;
            return false;
        }

        void consume(Inventory inv) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] == Items.WOOD) {
                    int n = counts[i];
                    for (int wood : WOODS) {
                        int take = Math.min(n, inv.countOf(wood));
                        inv.remove(wood, take);
                        n -= take;
                    }
                } else {
                    inv.remove(items[i], counts[i]);
                }
            }
        }
    }

    private static final int[] WOODS = {Items.WOOD, Items.MAHOGANY, Items.EBONWOOD};

    /** How many of an ingredient the player has ("Wood" means any kind of wood). */
    static int have(Inventory inv, int item) {
        if (item != Items.WOOD) return inv.countOf(item);
        int n = 0;
        for (int w : WOODS) n += inv.countOf(w);
        return n;
    }

    private static final int WB = Tiles.WORKBENCH, FN = Tiles.FURNACE, AN = Tiles.ANVIL;

    static final Recipe[] ALL = {
        // by hand
        new Recipe(Items.WORKBENCH, 1, HAND, Items.WOOD, 10),
        new Recipe(Items.TORCH, 3, HAND, Items.WOOD, 1, Items.GEL, 1),
        new Recipe(Items.PLATFORM, 2, HAND, Items.WOOD, 1),
        // work bench
        new Recipe(Items.WOOD_SWORD, 1, WB, Items.WOOD, 7),
        new Recipe(Items.WOOD_BOW, 1, WB, Items.WOOD, 10),
        new Recipe(Items.WOOD_HAMMER, 1, WB, Items.WOOD, 8),
        new Recipe(Items.WOOD_ARROW, 10, WB, Items.WOOD, 1, Items.STONE, 1),
        new Recipe(Items.FURNACE, 1, WB, Items.STONE, 20, Items.WOOD, 4, Items.TORCH, 3),
        new Recipe(Items.IRON_ANVIL, 1, WB, Items.IRON_BAR, 5),
        new Recipe(Items.WOOD_WALL, 4, WB, Items.WOOD, 1),
        new Recipe(Items.STONE_WALL, 4, WB, Items.STONE, 1),
        new Recipe(Items.DIRT_WALL, 4, WB, Items.DIRT, 1),
        new Recipe(Items.GLASS_WALL, 4, WB, Items.GLASS, 1),
        new Recipe(Items.GRAY_BRICK_WALL, 4, WB, Items.GRAY_BRICK, 1),
        new Recipe(Items.RED_BRICK_WALL, 4, WB, Items.RED_BRICK, 1),
        new Recipe(Items.HEALING_POTION, 2, WB, Items.BOTTLE, 2, Items.GEL, 2, Items.MUSHROOM, 1),
        new Recipe(Items.CHEST, 1, WB, Items.WOOD, 8, Items.IRON_BAR, 2),
        new Recipe(Items.GOLD_CHEST, 1, WB, Items.WOOD, 8, Items.GOLD_BAR, 2),
        new Recipe(Items.DOOR, 1, WB, Items.WOOD, 6),
        new Recipe(Items.TABLE, 1, WB, Items.WOOD, 8),
        new Recipe(Items.CHAIR, 1, WB, Items.WOOD, 4),
        new Recipe(Items.BED, 1, WB, Items.WOOD, 15, Items.GEL, 5),
        new Recipe(Items.FLAMING_ARROW, 10, WB, Items.WOOD_ARROW, 10, Items.TORCH, 1),
        new Recipe(Items.MAHOGANY_WALL, 4, WB, Items.MAHOGANY, 1),
        new Recipe(Items.OBSIDIAN_BRICK_WALL, 4, WB, Items.OBSIDIAN_BRICK, 1),
        new Recipe(Items.HELLSTONE_BRICK_WALL, 4, WB, Items.HELLSTONE_BRICK, 1),
        // furnace
        new Recipe(Items.COPPER_BAR, 1, FN, Items.COPPER_ORE, 3),
        new Recipe(Items.IRON_BAR, 1, FN, Items.IRON_ORE, 3),
        new Recipe(Items.SILVER_BAR, 1, FN, Items.SILVER_ORE, 4),
        new Recipe(Items.GOLD_BAR, 1, FN, Items.GOLD_ORE, 4),
        new Recipe(Items.GLASS, 1, FN, Items.SAND, 2),
        new Recipe(Items.BOTTLE, 2, FN, Items.GLASS, 1),
        new Recipe(Items.GRAY_BRICK, 1, FN, Items.STONE, 2),
        new Recipe(Items.RED_BRICK, 1, FN, Items.CLAY, 2),
        new Recipe(Items.DEMONITE_BAR, 1, FN, Items.DEMONITE_ORE, 3),
        new Recipe(Items.HELLSTONE_BAR, 1, FN, Items.HELLSTONE, 3, Items.OBSIDIAN, 1),
        new Recipe(Items.OBSIDIAN_BRICK, 1, FN, Items.OBSIDIAN, 2),
        new Recipe(Items.HELLSTONE_BRICK, 1, FN, Items.HELLSTONE, 1, Items.STONE, 1),
        // anvil
        new Recipe(Items.COPPER_PICK, 1, AN, Items.COPPER_BAR, 12, Items.WOOD, 4),
        new Recipe(Items.COPPER_AXE, 1, AN, Items.COPPER_BAR, 9, Items.WOOD, 3),
        new Recipe(Items.COPPER_HAMMER, 1, AN, Items.COPPER_BAR, 10, Items.WOOD, 3),
        new Recipe(Items.COPPER_SHORTSWORD, 1, AN, Items.COPPER_BAR, 5),
        new Recipe(Items.COPPER_SWORD, 1, AN, Items.COPPER_BAR, 8),
        new Recipe(Items.COPPER_BOW, 1, AN, Items.COPPER_BAR, 7),
        new Recipe(Items.IRON_PICK, 1, AN, Items.IRON_BAR, 12, Items.WOOD, 3),
        new Recipe(Items.IRON_AXE, 1, AN, Items.IRON_BAR, 9, Items.WOOD, 3),
        new Recipe(Items.IRON_HAMMER, 1, AN, Items.IRON_BAR, 10, Items.WOOD, 3),
        new Recipe(Items.IRON_SWORD, 1, AN, Items.IRON_BAR, 8),
        new Recipe(Items.IRON_BOW, 1, AN, Items.IRON_BAR, 7),
        new Recipe(Items.SILVER_PICK, 1, AN, Items.SILVER_BAR, 12, Items.WOOD, 4),
        new Recipe(Items.SILVER_AXE, 1, AN, Items.SILVER_BAR, 9, Items.WOOD, 3),
        new Recipe(Items.SILVER_HAMMER, 1, AN, Items.SILVER_BAR, 10, Items.WOOD, 3),
        new Recipe(Items.SILVER_SWORD, 1, AN, Items.SILVER_BAR, 8),
        new Recipe(Items.SILVER_BOW, 1, AN, Items.SILVER_BAR, 7),
        new Recipe(Items.GOLD_PICK, 1, AN, Items.GOLD_BAR, 12, Items.WOOD, 4),
        new Recipe(Items.GOLD_AXE, 1, AN, Items.GOLD_BAR, 9, Items.WOOD, 3),
        new Recipe(Items.GOLD_HAMMER, 1, AN, Items.GOLD_BAR, 10, Items.WOOD, 3),
        new Recipe(Items.GOLD_SWORD, 1, AN, Items.GOLD_BAR, 8),
        new Recipe(Items.GOLD_BOW, 1, AN, Items.GOLD_BAR, 7),
        new Recipe(Items.EMPTY_BUCKET, 1, AN, Items.IRON_BAR, 3),
        new Recipe(Items.NIGHTMARE_PICK, 1, AN, Items.DEMONITE_BAR, 12),
        new Recipe(Items.WAR_AXE, 1, AN, Items.DEMONITE_BAR, 10),
        new Recipe(Items.THE_BREAKER, 1, AN, Items.DEMONITE_BAR, 12),
        new Recipe(Items.LIGHTS_BANE, 1, AN, Items.DEMONITE_BAR, 10),
        new Recipe(Items.DEMON_BOW, 1, AN, Items.DEMONITE_BAR, 10),
        new Recipe(Items.BLADE_OF_GRASS, 1, AN, Items.JUNGLE_SPORES, 12, Items.STINGER, 12, Items.VINE, 3),
        new Recipe(Items.MOLTEN_PICK, 1, AN, Items.HELLSTONE_BAR, 20),
        new Recipe(Items.FIERY_GREATSWORD, 1, AN, Items.HELLSTONE_BAR, 20),
        new Recipe(Items.MOLTEN_FURY, 1, AN, Items.HELLSTONE_BAR, 15),
        new Recipe(Items.COPPER_HELMET, 1, AN, Items.COPPER_BAR, 15),
        new Recipe(Items.COPPER_MAIL, 1, AN, Items.COPPER_BAR, 25),
        new Recipe(Items.COPPER_GREAVES, 1, AN, Items.COPPER_BAR, 20),
        new Recipe(Items.IRON_HELMET, 1, AN, Items.IRON_BAR, 15),
        new Recipe(Items.IRON_MAIL, 1, AN, Items.IRON_BAR, 25),
        new Recipe(Items.IRON_GREAVES, 1, AN, Items.IRON_BAR, 20),
        new Recipe(Items.SILVER_HELMET, 1, AN, Items.SILVER_BAR, 15),
        new Recipe(Items.SILVER_MAIL, 1, AN, Items.SILVER_BAR, 25),
        new Recipe(Items.SILVER_GREAVES, 1, AN, Items.SILVER_BAR, 20),
        new Recipe(Items.GOLD_HELMET, 1, AN, Items.GOLD_BAR, 15),
        new Recipe(Items.GOLD_MAIL, 1, AN, Items.GOLD_BAR, 25),
        new Recipe(Items.GOLD_GREAVES, 1, AN, Items.GOLD_BAR, 20),
        new Recipe(Items.SHADOW_HELMET, 1, AN, Items.DEMONITE_BAR, 15, Items.ROTTEN_CHUNK, 5),
        new Recipe(Items.SHADOW_MAIL, 1, AN, Items.DEMONITE_BAR, 25, Items.ROTTEN_CHUNK, 10),
        new Recipe(Items.SHADOW_GREAVES, 1, AN, Items.DEMONITE_BAR, 20, Items.ROTTEN_CHUNK, 8),
        new Recipe(Items.MOLTEN_HELMET, 1, AN, Items.HELLSTONE_BAR, 20),
        new Recipe(Items.MOLTEN_MAIL, 1, AN, Items.HELLSTONE_BAR, 30),
        new Recipe(Items.MOLTEN_GREAVES, 1, AN, Items.HELLSTONE_BAR, 25),
    };

    static String stationName(int station) {
        return station == HAND ? "By hand" : Tiles.NAME[station];
    }
}
