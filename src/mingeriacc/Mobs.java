package mingeriacc;

import java.util.Random;

/** Creature definitions, behaviour (AI), spawning and loot. */
final class Mobs {
    private Mobs() {}

    static final int GREEN_SLIME = 1, BLUE_SLIME = 2, RED_SLIME = 3, PURPLE_SLIME = 4, YELLOW_SLIME = 5,
            BLACK_SLIME = 6, PINKY = 7, ZOMBIE = 8, DEMON_EYE = 9, CAVE_BAT = 10, SKELETON = 11,
            JUNGLE_SLIME = 12, HORNET = 13, EATER = 14, LAVA_SLIME = 15, FIRE_IMP = 16, DEMON = 17,
            HELLBAT = 18, BUNNY = 19, GUIDE = 20, MERCHANT = 21, NURSE = 22;
    static final int COUNT = 23;

    static final int AI_SLIME = 0, AI_FIGHTER = 1, AI_FLYER = 2, AI_BAT = 3, AI_HORNET = 4, AI_CASTER = 5,
            AI_CRITTER = 6, AI_TOWN = 7;

    static final String[] NAME = new String[COUNT];
    static final int[] W = new int[COUNT], H = new int[COUNT];
    static final int[] LIFE = new int[COUNT], DAMAGE = new int[COUNT], DEFENSE = new int[COUNT];
    static final double[] KB_RES = new double[COUNT];
    static final int[] VALUE = new int[COUNT];     // coins dropped, in copper
    static final int[] AI = new int[COUNT];
    static final int[] COLOR = new int[COUNT];
    static final boolean[] NIGHT = new boolean[COUNT];      // leaves when the sun rises
    static final boolean[] LAVA_PROOF = new boolean[COUNT];
    static final int[] HIT_FX = new int[COUNT];             // debuff given to the player on touch
    static final double[] SPEED = new double[COUNT];        // flyer speed

    private static void def(int id, String name, int w, int h, int life, int dmg, int def, double kb, int value, int ai) {
        NAME[id] = name;
        W[id] = w;
        H[id] = h;
        LIFE[id] = life;
        DAMAGE[id] = dmg;
        DEFENSE[id] = def;
        KB_RES[id] = kb;
        VALUE[id] = value;
        AI[id] = ai;
        SPEED[id] = 1;
    }

