package mingeriacc;

import mingeriacc.Entities.Drop;

/**
 * Things that happen at certain times: blood moons, slime rain, falling stars
 * and the Eye of Cthulhu coming by itself. The state of an event is kept in
 * the world (and saved with it).
 */
final class Events {
    private Events() {}

    static final double NIGHTFALL = 19.5, DAWN = 4.5, MORNING = 7.0, EVENING = 18.0;
    static final int SLIME_RAIN_GOAL = 60;
    static final int SLIME_RAIN_COLOR = 0x6ad0ff;

    /** Whether the clock went past an hour between two steps (the day wraps at 24). */
    private static boolean passed(double from, double to, double hour) {
        if (to >= from) return from < hour && to >= hour;
        return from < hour || to >= hour;
    }

    static void update(Game g, double prevTime) {
        World w = g.world;
        double t = w.time;
        if (passed(prevTime, t, NIGHTFALL)) nightfall(g);
        if (passed(prevTime, t, DAWN)) dawn(g);
        if (passed(prevTime, t, MORNING)) morning(g);
        if (passed(prevTime, t, EVENING) && w.slimeRain) endSlimeRain(g);
        if (g.eyeTimer > 0 && --g.eyeTimer == 0 && g.isNight() && !Boss.alive(g, Mobs.EYE_OF_CTHULHU)) Boss.spawnEye(g);
        if (w.slimeRain) slimeRain(g);
        if (g.isNight() && !g.player.dead && g.rnd.nextInt(1500) == 0) fallingStar(g);
    }

    private static void nightfall(Game g) {
        World w = g.world;
        Player p = g.player;
        if (!w.downedEye && p.lifeMax >= 200 && p.defense >= 10 && g.rnd.nextInt(3) == 0
                && !Boss.alive(g, Mobs.EYE_OF_CTHULHU)) {
            // a strong enough player is noticed by the Eye
            g.message("You feel an evil presence watching you...", Pal.UI_BOSS);
            g.eyeTimer = 60 * 12;
        } else if (w.day >= 3 && p.lifeMax >= 120 && g.rnd.nextInt(9) == 0) {
            startBloodMoon(g);
        }
    }

    static void startBloodMoon(Game g) {
        g.world.bloodMoon = true;
        g.message("The Blood Moon is rising...", Pal.UI_EVENT);
    }

    private static void dawn(Game g) {
        World w = g.world;
        if (w.bloodMoon) {
            w.bloodMoon = false;
            g.message("The Blood Moon has set.", Pal.UI_DIM);
        }
        // fallen stars fade away in the sunlight
        for (Drop d : g.drops) {
            if (d.item != Items.FALLEN_STAR) continue;
            d.dead = true;
            g.burst(d.x + 3, d.y + 3, Pal.STAR, 6, 1.0);
        }
    }

    private static void morning(Game g) {
        World w = g.world;
        if (!w.slimeRain && w.day >= 3 && g.rnd.nextInt(40) == 0) startSlimeRain(g);
    }

    static void startSlimeRain(Game g) {
        g.world.slimeRain = true;
        g.world.slimeRainKills = 0;
        g.message("Slime is falling from the sky!", SLIME_RAIN_COLOR);
    }

    static void endSlimeRain(Game g) {
        g.world.slimeRain = false;
        g.message("Slime rain has ended.", SLIME_RAIN_COLOR);
    }

    /** Called when a slime dies: enough of them end the rain. */
    static void slimeKilled(Game g) {
        World w = g.world;
        if (!w.slimeRain) return;
        if (++w.slimeRainKills >= SLIME_RAIN_GOAL) endSlimeRain(g);
    }

    /** Slimes drop from the sky around a player who is out in the open. */
    private static void slimeRain(Game g) {
        Player p = g.player;
        World w = g.world;
        if (p.dead || g.isNight() || g.ticks % 45 != 0) return;
        int ptx = (int) (p.centerX() / Tiles.T);
        if (Mobs.zone(w, (int) (p.centerY() / Tiles.T)) != Mobs.ZONE_SURFACE) return;
        int slimes = 0;
        for (Mob m : g.mobs) if (Mobs.isSlime(m.type)) slimes++;
        if (slimes >= 12) return;
        int tx = ptx + (int) ((g.rnd.nextDouble() - 0.5) * g.viewW / Tiles.T * 1.4);
        int ty = (int) (g.camY / Tiles.T) - 3;
        if (!w.inside(tx, Math.max(0, ty)) || ty >= w.skyTop[Math.max(0, Math.min(w.w - 1, tx))]) return;
        int r = g.rnd.nextInt(100);
        int type = r < 50 ? Mobs.GREEN_SLIME : r < 80 ? Mobs.BLUE_SLIME : r < 93 ? Mobs.PURPLE_SLIME
                : r < 99 ? Mobs.YELLOW_SLIME : Mobs.PINKY;
        if (g.biome == Background.SNOW && g.rnd.nextBoolean()) type = Mobs.ICE_SLIME;
        Mob m = new Mob(type, tx * Tiles.T + Tiles.T / 2.0, Math.max(Mobs.H[type], ty * Tiles.T));
        m.vy = 1;
        m.variant = g.rnd.nextInt(2);
        g.mobs.add(m);
    }

    /** A star falls from the night sky and lands as a Fallen Star. */
    static void fallingStar(Game g) {
        Player p = g.player;
        double x = p.centerX() + (g.rnd.nextDouble() - 0.5) * g.viewW * 2.4;
        double y = Math.max(4, g.camY - 60);
        double vx = (g.rnd.nextBoolean() ? 1 : -1) * (1.2 + g.rnd.nextDouble() * 1.2);
        int tx = (int) (x / Tiles.T);
        if (tx < 2 || tx >= g.world.w - 2 || y / Tiles.T >= g.world.skyTop[tx]) return;
        Projectile pr = new Projectile(Projectile.FALLING_STAR, x, y, vx, 4.2);
        pr.damage = 30;
        pr.knock = 4;
        g.projectiles.add(pr);
    }
}
