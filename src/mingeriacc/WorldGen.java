package mingeriacc;

import java.util.Random;

/** World generation: terrain, layers, caves, ores, trees and plants. */
final class WorldGen {
    static final String[] SIZE_NAMES = {"Small", "Medium", "Large"};
    static final int[][] SIZES = {{1400, 500}, {2400, 700}, {3600, 1000}};
    /** Amounts that can be set when creating a world (1 = normal). */
    static final String[] ORE_NAMES = {"Copper", "Iron", "Silver", "Gold", "Demonite", "Hellstone", "Gems"};
    static final int COPPER = 0, IRON = 1, SILVER = 2, GOLD = 3, DEMONITE = 4, HELLSTONE = 5, GEMS = 6;

    /** Generation progress, readable from another thread. */
    static final class Progress {
        volatile String stage = "";
        volatile double value;
        volatile World result;
        volatile Throwable error;
    }

    private final World world;
    private final Random rnd;
    private final Noise noise, noise2, noise3;
    private final Progress progress;
    private final int W, H;
    private int[] surf, rock;
    private double[] ore = {1, 1, 1, 1, 1, 1, 1};
    /** Adding gems and moss to a world from an older version: only untouched rock is changed. */
    private boolean retrofit;

    private WorldGen(World world, Progress progress) {
        this.world = world;
        this.rnd = new Random(world.seed);
        this.noise = new Noise(world.seed);
        this.noise2 = new Noise(world.seed * 31 + 7);
        this.noise3 = new Noise(world.seed * 17 + 3);
        this.progress = progress;
        this.W = world.w;
        this.H = world.h;
    }

    static World generate(String name, int sizeIndex, long seed, Progress p) {
        return generate(name, sizeIndex, seed, p, null);
    }

    /** oreAmounts: multipliers in the order of ORE_NAMES (null = all normal). */
    static World generate(String name, int sizeIndex, long seed, Progress p, double[] oreAmounts) {
        int[] s = SIZES[sizeIndex];
        World w = new World(s[0], s[1]);
        w.name = name;
        w.seed = seed;
        w.sizeIndex = sizeIndex;
        long id = new Random().nextLong();
        w.id = id == 0 ? 1 : id;
        WorldGen gen = new WorldGen(w, p);
        if (oreAmounts != null) gen.ore = oreAmounts.clone();
        gen.run();
        return w;
    }

    /**
     * Brings a world made by an older version up to date: gems in unbroken rock
     * and moss on natural cave stone deep down. Returns the number of gem tiles.
     */
    static int retrofit(World w) {
        WorldGen gen = new WorldGen(w, null);
        gen.retrofit = true;
        int before = gen.countGems();
        gen.gems();
        gen.moss();
        return gen.countGems() - before;
    }

    private int countGems() {
        int n = 0;
        for (byte b : world.tiles) if (Tiles.isGem(b & 0xff)) n++;
        return n;
    }

    private void stage(String s, double v) {
        if (progress != null) {
            progress.stage = s;
            progress.value = v;
        }
    }

    private void run() {
        stage("Shaping the terrain", 0.02);
        terrain();
        planBiomes();
        stage("Laying down layers", 0.1);
        layers();
        stage("Growing the jungle", 0.16);
        jungle();
        stage("Freezing the snowlands", 0.18);
        snow();
        stage("Spreading the corruption", 0.2);
        corruption();
        stage("Digging caves", 0.25);
        caves();
        stage("Opening chasms", 0.5);
        chasms();
        stage("Heating the underworld", 0.55);
        underworld();
        stage("Scattering ores", 0.62);
        ores();
        stage("Hiding gems", 0.65);
        gems();
        stage("Growing grass", 0.68);
        grass();
        stage("Growing moss", 0.7);
        moss();
        world.updateAllSky();
        stage("Planting trees", 0.72);
        trees();
        stage("Picking flowers", 0.76);
        decorations();
        world.updateAllSky();
        stage("Filling the seas", 0.8);
        waters();
        stage("Building cabins", 0.9);
        cabins();
        stage("Hiding treasures", 0.94);
        treasures();
        world.updateAllSky();
        stage("Settling the water", 0.96);
        world.liquids.settle(300);
        world.removeFloatingTrees();
        spawn();
        stage("Done", 1.0);
    }

    // ---- snow ------------------------------------------------------------------

    private int jungleSide = 1;
    private int snowL = -1, snowR = -1;

    /** Decides where the big biomes go: the jungle on one side, the snow between it and the spawn. */
    private void planBiomes() {
        jungleSide = rnd.nextBoolean() ? 1 : -1;
        int center = (int) (W / 2 + jungleSide * W * 0.13);
        int half = (int) (W * 0.045) + 6;
        snowL = center - half;
        snowR = center + half;
    }

    private boolean inSnow(int x, int y) {
        if (snowL < 0) return false;
        double wob = noise3.noise(y / 26.0 + 77) * 10;
        if (x < snowL + wob || x > snowR + wob) return false;
        // deepest in the middle, like a valley of ice going down
        double d = Math.abs(x - (snowL + snowR) / 2.0) / ((snowR - snowL) / 2.0);
        int top = surf != null ? surf[Math.max(0, Math.min(W - 1, x))] : world.surfaceLevel;
        double bottom = top + (H * 0.62 - top) * (1 - d * d * 0.65);
        return y < bottom;
    }

    private void snow() {
        for (int x = Math.max(0, snowL - 20); x < Math.min(W, snowR + 20); x++)
            for (int y = Math.max(0, surf[x] - 2); y < world.underworld && y < H; y++) {
                if (!inSnow(x, y)) continue;
                int i = x + y * W;
                int t = world.tiles[i] & 0xff;
                if (t == Tiles.DIRT || t == Tiles.CLAY || t == Tiles.SAND) world.setRaw(x, y, Tiles.SNOW);
                else if (t == Tiles.STONE && (y < rock[x] + 6 || noise2.fractal(x / 14.0 + 90, y / 14.0, 2) > -0.08))
                    world.setRaw(x, y, Tiles.ICE);
                int wl = world.walls[i];
                if (wl == Tiles.W_DIRT_N) world.walls[i] = (byte) Tiles.W_SNOW_N;
                else if (wl == Tiles.W_STONE_N) world.walls[i] = (byte) Tiles.W_ICE_N;
            }
    }

    // ---- gems and moss ------------------------------------------------------------

