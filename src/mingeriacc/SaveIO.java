package mingeriacc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Saving worlds, characters and settings (on Windows in %APPDATA%\Mingeriacc).
 *
 * A world file is GZIP data: a fixed header (name, size, time...) followed by
 * named chunks (tag, length, bytes) up to an "END" tag. Unknown chunks are
 * skipped, so new data can be added without breaking older saves. Version 1
 * files (game 0.1) had no chunks and are still loaded. Up to version 3 (0.3)
 * the player was saved inside the world; since version 4 characters have files
 * of their own (same chunk format), and the players of old worlds are moved
 * into character files once.
 */
final class SaveIO {
    private SaveIO() {}

    private static final String MAGIC = "MINGERIACC";
    /** Header used before the game was renamed from Mineriacc; still accepted when loading. */
    private static final String OLD_MAGIC = "MINERIACC";
    static final int VERSION = 4;
    private static final String PLAYER_MAGIC = "MINGERIACC_PLAYER";
    static final int PLAYER_VERSION = 1;

    private static boolean isWorldHeader(String s) {
        return MAGIC.equals(s) || OLD_MAGIC.equals(s);
    }

    static File baseDir() {
        // -Dmingeriacc.home=folder keeps everything in another folder (tests, portable use)
        String home = System.getProperty("mingeriacc.home");
        if (home != null) {
            File f = new File(home);
            f.mkdirs();
            return f;
        }
        String appdata = System.getenv("APPDATA");
        File base = appdata != null ? new File(appdata, "Mingeriacc")
                : new File(System.getProperty("user.home"), ".mingeriacc");
        if (!base.exists()) {
            // the game used to be called Mineriacc: move its saves and settings over
            File old = appdata != null ? new File(appdata, "Mineriacc")
                    : new File(System.getProperty("user.home"), ".mineriacc");
            if (old.isDirectory() && !old.renameTo(base)) return old;
        }
        base.mkdirs();
        return base;
    }

    static File worldDir() {
        File d = new File(baseDir(), "worlds");
        if (!d.exists()) {
            // version 0.1 kept worlds in a folder called "maailmat": move them over
            File old = new File(baseDir(), "maailmat");
            if (old.isDirectory() && !old.renameTo(d)) return old;
        }
        d.mkdirs();
        return d;
    }

    static File playerDir() {
        return new File(baseDir(), "players");
    }

    static final class WorldInfo {
        File file;
        String name;
        int sizeIndex, day;
        long modified;
    }

    static List<WorldInfo> listWorlds() {
        List<WorldInfo> out = new ArrayList<>();
        File[] files = worldDir().listFiles((d, n) -> n.endsWith(".mwld"));
        if (files == null) return out;
        for (File f : files) {
            try (DataInputStream in = open(f)) {
                if (!isWorldHeader(in.readUTF())) continue;
                in.readInt();
                WorldInfo wi = new WorldInfo();
                wi.file = f;
                wi.name = in.readUTF();
                in.readLong();
                wi.sizeIndex = in.readInt();
                in.readInt();
                in.readInt();
                in.readInt();
                in.readInt();
                in.readDouble();
                wi.day = in.readInt();
                wi.modified = f.lastModified();
                out.add(wi);
            } catch (IOException e) {
                System.err.println("Could not read world " + f + ": " + e);
            }
        }
        out.sort((a, b) -> Long.compare(b.modified, a.modified));
        return out;
    }

