package mingeriacc;

/** The player character: movement, health and drawing. */
final class Player extends Body {
    static final int W = 10, H = 22;
    static final double MAX_SPEED = 1.5;
    static final double GRAVITY = 0.2;
    static final double MAX_FALL = 5.0;
    static final double JUMP_SPEED = 2.55;
    static final int JUMP_HOLD = 13;
    static final int MAX_LIFE_CAP = 400;

    final Humanoid.Look look = new Humanoid.Look();
    int dir = 1;
    double walkPhase;
    private int jumpTimer, jumpBuffer, coyote;
    boolean landedHard;  // for the landing sound
    boolean jumped;
    private double lastVy;

    // health
    int life = 100, lifeMax = 100;
    int immune;          // invincibility frames after a hit
    boolean dead;
    int respawnTimer;
    int sinceHurt = 10000;
    double regenAcc;
    int potionSickness;  // frames until healing items work again
    double fallStartY;
    int fallDamage;      // set when landing after a long fall

    // stats from equipment (set every frame by Game.applyEquipment)
    int defense;
    double moveSpeed = 1, jumpBoost = 0;
    double regenBonus, damageBonus;
    boolean sprint, doubleJump, noFall, fireImmune, swim, headLight;
    String setBonus = "";

    // liquids and debuffs
    static final int BREATH_MAX = 200;
    boolean wet, lavaWet, headWet;
    int breath = BREATH_MAX;
    int onFire, poisoned;
    int runTimer;
    private boolean usedDouble;
    boolean didDoubleJump;
    int spawnX = -1, spawnY = -1; // bed

    // item use (swing, stab, shoot...)
    int useTimer, useDuration, useItem, useStyle;
    int swingId;
    double aimAngle;     // screen angle towards the mouse when the use started

    Player() {
        w = W;
        h = H;
    }

    void spawnAt(World wd) {
        boolean bed = spawnX >= 0 && wd.tile(spawnX, spawnY) == Tiles.BED;
        int sx = bed ? spawnX : wd.spawnX, sy = bed ? spawnY + 1 : wd.spawnY;
        x = sx * T + T / 2.0 - W / 2.0;
        y = sy * T - H;
        vx = vy = 0;
        fallStartY = y;
        onFire = poisoned = 0;
        breath = BREATH_MAX;
    }

    /** Whether any tile the body covers holds enough liquid of the type. */
    boolean inLiquid(World wd, int type, double yFrom, double yTo) {
        int tx0 = (int) Math.floor(x / T), tx1 = (int) Math.floor((x + w - 0.001) / T);
        int ty0 = (int) Math.floor(yFrom / T), ty1 = (int) Math.floor((yTo - 0.001) / T);
        for (int ty = ty0; ty <= ty1; ty++)
            for (int tx = tx0; tx <= tx1; tx++)
                if (wd.liquidAt(tx, ty) > 60 && wd.liquidType(tx, ty) == type) return true;
        return false;
    }

    boolean using() { return useTimer > 0; }