    /** Small clusters of rubies, sapphires, emeralds and topazes in the cavern stone. */
    private void gems() {
        double area = (double) W * Math.max(1, world.underworld - world.rockLevel);
        int[] types = {Tiles.RUBY, Tiles.SAPPHIRE, Tiles.EMERALD, Tiles.TOPAZ};
        double[] amount = {1.0, 0.85, 0.75, 0.65};
        double[] from = {0.34, 0.38, 0.44, 0.5};
        for (int k = 0; k < 4; k++) {
            int n = (int) (area / 9000 * amount[k] * ore[GEMS]);
            int top = Math.max(world.rockLevel, (int) (H * from[k]));
            int bot = world.underworld - 8;
            if (bot <= top) continue;
            for (int i = 0; i < n; i++) {
                int x = 10 + rnd.nextInt(W - 20), y = top + rnd.nextInt(bot - top);
                gemCluster(x, y, types[k], 2 + rnd.nextInt(3));
            }
        }
    }

    private void gemCluster(int x, int y, int type, int size) {
        for (int k = 0; k < size; k++) {
            int xx = x + rnd.nextInt(3) - 1, yy = y + rnd.nextInt(3) - 1;
            if (world.tile(xx, yy) != Tiles.STONE) continue;
            if (retrofit && !enclosed(xx, yy)) continue;
            world.setRaw(xx, yy, type);
            x = xx;
            y = yy;
        }
    }

    /** No air around: rock nobody has dug into. */
    private boolean enclosed(int x, int y) {
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                if (!world.solid(x + dx, y + dy)) return false;
        return true;
    }

    /** Patches of coloured moss on the cave stone; each world has a few of the colours. */
    private void moss() {
        int[] all = {Tiles.GREEN_MOSS, Tiles.BROWN_MOSS, Tiles.RED_MOSS, Tiles.BLUE_MOSS, Tiles.PURPLE_MOSS};
        Random r = new Random(world.seed * 13 + 5);
        for (int i = all.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = all[i]; all[i] = all[j]; all[j] = t;
        }
        int[] kinds = {all[0], all[1], all[2]};
        int top = world.rockLevel + 6, bot = world.underworld - 6;
        for (int y = top; y < bot; y++)
            for (int x = 2; x < W - 2; x++) {
                if (world.tile(x, y) != Tiles.STONE || !exposed(x, y)) continue;
                if (noise3.fractal(x / 55.0 + 200, y / 38.0, 2) < 0.12) continue;
                if (retrofit) {
                    int wl = world.wall(x, y);
                    boolean natural = wl == 0 || Tiles.WALL_NATURAL[wl];
                    if (!natural) continue;
                    for (int dy = -1; dy <= 1; dy++) {
                        int wa = world.wall(x, y + dy);
                        if (wa != 0 && !Tiles.WALL_NATURAL[wa]) natural = false;
                    }
                    if (!natural) continue;
                }
                double v = (noise2.noise(x / 90.0 + 50, y / 70.0) + 1) / 2;
                int moss = kinds[Math.max(0, Math.min(2, (int) (v * 3)))];
                if (r.nextInt(100) < 85) world.setRaw(x, y, moss);
            }
        for (int y = top; y < bot; y++)
            for (int x = 2; x < W - 2; x++)
                if (Tiles.isMoss(world.tile(x, y)) && world.tile(x, y - 1) == Tiles.AIR && world.liquidAt(x, y - 1) == 0
                        && r.nextInt(4) == 0) world.setRaw(x, y - 1, Tiles.MOSS_PLANT);
    }

    // ---- biome areas -------------------------------------------------------

    private int jungleL = -1, jungleR = -1;
    private final java.util.List<int[]> corrupt = new java.util.ArrayList<>();
    private int seaLevel;

    private boolean inJungle(int x, int y) {
        if (jungleL < 0) return false;
        double wob = noise3.noise(y / 30.0 + 11) * 14;
        return x >= jungleL + wob && x <= jungleR + wob;
    }

    private boolean inCorruption(int x, int y) {
        for (int[] c : corrupt) {
            double wob = noise3.noise(y / 25.0 + c[0] * 0.01) * 10;
            if (x >= c[0] + wob && x <= c[1] + wob) return true;
        }
        return false;
    }

    /** Mud, jungle grass and mahogany on one side of the spawn. */
    private void jungle() {
        int side = jungleSide;
        int center = (int) (W / 2 + side * W * 0.26);
        int half = (int) (W * 0.065);
        jungleL = center - half;
        jungleR = center + half;
        for (int x = Math.max(0, jungleL - 20); x < Math.min(W, jungleR + 20); x++)
            for (int y = surf[x] - 2; y < world.underworld; y++) {
                if (!inJungle(x, y) || !world.inside(x, y)) continue;
                int i = x + y * W;
                int t = world.tiles[i] & 0xff;
                if (t == Tiles.DIRT || t == Tiles.CLAY || t == Tiles.SAND) world.setRaw(x, y, Tiles.MUD);
                else if (t == Tiles.STONE && noise2.fractal(x / 20.0, y / 20.0, 2) > -0.1) world.setRaw(x, y, Tiles.MUD);
                if (world.walls[i] != 0) world.walls[i] = (byte) Tiles.W_MUD_N;
            }
        corruptSide = -side;
    }

    private int corruptSide = 1;

    /** Ebonstone patches on the other side. */
    private void corruption() {
        int patches = world.sizeIndex == 0 ? 1 : 2;
        for (int p = 0; p < patches; p++) {
            double dist = p == 0 ? 0.2 + rnd.nextDouble() * 0.06 : 0.34 + rnd.nextDouble() * 0.04;
            int center = (int) (W / 2 + corruptSide * W * dist);
            int half = (int) (W * 0.035) + rnd.nextInt(10);
            int[] c = {center - half, center + half};
            corrupt.add(c);
            for (int x = Math.max(0, c[0] - 15); x < Math.min(W, c[1] + 15); x++) {
                int bottom = world.rockLevel + 30 + (int) (noise.noise(x / 12.0 + 40) * 12);
                for (int y = surf[x] - 2; y < bottom && y < H; y++) {
                    if (!inCorruption(x, y) || !world.inside(x, y)) continue;
                    int i = x + y * W;
                    int t = world.tiles[i] & 0xff;
                    if (t == Tiles.STONE) world.setRaw(x, y, Tiles.EBONSTONE);
                    else if (t == Tiles.SAND) world.setRaw(x, y, Tiles.EBONSAND);
                    else if (t == Tiles.DIRT && y > rock[x] - 6 && rnd.nextInt(3) == 0) world.setRaw(x, y, Tiles.EBONSTONE);
                    if (world.walls[i] != 0) world.walls[i] = (byte) Tiles.W_EBON_N;
                }
            }
        }
    }

