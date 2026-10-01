package mingeriacc;

/**
 * Bosses. The Eye of Cthulhu hovers above the player calling servants, then
 * charges three times; at half life it spins, tears its eye open into a mouth
 * and charges faster and more often. It flies away at dawn or when the player
 * dies.
 *
 * State in Mob.ai: 0 = state, 1 = state timer, 2 = counter (charges or
 * servants), 3 = phase (0 eye, 1 mouth), 4 = spin speed.
 */
final class Boss {
    private Boss() {}

    private static final int HOVER = 0, CHARGE = 1, TRANSFORM = 2, LEAVE = 3;

    /** The boss being fought (the first one alive), or null. */
    static Mob active(Game g) {
        for (Mob m : g.mobs) if (!m.dead && Mobs.BOSS[m.type]) return m;
        return null;
    }

    static boolean alive(Game g, int type) {
        for (Mob m : g.mobs) if (!m.dead && m.type == type) return true;
        return false;
    }

    /** Restores what a saved boss had changed about itself (the Eye's second phase). */
    static void afterLoad(Mob m) {
        if (m.type == Mobs.EYE_OF_CTHULHU && m.ai[3] > 0) {
            m.defense = 0;
            m.damage = 23;
        }
    }

    // ---- summoning ------------------------------------------------------------

    /** Uses a Suspicious Looking Eye; returns false if nothing happened. */
    static boolean summonEye(Game g) {
        if (!g.isNight()) {
            g.hint("Nothing happens... It only works at night");
            return false;
        }
        if (alive(g, Mobs.EYE_OF_CTHULHU)) {
            g.hint("The Eye of Cthulhu is already here");
            return false;
        }
        spawnEye(g);
        return true;
    }

    static void spawnEye(Game g) {
        Player p = g.player;
        double x = p.centerX() + (g.rnd.nextBoolean() ? 1 : -1) * (60 + g.rnd.nextInt(120));
        double y = p.centerY() - g.viewH / 2.0 - 70;
        Mob m = new Mob(Mobs.EYE_OF_CTHULHU, x, y + Mobs.H[Mobs.EYE_OF_CTHULHU] / 2.0);
        m.rot = Math.PI / 2;
        g.mobs.add(m);
        g.message(Mobs.NAME[m.type] + " has awoken!", Pal.UI_BOSS);
        g.playAt(Audio.ROAR, p.centerX(), 0.8, 1.0);
    }

    // ---- the Eye of Cthulhu ------------------------------------------------------

    static void eye(Mob m, Game g) {
        Player p = g.player;
        int state = (int) m.ai[0];
        boolean mouth = m.ai[3] > 0;
        double dx = p.centerX() - m.centerX(), dy = p.centerY() - m.centerY();
        double toPlayer = Math.atan2(dy, dx);
        m.ai[1]++;

        if (state != LEAVE && (!g.isNight() || p.dead)) {
            state = LEAVE;
            m.ai[0] = LEAVE;
            m.ai[1] = 0;
        }
        switch (state) {
            case HOVER: {
                // hover above the player, eye turned towards it
                double tx = p.centerX(), ty = p.centerY() - 95;
                double hx = tx - m.centerX(), hy = ty - m.centerY();
                double d = Math.max(1, Math.hypot(hx, hy));
                double max = mouth ? 3.6 : 3.0, acc = mouth ? 0.11 : 0.08;
                m.vx += hx / d * acc;
                m.vy += hy / d * acc;
                double v = Math.hypot(m.vx, m.vy);
                if (v > max) {
                    m.vx *= max / v;
                    m.vy *= max / v;
                }
                m.rot = turn(m.rot, toPlayer, 0.12);
                int hoverTime = mouth ? 80 : 210;
                if (!mouth && m.ai[1] > 40 && m.ai[1] % 45 == 0 && m.ai[2] < 4 && servants(g) < 8) {
                    spawnServant(m, g);
                    m.ai[2]++;
                }
                if (m.ai[1] > hoverTime) {
                    m.ai[0] = CHARGE;
                    m.ai[1] = 0;
                    m.ai[2] = 0;
                    charge(m, g, mouth);
                }
                break;
            }
            case CHARGE: {
                boolean enraged = m.life < m.lifeMax / 4;
                int dashTime = mouth ? 40 : 46, restTime = enraged ? 12 : mouth ? 18 : 26;
                if (m.ai[1] <= dashTime) {
                    m.vx *= 0.997;
                    m.vy *= 0.997;
                    m.rot = Math.atan2(m.vy, m.vx);
                } else {
                    m.vx *= 0.91;
                    m.vy *= 0.91;
                    m.rot = turn(m.rot, toPlayer, 0.15);
                }
                if (m.ai[1] > dashTime + restTime) {
                    m.ai[2]++;
                    int charges = mouth ? (enraged ? 5 : 4) : 3;
                    if (m.ai[2] >= charges) {
                        m.ai[0] = HOVER;
                        m.ai[1] = 0;
                        m.ai[2] = 0;
                    } else {
                        m.ai[1] = 0;
                        charge(m, g, mouth);
                    }
                }
                break;
            }
            case TRANSFORM: {
                // spin faster and faster; halfway the eye tears open into a mouth
                m.vx *= 0.94;
                m.vy *= 0.94;
                double t = m.ai[1] / 150.0;
                m.ai[4] = Math.sin(Math.min(1, t) * Math.PI) * 0.45;
                m.rot += m.ai[4];
                if (m.ai[1] == 75) {
                    m.ai[3] = 1;
                    m.defense = 0;
                    m.damage = 23;
                    g.burst(m.centerX(), m.centerY(), 0x8a1a1a, 40, 3.0);
                    g.burst(m.centerX(), m.centerY(), 0xece4e0, 16, 2.5);
                    g.burst(m.centerX(), m.centerY(), 0x3a6ab0, 10, 2.0);
                    g.playAt(Audio.ROAR, m.centerX(), 0.8, 0.85);
                } else if (g.rnd.nextInt(3) == 0) {
                    g.burst(m.centerX(), m.centerY(), 0x8a1a1a, 2, 1.5);
                }
                if (m.ai[1] >= 150) {
                    m.ai[0] = HOVER;
                    m.ai[1] = 0;
                    m.ai[2] = 0;
                }
                break;
            }
            default: {
                // fly away
                m.vy = Math.max(-6, m.vy - 0.12);
                m.vx *= 0.98;
                m.rot = turn(m.rot, -Math.PI / 2, 0.1);
                if (m.centerY() < g.camY - 400 || m.ai[1] > 600) {
                    m.dead = true;
                    g.message(Mobs.NAME[m.type] + " has left...", Pal.UI_BOSS);
                }
                break;
            }
        }
        // at half life it changes
        if (!mouth && state != TRANSFORM && state != LEAVE && m.life < m.lifeMax / 2) {
            m.ai[0] = TRANSFORM;
            m.ai[1] = 0;
            g.playAt(Audio.ROAR, m.centerX(), 0.6, 1.2);
        }
        m.move(g.world);
        m.dir = Math.cos(m.rot) >= 0 ? 1 : -1;
        if (m.ai[3] > 0 && g.rnd.nextInt(5) == 0) g.dust(m.centerX(), m.centerY() + 4, 0xa01818, 1);
    }