    void update(World wd, Input in, boolean controlsEnabled) {
        boolean left = controlsEnabled && in.left();
        boolean right = controlsEnabled && in.right();
        boolean jumpHeld = controlsEnabled && in.jump();
        jumped = false;
        didDoubleJump = false;
        wet = inLiquid(wd, Liquids.WATER, y + 6, y + H);
        lavaWet = inLiquid(wd, Liquids.LAVA, y + 4, y + H);
        headWet = inLiquid(wd, Liquids.WATER, y + 2, y + 5);
        boolean sprinting = sprint && runTimer > 50 && onGround;
        double maxSpeed = MAX_SPEED * moveSpeed * (sprinting ? 1.55 : 1) * (wet && !swim ? 0.55 : lavaWet ? 0.5 : 1);
        if (onGround && Math.abs(vx) > MAX_SPEED * 0.9 && (left || right)) runTimer++;
        else if (!left && !right || Math.abs(vx) < 0.5) runTimer = 0;

        // horizontal movement
        double accel = onGround ? 0.11 : 0.08;
        if (left && !right) {
            if (vx > -maxSpeed) vx = Math.max(-maxSpeed, vx - accel * (vx > 0 ? 2 : 1));
            if (!using()) dir = -1;
        } else if (right && !left) {
            if (vx < maxSpeed) vx = Math.min(maxSpeed, vx + accel * (vx < 0 ? 2 : 1));
            if (!using()) dir = 1;
        } else {
            vx *= onGround ? 0.78 : 0.96;
            if (Math.abs(vx) < 0.03) vx = 0;
        }
        if (Math.abs(vx) > maxSpeed * 1.5) vx *= 0.92; // knockback fades

        // jump: input buffer + "coyote time" + higher jump while held
        if (controlsEnabled && in.jumpPressed()) jumpBuffer = 7;
        else if (jumpBuffer > 0) jumpBuffer--;
        if (onGround) {
            coyote = 6;
            usedDouble = false;
        } else if (coyote > 0) {
            coyote--;
        }
        if (wet && controlsEnabled && in.jumpPressed() && !onGround) {
            // a swim stroke
            vy = -2.0;
            jumpBuffer = 0;
        } else if (jumpBuffer > 0 && coyote == 0 && !onGround && doubleJump && !usedDouble && in.jumpPressed()) {
            vy = -JUMP_SPEED;
            jumpTimer = JUMP_HOLD * 3 / 4;
            jumpBuffer = 0;
            usedDouble = true;
            didDoubleJump = true;
        }
        if (jumpBuffer > 0 && coyote > 0) {
            vy = -JUMP_SPEED;
            jumpTimer = JUMP_HOLD + (int) Math.round(jumpBoost);
            jumpBuffer = 0;
            coyote = 0;
            onGround = false;
            jumped = true;
        }
        if (jumpTimer > 0) {
            if (jumpHeld) {
                vy = -JUMP_SPEED;
                jumpTimer--;
            } else {
                jumpTimer = 0;
            }
        }

        // dropping through platforms
        if (controlsEnabled && in.downKey() && onGround && standingOnPlatform(wd)) dropTimer = 12;

        if (wet || lavaWet) {
            vy = Math.min(1.6, vy + GRAVITY * 0.35);
            if (swim && jumpHeld) vy = Math.max(-2.0, vy - 0.3);
        } else {
            vy = Math.min(MAX_FALL, vy + GRAVITY);
        }
        lastVy = vy;

        boolean wasGround = onGround;
        move(wd);
        if (hitCeiling) jumpTimer = 0;
        landedHard = !wasGround && onGround && lastVy > 3.2;

        // fall damage: more than 25 tiles
        fallDamage = 0;
        if (!wasGround && onGround && !noFall) {
            int tiles = (int) ((y - fallStartY) / T);
            if (tiles > 25) fallDamage = (tiles - 25) * 10;
        }
        if (onGround || vy < 0 || wet) fallStartY = y;

        // breath under water
        if (headWet && !swim) {
            if (breath > 0 && age() % 6 == 0) breath--;
        } else {
            breath = Math.min(BREATH_MAX, breath + 3);
        }

        if (onGround && Math.abs(vx) > 0.1) walkPhase += Math.abs(vx) * 0.13;
        else if (onGround) walkPhase = 0;

        if (useTimer > 0) useTimer--;
        if (immune > 0) immune--;
        if (potionSickness > 0) potionSickness--;
        if (onFire > 0) onFire--;
        if (poisoned > 0) poisoned--;
        sinceHurt++;
        ticks++;
    }

    private long ticks;

    private long age() {
        return ticks;
    }

    /** Natural life regeneration, faster the longer since the last hit. */
    void regenerate() {
        if (dead || life >= lifeMax || onFire > 0 || poisoned > 0) {
            regenAcc = 0;
            return;
        }
        double perSec = 0.3 + Math.min(1.0, sinceHurt / 1800.0) * 1.7;
        if (Math.abs(vx) < 0.05 && onGround) perSec *= 1.4;
        perSec += regenBonus;
        regenAcc += perSec / 60.0;
        while (regenAcc >= 1) {
            regenAcc -= 1;
            if (life < lifeMax) life++;
        }
    }

