package mingeriacc;

/** Drawing of creatures. */
final class MobArt {
    private MobArt() {}

    private static final Sprite EYE = buildEye();
    private static final Sprite[] BAT = {buildBat(0), buildBat(1), buildBat(2)};
    private static final Humanoid.Look[] ZOMBIE_LOOK = {Humanoid.zombie(0), Humanoid.zombie(1)};
    private static final Humanoid.Look SKELETON_LOOK = Humanoid.skeleton();
    private static final Sprite[] HELLBAT = {BAT[0].recolor(0x4a3448, 0x8a2a14, 0x6a4a5a, 0xd0501a),
            BAT[1].recolor(0x4a3448, 0x8a2a14, 0x6a4a5a, 0xd0501a), BAT[2].recolor(0x4a3448, 0x8a2a14, 0x6a4a5a, 0xd0501a)};
    private static final Sprite[] HORNET = {buildHornet(0), buildHornet(1)};
    private static final Sprite EATER = buildEater();
    private static final Sprite[] DEMON = {buildDemon(0), buildDemon(1)};
    private static final Sprite[] BUNNY = {buildBunny(0), buildBunny(1)};
    private static final Humanoid.Look[] TOWN = new Humanoid.Look[3];
    private static final Humanoid.Look IMP = new Humanoid.Look();

    static {
        Humanoid.Look guide = new Humanoid.Look();
        guide.hair = 0x6a4020;
        guide.hairD = 0x4a2a14;
        guide.shirt = 0x4a8a3a;
        guide.shirtD = 0x346a2a;
        guide.pants = 0x5a4a3a;
        TOWN[0] = guide;
        Humanoid.Look merchant = new Humanoid.Look();
        merchant.hair = 0xd8d8d8;
        merchant.hairD = 0xa8a8a8;
        merchant.hairStyle = Humanoid.HAIR_BEARD;
        merchant.shirt = 0x7a4a2a;
        merchant.shirtD = 0x5a3418;
        merchant.pants = 0x3a3a4a;
        merchant.skin = 0xe8b088;
        TOWN[1] = merchant;
        Humanoid.Look nurse = new Humanoid.Look();
        nurse.hair = 0xc8402a;
        nurse.hairD = 0x8a2a1a;
        nurse.hairStyle = Humanoid.HAIR_LONG;
        nurse.shirt = 0xf0f0f4;
        nurse.shirtD = 0xc8c8d4;
        nurse.pants = 0xe8e8f0;
        nurse.pantsD = 0xc0c0cc;
        nurse.shoes = 0xd8d8e0;
        nurse.eye = 0x3a8a4a;
        TOWN[2] = nurse;
        IMP.skin = 0xa0406a;
        IMP.skinD = 0x7a2a4a;
        IMP.hair = 0x2a1020;
        IMP.hairD = 0x1a0810;
        IMP.hairStyle = Humanoid.HAIR_SPIKY;
        IMP.eye = 0xffd040;
        IMP.sclera = 0xff8a20;
        IMP.shirt = 0x5a1a3a;
        IMP.shirtD = 0x3a0e24;
        IMP.pants = 0x3a0e24;
        IMP.pantsD = 0x2a0818;
        IMP.shoes = 0x1a0810;
    }

    private static Sprite buildHornet(int f) {
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        m.put('k', 0x1a1410);
        m.put('y', 0xe8b830);
        m.put('Y', 0xffe070);
        m.put('w', 0xd8e8ff);
        m.put('e', 0xd02020);
        String[][] fr = {
            {"....ww.ww.....",
             "...wwwwwww....",
             "....wwkww.....",
             "..kkykykyk....",
             ".kYyykykykk...",
             "keYyykykykyk..",
             ".kyykykykyk...",
             "..kkkkkkkk.k..",
             "..........kk..",
             "...........k.."},
            {"..............",
             "..wwwwwwwww...",
             "...wwwkwww....",
             "..kkykykyk....",
             ".kYyykykykk...",
             "keYyykykykyk..",
             ".kyykykykyk...",
             "..kkkkkkkk.k..",
             "..........kk..",
             "...........k.."}};
        return Sprite.ascii(m, fr[f]);
    }