    /** Deep vertical shafts in the corruption, lined with ebonstone, with demonite at the bottom. */
    private void chasms() {
        for (int[] c : corrupt) {
            int n = 2 + rnd.nextInt(3);
            for (int k = 0; k < n; k++) {
                double x = c[0] + (c[1] - c[0]) * (k + 0.5) / n + (rnd.nextDouble() - 0.5) * 8;
                int y0 = world.surfaceAt((int) x) - 2;
                int depth = 45 + rnd.nextInt(50);
                double r = 2.2 + rnd.nextDouble();
                for (int s = 0; s < depth; s++) {
                    int y = y0 + s;
                    x += Math.sin(s * 0.15 + k) * 0.6;
                    // ebonstone lining
                    for (int dx = (int) (-r - 3); dx <= r + 3; dx++)
                        for (int dy = -1; dy <= 1; dy++) {
                            int xx = (int) x + dx, yy = y + dy;
                            if (world.inside(xx, yy) && Tiles.SOLID[world.tile(xx, yy)] && yy > y0 + 2)
                                world.setRaw(xx, yy, Tiles.EBONSTONE);
                        }
                    carve(x, y, r);
                    // side pockets
                    if (s > 10 && rnd.nextInt(18) == 0) {
                        int dir = rnd.nextBoolean() ? 1 : -1;
                        for (int j = 0; j < 8 + rnd.nextInt(10); j++) carve(x + dir * j, y + j * 0.2, 1.6);
                    }
                }
                // demonite around the bottom
                for (int j = 0; j < (int) Math.round(6 * ore[DEMONITE]); j++)
                    runner(x + (rnd.nextDouble() - 0.5) * 16, y0 + depth + rnd.nextInt(6) - 3, 1.4, 4 + rnd.nextInt(3),
                            Tiles.DEMONITE, Tiles.EBONSTONE, Tiles.STONE, Tiles.DIRT);
            }
        }
    }

    /** The underworld: ash, open caverns, lava lakes, hellstone and ruined houses. */
    private void underworld() {
        int top = H - (int) (H * 0.12);
        world.underworld = top;
        for (int x = 0; x < W; x++) {
            int start = top - 4 + (int) (noise.noise(x / 17.0 + 70) * 4);
            int ceil = top + 6 + (int) (noise2.fractal(x / 40.0, 3) * 7);
            int floor = H - 11 + (int) (noise3.fractal(x / 30.0 + 5, 2) * 5);
            for (int y = start; y < H; y++) {
                int i = x + y * W;
                world.walls[i] = 0;
                boolean island = noise2.fractal(x / 22.0, y / 10.0, 2) > 0.3;
                if (y >= ceil && y < floor && !island) world.setRaw(x, y, Tiles.AIR);
                else world.setRaw(x, y, Tiles.ASH);
            }
            int lava = H - 16;
            for (int y = lava; y < H; y++)
                if (world.tile(x, y) == Tiles.AIR) {
                    world.liquid[x + y * W] = (byte) Liquids.FULL;
                    world.lqType[x + y * W] = (byte) Liquids.LAVA;
                }
        }
        // ruined houses
        int ruins = 3 + world.sizeIndex * 2;
        for (int k = 0, tries = 0; k < ruins && tries < 200; tries++) {
            int x = 20 + rnd.nextInt(W - 60);
            int y = top + 6;
            while (y < H - 5 && world.tile(x, y) != Tiles.AIR) y++;
            while (y < H - 5 && world.tile(x, y + 1) == Tiles.AIR) y++;
            if (y >= H - 18 || world.liquidAt(x, y) > 0) continue;
            house(x, y - 7, 12, 8, Tiles.OBSIDIAN_BRICK, Tiles.W_OBSIDIAN_BRICK, Tiles.GOLD_CHEST, 3);
            k++;
        }
    }

    // ---- water -----------------------------------------------------------------

    private void fillWater(int x, int y, int type) {
        if (!world.inside(x, y) || world.tile(x, y) != Tiles.AIR) return;
        world.liquid[x + y * W] = (byte) Liquids.FULL;
        world.lqType[x + y * W] = (byte) type;
    }

    private void waters() {
        // oceans at both edges
        for (int x = 0; x < W; x++) {
            int de = Math.min(x, W - 1 - x);
            if (de > 140) continue;
            for (int y = seaLevel; y < H && y < surf[x] + 2; y++) fillWater(x, y, Liquids.WATER);
        }
        // lakes in hollows
        int lakes = 2 + W / 600;
        for (int k = 0, tries = 0; k < lakes && tries < 100; tries++) {
            int x0 = 160 + rnd.nextInt(W - 320);
            if (Math.abs(x0 - W / 2) < 70) continue;
            int w = 12 + rnd.nextInt(14);
            int rim = Integer.MAX_VALUE, low = 0;
            for (int x = x0; x < x0 + w; x++) {
                rim = Math.min(rim, world.surfaceAt(x));
                low = Math.max(low, world.surfaceAt(x));
            }
            if (low - rim > 8) continue;
            // trees standing where the lake goes would be left floating over the water
            for (int x = x0 - 1; x <= x0 + w; x++) {
                int s0 = world.surfaceAt(x);
                if (Tiles.isTrunk(world.tile(x, s0 - 1))) world.removeTree(x, s0 - 1);
            }
            int level = Math.max(rim, low - 1) + 1;
            int depth = 4 + rnd.nextInt(4);
            for (int x = x0 + 1; x < x0 + w - 1; x++) {
                double t = (x - x0) / (double) (w - 1);
                int d = (int) Math.round(Math.sin(t * Math.PI) * depth);
                int s0 = world.surfaceAt(x);
                for (int y = s0 - 1; y < level + d; y++) {
                    int tt = world.tile(x, y);
                    if (y >= s0 || (tt != Tiles.AIR && !Tiles.SOLID[tt] && !Tiles.isTrunk(tt) && !Tiles.isLeaves(tt)))
                        world.setRaw(x, y, Tiles.AIR);
                }
                for (int y = level; y < level + d; y++) fillWater(x, y, Liquids.WATER);
                if (inSnow(x, level) && d > 0) {
                    // lakes in the snow are frozen over
                    world.liquid[x + level * W] = 0;
                    world.setRaw(x, level, Tiles.ICE);
                }
                int bottom = level + d;
                for (int y = bottom; y < bottom + 2; y++)
                    if (Tiles.SOLID[world.tile(x, y)]) world.setRaw(x, y, inJungle(x, y) ? Tiles.MUD : Tiles.SAND);
            }
            k++;
        }
        // pockets of water in caves, and lava deep down
        int pockets = W * H / 9000;
        for (int k = 0; k < pockets; k++) {
            int x = 10 + rnd.nextInt(W - 20);
            int y = world.surfaceLevel + 10 + rnd.nextInt(Math.max(1, world.underworld - world.surfaceLevel - 30));
            boolean lava = y > H * 0.62 && rnd.nextInt(3) == 0;
            pool(x, y, lava ? Liquids.LAVA : Liquids.WATER);
        }
    }

