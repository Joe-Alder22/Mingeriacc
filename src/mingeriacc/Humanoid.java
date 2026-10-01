package mingeriacc;

/**
 * Procedural drawing of human-shaped characters (the player, zombies,
 * skeletons and town folk). The figure is 10 x 22 pixels with swinging limbs.
 */
final class Humanoid {
    private Humanoid() {}

    static final int HAIR_SHORT = 0, HAIR_LONG = 1, HAIR_NONE = 2, HAIR_BEARD = 3, HAIR_SPIKY = 4;

    /** Colours and style of a character. */
    static final class Look {
        int skin = Pal.SKIN, skinD = Pal.SKIN_D, hair = Pal.HAIR, hairD = Pal.HAIR_D, eye = 0x2a4a8a,
                sclera = 0xf4f4f4, shirt = Pal.SHIRT, shirtD = Pal.SHIRT_D, pants = Pal.PANTS,
                pantsD = Pal.PANTS_D, shoes = Pal.SHOES;
        int hairStyle = HAIR_SHORT;
        boolean skeleton;
        /** Armour colour ramps (outline, dark, mid, light, highlight) or null. */
        int[] helm, body, legs;
        /** Head gear drawn as a pointy hat (in the helm colours) instead of a helmet. */
        boolean pointyHat;

        Look copy() {
            Look l = new Look();
            l.assign(this);
            return l;
        }

        /** Takes over the colours and style of another look (not the armour). */
        void assign(Look o) {
            skin = o.skin; skinD = o.skinD; hair = o.hair; hairD = o.hairD; eye = o.eye; sclera = o.sclera;
            shirt = o.shirt; shirtD = o.shirtD; pants = o.pants; pantsD = o.pantsD; shoes = o.shoes;
            hairStyle = o.hairStyle; skeleton = o.skeleton;
        }
    }

    static Look zombie(int variant) {
        Look l = new Look();
        l.skin = variant == 1 ? 0x8aa080 : 0x74a06a;
        l.skinD = Pal.shade(l.skin, 190);
        l.hair = variant == 1 ? 0x5a4a3a : 0x2e2e22;
        l.hairD = Pal.shade(l.hair, 170);
        l.eye = 0xd02020;
        l.sclera = 0xd8d890;
        l.shirt = variant == 1 ? 0x7a4a3a : 0x3e5a86;
        l.shirtD = Pal.shade(l.shirt, 180);
        l.pants = 0x4a3e32;
        l.pantsD = 0x322a22;
        l.shoes = 0x2a2420;
        l.hairStyle = variant == 1 ? HAIR_LONG : HAIR_SPIKY;
        return l;
    }

    static Look skeleton() {
        Look l = new Look();
        l.skeleton = true;
        l.skin = 0xe2dcc6;
        l.skinD = 0xaaa28a;
        l.eye = 0x101010;
        l.sclera = 0x101010;
        l.shirt = 0xe2dcc6;
        l.shirtD = 0xaaa28a;
        l.pants = 0xe2dcc6;
        l.pantsD = 0xaaa28a;
        l.shoes = 0xc8c0a8;
        l.hairStyle = HAIR_NONE;
        return l;
    }

    // ---- drawing state (single-threaded rendering) ----------------------------

    private static Screen scr;
    private static int c, py0, dirv, light, flash;

    private static int col(int rgb) {
        int v = Pal.mulLight(rgb, light);
        return flash > 0 ? Pal.blend(v, 0xffffff, flash) : v;
    }

    private static void rectM(int ox, int oy, int w, int h, int color) {
        if (dirv > 0) scr.fill(c + ox, py0 + oy, w, h, col(color));
        else scr.fill(c - ox - w, py0 + oy, w, h, col(color));
    }

    private static void px(int ox, int oy, int color) {
        rectM(ox, oy, 1, 1, color);
    }

