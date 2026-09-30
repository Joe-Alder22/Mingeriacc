package mingeriacc;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/** Title screen, world selection and creation, settings, help and the pause menu. */
final class Menus {
    private final Main m;
    private Game titleGame;
    private double menuHour = 7.0, menuCam, titleCamY = Double.MAX_VALUE;

    private List<SaveIO.WorldInfo> worlds;
    private int scroll;
    private SaveIO.WorldInfo confirmDelete;
    private String worldError;

    private final StringBuilder nameField = new StringBuilder();
    private final StringBuilder seedField = new StringBuilder();
    private int focus; // 0 = name, 1 = seed
    private int sizeChoice = 0;

    private WorldGen.Progress progress;
    private String genName;
    private long genSeed;
    private int genSize;

    private static final String[] NAMES = {"Mossy Hollow", "Stonebrook", "Pinewood", "Copper Peak", "Oakshore",
            "Quarry Vale", "Goldmere", "Sandridge", "Cliffhaven", "Frostmoor", "Thunderstone", "Sprucedale",
            "Molehill", "Resin Grove", "Bear Hollow", "Highfell"};

    Menus(Main m) {
        this.m = m;
    }

    // ---- background ------------------------------------------------------------

    private void drawTitleBackground(Screen s) {
        if (titleGame == null) {
            World w = WorldGen.generate("Menu", 0, 20260928L, null);
            titleGame = new Game(w, m.audio);
            titleGame.hidePlayer = true;
            titleGame.player.x = 0;
            titleGame.player.y = 0;
        }
        Game g = titleGame;
        World w = g.world;
        menuHour = (menuHour + 0.0035) % 24;
        menuCam += 0.35;
        w.time = menuHour;
        g.ticks++;
        // the camera glides slowly back and forth over the terrain, starting mid-world
        double left = 120 * Tiles.T, span = (w.w - 240) * Tiles.T - s.w;
        double pos = (w.w / 2.0 * Tiles.T - left + menuCam) % (span * 2);
        double cx = pos < span ? left + pos : left + span * 2 - pos;
        int col = (int) ((cx + s.w / 2.0) / Tiles.T);
        double sum = 0;
        int cnt = 0;
        for (int dx = -40; dx <= 40; dx += 4) {
            sum += w.surfaceAt(col + dx);
            cnt++;
        }
        double target = sum / cnt * Tiles.T - s.h * 0.68;
        if (titleCamY == Double.MAX_VALUE) titleCamY = target;
        titleCamY += (target - titleCamY) * 0.02;
        g.camX = cx;
        g.camY = titleCamY;
        g.viewW = s.w;
        g.viewH = s.h;
        // keep the world alive a little (leaves, fireflies)
        g.particles.removeIf(p -> !p.update(w));
        m.renderer.render(s, g, m.frameTicks, 0);
    }

    /** The frozen game view behind a menu (pause). */
    private void drawFrozenGame(Screen s) {
        m.renderer.render(s, m.game, m.frameTicks, 0);
    }

    private void background(Screen s) {
        if (m.game != null) drawFrozenGame(s);
        else drawTitleBackground(s);
    }

    // ---- routing ------------------------------------------------------------

    void render(Screen s, Input in) {
        switch (m.state) {
            case TITLE: title(s, in); break;
            case WORLDS: worldList(s, in); break;
            case CREATE: create(s, in); break;
            case GENERATING: generating(s, in); break;
            case SETTINGS: settings(s, in); break;
            case HELP: help(s, in); break;
            default: break;
        }
    }

    private void go(Main.State st) {
        m.state = st;
        scroll = 0;
        confirmDelete = null;
    }

    // ---- title screen ----------------------------------------------------------

    /** Bold letters for the logo (other letters are made bold from the font). */
    private static final java.util.Map<Character, String[]> LOGO = new java.util.HashMap<>();

