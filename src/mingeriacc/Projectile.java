package mingeriacc;

/** Arrows and other things that fly. */
final class Projectile {
    static final int WOOD_ARROW = 1, STINGER = 2, FIREBALL = 3, SCYTHE = 4, FLAMING_ARROW = 5;

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

    Projectile(int type, double x, double y, double vx, double vy) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        if (type == SCYTHE) life = 240;
        if (type == FIREBALL) life = 300;
        if (type == FIREBALL || type == FLAMING_ARROW) effect = Items.FX_FIRE;
        if (type == STINGER) effect = Items.FX_POISON;
    }

    boolean isArrow() {
        return type == WOOD_ARROW || type == FLAMING_ARROW;
    }

    /** Moves the projectile; returns false when it hits a tile. */
    boolean update(World w) {
        age++;
        if (--life <= 0) return false;
        if (isArrow() && age > 12) vy = Math.min(6, vy + 0.09);
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
            x += vx / steps;
            y += vy / steps;
            if (w.solid((int) Math.floor(x / Tiles.T), (int) Math.floor(y / Tiles.T))) return false;
        }
        if (effect == Items.FX_FIRE && w.liquidAt((int) (x / Tiles.T), (int) (y / Tiles.T)) > 60
                && !w.isLava((int) (x / Tiles.T), (int) (y / Tiles.T))) return false;
        return x > 0 && y > 0 && x < w.w * Tiles.T && y < w.h * Tiles.T;
    }

    /** Hit box test against a body. */
    boolean hits(Body b) {
        if (type == SCYTHE || type == FIREBALL) {
            return x + 3 > b.x && x - 3 < b.x + b.w && y + 3 > b.y && y - 3 < b.y + b.h;
        }
        return b.contains(x, y) || b.contains(x - vx * 0.5, y - vy * 0.5);
    }

    void draw(Screen s, int camX, int camY, int light) {
        double px = x - camX, py = y - camY;
        switch (type) {
            case WOOD_ARROW: case FLAMING_ARROW: {
                Sprite icon = Items.ICON[type == WOOD_ARROW ? Items.WOOD_ARROW : Items.FLAMING_ARROW];
                double ang = Math.atan2(vy, vx);
                s.drawRotated(icon, 10.5, 3.5, px, py, ang + Math.PI / 4, false, type == FLAMING_ARROW ? 0xffffff : light);
                if (type == FLAMING_ARROW) s.glow(px, py, 5, 0xff8a30, 120);
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
}
