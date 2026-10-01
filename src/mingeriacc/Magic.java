package mingeriacc;

import java.util.ArrayList;
import java.util.List;

import mingeriacc.Entities.Particle;

/**
 * Magic: staffs of fire, water, earth and lightning that use mana. Fire,
 * water and earth shoot projectiles (Projectile); lightning strikes at once
 * and jumps from enemy to enemy, drawn as short-lived bolts.
 */
final class Magic {
    private Magic() {}

    static final double LIGHTNING_RANGE = 26 * Tiles.T;

    /** A lightning bolt on screen: a jagged line that fades in a few frames. */
    static final class Bolt {
        final double[] xs, ys;
        int life;
        final int maxLife;

        Bolt(double[] xs, double[] ys, int life) {
            this.xs = xs;
            this.ys = ys;
            this.life = this.maxLife = life;
        }
    }

    /** Whether the player has the mana for a staff (drinks a mana potion if needed). */
    static boolean canCast(Game g, int item) {
        Player p = g.player;
        int cost = Items.MANA[item];
        if (p.mana < cost) g.autoManaPotion();
        if (p.mana < cost) {
            g.hint("Not enough mana");
            return false;
        }
        return true;
    }

    static int damage(Game g, int item) {
        Player p = g.player;
        return (int) Math.round(Items.DAMAGE[item] * (1 + p.damageBonus + p.magicBonus) * (0.9 + g.rnd.nextDouble() * 0.2));
    }

    /** Casts a staff towards the mouse (the mana is checked by canCast). */
    static void cast(Game g, int item) {
        Player p = g.player;
        p.useMana(Items.MANA[item]);
        double a = p.aimAngle;
        double tipX = p.shoulderX() + Math.cos(a) * 11, tipY = p.shoulderY() + Math.sin(a) * 11;
        int proj = Items.PROJ[item];
        if (proj == 0) {
            lightning(g, tipX, tipY, damage(g, item), Items.CRIT[item]);
            return;
        }
        double speed = Items.SHOOT_SPEED[item];
        Projectile pr = new Projectile(proj, tipX, tipY, Math.cos(a) * speed, Math.sin(a) * speed);
        pr.damage = damage(g, item);
        pr.knock = Items.KNOCK[item];
        pr.crit = Items.CRIT[item];
        g.projectiles.add(pr);
        switch (proj) {
            case Projectile.FLAME_BOLT: g.playAt(Audio.ZAP, p.centerX(), 0.4, 0.8 + g.rnd.nextDouble() * 0.1); break;
            case Projectile.WATER_BOLT: g.playAt(Audio.SPLASH, p.centerX(), 0.35, 1.4); g.playAt(Audio.ZAP, p.centerX(), 0.25, 1.1); break;
            default: g.playAt(Audio.ZAP, p.centerX(), 0.35, 0.6); g.playAt(Audio.DIG_HARD, p.centerX(), 0.4, 0.7); break;
        }
        for (int i = 0; i < 5; i++) {
            int c = proj == Projectile.FLAME_BOLT ? Pal.FLAME : proj == Projectile.WATER_BOLT ? Pal.WATER_L : Pal.EMERALD;
            Particle pa = new Particle(tipX, tipY, Math.cos(a) * 0.8 + (g.rnd.nextDouble() - 0.5),
                    Math.sin(a) * 0.8 + (g.rnd.nextDouble() - 0.5), 12 + g.rnd.nextInt(10), c, 0);
            pa.glow = true;
            pa.noCollide = true;
            g.particles.add(pa);
        }
    }

