package mingeriacc;

import java.util.ArrayList;

/** Arrows, magic bolts and other things that fly. */
final class Projectile {
    static final int WOOD_ARROW = 1, STINGER = 2, FIREBALL = 3, SCYTHE = 4, FLAMING_ARROW = 5,
            FLAME_BOLT = 6, WATER_BOLT = 7, ROCK = 8, SHARD = 9, UNHOLY_ARROW = 10, FROST_BOLT = 11,
            FALLING_STAR = 12;

    final int type;
    double x, y, vx, vy;
    int damage;
    double knock;
    boolean hostile;
    int life = 600, age;
    boolean dead;
    int pierce = 1;
    int crit;
    int effect;          // Items.FX_*
    int bounces;         // water bolts bounce off walls
    double gravity;
    double spin;
    /** Creatures already hit (piercing projectiles hit each creature once). */
    private ArrayList<Mob> hit;

    Projectile(int type, double x, double y, double vx, double vy) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        switch (type) {
            case SCYTHE: life = 240; break;
            case FIREBALL: life = 300; break;
            case FLAME_BOLT: life = 75; break;
            case WATER_BOLT: life = 360; bounces = 3; pierce = 3; break;
            case ROCK: gravity = 0.12; break;
            case SHARD: life = 45; gravity = 0.15; break;
            case FROST_BOLT: life = 80; break;
            case FALLING_STAR: life = 2000; pierce = 99; break;
            default: break;
        }
        if (type == FIREBALL || type == FLAMING_ARROW || type == FLAME_BOLT) effect = Items.FX_FIRE;
        if (type == STINGER) effect = Items.FX_POISON;
        if (type == FROST_BOLT) effect = Items.FX_FROST;
    }

    boolean isArrow() {
        return type == WOOD_ARROW || type == FLAMING_ARROW || type == UNHOLY_ARROW;
    }

    /** Whether it has already hit this creature (and remembers it if not). */
    boolean alreadyHit(Mob m) {
        if (hit == null) hit = new ArrayList<>();
        if (hit.contains(m)) return true;
        hit.add(m);
        return false;
    }

    /** Moves the projectile; returns false when it hits a tile. */
    boolean update(World w) {
        age++;
        if (--life <= 0) return false;
        if (isArrow() && age > 12) vy = Math.min(6, vy + 0.09);
        vy = Math.min(6, vy + gravity);
        spin += Math.hypot(vx, vy) * 0.15;
        if (type == SCYTHE) {
            // scythes speed up and pass through walls
            vx *= 1.03;
            vy *= 1.03;
            x += vx;
            y += vy;
            return x > 0 && y > 0 && x < w.w * Tiles.T && y < w.h * Tiles.T;
        }
        int steps = (int) Math.ceil(Math.max(Math.abs(vx), Math.abs(vy)) / 3.0);
        for (int i = 0; i < steps; i++) {
            double nx = x + vx / steps, ny = y + vy / steps;
            if (solidAt(w, nx, ny)) {
                if (bounces <= 0) {
                    x = nx;
                    y = ny;
                    return false;
                }
                // bounce: reverse the axis that ran into the wall
                bounces--;
                boolean hx = solidAt(w, nx, y), hy = solidAt(w, x, ny);
                if (hx || !hy) vx = -vx;
                if (hy || !hx) vy = -vy;
                break;
            }
            x = nx;
            y = ny;
        }
        if (effect == Items.FX_FIRE && w.liquidAt((int) (x / Tiles.T), (int) (y / Tiles.T)) > 60
                && !w.isLava((int) (x / Tiles.T), (int) (y / Tiles.T))) return false;
        return x > 0 && y > 0 && x < w.w * Tiles.T && y < w.h * Tiles.T;
    }

    private static boolean solidAt(World w, double px, double py) {
        return w.solid((int) Math.floor(px / Tiles.T), (int) Math.floor(py / Tiles.T));
    }

    /** Hit box test against a body. */
    boolean hits(Body b) {
        double r = type == SCYTHE || type == FIREBALL || type == ROCK || type == FALLING_STAR ? 3
                : type == FLAME_BOLT || type == WATER_BOLT ? 2 : 0;
        if (r > 0) return x + r > b.x && x - r < b.x + b.w && y + r > b.y && y - r < b.y + b.h;
        return b.contains(x, y) || b.contains(x - vx * 0.5, y - vy * 0.5);
    }

    /** Light the projectile gives off (0 = none). */
    int lightLevel() {
        switch (type) {
            case FALLING_STAR: return 13;
            case FLAME_BOLT: return 10;
            case FIREBALL: return 9;
            case FLAMING_ARROW: case SCYTHE: return 7;
            case WATER_BOLT: case FROST_BOLT: return 6;
            case UNHOLY_ARROW: return 4;
            default: return 0;
        }
    }

    int lightColor() {
        switch (type) {
            case FALLING_STAR: return 0xfff0b0;
            case WATER_BOLT: return 0x6ab0ff;
            case FROST_BOLT: return 0xa8e0ff;
            case SCYTHE: case UNHOLY_ARROW: return 0xb070ff;
            default: return 0xffa050;
        }
    }

    void draw(Screen s, int camX, int camY, int light) {
        double px = x - camX, py = y - camY;
        switch (type) {
            case WOOD_ARROW: case FLAMING_ARROW: case UNHOLY_ARROW: {
                int item = type == WOOD_ARROW ? Items.WOOD_ARROW : type == FLAMING_ARROW ? Items.FLAMING_ARROW
                        : Items.UNHOLY_ARROW;
                double ang = Math.atan2(vy, vx);
                s.drawRotated(Items.ICON[item], 10.5, 3.5, px, py, ang + Math.PI / 4, false,
                        type == WOOD_ARROW ? light : 0xffffff);
                if (type == FLAMING_ARROW) s.glow(px, py, 5, 0xff8a30, 120);
                if (type == UNHOLY_ARROW) s.glow(px, py, 5, 0x9a4aff, 90);
                break;
            }
            case STINGER: {
                double d = Math.max(0.1, Math.hypot(vx, vy));
                for (int i = 0; i < 4; i++) s.pset((int) (px - vx / d * i), (int) (py - vy / d * i), Pal.mulLight(i == 0 ? 0x1a1a14 : 0xe8c030, light));
                break;
            }
            case FIREBALL:
                s.glow(px, py, 9, 0xff6a20, 140);
                s.disc(px, py, 2.5, Pal.LAVA_L, 256);
                s.disc(px - 0.5, py - 0.5, 1.2, 0xfff4c0, 256);
                break;
            case FLAME_BOLT: {
                double d = Math.max(0.1, Math.hypot(vx, vy));
                for (int i = 1; i < 6; i++)
                    s.disc(px - vx / d * i * 1.6, py - vy / d * i * 1.6, 2.2 - i * 0.35, i < 3 ? Pal.FLAME : Pal.FLAME_R, 200 - i * 30);
                s.glow(px, py, 10, 0xff7a20, 150);
                s.disc(px, py, 2.4, Pal.FLAME, 256);
                s.disc(px - 0.4, py - 0.4, 1.3, 0xfff8d0, 256);
                break;
            }
            case WATER_BOLT: {
                double d = Math.max(0.1, Math.hypot(vx, vy));
                for (int i = 1; i < 5; i++)
                    s.disc(px - vx / d * i * 1.8, py - vy / d * i * 1.8, 1.8 - i * 0.3, Pal.WATER_L, 150 - i * 25);
                s.glow(px, py, 8, 0x3a7aff, 110);
                s.disc(px, py, 2.8, 0x3a80e8, 230);
                s.disc(px - 0.8, py - 0.8, 1.2, 0xe0f4ff, 256);
                break;
            }
            case ROCK: case SHARD: {
                double r = type == ROCK ? 3.2 : 1.3;
                int base = Pal.mulLight(0x7a6a5a, light), dark = Pal.mulLight(0x4a3e34, light), lit = Pal.mulLight(0xa8988a, light);
                s.disc(px, py, r, dark, 256);
                s.disc(px - 0.4, py - 0.4, r - 0.6, base, 256);
                if (type == ROCK) {
                    // a few lit facets that turn as the rock spins
                    for (int k = 0; k < 3; k++) {
                        double a = spin + k * 2.1;
                        s.pset((int) Math.round(px + Math.cos(a) * 1.6), (int) Math.round(py + Math.sin(a) * 1.6), lit);
                    }
                    s.pset((int) Math.round(px + Math.cos(spin + 1) * 2.2), (int) Math.round(py + Math.sin(spin + 1) * 2.2),
                            Pal.mulLight(Pal.EMERALD, light));
                }
                break;
            }
            case FROST_BOLT: {
                double ang = Math.atan2(vy, vx);
                double cx = Math.cos(ang), cy = Math.sin(ang);
                for (int i = 0; i < 7; i++) {
                    int c = i < 2 ? 0xffffff : i < 4 ? Pal.ICE_L : Pal.ICE;
                    s.pset((int) Math.round(px - cx * i), (int) Math.round(py - cy * i), c);
                    if (i > 1 && i < 5) {
                        s.pblend((int) Math.round(px - cx * i - cy), (int) Math.round(py - cy * i + cx), Pal.ICE, 150);
                        s.pblend((int) Math.round(px - cx * i + cy), (int) Math.round(py - cy * i - cx), Pal.ICE, 150);
                    }
                }
                s.glow(px, py, 7, 0x8ad0ff, 100);
                break;
            }
            case FALLING_STAR: {
                double d = Math.max(0.1, Math.hypot(vx, vy));
                for (int i = 1; i < 18; i++)
                    s.glow(px - vx / d * i * 2.2, py - vy / d * i * 2.2, 3.5 - i * 0.15, i < 6 ? 0xfff0a0 : 0xffa0d0, 140 - i * 7);
                s.glow(px, py, 12, 0xfff0a0, 160);
                star(s, px, py, 3.5, Pal.STAR, age * 0.15);
                break;
            }
            default: { // scythe
                double a = age * 0.4;
                s.glow(px, py, 10, 0x9a4aff, 110);
                for (int i = 0; i < 10; i++) {
                    double t = a + i * 0.28;
                    double r = 3 + i * 0.35;
                    s.pset((int) Math.round(px + Math.cos(t) * r), (int) Math.round(py + Math.sin(t) * r), i < 5 ? 0xe0c0ff : 0x8a4ae0);
                    s.pset((int) Math.round(px - Math.cos(t) * r), (int) Math.round(py - Math.sin(t) * r), i < 5 ? 0xe0c0ff : 0x8a4ae0);
                }
            }
        }
    }

    /** A small five-pointed star. */
    static void star(Screen s, double cx, double cy, double r, int c, double rot) {
        int x0 = (int) Math.floor(cx - r - 1), x1 = (int) Math.ceil(cx + r + 1);
        int y0 = (int) Math.floor(cy - r - 1), y1 = (int) Math.ceil(cy + r + 1);
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d = Math.hypot(dx, dy);
                double a = Math.atan2(dy, dx) - rot;
                double edge = r * (0.5 + 0.5 * Math.pow(Math.abs(Math.cos(a * 2.5)), 3));
                if (d <= edge) s.pset(x, y, d < r * 0.35 ? 0xffffff : c);
            }
    }
}
