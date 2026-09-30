package mingeriacc;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

/**
 * Mingeriacc - a clone of a clone. Window, game loop and state switching.
 * The game logic always runs at 60 steps per second.
 */
public final class Main {
    static final String TITLE = "Mingeriacc";
    static final String VERSION = "0.3.0";
    static final int TPS = 60;

    enum State { TITLE, WORLDS, CREATE, GENERATING, PLAYING, SETTINGS, HELP }

    JFrame frame;
    Canvas canvas;
    final Screen screen = new Screen(640, 360);
    final Input input = new Input();
    final Audio audio = new Audio();
    final Renderer renderer = new Renderer();
    final Hud hud = new Hud();
    final Menus menus = new Menus(this);
    Properties settings;

    State state = State.TITLE;
    State returnState = State.TITLE; // where Settings and Help return to
    Game game;
    File gameFile;
    boolean paused;
    boolean fullscreen, showFps;
    int zoom = 1; // 0 = near, 1 = normal, 2 = far
    long frameTicks;
    volatile boolean quitRequested;
    int scale = 2, offX, offY;
    private int fps, fpsCount;
    private long fpsTime;
    private volatile boolean toggleFullscreenPending;

    public static void main(String[] args) {
        // Windows display scaling (e.g. 150 %) must not blur the pixels
        System.setProperty("sun.java2d.uiScale", "1");
        System.setProperty("sun.java2d.noddraw", "true");
        Main m = new Main();
        try {
            m.start();
        } catch (Throwable t) {
            m.crash(t);
        }
    }

    void start() throws Exception {
        settings = SaveIO.loadSettings();
        audio.musicVolume = num("music", 60) / 100.0;
        audio.sfxVolume = num("sfx", 80) / 100.0;
        audio.musicMuted = "1".equals(settings.getProperty("music_muted", "0"));
        fullscreen = "1".equals(settings.getProperty("fullscreen", "0"));
        showFps = "1".equals(settings.getProperty("fps", "0"));
        zoom = Math.max(0, Math.min(2, num("zoom", 1)));
        renderer.smoothLight = !"retro".equals(settings.getProperty("lighting", "smooth"));
        Ui.audio = audio;

        SwingUtilities.invokeAndWait(this::createWindow);
        audio.start();
        audio.setMusic(Music.TITLE);
        loop();
    }