    private void pool(int x, int y, int type) {
        y = floorBelow(x, y, 30);
        if (y < 0) return;
        int r = 3 + rnd.nextInt(4);
        for (int dy = -r; dy <= 0; dy++)
            for (int dx = -r * 2; dx <= r * 2; dx++)
                fillWater(x + dx, y + dy, type);
    }

    // ---- cabins, houses and chests -------------------------------------------------

    /** A small house: frame of blocks, a wall behind, a chest on the floor. (x, y) = top-left. */
    private void house(int x, int y, int w, int h, int block, int wall, int chest, int lootTier) {
        for (int dy = 0; dy < h; dy++)
            for (int dx = 0; dx < w; dx++) {
                int xx = x + dx, yy = y + dy;
                if (!world.inside(xx, yy)) continue;
                boolean frame = dx == 0 || dy == 0 || dx == w - 1 || dy == h - 1;
                world.liquid[xx + yy * W] = 0;
                world.setRaw(xx, yy, frame ? block : Tiles.AIR);
                world.walls[xx + yy * W] = (byte) wall;
            }
        // a doorway on one side
        int side = rnd.nextBoolean() ? 0 : w - 1;
        for (int dy = h - 4; dy < h - 1; dy++) world.setRaw(x + side, y + dy, Tiles.AIR);
        int cx = x + 2 + rnd.nextInt(Math.max(1, w - 5));
        world.placeFurniture(chest, cx, y + h - 3, 0);
        fillChest(cx, y + h - 3, lootTier);
        world.setRaw(x + w / 2, y + 2, Tiles.TORCH);
    }

    private void cabins() {
        int count = Math.max(3, W / 170);
        for (int k = 0, tries = 0; k < count && tries < count * 20; tries++) {
            int x = 40 + rnd.nextInt(W - 100);
            int y = world.surfaceLevel + 15 + rnd.nextInt(Math.max(1, world.underworld - world.surfaceLevel - 45));
            if (inCorruption(x, y) || inCorruption(x + 10, y)) continue;
            boolean deep = y > world.rockLevel + 40;
            boolean jungle = inJungle(x, y);
            boolean snow = inSnow(x, y) || inSnow(x + 10, y);
            int block = jungle ? Tiles.MAHOGANY_PLANKS : snow ? Tiles.BOREAL_PLANKS : deep ? Tiles.GRAY_BRICK : Tiles.PLANKS;
            int wall = jungle ? Tiles.W_MAHOGANY : snow ? Tiles.W_BOREAL : deep ? Tiles.W_GRAY_BRICK : Tiles.W_WOOD;
            int chest = snow ? Tiles.ICE_CHEST : deep ? Tiles.GOLD_CHEST : Tiles.CHEST;
            house(x, y, 10, 7, block, wall, chest, jungle ? 2 : snow ? 4 : deep ? 1 : 0);
            k++;
        }
        // chests lying in caves
        int chests = W * H / 45000;
        for (int k = 0, tries = 0; k < chests && tries < chests * 40; tries++) {
            int x = 10 + rnd.nextInt(W - 20);
            int y = world.surfaceLevel + 10 + rnd.nextInt(Math.max(1, world.underworld - world.surfaceLevel - 20));
            y = floorBelow(x, y, 30);
            if (y < 0 || !fits(x, y, 2, 2) || world.liquidAt(x, y) > 0) continue;
            boolean deep = y > world.rockLevel + 40;
            boolean snow = inSnow(x, y);
            world.placeFurniture(snow ? Tiles.ICE_CHEST : deep ? Tiles.GOLD_CHEST : Tiles.CHEST, x, y - 1, 0);
            fillChest(x, y - 1, inJungle(x, y) ? 2 : snow ? 4 : deep ? 1 : 0);
            k++;
        }
    }

    /** Loot: 0 = underground, 1 = deep (gold chest), 2 = jungle, 3 = underworld, 4 = snow. */
    private void fillChest(int x, int y, int tier) {
        World.Chest c = new World.Chest(x, y);
        world.chests.put(x + y * W, c);
        int[][] main = {
            {Items.HERMES_BOOTS, Items.CLOUD_BOTTLE, Items.BAND_REGEN, Items.AGLET, Items.BALLOON, Items.WOOD_BOW,
                Items.MAGIC_MIRROR},
            {Items.HORSESHOE, Items.CLOUD_BOTTLE, Items.HERMES_BOOTS, Items.BALLOON, Items.BAND_REGEN, Items.FLIPPER,
                Items.MAGIC_MIRROR},
            {Items.ANKLET, Items.FLIPPER, Items.AGLET, Items.BAND_REGEN},
            {Items.OBSIDIAN_SKULL, Items.HORSESHOE, Items.CLOUD_BOTTLE},
            {Items.ICE_SKATES, Items.BLIZZARD_BOTTLE, Items.ICE_BLADE, Items.MAGIC_MIRROR},
        };
        c.add(main[tier][rnd.nextInt(main[tier].length)], 1);
        if (rnd.nextInt(3) == 0) c.add(Items.HEALING_POTION, 1 + rnd.nextInt(3));
        if (rnd.nextInt(2) == 0) c.add(Items.TORCH, 8 + rnd.nextInt(15));
        if (rnd.nextInt(3) == 0) c.add(tier == 3 ? Items.FLAMING_ARROW : Items.WOOD_ARROW, 20 + rnd.nextInt(30));
        int[] bars = tier == 0 || tier == 4 ? new int[]{Items.COPPER_BAR, Items.IRON_BAR} : tier == 3
                ? new int[]{Items.HELLSTONE_BAR, Items.GOLD_BAR} : new int[]{Items.SILVER_BAR, Items.GOLD_BAR};
        if (rnd.nextInt(2) == 0) c.add(bars[rnd.nextInt(bars.length)], 3 + rnd.nextInt(6));
        if (tier == 2 && rnd.nextInt(2) == 0) c.add(Items.JUNGLE_SPORES, 2 + rnd.nextInt(4));
        if (tier >= 1 && rnd.nextInt(8) == 0) c.add(Items.LIFE_CRYSTAL, 1);
        if (tier >= 1 && rnd.nextInt(3) == 0) c.add(Items.RUBY + rnd.nextInt(4), 2 + rnd.nextInt(4));
        if (rnd.nextInt(5) == 0) c.add(Items.FALLEN_STAR, 1 + rnd.nextInt(2));
        c.add(tier == 0 ? Items.SILVER_COIN : Items.SILVER_COIN, 5 + rnd.nextInt(tier == 0 ? 20 : 60));
    }

