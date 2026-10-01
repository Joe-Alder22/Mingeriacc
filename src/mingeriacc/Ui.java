package mingeriacc;

/** Simple immediate-mode UI widgets for the menus. */
final class Ui {
    private Ui() {}

    static Audio audio;

    static boolean hover(Input in, int x, int y, int w, int h) {
        return in.mouseX >= x && in.mouseY >= y && in.mouseX < x + w && in.mouseY < y + h;
    }

    static boolean button(Screen s, Input in, String label, int x, int y, int w, int h) {
        return button(s, in, label, x, y, w, h, true);
    }

    static boolean button(Screen s, Input in, String label, int x, int y, int w, int h, boolean enabled) {
        boolean hv = enabled && hover(in, x, y, w, h);
        int bg = hv ? Pal.UI_BG2 : Pal.UI_BG;
        s.fillRound(x, y, w, h, bg, hv ? 240 : 205);
        int edge = !enabled ? Pal.GRAY_D : hv ? Pal.UI_SEL : Pal.UI_EDGE;
        s.rectRound(x, y, w, h, edge);
        // soft highlight along the top edge
        s.fillA(x + 2, y + 1, w - 4, 1, Pal.WHITE, hv ? 55 : 28);
        s.fillA(x + 2, y + h - 2, w - 4, 1, Pal.BLACK, 40);
        int tc = !enabled ? Pal.GRAY : hv ? Pal.UI_SEL : Pal.UI_TEXT;
        s.textShadow(label, x + (w - Font.width(label)) / 2, y + (h - 7) / 2, tc);
        if (hv && in.clickL) {
            in.consumeClicks();
            if (audio != null) audio.play(Audio.MENU, 0.45, 1.0);
            return true;
        }
        return false;
    }

    /** A horizontal slider (drag or click with the left button); returns the new value. */
    static double slider(Screen s, Input in, int x, int y, int w, double value, double min, double max, double step) {
        boolean hv = hover(in, x - 3, y - 3, w + 6, 11);
        if (hv && in.mouseL) {
            double t = Math.max(0, Math.min(1, (in.mouseX - x) / (double) (w - 1)));
            value = min + t * (max - min);
            value = Math.max(min, Math.min(max, Math.round(value / step) * step));
        }
        s.fillRound(x, y + 1, w, 4, Pal.BLACK, 190);
        int fill = (int) Math.round((value - min) / (max - min) * (w - 1));
        s.fill(x + 1, y + 2, Math.max(0, fill - 1), 2, Pal.UI_EDGE_L);
        s.fillRound(x + fill - 2, y - 1, 5, 8, hv ? Pal.UI_SEL : Pal.UI_TEXT, 255);
        s.rectRound(x + fill - 2, y - 1, 5, 8, Pal.UI_BG);
        return value;
    }

    /** "< label >": returns -1 or 1 when an arrow is clicked, else 0. */
    static int chooser(Screen s, Input in, String label, int x, int y, int w) {
        int r = 0;
        if (button(s, in, "<", x, y, 14, 14)) r = -1;
        if (button(s, in, ">", x + w - 14, y, 14, 14)) r = 1;
        s.textShadow(label, x + (w - Font.width(label)) / 2, y + 4, Pal.UI_TEXT);
        return r;
    }

    static void textField(Screen s, String text, int x, int y, int w, boolean focused, long ticks) {
        s.fillRound(x, y, w, 16, Pal.BLACK, 200);
        s.rectRound(x, y, w, 16, focused ? Pal.UI_SEL : Pal.UI_EDGE);
        s.textShadow(text, x + 5, y + 5, Pal.UI_TEXT);
        if (focused && (ticks / 30) % 2 == 0) s.fill(x + 6 + Font.width(text), y + 4, 1, 9, Pal.UI_SEL);
    }

    /** Edits text with the typed characters. Returns true if Enter was pressed. */
    static boolean edit(StringBuilder sb, Input in, int max, boolean digitsOnly) {
        boolean enter = false;
        for (char c : in.typed.toCharArray()) {
            if (c == '\b') {
                if (sb.length() > 0) sb.setLength(sb.length() - 1);
            } else if (c == '\n' || c == '\r') {
                enter = true;
            } else if (sb.length() < max) {
                boolean ok = digitsOnly ? (Character.isDigit(c) || (c == '-' && sb.length() == 0))
                        : (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '\'');
                if (ok && Font.has(c)) sb.append(c);
            }
        }
        return enter;
    }

    static void dim(Screen s, int a) {
        s.fillA(0, 0, s.w, s.h, Pal.BLACK, a);
    }
}