    /** Lightning from (x0, y0) to the enemy nearest the mouse, then on to up to two more. */
    static void lightning(Game g, double x0, double y0, int damage, int crit) {
        World w = g.world;
        Mob target = null;
        double best = 56;
        for (Mob m : g.mobs) {
            if (m.dead || Mobs.isCritter(m.type)) continue;
            double d = Math.hypot(m.centerX() - g.mouseWX, m.centerY() - g.mouseWY);
            if (d < best && Math.hypot(m.centerX() - x0, m.centerY() - y0) < LIGHTNING_RANGE
                    && w.lineClear(x0, y0, m.centerX(), m.centerY())) {
                best = d;
                target = m;
            }
        }
        g.playAt(Audio.THUNDER, x0, 0.4, 1.3 + g.rnd.nextDouble() * 0.3);
        g.playAt(Audio.ZAP, x0, 0.3, 1.6);
        if (target == null) {
            // no enemy: the bolt strikes towards the mouse and stops at a wall
            double a = Math.atan2(g.mouseWY - y0, g.mouseWX - x0);
            double len = Math.min(LIGHTNING_RANGE * 0.6, Math.hypot(g.mouseWX - x0, g.mouseWY - y0));
            double ex = x0, ey = y0;
            for (double t = 0; t < len; t += 2) {
                double nx = x0 + Math.cos(a) * t, ny = y0 + Math.sin(a) * t;
                if (w.solid((int) Math.floor(nx / Tiles.T), (int) Math.floor(ny / Tiles.T))) break;
                ex = nx;
                ey = ny;
            }
            addBolt(g, x0, y0, ex, ey);
            sparks(g, ex, ey);
            return;
        }
        List<Mob> hit = new ArrayList<>();
        Mob cur = target;
        double fx = x0, fy = y0;
        int dmg = damage;
        for (int k = 0; k < 3 && cur != null; k++) {
            addBolt(g, fx, fy, cur.centerX(), cur.centerY());
            sparks(g, cur.centerX(), cur.centerY());
            boolean c = g.rnd.nextInt(100) < crit;
            int dir = cur.centerX() >= fx ? 1 : -1;
            Combat.hurtMob(g, cur, dmg, 2.5, dir, c);
            hit.add(cur);
            fx = cur.centerX();
            fy = cur.centerY();
            Mob next = null;
            double nb = 80;
            for (Mob m : g.mobs) {
                if (m.dead || hit.contains(m) || Mobs.isCritter(m.type)) continue;
                double d = Math.hypot(m.centerX() - fx, m.centerY() - fy);
                if (d < nb && w.lineClear(fx, fy, m.centerX(), m.centerY())) {
                    nb = d;
                    next = m;
                }
            }
            cur = next;
            dmg = (int) Math.round(dmg * 0.75);
        }
    }

    private static void addBolt(Game g, double x0, double y0, double x1, double y1) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int n = Math.max(2, (int) (len / 6));
        double[] xs = new double[n + 1], ys = new double[n + 1];
        double nx = -(y1 - y0) / Math.max(1, len), ny = (x1 - x0) / Math.max(1, len);
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double off = i == 0 || i == n ? 0 : (g.rnd.nextDouble() - 0.5) * 7;
            xs[i] = x0 + (x1 - x0) * t + nx * off;
            ys[i] = y0 + (y1 - y0) * t + ny * off;
        }
        g.bolts.add(new Bolt(xs, ys, 10));
    }

    private static void sparks(Game g, double x, double y) {
        for (int i = 0; i < 8; i++) {
            double a = g.rnd.nextDouble() * Math.PI * 2, v = 0.5 + g.rnd.nextDouble() * 1.5;
            Particle p = new Particle(x, y, Math.cos(a) * v, Math.sin(a) * v, 10 + g.rnd.nextInt(12),
                    g.rnd.nextBoolean() ? 0xffffff : 0xa8d0ff, 0.05);
            p.glow = true;
            g.particles.add(p);
        }
    }

    /** A boulder breaks into shards that fly on. */
    static void shatter(Game g, Projectile rock) {
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + (g.rnd.nextDouble() - 0.5) * 2.6;
            double v = 1.6 + g.rnd.nextDouble() * 1.4;
            Projectile s = new Projectile(Projectile.SHARD, rock.x - rock.vx, rock.y - rock.vy,
                    Math.cos(a) * v + rock.vx * 0.2, Math.sin(a) * v);
            s.damage = Math.max(1, rock.damage * 2 / 5);
            s.knock = 2;
            s.crit = rock.crit;
            g.projectiles.add(s);
        }
        g.burst(rock.x, rock.y, 0x7a6a5a, 10, 1.6);
        g.playAt(Audio.BREAK, rock.x, 0.5, 0.8);
    }

    /** Updates the lightning bolts on screen. */
    static void update(Game g) {
        g.bolts.removeIf(b -> --b.life <= 0);
    }
}
