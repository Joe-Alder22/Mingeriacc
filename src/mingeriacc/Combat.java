package mingeriacc;

import java.util.Iterator;

import mingeriacc.Entities.Particle;

/** Damage, knockback, death and respawning. */
final class Combat {
    private Combat() {}

    static final int RESPAWN_TIME = 60 * 5;

    // ---- the player ---------------------------------------------------------

    static void hurtPlayer(Game g, int damage, int hitDir, String source) {
        Player p = g.player;
        if (p.dead || p.immune > 0) return;
        int d = Math.max(1, damage - p.defense / 2);
        p.life -= d;
        p.sinceHurt = 0;
        p.immune = 40;
        p.vx = hitDir * 2.3;
        p.vy = -1.9;
        g.number(p.centerX(), p.y - 4, d, Pal.UI_HURT, false);
        g.playAt(Audio.HURT, p.centerX(), 0.7, 0.95 + g.rnd.nextDouble() * 0.1);
        g.burst(p.centerX(), p.centerY(), 0xa01818, 6, 1.2);
        if (p.life <= 0) killPlayer(g, source);
    }

    /** Damage over time to the player (burning, poison, drowning). */
    static void dotPlayer(Game g, int damage, String cause) {
        Player p = g.player;
        if (p.dead) return;
        p.life -= damage;
        p.sinceHurt = 0;
        if (p.life <= 0) {
            killPlayer(g, null);
            g.deathText = "You " + cause + "...";
        }
    }

    static void killPlayer(Game g, String source) {
        Player p = g.player;
        p.life = 0;
        p.dead = true;
        p.respawnTimer = RESPAWN_TIME;
        p.useTimer = 0;
        g.returnCursorItem();
        g.inventoryOpen = false;
        // drop half of the coins
        for (int coin : Inventory.COINS) {
            int n = g.inv.countOf(coin);
            int half = n / 2;
            if (half > 0) {
                g.inv.remove(coin, half);
                Entities.Drop d = new Entities.Drop(p.centerX() - 3, p.centerY(), coin, half);
                d.vx = (g.rnd.nextDouble() - 0.5) * 3;
                d.vy = -2 - g.rnd.nextDouble() * 2;
                d.pickupDelay = 60;
                g.drops.add(d);
            }
        }
        g.burst(p.centerX(), p.centerY(), 0xa01818, 30, 2.5);
        g.burst(p.centerX(), p.centerY(), p.look.shirt, 10, 2.0);
        g.playAt(Audio.DEATH, p.centerX(), 0.8, 1.0);
        String how = source == null ? "You were slain..." : "You were slain by a " + source + "...";
        g.deathText = how;
        g.message(how, Pal.UI_BAD);
    }

    static void updateDead(Game g) {
        Player p = g.player;
        if (--p.respawnTimer <= 0) {
            p.dead = false;
            p.life = p.lifeMax;
            p.spawnAt(g.world);
            p.immune = 120;
            g.deathText = null;
            g.snapCamera();
        }
    }

    // ---- creatures ------------------------------------------------------------

    /** Damages a creature; returns true if it died. */
    static boolean hurtMob(Game g, Mob m, int damage, double knock, int dir, boolean crit) {
        if (m.dead) return false;
        int d = Math.max(1, damage - m.defense / 2);
        if (crit) d *= 2;
        m.life -= d;
        m.hitFlash = 8;
        m.hurtTimer = 0;
        double k = knock * m.kbRes * (crit ? 1.4 : 1);
        if (k > 0) {
            int ai = Mobs.AI[m.type];
            if (ai == Mobs.AI_FLYER || ai == Mobs.AI_BAT) {
                m.vx = dir * k * 0.6;
                m.vy = -k * 0.15;
            } else {
                m.vx = dir * k * 0.5;
                m.vy = -Math.min(3.5, k * 0.45);
                m.onGround = false;
            }
        }
        g.number(m.centerX(), m.y - 4, d, crit ? Pal.UI_CRIT : Pal.UI_DAMAGE, crit);
        boolean slime = Mobs.isSlime(m.type);
        int sound = slime ? Audio.SQUISH : m.type == Mobs.SKELETON ? Audio.BONE : Audio.HIT;
        g.playAt(sound, m.centerX(), 0.6, 0.9 + g.rnd.nextDouble() * 0.2);
        g.burst(m.centerX(), m.centerY(), bloodColor(m), 4, 1.2);
        if (m.life <= 0) {
            killMob(g, m);
            return true;
        }
        return false;
    }