    private static DataInputStream open(File f) throws IOException {
        return new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f), 1 << 16)));
    }

    private static String fileBase(String name, String fallback) {
        String base = name.toLowerCase().replace('ä', 'a').replace('ö', 'o').replace('å', 'a')
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return base.isEmpty() ? fallback : base;
    }

    static File newWorldFile(String name) {
        String base = fileBase(name, "world");
        File f = new File(worldDir(), base + ".mwld");
        int n = 2;
        while (f.exists()) f = new File(worldDir(), base + "_" + n++ + ".mwld");
        return f;
    }

    private static File newPlayerFile(String name) {
        File dir = playerDir();
        dir.mkdirs();
        String base = fileBase(name, "player");
        File f = new File(dir, base + ".mplr");
        int n = 2;
        while (f.exists()) f = new File(dir, base + "_" + n++ + ".mplr");
        return f;
    }

    /** The id of a world saved before worlds had ids (it can be worked out again from the header). */
    static long legacyId(long seed, String name, int w, int h) {
        long id = seed * 0x9E3779B97F4A7C15L + name.hashCode() * 31L + w * 7919L + h;
        return id == 0 ? 1 : id;
    }

    // ---- chunks ----------------------------------------------------------------

    private interface ChunkWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private static void chunk(DataOutputStream out, String tag, byte[] data) throws IOException {
        out.writeUTF(tag);
        out.writeInt(data.length);
        out.write(data);
    }

    private static void chunk(DataOutputStream out, String tag, ChunkWriter w) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream d = new DataOutputStream(bytes)) {
            w.write(d);
        }
        chunk(out, tag, bytes.toByteArray());
    }

    private static DataOutputStream create(File tmp) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(tmp), 1 << 16), 1 << 16));
    }

    // ---- saving worlds ----------------------------------------------------------

    static void save(Game g, File file) throws IOException {
        World w = g.world;
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (DataOutputStream out = create(tmp)) {
            out.writeUTF(MAGIC);
            out.writeInt(VERSION);
            out.writeUTF(w.name);
            out.writeLong(w.seed);
            out.writeInt(w.sizeIndex);
            out.writeInt(w.w);
            out.writeInt(w.h);
            out.writeInt(w.surfaceLevel);
            out.writeInt(w.rockLevel);
            out.writeDouble(w.time);
            out.writeInt(w.day);
            out.writeInt(w.spawnX);
            out.writeInt(w.spawnY);

            chunk(out, "TILES", w.tiles);
            chunk(out, "WALLS", w.walls);
            chunk(out, "META", w.meta);
            // an item held on the cursor goes into the character's inventory; what doesn't fit lies on the ground
            Player p = g.player;
            int cursorLeft = g.cursorItem != 0 ? g.inv.copy().add(g.cursorItem, g.cursorCount) : 0;
            chunk(out, "DROPS", d -> {
                d.writeInt(g.drops.size() + (cursorLeft > 0 ? 1 : 0));
                for (Entities.Drop dr : g.drops) {
                    d.writeShort(dr.item);
                    d.writeInt(dr.count);
                    d.writeDouble(dr.x);
                    d.writeDouble(dr.y);
                }
                if (cursorLeft > 0) {
                    d.writeShort(g.cursorItem);
                    d.writeInt(cursorLeft);
                    d.writeDouble(p.centerX() - 3);
                    d.writeDouble(p.y + 4);
                }
            });
            chunk(out, "LIQUID", w.liquid);
            chunk(out, "LQTYPE", w.lqType);
            chunk(out, "WORLD2", d -> d.writeInt(w.underworld));
            chunk(out, "CHESTS", d -> {
                d.writeInt(w.chests.size());
                for (World.Chest c : w.chests.values()) {
                    d.writeInt(c.x);
                    d.writeInt(c.y);
                    for (int i = 0; i < World.Chest.SIZE; i++) {
                        d.writeShort(c.id[i]);
                        d.writeInt(c.count[i]);
                    }
                }
            });
            chunk(out, "NPCS", d -> {
                d.writeInt(g.town.size());
                for (Mob m : g.town) {
                    d.writeInt(m.type);
                    d.writeDouble(m.x);
                    d.writeDouble(m.y);
                    d.writeInt(m.homeX);
                    d.writeInt(m.homeY);
                    d.writeInt(m.life);
                    d.writeUTF(m.name);
                }
            });
            chunk(out, "WORLD3", d -> {
                d.writeLong(w.id);
                d.writeInt((w.downedEye ? 1 : 0) | (w.bloodMoon ? 2 : 0) | (w.slimeRain ? 4 : 0));
                d.writeInt(w.slimeRainKills);
            });
            chunk(out, "MOBS", d -> {
                int n = 0;
                for (Mob m : g.mobs) if (!m.dead) n++;
                d.writeInt(n);
                for (Mob m : g.mobs) {
                    if (m.dead) continue;
                    d.writeInt(m.type);
                    d.writeDouble(m.x);
                    d.writeDouble(m.y);
                    d.writeDouble(m.vx);
                    d.writeDouble(m.vy);
                    d.writeInt(m.life);
                    d.writeInt(m.dir);
                    d.writeInt(m.variant);
                    d.writeInt(m.ai.length);
                    for (double v : m.ai) d.writeDouble(v);
                    d.writeInt(m.timer);
                    d.writeInt(m.attackTimer);
                    d.writeInt(m.onFire);
                    d.writeInt(m.poisoned);
                    d.writeInt(m.frostburn);
                    d.writeDouble(m.rot);
                }
            });
            out.writeUTF("END");
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // ---- loading worlds ---------------------------------------------------------

    static Game load(File file, Audio audio) throws IOException {
        int ver;
        Game g;
        try (DataInputStream in = open(file)) {
            if (!isWorldHeader(in.readUTF())) throw new IOException("Not a Mingeriacc world");
            ver = in.readInt();
            if (ver > VERSION) throw new IOException("The world was made with a newer version of the game");
            String name = in.readUTF();
            long seed = in.readLong();
            int sizeIndex = in.readInt();
            int w = in.readInt(), h = in.readInt();
            World world = new World(w, h);
            world.name = name;
            world.seed = seed;
            world.id = legacyId(seed, name, w, h);
            world.sizeIndex = sizeIndex;
            world.surfaceLevel = in.readInt();
            world.rockLevel = in.readInt();
            world.time = in.readDouble();
            world.day = in.readInt();
            world.spawnX = in.readInt();
            world.spawnY = in.readInt();
            g = new Game(world, audio);
            boolean hasNpcs = false;
            if (ver == 1) loadV1(in, g);
            else hasNpcs = loadChunks(in, g);
            for (int i = 0; i < world.tiles.length; i++) {
                if ((world.tiles[i] & 0xff) >= Tiles.COUNT) world.tiles[i] = 0;
                if ((world.walls[i] & 0xff) >= Tiles.WALL_COUNT) world.walls[i] = 0;
            }
            world.updateAllSky();
            // chests without a record (e.g. from world generation of a newer build) get an empty one
            for (int i = 0; i < world.tiles.length; i++) {
                int t = world.tiles[i] & 0xff;
                if (Tiles.isChest(t) && (world.meta[i] & 63) == 0 && !world.chests.containsKey(i))
                    world.chests.put(i, new World.Chest(i % world.w, i / world.w));
            }
            if (!hasNpcs) {
                // worlds from before town folk: the guide comes along
                g.town.add(Town.create(g, Mobs.GUIDE, world.spawnX * Tiles.T + 16, world.spawnY * Tiles.T));
            }
        }
        World world = g.world;
        if (ver <= 3) {
            // keep a copy of the world as the old version saved it, then bring it up to date
            backup(file, "backup_0.3");
            int gems = WorldGen.retrofit(world);
            if (gems > 0) g.message("Gems and moss have appeared deep in the caverns", Pal.UI_GOOD);
        }
        int trees = world.removeFloatingTrees();
        if (trees > 0) System.err.println("Removed " + trees + " floating trees");
        world.liquids.activateAll();
        g.findStations();
        g.detectBiome();
        return g;
    }

    private static void backup(File file, String folder) {
        File dir = new File(file.getParentFile(), folder);
        File b = new File(dir, file.getName());
        if (b.exists()) return;
        try {
            dir.mkdirs();
            Files.copy(file.toPath(), b.toPath());
        } catch (IOException e) {
            System.err.println("Could not back up " + file + ": " + e);
        }
    }

    /** Reads the chunks; returns true if the save had town folk data. */
    private static boolean loadChunks(DataInputStream in, Game g) throws IOException {
        World w = g.world;
        boolean npcs = false;
        while (true) {
            String tag = in.readUTF();
            if (tag.equals("END")) break;
            int len = in.readInt();
            byte[] data = new byte[len];
            in.readFully(data);
            DataInputStream d = new DataInputStream(new ByteArrayInputStream(data));
            switch (tag) {
                case "TILES": System.arraycopy(data, 0, w.tiles, 0, Math.min(len, w.tiles.length)); break;
                case "WALLS": System.arraycopy(data, 0, w.walls, 0, Math.min(len, w.walls.length)); break;
                case "META": System.arraycopy(data, 0, w.meta, 0, Math.min(len, w.meta.length)); break;
                // the player inside worlds of version 3 and older (moved into a character file once)
                case "PLAYER": {
                    Player p = g.player;
                    p.x = d.readDouble();
                    p.y = d.readDouble();
                    p.dir = d.readInt();
                    p.life = d.readInt();
                    p.lifeMax = Math.max(100, Math.min(Player.MAX_LIFE_CAP, d.readInt()));
                    p.life = Math.max(1, Math.min(p.lifeMax, p.life));
                    p.potionSickness = d.readInt();
                    break;
                }
                case "INV": readInventory(d, g.inv); break;
                case "PLAYER2":
                    g.player.spawnX = d.readInt();
                    g.player.spawnY = d.readInt();
                    break;
                case "EQUIP": readEquipment(d, g.inv); break;
                case "DROPS": readDrops(d, g); break;
                case "LIQUID": System.arraycopy(data, 0, w.liquid, 0, Math.min(len, w.liquid.length)); break;
                case "LQTYPE": System.arraycopy(data, 0, w.lqType, 0, Math.min(len, w.lqType.length)); break;
                case "WORLD2": w.underworld = Math.max(1, Math.min(w.h, d.readInt())); break;
                case "CHESTS": {
                    int n = d.readInt();
                    for (int k = 0; k < n; k++) {
                        World.Chest c = new World.Chest(d.readInt(), d.readInt());
                        for (int i = 0; i < World.Chest.SIZE; i++) {
                            int id = validItem(d.readShort());
                            int cnt = d.readInt();
                            c.id[i] = cnt > 0 ? id : 0;
                            c.count[i] = id != 0 ? cnt : 0;
                        }
                        if (w.inside(c.x, c.y)) w.chests.put(c.x + c.y * w.w, c);
                    }
                    break;
                }
                case "NPCS": {
                    npcs = true;
                    int n = d.readInt();
                    for (int k = 0; k < n; k++) {
                        int type = d.readInt();
                        double x = d.readDouble(), y = d.readDouble();
                        int hx = d.readInt(), hy = d.readInt(), life = d.readInt();
                        String name = d.readUTF();
                        if (type < 0 || type >= Mobs.COUNT || !Mobs.isTown(type)) continue;
                        Mob m = Town.create(g, type, x + Mobs.W[type] / 2.0, y + Mobs.H[type]);
                        m.homeX = hx;
                        m.homeY = hy;
                        m.life = Math.max(1, Math.min(m.lifeMax, life));
                        m.name = name;
                        g.town.add(m);
                    }
                    break;
                }
                case "WORLD3": {
                    long id = d.readLong();
                    if (id != 0) w.id = id;
                    int flags = d.readInt();
                    w.downedEye = (flags & 1) != 0;
                    w.bloodMoon = (flags & 2) != 0;
                    w.slimeRain = (flags & 4) != 0;
                    w.slimeRainKills = d.readInt();
                    break;
                }
                case "MOBS": readMobs(d, g); break;
                default: break; // unknown chunk from a newer version: skip
            }
        }
        return npcs;
    }

    private static void readMobs(DataInputStream d, Game g) throws IOException {
        int n = d.readInt();
        for (int k = 0; k < n; k++) {
            int type = d.readInt();
            double x = d.readDouble(), y = d.readDouble(), vx = d.readDouble(), vy = d.readDouble();
            int life = d.readInt(), dir = d.readInt(), variant = d.readInt();
            int an = d.readInt();
            double[] ai = new double[an];
            for (int i = 0; i < an; i++) ai[i] = d.readDouble();
            int timer = d.readInt(), attack = d.readInt(), fire = d.readInt(), poison = d.readInt(), frost = d.readInt();
            double rot = d.readDouble();
            if (type <= 0 || type >= Mobs.COUNT || Mobs.isTown(type)) continue;
            Mob m = new Mob(type, x + Mobs.W[type] / 2.0, y + Mobs.H[type]);
            m.vx = vx;
            m.vy = vy;
            m.life = Math.max(1, Math.min(m.lifeMax, life));
            m.dir = dir >= 0 ? 1 : -1;
            m.variant = variant;
            System.arraycopy(ai, 0, m.ai, 0, Math.min(an, m.ai.length));
            m.timer = timer;
            m.attackTimer = attack;
            m.onFire = fire;
            m.poisoned = poison;
            m.frostburn = frost;
            m.rot = rot;
            Boss.afterLoad(m);
            g.mobs.add(m);
        }
    }

    private static int validItem(int id) {
        return id > 0 && id < Items.COUNT ? id : 0;
    }

    private static void readInventory(DataInputStream d, Inventory inv) throws IOException {
        inv.selected = Math.max(0, Math.min(9, d.readInt()));
        int n = d.readInt();
        for (int i = 0; i < n; i++) {
            int id = d.readShort(), c = d.readInt();
            if (i >= Inventory.SIZE) continue;
            if (id <= 0 || id >= Items.COUNT || c <= 0) { id = 0; c = 0; }
            inv.id[i] = id;
            inv.count[i] = c;
        }
        int tid = d.readShort(), tc = d.readInt();
        if (tid > 0 && tid < Items.COUNT && tc > 0) {
            inv.trashId = tid;
            inv.trashCount = tc;
        }
    }

    private static void writeInventory(DataOutputStream d, Inventory inv) throws IOException {
        d.writeInt(inv.selected);
        d.writeInt(Inventory.SIZE);
        for (int i = 0; i < Inventory.SIZE; i++) {
            d.writeShort(inv.id[i]);
            d.writeInt(inv.count[i]);
        }
        d.writeShort(inv.trashId);
        d.writeInt(inv.trashCount);
    }

    private static void readEquipment(DataInputStream d, Inventory inv) throws IOException {
        for (int i = 0; i < 3; i++) inv.armor[i] = validItem(d.readShort());
        int n = d.readInt();
        for (int i = 0; i < n; i++) {
            int id = validItem(d.readShort());
            if (i < inv.acc.length) inv.acc[i] = id;
        }
    }

    private static void writeEquipment(DataOutputStream d, Inventory inv) throws IOException {
        for (int a : inv.armor) d.writeShort(a);
        d.writeInt(inv.acc.length);
        for (int a : inv.acc) d.writeShort(a);
    }

    private static void readDrops(DataInputStream in, Game g) throws IOException {
        int nd = in.readInt();
        for (int i = 0; i < nd; i++) {
            int id = in.readShort();
            int c = in.readInt();
            double x = in.readDouble(), y = in.readDouble();
            if (id > 0 && id < Items.COUNT && c > 0) g.drops.add(new Entities.Drop(x, y, id, c));
        }
    }

    /** The 0.1 format: raw tiles and walls followed by the player and items. */
    private static void loadV1(DataInputStream in, Game g) throws IOException {
        World world = g.world;
        in.readFully(world.tiles);
        in.readFully(world.walls);
        // walls deep down were made by world generation: mark them natural
        for (int y = world.surfaceLevel; y < world.h; y++)
            for (int x = 0; x < world.w; x++) {
                int i = x + y * world.w;
                if (world.walls[i] == Tiles.W_DIRT) world.walls[i] = (byte) Tiles.W_DIRT_N;
                else if (world.walls[i] == Tiles.W_STONE) world.walls[i] = (byte) Tiles.W_STONE_N;
            }
        readV1Player(in, g);
        readDrops(in, g);
    }

    private static void readV1Player(DataInputStream in, Game g) throws IOException {
        g.player.x = in.readDouble();
        g.player.y = in.readDouble();
        g.player.dir = in.readInt();
        g.inv.selected = Math.max(0, Math.min(9, in.readInt()));
        for (int i = 0; i < Inventory.SIZE; i++) {
            int id = in.readShort();
            int c = in.readInt();
            if (id <= 0 || id >= Items.COUNT || c <= 0) { id = 0; c = 0; }
            g.inv.id[i] = id;
            g.inv.count[i] = c;
        }
        // players of 0.1 had no weapon: give the starter sword
        if (g.inv.countOf(Items.COPPER_SHORTSWORD) == 0) g.inv.add(Items.COPPER_SHORTSWORD, 1);
    }

    // ---- characters ----------------------------------------------------------------

    /** All characters, the most recently played first. Moves the players of old worlds out first. */
    static List<Profile> listPlayers() {
        if (!playerDir().exists()) migrateOldWorlds();
        List<Profile> out = new ArrayList<>();
        File[] files = playerDir().listFiles((d, n) -> n.endsWith(".mplr"));
        if (files == null) return out;
        for (File f : files) {
            try {
                Profile p = new Profile();
                p.file = f;
                readPlayer(f, p, null, null);
                out.add(p);
            } catch (IOException e) {
                System.err.println("Could not read character " + f + ": " + e);
            }
        }
        out.sort((a, b) -> Long.compare(b.file.lastModified(), a.file.lastModified()));
        return out;
    }

    /** Names of the characters made from 0.3 worlds during this run (shown once in the menu). */
    static final List<String> migrated = new ArrayList<>();

    /** Worlds of version 3 and older held the player: each such player becomes a character named after its world. */
    private static void migrateOldWorlds() {
        playerDir().mkdirs();
        File[] files = worldDir().listFiles((d, n) -> n.endsWith(".mwld"));
        if (files == null) return;
        for (File f : files) {
            try (DataInputStream in = open(f)) {
                if (!isWorldHeader(in.readUTF())) continue;
                int ver = in.readInt();
                if (ver > 3) continue;
                String name = in.readUTF();
                long seed = in.readLong();
                in.readInt();
                int w = in.readInt(), h = in.readInt();
                in.readInt();
                in.readInt();
                in.readDouble();
                in.readInt();
                int spawnX = in.readInt(), spawnY = in.readInt();
                // read the player chunks into a stand-in game
                Game g = new Game(new World(1, 1), null);
                g.player.x = spawnX * Tiles.T;
                g.player.y = spawnY * Tiles.T - Player.H;
                if (ver == 1) {
                    in.skipNBytes((long) w * h * 2);
                    readV1Player(in, g);
                } else {
                    loadChunks(in, g);
                }
                Profile prof = new Profile();
                prof.name = name;
                prof.file = newPlayerFile(name);
                prof.worlds.put(legacyId(seed, name, w, h),
                        new double[]{g.player.x, g.player.y, g.player.spawnX, g.player.spawnY});
                g.profile = prof;
                writePlayer(prof, g.player, g.inv);
                migrated.add(name);
            } catch (IOException | RuntimeException e) {
                System.err.println("Could not move the player out of " + f + ": " + e);
            }
        }
    }

    /** A new character with the starting kit. */
    static Profile createPlayer(String name, Humanoid.Look look) throws IOException {
        Profile prof = new Profile();
        prof.name = name;
        prof.look.assign(look);
        prof.file = newPlayerFile(name);
        Player p = new Player();
        Inventory inv = new Inventory();
        inv.giveStarterKit();
        writePlayer(prof, p, inv);
        return prof;
    }

    /** Saves the playing character, including where it is in the current world. */
    static void savePlayer(Game g) throws IOException {
        Profile prof = g.profile;
        if (prof == null) return;
        Player p = g.player;
        World w = g.world;
        boolean dead = p.dead;
        prof.worlds.put(w.id, new double[]{dead ? w.spawnX * Tiles.T : p.x, dead ? w.spawnY * Tiles.T - Player.H : p.y,
                p.spawnX, p.spawnY});
        Inventory inv = g.inv.copy();
        if (g.cursorItem != 0) inv.add(g.cursorItem, g.cursorCount);
        int life = p.life;
        if (dead) p.life = p.lifeMax;
        try {
            writePlayer(prof, p, inv);
        } finally {
            p.life = life;
        }
    }

    private static void writePlayer(Profile prof, Player p, Inventory inv) throws IOException {
        File file = prof.file;
        file.getParentFile().mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        Humanoid.Look l = prof.look;
        try (DataOutputStream out = create(tmp)) {
            out.writeUTF(PLAYER_MAGIC);
            out.writeInt(PLAYER_VERSION);
            out.writeUTF(prof.name);
            chunk(out, "LOOK", d -> {
                d.writeInt(l.hairStyle);
                for (int c : new int[]{l.skin, l.skinD, l.hair, l.hairD, l.eye, l.shirt, l.shirtD, l.pants, l.pantsD, l.shoes})
                    d.writeInt(c);
            });
            chunk(out, "STATS", d -> {
                d.writeInt(Math.max(1, p.life));
                d.writeInt(p.lifeMax);
                d.writeInt(p.mana);
                d.writeInt(p.manaMax);
                d.writeInt(p.potionSickness);
                d.writeLong(prof.playTicks);
            });
            chunk(out, "INV", d -> writeInventory(d, inv));
            chunk(out, "EQUIP", d -> writeEquipment(d, inv));
            chunk(out, "WORLDS", d -> {
                d.writeInt(prof.worlds.size());
                for (Map.Entry<Long, double[]> e : prof.worlds.entrySet()) {
                    double[] r = e.getValue();
                    d.writeLong(e.getKey());
                    d.writeDouble(r[0]);
                    d.writeDouble(r[1]);
                    d.writeInt((int) r[2]);
                    d.writeInt((int) r[3]);
                }
            });
            out.writeUTF("END");
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        prof.lifeMax = p.lifeMax;
        prof.manaMax = p.manaMax;
    }

    /** Reads a character file into a profile, and into a player and inventory if given. */
    private static void readPlayer(File f, Profile prof, Player p, Inventory inv) throws IOException {
        try (DataInputStream in = open(f)) {
            if (!PLAYER_MAGIC.equals(in.readUTF())) throw new IOException("Not a Mingeriacc character");
            int ver = in.readInt();
            if (ver > PLAYER_VERSION) throw new IOException("The character was made with a newer version of the game");
            prof.name = in.readUTF();
            prof.worlds.clear();
            while (true) {
                String tag = in.readUTF();
                if (tag.equals("END")) break;
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                DataInputStream d = new DataInputStream(new ByteArrayInputStream(data));
                switch (tag) {
                    case "LOOK": {
                        Humanoid.Look l = prof.look;
                        l.hairStyle = d.readInt();
                        l.skin = d.readInt();
                        l.skinD = d.readInt();
                        l.hair = d.readInt();
                        l.hairD = d.readInt();
                        l.eye = d.readInt();
                        l.shirt = d.readInt();
                        l.shirtD = d.readInt();
                        l.pants = d.readInt();
                        l.pantsD = d.readInt();
                        l.shoes = d.readInt();
                        break;
                    }
                    case "STATS": {
                        int life = d.readInt();
                        prof.lifeMax = Math.max(100, Math.min(Player.MAX_LIFE_CAP, d.readInt()));
                        int mana = d.readInt();
                        prof.manaMax = Math.max(Player.BASE_MANA, Math.min(Player.MAX_MANA_CAP, d.readInt()));
                        int sickness = d.readInt();
                        prof.playTicks = d.readLong();
                        if (p != null) {
                            p.lifeMax = prof.lifeMax;
                            p.life = Math.max(1, Math.min(p.lifeMax, life));
                            p.manaMax = prof.manaMax;
                            p.mana = Math.max(0, mana);
                            p.potionSickness = sickness;
                        }
                        break;
                    }
                    case "INV": if (inv != null) readInventory(d, inv); break;
                    case "EQUIP": if (inv != null) readEquipment(d, inv); break;
                    case "WORLDS": {
                        int n = d.readInt();
                        for (int k = 0; k < n; k++) {
                            long id = d.readLong();
                            double x = d.readDouble(), y = d.readDouble();
                            int sx = d.readInt(), sy = d.readInt();
                            prof.worlds.put(id, new double[]{x, y, sx, sy});
                        }
                        break;
                    }
                    default: break;
                }
            }
        }
    }

    /** Puts a character into a game: its stats and inventory, and its place in this world. */
    static void loadPlayer(Profile prof, Game g) throws IOException {
        Player p = g.player;
        Inventory inv = g.inv;
        inv.clear();
        readPlayer(prof.file, prof, p, inv);
        p.look.assign(prof.look);
        g.profile = prof;
        World w = g.world;
        double[] r = prof.worlds.get(w.id);
        p.spawnX = r != null ? (int) r[2] : -1;
        p.spawnY = r != null ? (int) r[3] : -1;
        if (r != null && r[0] >= 0 && r[1] >= 0 && r[0] < w.w * Tiles.T - Player.W && r[1] < w.h * Tiles.T - Player.H) {
            p.x = r[0];
            p.y = r[1];
            if (p.collides(w, p.x, p.y)) p.spawnAt(w);
        } else {
            p.spawnAt(w);
        }
        p.vx = p.vy = 0;
        p.fallStartY = p.y;
        g.snapCamera();
        g.findStations();
        g.detectBiome();
    }

    static boolean deletePlayer(Profile prof) {
        return prof.file.delete();
    }

    // ---- settings ---------------------------------------------------------

    private static Properties read(File f) {
        Properties p = new Properties();
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Could not read settings: " + e);
            }
        }
        return p;
    }

    static Properties loadSettings() {
        File f = new File(baseDir(), "settings.properties");
        if (f.exists()) return read(f);
        // carry over the settings of version 0.1, which used Finnish key names
        Properties old = read(new File(baseDir(), "asetukset.properties"));
        Properties p = new Properties();
        String[][] keys = {{"musiikki", "music"}, {"aanet", "sfx"}, {"musiikki_pois", "music_muted"},
                {"koko_naytto", "fullscreen"}, {"fps", "fps"}};
        for (String[] k : keys) {
            String v = old.getProperty(k[0]);
            if (v != null) p.setProperty(k[1], v);
        }
        return p;
    }

    static void saveSettings(Properties p) {
        File f = new File(baseDir(), "settings.properties");
        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "Mingeriacc settings");
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e);
        }
    }
}