    // ---- terrain ---------------------------------------------------------

    private void terrain() {
        surf = new int[W];
        rock = new int[W];
        double base = H * 0.24;
        int maxSurf = 0;
        for (int x = 0; x < W; x++) {
            double e = noise.fractal(x / 150.0, 4) * 58 + noise.fractal(x / 36.0 + 50, 3) * 10;
            double m = noise2.fractal(x / 420.0, 2);
            if (m > 0.12) {
                double k = (m - 0.12) * 3.2;
                double ridge = 1 - Math.abs(noise2.noise(x / 55.0 + 9));
                e += k * k * 38 * (0.6 + ridge * 0.6);
            }
            // flatter terrain around the spawn point
            double dc = Math.abs(x - W / 2.0);
            if (dc < 60) e *= 0.35 + 0.65 * dc / 60.0;
            // beaches going down into the ocean at the edges of the world
            double de = Math.min(x, W - 1 - x);
            if (de < 140) {
                double t = Math.pow(de / 140.0, 1.5);
                e = e * t + (-32) * (1 - t);
            }
            int s = (int) Math.round(base - e);
            s = Math.max((int) (H * 0.08), Math.min((int) (H * 0.36), s));
            surf[x] = s;
            if (de > 140) maxSurf = Math.max(maxSurf, s);
        }
        seaLevel = (int) Math.round(base + 3);
        // smooth out single-tile spikes
        for (int pass = 0; pass < 2; pass++)
            for (int x = 1; x < W - 1; x++)
                if (surf[x] < surf[x - 1] && surf[x] < surf[x + 1]) surf[x] = Math.min(surf[x - 1], surf[x + 1]);

        for (int x = 0; x < W; x++) {
            int r = (int) (H * 0.34 + noise3.fractal(x / 70.0, 3) * 24);
            rock[x] = Math.max(r, surf[x] + 10);
        }
        world.surfaceLevel = maxSurf + 3;
        world.rockLevel = (int) (H * 0.34);
    }

    private void layers() {
        for (int x = 0; x < W; x++) {
            for (int y = surf[x]; y < H; y++) {
                int t = y < rock[x] ? Tiles.DIRT : Tiles.STONE;
                world.setRaw(x, y, t);
                if (y >= surf[x] + 3) {
                    world.walls[x + y * W] = (byte) (y < rock[x] ? Tiles.W_DIRT_N : Tiles.W_STONE_N);
                }
            }
        }
        // sandy beaches
        for (int x = 0; x < W; x++) {
            int de = Math.min(x, W - 1 - x);
            if (de < 150) {
                int depth = 6 + (int) (noise3.noise(x / 9.0 + 3) * 3);
                for (int y = surf[x]; y < surf[x] + depth; y++) world.setRaw(x, y, Tiles.SAND);
            }
        }
        // deserts
        int deserts = world.sizeIndex == 0 ? 1 : 2;
        for (int d = 0; d < deserts; d++) {
            int dw = 60 + rnd.nextInt(70);
            int cx;
            int tries = 0;
            do {
                cx = 120 + rnd.nextInt(W - 240);
                tries++;
            } while ((Math.abs(cx - W / 2) < 150 + dw || (cx + dw / 2 + 20 > snowL && cx - dw / 2 - 20 < snowR)) && tries < 50);
            for (int x = cx - dw / 2; x < cx + dw / 2; x++) {
                if (x < 0 || x >= W) continue;
                double t = 1 - Math.abs(x - cx) / (dw / 2.0);
                int depth = (int) (8 + 26 * Math.sqrt(Math.max(0, t)) + noise.noise(x / 7.0) * 3);
                for (int y = surf[x]; y < surf[x] + depth && y < H; y++) {
                    world.setRaw(x, y, Tiles.SAND);
                }
            }
        }
        // stone in the dirt layer and dirt in the stone layer
        int n = (int) (W * 0.35);
        for (int i = 0; i < n; i++) {
            int x = rnd.nextInt(W);
            int y = surf[x] + 5 + rnd.nextInt(Math.max(1, rock[x] - surf[x]));
            runner(x, y, 1.5 + rnd.nextDouble() * 2.5, 4 + rnd.nextInt(8), Tiles.STONE, false);
        }
        n = W * H / 1800;
        for (int i = 0; i < n; i++) {
            int x = rnd.nextInt(W);
            int y = rock[x] + rnd.nextInt(Math.max(1, H - rock[x] - 5));
            double depthT = (double) (y - rock[x]) / (H - rock[x]);
            if (rnd.nextDouble() < depthT * 0.8) continue; // less dirt deeper down
            runner(x, y, 2 + rnd.nextDouble() * 2.5, 5 + rnd.nextInt(10), Tiles.DIRT, false);
        }
        // clay near the surface
        n = (int) (W * 0.1);
        for (int i = 0; i < n; i++) {
            int x = rnd.nextInt(W);
            int y = surf[x] + 2 + rnd.nextInt(14);
            runner(x, y, 1.5 + rnd.nextDouble() * 1.5, 3 + rnd.nextInt(6), Tiles.CLAY, false);
        }
        // sand pockets deep down
        n = W * H / 16000;
        for (int i = 0; i < n; i++) {
            int x = rnd.nextInt(W);
            int y = rock[x] + rnd.nextInt(Math.max(1, H - rock[x] - 5));
            runner(x, y, 2 + rnd.nextDouble() * 2, 4 + rnd.nextInt(8), Tiles.SAND, false);
        }
    }