    /**
     * Draws the figure except the front arm. c = centre x on screen, top = top y,
     * walk = walk animation phase, backArm = back arm angle (0 = down, pi/2 = forward).
     */
    static void drawBody(Screen s, int cx, int top, int dir, Look L, double walk, boolean onGround,
                         double backArm, int lightColor, int flashAmount) {
        scr = s;
        c = cx;
        py0 = top;
        dirv = dir;
        light = lightColor;
        flash = flashAmount;

        double a = onGround ? Math.sin(walk) * 0.6 : 0;
        double frontLeg = onGround ? a : 0.45;
        double backLeg = onGround ? -a : -0.3;

        int[] lg = L.legs;
        int pants = lg != null ? lg[2] : L.pants, pantsD = lg != null ? lg[1] : L.pantsD;
        int shoes = lg != null ? lg[1] : L.shoes;
        int[] bd = L.body;
        int shirt = bd != null ? bd[2] : L.shirt, shirtD = bd != null ? bd[1] : L.shirtD;

        // back leg and back arm
        drawLeg(backLeg, pantsD, shoes, L.skeleton);
        s.limb(c - dirv, py0 + 8, backArm * dirv, 6, L.skeleton ? 1 : 2, col(shirtD));
        handAt(c - dirv, py0 + 8, backArm * dirv, 6, L.skinD);

        // body
        if (L.skeleton) {
            rectM(-1, 7, 2, 7, L.skinD);
            for (int y = 8; y <= 12; y += 2) rectM(-3, y, 6, 1, L.skin);
            rectM(-3, 13, 6, 1, L.skinD);
        } else {
            rectM(-3, 7, 6, 7, shirt);
            rectM(-3, 7, 1, 7, shirtD);
            rectM(2, 8, 1, 5, bd != null ? bd[3] : Pal.lerp(shirt, 0xffffff, 0.15));
            if (bd != null) {
                rectM(-2, 8, 4, 1, bd[3]);
                rectM(-2, 11, 4, 1, bd[1]);
            }
            rectM(-3, 13, 6, 1, pantsD);
            rectM(-3, 14, 6, 1, pants);
        }
        drawLeg(frontLeg, pants, shoes, L.skeleton);

        // head
        rectM(-3, 1, 6, 6, L.skin);
        rectM(-3, 6, 6, 1, L.skinD);
        rectM(-3, 1, 1, 5, L.skinD);
        if (L.skeleton) {
            rectM(0, 3, 2, 2, 0x101010);
            rectM(-1, 6, 3, 1, 0x3a3630);
        } else {
            px(0, 3, L.sclera);
            px(0, 4, L.sclera);
            px(1, 3, L.eye);
            px(1, 4, L.eye);
            px(2, 5, L.skinD);
        }
        int[] hm = L.helm;
        if (hm != null && !L.pointyHat) {
            rectM(-3, 0, 6, 3, hm[2]);
            rectM(-4, 1, 1, 4, hm[1]);
            rectM(-3, 0, 6, 1, hm[3]);
            rectM(-2, 0, 2, 1, hm[4]);
            rectM(-3, 3, 6, 1, hm[1]);
            rectM(2, 1, 1, 2, hm[1]);
        } else {
            drawHair(L);
            if (hm != null) {
                // pointy hat with a golden band, the tip bending backwards
                rectM(-5, 1, 9, 1, hm[1]);
                rectM(-4, 0, 7, 1, 0xe8c030);
                rectM(-3, -1, 6, 1, hm[2]);
                rectM(-3, -2, 5, 1, hm[2]);
                rectM(-2, -2, 2, 1, hm[3]);
                rectM(-3, -3, 4, 1, hm[2]);
                rectM(-3, -4, 3, 1, hm[3]);
                rectM(-4, -5, 2, 1, hm[2]);
                px(-5, -6, hm[1]);
            }
        }
    }

    private static void drawHair(Look L) {
        switch (L.hairStyle) {
            case HAIR_SHORT:
                rectM(-3, 0, 6, 2, L.hair);
                rectM(-4, 1, 2, 4, L.hair);
                rectM(-2, 0, 3, 1, Pal.lerp(L.hair, 0xffffff, 0.2));
                rectM(1, 2, 1, 1, L.hairD);
                break;
            case HAIR_LONG:
                rectM(-3, 0, 6, 2, L.hair);
                rectM(-4, 1, 2, 7, L.hair);
                rectM(-3, 6, 1, 3, L.hairD);
                rectM(-2, 0, 3, 1, Pal.lerp(L.hair, 0xffffff, 0.2));
                break;
            case HAIR_BEARD:
                rectM(-3, 0, 6, 1, L.hair);
                rectM(-4, 1, 2, 3, L.hair);
                rectM(-1, 5, 4, 2, L.hair);
                rectM(0, 7, 2, 1, L.hairD);
                break;
            case HAIR_SPIKY:
                rectM(-3, 0, 6, 1, L.hair);
                rectM(-4, 1, 2, 3, L.hair);
                px(-2, -1, L.hair);
                px(0, -1, L.hair);
                px(2, 0, L.hairD);
                break;
            default:
                break;
        }
    }

    /** Draws the front arm at an angle (0 = down, pi/2 = forward, pi = up). */
    static void drawFrontArm(double angle, Look L) {
        int shirt = L.body != null ? L.body[2] : L.shirt;
        scr.limb(c + dirv, py0 + 8, angle * dirv, 6, L.skeleton ? 1 : 2, col(shirt));
        handAt(c + dirv, py0 + 8, angle * dirv, 6, L.skin);
    }

    /** Shorter front arm used while holding an item. */
    static void drawItemArm(double angle, Look L) {
        int shirt = L.body != null ? L.body[2] : L.shirt;
        scr.limb(c + dirv, py0 + 8, angle * dirv, 5, L.skeleton ? 1 : 2, col(shirt));
        handAt(c + dirv, py0 + 8, angle * dirv, 5, L.skin);
    }

    private static void drawLeg(double angle, int color, int shoes, boolean skeleton) {
        double hx = c + 0.5 * dirv, hy = py0 + 14;
        double ang = angle * dirv;
        scr.limb(hx, hy, ang, 6, skeleton ? 1 : 2, col(color));
        int ex = (int) Math.floor(hx + Math.sin(ang) * 6);
        int ey = (int) Math.floor(hy + Math.cos(ang) * 6);
        scr.fill(dirv > 0 ? ex - 1 : ex - 1, ey, 3, 2, col(shoes));
        scr.fill(dirv > 0 ? ex + 1 : ex - 1, ey, 1, 1, col(Pal.lerp(shoes, 0xffffff, 0.2)));
    }

    private static void handAt(double sx, double sy, double ang, double len, int color) {
        int hx = (int) Math.floor(sx + Math.sin(ang) * len - 0.5);
        int hy = (int) Math.floor(sy + Math.cos(ang) * len);
        scr.fill(hx, hy, 2, 2, col(color));
    }
}