    private static Sprite buildEater() {
        Sprite s = new Sprite(16, 14);
        for (int y = 0; y < 14; y++)
            for (int x = 0; x < 16; x++) {
                double dx = (x + 0.5 - 7) / 7.5, dy = (y + 0.5 - 7) / 6.5;
                double d = dx * dx + dy * dy;
                if (d > 1) continue;
                int c = d > 0.75 ? 0x3a2a4a : dy < -0.3 ? 0x8a6aaa : 0x6a4a8a;
                // mouth on the right side
                if (x > 10 && Math.abs(dy) < 0.35 - (x - 10) * 0.02) c = 0x2a0a14;
                if (x > 10 && Math.abs(dy) < 0.4 && Math.abs(dy) > 0.28) c = 0xe8d8c0;
                s.set(x, y, c);
            }
        s.set(8, 4, 0xd0ff40);
        s.set(9, 4, 0xd0ff40);
        s.set(4, 3, 0x9a7aba);
        return s;
    }

    private static Sprite buildDemon(int f) {
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        m.put('k', 0x2a0806);
        m.put('r', 0xb02a1a);
        m.put('R', 0xe0503a);
        m.put('w', 0x5a1410);
        m.put('W', 0x7a2014);
        m.put('h', 0xe8d8c0);
        m.put('e', 0xffe040);
        String[][] fr = {
            {"ww.........h..h.......ww",
             "wWw........khhk......wWw",
             "wWWw.......kRRk.....wWWw",
             "wWWWw.....kReRek...wWWWw",
             ".wWWWw....kRRRRk..wWWWw.",
             "..wWWWw..krRRRRrk.wWWw..",
             "...wWWWkkrrRRRRrrkWWw...",
             "....wwkrrrrRRRRrrrkw....",
             "......krrrrrRRrrrrk.....",
             "......krrrrrrrrrrrk.....",
             ".......krrrrrrrrrk......",
             "........krrrrrrrk.......",
             "........kr.kk.rk........",
             "........kr....rk........",
             ".......kr......rk.......",
             ".......kk......kk.......",
             "........................",
             "........................"},
            {"...........h..h.........",
             "...........khhk.........",
             "...........kRRk.........",
             "..........kReRek........",
             "..........kRRRRk........",
             "wwww.....krRRRRrk...wwww",
             "wWWWwwwkkrrRRRRrrkwwWWWw",
             ".wWWWWkrrrrRRRRrrrkWWWw.",
             "..wWWWkrrrrrRRrrrrkWWw..",
             "...wWWkrrrrrrrrrrrkWw...",
             "....ww.krrrrrrrrrk.w....",
             "........krrrrrrrk.......",
             "........kr.kk.rk........",
             "........kr....rk........",
             ".......kr......rk.......",
             ".......kk......kk.......",
             "........................",
             "........................"}};
        return Sprite.ascii(m, fr[f]);
    }

    private static Sprite buildBunny(int f) {
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        m.put('w', 0xf0f0f4);
        m.put('g', 0xc8c8d4);
        m.put('p', 0xf0a0b0);
        m.put('k', 0x1a1a20);
        String[][] fr = {
            {".......ww.", ".......wp.", ".......wp.", "..gwwwwww.", ".gwwwwwwkw", "gwwwwwwwww", ".gwwwwgww.", "..g..g.g.."},
            {"......ww..", "......wp..", "......wp..", ".gwwwwwww.", "gwwwwwwwkw", ".gwwwwwwww", "..gw..gw..", "..g....g.."}};
        return Sprite.ascii(m, fr[f]);
    }

