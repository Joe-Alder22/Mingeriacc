package mingeriacc;

import mingeriacc.Entities.Drop;
import mingeriacc.Entities.Particle;

/**
 * World rendering. Tiles, walls and backdrops are drawn unlit into a world
 * layer, which is then lit per pixel (smoothly or per tile) and put on top of
 * the sky. Creatures, items and effects are drawn after that.
 */
final class Renderer {
    static final int T = Tiles.T;
    private static final int EMISSIVE = 0xfe000000, LIT = 0xff000000;

    final Background bg = new Background();
    private final Screen layer = new Screen(16, 16);
    private final int[] extra = new int[4 * 8];
    private int[] grid = new int[0];     // light colour per tile
    private int[] corner = new int[0];   // light colour per tile corner
    private int gx0, gy0, gw, gh;
    private int skyTint;
    private boolean anyGlass;
    private int biome = -1;
    boolean smoothLight = true;

    /** Draws the sky, the landscape and the world. */
    void render(Screen s, Game g, long frameTicks, int hoverItem) {
        World w = g.world;
        double camSpawnY = w.spawnY * T - s.h / 2.0;
        if (g.hidePlayer && frameTicks % 30 == 0) g.detectBiome();
        int b = g.biome;
        // the camera decides between the underworld and the rest
        if (g.camY + s.h * 0.6 >= w.underworld * T) b = Background.UNDERWORLD;
        else if (b == Background.UNDERWORLD) b = Background.FOREST;
        bg.setBiome(b, biome < 0);
        biome = b;
        bg.hellRefY = (w.underworld + (w.h - w.underworld) * 0.45) * T - s.h / 2.0;
        bg.draw(s, w.time, g.camX, g.camY, camSpawnY, frameTicks);
        drawWorld(s, g, hoverItem);
    }

    // ---- light --------------------------------------------------------------

    private void buildLight(Game g, int tx0, int ty0, int tx1, int ty1) {
        gx0 = tx0 - 1;
        gy0 = ty0 - 1;
        gw = tx1 - tx0 + 3;
        gh = ty1 - ty0 + 3;
        if (grid.length < gw * gh) grid = new int[gw * gh];
        for (int y = 0; y < gh; y++)
            for (int x = 0; x < gw; x++) grid[x + y * gw] = g.light.color(gx0 + x, gy0 + y, skyTint);
        // corner (i, j) sits at the top-left of grid tile (i, j)
        int cw = gw, ch = gh;
        if (corner.length < cw * ch) corner = new int[cw * ch];
        for (int y = 1; y < ch; y++)
            for (int x = 1; x < cw; x++) {
                int a = grid[(x - 1) + (y - 1) * gw], b = grid[x + (y - 1) * gw];
                int c = grid[(x - 1) + y * gw], d = grid[x + y * gw];
                int r = ((a >> 16 & 255) + (b >> 16 & 255) + (c >> 16 & 255) + (d >> 16 & 255)) >> 2;
                int gg = ((a >> 8 & 255) + (b >> 8 & 255) + (c >> 8 & 255) + (d >> 8 & 255)) >> 2;
                int bb = ((a & 255) + (b & 255) + (c & 255) + (d & 255)) >> 2;
                corner[x + y * cw] = r << 16 | gg << 8 | bb;
            }
    }

    /** Light colour of the tile at a world pixel position. */
    int lightAt(double wx, double wy) {
        int x = (int) Math.floor(wx / T) - gx0, y = (int) Math.floor(wy / T) - gy0;
        if (x < 0 || y < 0 || x >= gw || y >= gh) return 0;
        return grid[x + y * gw];
    }

    // ---- world -------------------------------------------------------------