    /** Damage over time (burning, poison): ignores defence, no knockback. */
    static void dot(Game g, Mob m, int damage) {
        if (m.dead) return;
        m.life -= damage;
        g.number(m.centerX(), m.y - 2, damage, 0xc8a060, false);
        if (m.life <= 0) {
            if (Mobs.isTown(m.type)) Town.died(g, m);
            else killMob(g, m);
        }
    }

    /** Puts a burning or poison effect on a creature. */
    static void applyEffect(Game g, Mob m, int fx) {
        if (fx == Items.FX_FIRE && !Mobs.LAVA_PROOF[m.type]) m.onFire = Math.max(m.onFire, 240);
        else if (fx == Items.FX_POISON && g.rnd.nextInt(3) == 0) m.poisoned = Math.max(m.poisoned, 360);
    }

    /** Puts a burning or poison effect on the player. */
    static void applyEffect(Player p, int fx) {
        if (fx == Items.FX_FIRE && !p.fireImmune) p.onFire = Math.max(p.onFire, 300);
        else if (fx == Items.FX_POISON) p.poisoned = Math.max(p.poisoned, 420);
    }

    static int bloodColor(Mob m) {
        if (Mobs.isSlime(m.type)) return Mobs.COLOR[m.type];
        if (m.type == Mobs.SKELETON) return 0xd8d0b8;
        return 0x8a1a1a;
    }

    static void killMob(Game g, Mob m) {
        m.dead = true;
        Mobs.dropLoot(m, g);
        g.burst(m.centerX(), m.centerY(), bloodColor(m), 16, 2.0);
        if (m.type == Mobs.ZOMBIE) g.burst(m.centerX(), m.centerY(), 0x3e5a86, 6, 1.6);
        boolean slime = Mobs.isSlime(m.type);
        g.playAt(slime ? Audio.SLIME_DIE : m.type == Mobs.SKELETON ? Audio.BONE : Audio.KILL, m.centerX(), 0.7, 1.0);
    }

    // ---- melee --------------------------------------------------------------

    /** Checks the swung or stabbed item against creatures. */
    static void melee(Game g) {
        Player p = g.player;
        if (!p.using() || p.useItem == 0) return;
        int item = p.useItem;
        int style = p.useStyle;
        if (Items.DAMAGE[item] <= 0 || (style != Items.S_SWING && style != Items.S_STAB)) return;
        double[] pts = new double[18];
        int n = 0;
        if (style == Items.S_SWING) {
            Sprite icon = Items.ICON[item];
            double len = Math.hypot(icon.w, icon.h) - 3;
            double a1 = p.swingAngle();
            double a0 = p.useDuration > 0 ? 3.6 - (1.0 - (p.useTimer + 1) / (double) p.useDuration) * 3.3 : a1;
            for (int k = 0; k < 3; k++) {
                double a = a0 + (a1 - a0) * k / 2.0;
                double dx = Math.sin(a) * p.dir, dy = Math.cos(a);
                double hx = p.shoulderX() + dx * 5, hy = p.shoulderY() + dy * 5;
                for (double f : new double[]{0.35, 0.7, 1.0}) {
                    pts[n++] = hx + dx * len * f;
                    pts[n++] = hy + dy * len * f;
                }
            }
        } else {
            double reach = 2 + p.stabReach() * 5;
            double cx = Math.cos(p.aimAngle), cy = Math.sin(p.aimAngle);
            for (double f : new double[]{3, 6, 9, 11}) {
                pts[n++] = p.shoulderX() + cx * (reach + f);
                pts[n++] = p.shoulderY() + cy * (reach + f);
            }
        }
        for (Mob m : g.mobs) {
            if (m.dead || m.lastSwing == p.swingId) continue;
            for (int i = 0; i < n; i += 2) {
                double px = pts[i], py = pts[i + 1];
                if (px >= m.x - 2 && px < m.x + m.w + 2 && py >= m.y - 2 && py < m.y + m.h + 2) {
                    m.lastSwing = p.swingId;
                    boolean crit = g.rnd.nextInt(100) < Items.CRIT[item];
                    int dmg = (int) Math.round(Items.DAMAGE[item] * (1 + p.damageBonus) * (0.85 + g.rnd.nextDouble() * 0.3));
                    int dir = m.centerX() >= p.centerX() ? 1 : -1;
                    if (!hurtMob(g, m, dmg, Items.KNOCK[item], dir, crit)) applyEffect(g, m, Items.EFFECT[item]);
                    break;
                }
            }
        }
    }