    static {
        LOGO.put('M', new String[]{"XX...XX", "XXX.XXX", "XXXXXXX", "XX.X.XX", "XX...XX", "XX...XX", "XX...XX"});
        LOGO.put('I', new String[]{"XXXX", ".XX.", ".XX.", ".XX.", ".XX.", ".XX.", "XXXX"});
        LOGO.put('N', new String[]{"XX..XX", "XXX.XX", "XXXXXX", "XX.XXX", "XX..XX", "XX..XX", "XX..XX"});
        LOGO.put('G', new String[]{".XXXX.", "XX..XX", "XX....", "XX.XXX", "XX..XX", "XX..XX", ".XXXXX"});
        LOGO.put('E', new String[]{"XXXXXX", "XX....", "XX....", "XXXXX.", "XX....", "XX....", "XXXXXX"});
        LOGO.put('R', new String[]{"XXXXX.", "XX..XX", "XX..XX", "XXXXX.", "XX.XX.", "XX..XX", "XX..XX"});
        LOGO.put('A', new String[]{".XXXX.", "XX..XX", "XX..XX", "XXXXXX", "XX..XX", "XX..XX", "XX..XX"});
        LOGO.put('C', new String[]{".XXXX.", "XX..XX", "XX....", "XX....", "XX....", "XX..XX", ".XXXX."});
    }

