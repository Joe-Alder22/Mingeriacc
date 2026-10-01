package mingeriacc;

/** Items dropped on the ground, and particles. */
final class Entities {
    private Entities() {}

    static final class Drop {
        static final int SIZE = 6;
        double x, y, vx, vy;
        int item, count;
        int age, pickupDelay;
        boolean dead;

        Drop(double x, double y, int item, int count) {
            this.x = x;
            this.y = y;
            this.item = item;
            this.count = count;
        }

        void update(World w, Player p, boolean canTake) {
            age++;
            if (pickupDelay > 0) pickupDelay--;
            double pcx = p.centerX(), pcy = p.centerY();
            double dx = pcx - (x + SIZE / 2.0), dy = pcy - (y + SIZE / 2.0);
            double dist = Math.hypot(dx, dy);
            double range = Items.isCoin(item) ? 56 : 42;
            boolean magnet = canTake && !p.dead && pickupDelay == 0 && dist < range;
            if (magnet) {
                // pulled towards the player
                double k = 0.35 / Math.max(1, dist);
                vx = vx * 0.85 + dx * k * 2.2;
                vy = vy * 0.85 + dy * k * 2.2;
                x += vx;
                y += vy;
                return;
            }
            vy = Math.min(4, vy + 0.15);
            double nx = x + vx;
            if (hit(w, nx, y)) vx = -vx * 0.3;
            else x = nx;
            double ny = y + vy;
            if (hit(w, x, ny)) {
                if (vy > 0) {
                    y = Math.floor((ny + SIZE) / Tiles.T) * Tiles.T - SIZE;
                    if (hit(w, x, y)) y = ny - vy;
                    vx *= 0.7;
                }
                vy = 0;
            } else {
                y = ny;
            }
            // stuck inside a tile (e.g. a tile was placed on top): lift it up
            if (hit(w, x, y)) y -= 1;
        }

        private boolean hit(World w, double px, double py) {
            int T = Tiles.T;
            int tx0 = (int) Math.floor(px / T), tx1 = (int) Math.floor((px + SIZE - 0.01) / T);
            int ty0 = (int) Math.floor(py / T), ty1 = (int) Math.floor((py + SIZE - 0.01) / T);
            for (int ty = ty0; ty <= ty1; ty++)
                for (int tx = tx0; tx <= tx1; tx++) {
                    if (w.solid(tx, ty)) return true;
                    if (w.isPlatform(tx, ty) && vy >= 0 && py + SIZE <= ty * T + 2 + vy
                            && py + SIZE > ty * T && y + SIZE <= ty * T + 0.5) return true;
                }
            return false;
        }
    }

    static final class Particle {
        double x, y, vx, vy;
        int life, maxLife, color;
        double gravity;
        boolean glow;       // drawn as light (ignores darkness)
        boolean noCollide;
        boolean settle;     // comes to rest and fades where it lands (snowflakes, leaves)
        int size = 1;
        double drag = 1;

        Particle(double x, double y, double vx, double vy, int life, int color, double gravity) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = this.maxLife = life;
            this.color = color;
            this.gravity = gravity;
        }

        boolean update(World w) {
            vy += gravity;
            vx *= drag;
            vy *= drag;
            x += vx;
            y += vy;
            if (!noCollide && w.solid((int) Math.floor(x / Tiles.T), (int) Math.floor(y / Tiles.T))) {
                x -= vx;
                y -= vy;
                if (settle) {
                    vx = vy = gravity = 0;
                    noCollide = true;
                    life = Math.min(life, 50);
                } else {
                    vx *= -0.3;
                    vy *= -0.3;
                    life -= 2;
                }
            }
            return --life > 0;
        }
    }
}