    private static void charge(Mob m, Game g, boolean mouth) {
        Player p = g.player;
        boolean enraged = m.life < m.lifeMax / 4;
        double speed = mouth ? (enraged ? 4.6 : 4.0) : 3.4;
        // aim a little ahead of a running player
        double tx = p.centerX() + p.vx * 12, ty = p.centerY() + p.vy * 6;
        double a = Math.atan2(ty - m.centerY(), tx - m.centerX());
        m.vx = Math.cos(a) * speed;
        m.vy = Math.sin(a) * speed;
        m.rot = a;
        g.playAt(Audio.ROAR, m.centerX(), 0.35, mouth ? 1.5 : 1.8);
    }

    private static int servants(Game g) {
        int n = 0;
        for (Mob m : g.mobs) if (!m.dead && m.type == Mobs.SERVANT) n++;
        return n;
    }

    private static void spawnServant(Mob m, Game g) {
        double a = m.rot;
        Mob s = new Mob(Mobs.SERVANT, m.centerX() + Math.cos(a) * 18, m.centerY() + Math.sin(a) * 18 + 4);
        s.vx = Math.cos(a) * 2.2 + m.vx * 0.5;
        s.vy = Math.sin(a) * 2.2 + m.vy * 0.5;
        s.rot = a;
        g.mobs.add(s);
        g.burst(s.centerX(), s.centerY(), 0x8a1a1a, 6, 1.2);
        g.playAt(Audio.SQUISH, m.centerX(), 0.5, 0.7);
    }

    /** Turns an angle towards a target by at most step radians. */
    private static double turn(double a, double target, double step) {
        double d = Math.atan2(Math.sin(target - a), Math.cos(target - a));
        return a + Math.max(-step, Math.min(step, d));
    }

    static void eyeLoot(Mob m, Game g) {
        int tx = (int) (m.centerX() / Tiles.T), ty = (int) (m.centerY() / Tiles.T);
        int ore = 30 + g.rnd.nextInt(58);
        while (ore > 0) {
            int n = Math.min(ore, 20);
            g.spawnDrop(tx, ty, Items.DEMONITE_ORE, n);
            ore -= n;
        }
        g.spawnDrop(tx, ty, Items.UNHOLY_ARROW, 20 + g.rnd.nextInt(30));
        g.spawnDrop(tx, ty, Items.HEALING_POTION, 5 + g.rnd.nextInt(6));
        g.spawnDrop(tx, ty, Items.EYE_SHIELD, 1);
        for (int i = 0; i < 6; i++) g.spawnDrop(tx, ty, Items.HEART, 1);
        g.burst(m.centerX(), m.centerY(), 0x8a1a1a, 60, 3.5);
        g.burst(m.centerX(), m.centerY(), 0xece4e0, 20, 3.0);
        g.playAt(Audio.ROAR, m.centerX(), 0.7, 1.4);
        g.message(Mobs.NAME[m.type] + " has been defeated!", Pal.UI_BOSS);
        if (!g.world.downedEye) g.message("With the Eye gone, a dryad may come to town.", Pal.UI_GOOD);
        g.world.downedEye = true;
    }
}