    private static Sprite buildEye() {
        Sprite s = new Sprite(18, 13);
        double cx = 11.5, cy = 6.5;
        // tendrils behind the eye
        for (int k = 0; k < 3; k++) {
            double yy = cy - 2.5 + k * 2.5;
            for (int x = 0; x < 7; x++) {
                int y = (int) Math.round(yy + Math.sin(x * 0.9 + k) * 0.8 + (k - 1) * x * 0.25);
                s.set(x + 1, y, x < 3 ? 0x7a1a1a : 0xa02828);
            }
        }
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d = Math.hypot(dx, dy);
                if (d > 6.2) continue;
                int c = d > 5.4 ? 0x8a3a3a : 0xece4e0;
                if (d <= 5.4 && dx + dy > 3.5) c = 0xc8bcb8;
                // veins
                if (d > 3.6 && d <= 5.4 && ((int) (Math.atan2(dy, dx) * 5 + 20)) % 4 == 0) c = 0xd06060;
                double ix = dx - 2.2, iy = dy;
                double di = Math.hypot(ix, iy);
                if (di < 2.9) c = di < 1.5 ? 0x101018 : 0x3a6ab0;
                if (di < 2.9 && di >= 2.2) c = 0x2a4a80;
                s.set(x, y, c);
            }
        s.set(13, 4, 0xffffff);
        s.set(12, 3, 0xffffff);
        return s;
    }

    private static Sprite buildBat(int frame) {
        String[][] f = {
            {"............",
             "dd........dd",
             "ddd..bb..ddd",
             ".dddbbbbddd.",
             "..ddbebbdd..",
             "....bbbb....",
             ".....bb.....",
             "............"},
            {"............",
             "............",
             ".....bb.....",
             "dddddbebdddd",
             ".dddbbbbddd.",
             "..d.bbbb.d..",
             ".....bb.....",
             "............"},
            {"............",
             "............",
             ".....bb.....",
             "....bebb....",
             "...dbbbbd...",
             "..ddbbbbdd..",
             ".ddd.bb.ddd.",
             "dd........dd"},
        };
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        m.put('d', 0x4a3448);
        m.put('b', 0x6a4a5a);
        m.put('e', 0xf0d040);
        return Sprite.ascii(m, f[frame]);
    }

    static void draw(Screen s, Mob m, int camX, int camY, int light, long ticks) {
        int flash = m.hitFlash > 0 ? 170 : 0;
        int sx = (int) Math.round(m.x) - camX, sy = (int) Math.round(m.y + m.stepOffset) - camY;
        if (m.onFire > 0) light = Pal.lerp(light, 0xffc080, 0.4);
        switch (m.type) {
            case Mobs.HORNET: {
                Sprite spr = HORNET[(int) (ticks / 3 % 2)];
                s.drawFx(spr, sx - 1, sy, m.dir < 0, light, 256, 0xffffff, flash);
                return;
            }
            case Mobs.EATER: {
                double cx = m.x + m.w / 2.0 - camX, cy = m.y + m.h / 2.0 - camY;
                s.drawRotated(EATER, 7, 7, cx, cy, m.rot, false, flashed(light, flash));
                return;
            }
            case Mobs.DEMON: {
                Sprite spr = DEMON[(int) (ticks / 8 % 2)];
                s.drawFx(spr, sx - 1, sy, m.dir < 0, 0xffffff, 256, 0xffffff, flash);
                return;
            }
            case Mobs.HELLBAT: {
                int f = (int) ((ticks / 4 + m.variant) % 4);
                s.drawFx(HELLBAT[f == 3 ? 1 : f], sx, sy, m.dir < 0, 0xffffff, 256, 0xffffff, flash);
                return;
            }
            case Mobs.BUNNY: {
                Sprite spr = BUNNY[m.onGround ? 0 : 1];
                s.drawFx(spr, sx, sy, m.dir < 0, light, 256, 0xffffff, flash);
                return;
            }
            case Mobs.FIRE_IMP: case Mobs.GUIDE: case Mobs.MERCHANT: case Mobs.NURSE: {
                Humanoid.Look look = m.type == Mobs.FIRE_IMP ? IMP : TOWN[m.type - Mobs.GUIDE];
                int c = sx + m.w / 2;
                boolean casting = m.type == Mobs.FIRE_IMP && m.attackTimer > 230;
                double arm = casting ? 2.4 : m.onGround ? Math.sin(m.anim) * 0.5 : 0.6;
                Humanoid.drawBody(s, c, sy, m.dir, look, m.anim, m.onGround, -arm * 0.8, m.type == Mobs.FIRE_IMP
                        ? Pal.lerp(light, 0xffffff, 0.3) : light, flash);
                Humanoid.drawFrontArm(arm, look);
                if (m.type == Mobs.FIRE_IMP) {
                    // horns
                    s.pset(c - 3, sy - 1, 0xe8d8c0);
                    s.pset(c + 2, sy - 1, 0xe8d8c0);
                    if (casting) s.glow(c + m.dir * 4, sy + 2, 5, 0xff7020, 150);
                }
                return;
            }
            default:
                break;
        }
        switch (Mobs.AI[m.type]) {
            case Mobs.AI_SLIME:
                slime(s, m, sx, sy, light, flash);
                break;
            case Mobs.AI_FIGHTER: {
                Humanoid.Look look = m.type == Mobs.SKELETON ? SKELETON_LOOK : ZOMBIE_LOOK[m.variant & 1];
                int c = sx + m.w / 2;
                double reach = 1.35 + Math.sin(m.anim * 0.5) * 0.08;
                Humanoid.drawBody(s, c, sy, m.dir, look, m.anim, m.onGround, reach + 0.1, light, flash);
                Humanoid.drawFrontArm(reach, look);
                break;
            }
            case Mobs.AI_FLYER: {
                double cx = m.x + m.w / 2.0 - camX, cy = m.y + m.h / 2.0 - camY;
                Sprite e = EYE;
                s.drawRotated(e, 11.5, 6.5, cx, cy, m.rot, false, flashed(light, flash));
                break;
            }
            default: {
                int f = (int) ((ticks / 5 + m.variant) % 4);
                Sprite b = BAT[f == 3 ? 1 : f];
                s.drawFx(b, sx, sy, m.dir < 0, light, 256, 0xffffff, flash);
            }
        }
    }

    private static int flashed(int light, int flash) {
        return flash > 0 ? Pal.lerp(light, 0xffffff, flash / 256.0) : light;
    }

    private static void slime(Screen s, Mob m, int sx, int sy, int light, int flash) {
        int base = Mobs.COLOR[m.type];
        // squash and stretch
        double sq;
        if (!m.onGround) sq = Math.max(-0.25, Math.min(0.2, m.vy * 0.06));
        else sq = m.ai[0] > 55 ? 0.18 : Math.sin(m.anim * 0.12) * 0.04;
        double w = m.w * (1 + sq), h = m.h * (1 - sq);
        double cx = sx + m.w / 2.0, bottom = sy + m.h;
        double top = bottom - h;
        int alpha = m.type == Mobs.BLACK_SLIME ? 235 : 190;
        if (m.type == Mobs.LAVA_SLIME) {
            light = 0xffffff;
            alpha = 230;
        }
        int dark = Pal.shade(base, 150), lite = Pal.lerp(base, 0xffffff, 0.45);
        for (int y = (int) Math.floor(top); y < (int) Math.ceil(bottom); y++)
            for (int x = (int) Math.floor(cx - w / 2); x < (int) Math.ceil(cx + w / 2); x++) {
                double nx = (x + 0.5 - cx) / (w / 2), ny = (y + 0.5 - (top + h * 0.62)) / (h * 0.62);
                if (ny > 0) ny = (y + 0.5 - (top + h * 0.62)) / (h * 0.38);
                double d = nx * nx + ny * ny;
                if (d > 1) continue;
                int c = d > 0.72 ? dark : base;
                if (nx < -0.2 && ny < -0.25 && d < 0.5) c = lite;
                c = Pal.mulLight(c, light);
                if (flash > 0) c = Pal.blend(c, 0xffffff, flash);
                s.pblend(x, y, c, d > 0.72 ? Math.min(256, alpha + 40) : alpha);
            }
        // eyes
        int ey = (int) Math.round(top + h * 0.45);
        int ex = (int) Math.round(cx + m.dir * w * 0.12);
        int eyeC = Pal.mulLight(0x141018, light);
        if (m.type == Mobs.PINKY) {
            s.pset(ex - 1, ey, eyeC);
            s.pset(ex + 1, ey, eyeC);
        } else {
            s.fill(ex - 3, ey, 2, 2, eyeC);
            s.fill(ex + 1, ey, 2, 2, eyeC);
            s.pset(ex - 3, ey, Pal.mulLight(0xffffff, light));
            s.pset(ex + 1, ey, Pal.mulLight(0xffffff, light));
        }
    }
}
