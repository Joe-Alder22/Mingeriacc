package mingeriacc;

import java.util.Random;

/**
 * The sky and the distant landscape: smooth sky gradients, sun and moon with a
 * soft glow, soft clouds and parallax layers of hills and forests.
 */
final class Background {
    static final int FOREST = 0, DESERT = 1, JUNGLE = 2, CORRUPTION = 3, OCEAN = 4, UNDERWORLD = 5, SNOW = 6;
    static final int BIOMES = 7;

    private final Noise noise = new Noise(777);
    private final Cloud[] clouds;
    private final Sprite[] pines = new Sprite[4], rounds = new Sprite[4], cacti = new Sprite[3];
    private final Sprite[] jungleTrees = new Sprite[4], deadTrees = new Sprite[4], spires = new Sprite[4];
    /** Camera height at which the underworld background sits in the middle of the view. */
    double hellRefY;
    /** A blood moon colours the night sky red. */
    boolean bloodMoon;
    private int capColor, capDepth;   // snow caps on the next landscape layer
    private final double[] amount = new double[BIOMES];
    private double cloudDrift;
    private int[] grad = new int[0];

    private static final class Cloud {
        int w, h;
        byte[] alpha, shade;
        double x, y, speed, depth;
    }

    Background() {
        Random r = new Random(99);
        clouds = new Cloud[11];
        for (int i = 0; i < clouds.length; i++) clouds[i] = makeCloud(r, i);
        for (int i = 0; i < 4; i++) {
            pines[i] = pine(r, 14 + i * 4);
            rounds[i] = roundTree(r, 12 + i * 4);
        }
        for (int i = 0; i < 3; i++) cacti[i] = cactus(r, 10 + i * 5);
        for (int i = 0; i < 4; i++) {
            jungleTrees[i] = jungleTree(r, 22 + i * 6);
            deadTrees[i] = deadTree(r, 14 + i * 5);
            spires[i] = spire(r, 20 + i * 10);
        }
        amount[FOREST] = 1;
    }

