package mingeriacc;

import java.util.Random;

/** Creature definitions, behaviour (AI), spawning and loot. */
final class Mobs {
    private Mobs() {}

    static final int GREEN_SLIME = 1, BLUE_SLIME = 2, RED_SLIME = 3, PURPLE_SLIME = 4, YELLOW_SLIME = 5,
            BLACK_SLIME = 6, PINKY = 7, ZOMBIE = 8, DEMON_EYE = 9, CAVE_BAT = 10, SKELETON = 11,
            JUNGLE_SLIME = 12, HORNET = 13, EATER = 14, LAVA_SLIME = 15, FIRE_IMP = 16, DEMON = 17,
            HELLBAT = 18, BUNNY = 19, GUIDE = 20, MERCHANT = 21, NURSE = 22,
            EYE_OF_CTHULHU = 23, SERVANT = 24, BLOOD_ZOMBIE = 25, DRIPPLER = 26, ICE_SLIME = 27,
            FROZEN_ZOMBIE = 28, ICE_BAT = 29, PENGUIN = 30, ARCANIST = 31, DRYAD = 32;
    static final int COUNT = 33;

    static final int AI_SLIME = 0, AI_FIGHTER = 1, AI_FLYER = 2, AI_BAT = 3, AI_HORNET = 4, AI_CASTER = 5,
            AI_CRITTER = 6, AI_TOWN = 7, AI_BOSS_EYE = 8, AI_CHASER = 9;