    static {
        def(GREEN_SLIME, "Green Slime", 14, 10, 14, 6, 0, 1.0, 25, AI_SLIME);
        def(BLUE_SLIME, "Blue Slime", 14, 10, 25, 7, 2, 1.0, 25, AI_SLIME);
        def(RED_SLIME, "Red Slime", 14, 10, 35, 12, 4, 1.0, 50, AI_SLIME);
        def(PURPLE_SLIME, "Purple Slime", 15, 11, 40, 12, 6, 0.9, 60, AI_SLIME);
        def(YELLOW_SLIME, "Yellow Slime", 15, 11, 45, 15, 7, 1.0, 80, AI_SLIME);
        def(BLACK_SLIME, "Black Slime", 15, 11, 45, 15, 8, 1.0, 80, AI_SLIME);
        def(PINKY, "Pinky", 8, 6, 150, 5, 5, 0.4, 500, AI_SLIME);
        def(ZOMBIE, "Zombie", 10, 22, 45, 14, 6, 0.5, 60, AI_FIGHTER);
        def(DEMON_EYE, "Demon Eye", 14, 12, 60, 18, 2, 0.8, 75, AI_FLYER);
        def(CAVE_BAT, "Cave Bat", 12, 8, 16, 13, 2, 0.8, 90, AI_BAT);
        def(SKELETON, "Skeleton", 10, 22, 60, 20, 8, 0.5, 130, AI_FIGHTER);
        def(JUNGLE_SLIME, "Jungle Slime", 15, 11, 60, 18, 6, 0.9, 100, AI_SLIME);
        def(HORNET, "Hornet", 14, 12, 44, 26, 12, 0.5, 200, AI_HORNET);
        def(EATER, "Eater of Souls", 14, 14, 40, 22, 8, 0.8, 90, AI_FLYER);
        def(LAVA_SLIME, "Lava Slime", 15, 11, 50, 15, 10, 0.9, 250, AI_SLIME);
        def(FIRE_IMP, "Fire Imp", 10, 22, 70, 20, 16, 0.5, 300, AI_CASTER);
        def(DEMON, "Demon", 22, 18, 120, 32, 8, 0.2, 300, AI_FLYER);
        def(HELLBAT, "Hellbat", 12, 8, 35, 21, 8, 0.8, 100, AI_BAT);
        def(BUNNY, "Bunny", 10, 8, 5, 0, 0, 1.0, 0, AI_CRITTER);
        def(GUIDE, "Guide", 10, 22, 250, 0, 15, 0.5, 0, AI_TOWN);
        def(MERCHANT, "Merchant", 10, 22, 250, 0, 15, 0.5, 0, AI_TOWN);
        def(NURSE, "Nurse", 10, 22, 250, 0, 15, 0.5, 0, AI_TOWN);
        COLOR[GREEN_SLIME] = 0x5cc84a;
        COLOR[BLUE_SLIME] = 0x4a86ea;
        COLOR[RED_SLIME] = 0xe04848;
        COLOR[PURPLE_SLIME] = 0xa060e0;
        COLOR[YELLOW_SLIME] = 0xf0d040;
        COLOR[BLACK_SLIME] = 0x3a3a4c;
        COLOR[PINKY] = 0xf08ad0;
        COLOR[JUNGLE_SLIME] = 0x4a9a2a;
        COLOR[LAVA_SLIME] = 0xff7a20;
        NIGHT[ZOMBIE] = true;
        NIGHT[DEMON_EYE] = true;
        for (int t : new int[]{LAVA_SLIME, FIRE_IMP, DEMON, HELLBAT}) LAVA_PROOF[t] = true;
        HIT_FX[LAVA_SLIME] = Items.FX_FIRE;
        HIT_FX[HELLBAT] = Items.FX_FIRE;
        HIT_FX[HORNET] = Items.FX_POISON;
        SPEED[EATER] = 1.25;
        SPEED[DEMON] = 0.7;
        SPEED[HELLBAT] = 1.4;
    }

    static boolean isSlime(int type) {
        return AI[type] == AI_SLIME;
    }

    static boolean isTown(int type) {
        return AI[type] == AI_TOWN;
    }

    static boolean isCritter(int type) {
        return AI[type] == AI_CRITTER;
    }

    // ---- behaviour ----------------------------------------------------------

    static void update(Mob m, Game g) {
        m.anim += 1;
        if (m.hitFlash > 0) m.hitFlash--;
        if (m.immune > 0) m.immune--;
        m.hurtTimer++;
        switch (AI[m.type]) {
            case AI_SLIME: slime(m, g); break;
            case AI_FIGHTER: fighter(m, g); break;
            case AI_FLYER: flyer(m, g); break;
            case AI_HORNET: hornet(m, g); break;
            case AI_CASTER: caster(m, g); break;
            case AI_CRITTER: critter(m, g); break;
            case AI_TOWN: Town.walk(m, g); break;
            default: bat(m, g); break;
        }
        debuffs(m, g);
    }

    /** Burning, poison and lava. */
    private static void debuffs(Mob m, Game g) {
        World w = g.world;
        int tx = (int) (m.centerX() / Tiles.T), ty = (int) ((m.y + m.h - 2) / Tiles.T);
        if (!LAVA_PROOF[m.type] && w.isLava(tx, ty) && w.liquidAt(tx, ty) > 60) {
            m.onFire = 420;
            if (m.immune == 0) {
                m.immune = 30;
                Combat.hurtMob(g, m, 50, 0, 0, false);
            }
        }
        if (m.onFire > 0) {
            m.onFire--;
            if (w.liquidAt(tx, ty) > 60 && !w.isLava(tx, ty)) m.onFire = 0;
            if (g.ticks % 15 == 0) Combat.dot(g, m, 2);
            if (g.rnd.nextInt(3) == 0) g.flame(m.x + g.rnd.nextDouble() * m.w, m.y + g.rnd.nextDouble() * m.h);
        }
        if (m.poisoned > 0) {
            m.poisoned--;
            if (g.ticks % 20 == 0) Combat.dot(g, m, 2);
            if (g.rnd.nextInt(6) == 0) g.dust(m.x + g.rnd.nextDouble() * m.w, m.y + g.rnd.nextDouble() * m.h, 0x8ae04a, 1);
        }
    }