    /** Creatures hurt the player on touch. */
    static void contact(Game g) {
        Player p = g.player;
        // town folk get hurt by enemies too
        for (Mob t : g.town) {
            if (t.dead || t.immune > 0) continue;
            for (Mob m : g.mobs) {
                if (m.dead || m.damage <= 0 || !m.intersects(t)) continue;
                int d = Math.max(1, m.damage - t.defense / 2);
                t.life -= d;
                t.immune = 30;
                t.hitFlash = 8;
                t.vx = (t.centerX() >= m.centerX() ? 1 : -1) * 1.5;
                t.vy = -1.5;
                g.number(t.centerX(), t.y - 4, d, Pal.UI_HURT, false);
                if (t.life <= 0) Town.died(g, t);
                break;
            }
        }
        if (p.dead || p.immune > 0) return;
        for (Mob m : g.mobs) {
            if (m.dead || m.damage <= 0) continue;
            if (m.x < p.x + p.w - 1 && m.x + m.w > p.x + 1 && m.y < p.y + p.h - 1 && m.y + m.h > p.y + 1) {
                int dir = p.centerX() >= m.centerX() ? 1 : -1;
                hurtPlayer(g, m.damage, dir, Mobs.NAME[m.type]);
                applyEffect(p, Mobs.HIT_FX[m.type]);
                return;
            }
        }
    }

    // ---- projectiles ---------------------------------------------------------

    static void projectiles(Game g) {
        for (Iterator<Projectile> it = g.projectiles.iterator(); it.hasNext(); ) {
            Projectile pr = it.next();
            if (!pr.update(g.world)) {
                g.dust(pr.x, pr.y, pr.isArrow() ? Pal.WOOD : pr.type == Projectile.FIREBALL ? Pal.LAVA : 0x8ae04a, 3);
                if (pr.type == Projectile.WOOD_ARROW && g.rnd.nextInt(3) == 0) {
                    Entities.Drop d = new Entities.Drop(pr.x - pr.vx - 3, pr.y - pr.vy - 3, Items.WOOD_ARROW, 1);
                    d.pickupDelay = 20;
                    g.drops.add(d);
                }
                g.playAt(Audio.ARROW_HIT, pr.x, 0.35, 1.0);
                it.remove();
                continue;
            }
            if (!pr.hostile) {
                for (Mob m : g.mobs) {
                    if (m.dead || !pr.hits(m)) continue;
                    boolean crit = g.rnd.nextInt(100) < pr.crit;
                    if (!hurtMob(g, m, pr.damage, pr.knock, pr.vx >= 0 ? 1 : -1, crit)) applyEffect(g, m, pr.effect);
                    if (--pr.pierce <= 0) {
                        pr.dead = true;
                        break;
                    }
                }
            } else if (!g.player.dead && g.player.immune == 0 && pr.hits(g.player)) {
                hurtPlayer(g, pr.damage, pr.vx >= 0 ? 1 : -1, null);
                applyEffect(g.player, pr.effect);
                pr.dead = true;
            }
            if (pr.dead) it.remove();
        }
    }

    /** Small puffs used by several effects. */
    static Particle puff(double x, double y, int color, double speed, java.util.Random r) {
        Particle p = new Particle(x, y, (r.nextDouble() - 0.5) * speed * 2, (r.nextDouble() - 0.8) * speed * 2,
                20 + r.nextInt(20), color, 0.12);
        return p;
    }
}
