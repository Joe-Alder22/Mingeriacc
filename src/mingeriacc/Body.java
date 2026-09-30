package mingeriacc;

/** Something that moves in the world and collides with tiles: the player and creatures. */
abstract class Body {
    static final int T = Tiles.T;

    double x, y, vx, vy;
    int w, h;
    boolean onGround, hitWall, hitCeiling;
    int dropTimer;       // falls through platforms while > 0
    double stepOffset;   // smooths out step-ups when drawing
    boolean canStepUp = true;
    boolean usePlatforms = true;
    boolean passDoors;   // town folk walk through closed doors
    boolean noClip;      // flies through tiles

    double centerX() { return x + w / 2.0; }
    double centerY() { return y + h / 2.0; }

    boolean collides(World wd, double px, double py) {
        int tx0 = (int) Math.floor(px / T), tx1 = (int) Math.floor((px + w - 0.001) / T);
        int ty0 = (int) Math.floor(py / T), ty1 = (int) Math.floor((py + h - 0.001) / T);
        for (int ty = ty0; ty <= ty1; ty++)
            for (int tx = tx0; tx <= tx1; tx++)
                if (wd.solid(tx, ty) && !(passDoors && wd.tile(tx, ty) == Tiles.DOOR_CLOSED)) return true;
        return false;
    }

    /** Moves with the current velocity and resolves tile collisions. */
    void move(World wd) {
        hitWall = hitCeiling = false;
        if (noClip) {
            x += vx;
            y += vy;
            onGround = false;
            return;
        }
        moveX(wd, vx);
        moveY(wd, vy);
        if (dropTimer > 0) dropTimer--;
        stepOffset *= 0.72;
        if (Math.abs(stepOffset) < 0.3) stepOffset = 0;
        // stay inside the world
        if (x < 0) { x = 0; vx = 0; }
        if (x > wd.w * T - w) { x = wd.w * T - w; vx = 0; }
        if (y < 0) { y = 0; vy = Math.max(0, vy); }
        if (y > wd.h * T - h) { y = wd.h * T - h; vy = 0; onGround = true; }
    }

    void moveX(World wd, double dx) {
        if (dx == 0) return;
        double nx = x + dx;
        if (!collides(wd, nx, y)) {
            x = nx;
            return;
        }
        // automatically step up ledges one tile high
        if (onGround && canStepUp) {
            for (int up = 1; up <= T; up++) {
                if (!collides(wd, nx, y - up)) {
                    x = nx;
                    y -= up;
                    stepOffset += up;
                    return;
                }
            }
        }
        if (dx > 0) {
            int tc = (int) Math.floor((nx + w - 0.001) / T);
            x = tc * T - w;
        } else {
            int tc = (int) Math.floor(nx / T);
            x = (tc + 1) * T;
        }
        vx = 0;
        hitWall = true;
    }

    void moveY(World wd, double dy) {
        onGround = false;
        double ny = y + dy;
        if (dy > 0) {
            int tx0 = (int) Math.floor(x / T), tx1 = (int) Math.floor((x + w - 0.001) / T);
            int row = (int) Math.floor((ny + h - 0.001) / T);
            boolean hit = collides(wd, x, ny);
            if (!hit && dropTimer == 0 && usePlatforms) {
                double top = row * T;
                if (y + h <= top + 0.01) {
                    for (int tx = tx0; tx <= tx1; tx++)
                        if (wd.isPlatform(tx, row)) { hit = true; break; }
                }
            }
            if (hit) {
                y = row * T - h;
                // if a solid tile was already higher up, find the right spot
                while (collides(wd, x, y) && y > 0) y -= T;
                vy = 0;
                onGround = true;
            } else {
                y = ny;
            }
        } else if (dy < 0) {
            if (collides(wd, x, ny)) {
                int row = (int) Math.floor(ny / T);
                y = (row + 1) * T;
                vy = 0;
                hitCeiling = true;
            } else {
                y = ny;
            }
        }
        if (!onGround && vy >= 0) {
            // standing on something (e.g. exactly at an edge)?
            if (collides(wd, x, y + 0.5)) onGround = true;
            else if (dropTimer == 0 && usePlatforms && standingOnPlatform(wd)) onGround = true;
        }
    }

    boolean standingOnPlatform(World wd) {
        double bottom = y + h;
        if (Math.abs(bottom - Math.round(bottom / T) * T) > 0.05) return false;
        int row = (int) Math.round(bottom / T);
        int tx0 = (int) Math.floor(x / T), tx1 = (int) Math.floor((x + w - 0.001) / T);
        for (int tx = tx0; tx <= tx1; tx++) if (wd.isPlatform(tx, row)) return true;
        return false;
    }

    /** Whether the box overlaps a tile. */
    boolean overlapsTile(int tx, int ty) {
        return x < (tx + 1) * T && x + w > tx * T && y < (ty + 1) * T && y + h > ty * T;
    }

    boolean intersects(double ox, double oy, double ow, double oh) {
        return x < ox + ow && x + w > ox && y < oy + oh && y + h > oy;
    }

    boolean intersects(Body o) {
        return intersects(o.x, o.y, o.w, o.h);
    }

    boolean contains(double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}