    static final String[] NAME = new String[COUNT];
    static final int[] W = new int[COUNT], H = new int[COUNT];
    static final int[] LIFE = new int[COUNT], DAMAGE = new int[COUNT], DEFENSE = new int[COUNT];
    static final double[] KB_RES = new double[COUNT];
    static final int[] VALUE = new int[COUNT];     // coins dropped, in copper
    static final int[] AI = new int[COUNT];
    static final int[] COLOR = new int[COUNT];
    static final boolean[] NIGHT = new boolean[COUNT];      // leaves when the sun rises
    static final boolean[] LAVA_PROOF = new boolean[COUNT];
    static final boolean[] BOSS = new boolean[COUNT];
    static final boolean[] NO_CLIP = new boolean[COUNT];    // flies through tiles
    static final int[] HIT_FX = new int[COUNT];             // debuff given to the player on touch
    static final double[] SPEED = new double[COUNT];        // flyer speed
    static final int[] HURT_SOUND = new int[COUNT];         // when hit
    static final double[] HURT_PITCH = new double[COUNT];
    static final int[] IDLE_SOUND = new int[COUNT];         // made now and then (-1 = none)
    static final int[] IDLE_RATE = new int[COUNT];          // about once in this many frames

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
        def(EYE_OF_CTHULHU, "Eye of Cthulhu", 40, 40, 2200, 15, 12, 0, 30000, AI_BOSS_EYE);
        def(SERVANT, "Servant of Cthulhu", 9, 9, 8, 12, 0, 1.0, 0, AI_CHASER);
        def(BLOOD_ZOMBIE, "Blood Zombie", 10, 22, 90, 21, 8, 0.5, 120, AI_FIGHTER);
        def(DRIPPLER, "Drippler", 14, 14, 80, 20, 4, 0.8, 100, AI_FLYER);
        def(ICE_SLIME, "Ice Slime", 14, 10, 30, 12, 4, 1.0, 60, AI_SLIME);
        def(FROZEN_ZOMBIE, "Frozen Zombie", 10, 22, 60, 16, 8, 0.5, 90, AI_FIGHTER);
        def(ICE_BAT, "Ice Bat", 12, 8, 25, 16, 4, 0.8, 110, AI_BAT);
        def(PENGUIN, "Penguin", 8, 10, 5, 0, 0, 1.0, 0, AI_CRITTER);
        def(ARCANIST, "Arcanist", 10, 22, 250, 0, 15, 0.5, 0, AI_TOWN);
        def(DRYAD, "Dryad", 10, 22, 250, 0, 15, 0.5, 0, AI_TOWN);
        COLOR[GREEN_SLIME] = 0x5cc84a;
        COLOR[BLUE_SLIME] = 0x4a86ea;
        COLOR[RED_SLIME] = 0xe04848;
        COLOR[PURPLE_SLIME] = 0xa060e0;
        COLOR[YELLOW_SLIME] = 0xf0d040;
        COLOR[BLACK_SLIME] = 0x3a3a4c;
        COLOR[PINKY] = 0xf08ad0;
        COLOR[JUNGLE_SLIME] = 0x4a9a2a;
        COLOR[LAVA_SLIME] = 0xff7a20;
        COLOR[ICE_SLIME] = 0x9adcf8;
        for (int t : new int[]{ZOMBIE, DEMON_EYE, BLOOD_ZOMBIE, DRIPPLER, FROZEN_ZOMBIE}) NIGHT[t] = true;
        for (int t : new int[]{LAVA_SLIME, FIRE_IMP, DEMON, HELLBAT}) LAVA_PROOF[t] = true;
        BOSS[EYE_OF_CTHULHU] = true;
        NO_CLIP[EYE_OF_CTHULHU] = true;
        NO_CLIP[SERVANT] = true;
        HIT_FX[LAVA_SLIME] = Items.FX_FIRE;
        HIT_FX[HELLBAT] = Items.FX_FIRE;
        HIT_FX[HORNET] = Items.FX_POISON;
        HIT_FX[ICE_SLIME] = Items.FX_FROST;
        HIT_FX[FROZEN_ZOMBIE] = Items.FX_FROST;
        HIT_FX[ICE_BAT] = Items.FX_FROST;
        SPEED[EATER] = 1.25;
        SPEED[DEMON] = 0.7;
        SPEED[HELLBAT] = 1.4;
        SPEED[DRIPPLER] = 0.5;
        SPEED[ICE_BAT] = 1.1;
        sounds();
    }

    private static void sound(int type, int hurt, double pitch, int idle, int rate) {
        HURT_SOUND[type] = hurt;
        HURT_PITCH[type] = pitch;
        IDLE_SOUND[type] = idle;
        IDLE_RATE[type] = rate;
    }

    /** Every creature sounds like itself when hit, and many make a noise now and then. */
    private static void sounds() {
        for (int t = 1; t < COUNT; t++) sound(t, Audio.HIT, 1, -1, 0);
        for (int t = 1; t < COUNT; t++) if (AI[t] == AI_SLIME) sound(t, Audio.SQUISH, 1, -1, 0);
        sound(PINKY, Audio.SQUISH, 1.5, -1, 0);
        sound(ICE_SLIME, Audio.SQUISH, 1.2, -1, 0);
        sound(ZOMBIE, Audio.OOF, 0.55, Audio.ZOMBIE, 900);
        sound(BLOOD_ZOMBIE, Audio.OOF, 0.5, Audio.ZOMBIE, 700);
        sound(FROZEN_ZOMBIE, Audio.OOF, 0.6, Audio.ZOMBIE, 900);
        sound(SKELETON, Audio.BONE, 1, Audio.RATTLE, 700);
        for (int t : new int[]{DEMON_EYE, EATER, DRIPPLER, SERVANT, EYE_OF_CTHULHU}) sound(t, Audio.SQUELCH, 1, -1, 0);
        sound(DEMON_EYE, Audio.SQUELCH, 1.1, Audio.SQUELCH, 1200);
        sound(DRIPPLER, Audio.SQUELCH, 0.8, Audio.SQUELCH, 900);
        sound(EYE_OF_CTHULHU, Audio.SQUELCH, 0.7, -1, 0);
        sound(SERVANT, Audio.SQUELCH, 1.5, -1, 0);
        sound(CAVE_BAT, Audio.SQUEAK, 1, Audio.SQUEAK, 420);
        sound(ICE_BAT, Audio.SQUEAK, 1.15, Audio.SQUEAK, 420);
        sound(HELLBAT, Audio.SQUEAK, 0.8, Audio.SQUEAK, 420);
        sound(HORNET, Audio.BONE, 1.7, Audio.BUZZ, 110);
        sound(FIRE_IMP, Audio.GROWL, 1.5, Audio.GROWL, 900);
        sound(DEMON, Audio.GROWL, 0.9, Audio.GROWL, 700);
        sound(BUNNY, Audio.SQUEAK, 1.6, -1, 0);
        sound(PENGUIN, Audio.SQUAWK, 1, Audio.SQUAWK, 700);
        for (int t = 1; t < COUNT; t++) if (AI[t] == AI_TOWN) sound(t, Audio.OOF, 1, -1, 0);
        sound(NURSE, Audio.OOF, 1.35, -1, 0);
        sound(DRYAD, Audio.OOF, 1.3, -1, 0);
        sound(ARCANIST, Audio.OOF, 0.85, -1, 0);
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
            case AI_BOSS_EYE: Boss.eye(m, g); break;
            case AI_CHASER: chaser(m, g); break;
            default: bat(m, g); break;
        }
        debuffs(m, g);
    }

    /** Burning, poison, frostburn and lava. */
    private static void debuffs(Mob m, Game g) {
        World w = g.world;
        int tx = (int) (m.centerX() / Tiles.T), ty = (int) ((m.y + m.h - 2) / Tiles.T);
        if (!LAVA_PROOF[m.type] && !NO_CLIP[m.type] && w.isLava(tx, ty) && w.liquidAt(tx, ty) > 60) {
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
        if (m.frostburn > 0) {
            m.frostburn--;
            if (g.ticks % 15 == 7) Combat.dot(g, m, 3);
            if (g.rnd.nextInt(3) == 0) {
                Entities.Particle p = new Entities.Particle(m.x + g.rnd.nextDouble() * m.w, m.y + g.rnd.nextDouble() * m.h,
                        (g.rnd.nextDouble() - 0.5) * 0.3, -0.2 - g.rnd.nextDouble() * 0.3, 25, Pal.ICE_L, -0.005);
                p.glow = true;
                p.noCollide = true;
                g.particles.add(p);
            }
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

    /** How much lower the player's feet are than the creature's (negative: the player is higher). */
    private static double feetBelow(Mob m, Player p) {
        return (p.y + p.h) - (m.y + m.h);
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
            // a target below: slip through the platform
            if (target && feetBelow(m, p) > 10 && m.standingOnPlatform(g.world)) m.dropTimer = 10;
            m.ai[0] += (m.hurtTimer < 60 ? 3 : 1) * (target ? 1 : 0.6);
            if (m.ai[0] >= 80) {
                m.ai[1]++;
                boolean big = ((int) m.ai[1]) % 3 == 0 || (target && feetBelow(m, p) < -16);
                m.vy = big ? -4.0 : -2.7;
                m.vx = m.dir * (big ? 1.05 : 1.3) * (m.type == PINKY ? 1.2 : 1);
                if (target) g.playAt(Audio.BLUB, m.centerX(), 0.18, (big ? 0.8 : 1.0) * (m.type == PINKY ? 1.5 : 1));
                m.ai[0] = -g.rnd.nextInt(40);
                m.onGround = false;
            }
        } else if (m.hitWall) {
            m.vx *= 0.5;
        }
        gravity(m, g);
        m.move(g.world);
    }

    /** Walkers (zombies, skeletons): they jump over walls and use platforms to follow the player up or down. */
    private static void fighter(Mob m, Game g) {
        Player p = g.player;
        World w = g.world;
        double dx = p.centerX() - m.centerX();
        double below = feetBelow(m, p);
        boolean flee = fleeing(m, g) || p.dead;
        int want = dx >= 0 ? 1 : -1;
        if (flee) want = -want;
        if (m.ai[2] > 0) {
            m.ai[2]--; // turned around after getting stuck
        } else if (Math.abs(dx) > 6 || flee) {
            m.dir = want;
        }
        double max = m.type == SKELETON ? 0.62 : m.type == BLOOD_ZOMBIE ? 0.58 : 0.5;
        double acc = m.onGround ? 0.05 : 0.02;
        if (m.dir > 0 ? m.vx < max : m.vx > -max) m.vx += m.dir * acc;
        else if (m.onGround) m.vx *= 0.9;
        boolean blocked = m.hitWall && m.onGround;
        int door = blocked ? doorAhead(m, w) : -1;
        if (door >= 0 && !flee && w.bloodMoon) {
            // during a blood moon the dead push doors open
            m.vx = 0;
            if (++m.bash % 30 == 0) g.playAt(Audio.DOOR, m.centerX(), 0.25, 0.6);
            if (m.bash > 150) {
                g.mobOpensDoor(door % w.w, door / w.w, m.dir);
                m.bash = 0;
            }
        } else if (blocked) {
            m.bash = 0;
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
        if (m.jumpWait > 0) m.jumpWait--;
        if (m.onGround && !flee && !p.dead) {
            if (below > 10 && m.standingOnPlatform(w) && Math.abs(dx) < 20 * Tiles.T) {
                m.dropTimer = 10; // the player is below: drop through the platform
            } else if (below < -10 && Math.abs(dx) < 7 * Tiles.T && m.jumpWait == 0 && platformAbove(m, w)) {
                m.vy = -3.7; // the player is above: jump up onto the platform
                m.onGround = false;
                m.jumpWait = 45;
            } else if (Math.abs(dx) < 3 * Tiles.T && p.centerY() < m.y - 6 && g.rnd.nextInt(40) == 0) {
                m.vy = -3.6; // hop up to a player standing higher up
            }
        }
        gravity(m, g);
        m.move(w);
        m.anim = m.onGround ? m.anim + Math.abs(m.vx) * 0.13 : m.anim;
    }

    /** Index (x + y * w) of a closed door right in front of a walker, or -1. */
    private static int doorAhead(Mob m, World w) {
        int tx = (int) Math.floor((m.dir > 0 ? m.x + m.w + 1 : m.x - 1) / Tiles.T);
        int ty0 = (int) Math.floor(m.y / Tiles.T), ty1 = (int) Math.floor((m.y + m.h - 1) / Tiles.T);
        for (int ty = ty0; ty <= ty1; ty++) if (w.tile(tx, ty) == Tiles.DOOR_CLOSED) return tx + ty * w.w;
        return -1;
    }

    /** Whether there is a platform 1..4 tiles above the feet, within reach of a jump. */
    private static boolean platformAbove(Mob m, World w) {
        int feet = (int) Math.floor((m.y + m.h + 0.5) / Tiles.T);
        int tx0 = (int) Math.floor(m.x / Tiles.T) - 1, tx1 = (int) Math.floor((m.x + m.w - 0.001) / Tiles.T) + 1;
        for (int ty = feet - 1; ty >= feet - 4; ty--)
            for (int tx = tx0; tx <= tx1; tx++)
                if (w.isPlatform(tx, ty) && !w.solid(tx, ty - 1)) return true;
        return false;
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
        } else if (m.type == DRIPPLER) {
            dy += Math.sin(m.anim * 0.03) * 20 - 10;
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
        if (m.type == DRIPPLER && g.rnd.nextInt(12) == 0) {
            g.dust(m.x + 2 + g.rnd.nextDouble() * (m.w - 4), m.y + m.h, 0xa01818, 1);
        }
        double pvx = m.vx, pvy = m.vy;
        m.move(g.world);
        if (m.hitWall) m.vx = -pvx * 0.6;
        if (m.onGround || m.hitCeiling) m.vy = -pvy * 0.6;
        m.dir = m.vx >= 0 ? 1 : -1;
        m.rot = Math.atan2(m.vy, m.vx);
    }

    /** Servants of Cthulhu: small eyes flying straight at the player through walls. */
    private static void chaser(Mob m, Game g) {
        Player p = g.player;
        double tx = p.centerX(), ty = p.centerY();
        if (p.dead) {
            tx = m.centerX();
            ty = m.centerY() - 400;
        }
        double dx = tx - m.centerX(), dy = ty - m.centerY();
        double d = Math.max(1, Math.hypot(dx, dy));
        double sp = 1.9;
        m.vx += (dx / d * sp - m.vx) * 0.035;
        m.vy += (dy / d * sp - m.vy) * 0.035;
        m.move(g.world);
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
        boolean penguin = m.type == PENGUIN;
        if (m.onGround) {
            m.vx *= penguin ? 0.9 : 0.8;
            if (scared) m.dir = dx >= 0 ? 1 : -1;
            else if (g.rnd.nextInt(200) == 0) m.dir = -m.dir;
            if (penguin) {
                // penguins waddle and hop only a little
                if (scared || g.rnd.nextInt(3) != 0) m.vx = m.dir * (scared ? 0.9 : 0.35);
                if (m.hitWall || g.rnd.nextInt(120) == 0) m.vy = -1.6;
            } else if ((scared && g.rnd.nextInt(8) == 0) || g.rnd.nextInt(90) == 0) {
                m.vy = -2.2;
                m.vx = m.dir * (scared ? 1.4 : 0.8);
            }
        }
        if (m.hitWall && m.onGround && !penguin) m.dir = -m.dir;
        gravity(m, g);
        m.move(g.world);
        if (penguin && m.onGround) m.anim += Math.abs(m.vx) * 0.2;
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
        int v = r.nextInt(Math.max(1, sum));
        for (int i = 0; i < weightsAndTypes.length; i += 2) {
            v -= weightsAndTypes[i];
            if (v < 0) return weightsAndTypes[i + 1];
        }
        return weightsAndTypes[1];
    }

    static int chooseType(Game g, int zone) {
        Random r = g.rnd;
        boolean night = g.isNight();
        if (zone == ZONE_UNDERWORLD) return pick(r, 30, LAVA_SLIME, 25, FIRE_IMP, 15, DEMON, 30, HELLBAT);
        if (zone == ZONE_SURFACE && night && g.world.bloodMoon)
            return pick(r, 30, ZOMBIE, 30, BLOOD_ZOMBIE, 25, DRIPPLER, 15, DEMON_EYE);
        if (g.biome == Background.CORRUPTION && r.nextInt(100) < 65) return EATER;
        if (g.biome == Background.JUNGLE) {
            if (zone == ZONE_SURFACE && !night) return pick(r, 70, JUNGLE_SLIME, 15, HORNET, 15, GREEN_SLIME);
            if (zone != ZONE_SURFACE) return pick(r, 40, HORNET, 40, JUNGLE_SLIME, 20, CAVE_BAT);
        }
        if (g.biome == Background.SNOW) {
            if (zone == ZONE_SURFACE) return night ? pick(r, 60, FROZEN_ZOMBIE, 30, DEMON_EYE, 10, ICE_SLIME) : ICE_SLIME;
            if (zone == ZONE_UNDERGROUND) return pick(r, 45, ICE_BAT, 45, ICE_SLIME, 10, CAVE_BAT);
            return pick(r, 35, ICE_BAT, 30, ICE_SLIME, 20, SKELETON, 15, BLACK_SLIME);
        }
        switch (zone) {
            case ZONE_SURFACE:
                if (night) return pick(r, 55, ZOMBIE, 40, DEMON_EYE, 5, BLUE_SLIME);
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
            else if (!BOSS[m.type] && m.type != SERVANT) hostile++;
        }
        // bunnies in the forest and penguins in the snow by day
        if (playerZone == ZONE_SURFACE && !g.isNight() && critters < 3 && g.rnd.nextInt(500) == 0) {
            if (g.biome == Background.FOREST) spawnAt(g, BUNNY, ptx, pty);
            else if (g.biome == Background.SNOW) spawnAt(g, PENGUIN, ptx, pty);
        }
        boolean blood = w.bloodMoon && g.isNight() && playerZone == ZONE_SURFACE;
        int max = playerZone == ZONE_SURFACE ? (g.isNight() ? (blood ? 12 : 8) : 5) : 8;
        if (g.town.size() > 0 && g.nearTown()) max = blood ? 5 : 2;
        if (hostile >= max) return;
        int rate = playerZone == ZONE_SURFACE ? (g.isNight() ? (blood ? 50 : 150) : 300) : playerZone == ZONE_UNDERWORLD ? 120 : 170;
        if (g.rnd.nextInt(rate) != 0) return;
        spawnAt(g, -1, ptx, pty);
    }

    static Mob spawnAt(Game g, int fixedType, int ptx, int pty) {
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
            return m;
        }
        return null;
    }

    private static boolean free(World w, int tx, int ty, int cw, int ch) {
        for (int y = ty; y < ty + ch; y++)
            for (int x = tx; x < tx + cw; x++)
                if (w.solid(x, y) || !w.inside(x, y)) return false;
        return true;
    }

    /** Removes creatures that are far away (and night creatures after sunrise). Bosses leave by themselves. */
    static boolean shouldDespawn(Mob m, Game g) {
        if (BOSS[m.type]) return false;
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
            case RED_SLIME: case ICE_SLIME:
                g.spawnDrop(tx, ty, Items.GEL, 2 + r.nextInt(2));
                break;
            case GREEN_SLIME: case BLUE_SLIME:
                g.spawnDrop(tx, ty, Items.GEL, 1 + r.nextInt(2));
                break;
            case DEMON_EYE:
                if (r.nextInt(3) == 0) g.spawnDrop(tx, ty, Items.LENS, 1);
                break;
            case DRIPPLER:
                if (r.nextInt(4) == 0) g.spawnDrop(tx, ty, Items.LENS, 1);
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
            case FROZEN_ZOMBIE:
                if (r.nextInt(60) == 0) g.spawnDrop(tx, ty, Items.ICE_SKATES, 1);
                break;
            case EYE_OF_CTHULHU:
                Boss.eyeLoot(m, g);
                break;
            default:
                break;
        }
        // hearts and mana stars help in long fights
        if (!isCritter(m.type) && !BOSS[m.type]) {
            Player p = g.player;
            if (p.life < p.lifeMax && r.nextInt(m.type == SERVANT ? 2 : 7) == 0) g.spawnDrop(tx, ty, Items.HEART, 1);
            if (p.mana < p.manaCap() && g.carriesMagic() && r.nextInt(5) == 0) g.spawnDrop(tx, ty, Items.MANA_STAR, 1);
        }
        int value = (int) (VALUE[m.type] * (0.75 + r.nextDouble() * 0.5));
        g.spawnCoins(m.centerX(), m.centerY(), value);
    }
}
