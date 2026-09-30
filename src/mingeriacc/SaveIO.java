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
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Saving worlds and settings (on Windows in %APPDATA%\Mingeriacc).
 *
 * A world file is GZIP data: a fixed header (name, size, time...) followed by
 * named chunks (tag, length, bytes) up to an "END" tag. Unknown chunks are
 * skipped, so new data can be added without breaking older saves. Version 1
 * files (game 0.1) had no chunks and are still loaded.
 */
final class SaveIO {
    private SaveIO() {}

    private static final String MAGIC = "MINGERIACC";
    /** Header used before the game was renamed from Mineriacc; still accepted when loading. */
    private static final String OLD_MAGIC = "MINERIACC";
    static final int VERSION = 3;

    private static boolean isWorldHeader(String s) {
        return MAGIC.equals(s) || OLD_MAGIC.equals(s);
    }

    static File baseDir() {
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

    static File newWorldFile(String name) {
        String base = name.toLowerCase().replace('ä', 'a').replace('ö', 'o').replace('å', 'a')
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "world";
        File f = new File(worldDir(), base + ".mwld");
        int n = 2;
        while (f.exists()) f = new File(worldDir(), base + "_" + n++ + ".mwld");
        return f;
    }

    // ---- saving ---------------------------------------------------------------

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

    static void save(Game g, File file) throws IOException {
        World w = g.world;
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(tmp), 1 << 16), 1 << 16))) {
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
            Player p = g.player;
            chunk(out, "PLAYER", d -> {
                boolean dead = p.dead;
                d.writeDouble(dead ? w.spawnX * Tiles.T : p.x);
                d.writeDouble(dead ? w.spawnY * Tiles.T - Player.H : p.y);
                d.writeInt(p.dir);
                d.writeInt(dead ? p.lifeMax : p.life);
                d.writeInt(p.lifeMax);
                d.writeInt(p.potionSickness);
            });
            Inventory inv = g.inv;
            chunk(out, "INV", d -> {
                d.writeInt(inv.selected);
                d.writeInt(Inventory.SIZE);
                for (int i = 0; i < Inventory.SIZE; i++) {
                    d.writeShort(inv.id[i]);
                    d.writeInt(inv.count[i]);
                }
                d.writeShort(inv.trashId);
                d.writeInt(inv.trashCount);
            });
            // an item held on the cursor is saved on the ground next to the player
            chunk(out, "DROPS", d -> {
                boolean cursor = g.cursorItem != 0 && g.cursorCount > 0;
                d.writeInt(g.drops.size() + (cursor ? 1 : 0));
                for (Entities.Drop dr : g.drops) {
                    d.writeShort(dr.item);
                    d.writeInt(dr.count);
                    d.writeDouble(dr.x);
                    d.writeDouble(dr.y);
                }
                if (cursor) {
                    d.writeShort(g.cursorItem);
                    d.writeInt(g.cursorCount);
                    d.writeDouble(p.centerX() - 3);
                    d.writeDouble(p.y + 4);
                }
            });
            chunk(out, "LIQUID", w.liquid);
            chunk(out, "LQTYPE", w.lqType);
            chunk(out, "WORLD2", d -> d.writeInt(w.underworld));
            chunk(out, "PLAYER2", d -> {
                d.writeInt(p.spawnX);
                d.writeInt(p.spawnY);
            });
            chunk(out, "EQUIP", d -> {
                for (int a : inv.armor) d.writeShort(a);
                d.writeInt(inv.acc.length);
                for (int a : inv.acc) d.writeShort(a);
            });
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
            out.writeUTF("END");
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // ---- loading ---------------------------------------------------------------

    static Game load(File file, Audio audio) throws IOException {
        try (DataInputStream in = open(file)) {
            if (!isWorldHeader(in.readUTF())) throw new IOException("Not a Mingeriacc world");
            int ver = in.readInt();
            if (ver > VERSION) throw new IOException("The world was made with a newer version of the game");
            String name = in.readUTF();
            long seed = in.readLong();
            int sizeIndex = in.readInt();
            int w = in.readInt(), h = in.readInt();
            World world = new World(w, h);
            world.name = name;
            world.seed = seed;
            world.sizeIndex = sizeIndex;
            world.surfaceLevel = in.readInt();
            world.rockLevel = in.readInt();
            world.time = in.readDouble();
            world.day = in.readInt();
            world.spawnX = in.readInt();
            world.spawnY = in.readInt();
            Game g = new Game(world, audio);
            boolean hasNpcs = false;
            if (ver == 1) loadV1(in, g);
            else hasNpcs = loadChunks(in, g);
            for (int i = 0; i < world.tiles.length; i++) {
                if ((world.tiles[i] & 0xff) >= Tiles.COUNT) world.tiles[i] = 0;
                if ((world.walls[i] & 0xff) >= Tiles.WALL_COUNT) world.walls[i] = 0;
            }
            world.updateAllSky();
            world.liquids.activateAll();
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
            g.player.fallStartY = g.player.y;
            g.snapCamera();
            g.findStations();
            g.detectBiome();
            return g;
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
                case "INV": {
                    Inventory inv = g.inv;
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
                    break;
                }
                case "DROPS": readDrops(d, g); break;
                case "LIQUID": System.arraycopy(data, 0, w.liquid, 0, Math.min(len, w.liquid.length)); break;
                case "LQTYPE": System.arraycopy(data, 0, w.lqType, 0, Math.min(len, w.lqType.length)); break;
                case "WORLD2": w.underworld = Math.max(1, Math.min(w.h, d.readInt())); break;
                case "PLAYER2":
                    g.player.spawnX = d.readInt();
                    g.player.spawnY = d.readInt();
                    break;
                case "EQUIP": {
                    Inventory inv = g.inv;
                    for (int i = 0; i < 3; i++) inv.armor[i] = validItem(d.readShort());
                    int n = d.readInt();
                    for (int i = 0; i < n; i++) {
                        int id = validItem(d.readShort());
                        if (i < inv.acc.length) inv.acc[i] = id;
                    }
                    break;
                }
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
                default: break; // unknown chunk from a newer version: skip
            }
        }
        return npcs;
    }

    private static int validItem(int id) {
        return id > 0 && id < Items.COUNT ? id : 0;
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
        readDrops(in, g);
        // players of 0.1 had no weapon: give the starter sword
        if (g.inv.countOf(Items.COPPER_SHORTSWORD) == 0) g.inv.add(Items.COPPER_SHORTSWORD, 1);
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