    /** Whether the creature should run away from the player (night creatures in daylight). */
    private static boolean fleeing(Mob m, Game g) {
        return NIGHT[m.type] && !g.isNight() && m.centerY() / Tiles.T < g.world.surfaceLevel;
    }

    private static void gravity(Mob m, Game g) {
        int tx = (int) (m.centerX() / Tiles.T), ty = (int) (m.centerY() / Tiles.T);
        boolean wet = g.world.liquidAt(tx, ty) > 60;
        m.vy = wet ? Math.min(1.5, m.vy + 0.08) : Math.min(5, m.vy + 0.2);
        if (wet) m.vx *= 0.95;
    }

    private static void slime(Mob m, Game g) {
        Player p = g.player;
        double dx = p.centerX() - m.centerX();
        boolean target = !p.dead && Math.abs(dx) < 30 * Tiles.T && Math.abs(p.centerY() - m.centerY()) < 20 * Tiles.T;
        if (m.onGround) {
            m.vx *= 0.8;
            if (Math.abs(m.vx) < 0.05) m.vx = 0;
            if (target) m.dir = dx >= 0 ? 1 : -1;
            else if (g.rnd.nextInt(400) == 0) m.dir = -m.dir;
            m.ai[0] += (m.hurtTimer < 60 ? 3 : 1) * (target ? 1 : 0.6);
            if (m.ai[0] >= 80) {
                m.ai[1]++;
                boolean big = ((int) m.ai[1]) % 3 == 0;
                m.vy = big ? -4.0 : -2.7;
                m.vx = m.dir * (big ? 1.05 : 1.3) * (m.type == PINKY ? 1.2 : 1);
                m.ai[0] = -g.rnd.nextInt(40);
                m.onGround = false;
            }
        } else if (m.hitWall) {
            m.vx *= 0.5;
        }
        gravity(m, g);
        m.move(g.world);
    }

    private static void fighter(Mob m, Game g) {
        Player p = g.player;
        double dx = p.centerX() - m.centerX();
        boolean flee = fleeing(m, g) || p.dead;
        int want = dx >= 0 ? 1 : -1;
        if (flee) want = -want;
        if (m.ai[2] > 0) {
            m.ai[2]--; // turned around after getting stuck
        } else if (Math.abs(dx) > 6 || flee) {
            m.dir = want;
        }
        double max = m.type == SKELETON ? 0.62 : 0.5;
        double acc = m.onGround ? 0.05 : 0.02;
        if (m.dir > 0 ? m.vx < max : m.vx > -max) m.vx += m.dir * acc;
        else if (m.onGround) m.vx *= 0.9;
        boolean blocked = m.hitWall && m.onGround;
        if (blocked) {
            m.ai[1]++;
            m.vy = -3.3;
            m.onGround = false;
            if (m.ai[1] > 4) {
                m.dir = -m.dir;
                m.ai[2] = 120;
                m.ai[1] = 0;
            }
        } else if (m.onGround && Math.abs(m.vx) > 0.2) {
            m.ai[1] = Math.max(0, m.ai[1] - 0.02);
        }
        // hop up to a player standing higher up
        if (m.onGround && !flee && Math.abs(dx) < 3 * Tiles.T && p.centerY() < m.y - 6 && g.rnd.nextInt(40) == 0) {
            m.vy = -3.6;
        }
        gravity(m, g);
        m.move(g.world);
        m.anim = m.onGround ? m.anim + Math.abs(m.vx) * 0.13 : m.anim;
    }

