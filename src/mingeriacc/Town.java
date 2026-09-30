package mingeriacc;

import java.util.ArrayDeque;
import java.util.HashSet;

/**
 * Town folk and their houses. A house is an enclosed room with background
 * walls, a light source, a chair, a table (or a work bench) and a door. Town
 * folk move into free houses during the day when their conditions are met.
 */
final class Town {
    private Town() {}

    private static final String[] GUIDE_NAMES = {"Andrew", "Asher", "Brandon", "Cole", "Dylan", "Garrett", "Jake", "Levi"};
    private static final String[] MERCHANT_NAMES = {"Alfred", "Barney", "Gilbert", "Harold", "Milton", "Walter"};
    private static final String[] NURSE_NAMES = {"Abigail", "Allison", "Amy", "Kelsey", "Molly", "Nora"};

    static final int[] MERCHANT_SHOP = {Items.TORCH, Items.WOOD_ARROW, Items.HEALING_POTION, Items.BOTTLE,
            Items.MINING_HELMET, Items.EMPTY_BUCKET, Items.COPPER_PICK, Items.COPPER_AXE, Items.ACORN, Items.CHAIR,
            Items.FLAMING_ARROW};

    private static final String[] GUIDE_TIPS = {
        "Chop trees with your axe, then make a work bench from ten wood.",
        "Build a house before night! It needs walls behind it, a door, a light, a table and a chair.",
        "Right-click a chair to hear if the room is good enough for someone to live in.",
        "Smelt ore at a furnace. With five iron bars you can make an anvil for better tools.",
        "Slimes drop gel. Gel and wood make torches.",
        "Life crystals are hidden deep underground. Each one gives you more life.",
        "The jungle hides stingers and spores; together with vines they make a fine sword.",
        "Demonite ore lies deep in the corruption, but only a gold pickaxe can break it.",
        "Hellstone in the underworld needs a pickaxe made of demonite.",
        "Where water meets lava, obsidian forms. You need it for hellstone bars.",
        "Chests in caves often hold boots, balloons and other useful gear. Wear them from your inventory.",
        "If you carry fifty silver coins, a merchant might move into a free house.",
    };

    private static final String[] MERCHANT_LINES = {
        "Everything a brave digger needs, at a very fair price.",
        "Torches, arrows, potions... Buy now, thank me later.",
        "Anything you don't need? I'll take it off your hands.",
    };

    private static final String[] NURSE_LINES = {
        "Hold still, this won't hurt. Much.",
        "You look terrible. Let me help, for a small fee.",
        "Please try not to get eaten by slimes again.",
    };

    // ---- houses ---------------------------------------------------------------

    static final class Room {
        boolean ok;
        String reason = "";
    }

