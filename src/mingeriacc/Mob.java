package mingeriacc;

/** A creature in the world (enemy, critter, boss or town folk). Behaviour and looks are in Mobs and MobArt. */
final class Mob extends Body {
    final int type;
    int life, lifeMax, damage, defense;
    double kbRes;              // 1 = full knockback, 0 = none
    int dir = 1;
    final double[] ai = new double[6];
    int timer;
    int hitFlash;              // frames of white flash after a hit
    int lastSwing = -1;        // swing that last hit (one hit per swing)
    int hurtTimer;             // frames since the last hit
    boolean dead;
    double anim;
    double rot;                // drawing rotation (flyers)
    int variant;
    int offscreen;             // frames spent far away
    int onFire, poisoned, frostburn; // debuff frames left
    int attackTimer;
    int bash;                  // frames spent pushing against a closed door
    int jumpWait;              // frames until the next jump towards a platform
    // town folk
    int homeX = -1, homeY = -1;
    String name = "";
    int immune;
    boolean talking;

    Mob(int type, double cx, double bottom) {
        this.type = type;
        w = Mobs.W[type];
        h = Mobs.H[type];
        x = cx - w / 2.0;
        y = bottom - h;
        life = lifeMax = Mobs.LIFE[type];
        damage = Mobs.DAMAGE[type];
        defense = Mobs.DEFENSE[type];
        kbRes = Mobs.KB_RES[type];
        hurtTimer = 1000;
        noClip = Mobs.NO_CLIP[type];
    }
}