    private static void flyer(Mob m, Game g) {
        Player p = g.player;
        m.usePlatforms = false;
        double tx = p.centerX(), ty = p.centerY();
        if (fleeing(m, g) || p.dead) {
            tx = m.centerX() + (m.centerX() < p.centerX() ? -400 : 400);
            ty = m.centerY() - 300;
        }
        double dx = tx - m.centerX(), dy = ty - m.centerY();
        double sp = SPEED[m.type];
        double acc = 0.045 * sp;
        if (m.type == EATER) {
            // eaters wobble as they fly
            dx += Math.sin(m.anim * 0.05) * 40;
            dy += Math.cos(m.anim * 0.07) * 30;
        }
        m.vx += dx > 0 ? acc : -acc;
        m.vy += dy > 0 ? acc * 0.6 : -acc * 0.6;
        m.vx = Math.max(-2.0 * sp, Math.min(2.0 * sp, m.vx));
        m.vy = Math.max(-1.2 * sp, Math.min(1.2 * sp, m.vy));
        if (m.type == DEMON) {
            // demons keep their distance and throw scythes
            m.noClip = true;
            if (Math.hypot(dx, dy) < 60) m.vx *= 0.9;
            if (!p.dead && ++m.attackTimer > 150 && Math.abs(dx) < 300) {
                m.attackTimer = 0;
                for (int k = -1; k <= 1; k++) {
                    double a = Math.atan2(dy, dx) + k * 0.25;
                    g.shootHostile(Projectile.SCYTHE, m.centerX(), m.centerY(), Math.cos(a) * 1.6, Math.sin(a) * 1.6, 21);
                }
                g.playAt(Audio.SPELL, m.centerX(), 0.5, 0.8);
            }
        }
        double pvx = m.vx, pvy = m.vy;
        m.move(g.world);
        if (m.hitWall) m.vx = -pvx * 0.6;
        if (m.onGround || m.hitCeiling) m.vy = -pvy * 0.6;
        m.dir = m.vx >= 0 ? 1 : -1;
        m.rot = Math.atan2(m.vy, m.vx);
    }

    private static void hornet(Mob m, Game g) {
        Player p = g.player;
        m.usePlatforms = false;
        // hover above and to the side of the player
        double side = m.centerX() < p.centerX() ? -1 : 1;
        double tx = p.centerX() + side * 70, ty = p.centerY() - 40 + Math.sin(m.anim * 0.04) * 20;
        if (p.dead) ty -= 300;
        double dx = tx - m.centerX(), dy = ty - m.centerY();
        m.vx += Math.signum(dx) * 0.04;
        m.vy += Math.signum(dy) * 0.04;
        m.vx = Math.max(-1.3, Math.min(1.3, m.vx)) * 0.99;
        m.vy = Math.max(-1.1, Math.min(1.1, m.vy)) * 0.99;
        double pvx = m.vx, pvy = m.vy;
        m.move(g.world);
        if (m.hitWall) m.vx = -pvx * 0.6;
        if (m.onGround || m.hitCeiling) m.vy = -pvy * 0.6;
        m.dir = p.centerX() >= m.centerX() ? 1 : -1;
        if (!p.dead && ++m.attackTimer > 110 && Math.abs(p.centerX() - m.centerX()) < 200) {
            m.attackTimer = g.rnd.nextInt(40);
            double ax = p.centerX() - m.centerX(), ay = p.centerY() - m.centerY();
            double d = Math.max(1, Math.hypot(ax, ay));
            g.shootHostile(Projectile.STINGER, m.centerX(), m.centerY() + 3, ax / d * 3.2, ay / d * 3.2, 16);
        }
    }