    // ---- caves -----------------------------------------------------------

    private void caves() {
        // large caverns and passages from noise
        for (int x = 0; x < W; x++) {
            if (x % 64 == 0) stage("Digging caves", 0.30 + 0.2 * x / W);
            for (int y = surf[x] + 12; y < H - 3; y++) {
                double depth = (double) (y - rock[x]) / (H - rock[x]);
                double n = noise.fractal(x / 55.0, y / 32.0, 3);
                double thr = y < rock[x] ? 0.34 : 0.25 - 0.05 * Math.min(1, Math.max(0, depth));
                boolean carve = n > thr;
                if (!carve && y > rock[x] + 5) {
                    double t = noise2.fractal(x / 150.0, y / 80.0, 2);
                    carve = Math.abs(t) < 0.018 + 0.012 * Math.min(1, Math.max(0, depth));
                }
                if (carve && x > 3 && x < W - 4 && !oceanZone(x, y)) world.setRaw(x, y, Tiles.AIR);
            }
        }
        // worm-like tunnels
        int worms = W * H / 7000;
        for (int i = 0; i < worms; i++) {
            if (i % 20 == 0) stage("Digging tunnels", 0.50 + 0.1 * i / worms);
            int x = rnd.nextInt(W);
            int y = surf[x] + 10 + rnd.nextInt(Math.max(1, H - surf[x] - 20));
            worm(x, y, rnd.nextDouble() * Math.PI * 2, 40 + rnd.nextInt(140), 1.2 + rnd.nextDouble() * 2.2);
        }
        // cave entrances at the surface
        int entrances = Math.max(4, W / 150);
        for (int i = 0; i < entrances; i++) {
            int x;
            int tries = 0;
            do {
                x = 180 + rnd.nextInt(W - 360);
                tries++;
            } while (Math.abs(x - W / 2) < 50 && tries < 30);
            double ang = Math.PI / 2 + (rnd.nextDouble() - 0.5) * 1.2;
            worm(x, surf[x] - 1, ang, 50 + rnd.nextInt(110), 1.4 + rnd.nextDouble() * 1.3);
        }
    }

    private void worm(double x, double y, double ang, int len, double radius) {
        double turn = 0;
        for (int s = 0; s < len; s++) {
            double r = radius * (0.75 + 0.5 * noise3.noise(s / 9.0, x / 10.0 + 0.3));
            carve(x, y, r);
            turn += (rnd.nextDouble() - 0.5) * 0.18;
            turn *= 0.9;
            ang += turn;
            // keep tunnels mostly horizontal
            x += Math.cos(ang) * 1.2;
            y += Math.sin(ang) * 0.9;
            if (y < 5 || y > H - 5 || x < 5 || x > W - 5) break;
        }
    }

    /** Near the oceans caves would drain the sea, so none are dug there. */
    private boolean oceanZone(int x, int y) {
        return Math.min(x, W - 1 - x) < 175 && y < seaLevel + 45;
    }

