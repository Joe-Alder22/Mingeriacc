package mingeriacc;

import java.awt.event.KeyEvent;

/**
 * Keyboard and mouse state. AWT writes here on the event thread, and the
 * game loop reads the state once per frame (beginFrame).
 */
final class Input {
    private final boolean[] down = new boolean[1024];
    private final boolean[] pressedRaw = new boolean[1024];
    private final boolean[] pressed = new boolean[1024];
    private final StringBuilder typedRaw = new StringBuilder();
    String typed = "";

    // mouse (in window pixels, converted to the game resolution)
    volatile int rawMouseX, rawMouseY;
    int mouseX, mouseY;
    private boolean lDownRaw, rDownRaw, lPressRaw, rPressRaw, lReleaseRaw;
    boolean mouseL, mouseR, clickL, clickR, releaseL;
    private int wheelRaw;
    int wheel;
    boolean mouseInside = true;

    synchronized void keyDown(int code) {
        if (code < 0 || code >= down.length) return;
        if (!down[code]) pressedRaw[code] = true;
        down[code] = true;
    }

    synchronized void keyUp(int code) {
        if (code < 0 || code >= down.length) return;
        down[code] = false;
    }

    synchronized void keyTyped(char c) {
        typedRaw.append(c);
    }

    synchronized void mouseDown(int button) {
        if (button == 1) { lDownRaw = true; lPressRaw = true; }
        if (button == 3) { rDownRaw = true; rPressRaw = true; }
    }

    synchronized void mouseUp(int button) {
        if (button == 1) { lDownRaw = false; lReleaseRaw = true; }
        if (button == 3) rDownRaw = false;
    }

    synchronized void wheel(int n) {
        wheelRaw += n;
    }

    synchronized void releaseAll() {
        java.util.Arrays.fill(down, false);
        lDownRaw = rDownRaw = false;
    }

    /** Copies the queued events into this frame's state. */
    synchronized void beginFrame(int offX, int offY, int scale, int sw, int sh) {
        for (int i = 0; i < down.length; i++) {
            pressed[i] = pressedRaw[i];
            pressedRaw[i] = false;
        }
        typed = typedRaw.toString();
        typedRaw.setLength(0);
        mouseL = lDownRaw;
        mouseR = rDownRaw;
        clickL = lPressRaw;
        clickR = rPressRaw;
        releaseL = lReleaseRaw;
        lPressRaw = rPressRaw = lReleaseRaw = false;
        wheel = wheelRaw;
        wheelRaw = 0;
        scale = Math.max(1, scale);
        mouseX = Math.max(0, Math.min(sw - 1, (rawMouseX - offX) / scale));
        mouseY = Math.max(0, Math.min(sh - 1, (rawMouseY - offY) / scale));
    }

    boolean down(int code) {
        return down[code];
    }

    boolean pressed(int code) {
        return pressed[code];
    }

    boolean left() { return down[KeyEvent.VK_A] || down[KeyEvent.VK_LEFT]; }
    boolean right() { return down[KeyEvent.VK_D] || down[KeyEvent.VK_RIGHT]; }
    boolean jump() { return down[KeyEvent.VK_SPACE] || down[KeyEvent.VK_W] || down[KeyEvent.VK_UP]; }
    boolean jumpPressed() {
        return pressed[KeyEvent.VK_SPACE] || pressed[KeyEvent.VK_W] || pressed[KeyEvent.VK_UP];
    }
    boolean downKey() { return down[KeyEvent.VK_S] || down[KeyEvent.VK_DOWN]; }
    boolean shift() { return down[KeyEvent.VK_SHIFT]; }

    /** One-shot events are handled only in the first step of a frame. */
    void consumeFrameEvents() {
        clickL = clickR = releaseL = false;
        java.util.Arrays.fill(pressed, false);
        typed = "";
        wheel = 0;
    }

    /** Drops clicks when the screen changes (so a click doesn't fall through). */
    void consumeClicks() {
        clickL = clickR = false;
    }
}