    private static void caster(Mob m, Game g) {
        Player p = g.player;
        m.passDoors = false;
        double dx = p.centerX() - m.centerX();
        m.dir = dx >= 0 ? 1 : -1;
        m.vx *= 0.8;
        gravity(m, g);
        m.move(g.world);
        if (p.dead) return;
        m.attackTimer++;
        if (m.attackTimer == 200) {
            // teleport somewhere near the player
            for (int k = 0; k < 20; k++) {
                int tx = (int) (p.centerX() / Tiles.T) + (g.rnd.nextBoolean() ? 1 : -1) * (6 + g.rnd.nextInt(12));
                int ty = (int) (p.centerY() / Tiles.T) - 6 + g.rnd.nextInt(10);
                if (!g.world.solid(tx, ty) && !g.world.solid(tx, ty - 1) && !g.world.solid(tx, ty - 2)
                        && g.world.solid(tx, ty + 1)) {
                    g.burst(m.centerX(), m.centerY(), 0xc050ff, 10, 1.5);
                    m.x = tx * Tiles.T - 1;
                    m.y = (ty + 1) * Tiles.T - m.h;
                    m.vx = m.vy = 0;
                    g.burst(m.centerX(), m.centerY(), 0xc050ff, 10, 1.5);
                    g.playAt(Audio.SPELL, m.centerX(), 0.4, 1.3);
                    break;
                }
            }
        } else if (m.attackTimer > 240 && m.attackTimer % 30 == 0 && m.attackTimer <= 330) {
            double ax = p.centerX() - m.centerX(), ay = p.centerY() - m.centerY();
            double d = Math.max(1, Math.hypot(ax, ay));
            g.shootHostile(Projectile.FIREBALL, m.centerX(), m.y + 8, ax / d * 2.2, ay / d * 2.2, 21);
            g.playAt(Audio.SPELL, m.centerX(), 0.4, 1.0);
        } else if (m.attackTimer > 360) {
            m.attackTimer = g.rnd.nextInt(60);
        }
    }

    private static void critter(Mob m, Game g) {
        Player p = g.player;
        double dx = m.centerX() - p.centerX();
        boolean scared = Math.abs(dx) < 50 && Math.abs(m.centerY() - p.centerY()) < 30;
        if (m.onGround) {
            m.vx *= 0.8;
            if (scared) m.dir = dx >= 0 ? 1 : -1;
            else if (g.rnd.nextInt(200) == 0) m.dir = -m.dir;
            if ((scared && g.rnd.nextInt(8) == 0) || g.rnd.nextInt(90) == 0) {
                m.vy = -2.2;
                m.vx = m.dir * (scared ? 1.4 : 0.8);
            }
        }
        if (m.hitWall && m.onGround) m.dir = -m.dir;
        gravity(m, g);
        m.move(g.world);
    }

    private static void bat(Mob m, Game g) {
        Player p = g.player;
        m.usePlatforms = false;
        double sp = SPEED[m.type];
        if (--m.timer <= 0) {
            m.timer = 20 + g.rnd.nextInt(40);
            double dx = p.centerX() - m.centerX(), dy = p.centerY() - m.centerY();
            double d = Math.max(1, Math.hypot(dx, dy));
            double v = (1.2 + g.rnd.nextDouble() * 0.6) * sp;
            m.ai[0] = dx / d * v + (g.rnd.nextDouble() - 0.5) * 1.4;
            m.ai[1] = dy / d * v + (g.rnd.nextDouble() - 0.5) * 1.4;
            if (p.dead) m.ai[1] = -1;
        }
        m.vx += (m.ai[0] - m.vx) * 0.06;
        m.vy += (m.ai[1] - m.vy) * 0.06;
        double pvx = m.vx, pvy = m.vy;
        m.move(g.world);
        if (m.hitWall) { m.vx = -pvx; m.ai[0] = -m.ai[0]; }
        if (m.onGround || m.hitCeiling) { m.vy = -pvy; m.ai[1] = -m.ai[1]; }
        m.dir = m.vx >= 0 ? 1 : -1;
    }

    // ---- spawning -------------------------------------------------------------

    static final int ZONE_SURFACE = 0, ZONE_UNDERGROUND = 1, ZONE_CAVERN = 2, ZONE_UNDERWORLD = 3;

    static int zone(World w, int ty) {
        if (ty >= w.underworld) return ZONE_UNDERWORLD;
        if (ty < w.surfaceLevel) return ZONE_SURFACE;
        if (ty < w.rockLevel + 10) return ZONE_UNDERGROUND;
        return ZONE_CAVERN;
    }