    int num(String key, int def) {
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(settings.getProperty(key, "" + def).trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    void saveSettings() {
        settings.setProperty("music", "" + (int) Math.round(audio.musicVolume * 100));
        settings.setProperty("sfx", "" + (int) Math.round(audio.sfxVolume * 100));
        settings.setProperty("music_muted", audio.musicMuted ? "1" : "0");
        settings.setProperty("fullscreen", fullscreen ? "1" : "0");
        settings.setProperty("fps", showFps ? "1" : "0");
        settings.setProperty("zoom", "" + zoom);
        settings.setProperty("lighting", renderer.smoothLight ? "smooth" : "retro");
        SaveIO.saveSettings(settings);
    }

    // ---- window ------------------------------------------------------------

    private void createWindow() {
        frame = new JFrame(TITLE + " " + VERSION);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setIconImage(makeIcon());
        canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        canvas.setPreferredSize(new Dimension(1280, 720));
        canvas.setFocusTraversalKeysEnabled(false); // Tab etc. go to the game
        canvas.setIgnoreRepaint(true);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);

        BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor none = Toolkit.getDefaultToolkit().createCustomCursor(blank, new Point(0, 0), "blank");
        canvas.setCursor(none);

        canvas.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11 || (e.getKeyCode() == KeyEvent.VK_ENTER && e.isAltDown())) {
                    toggleFullscreenPending = true;
                    return;
                }
                input.keyDown(e.getKeyCode());
            }
            @Override public void keyReleased(KeyEvent e) { input.keyUp(e.getKeyCode()); }
            @Override public void keyTyped(KeyEvent e) { input.keyTyped(e.getKeyChar()); }
        });
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                canvas.requestFocus();
                input.mouseDown(e.getButton());
            }
            @Override public void mouseReleased(MouseEvent e) { input.mouseUp(e.getButton()); }
            @Override public void mouseMoved(MouseEvent e) { input.rawMouseX = e.getX(); input.rawMouseY = e.getY(); }
            @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) { input.wheel(e.getWheelRotation()); }
            @Override public void mouseEntered(MouseEvent e) { input.mouseInside = true; }
            @Override public void mouseExited(MouseEvent e) { input.mouseInside = false; }
        };
        canvas.addMouseListener(ma);
        canvas.addMouseMotionListener(ma);
        canvas.addMouseWheelListener(ma);
        canvas.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { input.releaseAll(); }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { quitRequested = true; }
        });

        applyWindowMode();
        canvas.requestFocus();
    }

    private void applyWindowMode() {
        boolean visible = frame.isDisplayable();
        if (visible) frame.dispose();
        frame.setUndecorated(fullscreen);
        if (fullscreen) {
            Rectangle b = frame.getGraphicsConfiguration() != null
                    ? frame.getGraphicsConfiguration().getBounds()
                    : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().getBounds();
            frame.setExtendedState(JFrame.NORMAL);
            frame.setBounds(b);
        } else {
            frame.setExtendedState(JFrame.NORMAL);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        frame.setVisible(true);
        canvas.requestFocus();
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        saveSettings();
        try {
            SwingUtilities.invokeAndWait(() -> {
                applyWindowMode();
                recreateBuffers();
            });
        } catch (Exception e) {
            System.err.println("Switching the display mode failed: " + e);
        }
        input.releaseAll();
    }

    private void recreateBuffers() {
        try {
            if (canvas.isDisplayable()) canvas.createBufferStrategy(2);
        } catch (Exception e) {
            System.err.println("Creating the draw buffer failed: " + e);
        }
    }

    void requestFullscreenToggle() {
        toggleFullscreenPending = true;
    }

    private static BufferedImage makeIcon() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Sprite pick = Items.ICON[Items.COPPER_PICK];
        Sprite dirt = Tiles.macro[Tiles.DIRT];
        for (int y = 0; y < 32; y++)
            for (int x = 0; x < 32; x++) {
                int c = 0;
                if (y >= 20) {
                    c = 0xff000000 | (y < 23 ? Pal.GRASS : dirt.p[(x / 2 % 32) + ((y - 20) / 2 % 32) * 32] & 0xffffff);
                }
                int px = pick.get(x / 2 - 1, y / 2 - 1);
                if (px != 0 && y < 27) c = px | 0xff000000;
                img.setRGB(x, y, c);
            }
        return img;
    }

    // ---- loop -------------------------------------------------------------

    private void loop() {
        final long tick = 1_000_000_000L / TPS;
        long last = System.nanoTime();
        long acc = 0;
        fpsTime = System.currentTimeMillis();
        while (true) {
            if (quitRequested) {
                quit();
                return;
            }
            if (toggleFullscreenPending) {
                toggleFullscreenPending = false;
                toggleFullscreen();
                last = System.nanoTime();
            }
            long now = System.nanoTime();
            acc += now - last;
            last = now;
            if (acc > tick * 8) acc = tick * 8; // don't try to catch up too much
            if (acc < tick) {
                long sleepNs = tick - acc;
                if (sleepNs > 1_500_000) {
                    try { Thread.sleep(1); } catch (InterruptedException ignored) { }
                } else {
                    Thread.yield();
                }
                continue;
            }
            layout();
            input.beginFrame(offX, offY, scale, screen.w, screen.h);
            int steps = 0;
            while (acc >= tick && steps < 4) {
                frameTicks++;
                update(steps == 0);
                acc -= tick;
                steps++;
            }
            render();
            present();
            fpsCount++;
            long ms = System.currentTimeMillis();
            if (ms - fpsTime >= 1000) {
                fps = fpsCount;
                fpsCount = 0;
                fpsTime = ms;
            }
        }
    }

    private static final int[] ZOOM_HEIGHT = {360, 450, 560};

    private void layout() {
        int cw = Math.max(1, canvas.getWidth()), ch = Math.max(1, canvas.getHeight());
        int s = (int) Math.round(ch / (double) ZOOM_HEIGHT[zoom]);
        s = Math.max(1, s);
        while (s > 1 && ch / s < 300) s--;
        while (cw / s < 480 && s > 1) s--;
        scale = s;
        screen.resize(cw / s, ch / s);
        offX = (cw - screen.w * s) / 2;
        offY = (ch - screen.h * s) / 2;
    }

    /** One game logic step. first = first step of the frame (clicks are only handled then). */
    private void update(boolean first) {
        if (!first && state == State.PLAYING) input.consumeFrameEvents();
        if (input.pressed(KeyEvent.VK_F3) && first) {
            showFps = !showFps;
            saveSettings();
        }
        if (input.pressed(KeyEvent.VK_M) && first && state != State.CREATE) {
            audio.musicMuted = !audio.musicMuted;
            saveSettings();
            if (game != null) game.message(audio.musicMuted ? "Music off" : "Music on", Pal.UI_DIM);
        }
        if (state != State.PLAYING || game == null) return;
        if (first && input.pressed(KeyEvent.VK_ESCAPE)) {
            if (game.talkTo != null && !game.inventoryOpen) game.closeTalk();
            else if (game.inventoryOpen) game.toggleInventory();
            else paused = !paused;
        }
        if (paused) return;
        game.viewW = screen.w;
        game.viewH = screen.h;
        boolean uiBlocks = hud.update(game, input, screen.w, screen.h);
        game.update(input, uiBlocks);
        if (game.saveRequested) {
            game.saveRequested = false;
            saveGame(true);
        }
        // music to suit the situation
        audio.setMusic(musicFor(game));
    }

    static int musicFor(Game g) {
        switch (g.biome) {
            case Background.UNDERWORLD: return Music.UNDERWORLD;
            case Background.JUNGLE: return Music.JUNGLE;
            case Background.CORRUPTION: return Music.CORRUPTION;
            default: return g.isUnderground() ? Music.CAVE : g.isNight() ? Music.NIGHT : Music.DAY;
        }
    }

    private void render() {
        switch (state) {
            case PLAYING:
                renderGame();
                break;
            default:
                menus.render(screen, input);
        }
        if (showFps) {
            String t = fps + " FPS";
            if (game != null && state == State.PLAYING) {
                t += "  X " + (int) (game.player.centerX() / Tiles.T) + "  Y " + (int) (game.player.centerY() / Tiles.T)
                        + "  Light " + game.light.get((int) (game.player.centerX() / Tiles.T),
                        (int) (game.player.centerY() / Tiles.T)) + "  Items " + game.drops.size()
                        + "  Enemies " + game.mobs.size();
            }
            screen.textShadow(t, 6, screen.h - 10, Pal.UI_GOOD);
        }
        if (input.mouseInside) {
            int ci = state == State.PLAYING && game != null ? game.cursorItem : 0;
            int cc = state == State.PLAYING && game != null ? game.cursorCount : 0;
            Hud.drawCursor(screen, input.mouseX, input.mouseY, ci, cc);
        }
    }

    private void renderGame() {
        Game g = game;
        g.viewW = screen.w;
        g.viewH = screen.h;
        int held = g.inv.selectedItem();
        int kind = Items.KIND[held];
        boolean showAim = !paused && !g.inventoryOpen && held != 0
                && (kind == Items.K_TOOL || kind == Items.K_TILE || kind == Items.K_WALL);
        if (kind == Items.K_TOOL && Items.TOOL[held] == Tiles.TOOL_HAMMER) showAim &= g.world.wall(g.aimX, g.aimY) != 0;
        renderer.render(screen, g, frameTicks, showAim ? held : 0);
        hud.draw(screen, g, input);
        if (paused) menus.renderPause(screen, input);
    }

    private void present() {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) {
            if (canvas.isDisplayable()) {
                try {
                    canvas.createBufferStrategy(2);
                } catch (Exception ignored) { }
            }
            return;
        }
        try {
            do {
                do {
                    Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    int cw = canvas.getWidth(), ch = canvas.getHeight();
                    g.setColor(Color.BLACK);
                    if (offX > 0) {
                        g.fillRect(0, 0, offX, ch);
                        g.fillRect(cw - offX - 1, 0, offX + 1, ch);
                    }
                    if (offY > 0) {
                        g.fillRect(0, 0, cw, offY);
                        g.fillRect(0, ch - offY - 1, cw, offY + 1);
                    }
                    g.drawImage(screen.img, offX, offY, screen.w * scale, screen.h * scale, null);
                    g.dispose();
                } while (bs.contentsRestored());
                bs.show();
            } while (bs.contentsLost());
            Toolkit.getDefaultToolkit().sync();
        } catch (IllegalStateException e) {
            // the buffer was lost (e.g. display mode change): create a new one
            SwingUtilities.invokeLater(this::recreateBuffers);
        }
    }

    // ---- starting and saving a game -------------------------------------------

    void startGame(Game g, File file) {
        game = g;
        gameFile = file;
        paused = false;
        state = State.PLAYING;
        input.consumeClicks();
    }

    boolean saveGame(boolean announce) {
        if (game == null || gameFile == null) return false;
        try {
            SaveIO.save(game, gameFile);
            if (announce) game.message("World saved", Pal.UI_GOOD);
            return true;
        } catch (IOException e) {
            game.message("Saving failed: " + e.getMessage(), Pal.UI_BAD);
            System.err.println("Saving failed: " + e);
            return false;
        }
    }

    void exitToTitle() {
        saveGame(false);
        game = null;
        gameFile = null;
        paused = false;
        state = State.TITLE;
        audio.setMusic(Music.TITLE);
    }

    void quit() {
        if (state == State.PLAYING && game != null) saveGame(false);
        saveSettings();
        audio.stop();
        try {
            SwingUtilities.invokeAndWait(() -> frame.dispose());
        } catch (Exception ignored) { }
        System.exit(0);
    }

    private void crash(Throwable t) {
        t.printStackTrace();
        File log = new File(SaveIO.baseDir(), "error.log");
        try (PrintWriter pw = new PrintWriter(new FileWriter(log, true))) {
            pw.println("---- " + new java.util.Date() + " (Mingeriacc " + VERSION + ", Java "
                    + System.getProperty("java.version") + ")");
            t.printStackTrace(pw);
        } catch (IOException ignored) { }
        try {
            if (game != null) saveGame(false);
        } catch (Throwable ignored) { }
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(frame, "Mingeriacc crashed: " + t + "\n\nThe details were saved to:\n"
                    + log.getAbsolutePath(), "Mingeriacc - error", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }
}