    void drawWorld(Screen s, Game g, int hoverItem) {
        World w = g.world;
        int cx = (int) Math.round(g.camX), cy = (int) Math.round(g.camY);
        int tx0 = Math.floorDiv(cx, T), ty0 = Math.floorDiv(cy, T);
        int tx1 = Math.floorDiv(cx + s.w - 1, T), ty1 = Math.floorDiv(cy + s.h - 1, T);

        // lights
        int n = 0;
        int held = g.inv.selectedItem();
        if (held == Items.TORCH && !g.hidePlayer && !g.player.dead) {
            extra[0] = (int) (g.player.centerX() / T);
            extra[1] = (int) ((g.player.y + 6) / T);
            extra[2] = 14;
            extra[3] = Tiles.TORCH_LIGHT;
            n = 1;
        }
        if (g.player.headLight && !g.hidePlayer && !g.player.dead) {
            extra[n * 4] = (int) ((g.player.centerX() + g.player.dir * 6) / T);
            extra[n * 4 + 1] = (int) ((g.player.y + 2) / T);
            extra[n * 4 + 2] = 13;
            extra[n * 4 + 3] = 0xfff4d8;
            n++;
        }
        g.light.compute(w, tx0, ty0, tx1, ty1, g.skyLevel(), extra, n);
        skyTint = Background.sunTint(w.time);
        buildLight(g, tx0, ty0, tx1, ty1);

        // tiles into the world layer
        layer.resize(s.w, s.h);
        java.util.Arrays.fill(layer.px, 0);
        anyGlass = false;
        long anim = g.ticks;
        int bgx = (int) Math.floor(g.camX * 0.55), bgy = (int) Math.floor(g.camY * 0.55);
        for (int ty = ty0; ty <= ty1; ty++) {
            if (ty < 0 || ty >= w.h) continue;
            for (int tx = tx0; tx <= tx1; tx++) {
                if (tx < 0 || tx >= w.w) continue;
                int t = w.tile(tx, ty);
                int wl = w.wall(tx, ty);
                int sx = tx * T - cx, sy = ty * T - cy;
                boolean solid = Tiles.SOLID[t] && !Tiles.TRANSPARENT[t];
                boolean up = !w.solid(tx, ty - 1), dn = !w.solid(tx, ty + 1);
                boolean lf = !w.solid(tx - 1, ty), rt = !w.solid(tx + 1, ty);
                boolean covered = solid && !up && !dn && !lf && !rt;
                int h = Noise.hash(tx, ty);

                layer.mask = LIT;
                if (!covered) {
                    if (wl != 0) {
                        drawWall(w, tx, ty, wl, sx, sy);
                        int ws = g.wallCrackStage(tx, ty);
                        if (ws > 0) layer.draw(Tiles.cracks[ws - 1], sx, sy, false, 256);
                    } else if (ty >= w.surfaceLevel && ty < w.underworld) {
                        drawBackdrop(w, tx, ty, sx, sy, bgx, bgy, g.biome);
                    }
                }
                if (t == Tiles.AIR) continue;
                if (Tiles.TRANSPARENT[t]) {
                    anyGlass = true;
                    continue;
                }
                if (Tiles.isFurniture(t)) {
                    int ox = w.originX(tx, ty), oy = w.originY(tx, ty);
                    if (tx == Math.max(ox, tx0) && ty == Math.max(oy, ty0)) drawFurniture(w, t, ox, oy, cx, cy, anim);
                } else if (solid) {
                    drawSolid(w, t, tx, ty, sx, sy, up, dn, lf, rt, h);
                } else {
                    drawDecor(w, t, tx, ty, sx, sy, h, anim);
                }
                int cs = g.crackStage(tx, ty);
                if (cs > 0) layer.draw(Tiles.cracks[cs - 1], sx, sy, false, 256);
            }
        }
        layer.mask = 0;
        composite(s, tx0, ty0, tx1, ty1, cx, cy);

        // glass is see-through, so it is drawn over the lit result
        if (anyGlass) {
            for (int ty = ty0; ty <= ty1; ty++)
                for (int tx = tx0; tx <= tx1; tx++) {
                    int t = w.tile(tx, ty);
                    if (!Tiles.TRANSPARENT[t]) continue;
                    int l = lightAt(tx * T + 4, ty * T + 4);
                    s.drawLit(Tiles.tex[t][Noise.hash(tx, ty) & 3], tx * T - cx, ty * T - cy, false, l);
                    int e = Pal.mulLight(Tiles.EDGE[t], l);
                    if (!w.solid(tx, ty - 1)) s.fill(tx * T - cx, ty * T - cy, T, 1, e);
                    if (!w.solid(tx, ty + 1)) s.fill(tx * T - cx, ty * T - cy + T - 1, T, 1, e);
                    if (!w.solid(tx - 1, ty)) s.fill(tx * T - cx, ty * T - cy, 1, T, e);
                    if (!w.solid(tx + 1, ty)) s.fill(tx * T - cx + T - 1, ty * T - cy, 1, T, e);
                }
        }

        drawEntities(s, g, cx, cy, held);
        drawLiquids(s, w, tx0, ty0, tx1, ty1, cx, cy, g.ticks);

        // target highlight
        if (hoverItem != 0 && g.aimInRange && !g.player.dead) {
            int sx = g.aimX * T - cx, sy = g.aimY * T - cy;
            int c = Pal.WHITE;
            int a = 150 + (int) (Math.sin(g.ticks * 0.15) * 60);
            for (int i = 0; i < 3; i++) {
                s.pblend(sx - 1 + i, sy - 1, c, a);
                s.pblend(sx - 1, sy - 1 + i, c, a);
                s.pblend(sx + T - i, sy - 1, c, a);
                s.pblend(sx + T, sy - 1 + i, c, a);
                s.pblend(sx - 1 + i, sy + T, c, a);
                s.pblend(sx - 1, sy + T - i, c, a);
                s.pblend(sx + T - i, sy + T, c, a);
                s.pblend(sx + T, sy + T - i, c, a);
            }
        }

        // pickup texts and damage numbers
        for (Game.Popup pp : g.popups) {
            int tw = Font.width(pp.text);
            int px = (int) Math.round(pp.x) - cx - tw / 2, py = (int) Math.round(pp.y) - cy;
            double f = pp.life / (double) Math.max(1, pp.maxLife);
            int col = f > 0.3 ? pp.color : Pal.lerp(Pal.GRAY_D, pp.color, f / 0.3);
            if (pp.big) s.textOutline(pp.text + "!", px, py, col);
            else s.textOutline(pp.text, px, py, col);
        }
    }