    private static int pick(Random r, int... weightsAndTypes) {
        int sum = 0;
        for (int i = 0; i < weightsAndTypes.length; i += 2) sum += weightsAndTypes[i];
        int v = r.nextInt(sum);
        for (int i = 0; i < weightsAndTypes.length; i += 2) {
            v -= weightsAndTypes[i];
            if (v < 0) return weightsAndTypes[i + 1];
        }
        return weightsAndTypes[1];
    }

    static int chooseType(Game g, int zone) {
        Random r = g.rnd;
        if (zone == ZONE_UNDERWORLD) return pick(r, 30, LAVA_SLIME, 25, FIRE_IMP, 15, DEMON, 30, HELLBAT);
        if (g.biome == Background.CORRUPTION && r.nextInt(100) < 65) return EATER;
        if (g.biome == Background.JUNGLE) {
            if (zone == ZONE_SURFACE && !g.isNight()) return pick(r, 70, JUNGLE_SLIME, 15, HORNET, 15, GREEN_SLIME);
            if (zone != ZONE_SURFACE) return pick(r, 40, HORNET, 40, JUNGLE_SLIME, 20, CAVE_BAT);
        }
        switch (zone) {
            case ZONE_SURFACE:
                if (g.isNight()) return pick(r, 55, ZOMBIE, 40, DEMON_EYE, 5, BLUE_SLIME);
                return pick(r, 60, GREEN_SLIME, 30, BLUE_SLIME, 8, PURPLE_SLIME, 1, PINKY);
            case ZONE_UNDERGROUND:
                return pick(r, 30, BLUE_SLIME, 25, RED_SLIME, 30, CAVE_BAT, 10, GREEN_SLIME, 1, PINKY);
            default:
                return pick(r, 20, RED_SLIME, 18, YELLOW_SLIME, 18, BLACK_SLIME, 25, CAVE_BAT, 25, SKELETON);
        }
    }

    /** Tries to spawn a creature somewhere just outside the view. */
    static void trySpawn(Game g) {
        World w = g.world;
        Player p = g.player;
        if (p.dead) return;
        int ptx = (int) (p.centerX() / Tiles.T), pty = (int) (p.centerY() / Tiles.T);
        int playerZone = zone(w, pty);
        int hostile = 0, critters = 0;
        for (Mob m : g.mobs) {
            if (isCritter(m.type)) critters++;
            else hostile++;
        }
        // bunnies in the forest by day
        if (playerZone == ZONE_SURFACE && !g.isNight() && g.biome == Background.FOREST && critters < 3
                && g.rnd.nextInt(500) == 0) {
            spawnAt(g, BUNNY, ptx, pty);
        }
        int max = playerZone == ZONE_SURFACE ? (g.isNight() ? 8 : 5) : 8;
        if (g.town.size() > 0 && g.nearTown()) max = 2;
        if (hostile >= max) return;
        int rate = playerZone == ZONE_SURFACE ? (g.isNight() ? 150 : 300) : playerZone == ZONE_UNDERWORLD ? 120 : 170;
        if (g.rnd.nextInt(rate) != 0) return;
        spawnAt(g, -1, ptx, pty);
    }