    private static boolean[][] logoGlyph(char ch) {
        String[] rows = LOGO.get(ch);
        if (rows == null) {
            boolean[][] g = Font.glyphRows(ch);
            return g == null ? null : bold(g);
        }
        boolean[][] g = new boolean[rows.length][rows[0].length()];
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++) g[y][x] = rows[y].charAt(x) == 'X';
        return g;
    }

    /** A glyph made bold: every pixel also fills the pixels right of and below it. */
    private static boolean[][] bold(boolean[][] g) {
        int h = g.length + 1, w = g[0].length + 1;
        boolean[][] o = new boolean[h][w];
        for (int y = 0; y < g.length; y++)
            for (int x = 0; x < g[y].length; x++)
                if (g[y][x]) {
                    o[y][x] = o[y][x + 1] = o[y + 1][x] = o[y + 1][x + 1] = true;
                }
        return o;
    }

    /** The game's name built from blocks of grass, dirt, stone and ore. */
    private void logo(Screen s, String text, int y, int n) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            boolean[][] g = logoGlyph(text.charAt(i));
            if (g != null) w += (g[0].length + 1) * n;
        }
        w -= n;
        int x = (s.w - w) / 2;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            boolean[][] g = logoGlyph(ch);
            if (g == null) continue;
            int bob = (int) Math.round(Math.sin(m.frameTicks * 0.03 + i * 0.55) * 1.6);
            int ly = y + bob;
            // shadow and outline
            for (int gy = 0; gy < g.length; gy++)
                for (int gx = 0; gx < g[gy].length; gx++)
                    if (g[gy][gx]) s.fillA(x + gx * n + 3, ly + gy * n + 4, n, n, Pal.BLACK, 90);
            for (int gy = 0; gy < g.length; gy++)
                for (int gx = 0; gx < g[gy].length; gx++)
                    if (g[gy][gx]) s.fill(x + gx * n - 1, ly + gy * n - 1, n + 2, n + 2, 0x1a120c);
            for (int gy = 0; gy < g.length; gy++)
                for (int gx = 0; gx < g[gy].length; gx++) {
                    if (!g[gy][gx]) continue;
                    boolean up = gy == 0 || !g[gy - 1][gx];
                    boolean dn = gy == g.length - 1 || !g[gy + 1][gx];
                    boolean lf = gx == 0 || !g[gy][gx - 1];
                    boolean rt = gx == g[gy].length - 1 || !g[gy][gx + 1];
                    int ore = Noise.hash(i * 31 + gx, gy);
                    for (int py = 0; py < n; py++)
                        for (int px = 0; px < n; px++) {
                            int wx = gx * n + px + i * 40, wy = gy * n + py;
                            int c;
                            if (gy >= 4) c = Tiles.tex[Tiles.STONE][ore & 3].p[(px % 8) + (py % 8) * 8];
                            else c = Tiles.texel(Tiles.DIRT, 0, 0, 0, wx, wy);
                            if (gy >= 4 && (ore & 15) == 3) c = Tiles.tex[Tiles.GOLD][ore >> 4 & 3].p[(px % 8) + (py % 8) * 8];
                            if (gy >= 4 && (ore & 15) == 7) c = Tiles.tex[Tiles.COPPER][ore >> 4 & 3].p[(px % 8) + (py % 8) * 8];
                            if (up && py < Math.max(2, n / 2)) c = py == 0 ? Pal.GRASS_HL : py == 1 ? Pal.GRASS_L : Pal.GRASS;
                            if ((lf && px == 0) || (dn && py == n - 1)) c = Pal.shade(c, 170);
                            else if ((rt && px == n - 1)) c = Pal.shade(c, 150);
                            else if (up && py == 0) c = Pal.lerp(c, 0xffffff, 0.25);
                            s.pset(x + gx * n + px, ly + gy * n + py, c);
                        }
                }
            x += (g[0].length + 1) * n;
        }
    }

    private void title(Screen s, Input in) {
        drawTitleBackground(s);
        Ui.dim(s, 25);
        int n = s.w >= 760 ? 5 : 4;
        int y = (int) (s.h * 0.1);
        logo(s, "MINGERIACC", y, n);
        String sub = "A clone of a clone";
        s.textOutline(sub, (s.w - Font.width(sub)) / 2, y + 7 * n + 12, Pal.UI_TEXT);

        int bw = 150, bh = 18, bx = (s.w - bw) / 2;
        int by = (int) (s.h * 0.44);
        if (Ui.button(s, in, "Play", bx, by, bw, bh)) {
            worlds = SaveIO.listWorlds();
            go(Main.State.WORLDS);
        }
        if (Ui.button(s, in, "Help", bx, by + 24, bw, bh)) {
            m.returnState = Main.State.TITLE;
            go(Main.State.HELP);
        }
        if (Ui.button(s, in, "Settings", bx, by + 48, bw, bh)) {
            m.returnState = Main.State.TITLE;
            go(Main.State.SETTINGS);
        }
        if (Ui.button(s, in, "Quit", bx, by + 72, bw, bh)) {
            m.quitRequested = true;
        }
        String v = "Version " + Main.VERSION;
        s.textShadow(v, s.w - Font.width(v) - 6, s.h - 11, Pal.UI_DIM);
        s.textShadow("All graphics and sound made in code", 6, s.h - 11, Pal.UI_DIM);
    }

    // ---- worlds -----------------------------------------------------------------

    private void worldList(Screen s, Input in) {
        drawTitleBackground(s);
        Ui.dim(s, 90);
        int pw = Math.min(360, s.w - 20), ph = Math.min(260, s.h - 20);
        int px = (s.w - pw) / 2, py = (s.h - ph) / 2;
        s.panel(px, py, pw, ph);
        s.textCenter("Select a World", s.w / 2, py + 8, Pal.UI_SEL);

        int rowH = 22, listY = py + 24, visible = (ph - 24 - 34) / rowH;
        if (worlds == null) worlds = SaveIO.listWorlds();
        if (confirmDelete != null) {
            s.textCenter("Delete the world " + confirmDelete.name + "?", s.w / 2, py + ph / 2 - 30, Pal.UI_TEXT);
            s.textCenter("This cannot be undone.", s.w / 2, py + ph / 2 - 18, Pal.UI_BAD);
            if (Ui.button(s, in, "Delete", s.w / 2 - 84, py + ph / 2, 80, 18)) {
                if (!confirmDelete.file.delete()) worldError = "Deleting failed";
                worlds = SaveIO.listWorlds();
                confirmDelete = null;
            }
            if (Ui.button(s, in, "Cancel", s.w / 2 + 4, py + ph / 2, 80, 18) || in.pressed(KeyEvent.VK_ESCAPE)) {
                confirmDelete = null;
            }
            return;
        }
        if (worlds.isEmpty()) {
            s.textCenter("No worlds yet.", s.w / 2, listY + 30, Pal.UI_DIM);
            s.textCenter("Create your first one below!", s.w / 2, listY + 42, Pal.UI_DIM);
        }
        int maxScroll = Math.max(0, worlds.size() - visible);
        if (in.wheel != 0) scroll = Math.max(0, Math.min(maxScroll, scroll + in.wheel));
        for (int i = 0; i < visible && i + scroll < worlds.size(); i++) {
            SaveIO.WorldInfo wi = worlds.get(i + scroll);
            int ry = listY + i * rowH;
            int rw = pw - 16 - 24;
            boolean hv = Ui.hover(in, px + 8, ry, rw, rowH - 3);
            s.fillRound(px + 8, ry, rw, rowH - 3, hv ? Pal.UI_BG2 : Pal.BLACK, hv ? 230 : 120);
            if (hv) s.rectRound(px + 8, ry, rw, rowH - 3, Pal.UI_SEL);
            s.textShadow(wi.name, px + 14, ry + 2, hv ? Pal.UI_SEL : Pal.UI_TEXT);
            String info = WorldGen.SIZE_NAMES[Math.max(0, Math.min(2, wi.sizeIndex))] + ", day " + wi.day;
            s.text(info, px + 14, ry + 11, Pal.UI_DIM);
            if (hv && in.clickL) {
                in.consumeClicks();
                m.audio.play(Audio.MENU, 0.45, 1.0);
                load(wi);
                return;
            }
            if (Ui.button(s, in, "X", px + pw - 30, ry, 20, rowH - 3)) confirmDelete = wi;
        }
        if (maxScroll > 0) {
            s.textShadow((scroll + 1) + "-" + Math.min(worlds.size(), scroll + visible) + " / " + worlds.size(),
                    px + pw - 60, py + 8, Pal.UI_DIM);
        }
        if (worldError != null) s.textCenter(worldError, s.w / 2, py + ph - 42, Pal.UI_BAD);
        int bw = (pw - 24) / 2;
        if (Ui.button(s, in, "New World", px + 8, py + ph - 26, bw, 18)) {
            nameField.setLength(0);
            nameField.append(NAMES[new Random().nextInt(NAMES.length)]);
            seedField.setLength(0);
            focus = 0;
            worldError = null;
            go(Main.State.CREATE);
        }
        if (Ui.button(s, in, "Back", px + 16 + bw, py + ph - 26, bw, 18) || in.pressed(KeyEvent.VK_ESCAPE)) {
            worldError = null;
            go(Main.State.TITLE);
        }
    }

    private void load(SaveIO.WorldInfo wi) {
        try {
            Game g = SaveIO.load(wi.file, m.audio);
            g.viewW = m.screen.w;
            g.viewH = m.screen.h;
            g.snapCamera();
            g.message("Welcome back to " + g.world.name + "!", Pal.UI_SEL);
            m.startGame(g, wi.file);
            worldError = null;
        } catch (IOException | RuntimeException e) {
            worldError = "Loading failed: " + e.getMessage();
            System.err.println("Loading failed: " + e);
            e.printStackTrace();
        }
    }

    // ---- new world --------------------------------------------------------------

    private void create(Screen s, Input in) {
        drawTitleBackground(s);
        Ui.dim(s, 90);
        int pw = Math.min(320, s.w - 20), ph = 200;
        int px = (s.w - pw) / 2, py = (s.h - ph) / 2;
        s.panel(px, py, pw, ph);
        s.textCenter("New World", s.w / 2, py + 8, Pal.UI_SEL);

        int fx = px + 12, fw = pw - 24;
        s.textShadow("Name", fx, py + 26, Pal.UI_TEXT);
        Ui.textField(s, nameField.toString(), fx, py + 36, fw, focus == 0, m.frameTicks);
        if (in.clickL && Ui.hover(in, fx, py + 36, fw, 16)) focus = 0;

        s.textShadow("Size", fx, py + 60, Pal.UI_TEXT);
        int bw = (fw - 8) / 3;
        for (int i = 0; i < 3; i++) {
            String label = (sizeChoice == i ? "> " : "") + WorldGen.SIZE_NAMES[i] + (sizeChoice == i ? " <" : "");
            if (Ui.button(s, in, label, fx + i * (bw + 4), py + 70, bw, 18)) sizeChoice = i;
        }
        int[] sz = WorldGen.SIZES[sizeChoice];
        s.text(sz[0] + " × " + sz[1] + " tiles", fx, py + 92, Pal.UI_DIM);

        s.textShadow("Seed (empty = random)", fx, py + 108, Pal.UI_TEXT);
        Ui.textField(s, seedField.toString(), fx, py + 118, fw, focus == 1, m.frameTicks);
        if (in.clickL && Ui.hover(in, fx, py + 118, fw, 16)) focus = 1;

        if (in.pressed(KeyEvent.VK_TAB)) focus = 1 - focus;
        boolean enter = focus == 0 ? Ui.edit(nameField, in, 20, false) : Ui.edit(seedField, in, 18, true);

        int bw2 = (fw - 8) / 2;
        boolean ok = nameField.toString().trim().length() > 0;
        if (Ui.button(s, in, "Create World", fx, py + ph - 28, bw2, 18, ok) || (enter && ok)) startGeneration();
        if (Ui.button(s, in, "Back", fx + bw2 + 8, py + ph - 28, bw2, 18) || in.pressed(KeyEvent.VK_ESCAPE)) {
            go(Main.State.WORLDS);
        }
    }

    private void startGeneration() {
        genName = nameField.toString().trim();
        genSize = sizeChoice;
        String seedText = seedField.toString().trim();
        long seed;
        if (seedText.isEmpty()) {
            seed = new Random().nextInt(1_000_000_000);
        } else {
            try {
                seed = Long.parseLong(seedText);
            } catch (NumberFormatException e) {
                seed = seedText.hashCode();
            }
        }
        genSeed = seed;
        progress = new WorldGen.Progress();
        final WorldGen.Progress p = progress;
        final String name = genName;
        final int size = genSize;
        final long sd = seed;
        Thread t = new Thread(() -> {
            try {
                p.result = WorldGen.generate(name, size, sd, p);
            } catch (Throwable e) {
                p.error = e;
            }
        }, "Mingeriacc-worldgen");
        t.setDaemon(true);
        t.start();
        go(Main.State.GENERATING);
    }

    private void generating(Screen s, Input in) {
        drawTitleBackground(s);
        Ui.dim(s, 140);
        WorldGen.Progress p = progress;
        s.textCenter("Creating the world " + genName, s.w / 2, s.h / 2 - 30, Pal.UI_SEL);
        String st = p.stage + "...";
        s.textCenter(st, s.w / 2, s.h / 2 - 14, Pal.UI_TEXT);
        int bw = Math.min(260, s.w - 40), bx = (s.w - bw) / 2, by = s.h / 2 + 2;
        s.rectRound(bx, by, bw, 10, Pal.UI_EDGE);
        int fill = (int) ((bw - 4) * Math.max(0, Math.min(1, p.value)));
        s.fill(bx + 2, by + 2, fill, 6, Pal.GRASS);
        s.fill(bx + 2, by + 2, fill, 2, Pal.GRASS_L);
        s.textCenter("Seed " + genSeed, s.w / 2, by + 18, Pal.UI_DIM);
        if (p.error != null) {
            worldError = "Creating the world failed: " + p.error;
            p.error.printStackTrace();
            go(Main.State.WORLDS);
            return;
        }
        World w = p.result;
        if (w != null) {
            Game g = new Game(w, m.audio);
            g.viewW = m.screen.w;
            g.viewH = m.screen.h;
            g.startNew();
            File f = SaveIO.newWorldFile(w.name);
            m.startGame(g, f);
            m.saveGame(false);
            worlds = null;
        }
    }

    // ---- settings ----------------------------------------------------------------

    private static final String[] ZOOM_NAMES = {"Near", "Normal", "Far"};

    private void settings(Screen s, Input in) {
        background(s);
        Ui.dim(s, 120);
        int pw = Math.min(300, s.w - 20), ph = 218;
        int px = (s.w - pw) / 2, py = (s.h - ph) / 2;
        s.panel(px, py, pw, ph);
        s.textCenter("Settings", s.w / 2, py + 8, Pal.UI_SEL);
        int y = py + 28;
        int mv = (int) Math.round(m.audio.musicVolume * 100);
        int sv = (int) Math.round(m.audio.sfxVolume * 100);
        int nmv = volumeRow(s, in, "Music: " + (m.audio.musicMuted ? "off (M)" : mv + " %"), mv, px, y, pw);
        if (nmv != mv) m.audio.musicMuted = false;
        m.audio.musicVolume = nmv / 100.0;
        y += 24;
        int nsv = volumeRow(s, in, "Sound effects: " + sv + " %", sv, px, y, pw);
        if (nsv != sv) m.audio.play(Audio.PICKUP, 0.5, 1.0);
        m.audio.sfxVolume = nsv / 100.0;
        y += 24;
        if (Ui.button(s, in, "Zoom: " + ZOOM_NAMES[m.zoom], px + 12, y, pw - 24, 18)) {
            m.zoom = (m.zoom + 1) % ZOOM_NAMES.length;
        }
        y += 24;
        if (Ui.button(s, in, "Lighting: " + (m.renderer.smoothLight ? "Smooth" : "Retro"), px + 12, y, pw - 24, 18)) {
            m.renderer.smoothLight = !m.renderer.smoothLight;
        }
        y += 24;
        if (Ui.button(s, in, "Fullscreen: " + (m.fullscreen ? "Yes" : "No") + "  (F11)", px + 12, y, pw - 24, 18)) {
            m.requestFullscreenToggle();
        }
        y += 24;
        if (Ui.button(s, in, "FPS counter: " + (m.showFps ? "Yes" : "No") + "  (F3)", px + 12, y, pw - 24, 18)) {
            m.showFps = !m.showFps;
        }
        if (Ui.button(s, in, "Back", px + 12, py + ph - 28, pw - 24, 18) || in.pressed(KeyEvent.VK_ESCAPE)) {
            m.saveSettings();
            go(m.returnState);
        }
    }

    private int volumeRow(Screen s, Input in, String label, int value, int px, int y, int pw) {
        s.textShadow(label, px + 14, y + 5, Pal.UI_TEXT);
        int bx = px + pw - 12 - 44;
        if (Ui.button(s, in, "-", bx, y, 20, 18)) value = Math.max(0, value - 10);
        if (Ui.button(s, in, "+", bx + 24, y, 20, 18)) value = Math.min(100, value + 10);
        return value;
    }

    // ---- help ----------------------------------------------------------------------

    private static final String[][] CONTROLS = {
        {"A / D or arrows", "Move"},
        {"Space / W", "Jump (hold for a higher jump)"},
        {"S", "Drop through a wood platform"},
        {"Left mouse", "Use: mine, chop, build, attack"},
        {"Right mouse", "Doors, chests, beds, talk; equip gear"},
        {"Wheel / 1-0", "Select a hotbar slot"},
        {"E or I", "Inventory and crafting"},
        {"Shift + click", "Trash, move to a chest, or sell"},
        {"H", "Quick heal (potion or mushroom)"},
        {"Q", "Throw the selected item"},
        {"M", "Music on / off"},
        {"F11 / F3", "Fullscreen / FPS and position"},
        {"ESC", "Menu (save and quit)"},
    };

    private static final String[] TIPS = {
        "Craft a work bench from wood, then a furnace to smelt ore into bars.",
        "An anvil (5 iron bars) makes better tools, swords and bows from bars.",
        "Slimes drop gel for torches. Zombies and demon eyes come out at night.",
        "Life crystals deep underground raise your maximum life.",
        "Build a house (walls, door, light, table, chair) and town folk move in.",
        "The world saves itself every 5 minutes and when you quit.",
    };

    private void help(Screen s, Input in) {
        background(s);
        Ui.dim(s, 130);
        int pw = Math.min(470, s.w - 16), ph = Math.min(s.h - 16, 272);
        int px = (s.w - pw) / 2, py = (s.h - ph) / 2;
        s.panel(px, py, pw, ph);
        s.textCenter("Help", s.w / 2, py + 8, Pal.UI_SEL);
        int y = py + 24;
        for (String[] c : CONTROLS) {
            s.textShadow(c[0], px + 12, y, Pal.UI_SEL);
            s.textShadow(c[1], px + 120, y, Pal.UI_TEXT);
            y += 11;
        }
        y += 4;
        for (String t : TIPS) {
            s.text(t, px + 12, y, Pal.UI_DIM);
            y += 10;
        }
        if (Ui.button(s, in, "Back", px + pw / 2 - 60, py + ph - 24, 120, 18) || in.pressed(KeyEvent.VK_ESCAPE)) {
            go(m.returnState);
        }
    }

    // ---- pause -------------------------------------------------------------------

    void renderPause(Screen s, Input in) {
        Ui.dim(s, 120);
        int pw = 170, ph = 150;
        int px = (s.w - pw) / 2, py = (s.h - ph) / 2;
        s.panel(px, py, pw, ph);
        s.textCenter("Paused", s.w / 2, py + 8, Pal.UI_SEL);
        int bx = px + 12, bw = pw - 24, y = py + 24;
        if (Ui.button(s, in, "Resume", bx, y, bw, 18)) m.paused = false;
        y += 23;
        if (Ui.button(s, in, "Help", bx, y, bw, 18)) {
            m.returnState = Main.State.PLAYING;
            go(Main.State.HELP);
        }
        y += 23;
        if (Ui.button(s, in, "Settings", bx, y, bw, 18)) {
            m.returnState = Main.State.PLAYING;
            go(Main.State.SETTINGS);
        }
        y += 23;
        if (Ui.button(s, in, "Save", bx, y, bw, 18)) m.saveGame(true);
        y += 23;
        if (Ui.button(s, in, "Save and Quit", bx, y, bw, 18)) {
            m.exitToTitle();
            worlds = null;
        }
    }
}
