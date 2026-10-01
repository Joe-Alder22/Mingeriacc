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
    private static final Humanoid.Look[] TOWN = new Humanoid.Look[Mobs.COUNT];
    private static final Humanoid.Look IMP = new Humanoid.Look();
    private static final Sprite[] BOSS_EYE = {buildBossEye(false, 0), buildBossEye(false, 1)};
    private static final Sprite[] BOSS_MOUTH = {buildBossEye(true, 0), buildBossEye(true, 1)};
    private static final Sprite SERVANT = buildServant();
    private static final Sprite[] DRIPPLER = {buildDrippler(0), buildDrippler(1)};
    private static final Sprite[] PENGUIN = {buildPenguin(0), buildPenguin(1)};
    private static final Sprite[] ICE_BAT = {BAT[0].recolor(0x4a3448, 0x3a5a8a, 0x6a4a5a, 0x8ac0e8, 0xf0d040, 0xffffff),
            BAT[1].recolor(0x4a3448, 0x3a5a8a, 0x6a4a5a, 0x8ac0e8, 0xf0d040, 0xffffff),
            BAT[2].recolor(0x4a3448, 0x3a5a8a, 0x6a4a5a, 0x8ac0e8, 0xf0d040, 0xffffff)};
    private static final Humanoid.Look BLOOD_ZOMBIE = Humanoid.zombie(0);
    private static final Humanoid.Look FROZEN_ZOMBIE = Humanoid.zombie(1);

    static {
        Humanoid.Look guide = new Humanoid.Look();
        guide.hair = 0x6a4020;
        guide.hairD = 0x4a2a14;
        guide.shirt = 0x4a8a3a;
        guide.shirtD = 0x346a2a;
        guide.pants = 0x5a4a3a;
        TOWN[Mobs.GUIDE] = guide;
        Humanoid.Look merchant = new Humanoid.Look();
        merchant.hair = 0xd8d8d8;
        merchant.hairD = 0xa8a8a8;
        merchant.hairStyle = Humanoid.HAIR_BEARD;
        merchant.shirt = 0x7a4a2a;
        merchant.shirtD = 0x5a3418;
        merchant.pants = 0x3a3a4a;
        merchant.skin = 0xe8b088;
        TOWN[Mobs.MERCHANT] = merchant;
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
        TOWN[Mobs.NURSE] = nurse;
        Humanoid.Look arcanist = new Humanoid.Look();
        arcanist.hair = 0xe8e8f0;
        arcanist.hairD = 0xb8b8c8;
        arcanist.hairStyle = Humanoid.HAIR_BEARD;
        arcanist.shirt = 0x5a2a9a;
        arcanist.shirtD = 0x3e1a6e;
        arcanist.pants = 0x4a2280;
        arcanist.pantsD = 0x341660;
        arcanist.shoes = 0x2a1a3a;
        arcanist.eye = 0x6a3ab0;
        arcanist.helm = ItemArt.ARCANE;
        arcanist.pointyHat = true;
        TOWN[Mobs.ARCANIST] = arcanist;
        Humanoid.Look dryad = new Humanoid.Look();
        dryad.hair = 0x7ac83a;
        dryad.hairD = 0x4a8a24;
        dryad.hairStyle = Humanoid.HAIR_LONG;
        dryad.skin = 0xe8b080;
        dryad.skinD = 0xc08458;
        dryad.shirt = 0x3a9a44;
        dryad.shirtD = 0x2a7032;
        dryad.pants = 0x5aa83a;
        dryad.pantsD = 0x3a7a24;
        dryad.shoes = 0x6a4a2a;
        dryad.eye = 0x2a8a3a;
        TOWN[Mobs.DRYAD] = dryad;
        BLOOD_ZOMBIE.skin = 0xb85a52;
        BLOOD_ZOMBIE.skinD = 0x8a3a36;
        BLOOD_ZOMBIE.shirt = 0x6a1a1a;
        BLOOD_ZOMBIE.shirtD = 0x4a1010;
        BLOOD_ZOMBIE.eye = 0xffe040;
        BLOOD_ZOMBIE.sclera = 0xe0a0a0;
        FROZEN_ZOMBIE.skin = 0x9ab8cc;
        FROZEN_ZOMBIE.skinD = 0x7088a0;
        FROZEN_ZOMBIE.hair = 0xe8f0f8;
        FROZEN_ZOMBIE.hairD = 0xb8c8d8;
        FROZEN_ZOMBIE.hairStyle = Humanoid.HAIR_SHORT;
        FROZEN_ZOMBIE.shirt = 0x4a6ea8;
        FROZEN_ZOMBIE.shirtD = 0x34507e;
        FROZEN_ZOMBIE.eye = 0x9ae0ff;
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

    /** The Eye of Cthulhu facing right: a huge veined eyeball (or a toothed mouth) trailing tendrils. */
    private static Sprite buildBossEye(boolean mouth, int frame) {
        int w = 66, h = 46;
        double cx = 43, cy = 23, r = 19.5;
        Sprite s = new Sprite(w, h);
        for (int k = 0; k < 6; k++) {
            double baseY = cy - 11 + k * 4.4;
            double phase = k * 1.3 + frame * 1.1;
            for (int i = 0; i < 28; i++) {
                double x = cx - r + 5 - i;
                double y = baseY + Math.sin(i * 0.33 + phase) * (0.8 + i * 0.07) + (k - 2.5) * i * 0.1;
                double th = 1.9 - i * 0.055;
                int col = Pal.lerp(0xa83030, 0x5a1010, i / 28.0);
                for (int yy = (int) Math.floor(y - th); yy <= (int) Math.ceil(y + th); yy++)
                    for (int xx = (int) Math.floor(x - th); xx <= (int) Math.ceil(x + th); xx++)
                        if (Math.hypot(xx + 0.5 - x, yy + 0.5 - y) <= th)
                            s.set(xx, yy, yy + 0.5 < y - th * 0.3 ? Pal.lerp(col, 0xe06060, 0.3) : col);
            }
        }
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d = Math.hypot(dx, dy);
                if (d > r) continue;
                double lit = 1 - 0.28 * ((dx + dy) / r + 0.4);
                int c = Pal.shade(0xece4e0, (int) Math.max(150, Math.min(256, 256 * lit)));
                if (d > r - 1.3) c = 0x7a2a2a;
                else if (d > r * 0.55) {
                    double ang = Math.atan2(dy, dx);
                    double v = Math.sin(ang * 9 + Math.sin(d * 0.6) * 1.4);
                    if (v > 0.93) c = 0xc84848;
                    else if (v > 0.86 && d > r * 0.75) c = 0xd87070;
                }
                if (!mouth) {
                    double ix = dx - 9, iy = dy;
                    double di = Math.hypot(ix * 1.25, iy);
                    if (di < 9.5) c = Pal.lerp(0x5a8ad0, 0x22427a, di / 9.5);
                    if (di < 9.5 && di > 8.4) c = 0x1a2a50;
                    if (di < 4.6) c = 0x0c0c14;
                } else {
                    double open = (dx - 3) * 0.78 + 1.5 - Math.abs(dy);
                    if (dx > 2 && open > 0) {
                        c = Pal.lerp(0x6a1414, 0x2a0606, Math.min(1, (dx - 2) / r));
                        if (open < 2.4 && ((int) Math.floor(dx)) % 4 < 2) c = open < 1.2 ? 0xf2ead6 : 0xc8b898;
                    } else if (dx > 1 && open > -1.6) {
                        c = 0xb03838;
                    }
                }
                s.set(x, y, c);
            }
        if (!mouth) {
            s.set((int) cx + 6, (int) cy - 5, 0xffffff);
            s.set((int) cx + 7, (int) cy - 5, 0xffffff);
            s.set((int) cx + 6, (int) cy - 4, 0xe0ecff);
        }
        s.set((int) (cx - 7), (int) (cy - 12), 0xffffff);
        s.set((int) (cx - 6), (int) (cy - 13), 0xf8f8ff);
        return s;
    }

    private static Sprite buildServant() {
        Sprite s = new Sprite(15, 11);
        double cx = 10, cy = 5.5, r = 4.6;
        for (int k = 0; k < 2; k++)
            for (int i = 0; i < 7; i++) {
                int y = (int) Math.round(cy - 1 + k * 2 + Math.sin(i * 0.9 + k) * 0.7);
                s.set((int) (cx - r) - i + 1, y, i < 3 ? 0xa02828 : 0x7a1a1a);
            }
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d = Math.hypot(dx, dy);
                if (d > r) continue;
                int c = d > r - 1 ? 0x8a3a3a : dx + dy > 2.5 ? 0xc8bcb8 : 0xece4e0;
                double di = Math.hypot(dx - 1.8, dy);
                if (di < 2.2) c = di < 1.1 ? 0x101018 : 0x3a6ab0;
                s.set(x, y, c);
            }
        s.set((int) cx + 1, (int) cy - 2, 0xffffff);
        return s;
    }

    private static Sprite buildDrippler(int f) {
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        m.put('r', 0x7a1414);
        m.put('R', 0xb02a2a);
        m.put('L', 0xd04a44);
        m.put('w', 0xf0e8e0);
        m.put('k', 0x101010);
        m.put('d', 0x5a0a0a);
        String[][] fr = {
            {"................",
             "....rrrrrrr.....",
             "..rrLLRRRRRrr...",
             ".rRLwkRRRRwkRr..",
             ".rRRkkRRRRkkRRr.",
             "rRRRRRRRRRRRRRr.",
             "rRRRwkRRRRRRRRr.",
             "rRRRkkRRwkRRRRr.",
             ".rRRRRRRkkRRRr..",
             ".rrRRRRRRRRRrr..",
             "..rrrRRRRRrrr...",
             "...r..rRr..r....",
             "...r...r...r....",
             "...d...r...d....",
             ".......d........",
             "................"},
            {"................",
             "................",
             "....rrrrrrr.....",
             "..rrLLRRRRRrr...",
             ".rRLwkRRRRwkRr..",
             ".rRRkkRRRRkkRRr.",
             "rRRRRRRRRRRRRRr.",
             "rRRRwkRRRRRRRRr.",
             "rRRRkkRRwkRRRRr.",
             ".rRRRRRRkkRRRr..",
             ".rrRRRRRRRRRrr..",
             "..rrrRRRRRrrr...",
             "..r...rRr...r...",
             "..d....r....d...",
             ".......r........",
             ".......d........"}};
        return Sprite.ascii(m, fr[f]);
    }

    private static Sprite buildPenguin(int f) {
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        m.put('k', 0x1e2230);
        m.put('K', 0x3a4258);
        m.put('w', 0xf4f6fa);
        m.put('g', 0xc8ccd8);
        m.put('e', 0xffffff);
        m.put('y', 0xf0a020);
        String[][] fr = {
            {"..kkkk..", ".kKkkkk.", ".kkkkeky", ".kkkkkyy", "kKkkkwww", "kkkkwwww", "kkkkwwgw", ".kkkwwgw",
             "..kkwwg.", "..yy.yy."},
            {"..kkkk..", ".kKkkkk.", ".kkkkeky", ".kkkkkyy", ".Kkkkwww", "kKkkwwww", "kkkkwwgw", ".kkkwwgw",
             "..kkwwg.", "...yyyy."}};
        return Sprite.ascii(m, fr[f]);
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
            case Mobs.EYE_OF_CTHULHU: {
                double cx = m.x + m.w / 2.0 - camX, cy = m.y + m.h / 2.0 - camY;
                Sprite[] spr = m.ai[3] > 0 ? BOSS_MOUTH : BOSS_EYE;
                int lit = Pal.lerp(light, 0xffffff, 0.35);
                s.drawRotated(spr[(int) (ticks / 9 % 2)], 43, 23, cx, cy, m.rot, false, flashed(lit, flash));
                return;
            }
            case Mobs.SERVANT: {
                double cx = m.x + m.w / 2.0 - camX, cy = m.y + m.h / 2.0 - camY;
                s.drawRotated(SERVANT, 10, 5.5, cx, cy, m.rot, false, flashed(Pal.lerp(light, 0xffffff, 0.2), flash));
                return;
            }
            case Mobs.DRIPPLER: {
                int bob = (int) Math.round(Math.sin(m.anim * 0.08) * 1.5);
                s.drawFx(DRIPPLER[(int) (ticks / 20 % 2)], sx - 1, sy - 1 + bob, m.dir < 0, Pal.lerp(light, 0xffffff, 0.15),
                        256, 0xffffff, flash);
                return;
            }
            case Mobs.PENGUIN: {
                int f = m.onGround && Math.abs(m.vx) > 0.1 ? (int) (m.anim * 0.6) % 2 : 0;
                s.drawFx(PENGUIN[f], sx, sy, m.dir < 0, light, 256, 0xffffff, flash);
                return;
            }
            case Mobs.ICE_BAT: {
                int f = (int) ((ticks / 5 + m.variant) % 4);
                s.drawFx(ICE_BAT[f == 3 ? 1 : f], sx, sy, m.dir < 0, light, 256, 0xffffff, flash);
                return;
            }
            case Mobs.FIRE_IMP: case Mobs.GUIDE: case Mobs.MERCHANT: case Mobs.NURSE: case Mobs.ARCANIST: case Mobs.DRYAD: {
                Humanoid.Look look = m.type == Mobs.FIRE_IMP ? IMP : TOWN[m.type];
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
                Humanoid.Look look = m.type == Mobs.SKELETON ? SKELETON_LOOK : m.type == Mobs.BLOOD_ZOMBIE ? BLOOD_ZOMBIE
                        : m.type == Mobs.FROZEN_ZOMBIE ? FROZEN_ZOMBIE : ZOMBIE_LOOK[m.variant & 1];
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