    private static Sprite jungleTree(Random r, int h) {
        int w = h * 3 / 4;
        Sprite s = new Sprite(w, h);
        int cx = w / 2;
        int canopy = h / 3;
        for (int y = canopy / 2; y < h; y++) {
            s.set(cx, y, 2);
            if (y > h - 4) { s.set(cx - 1, y, 2); s.set(cx + 1, y, 2); }
        }
        for (int k = 0; k < 5; k++) {
            double bx = cx + (r.nextDouble() - 0.5) * w * 0.7, by = canopy * (0.4 + r.nextDouble() * 0.5);
            double rx = w * (0.18 + r.nextDouble() * 0.12), ry = canopy * 0.45;
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    double dx = (x + 0.5 - bx) / rx, dy = (y + 0.5 - by) / ry;
                    if (dx * dx + dy * dy <= 1) s.set(x, y, dy < -0.2 ? 1 : 2);
                }
        }
        // hanging vines
        for (int k = 0; k < 4; k++) {
            int x = r.nextInt(w), y0 = canopy;
            for (int y = y0; y < y0 + 3 + r.nextInt(6) && y < h; y++) if (s.get(x, y - 1) != 0 || y == y0) s.set(x, y, 2);
        }
        return s;
    }

    private static Sprite deadTree(Random r, int h) {
        int w = h;
        Sprite s = new Sprite(w, h);
        double x = w / 2.0;
        for (int y = h - 1; y > h / 5; y--) {
            s.set((int) x, y, 2);
            if (y > h - 4) s.set((int) x + 1, y, 2);
            x += (r.nextDouble() - 0.5) * 0.6;
            if (r.nextInt(5) == 0) {
                // a thorny branch
                int dir = r.nextBoolean() ? 1 : -1;
                double bx = x, by = y;
                for (int k = 0; k < 3 + r.nextInt(h / 4); k++) {
                    bx += dir;
                    by -= 0.7;
                    s.set((int) bx, (int) by, 1);
                }
            }
        }
        return s;
    }

    private static Sprite spire(Random r, int h) {
        int w = Math.max(5, h / 3);
        Sprite s = new Sprite(w, h);
        for (int y = 0; y < h; y++) {
            double half = w / 2.0 * Math.pow(y / (double) h, 0.8) + 0.5;
            double off = Math.sin(y * 0.3) * 0.8;
            for (int x = 0; x < w; x++) {
                double dx = x + 0.5 - w / 2.0 - off;
                if (Math.abs(dx) <= half) s.set(x, y, dx < 0 ? 1 : 2);
            }
        }
        return s;
    }

    // ---- generated shapes ---------------------------------------------------

    private static Cloud makeCloud(Random r, int i) {
        Cloud c = new Cloud();
        c.w = 70 + r.nextInt(90);
        c.h = 26 + r.nextInt(18);
        c.alpha = new byte[c.w * c.h];
        c.shade = new byte[c.w * c.h];
        int puffs = 5 + r.nextInt(5);
        double[][] p = new double[puffs][];
        for (int k = 0; k < puffs; k++) {
            double px = c.w * (0.18 + 0.64 * r.nextDouble());
            double rad = Math.min(c.h * (0.25 + 0.3 * r.nextDouble()), (c.h - 4) / 2.0);
            double py = Math.max(rad + 1, c.h - rad - 2 - r.nextDouble() * c.h * 0.25);
            p[k] = new double[]{px, py, Math.min(rad * 1.5, Math.min(px, c.w - px) - 1), rad};
        }
        for (int y = 0; y < c.h; y++)
            for (int x = 0; x < c.w; x++) {
                double dens = 0;
                for (double[] q : p) {
                    double dx = (x - q[0]) / q[2], dy = (y - q[1]) / q[3];
                    dens = Math.max(dens, 1 - (dx * dx + dy * dy));
                }
                if (y > c.h - 4) dens *= (c.h - y) / 4.0; // flat bottom
                if (dens <= 0) continue;
                double a = Math.min(1, dens / 0.35);
                a = a * a * (3 - 2 * a);
                c.alpha[x + y * c.w] = (byte) (a * 235);
                double sh = 1 - y / (double) c.h * 0.8 + dens * 0.25;
                c.shade[x + y * c.w] = (byte) (Math.max(0, Math.min(1, sh)) * 255);
            }
        c.x = r.nextDouble() * 2400;
        c.depth = 0.25 + r.nextDouble() * 0.75;
        c.y = 8 + r.nextDouble() * 90 * (1.2 - c.depth);
        c.speed = 0.15 + c.depth * 0.25;
        return c;
    }

    /** Tree silhouettes: 1 = lit side, 2 = shaded side. */
    private static Sprite pine(Random r, int h) {
        int w = h * 2 / 3 | 1;
        Sprite s = new Sprite(w, h);
        int cx = w / 2;
        int tiers = 3 + h / 12;
        for (int y = 0; y < h - 2; y++) {
            double t = y / (double) (h - 2);
            double tierT = (t * tiers) % 1.0;
            double half = (0.15 + t * 0.85) * (0.55 + tierT * 0.45) * w / 2.0;
            for (int x = 0; x < w; x++) {
                double dx = x + 0.5 - cx - 0.5;
                if (Math.abs(dx) <= half) s.set(x, y, dx < half * 0.1 ? 1 : 2);
            }
        }
        s.set(cx, h - 2, 2);
        s.set(cx, h - 1, 2);
        return s;
    }

    private static Sprite roundTree(Random r, int h) {
        int w = h;
        Sprite s = new Sprite(w, h);
        int trunk = Math.max(3, h / 4);
        double[][] blobs = new double[4][];
        for (int k = 0; k < 4; k++) {
            blobs[k] = new double[]{w / 2.0 + (r.nextDouble() - 0.5) * w * 0.45,
                    (h - trunk) * (0.35 + r.nextDouble() * 0.3), (h - trunk) * (0.28 + r.nextDouble() * 0.12)};
        }
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                boolean in = false;
                boolean lit = false;
                for (double[] b : blobs) {
                    double dx = x + 0.5 - b[0], dy = y + 0.5 - b[1];
                    if (dx * dx + dy * dy <= b[2] * b[2]) {
                        in = true;
                        if (dx + dy * 0.8 < -b[2] * 0.2) lit = true;
                    }
                }
                if (in) s.set(x, y, lit ? 1 : 2);
                else if (y >= h - trunk && Math.abs(x + 0.5 - w / 2.0) < 1) s.set(x, y, 2);
            }
        return s;
    }

    private static Sprite cactus(Random r, int h) {
        int w = 9;
        Sprite s = new Sprite(w, h);
        for (int y = 1; y < h; y++) { s.set(3, y, 1); s.set(4, y, 2); s.set(5, y, 2); }
        s.set(4, 0, 2);
        int ay = h / 3 + r.nextInt(Math.max(1, h / 4));
        for (int x = 0; x < 3; x++) s.set(x, ay, 2);
        for (int y = ay - 4; y <= ay; y++) s.set(0, Math.max(0, y), 1);
        int by = h / 2 + r.nextInt(Math.max(1, h / 4));
        for (int x = 6; x < 9; x++) s.set(x, by, 2);
        for (int y = by - 3; y <= by; y++) s.set(8, y, 2);
        return s;
    }

    // ---- sky colours -------------------------------------------------------

    private static final double[] KEY_H = {0, 4.5, 5.4, 6.6, 17.4, 18.7, 19.8, 24};
    private static final int[] KEY_TOP = {Pal.SKY_NIGHT_TOP, Pal.SKY_NIGHT_TOP, Pal.SKY_DAWN_TOP, Pal.SKY_DAY_TOP,
            Pal.SKY_DAY_TOP, Pal.SKY_DUSK_TOP, Pal.SKY_NIGHT_TOP, Pal.SKY_NIGHT_TOP};
    private static final int[] KEY_BOT = {Pal.SKY_NIGHT_BOT, Pal.SKY_NIGHT_BOT, Pal.SKY_DAWN_BOT, Pal.SKY_DAY_BOT,
            Pal.SKY_DAY_BOT, Pal.SKY_DUSK_BOT, Pal.SKY_NIGHT_BOT, Pal.SKY_NIGHT_BOT};

    static int skyColor(double hour, boolean top) {
        for (int i = 0; i < KEY_H.length - 1; i++) {
            if (hour >= KEY_H[i] && hour <= KEY_H[i + 1]) {
                double t = (hour - KEY_H[i]) / (KEY_H[i + 1] - KEY_H[i]);
                t = t * t * (3 - 2 * t);
                int[] k = top ? KEY_TOP : KEY_BOT;
                return Pal.lerp(k[i], k[i + 1], t);
            }
        }
        return top ? Pal.SKY_NIGHT_TOP : Pal.SKY_NIGHT_BOT;
    }

    /** Amount of daylight 0..1 for tinting the backgrounds. */
    static double daylight(double h) {
        if (h >= 6.5 && h < 17.5) return 1;
        if (h >= 4.5 && h < 6.5) return (h - 4.5) / 2.0;
        if (h >= 17.5 && h < 19.5) return 1 - (h - 17.5) / 2.0;
        return 0;
    }

    /** How strongly dawn or dusk colours the horizon (0..1). */
    static double twilight(double h) {
        double a = Math.max(0, 1 - Math.abs(h - 5.6) / 1.3);
        double b = Math.max(0, 1 - Math.abs(h - 18.6) / 1.3);
        return Math.max(a, b);
    }

    /** Colour of sunlight on the world (multiplies lit tiles). */
    static int sunTint(double h) {
        double day = daylight(h), tw = twilight(h);
        int c = Pal.lerp(0x9cb2ff, 0xffffff, day);
        return Pal.lerp(c, 0xffd0a8, tw * 0.5);
    }

    // ---- drawing ------------------------------------------------------------

    /** Eases the biome mix towards the given biome. */
    void setBiome(int biome, boolean instant) {
        for (int b = 0; b < BIOMES; b++) {
            double target = b == biome ? 1 : 0;
            amount[b] = instant ? target : amount[b] + (target - amount[b]) * 0.02;
        }
    }

    /** Sky and landscape. camSpawnY = camera height at the spawn point. */
    void draw(Screen s, double hour, double camX, double camY, double camSpawnY, long ticks) {
        int top = skyColor(hour, true), bot = skyColor(hour, false);
        double skyOff = (camY - camSpawnY) * 0.1;
        double day = daylight(hour), tw = twilight(hour);

        // sun or moon position
        int horizon = (int) (s.h * 0.62 - skyOff);
        boolean sunUp = hour >= 4.5 && hour <= 19.5;
        double p = sunUp ? (hour - 4.5) / 15.0 : ((hour >= 19.5 ? hour - 19.5 : hour + 4.5) / 9.0);
        int bx = (int) (p * (s.w + 40)) - 20;
        int by = (int) (horizon - Math.sin(p * Math.PI) * s.h * (sunUp ? 0.5 : 0.45));

        // smooth gradient with a warm glow near the sun at dawn and dusk
        int glowCol = Pal.lerp(0xff9a50, 0xffd890, day);
        for (int y = 0; y < s.h; y++) {
            double t = Math.max(0, Math.min(1, (y + skyOff) / (s.h * 0.9)));
            t = t * t * (3 - 2 * t) * 0.85 + t * 0.15;
            int c = Pal.lerp(top, bot, t);
            int row = y * s.w;
            java.util.Arrays.fill(s.px, row, row + s.w, c);
            if (tw > 0.02 && sunUp) {
                double vy = Math.max(0, 1 - Math.abs(y - horizon) / (s.h * 0.45));
                if (vy <= 0) continue;
                for (int x = 0; x < s.w; x++) {
                    double dx = Math.abs(x - bx) / (double) s.w;
                    double g = tw * vy * vy * Math.max(0, 1 - dx * 1.4);
                    if (g > 0.01) s.px[row + x] = Pal.add(s.px[row + x], glowCol, (int) (g * 90));
                }
            }
        }

        double blood = bloodMoon ? 1 - day : 0;
        if (blood > 0.01) {
            int a = (int) (blood * 200);
            for (int i = 0; i < s.w * s.h; i++) {
                int c = s.px[i];
                s.px[i] = Pal.blend(c, Pal.add(Pal.mul(c, 200, 70, 70), 0x3a0606, 256), a);
            }
        }

        // stars
        if (day < 0.8) {
            int a = (int) ((1 - day / 0.8) * 220);
            for (int i = 0; i < 180; i++) {
                int h = Noise.hash(i, 17);
                int sx = Math.floorMod((h & 0xffff) - (int) (camX * 0.02), Math.max(1, s.w + 20)) - 10;
                int sy = Math.floorMod((h >>> 16) & 0x7fff, Math.max(1, (int) (s.h * 0.72))) - (int) (skyOff * 0.5);
                double tw2 = 0.6 + 0.4 * Math.sin(ticks * 0.03 + i * 1.7);
                int c = (i % 7 == 0) ? 0xffe8c0 : (i % 5 == 0) ? 0xc8d8ff : 0xffffff;
                int aa = (int) (a * tw2 * (0.4 + (h >>> 8 & 3) * 0.2));
                s.pblend(sx, sy, c, aa);
                if (i % 13 == 0) s.glow(sx + 0.5, sy + 0.5, 2.5, c, aa / 3);
            }
        }

        // sun and moon
        if (sunUp) {
            int sunC = Pal.lerp(0xffb070, 0xfff4d0, day);
            s.glow(bx, by, 70, Pal.lerp(0xff8a40, 0xfff0c0, day), 70);
            s.glow(bx, by, 22, sunC, 160);
            s.disc(bx, by, 7.5, sunC, 256);
            s.disc(bx - 1, by - 1, 5, 0xfffcf0, 256);
        } else if (bloodMoon) {
            s.glow(bx, by, 50, 0xc02020, 70);
            s.disc(bx, by, 7.5, 0xd83a32, 256);
            s.disc(bx - 2, by - 2, 1.6, 0xa82420, 200);
            s.disc(bx + 2, by + 1, 2.2, 0xb02a24, 200);
            s.disc(bx - 1, by + 3, 1.2, 0xb02a24, 200);
        } else {
            s.glow(bx, by, 40, 0x8090c0, 45);
            s.disc(bx, by, 7, 0xdcdcee, 256);
            s.disc(bx - 2, by - 2, 1.6, 0xb8b8cc, 200);
            s.disc(bx + 2, by + 1, 2.2, 0xbcbcd0, 200);
            s.disc(bx - 1, by + 3, 1.2, 0xbcbcd0, 200);
        }

        // clouds
        cloudDrift += 0.06;
        int cloudLight = Pal.lerp(0x48506e, 0xffffff, day);
        int cloudShade = Pal.lerp(0x262a40, 0xb4c4dc, day);
        if (tw > 0) {
            cloudLight = Pal.lerp(cloudLight, 0xffd0b0, tw * 0.6);
            cloudShade = Pal.lerp(cloudShade, 0xc07888, tw * 0.5);
        }
        double cloudAlpha = 0.55 + 0.45 * Math.max(day, 0.3);
        for (Cloud c : clouds) {
            double span = s.w + 400;
            int cx = (int) (Math.floorMod((long) (c.x + cloudDrift * c.speed - camX * 0.05 * c.depth), (long) span)) - 200;
            int cy = (int) (c.y - skyOff * (1 + c.depth));
            drawCloud(s, c, cx, cy, cloudLight, cloudShade, cloudAlpha * (0.6 + c.depth * 0.4));
        }

        // landscape layers
        int dimCol = Pal.lerp(0x3a4668, 0xffffff, 0.22 + 0.78 * day);
        if (tw > 0) dimCol = Pal.lerp(dimCol, 0xffd0b8, tw * 0.35);
        if (blood > 0.01) dimCol = Pal.lerp(dimCol, 0x8a3a3a, blood * 0.6);
        double corr = amount[CORRUPTION];
        if (corr > 0.01) {
            // the corruption darkens the sky
            int a = (int) (corr * 110);
            for (int i = 0; i < s.w * s.h; i++) s.px[i] = Pal.blend(s.px[i], Pal.mul(s.px[i], 150, 110, 190), a);
        }
        for (int b = 0; b < BIOMES; b++) {
            if (amount[b] < 0.01) continue;
            int a = (int) Math.round(amount[b] * 256);
            switch (b) {
                case FOREST: forest(s, camX, camY, camSpawnY, bot, dimCol, a); break;
                case DESERT: desert(s, camX, camY, camSpawnY, bot, dimCol, a); break;
                case JUNGLE: jungle(s, camX, camY, camSpawnY, bot, dimCol, a); break;
                case CORRUPTION: corruption(s, camX, camY, camSpawnY, bot, dimCol, a); break;
                case OCEAN: ocean(s, camX, camY, camSpawnY, bot, dimCol, a); break;
                case SNOW: snow(s, camX, camY, camSpawnY, bot, dimCol, a); break;
                default: break;
            }
        }
        if (amount[UNDERWORLD] > 0.01) hell(s, camX, camY, ticks, (int) Math.round(amount[UNDERWORLD] * 256));
    }

    private void jungle(Screen s, double camX, double camY, double camSpawnY, int horizon, int dim, int a) {
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x6a9a7a, 0.5, 0.12, 0.08, 280, 80, 91, 0.5,
                null, 0, 0, false);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x3e7a3a, 0.58, 0.2, 0.18, 160, 50, 103, 0.28,
                jungleTrees, 9, 12, false);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x28602a, 0.68, 0.32, 0.32, 100, 34, 117, 0.06,
                jungleTrees, 7, 14, false);
    }

    private void corruption(Screen s, double camX, double camY, double camSpawnY, int horizon, int dim, int a) {
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x453a5e, 0.5, 0.12, 0.08, 120, 100, 131, 0.25,
                spires, 13, 6, false);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x362c4c, 0.6, 0.2, 0.18, 90, 50, 143, 0.12,
                deadTrees, 9, 9, false);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x34284a, 0.69, 0.32, 0.32, 70, 30, 157, 0.05,
                deadTrees, 8, 11, false);
    }

    /** Snow: pale mountains with white caps, a dark pine forest and white drifts. */
    private void snow(Screen s, double camX, double camY, double camSpawnY, int horizon, int dim, int a) {
        capColor = 0xf4f8ff;
        capDepth = 12;
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x9ab0cc, 0.47, 0.12, 0.08, 200, 120, 223, 0.4,
                null, 0, 0, false);
        capColor = 0;
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x3e5a64, 0.59, 0.2, 0.18, 160, 46, 229, 0.25,
                pines, 6, 11, false);
        capColor = 0xffffff;
        capDepth = 5;
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0xd8e2f0, 0.69, 0.32, 0.32, 130, 26, 239, 0.04,
                pines, 17, 4, false);
        capColor = 0;
    }

    private void ocean(Screen s, double camX, double camY, double camSpawnY, int horizon, int dim, int a) {
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0x3a78b8, 0.68, 0.08, 0.1, 30, 2, 171, 0.3,
                null, 0, 0, true);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0xd8c08a, 0.76, 0.2, 0.3, 120, 12, 181, 0.05,
                null, 0, 0, true);
    }

    /** The underworld: a red glow, dark spires and rising embers. */
    private void hell(Screen s, double camX, double camY, long ticks, int alpha) {
        int top = 0x120404, bot = 0x9a300c;
        for (int y = 0; y < s.h; y++) {
            int c = Pal.lerp(top, bot, Math.pow(y / (double) s.h, 1.6));
            int row = y * s.w;
            for (int x = 0; x < s.w; x++) s.px[row + x] = alpha >= 256 ? c : Pal.blend(s.px[row + x], c, alpha);
        }
        double ref = hellRefY;
        layer(s, camX, camY, ref, 0xff6a20, 0xffffff, alpha, 0x3a1008, 0.62, 0.1, 0.1, 140, 90, 191, 0.25,
                spires, 11, 8, false);
        layer(s, camX, camY, ref, 0xff6a20, 0xffffff, alpha, 0x1e0806, 0.78, 0.25, 0.25, 90, 50, 211, 0.1,
                spires, 9, 9, false);
        for (int i = 0; i < 70; i++) {
            int h = Noise.hash(i, 77);
            double speed = 0.2 + (h & 7) * 0.05;
            int x = Math.floorMod((h >>> 8 & 0xffff) - (int) (camX * 0.3) + (int) (Math.sin(ticks * 0.01 + i) * 12), s.w);
            int y = Math.floorMod((int) ((h >>> 3 & 0x3ff) - ticks * speed), s.h);
            s.padd(x, y, 0xff8030, alpha * 3 / 4);
            if ((i & 3) == 0) s.glow(x + 0.5, y + 0.5, 3, 0xff6020, alpha / 5);
        }
    }

    private static void drawCloud(Screen s, Cloud c, int x0, int y0, int light, int shade, double alpha) {
        for (int y = 0; y < c.h; y++) {
            int sy = y0 + y;
            if (sy < 0 || sy >= s.h) continue;
            for (int x = 0; x < c.w; x++) {
                int sx = x0 + x;
                if (sx < 0 || sx >= s.w) continue;
                int a = c.alpha[x + y * c.w] & 255;
                if (a == 0) continue;
                int col = Pal.lerp(shade, light, (c.shade[x + y * c.w] & 255) / 255.0);
                int i = sx + sy * s.w;
                s.px[i] = Pal.blend(s.px[i], col, (int) (a * alpha));
            }
        }
    }

    private void layer(Screen s, double camX, double camY, double camSpawnY, int horizonCol, int dim, int alpha,
                       int col, double base, double fy, double fx, double scale, double amp, double seed,
                       double haze, Sprite[] trees, int treeEvery, int treeChance, boolean dunes) {
        int c = Pal.mulLight(Pal.lerp(col, horizonCol, haze), dim);
        int cLit = Pal.mulLight(Pal.lerp(Pal.lerp(col, 0xffffff, 0.12), horizonCol, haze), dim);
        int cShade = Pal.shade(c, 215);
        int fog = Pal.mulLight(Pal.lerp(horizonCol, col, 0.35), dim);
        int by = (int) (s.h * base - (camY - camSpawnY) * fy);
        if (by > s.h + 80) return;
        // colour by distance below the ridge line
        int span = Math.max(1, s.h - by + (int) amp / 2);
        if (grad.length < s.h + 1) grad = new int[s.h + (int) amp + 200];
        for (int d = 0; d < grad.length; d++)
            grad[d] = d < 2 ? cLit : Pal.lerp(c, fog, Math.min(1, d / (double) span * 1.6) * 0.6);
        if (capColor != 0) {
            int cap = Pal.mulLight(Pal.lerp(capColor, horizonCol, haze * 0.5), dim);
            for (int d = 0; d < capDepth && d < grad.length; d++)
                grad[d] = Pal.lerp(cap, grad[d], Math.max(0, (d - capDepth * 0.5) / (capDepth * 0.5)));
        }
        for (int x = 0; x < s.w; x++) {
            double wx = x + camX * fx + seed;
            double n = dunes ? Math.sin(wx / scale * 2.2) * 0.35 + noise.fractal(wx / (scale * 1.7), seed, 2) * 0.65
                    : noise.fractal(wx / scale, seed, 3);
            int hy = (int) (by - (n + 0.5) * amp);
            int y0 = Math.max(0, hy);
            for (int y = y0; y < s.h; y++) {
                int v = grad[Math.min(grad.length - 1, y - hy)];
                int i = y * s.w + x;
                s.px[i] = alpha >= 256 ? v : Pal.blend(s.px[i], v, alpha);
            }
            // trees standing on the ridge
            if (trees != null) {
                int cell = (int) Math.floor(wx / treeEvery);
                int local = (int) (wx - cell * (double) treeEvery);
                int h = Noise.hash(cell, (int) seed);
                if ((h & 15) < treeChance && local == (h >>> 4 & 3)) {
                    Sprite t = trees[(h >>> 8) & (trees.length - 1)];
                    int tx = x - t.w / 2;
                    int ty = hy - t.h + 2 + ((h >>> 12) & 1);
                    for (int yy = 0; yy < t.h; yy++)
                        for (int xx = 0; xx < t.w; xx++) {
                            int m = t.p[xx + yy * t.w];
                            if (m == 0) continue;
                            int px = tx + xx, py = ty + yy;
                            if (px < 0 || py < 0 || px >= s.w || py >= s.h) continue;
                            int v = (m & 0xff) == 1 ? cLit : cShade;
                            int i = py * s.w + px;
                            s.px[i] = alpha >= 256 ? v : Pal.blend(s.px[i], v, alpha);
                        }
                }
            }
        }
    }

    private void forest(Screen s, double camX, double camY, double camSpawnY, int horizon, int dim, int a) {
        layer(s, camX, camY, camSpawnY, horizon, dim, a, Pal.HILL_FAR, 0.50, 0.12, 0.08, 300, 90, 11, 0.55,
                null, 0, 0, false);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, Pal.HILL_MID, 0.58, 0.2, 0.18, 170, 55, 23, 0.3,
                pines, 7, 9, false);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, Pal.HILL_NEAR, 0.68, 0.32, 0.32, 110, 36, 37, 0.08,
                rounds, 11, 10, false);
    }

    private void desert(Screen s, double camX, double camY, double camSpawnY, int horizon, int dim, int a) {
        layer(s, camX, camY, camSpawnY, horizon, dim, a, Pal.DUNE_FAR, 0.52, 0.12, 0.08, 260, 50, 51, 0.45,
                null, 0, 0, true);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, 0xd0b07a, 0.6, 0.2, 0.18, 150, 34, 63, 0.25,
                null, 0, 0, true);
        layer(s, camX, camY, camSpawnY, horizon, dim, a, Pal.DUNE_NEAR, 0.69, 0.32, 0.32, 100, 26, 77, 0.05,
                cacti, 23, 5, true);
    }
}