    private void carve(double cx, double cy, double r) {
        int x0 = (int) Math.floor(cx - r), x1 = (int) Math.ceil(cx + r);
        int y0 = (int) Math.floor(cy - r), y1 = (int) Math.ceil(cy + r);
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                if (x < 3 || y < 1 || x >= W - 3 || y >= H - 3 || oceanZone(x, y)) continue;
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                if (dx * dx + dy * dy <= r * r) world.setRaw(x, y, Tiles.AIR);
            }
    }

    /** A Terraria "TileRunner"-style blob that only replaces solid tiles. */
    private void runner(double x, double y, double radius, int steps, int type, boolean ore) {
        double vx = rnd.nextDouble() * 2 - 1, vy = rnd.nextDouble() * 2 - 1;
        for (int s = 0; s < steps; s++) {
            double r = radius * (1 - 0.5 * s / (double) steps) * (0.8 + rnd.nextDouble() * 0.4);
            int x0 = (int) Math.floor(x - r), x1 = (int) Math.ceil(x + r);
            int y0 = (int) Math.floor(y - r), y1 = (int) Math.ceil(y + r);
            for (int yy = y0; yy <= y1; yy++)
                for (int xx = x0; xx <= x1; xx++) {
                    if (!world.inside(xx, yy)) continue;
                    double dx = xx + 0.5 - x, dy = yy + 0.5 - y;
                    if (dx * dx + dy * dy > r * r) continue;
                    int cur = world.tile(xx, yy);
                    boolean ok = ore ? (cur == Tiles.DIRT || cur == Tiles.STONE || cur == Tiles.CLAY || cur == Tiles.MUD
                                    || cur == Tiles.EBONSTONE || cur == Tiles.SNOW || cur == Tiles.ICE)
                                     : (cur == Tiles.DIRT || cur == Tiles.STONE);
                    if (ok) world.setRaw(xx, yy, type);
                }
            x += vx;
            y += vy;
            vx = Math.max(-1, Math.min(1, vx + (rnd.nextDouble() - 0.5) * 0.6));
            vy = Math.max(-1, Math.min(1, vy + (rnd.nextDouble() - 0.5) * 0.6));
        }
    }

    // ---- ores ------------------------------------------------------------

    /** A blob that replaces only the given tile types. */
    private void runner(double x, double y, double radius, int steps, int type, int... replace) {
        double vx = rnd.nextDouble() * 2 - 1, vy = rnd.nextDouble() * 2 - 1;
        for (int s = 0; s < steps; s++) {
            double r = radius * (1 - 0.5 * s / (double) steps) * (0.8 + rnd.nextDouble() * 0.4);
            for (int yy = (int) Math.floor(y - r); yy <= (int) Math.ceil(y + r); yy++)
                for (int xx = (int) Math.floor(x - r); xx <= (int) Math.ceil(x + r); xx++) {
                    if (!world.inside(xx, yy)) continue;
                    double dx = xx + 0.5 - x, dy = yy + 0.5 - y;
                    if (dx * dx + dy * dy > r * r) continue;
                    int cur = world.tile(xx, yy);
                    for (int t : replace) if (cur == t) { world.setRaw(xx, yy, type); break; }
                }
            x += vx;
            y += vy;
            vx = Math.max(-1, Math.min(1, vx + (rnd.nextDouble() - 0.5) * 0.6));
            vy = Math.max(-1, Math.min(1, vy + (rnd.nextDouble() - 0.5) * 0.6));
        }
    }

    private void ores() {
        // hellstone in the ash of the underworld
        int hell = (int) (W / 8 * ore[HELLSTONE]);
        for (int i = 0; i < hell; i++) {
            int x = rnd.nextInt(W);
            int y = world.underworld + 2 + rnd.nextInt(Math.max(1, H - world.underworld - 4));
            runner(x, y, 1.6 + rnd.nextDouble(), 4 + rnd.nextInt(5), Tiles.HELLSTONE, Tiles.ASH);
        }
        double area = (double) W * (world.underworld);
        oreVeins(Tiles.COPPER, (int) (area / 2200 * ore[COPPER]), 0.0, 0.85, 1.6);
        oreVeins(Tiles.IRON, (int) (area / 2800 * ore[IRON]), 0.26, 1.0, 1.6);
        oreVeins(Tiles.SILVER, (int) (area / 3800 * ore[SILVER]), 0.36, 1.0, 1.5);
        oreVeins(Tiles.GOLD, (int) (area / 5000 * ore[GOLD]), 0.46, 1.0, 1.5);
    }

    private void oreVeins(int type, int count, double minFrac, double maxFrac, double radius) {
        for (int i = 0; i < count; i++) {
            int x = rnd.nextInt(W);
            int top = Math.max(surf[x] + 4, (int) (H * minFrac));
            int bot = Math.min(H - 4, (int) (H * maxFrac));
            if (bot <= top) continue;
            int y = top + rnd.nextInt(bot - top);
            runner(x, y, radius * (0.7 + rnd.nextDouble() * 0.6), 3 + rnd.nextInt(6), type, true);
        }
    }

    // ---- grass, trees and decorations ------------------------------------------

    private boolean exposed(int x, int y) {
        return world.tile(x, y - 1) == Tiles.AIR || world.tile(x - 1, y) == Tiles.AIR
                || world.tile(x + 1, y) == Tiles.AIR || world.tile(x, y + 1) == Tiles.AIR;
    }

    private void grass() {
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < world.underworld; y++) {
                int t = world.tile(x, y);
                if (t == Tiles.MUD) {
                    // jungle grass grows on mud everywhere, even in caves
                    if (exposed(x, y)) world.setRaw(x, y, Tiles.JUNGLE_GRASS);
                } else if (t == Tiles.DIRT && y < world.surfaceLevel + 4 && exposed(x, y)) {
                    world.setRaw(x, y, inCorruption(x, y) ? Tiles.CORRUPT_GRASS : Tiles.GRASS);
                }
            }
        }
    }

    private void trees() {
        int last = -100;
        for (int x = 8; x < W - 8; x++) {
            int top = world.surfaceAt(x);
            if (top >= H || !Tiles.isTreeGround(world.tile(x, top))) continue;
            boolean jungle = world.tile(x, top) == Tiles.JUNGLE_GRASS;
            if (x - last < (jungle ? 3 : 6)) continue;
            double forest = noise3.fractal(x / 90.0 + 400, 2);
            double chance = forest > 0.0 ? 0.55 : forest > -0.2 ? 0.2 : 0.03;
            if (jungle) chance = 0.75;
            if (world.tile(x, top) == Tiles.SNOW) chance = 0.45;
            if (Math.abs(x - W / 2) < 4) continue; // no tree on the spawn point
            if (rnd.nextDouble() > chance) continue;
            if (growTree(world, x, top, rnd)) last = x;
        }
    }

    /** Grows a tree whose roots sit on top of tile (x, groundY); the kind depends on the grass. */
    static boolean growTree(World w, int x, int groundY, Random rnd) {
        int ground = w.tile(x, groundY);
        if (ground == Tiles.SNOW) return growPine(w, x, groundY, rnd);
        if (!Tiles.isGrass(ground)) return false;
        int trunk = ground == Tiles.JUNGLE_GRASS ? Tiles.MAHOGANY_TRUNK : ground == Tiles.CORRUPT_GRASS ? Tiles.EBON_TRUNK : Tiles.TRUNK;
        int leaves = Tiles.leavesOf(trunk);
        if (!Tiles.SOLID[w.tile(x - 1, groundY)] || !Tiles.SOLID[w.tile(x + 1, groundY)]) return false;
        boolean jungle = trunk == Tiles.MAHOGANY_TRUNK;
        int height = jungle ? 10 + rnd.nextInt(10) : 6 + rnd.nextInt(8);
        double radius = jungle ? 3.0 + rnd.nextDouble() * 1.2 : 2.3 + rnd.nextDouble() * 1.0;
        if (trunk == Tiles.EBON_TRUNK) radius *= 0.8;
        int topY = groundY - height;
        int canopyTop = topY - (int) Math.ceil(radius) - 1;
        if (canopyTop < 2) return false;
        int reach = (int) Math.ceil(radius);
        // room is needed for the trunk and the canopy
        for (int y = canopyTop; y < groundY; y++)
            for (int dx = -2; dx <= 2; dx++) {
                int t = w.tile(x + dx, y);
                if (Tiles.SOLID[t] || Tiles.isTrunk(t)) return false;
                if (dx == 0 && !Tiles.PLACE_OVER[t] && t != Tiles.SAPLING) return false;
            }
        // no other trees too close (canopies must not touch)
        int gap = jungle ? reach + 2 : reach * 2;
        for (int dx = -gap; dx <= gap; dx++)
            for (int y = canopyTop; y < groundY; y++)
                if (dx != 0 && Tiles.isTrunk(w.tile(x + dx, y))) return false;
        for (int y = topY; y < groundY; y++) w.set(x, y, trunk);
        int cy = topY - 1;
        int r = (int) Math.ceil(radius);
        for (int dy = -r - 1; dy <= r; dy++)
            for (int dx = -r; dx <= r; dx++) {
                double d = dx * dx + dy * dy * (jungle ? 1.6 : 1.25);
                if (d > radius * radius) continue;
                int xx = x + dx, yy = cy + dy;
                int t = w.tile(xx, yy);
                if (t == Tiles.AIR || Tiles.PLACE_OVER[t]) w.set(xx, yy, leaves);
            }
        // branches
        for (int y = topY + 2; y < groundY - 2; y++) {
            if (rnd.nextDouble() < 0.18) {
                int side = rnd.nextBoolean() ? 1 : -1;
                if (w.tile(x + side, y) == Tiles.AIR && w.tile(x + side, y - 1) != leaves
                        && w.tile(x + side, y + 1) != leaves) {
                    w.set(x + side, y, leaves);
                }
            }
        }
        return true;
    }

    /** A boreal pine on snow: a bare trunk under a tiered cone of snowy needles. */
    private static boolean growPine(World w, int x, int groundY, Random rnd) {
        if (!Tiles.SOLID[w.tile(x - 1, groundY)] || !Tiles.SOLID[w.tile(x + 1, groundY)]) return false;
        int trunkLen = 3 + rnd.nextInt(3), coneH = 6 + rnd.nextInt(5);
        int coneBottom = groundY - trunkLen, coneTop = coneBottom - coneH;
        int trunkTop = coneBottom - 2;
        if (coneTop < 2) return false;
        for (int y = coneTop - 1; y < groundY; y++)
            for (int dx = -3; dx <= 3; dx++) {
                int t = w.tile(x + dx, y);
                if (Tiles.SOLID[t] || Tiles.isTrunk(t)) return false;
                if (dx == 0 && !Tiles.PLACE_OVER[t] && t != Tiles.SAPLING) return false;
            }
        for (int dx = -5; dx <= 5; dx++)
            for (int y = coneTop; y < groundY; y++)
                if (dx != 0 && Tiles.isTrunk(w.tile(x + dx, y))) return false;
        for (int y = trunkTop; y < groundY; y++) w.set(x, y, Tiles.BOREAL_TRUNK);
        for (int y = coneTop; y <= coneBottom; y++) {
            int t = y - coneTop;
            int half = (int) Math.round(t * 0.45);
            if (t > 2 && t % 3 == 0) half = Math.max(1, half - 1);
            half = Math.min(3, half);
            for (int dx = -half; dx <= half; dx++) {
                int tt = w.tile(x + dx, y);
                if (tt == Tiles.AIR || Tiles.PLACE_OVER[tt]) w.set(x + dx, y, Tiles.BOREAL_LEAVES);
            }
        }
        return true;
    }

    private void decorations() {
        for (int x = 1; x < W - 1; x++) {
            for (int y = 1; y < world.underworld - 1; y++) {
                int t = world.tile(x, y);
                if (t == Tiles.GRASS && y < world.surfaceLevel + 4 && world.tile(x, y - 1) == Tiles.AIR) {
                    double v = rnd.nextDouble();
                    if (v < 0.06) world.setRaw(x, y - 1, Tiles.FLOWER);
                    else if (v < 0.075) world.setRaw(x, y - 1, Tiles.MUSHROOM);
                    else if (v < 0.42) world.setRaw(x, y - 1, Tiles.TUFT);
                } else if (t == Tiles.JUNGLE_GRASS) {
                    if (world.tile(x, y - 1) == Tiles.AIR) {
                        double v = rnd.nextDouble();
                        if (v < 0.04 && y > world.surfaceLevel) world.setRaw(x, y - 1, Tiles.JUNGLE_SPORE);
                        else if (v < 0.5) world.setRaw(x, y - 1, Tiles.JUNGLE_PLANT);
                    }
                    if (world.tile(x, y + 1) == Tiles.AIR && rnd.nextInt(4) == 0) {
                        int len = 2 + rnd.nextInt(7);
                        for (int k = 1; k <= len && world.tile(x, y + k) == Tiles.AIR; k++) world.setRaw(x, y + k, Tiles.VINE);
                    }
                } else if (t == Tiles.CORRUPT_GRASS && world.tile(x, y - 1) == Tiles.AIR && rnd.nextInt(3) == 0) {
                    world.setRaw(x, y - 1, Tiles.CORRUPT_PLANT);
                }
            }
        }
    }

    // ---- life crystals and pots --------------------------------------------------

    /** Whether a w x h object fits with its bottom row at y (air above solid ground). */
    private boolean fits(int x, int y, int w, int h) {
        for (int dx = 0; dx < w; dx++) {
            if (!Tiles.SOLID[world.tile(x + dx, y + 1)] || !world.inside(x + dx, y + 1)) return false;
            for (int dy = 0; dy < h; dy++) if (world.tile(x + dx, y - dy) != Tiles.AIR) return false;
        }
        return true;
    }

    /** Finds a cave floor near (x, y): searches downwards for air above ground. */
    private int floorBelow(int x, int y, int maxDrop) {
        for (int k = 0; k < maxDrop && y < H - 3; k++, y++) {
            if (world.tile(x, y) == Tiles.AIR && Tiles.SOLID[world.tile(x, y + 1)]) return y;
        }
        return -1;
    }

    private void treasures() {
        // life crystals in the caverns
        int crystals = Math.max(8, W * H / 60000);
        for (int i = 0, placed = 0; i < crystals * 60 && placed < crystals; i++) {
            int x = 10 + rnd.nextInt(W - 20);
            int y = world.rockLevel + rnd.nextInt(Math.max(1, H - world.rockLevel - 10));
            y = floorBelow(x, y, 40);
            if (y < 0 || !fits(x, y, 2, 2)) continue;
            world.placeFurniture(Tiles.LIFE_CRYSTAL, x, y - 1, 0);
            placed++;
        }
        // pots on cave floors
        int pots = W * H / 3500;
        for (int i = 0, placed = 0; i < pots * 20 && placed < pots; i++) {
            int x = 10 + rnd.nextInt(W - 20);
            int y = world.surfaceLevel + 5 + rnd.nextInt(Math.max(1, H - world.surfaceLevel - 15));
            y = floorBelow(x, y, 30);
            if (y < 0 || !fits(x, y, 2, 2)) continue;
            world.placeFurniture(Tiles.POT, x, y - 1, 0);
            placed++;
        }
    }

    private void spawn() {
        int x = W / 2;
        int y = world.surfaceAt(x);
        // make sure there is headroom
        while (y > 3 && (world.solid(x, y - 1) || world.solid(x, y - 2) || world.solid(x, y - 3))) y--;
        world.spawnX = x;
        world.spawnY = y;
    }
}
