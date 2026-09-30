package mingeriacc;

import java.util.HashMap;
import java.util.Map;

/**
 * The game's own pixel font: capitals 7 pixels tall, lower case letters with
 * descenders, a few accented letters, and small 3x5 digits for stack counts.
 */
final class Font {
    private Font() {}

    static final int LINE_H = 10;
    private static final Map<Character, boolean[][]> GLYPHS = new HashMap<>();
    private static final boolean[][][] SMALL = new boolean[10][][];

    private static void def(char c, String... rows) {
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        boolean[][] g = new boolean[rows.length][w];
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++)
                g[y][x] = rows[y].charAt(x) == 'X';
        GLYPHS.put(c, g);
    }

    static {
        def('A', ".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X");
        def('B', "XXXX.", "X...X", "X...X", "XXXX.", "X...X", "X...X", "XXXX.");
        def('C', ".XXX.", "X...X", "X....", "X....", "X....", "X...X", ".XXX.");
        def('D', "XXXX.", "X...X", "X...X", "X...X", "X...X", "X...X", "XXXX.");
        def('E', "XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "XXXXX");
        def('F', "XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "X....");
        def('G', ".XXX.", "X...X", "X....", "X.XXX", "X...X", "X...X", ".XXXX");
        def('H', "X...X", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X");
        def('I', "XXX", ".X.", ".X.", ".X.", ".X.", ".X.", "XXX");
        def('J', "..XXX", "...X.", "...X.", "...X.", "X..X.", "X..X.", ".XX..");
        def('K', "X...X", "X..X.", "X.X..", "XX...", "X.X..", "X..X.", "X...X");
        def('L', "X....", "X....", "X....", "X....", "X....", "X....", "XXXXX");
        def('M', "X...X", "XX.XX", "X.X.X", "X.X.X", "X...X", "X...X", "X...X");
        def('N', "X...X", "X...X", "XX..X", "X.X.X", "X..XX", "X...X", "X...X");
        def('O', ".XXX.", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX.");
        def('P', "XXXX.", "X...X", "X...X", "XXXX.", "X....", "X....", "X....");
        def('Q', ".XXX.", "X...X", "X...X", "X...X", "X.X.X", "X..X.", ".XX.X");
        def('R', "XXXX.", "X...X", "X...X", "XXXX.", "X.X..", "X..X.", "X...X");
        def('S', ".XXXX", "X....", "X....", ".XXX.", "....X", "....X", "XXXX.");
        def('T', "XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X..");
        def('U', "X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX.");
        def('V', "X...X", "X...X", "X...X", "X...X", "X...X", ".X.X.", "..X..");
        def('W', "X...X", "X...X", "X...X", "X.X.X", "X.X.X", "X.X.X", ".X.X.");
        def('X', "X...X", "X...X", ".X.X.", "..X..", ".X.X.", "X...X", "X...X");
        def('Y', "X...X", "X...X", ".X.X.", "..X..", "..X..", "..X..", "..X..");
        def('Z', "XXXXX", "....X", "...X.", "..X..", ".X...", "X....", "XXXXX");

        // lower case: x-height rows 2..6, descenders in rows 7..8
        def('a', "....", "....", ".XX.", "...X", ".XXX", "X..X", ".XXX");
        def('b', "X...", "X...", "XXX.", "X..X", "X..X", "X..X", "XXX.");
        def('c', "...", "...", ".XX", "X..", "X..", "X..", ".XX");
        def('d', "...X", "...X", ".XXX", "X..X", "X..X", "X..X", ".XXX");
        def('e', "....", "....", ".XX.", "X..X", "XXXX", "X...", ".XXX");
        def('f', "..X", ".X.", "XXX", ".X.", ".X.", ".X.", ".X.");
        def('g', "....", "....", ".XXX", "X..X", "X..X", "X..X", ".XXX", "...X", ".XX.");
        def('h', "X...", "X...", "XXX.", "X..X", "X..X", "X..X", "X..X");
        def('i', "X", ".", "X", "X", "X", "X", "X");
        def('j', "..X", "...", "..X", "..X", "..X", "..X", "..X", "X.X", ".X.");
        def('k', "X...", "X...", "X..X", "X.X.", "XX..", "X.X.", "X..X");
        def('l', "X.", "X.", "X.", "X.", "X.", "X.", ".X");
        def('m', ".....", ".....", "XX.X.", "X.X.X", "X.X.X", "X.X.X", "X.X.X");
        def('n', "....", "....", "XXX.", "X..X", "X..X", "X..X", "X..X");
        def('o', "....", "....", ".XX.", "X..X", "X..X", "X..X", ".XX.");
        def('p', "....", "....", "XXX.", "X..X", "X..X", "X..X", "XXX.", "X...", "X...");
        def('q', "....", "....", ".XXX", "X..X", "X..X", "X..X", ".XXX", "...X", "...X");
        def('r', "...", "...", "X.X", "XX.", "X..", "X..", "X..");
        def('s', "...", "...", ".XX", "X..", ".X.", "..X", "XX.");
        def('t', ".X.", ".X.", "XXX", ".X.", ".X.", ".X.", "..X");
        def('u', "....", "....", "X..X", "X..X", "X..X", "X..X", ".XXX");
        def('v', ".....", ".....", "X...X", "X...X", ".X.X.", ".X.X.", "..X..");
        def('w', ".....", ".....", "X...X", "X...X", "X.X.X", "X.X.X", ".X.X.");
        def('x', "....", "....", "X..X", "X..X", ".XX.", "X..X", "X..X");
        def('y', "....", "....", "X..X", "X..X", "X..X", "X..X", ".XXX", "...X", ".XX.");
        def('z', "....", "....", "XXXX", "...X", ".XX.", "X...", "XXXX");

        def('0', ".XXX.", "X...X", "X..XX", "X.X.X", "XX..X", "X...X", ".XXX.");
        def('1', ".X.", "XX.", ".X.", ".X.", ".X.", ".X.", "XXX");
        def('2', ".XXX.", "X...X", "....X", "...X.", "..X..", ".X...", "XXXXX");
        def('3', "XXXX.", "....X", "....X", ".XXX.", "....X", "....X", "XXXX.");
        def('4', "...X.", "..XX.", ".X.X.", "X..X.", "XXXXX", "...X.", "...X.");
        def('5', "XXXXX", "X....", "XXXX.", "....X", "....X", "X...X", ".XXX.");
        def('6', "..XX.", ".X...", "X....", "XXXX.", "X...X", "X...X", ".XXX.");
        def('7', "XXXXX", "....X", "...X.", "..X..", ".X...", ".X...", ".X...");
        def('8', ".XXX.", "X...X", "X...X", ".XXX.", "X...X", "X...X", ".XXX.");
        def('9', ".XXX.", "X...X", "X...X", ".XXXX", "....X", "...X.", ".XX..");
        def('.', ".", ".", ".", ".", ".", ".", "X");
        def(',', "..", "..", "..", "..", "..", ".X", "X.");
        def(':', ".", ".", "X", ".", ".", "X", ".");
        def(';', "..", "..", ".X", "..", "..", ".X", "X.");
        def('!', "X", "X", "X", "X", "X", ".", "X");
        def('?', ".XXX.", "X...X", "....X", "...X.", "..X..", ".....", "..X..");
        def('-', "...", "...", "...", "XXX", "...", "...", "...");
        def('+', ".....", "..X..", "..X..", "XXXXX", "..X..", "..X..", ".....");
        def('/', "....X", "....X", "...X.", "..X..", ".X...", "X....", "X....");
        def('(', "..X", ".X.", "X..", "X..", "X..", ".X.", "..X");
        def(')', "X..", ".X.", "..X", "..X", "..X", ".X.", "X..");
        def('\'', "X", "X", ".", ".", ".", ".", ".");
        def('"', "X.X", "X.X", "...", "...", "...", "...", "...");
        def('%', "XX..X", "XX..X", "...X.", "..X..", ".X...", "X..XX", "X..XX");
        def('=', "....", "....", "XXXX", "....", "XXXX", "....", "....");
        def('<', "...X", "..X.", ".X..", "X...", ".X..", "..X.", "...X");
        def('>', "X...", ".X..", "..X.", "...X", "..X.", ".X..", "X...");
        def('_', ".....", ".....", ".....", ".....", ".....", ".....", "XXXXX");
        def('*', ".....", "X.X.X", ".XXX.", "XXXXX", ".XXX.", "X.X.X", ".....");
        def('[', "XX", "X.", "X.", "X.", "X.", "X.", "XX");
        def(']', "XX", ".X", ".X", ".X", ".X", ".X", "XX");
        def('×', ".....", ".....", "X...X", ".X.X.", "..X..", ".X.X.", "X...X");
        def('#', ".X.X.", ".X.X.", "XXXXX", ".X.X.", "XXXXX", ".X.X.", ".X.X.");
        def('&', ".XX..", "X..X.", "X.X..", ".X...", "X.X.X", "X..X.", ".XX.X");

        String[][] sm = {
            {"XXX", "X.X", "X.X", "X.X", "XXX"}, {".X.", "XX.", ".X.", ".X.", "XXX"},
            {"XXX", "..X", "XXX", "X..", "XXX"}, {"XXX", "..X", ".XX", "..X", "XXX"},
            {"X.X", "X.X", "XXX", "..X", "..X"}, {"XXX", "X..", "XXX", "..X", "XXX"},
            {"XXX", "X..", "XXX", "X.X", "XXX"}, {"XXX", "..X", "..X", ".X.", ".X."},
            {"XXX", "X.X", "XXX", "X.X", "XXX"}, {"XXX", "X.X", "XXX", "..X", "XXX"},
        };
        for (int d = 0; d < 10; d++) {
            SMALL[d] = new boolean[5][3];
            for (int y = 0; y < 5; y++)
                for (int x = 0; x < 3; x++) SMALL[d][y][x] = sm[d][y].charAt(x) == 'X';
        }
    }

    /** The glyph used for a character (accented letters use their base letter). */
    private static char base(char c) {
        switch (c) {
            case 'Ä': case 'Å': return 'A';
            case 'Ö': return 'O';
            case 'É': return 'E';
            case 'Ü': return 'U';
            case 'ä': case 'å': return 'a';
            case 'ö': return 'o';
            case 'é': return 'e';
            case 'ü': return 'u';
            default: return c;
        }
    }

    private static boolean[][] glyph(char c) {
        boolean[][] g = GLYPHS.get(base(c));
        if (g == null) g = GLYPHS.get(Character.toUpperCase(base(c)));
        return g;
    }

    /** The pixels of a glyph (rows of booleans), or null. */
    static boolean[][] glyphRows(char c) {
        return glyph(c);
    }

    static boolean has(char c) {
        return c == ' ' || glyph(c) != null;
    }

    static int charWidth(char c) {
        if (c == ' ') return 3;
        boolean[][] g = glyph(c);
        return g == null ? 5 : g[0].length;
    }

    static int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += charWidth(s.charAt(i));
            if (i < s.length() - 1) w += 1;
        }
        return w;
    }

    /** Draws text; y is the top of the capital letters. */
    static void draw(Screen s, String text, int x, int y, int color) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            char raw = text.charAt(i);
            if (raw == ' ') { cx += 4; continue; }
            boolean[][] g = glyph(raw);
            if (g == null) g = GLYPHS.get('?');
            for (int gy = 0; gy < g.length; gy++)
                for (int gx = 0; gx < g[gy].length; gx++)
                    if (g[gy][gx]) s.pset(cx + gx, y + gy, color);
            // umlauts and rings
            boolean lower = Character.isLowerCase(raw);
            int dy = lower ? y : y - 2;
            if (raw == 'Ä' || raw == 'Ö' || raw == 'Ü' || raw == 'ä' || raw == 'ö' || raw == 'ü') {
                int w = g[0].length;
                s.pset(cx + (w > 4 ? 1 : 0), dy, color);
                s.pset(cx + w - (w > 4 ? 2 : 1), dy, color);
            } else if (raw == 'Å' || raw == 'å') {
                s.pset(cx + g[0].length / 2, dy, color);
            } else if (raw == 'É' || raw == 'é') {
                s.pset(cx + g[0].length - 2, dy, color);
            }
            cx += g[0].length + 1;
        }
    }

    /** Small 3x5 digits (stack counts). right is the right edge. */
    static void drawSmallNumber(Screen s, int n, int right, int y, int color, int outline) {
        String str = Integer.toString(n);
        int w = str.length() * 4 - 1;
        int x0 = right - w;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < str.length(); i++) {
                boolean[][] g = SMALL[str.charAt(i) - '0'];
                int bx = x0 + i * 4;
                for (int gy = 0; gy < 5; gy++)
                    for (int gx = 0; gx < 3; gx++) {
                        if (!g[gy][gx]) continue;
                        if (pass == 0) {
                            if (outline < 0) continue;
                            s.pset(bx + gx - 1, y + gy, outline);
                            s.pset(bx + gx + 1, y + gy, outline);
                            s.pset(bx + gx, y + gy - 1, outline);
                            s.pset(bx + gx, y + gy + 1, outline);
                            s.pset(bx + gx + 1, y + gy + 1, outline);
                        } else {
                            s.pset(bx + gx, y + gy, color);
                        }
                    }
            }
        }
    }

    /** Splits text into lines no wider than maxW pixels. */
    static java.util.List<String> wrap(String text, int maxW) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String next = line.length() == 0 ? word : line + " " + word;
                if (width(next) > maxW && line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(next);
                }
            }
            out.add(line.toString());
        }
        return out;
    }
}