    private static void spawnAt(Game g, int fixedType, int ptx, int pty) {
        World w = g.world;
        Player p = g.player;
        int halfW = g.viewW / 2 / Tiles.T + 3, halfH = g.viewH / 2 / Tiles.T + 3;
        for (int attempt = 0; attempt < 30; attempt++) {
            int tx = ptx + (g.rnd.nextBoolean() ? 1 : -1) * (halfW + g.rnd.nextInt(22));
            int ty = pty - halfH - 10 + g.rnd.nextInt(halfH * 2 + 20);
            if (!w.inside(tx, ty) || ty < 5 || ty >= w.h - 5) continue;
            int zone = zone(w, ty);
            int type = fixedType > 0 ? fixedType : chooseType(g, zone);
            int cw = (W[type] + Tiles.T - 1) / Tiles.T, ch = (H[type] + Tiles.T - 1) / Tiles.T;
            if (!free(w, tx, ty, cw, ch)) continue;
            int wl = w.wall(tx, ty);
            if (wl != 0 && !Tiles.WALL_NATURAL[wl]) continue; // player-built area
            boolean flying = AI[type] == AI_FLYER || AI[type] == AI_BAT || AI[type] == AI_HORNET;
            if (!flying) {
                // drop down to the ground (at most 12 tiles)
                int gy = ty;
                while (gy < ty + 12 && !w.solid(tx, gy + 1) && !w.isPlatform(tx, gy + 1)) gy++;
                if (!w.solid(tx, gy + 1) && !w.isPlatform(tx, gy + 1)) continue;
                if (!free(w, tx, gy - ch + 1, cw, ch)) continue;
                if (w.liquidAt(tx, gy) > 0 && !LAVA_PROOF[type]) continue;
                int gwl = w.wall(tx, gy);
                if (gwl != 0 && !Tiles.WALL_NATURAL[gwl]) continue;
                ty = gy;
            }
            if (w.isLava(tx, ty) && !LAVA_PROOF[type]) continue;
            if (zone == ZONE_SURFACE && ty > w.skyTop[tx] + 2 && !flying) continue; // not inside surface caves
            Mob m = new Mob(type, tx * Tiles.T + cw * Tiles.T / 2.0, (ty + 1) * Tiles.T);
            m.dir = p.centerX() > m.centerX() ? 1 : -1;
            m.variant = g.rnd.nextInt(2);
            g.mobs.add(m);
            return;
        }
    }

    private static boolean free(World w, int tx, int ty, int cw, int ch) {
        for (int y = ty; y < ty + ch; y++)
            for (int x = tx; x < tx + cw; x++)
                if (w.solid(x, y) || !w.inside(x, y)) return false;
        return true;
    }

    /** Removes creatures that are far away (and night creatures after sunrise). */
    static boolean shouldDespawn(Mob m, Game g) {
        double dx = Math.abs(m.centerX() - g.player.centerX()), dy = Math.abs(m.centerY() - g.player.centerY());
        boolean far = dx > g.viewW * 0.5 + 30 * Tiles.T || dy > g.viewH * 0.5 + 25 * Tiles.T;
        boolean off = dx > g.viewW * 0.5 + 16 || dy > g.viewH * 0.5 + 16;
        if (off) m.offscreen++;
        else m.offscreen = 0;
        if (far && m.offscreen > 60) return true;
        return fleeing(m, g) && m.offscreen > 120;
    }

    // ---- loot ------------------------------------------------------------------

    static void dropLoot(Mob m, Game g) {
        Random r = g.rnd;
        int tx = (int) (m.centerX() / Tiles.T), ty = (int) (m.centerY() / Tiles.T);
        switch (m.type) {
            case PINKY:
                g.spawnDrop(tx, ty, Items.GEL, 15 + r.nextInt(11));
                break;
            case PURPLE_SLIME: case YELLOW_SLIME: case BLACK_SLIME: case JUNGLE_SLIME: case LAVA_SLIME:
                g.spawnDrop(tx, ty, Items.GEL, 2 + r.nextInt(3));
                break;
            case RED_SLIME:
                g.spawnDrop(tx, ty, Items.GEL, 2 + r.nextInt(2));
                break;
            case GREEN_SLIME: case BLUE_SLIME:
                g.spawnDrop(tx, ty, Items.GEL, 1 + r.nextInt(2));
                break;
            case DEMON_EYE:
                if (r.nextInt(3) == 0) g.spawnDrop(tx, ty, Items.LENS, 1);
                break;
            case HORNET:
                if (r.nextInt(2) == 0) g.spawnDrop(tx, ty, Items.STINGER, 1);
                break;
            case EATER:
                if (r.nextInt(3) == 0) g.spawnDrop(tx, ty, Items.ROTTEN_CHUNK, 1);
                break;
            case FIRE_IMP:
                if (r.nextInt(50) == 0) g.spawnDrop(tx, ty, Items.OBSIDIAN_SKULL, 1);
                break;
            default:
                break;
        }
        int value = (int) (VALUE[m.type] * (0.75 + r.nextDouble() * 0.5));
        g.spawnCoins(m.centerX(), m.centerY(), value);
    }
}