    /** Checks the room around a tile (usually a chair). */
    static Room check(World w, int sx, int sy) {
        Room r = new Room();
        HashSet<Integer> seen = new HashSet<>();
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sx, sy});
        boolean light = false, chair = false, table = false, door = false;
        int cells = 0;
        while (!stack.isEmpty()) {
            int[] c = stack.pop();
            int x = c[0], y = c[1];
            if (!w.inside(x, y)) {
                r.reason = "This room is not enclosed.";
                return r;
            }
            int key = x + y * w.w;
            if (seen.contains(key)) continue;
            int t = w.tile(x, y);
            if (Tiles.isDoor(t) || t == Tiles.PLATFORM) {
                door = true;
                seen.add(key);
                continue;
            }
            if (Tiles.SOLID[t]) continue;
            seen.add(key);
            if (++cells > 750) {
                r.reason = "This room is too big or not enclosed.";
                return r;
            }
            if (w.wall(x, y) == 0) {
                r.reason = "This room is missing a background wall.";
                return r;
            }
            if (Tiles.isLightSource(t)) light = true;
            if (t == Tiles.CHAIR) chair = true;
            if (t == Tiles.TABLE || t == Tiles.WORKBENCH) table = true;
            stack.push(new int[]{x + 1, y});
            stack.push(new int[]{x - 1, y});
            stack.push(new int[]{x, y + 1});
            stack.push(new int[]{x, y - 1});
        }
        if (cells < 30) r.reason = "This room is too small.";
        else if (!light) r.reason = "This room needs a light source, like a torch.";
        else if (!chair) r.reason = "This room needs a chair.";
        else if (!table) r.reason = "This room needs a table or a work bench.";
        else if (!door) r.reason = "This room needs a door.";
        else {
            r.ok = true;
            r.reason = "This room is suitable housing.";
        }
        return r;
    }

    private static boolean chairTaken(Game g, int x, int y) {
        for (Mob m : g.town) if (m.homeX == x && m.homeY == y) return true;
        return false;
    }

    /** A free, valid house near the player: its chair position, or null. */
    private static int[] findHome(Game g) {
        World w = g.world;
        int px = (int) (g.player.centerX() / Tiles.T), py = (int) (g.player.centerY() / Tiles.T);
        for (int y = Math.max(1, py - 40); y < Math.min(w.h - 1, py + 40); y++)
            for (int x = Math.max(1, px - 80); x < Math.min(w.w - 1, px + 80); x++) {
                if (w.tile(x, y) != Tiles.CHAIR || w.originY(x, y) != y) continue;
                if (chairTaken(g, x, y)) continue;
                if (check(w, x, y).ok) return new int[]{x, y};
            }
        return null;
    }

    // ---- arrivals ---------------------------------------------------------------

    static boolean present(Game g, int type) {
        for (Mob m : g.town) if (m.type == type) return true;
        return false;
    }

    static Mob create(Game g, int type, double cx, double bottom) {
        Mob m = new Mob(type, cx, bottom);
        m.passDoors = true;
        String[] names = type == Mobs.GUIDE ? GUIDE_NAMES : type == Mobs.MERCHANT ? MERCHANT_NAMES : NURSE_NAMES;
        m.name = names[g.rnd.nextInt(names.length)];
        m.ai[3] = cx;
        return m;
    }

    /** Called every few seconds: town folk move in or find homes. */
    static void update(Game g) {
        World w = g.world;
        // homes that are no longer valid
        for (Mob m : g.town) {
            if (m.homeX >= 0 && (w.tile(m.homeX, m.homeY) != Tiles.CHAIR || !check(w, m.homeX, m.homeY).ok)) {
                m.homeX = m.homeY = -1;
            }
        }
        if (g.isNight() || g.player.dead) return;
        for (Mob m : g.town) {
            if (m.homeX < 0) {
                int[] h = findHome(g);
                if (h == null) return;
                m.homeX = h[0];
                m.homeY = h[1];
                g.message(m.name + " the " + Mobs.NAME[m.type] + " has moved into a house.", Pal.UI_GOOD);
            }
        }
        int want = -1;
        if (!present(g, Mobs.GUIDE)) want = Mobs.GUIDE;
        else if (!present(g, Mobs.MERCHANT) && g.inv.money() >= 5000) want = Mobs.MERCHANT;
        else if (!present(g, Mobs.NURSE) && present(g, Mobs.MERCHANT) && g.player.lifeMax > 100) want = Mobs.NURSE;
        if (want < 0) return;
        int[] h = findHome(g);
        if (h == null) return;
        Mob m = create(g, want, h[0] * Tiles.T + Tiles.T / 2.0, (h[1] + 2) * Tiles.T);
        m.homeX = h[0];
        m.homeY = h[1];
        g.town.add(m);
        g.message(m.name + " the " + Mobs.NAME[want] + " has arrived!", Pal.UI_SEL);
    }

    static void died(Game g, Mob m) {
        if (m.dead) return;
        m.dead = true;
        g.burst(m.centerX(), m.centerY(), 0x8a1a1a, 20, 2.0);
        g.message(m.name + " the " + Mobs.NAME[m.type] + " was slain...", Pal.UI_BAD);
        if (g.talkTo == m) g.closeTalk();
    }

    // ---- behaviour ---------------------------------------------------------------

    static void walk(Mob m, Game g) {
        World w = g.world;
        m.passDoors = true;
        if (m.talking) {
            m.vx *= 0.7;
            m.dir = g.player.centerX() >= m.centerX() ? 1 : -1;
        } else {
            double home = m.homeX >= 0 ? m.homeX * Tiles.T + Tiles.T / 2.0 : m.ai[3];
            if (g.isNight() && m.homeX >= 0) {
                double dx = home - m.centerX();
                if (Math.abs(dx) > 10) {
                    m.dir = dx > 0 ? 1 : -1;
                    m.ai[1] = 1;
                } else {
                    m.ai[1] = 0;
                }
            } else if (--m.timer <= 0) {
                if (g.rnd.nextInt(10) < 6) {
                    m.ai[1] = 0;
                    m.timer = 120 + g.rnd.nextInt(240);
                } else {
                    m.ai[1] = 1;
                    double target = home + (g.rnd.nextDouble() - 0.5) * 20 * Tiles.T;
                    m.dir = target > m.centerX() ? 1 : -1;
                    m.timer = 90 + g.rnd.nextInt(200);
                }
            }
            if (m.ai[1] == 1) {
                // don't walk into water or lava or off cliffs
                int ax = (int) ((m.centerX() + m.dir * 8) / Tiles.T), ay = (int) ((m.y + m.h) / Tiles.T);
                boolean danger = w.liquidAt(ax, ay - 1) > 0 || w.liquidAt(ax, ay) > 0
                        || (!w.solid(ax, ay) && !w.solid(ax, ay + 1) && !w.solid(ax, ay + 2) && !w.isPlatform(ax, ay));
                if (danger) {
                    m.ai[1] = 0;
                    m.timer = 60;
                } else {
                    m.vx = m.dir * 0.45;
                }
            } else {
                m.vx *= 0.7;
            }
            if (m.hitWall && m.onGround && m.ai[1] == 1) {
                m.vy = -3.2;
                m.onGround = false;
                if (++m.ai[2] > 3) {
                    m.dir = -m.dir;
                    m.ai[2] = 0;
                }
            }
        }
        m.vy = Math.min(5, m.vy + 0.2);
        m.move(w);
        m.anim = m.onGround && Math.abs(m.vx) > 0.1 ? m.anim + Math.abs(m.vx) * 0.13 : 0;
    }

    // ---- talking ---------------------------------------------------------------------

    static String greeting(Game g, Mob m) {
        switch (m.type) {
            case Mobs.GUIDE: return "Hello! Ask me for help if you're stuck.";
            case Mobs.MERCHANT: return MERCHANT_LINES[g.rnd.nextInt(MERCHANT_LINES.length)];
            default: return NURSE_LINES[g.rnd.nextInt(NURSE_LINES.length)];
        }
    }

    static String guideTip(Game g) {
        return GUIDE_TIPS[g.rnd.nextInt(GUIDE_TIPS.length)];
    }

    /** Healing cost at the nurse, in copper. */
    static int healCost(Player p) {
        int missing = p.lifeMax - Math.max(0, p.life);
        int cost = missing * 8;
        if (p.onFire > 0 || p.poisoned > 0) cost += 200;
        return cost;
    }
}