    /** Water (see-through, lit) and lava (glowing), drawn over creatures so they look submerged. */
    private void drawLiquids(Screen s, World w, int tx0, int ty0, int tx1, int ty1, int cx, int cy, long ticks) {
        for (int ty = Math.max(0, ty0); ty <= Math.min(w.h - 1, ty1); ty++)
            for (int tx = Math.max(0, tx0); tx <= Math.min(w.w - 1, tx1); tx++) {
                int a = w.liquidAt(tx, ty);
                if (a == 0) continue;
                int type = w.liquidType(tx, ty);
                boolean fedFromAbove = w.liquidAt(tx, ty - 1) > 0 && w.liquidType(tx, ty - 1) == type;
                int h = fedFromAbove ? T : Math.max(1, (a * T + 128) / 255);
                int sx = tx * T - cx, sy = ty * T - cy + T - h;
                boolean surface = !fedFromAbove;
                if (type == Liquids.LAVA) {
                    for (int y = 0; y < h; y++)
                        for (int x = 0; x < T; x++) {
                            int wx = tx * T + x, wy = ty * T + T - h + y;
                            double v = Math.sin(wx * 0.35 + ticks * 0.03) + Math.sin(wy * 0.4 - ticks * 0.02 + wx * 0.1);
                            int c = Pal.lerp(Pal.LAVA_D, Pal.LAVA, 0.5 + v * 0.25);
                            if (surface && y == 0) c = Pal.LAVA_L;
                            else if (surface && y == 1) c = Pal.lerp(Pal.LAVA, Pal.LAVA_L, 0.5);
                            s.pblend(sx + x, sy + y, c, 235);
                        }
                } else {
                    int l = lightAt(tx * T + 4, ty * T + 4);
                    int body = Pal.mulLight(Pal.WATER, Pal.lerp(l, 0xffffff, 0.08));
                    int top = Pal.mulLight(Pal.WATER_L, l);
                    for (int y = 0; y < h; y++)
                        for (int x = 0; x < T; x++) {
                            int c = body;
                            int alpha = 140;
                            if (surface && y == 0) {
                                c = top;
                                alpha = 200;
                            } else if (surface && y == 1 && ((tx * T + x + ticks / 6) % 11 < 3)) {
                                c = top;
                                alpha = 120;
                            }
                            s.pblend(sx + x, sy + y, c, alpha);
                        }
                }
            }
    }