    /** Swing angle: 0 = down, pi/2 = forward, pi = up (when facing right). */
    double swingAngle() {
        if (useDuration <= 0) return 0;
        double t = 1.0 - useTimer / (double) useDuration;
        return 3.6 - t * 3.3;
    }

    /** Arm angle (for Humanoid) that points along a screen angle. */
    double armAngleFor(double screenAngle) {
        return Math.atan2(dir * Math.cos(screenAngle), Math.sin(screenAngle));
    }

    /** Shoulder position in world pixels. */
    double shoulderX() { return centerX() + dir; }
    double shoulderY() { return y + 8; }

    /** How far a stab reaches (0..1) at the moment. */
    double stabReach() {
        if (useDuration <= 0) return 0;
        double t = 1.0 - useTimer / (double) useDuration;
        return t < 0.35 ? t / 0.35 : Math.max(0, 1 - (t - 0.35) / 0.65);
    }

    // ---- drawing ---------------------------------------------------------

    void draw(Screen s, int camX, int camY, int light, int heldItem) {
        if (dead) return;
        if (immune > 0 && (immune / 3) % 2 == 1) return;
        int px = (int) Math.round(x) - camX;
        int top = (int) Math.round(y + stepOffset) - camY;
        int c = px + W / 2;

        double a = onGround ? Math.sin(walkPhase) * 0.6 : 0;
        double backArm = onGround ? -a * 0.8 : -0.5;
        if (using()) backArm = 0.3;
        Humanoid.drawBody(s, c, top, dir, look, walkPhase, onGround, backArm, light, 0);

        double sx = c + dir, sy = top + 8;
        boolean holdTorch = heldItem == Items.TORCH && !using();
        if (using() && useItem > 0) {
            Sprite icon = Items.ICON[useItem];
            int style = useStyle;
            if (style == Items.S_SWING) {
                double A = swingAngle();
                double hx = sx + Math.sin(A) * 5 * dir, hy = sy + Math.cos(A) * 5;
                double armScreen = Math.atan2(Math.cos(A), Math.sin(A)); // when facing right
                double rot = armScreen - Math.atan2(-1, 1);
                double pivX = 1.5, pivY = icon.h - 1.5;
                if (!Items.isWeapon(useItem)) {
                    pivX = 0.5;
                    pivY = icon.h - 0.5;
                }
                s.drawRotated(icon, pivX, pivY, hx, hy, rot, dir < 0, light);
                Humanoid.drawItemArm(A, look);
            } else if (style == Items.S_STAB) {
                double A = armAngleFor(aimAngle);
                double reach = 2 + stabReach() * 5;
                double hx = sx + Math.cos(aimAngle) * reach, hy = sy + Math.sin(aimAngle) * reach;
                s.drawRotated(icon, 1.5, icon.h - 1.5, hx, hy, aimAngle + Math.PI / 4, false, light);
                Humanoid.drawItemArm(A, look);
            } else if (style == Items.S_SHOOT) {
                double A = armAngleFor(aimAngle);
                double hx = sx + Math.cos(aimAngle) * 5, hy = sy + Math.sin(aimAngle) * 5;
                s.drawRotated(icon, 2.5, icon.h / 2.0, hx, hy, aimAngle, false, light);
                Humanoid.drawItemArm(A, look);
            } else {
                // eat or hold up
                double A = 2.6;
                double hx = sx + Math.sin(A) * 5 * dir, hy = sy + Math.cos(A) * 5;
                s.drawLit(icon, (int) Math.round(hx) - icon.w / 2, (int) Math.round(hy) - icon.h, dir < 0, light);
                Humanoid.drawItemArm(A, look);
            }
        } else if (holdTorch) {
            double A = 2.1;
            double hx = sx + Math.sin(A) * 5 * dir, hy = sy + Math.cos(A) * 5;
            Sprite icon = Items.ICON[Items.TORCH];
            s.draw(icon, (int) Math.round(hx) - 3, (int) Math.round(hy) - 8, false, 256);
            Humanoid.drawItemArm(A, look);
        } else {
            double frontArm = onGround ? a * 0.8 : 0.6;
            Humanoid.drawFrontArm(frontArm, look);
        }
    }
}