    /** Lights the world layer and puts it on the screen. */
    private void composite(Screen s, int tx0, int ty0, int tx1, int ty1, int cx, int cy) {
        int[] src = layer.px, dst = s.px;
        int sw = s.w, sh = s.h;
        for (int ty = ty0; ty <= ty1; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                int sx = tx * T - cx, sy = ty * T - cy;
                int gi = (tx - gx0) + (ty - gy0) * gw;
                int c00, c10, c01, c11;
                if (smoothLight) {
                    c00 = corner[gi];
                    c10 = corner[gi + 1];
                    c01 = corner[gi + gw];
                    c11 = corner[gi + gw + 1];
                } else {
                    c00 = c10 = c01 = c11 = grid[gi];
                }
                int r00 = c00 >> 16 & 255, g00 = c00 >> 8 & 255, b00 = c00 & 255;
                int r10 = c10 >> 16 & 255, g10 = c10 >> 8 & 255, b10 = c10 & 255;
                int r01 = c01 >> 16 & 255, g01 = c01 >> 8 & 255, b01 = c01 & 255;
                int r11 = c11 >> 16 & 255, g11 = c11 >> 8 & 255, b11 = c11 & 255;
                for (int y = 0; y < T; y++) {
                    int dy = sy + y;
                    if (dy < 0 || dy >= sh) continue;
                    int fy = 2 * y + 1, iy = 16 - fy;
                    int rl = (r00 * iy + r01 * fy) >> 4, gl = (g00 * iy + g01 * fy) >> 4, bl = (b00 * iy + b01 * fy) >> 4;
                    int rr = (r10 * iy + r11 * fy) >> 4, gr = (g10 * iy + g11 * fy) >> 4, br = (b10 * iy + b11 * fy) >> 4;
                    int row = dy * sw;
                    for (int x = 0; x < T; x++) {
                        int dx = sx + x;
                        if (dx < 0 || dx >= sw) continue;
                        int v = src[row + dx];
                        if (v == 0) continue;
                        if ((v & 0xff000000) == EMISSIVE) {
                            dst[row + dx] = v & 0xffffff;
                            continue;
                        }
                        int fx = 2 * x + 1, ix = 16 - fx;
                        int lr = ((rl * ix + rr * fx) >> 4) + 1;
                        int lg = ((gl * ix + gr * fx) >> 4) + 1;
                        int lb = ((bl * ix + br * fx) >> 4) + 1;
                        dst[row + dx] = (((v >> 16 & 255) * lr >> 8) << 16) | (((v >> 8 & 255) * lg >> 8) << 8)
                                | ((v & 255) * lb >> 8);
                    }
                }
            }
        }
    }

    // ---- tile drawing ------------------------------------------------------------

    private void drawWall(World w, int tx, int ty, int wl, int sx, int sy) {
        int wx0 = tx * T, wy0 = ty * T;
        for (int y = 0; y < T; y++) {
            int dy = sy + y;
            if (dy < 0 || dy >= layer.h) continue;
            for (int x = 0; x < T; x++) {
                int dx = sx + x;
                if (dx < 0 || dx >= layer.w) continue;
                layer.px[dx + dy * layer.w] = Tiles.wallTexel(wl, wx0 + x, wy0 + y) & 0xffffff | LIT;
            }
        }
        // soft edge where the wall ends
        int edge = Pal.BLACK;
        int ea = 100;
        if (w.wall(tx, ty - 1) == 0 && !w.solid(tx, ty - 1)) for (int i = 0; i < T; i++) layer.pblend(sx + i, sy, edge, ea);
        if (w.wall(tx, ty + 1) == 0 && !w.solid(tx, ty + 1)) for (int i = 0; i < T; i++) layer.pblend(sx + i, sy + T - 1, edge, ea);
        if (w.wall(tx - 1, ty) == 0 && !w.solid(tx - 1, ty)) for (int i = 0; i < T; i++) layer.pblend(sx, sy + i, edge, ea);
        if (w.wall(tx + 1, ty) == 0 && !w.solid(tx + 1, ty)) for (int i = 0; i < T; i++) layer.pblend(sx + T - 1, sy + i, edge, ea);
    }

    /** The distant cave backdrop behind open underground spaces (moves slower than the world). */
    private void drawBackdrop(World w, int tx, int ty, int sx, int sy, int bgx, int bgy, int biome) {
        int border = w.rockLevel * T + (Noise.hash(tx, 991) & 7) - 4;
        for (int y = 0; y < T; y++) {
            int dy = sy + y;
            if (dy < 0 || dy >= layer.h) continue;
            int k = ty * T + y >= border ? 1 : 0;
            if (biome == Background.JUNGLE) k = 2;
            else if (biome == Background.CORRUPTION) k = 3;
            Sprite tex = Tiles.caveBack[k];
            int v = (dy + bgy) & 63;
            for (int x = 0; x < T; x++) {
                int dx = sx + x;
                if (dx < 0 || dx >= layer.w) continue;
                layer.px[dx + dy * layer.w] = tex.p[((dx + bgx) & 63) + v * 64] & 0xffffff | LIT;
            }
        }
    }

    private void drawSolid(World w, int t, int tx, int ty, int sx, int sy, boolean up, boolean dn, boolean lf, boolean rt, int h) {
        int edge = Tiles.EDGE[t];
        boolean grass = Tiles.isGrass(t);
        int gHL = Pal.GRASS_HL, gL = Pal.GRASS_L, gM = Pal.GRASS, gD = Pal.GRASS_D;
        if (t == Tiles.JUNGLE_GRASS) {
            gHL = 0xaae060; gL = Pal.JGRASS_L; gM = Pal.JGRASS; gD = Pal.JGRASS_D;
        } else if (t == Tiles.CORRUPT_GRASS) {
            gHL = 0xb0a0e0; gL = Pal.CGRASS_L; gM = Pal.CGRASS; gD = Pal.CGRASS_D;
        }
        boolean bleed = Tiles.BLEED_DST[t];
        int bU = 0, bD = 0, bL = 0, bR = 0; // neighbour tiles that bleed in
        if (bleed) {
            int n;
            if (!up && Tiles.BLEED_SRC[n = w.tile(tx, ty - 1)] && n != t) bU = n;
            if (!dn && Tiles.BLEED_SRC[n = w.tile(tx, ty + 1)] && n != t) bD = n;
            if (!lf && Tiles.BLEED_SRC[n = w.tile(tx - 1, ty)] && n != t) bL = n;
            if (!rt && Tiles.BLEED_SRC[n = w.tile(tx + 1, ty)] && n != t) bR = n;
        }
        int wx0 = tx * T, wy0 = ty * T;
        for (int y = 0; y < T; y++) {
            int dy = sy + y;
            if (dy < 0 || dy >= layer.h) continue;
            for (int x = 0; x < T; x++) {
                int dx = sx + x;
                if (dx < 0 || dx >= layer.w) continue;
                boolean top = y == 0, bottom = y == T - 1, left = x == 0, right = x == T - 1;
                // rounded corners
                if ((top && left && up && lf) || (top && right && up && rt)
                        || (bottom && left && dn && lf) || (bottom && right && dn && rt)) continue;
                int wx = wx0 + x, wy = wy0 + y;
                int c = Tiles.texel(t, h, x, y, wx, wy);
                if (bleed) {
                    int hb = Noise.hash(wx * 3 + wy, 5);
                    int depth = (hb & 3) == 0 ? 0 : 1 + ((hb >> 2) & 1);
                    int src = 0;
                    if (bL != 0 && x < depth) src = bL;
                    else if (bR != 0 && x >= T - depth) src = bR;
                    else if (bU != 0 && y < depth) src = bU;
                    else if (bD != 0 && y >= T - depth) src = bD;
                    if (src != 0) c = Tiles.texel(src, h, x, y, wx, wy);
                }
                if (grass) {
                    int hx = Noise.hash(wx, 17);
                    int depth = 2 + (hx & 1) + ((hx >> 3 & 3) == 0 ? 1 : 0);
                    if (up && y < depth) {
                        c = y == 0 ? ((hx >> 5 & 1) == 0 ? gL : gHL) : y == depth - 1 ? gD : gM;
                    } else if ((lf && x == 0 || rt && x == T - 1) && y < 4 && up) {
                        c = gD;
                    } else if ((top && up) || (bottom && dn) || (left && lf) || (right && rt)) {
                        c = edge;
                    } else if (up && y == depth && (hx >> 7 & 1) == 0) {
                        c = Pal.DIRT_DD;
                    }
                } else if ((top && up) || (bottom && dn) || (left && lf) || (right && rt)) {
                    c = edge;
                } else if (up && y == 1) {
                    c = Pal.lerp(c, 0xffffff, 0.12);
                } else if (dn && y == T - 2) {
                    c = Pal.shade(c, 215);
                }
                layer.px[dx + dy * layer.w] = c & 0xffffff | LIT;
            }
        }
        // blades of grass sticking up
        if (grass && up && w.tile(tx, ty - 1) == Tiles.AIR) {
            for (int x = 0; x < T; x++) {
                int hx = Noise.hash(wx0 + x, 31);
                if ((hx & 3) != 0) continue;
                int bh = 1 + ((hx >> 2) & 1);
                for (int k = 1; k <= bh; k++) layer.pset(sx + x, sy - k, k == bh ? gL : gM);
            }
        }
    }

    private void drawFurniture(World w, int t, int ox, int oy, int cx, int cy, long anim) {
        Sprite[] frames = Tiles.furn[t];
        int flags = w.meta(ox, oy) >> 6;
        int variant = t == Tiles.POT ? Noise.hash(ox, oy) % frames.length : 0;
        if (variant < 0) variant = -variant;
        Sprite spr = frames[variant];
        int sx = ox * T - cx, sy = oy * T - cy + (Tiles.FURN_H[t] * T - spr.h);
        boolean flip = (flags & 1) != 0 && t != Tiles.FURNACE;
        layer.draw(spr, sx, sy, flip, 256);
        Sprite[] glow = Tiles.furnGlow[t];
        if (glow != null) {
            layer.mask = EMISSIVE;
            layer.draw(glow[(int) ((anim / 6 + ox) % glow.length)], sx, sy, flip, 256);
            layer.mask = LIT;
        }
    }

    private void drawDecor(World w, int t, int tx, int ty, int sx, int sy, int h, long anim) {
        switch (t) {
            case Tiles.TORCH: {
                int f = (int) ((anim / 7 + tx * 3 + ty) % 3);
                // torches on a wall lean towards their support
                boolean below = w.solid(tx, ty + 1) || w.isPlatform(tx, ty + 1);
                int off = 0;
                if (!below && w.wall(tx, ty) == 0) {
                    if (w.solid(tx - 1, ty)) off = -2;
                    else if (w.solid(tx + 1, ty)) off = 2;
                }
                layer.draw(Tiles.torchStick, sx + off, sy, false, 256);
                layer.mask = EMISSIVE;
                layer.draw(Tiles.torchFlame[f][0], sx + off, sy, false, 256);
                layer.mask = LIT;
                break;
            }
            case Tiles.TRUNK: case Tiles.MAHOGANY_TRUNK: case Tiles.EBON_TRUNK: {
                boolean base = !Tiles.isTrunk(w.tile(tx, ty + 1));
                Sprite[] bases = t == Tiles.MAHOGANY_TRUNK ? Tiles.trunkBaseMaho : t == Tiles.EBON_TRUNK ? Tiles.trunkBaseEbon : Tiles.trunkBase;
                layer.draw(base ? bases[h & 3] : Tiles.tex[t][h & 3], sx, sy, false, 256);
                break;
            }
            case Tiles.LEAVES: case Tiles.MAHOGANY_LEAVES: case Tiles.EBON_LEAVES: {
                int leafL = t == Tiles.LEAVES ? Pal.LEAF_L : t == Tiles.MAHOGANY_LEAVES ? Pal.JGRASS_L : Pal.CGRASS_L;
                int leafD = t == Tiles.LEAVES ? Pal.LEAF_D : t == Tiles.MAHOGANY_LEAVES ? 0x2a5a14 : 0x2e2448;
                Sprite tex = Tiles.tex[t][h & 3];
                boolean up = !Tiles.isLeaves(w.tile(tx, ty - 1)), dn = !Tiles.isLeaves(w.tile(tx, ty + 1));
                int l = w.tile(tx - 1, ty), r = w.tile(tx + 1, ty);
                boolean lf = !Tiles.isLeaves(l) && !Tiles.isTrunk(l);
                boolean rt = !Tiles.isLeaves(r) && !Tiles.isTrunk(r);
                // deeper inside the canopy is darker (light comes from above)
                int depth = 0;
                while (depth < 4 && Tiles.isLeaves(w.tile(tx, ty - 1 - depth))) depth++;
                int shade = 266 - depth * 20 + (lf ? 8 : 0);
                for (int y = 0; y < T; y++)
                    for (int x = 0; x < T; x++) {
                        int c = tex.p[x + y * T];
                        int hx = Noise.hash(h + x * 7, y * 13);
                        boolean eT = up && y == 0, eB = dn && y == T - 1, eL = lf && x == 0, eR = rt && x == T - 1;
                        int edges = (eT ? 1 : 0) + (eB ? 1 : 0) + (eL ? 1 : 0) + (eR ? 1 : 0);
                        if (edges >= 2) continue;
                        if (edges == 1) {
                            if ((hx & 1) == 0) continue;
                            c = eT ? leafL : leafD;
                        }
                        // second row from the edge: a little raggedness
                        if ((up && y == 1 || lf && x == 1 || rt && x == T - 2) && (hx & 7) == 0) continue;
                        if (dn && y >= T - 2) c = Pal.shade(c, 200);
                        layer.pset(sx + x, sy + y, Pal.shade(c, Math.max(150, Math.min(256, shade - y * 20 / T))));
                    }
                break;
            }
            default: {
                Sprite[] v = Tiles.tex[t];
                layer.draw(v[h & (v.length - 1)], sx, sy, false, 256);
            }
        }
    }

    // ---- entities ----------------------------------------------------------------

    private void drawEntities(Screen s, Game g, int cx, int cy, int held) {
        // dropped items
        for (Drop d : g.drops) {
            Sprite ic = Items.SMALL[d.item];
            if (Items.isCoin(d.item)) ic = ItemArt.coinFrame(d.item, (int) ((d.age / 6 + d.x) % 4));
            int l = lightAt(d.x + 3, d.y + 3);
            l = Pal.lerp(l, 0xffffff, 0.12);
            if (d.item == Items.TORCH) l = 0xffffff;
            int bob = (int) Math.round(Math.sin((d.age + d.x) * 0.08) * 1.0);
            int dx = (int) Math.round(d.x) - cx + 3 - ic.w / 2, dy = (int) Math.round(d.y) - cy + 6 - ic.h + bob;
            s.drawLit(ic, dx, dy, false, l);
            if (Items.isCoin(d.item) && (g.ticks / 8 + (int) d.x) % 20 == 0) s.padd(dx + 2, dy + 1, 0xffffff, 200);
        }

        // creatures
        for (Mob m : g.mobs) {
            int l = lightAt(m.centerX(), m.centerY());
            MobArt.draw(s, m, cx, cy, l, g.ticks);
            if (m.life < m.lifeMax && !m.dead) {
                int bw = Math.max(12, m.w + 2), bx = (int) Math.round(m.centerX()) - cx - bw / 2;
                int by = (int) Math.round(m.y + m.h) - cy + 3;
                double f = Math.max(0, m.life / (double) m.lifeMax);
                s.fillA(bx - 1, by - 1, bw + 2, 4, Pal.BLACK, 170);
                s.fill(bx, by, (int) Math.ceil(bw * f), 2, Pal.lerp(0xe03a2a, 0x5ae04a, f));
            }
        }

        for (Mob m : g.town) {
            MobArt.draw(s, m, cx, cy, lightAt(m.centerX(), m.centerY()), g.ticks);
            if (m.life < m.lifeMax) {
                int bw = 14, bx = (int) Math.round(m.centerX()) - cx - bw / 2, by = (int) Math.round(m.y + m.h) - cy + 3;
                double f = Math.max(0, m.life / (double) m.lifeMax);
                s.fillA(bx - 1, by - 1, bw + 2, 4, Pal.BLACK, 170);
                s.fill(bx, by, (int) Math.ceil(bw * f), 2, Pal.lerp(0xe03a2a, 0x5ae04a, f));
            }
        }

        // player
        Player p = g.player;
        if (!g.hidePlayer) p.draw(s, cx, cy, lightAt(p.centerX(), p.centerY()), held);

        // projectiles
        for (Projectile pr : g.projectiles) pr.draw(s, cx, cy, lightAt(pr.x, pr.y));

        // particles
        for (Particle pa : g.particles) {
            int px = (int) Math.round(pa.x) - cx, py = (int) Math.round(pa.y) - cy;
            if (pa.glow) {
                int a = (int) Math.min(256, 256.0 * pa.life / Math.max(1, pa.maxLife) * 2);
                s.padd(px, py, pa.color, a);
                s.glow(px + 0.5, py + 0.5, 3, pa.color, a / 4);
                continue;
            }
            int c = Pal.mulLight(pa.color, lightAt(pa.x, pa.y));
            s.pset(px, py, c);
            if (pa.size > 1 || pa.life > pa.maxLife / 2) s.pset(px + 1, py, c);
        }
    }
}
