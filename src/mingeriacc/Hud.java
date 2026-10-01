package mingeriacc;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game UI: hotbar, inventory, trash, crafting, equipment, chests, the
 * merchant's shop, conversations, life, effects and tooltips.
 */
final class Hud {
    static final int SLOT = 20, STEP = 22, X0 = 8, Y0 = 16;
    private static final int CRAFT_COLS = 10, CRAFT_ROWS = 3;

    // what the mouse is over
    private static final int NONE = 0, INV = 1, TRASH = 2, RECIPE = 3, CHEST = 4, SHOP = 5, ARMOR = 6, ACC = 7, CHEAT = 8;
    private int hoverKind, hoverIndex = -1;
    private final List<Recipes.Recipe> recipes = new ArrayList<>();
    private int craftScroll;
    private int holdTimer;
    private int recipeTimer;
    private int screenW = 640, screenH = 360;
    private static final Sprite HEART = heart();
    private static final Sprite STAR = manaStar();
    /** The developer menu (every item, mining speed, god mode) is shown in the inventory. */
    boolean cheats;
    private int cheatScroll;
    private static final int[] CHEAT_ITEMS = cheatItems();

    static int slotX(int i) { return X0 + (i % 10) * STEP; }
    static int slotY(int i) { return Y0 + (i / 10) * STEP + (i >= 10 ? 6 : 0); }

    private static int trashX() { return X0 + 10 * STEP + 8; }
    private static int trashY() { return slotY(40); }

    private int craftY() { return slotY(40) + STEP + 30; }

    private int equipX() { return screenW - 8 - SLOT; }
    private int equipY(int i) { return 76 + i * STEP + (i >= 3 ? 8 : 0); }

    // the cheat panel sits between the inventory and the equipment column
    private static int cheatX() { return X0 + 10 * STEP + 34; }
    private static final int CHEAT_Y = 52, CHEAT_GRID_Y = CHEAT_Y + 34;
    // (it leaves room for the "Defense" label above the equipment and for a boss bar at the bottom)
    private int cheatCols() { return Math.max(1, (equipX() - 54 - cheatX()) / STEP); }
    private int cheatRows() { return Math.max(1, (screenH - CHEAT_GRID_Y - 34) / STEP); }

    private static int[] cheatItems() {
        int n = 0;
        for (int i = 1; i < Items.COUNT; i++) if (Items.KIND[i] != Items.K_PICKUP) n++;
        int[] out = new int[n];
        n = 0;
        for (int i = 1; i < Items.COUNT; i++) if (Items.KIND[i] != Items.K_PICKUP) out[n++] = i;
        return out;
    }

    /** The shop of the town dweller being talked to, or null. */
    private static int[] shop(Game g) {
        return g.talkTo != null && g.shopOpen ? Town.shop(g.talkTo.type) : null;
    }

    private static boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static Sprite heart() {
        String[] rows = {
            ".XX...XX.",
            "XhhX.XllX",
            "XhllXlllX",
            "XllllllmX",
            ".XlllllX.",
            "..XlmmX..",
            "...XmX...",
            "....X....",
        };
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        m.put('X', Pal.UI_HEART_D);
        m.put('h', Pal.UI_HEART_L);
        m.put('l', Pal.UI_HEART);
        m.put('m', 0xb82838);
        return Sprite.ascii(m, rows);
    }

    private static Sprite manaStar() {
        String[] rows = {
            "....X....",
            "...XlX...",
            "XXXXllXXX",
            "XhllllmmX",
            ".XhllmmX.",
            "..XlllX..",
            ".XllXmmX.",
            ".XmX.XmX.",
            ".XX...XX.",
        };
        java.util.Map<Character, Integer> m = new java.util.HashMap<>();
        m.put('X', Pal.MANA_D);
        m.put('h', Pal.MANA_L);
        m.put('l', Pal.MANA);
        m.put('m', 0x2a48c0);
        return Sprite.ascii(m, rows);
    }

    /** Refreshes the list of recipes shown (those near a station that the player has materials for). */
    private void refreshRecipes(Game g) {
        recipes.clear();
        List<Recipes.Recipe> other = new ArrayList<>();
        for (Recipes.Recipe r : Recipes.ALL) {
            if (r.station != Recipes.HAND && !g.nearStation[r.station]) continue;
            if (r.canCraft(g.inv)) recipes.add(r);
            else if (r.hasAny(g.inv)) other.add(r);
        }
        recipes.addAll(other);
        int maxScroll = Math.max(0, (recipes.size() + CRAFT_COLS - 1) / CRAFT_COLS - CRAFT_ROWS);
        craftScroll = Math.max(0, Math.min(maxScroll, craftScroll));
    }

    // ---- input ---------------------------------------------------------------

    private void findHover(Game g, int mx, int my) {
        hoverKind = NONE;
        hoverIndex = -1;
        int slots = g.inventoryOpen ? Inventory.SIZE : Inventory.HOTBAR;
        for (int i = 0; i < slots; i++)
            if (inRect(mx, my, slotX(i), slotY(i), SLOT, SLOT)) { hoverKind = INV; hoverIndex = i; return; }
        if (!g.inventoryOpen) return;
        if (inRect(mx, my, trashX(), trashY(), SLOT, SLOT)) { hoverKind = TRASH; return; }
        for (int i = 0; i < 8; i++)
            if (inRect(mx, my, equipX(), equipY(i), SLOT, SLOT)) {
                hoverKind = i < 3 ? ARMOR : ACC;
                hoverIndex = i < 3 ? i : i - 3;
                return;
            }
        if (cheats) {
            int cols = cheatCols(), rows = cheatRows();
            for (int k = 0; k < cols * rows; k++) {
                int i = k + cheatScroll * cols;
                if (i >= CHEAT_ITEMS.length) break;
                if (inRect(mx, my, cheatX() + (k % cols) * STEP, CHEAT_GRID_Y + (k / cols) * STEP, SLOT, SLOT)) {
                    hoverKind = CHEAT;
                    hoverIndex = i;
                    return;
                }
            }
        }
        int cy = craftY();
        int[] shop = shop(g);
        if (g.openChest != null) {
            for (int i = 0; i < World.Chest.SIZE; i++)
                if (inRect(mx, my, X0 + (i % 10) * STEP, cy + (i / 10) * STEP, SLOT, SLOT)) { hoverKind = CHEST; hoverIndex = i; return; }
        } else if (shop != null) {
            for (int i = 0; i < shop.length; i++)
                if (inRect(mx, my, X0 + (i % 10) * STEP, cy + (i / 10) * STEP, SLOT, SLOT)) { hoverKind = SHOP; hoverIndex = i; return; }
        } else {
            for (int k = 0; k < CRAFT_COLS * CRAFT_ROWS; k++) {
                int r = k + craftScroll * CRAFT_COLS;
                if (r >= recipes.size()) break;
                if (inRect(mx, my, X0 + (k % CRAFT_COLS) * STEP, cy + (k / CRAFT_COLS) * STEP, SLOT, SLOT)) {
                    hoverKind = RECIPE;
                    hoverIndex = r;
                    return;
                }
            }
        }
    }

    /** Handles the mouse in the UI; returns true if the mouse is over the UI. */
    boolean update(Game g, Input in, int screenWidth, int screenHeight) {
        screenW = screenWidth;
        screenH = screenHeight;
        int mx = in.mouseX, my = in.mouseY;
        if (g.player.dead) {
            hoverKind = NONE;
            return false;
        }
        if (g.inventoryOpen && --recipeTimer <= 0) {
            refreshRecipes(g);
            recipeTimer = 10;
        }
        findHover(g, mx, my);
        boolean over = hoverKind != NONE;
        int cy = craftY();
        if (g.inventoryOpen) {
            over |= inRect(mx, my, X0 - 4, Y0 - 4, 10 * STEP + 40, cy + 4 * STEP + 30 - (Y0 - 4));
            over |= inRect(mx, my, equipX() - 4, equipY(0) - 14, SLOT + 8, equipY(7) + SLOT + 4 - equipY(0) + 14);
            if (in.wheel != 0 && g.openChest == null && !g.shopOpen
                    && inRect(mx, my, X0 - 4, cy - 4, CRAFT_COLS * STEP + 8, CRAFT_ROWS * STEP + 8)) {
                craftScroll += in.wheel > 0 ? 1 : -1;
                refreshRecipes(g);
            }
            if (cheats) {
                int pw = cheatCols() * STEP + 4;
                boolean overPanel = inRect(mx, my, cheatX() - 4, CHEAT_Y - 4, pw + 4, screenH - CHEAT_Y);
                over |= overPanel;
                int rows = (CHEAT_ITEMS.length + cheatCols() - 1) / cheatCols();
                if (in.wheel != 0 && overPanel)
                    cheatScroll = Math.max(0, Math.min(Math.max(0, rows - cheatRows()), cheatScroll + (in.wheel > 0 ? 1 : -1)));
            }
        } else {
            over |= inRect(mx, my, X0 - 2, Y0 - 2, 10 * STEP + 2, SLOT + 4);
        }
        if (g.talkTo != null && !g.inventoryOpen) over |= dialog(null, g, in, false);

        Inventory inv = g.inv;
        if (!g.inventoryOpen) {
            if (in.clickL && hoverKind == INV) {
                g.selectSlot(hoverIndex);
                in.consumeClicks();
            }
            return over;
        }
        // chest buttons
        if (g.openChest != null && in.clickL) {
            int by = cy + 4 * STEP + 2;
            if (inRect(mx, my, X0, by, 70, 14)) {
                lootAll(g);
                in.consumeClicks();
                return true;
            }
            if (inRect(mx, my, X0 + 76, by, 80, 14)) {
                depositAll(g);
                in.consumeClicks();
                return true;
            }
        }

        switch (hoverKind) {
            case INV:
                if (in.clickL) {
                    if (in.shift() && g.cursorItem == 0 && inv.id[hoverIndex] != 0) quickMove(g, hoverIndex);
                    else clickSlot(g, inv.id, inv.count, hoverIndex);
                    in.consumeClicks();
                } else if (in.clickR) {
                    int item = inv.id[hoverIndex];
                    if (g.cursorItem == 0 && item != 0 && inv.equipFrom(hoverIndex)) {
                        g.audio.play(Audio.TICK, 0.3, 1.0);
                    } else {
                        takeOne(g, inv.id, inv.count, hoverIndex);
                    }
                    in.consumeClicks();
                }
                break;
            case CHEST:
                if (in.clickL) {
                    if (in.shift() && g.cursorItem == 0 && g.openChest.id[hoverIndex] != 0) {
                        World.Chest c = g.openChest;
                        int left = inv.add(c.id[hoverIndex], c.count[hoverIndex]);
                        c.count[hoverIndex] = left;
                        if (left == 0) c.id[hoverIndex] = 0;
                        g.audio.play(Audio.TICK, 0.3, 1.1);
                    } else {
                        clickSlot(g, g.openChest.id, g.openChest.count, hoverIndex);
                    }
                    in.consumeClicks();
                } else if (in.clickR) {
                    takeOne(g, g.openChest.id, g.openChest.count, hoverIndex);
                    in.consumeClicks();
                }
                break;
            case TRASH:
                if (in.clickL) {
                    if (g.cursorItem != 0) {
                        inv.trashId = g.cursorItem;
                        inv.trashCount = g.cursorCount;
                        g.cursorItem = 0;
                        g.cursorCount = 0;
                    } else if (inv.trashId != 0) {
                        g.cursorItem = inv.trashId;
                        g.cursorCount = inv.trashCount;
                        inv.trashId = inv.trashCount = 0;
                    }
                    g.audio.play(Audio.TICK, 0.3, 0.9);
                    in.consumeClicks();
                }
                break;
            case ARMOR: case ACC:
                if (in.clickL) {
                    clickEquip(g, hoverKind == ARMOR, hoverIndex);
                    in.consumeClicks();
                }
                break;
            case SHOP:
                if (in.clickL) {
                    if (g.cursorItem != 0) sellCursor(g);
                    else g.buy(shop(g)[hoverIndex]);
                    in.consumeClicks();
                }
                break;
            case CHEAT:
                if (in.clickL || in.clickR) {
                    int item = CHEAT_ITEMS[hoverIndex];
                    if (g.cursorItem == 0 || g.cursorItem == item) {
                        g.cursorItem = item;
                        g.cursorCount = in.clickR ? Math.min(Items.MAX_STACK[item], g.cursorCount + 1) : Items.MAX_STACK[item];
                        g.audio.play(Audio.TICK, 0.3, 1.3);
                    }
                    in.consumeClicks();
                }
                break;
            case RECIPE:
                if (in.mouseL && hoverIndex < recipes.size()) {
                    if (in.clickL) {
                        craft(g, recipes.get(hoverIndex), in.shift());
                        holdTimer = 0;
                    } else if (++holdTimer > 28 && holdTimer % 7 == 0) {
                        craft(g, recipes.get(hoverIndex), in.shift());
                    }
                    in.consumeClicks();
                }
                break;
            default:
                if (in.clickL && g.shopOpen && g.cursorItem != 0
                        && inRect(mx, my, X0 - 4, cy - 4, 10 * STEP + 8, 2 * STEP + 8)) {
                    sellCursor(g);
                    in.consumeClicks();
                } else if (in.clickL && !over && g.cursorItem != 0) {
                    g.spawnThrown(g.cursorItem, g.cursorCount);
                    g.cursorItem = 0;
                    g.cursorCount = 0;
                    in.consumeClicks();
                }
        }
        if (!in.mouseL) holdTimer = 0;
        return over || g.cursorItem != 0;
    }

    private void sellCursor(Game g) {
        if (Items.isCoin(g.cursorItem)) return;
        g.sell(g.cursorItem, g.cursorCount);
        g.cursorItem = 0;
        g.cursorCount = 0;
    }

    /** Shift-click: to the chest, sell to the merchant, or into the trash. */
    private void quickMove(Game g, int i) {
        Inventory inv = g.inv;
        if (g.openChest != null) {
            int left = g.openChest.add(inv.id[i], inv.count[i]);
            inv.count[i] = left;
            if (left == 0) inv.id[i] = 0;
        } else if (g.shopOpen) {
            if (Items.isCoin(inv.id[i])) return;
            g.sell(inv.id[i], inv.count[i]);
            inv.id[i] = 0;
            inv.count[i] = 0;
        } else {
            inv.trashId = inv.id[i];
            inv.trashCount = inv.count[i];
            inv.id[i] = 0;
            inv.count[i] = 0;
        }
        g.audio.play(Audio.TICK, 0.3, 0.8);
    }

    private void lootAll(Game g) {
        World.Chest c = g.openChest;
        for (int i = 0; i < World.Chest.SIZE; i++) {
            if (c.id[i] == 0) continue;
            int left = g.inv.add(c.id[i], c.count[i]);
            c.count[i] = left;
            if (left == 0) c.id[i] = 0;
        }
        g.audio.play(Audio.PICKUP, 0.4, 1.0);
    }

    /** Moves everything except the hotbar and coins into the chest. */
    private void depositAll(Game g) {
        Inventory inv = g.inv;
        for (int i = Inventory.HOTBAR; i < Inventory.SIZE; i++) {
            if (inv.id[i] == 0 || Items.isCoin(inv.id[i])) continue;
            int left = g.openChest.add(inv.id[i], inv.count[i]);
            inv.count[i] = left;
            if (left == 0) inv.id[i] = 0;
        }
        g.audio.play(Audio.PICKUP, 0.4, 0.8);
    }

    private void takeOne(Game g, int[] ids, int[] counts, int i) {
        if (ids[i] != 0 && (g.cursorItem == 0 || g.cursorItem == ids[i]) && g.cursorCount < Items.MAX_STACK[ids[i]]) {
            g.cursorItem = ids[i];
            g.cursorCount++;
            if (--counts[i] <= 0) { ids[i] = 0; counts[i] = 0; }
            g.audio.play(Audio.TICK, 0.25, 1.6);
        }
    }

    private void clickSlot(Game g, int[] ids, int[] counts, int i) {
        int sid = ids[i], sc = counts[i];
        if (g.cursorItem == 0) {
            if (sid == 0) return;
            g.cursorItem = sid;
            g.cursorCount = sc;
            ids[i] = 0;
            counts[i] = 0;
        } else if (sid == 0) {
            ids[i] = g.cursorItem;
            counts[i] = g.cursorCount;
            g.cursorItem = 0;
            g.cursorCount = 0;
        } else if (sid == g.cursorItem) {
            int max = Items.MAX_STACK[sid];
            int put = Math.min(max - sc, g.cursorCount);
            counts[i] += put;
            g.cursorCount -= put;
            if (g.cursorCount == 0) g.cursorItem = 0;
        } else {
            ids[i] = g.cursorItem;
            counts[i] = g.cursorCount;
            g.cursorItem = sid;
            g.cursorCount = sc;
        }
        if (ids == g.inv.id && (Items.isCoin(sid) || Items.isCoin(ids[i]))) g.inv.normalizeCoins();
        g.audio.play(Audio.TICK, 0.3, 1.2);
    }

    private void clickEquip(Game g, boolean armor, int i) {
        Inventory inv = g.inv;
        int[] arr = armor ? inv.armor : inv.acc;
        int cur = g.cursorItem;
        if (cur != 0) {
            boolean fits = armor ? Items.KIND[cur] == Items.K_ARMOR && Items.SLOT[cur] == i
                    : Items.KIND[cur] == Items.K_ACCESSORY && (!inv.wears(cur) || arr[i] == cur);
            if (!fits || g.cursorCount != 1) {
                g.audio.play(Audio.TICK, 0.25, 0.6);
                return;
            }
            g.cursorItem = arr[i];
            g.cursorCount = arr[i] != 0 ? 1 : 0;
            arr[i] = cur;
        } else if (arr[i] != 0) {
            g.cursorItem = arr[i];
            g.cursorCount = 1;
            arr[i] = 0;
        }
        g.audio.play(Audio.TICK, 0.3, 1.0);
    }

    private void craft(Game g, Recipes.Recipe r, boolean toInventory) {
        if (!r.canCraft(g.inv)) {
            g.audio.play(Audio.TICK, 0.25, 0.6);
            return;
        }
        if (toInventory) {
            r.consume(g.inv);
            int left = g.inv.add(r.result, r.amount);
            if (left > 0) g.spawnThrown(r.result, left);
        } else {
            if (g.cursorItem != 0 && (g.cursorItem != r.result
                    || g.cursorCount + r.amount > Items.MAX_STACK[r.result])) return;
            r.consume(g.inv);
            g.cursorItem = r.result;
            g.cursorCount += r.amount;
        }
        g.audio.play(Audio.CRAFT, 0.5, 1.0);
        refreshRecipes(g);
    }

    // ---- conversation -------------------------------------------------------------

    /**
     * Draws (s != null) or handles clicks (s == null) of the conversation box.
     * Returns true if the mouse is over it.
     */
    private boolean dialog(Screen s, Game g, Input in, boolean draw) {
        Mob m = g.talkTo;
        int pw = Math.min(340, screenW - 20);
        int px = (screenW - pw) / 2, py = 60;
        List<String> lines = Font.wrap(g.talkText, pw - 16);
        int ph = 30 + lines.size() * 10 + 24;
        String action = m.type == Mobs.GUIDE ? "Help" : Town.shop(m.type) != null ? "Shop"
                : "Heal (" + Items.money(Town.healCost(g.player)) + ")";
        int bw = Font.width(action) + 16;
        int bx1 = px + 8, bx2 = bx1 + bw + 6, by = py + ph - 22;
        if (draw) {
            s.panel(px, py, pw, ph);
            s.textShadow(m.name + " the " + Mobs.NAME[m.type], px + 8, py + 6, Pal.UI_SEL);
            for (int i = 0; i < lines.size(); i++) s.text(lines.get(i), px + 8, py + 20 + i * 10, Pal.UI_TEXT);
            if (Ui.button(s, in, action, bx1, by, bw, 16)) doAction(g);
            if (Ui.button(s, in, "Close", bx2, by, 60, 16)) g.closeTalk();
        }
        return inRect(in.mouseX, in.mouseY, px, py, pw, ph);
    }

    private void doAction(Game g) {
        Mob m = g.talkTo;
        if (m == null) return;
        if (m.type == Mobs.GUIDE) g.talkText = Town.guideTip(g);
        else if (Town.shop(m.type) != null) {
            g.shopOpen = true;
            if (!g.inventoryOpen) g.toggleInventory();
        } else g.nurseHeal();
    }

    // ---- drawing ---------------------------------------------------------

    static void drawSlot(Screen s, int x, int y, int item, int count, boolean selected, boolean hover, boolean dim) {
        int bg = selected ? Pal.UI_BG2 : Pal.UI_BG;
        s.fillRound(x, y, SLOT, SLOT, bg, selected ? 235 : 185);
        s.fillA(x + 2, y + 1, SLOT - 4, 1, Pal.WHITE, 22);
        int edge = selected ? Pal.UI_SEL : hover ? Pal.UI_EDGE_L : Pal.UI_EDGE;
        s.rectRound(x, y, SLOT, SLOT, edge);
        if (item != 0) {
            Sprite ic = Items.ICON[item];
            if (ic.w > SLOT - 4 || ic.h > SLOT - 4) ic = ic.fit(SLOT - 4);
            s.draw(ic, x + (SLOT - ic.w) / 2, y + (SLOT - ic.h) / 2, false, dim ? 110 : 256);
            if (count > 1) Font.drawSmallNumber(s, count, x + SLOT - 3, y + SLOT - 8, Pal.WHITE, Pal.BLACK);
        }
    }

    private static void emptyIcon(Screen s, int x, int y, int kind) {
        int c = Pal.UI_EDGE;
        switch (kind) {
            case 0: s.fill(x + 6, y + 6, 8, 5, c); s.fill(x + 5, y + 9, 2, 4, c); s.fill(x + 13, y + 9, 2, 4, c); break;
            case 1: s.fill(x + 6, y + 5, 8, 10, c); s.fill(x + 4, y + 5, 2, 5, c); s.fill(x + 14, y + 5, 2, 5, c); break;
            case 2: s.fill(x + 6, y + 5, 3, 10, c); s.fill(x + 11, y + 5, 3, 10, c); s.fill(x + 6, y + 5, 8, 3, c); break;
            default: s.rectRound(x + 6, y + 6, 8, 8, c);
        }
    }

    void draw(Screen s, Game g, Input in) {
        screenW = s.w;
        screenH = s.h;
        Inventory inv = g.inv;
        // name of the selected item
        int sel = inv.selectedItem();
        String title = g.inventoryOpen ? "Inventory" : sel != 0 ? Items.NAME[sel] : "";
        s.textShadow(title, X0 + 1, 4, g.inventoryOpen ? Pal.UI_TEXT : Pal.RARITY[Items.RARITY[sel]]);

        int slots = g.inventoryOpen ? Inventory.SIZE : Inventory.HOTBAR;
        for (int i = 0; i < slots; i++) {
            boolean selected = i == inv.selected;
            drawSlot(s, slotX(i), slotY(i), inv.id[i], inv.count[i], selected, hoverKind == INV && i == hoverIndex, false);
            if (i < 10) Font.drawSmallNumber(s, (i + 1) % 10, slotX(i) + 6, slotY(i) + 2, Pal.UI_DIM, -1);
        }

        if (g.inventoryOpen) drawOpen(s, g, in);
        else drawEffects(s, g);

        drawLife(s, g);
        drawBreath(s, g);
        boolean bossBar = drawBossBar(s, g);

        // messages (above the boss bar when there is one)
        int my = s.h - 14 - (bossBar ? 26 : 0);
        for (int i = g.messages.size() - 1; i >= 0; i--) {
            Game.Message m = g.messages.get(i);
            int c = m.life > 40 ? m.color : Pal.lerp(Pal.BLACK, m.color, m.life / 40.0);
            s.textShadow(m.text, 8, my, c);
            my -= 11;
        }

        if (g.talkTo != null && !g.inventoryOpen) dialog(s, g, in, true);

        // death
        if (g.player.dead && g.deathText != null) {
            Ui.dim(s, 70);
            String t = g.deathText;
            int n = 2;
            int tw = Font.width(t) * n;
            if (tw > s.w - 20) {
                n = 1;
                tw = Font.width(t);
            }
            s.textBig(t, (s.w - tw) / 2, s.h / 2 - 20, n, 0xe04040, Pal.BLACK);
            String r = "Respawning in " + (g.player.respawnTimer / 60 + 1) + "...";
            s.textCenter(r, s.w / 2, s.h / 2 + 4, Pal.UI_TEXT);
        }

        // tooltip or creature info
        if (g.cursorItem == 0) {
            int item = 0;
            Recipes.Recipe rec = null;
            boolean shop = false;
            switch (hoverKind) {
                case INV: item = inv.id[hoverIndex]; break;
                case TRASH: item = inv.trashId; break;
                case CHEST: item = g.openChest != null ? g.openChest.id[hoverIndex] : 0; break;
                case ARMOR: item = inv.armor[hoverIndex]; break;
                case ACC: item = inv.acc[hoverIndex]; break;
                case SHOP: item = shop(g) != null ? shop(g)[hoverIndex] : 0; shop = true; break;
                case CHEAT: item = CHEAT_ITEMS[hoverIndex]; break;
                case RECIPE:
                    if (hoverIndex < recipes.size()) {
                        rec = recipes.get(hoverIndex);
                        item = rec.result;
                    }
                    break;
                default: break;
            }
            if (item != 0) tooltip(s, in.mouseX, in.mouseY, item, rec, inv, shop, g);
            else if (hoverKind == NONE) creatureInfo(s, g, in);
        }
    }

    private void creatureInfo(Screen s, Game g, Input in) {
        for (Mob m : g.town) {
            if (m.contains(g.mouseWX, g.mouseWY)) {
                String t = m.name + " the " + Mobs.NAME[m.type] + ": " + Math.max(0, m.life) + "/" + m.lifeMax;
                s.textShadow(t, in.mouseX + 10, in.mouseY + 10, Pal.UI_GOOD);
                s.textShadow("Right-click to talk", in.mouseX + 10, in.mouseY + 20, Pal.UI_DIM);
                return;
            }
        }
        for (Mob m : g.mobs) {
            if (m.contains(g.mouseWX, g.mouseWY)) {
                String t = Mobs.NAME[m.type] + ": " + Math.max(0, m.life) + "/" + m.lifeMax;
                s.textShadow(t, in.mouseX + 10, in.mouseY + 10, Pal.UI_TEXT);
                return;
            }
        }
    }

    private void drawOpen(Screen s, Game g, Input in) {
        Inventory inv = g.inv;
        // trash
        int tx = trashX(), ty = trashY();
        drawSlot(s, tx, ty, inv.trashId, inv.trashCount, false, hoverKind == TRASH, true);
        if (inv.trashId == 0) {
            s.fill(tx + 6, ty + 7, 8, 8, Pal.UI_EDGE);
            s.fill(tx + 5, ty + 6, 10, 1, Pal.UI_EDGE_L);
            s.fill(tx + 8, ty + 5, 4, 1, Pal.UI_EDGE_L);
        }
        // money
        long money = inv.money();
        if (money > 0) s.textShadow("Coins: " + Items.money(money), X0 + 1, slotY(40) + STEP + 3, Pal.UI_SEL);

        // equipment
        int ex = equipX();
        String def = "Defense " + g.player.defense;
        s.textShadow(def, ex + SLOT - Font.width(def), equipY(0) - 12, Pal.UI_TEXT);
        for (int i = 0; i < 8; i++) {
            boolean armor = i < 3;
            int item = armor ? inv.armor[i] : inv.acc[i - 3];
            boolean hv = (hoverKind == ARMOR && armor && hoverIndex == i) || (hoverKind == ACC && !armor && hoverIndex == i - 3);
            drawSlot(s, ex, equipY(i), item, 1, false, hv, false);
            if (item == 0) emptyIcon(s, ex, equipY(i), armor ? i : 3);
        }

        int cy = craftY();
        if (g.openChest != null) {
            s.textShadow(Tiles.NAME[g.world.tile(g.openChest.x, g.openChest.y)], X0 + 1, cy - 11, Pal.UI_TEXT);
            World.Chest c = g.openChest;
            for (int i = 0; i < World.Chest.SIZE; i++)
                drawSlot(s, X0 + (i % 10) * STEP, cy + (i / 10) * STEP, c.id[i], c.count[i], false,
                        hoverKind == CHEST && hoverIndex == i, false);
            int by = cy + 4 * STEP + 2;
            smallButton(s, in, "Loot all", X0, by, 70);
            smallButton(s, in, "Deposit all", X0 + 76, by, 80);
            s.textShadow("Shift + click: move", X0 + 164, by + 3, Pal.UI_DIM);
        } else if (shop(g) != null) {
            int[] shop = shop(g);
            s.textShadow("Shop", X0 + 1, cy - 11, Pal.UI_TEXT);
            s.textShadow("(shift + click to sell)", X0 + 30, cy - 11, Pal.UI_DIM);
            for (int i = 0; i < shop.length; i++)
                drawSlot(s, X0 + (i % 10) * STEP, cy + (i / 10) * STEP, shop[i], 1, false,
                        hoverKind == SHOP && hoverIndex == i, inv.money() < Items.VALUE[shop[i]]);
        } else {
            StringBuilder near = new StringBuilder();
            for (int t : new int[]{Tiles.WORKBENCH, Tiles.FURNACE, Tiles.ANVIL})
                if (g.nearStation[t]) near.append(near.length() == 0 ? "" : ", ").append(Tiles.NAME[t]);
            s.textShadow("Crafting", X0 + 1, cy - 11, Pal.UI_TEXT);
            s.textShadow(near.length() == 0 ? "(by hand)" : "(" + near + ")", X0 + 48, cy - 11, Pal.UI_DIM);
            for (int k = 0; k < CRAFT_COLS * CRAFT_ROWS; k++) {
                int r = k + craftScroll * CRAFT_COLS;
                int x = X0 + (k % CRAFT_COLS) * STEP, y = cy + (k / CRAFT_COLS) * STEP;
                if (r >= recipes.size()) {
                    s.fillRound(x, y, SLOT, SLOT, Pal.UI_BG, 70);
                    continue;
                }
                Recipes.Recipe rc = recipes.get(r);
                drawSlot(s, x, y, rc.result, rc.amount, false, hoverKind == RECIPE && r == hoverIndex, !rc.canCraft(inv));
            }
            int rows = (recipes.size() + CRAFT_COLS - 1) / CRAFT_COLS;
            if (rows > CRAFT_ROWS) {
                String sc = "Wheel: more (" + (craftScroll + 1) + "/" + (rows - CRAFT_ROWS + 1) + ")";
                s.textShadow(sc, X0 + CRAFT_COLS * STEP - Font.width(sc), cy - 11, Pal.UI_DIM);
            }
            if (recipes.isEmpty()) s.textShadow("Gather materials to see recipes", X0 + 4, cy + 6, Pal.UI_DIM);
        }
        if (cheats) drawCheats(s, g, in);
    }

    /** The developer menu: every item in the game, mining speed, god mode and the time of day. */
    private void drawCheats(Screen s, Game g, Input in) {
        int x = cheatX(), cols = cheatCols(), rows = cheatRows();
        int pw = cols * STEP + 4;
        s.panel(x - 4, CHEAT_Y - 4, pw + 4, Math.min(screenH - CHEAT_Y, rows * STEP + 42));
        s.textShadow("Cheats", x, CHEAT_Y, Pal.UI_SEL);
        String ms = String.format(java.util.Locale.ROOT, "Mining %.1fx", g.miningSpeed);
        s.textShadow(ms, x + 42, CHEAT_Y, Pal.UI_TEXT);
        g.miningSpeed = Ui.slider(s, in, x + 42, CHEAT_Y + 11, 90, g.miningSpeed, 1, 20, 0.5);
        int bx = x + 142;
        if (Ui.button(s, in, g.godMode ? "God: on" : "God: off", bx, CHEAT_Y - 1, 58, 14)) g.godMode = !g.godMode;
        if (Ui.button(s, in, g.isNight() ? "Make day" : "Make night", bx + 62, CHEAT_Y - 1, 64, 14))
            g.world.time = g.isNight() ? 7.5 : 19.6;
        if (Ui.button(s, in, "Full", bx, CHEAT_Y + 15, 58, 14)) {
            g.player.life = g.player.lifeMax;
            g.player.mana = g.player.manaCap();
            g.player.potionSickness = 0;
        }
        int total = (CHEAT_ITEMS.length + cols - 1) / cols;
        if (total > rows) {
            String sc = (cheatScroll + 1) + "/" + (total - rows + 1);
            s.textShadow(sc, bx + 62 + 64 - Font.width(sc), CHEAT_Y + 18, Pal.UI_DIM);
        }
        for (int k = 0; k < cols * rows; k++) {
            int i = k + cheatScroll * cols;
            if (i >= CHEAT_ITEMS.length) break;
            drawSlot(s, x + (k % cols) * STEP, CHEAT_GRID_Y + (k / cols) * STEP, CHEAT_ITEMS[i], 1, false,
                    hoverKind == CHEAT && hoverIndex == i, false);
        }
    }

    /** Health bar of a boss being fought, at the bottom of the screen. Returns true if drawn. */
    private boolean drawBossBar(Screen s, Game g) {
        Mob b = Boss.active(g);
        if (b == null || g.player.dead) return false;
        if (Math.abs(b.centerX() - g.player.centerX()) > s.w * 1.5 || Math.abs(b.centerY() - g.player.centerY()) > s.h * 1.5)
            return false;
        int bw = Math.min(220, s.w - 60), bx = (s.w - bw) / 2, by = s.h - 16;
        double f = Math.max(0, b.life / (double) b.lifeMax);
        s.fillRound(bx - 2, by - 2, bw + 4, 10, Pal.BLACK, 200);
        s.fill(bx, by, bw, 6, 0x3a1020);
        int fw = (int) Math.ceil(bw * f);
        s.fill(bx, by, fw, 6, Pal.lerp(0xc02a2a, 0xe05050, 0.3));
        s.fill(bx, by, fw, 2, 0xff8a8a);
        s.rectRound(bx - 2, by - 2, bw + 4, 10, Pal.UI_BOSS);
        String name = Mobs.NAME[b.type] + "  " + Math.max(0, b.life) + "/" + b.lifeMax;
        s.textCenter(name, s.w / 2, by - 11, Pal.UI_BOSS);
        return true;
    }

    private static void smallButton(Screen s, Input in, String label, int x, int y, int w) {
        boolean hv = Ui.hover(in, x, y, w, 14);
        s.fillRound(x, y, w, 14, hv ? Pal.UI_BG2 : Pal.UI_BG, 220);
        s.rectRound(x, y, w, 14, hv ? Pal.UI_SEL : Pal.UI_EDGE);
        s.textShadow(label, x + (w - Font.width(label)) / 2, y + 4, hv ? Pal.UI_SEL : Pal.UI_TEXT);
    }

    /** Potion sickness, burning and poison below the hotbar. */
    private void drawEffects(Screen s, Game g) {
        Player p = g.player;
        int y = Y0 + SLOT + 5;
        if (p.potionSickness > 0) {
            s.textShadow("Potion sickness " + (p.potionSickness / 60 + 1) + " s", X0 + 1, y, Pal.UI_DIM);
            y += 10;
        }
        if (p.onFire > 0) {
            s.textShadow("On fire!", X0 + 1, y, 0xff9a40);
            y += 10;
        }
        if (p.poisoned > 0) {
            s.textShadow("Poisoned", X0 + 1, y, 0x9ae060);
            y += 10;
        }
        if (p.chilled > 0) s.textShadow("Chilled", X0 + 1, y, Pal.ICE_L);
    }

    /** Bubbles above the head while diving. */
    private void drawBreath(Screen s, Game g) {
        Player p = g.player;
        if (p.dead || p.breath >= Player.BREATH_MAX) return;
        int n = (p.breath + 19) / 20;
        int cx = (int) Math.round(p.centerX() - g.camX), y = (int) Math.round(p.y - g.camY) - 10;
        int x0 = cx - 10 * 6 / 2;
        for (int i = 0; i < 10; i++) {
            int bx = x0 + i * 6;
            if (i < n) {
                s.disc(bx + 2, y + 2, 2.2, 0x9ad0ff, 200);
                s.pset(bx + 1, y + 1, 0xffffff);
            } else {
                s.disc(bx + 2, y + 2, 2.2, 0x3a4a6a, 90);
            }
        }
    }

    private void drawLife(Screen s, Game g) {
        Player p = g.player;
        int hearts = p.lifeMax / 20;
        int perRow = 10;
        int hw = 11;
        int rowW = Math.min(hearts, perRow) * hw;
        int x0 = s.w - rowW - 8, y0 = 15;
        String lt = "Life: " + Math.max(0, p.life) + "/" + p.lifeMax;
        s.textShadow(lt, s.w - 8 - Font.width(lt), 4, Pal.UI_TEXT);
        for (int i = 0; i < hearts; i++) {
            int x = x0 + (i % perRow) * hw, y = y0 + (i / perRow) * 10;
            double fill = Math.max(0, Math.min(1, (p.life - i * 20) / 20.0));
            s.drawFx(HEART, x, y, false, 0x505060, 150, 0, 0);
            if (fill > 0) s.drawFx(HEART, x, y, false, 0xffffff, (int) (60 + fill * 196), 0, 0);
        }
        int ly = y0 + ((hearts - 1) / perRow + 1) * 10 + 1;
        // mana stars below the hearts
        int stars = p.manaCap() / 20;
        int sx0 = s.w - stars * hw - 8;
        String mt = "Mana " + p.mana;
        s.textShadow(mt, sx0 - Font.width(mt) - 4, ly + 1, Pal.UI_MANA);
        for (int i = 0; i < stars; i++) {
            int x = sx0 + i * hw + 1;
            double fill = Math.max(0, Math.min(1, (p.mana - i * 20) / 20.0));
            s.drawFx(STAR, x, ly, false, 0x505060, 150, 0, 0);
            if (fill > 0) s.drawFx(STAR, x, ly, false, 0xffffff, (int) (60 + fill * 196), 0, 0);
        }
        ly += 13;
        if (g.inventoryOpen) return; // the equipment column is there
        String clock = g.clockText();
        String depth = g.depthText();
        s.textShadow(clock, s.w - Font.width(clock) - 8, ly, Pal.UI_TEXT);
        s.textShadow(depth, s.w - Font.width(depth) - 8, ly + 11, Pal.UI_DIM);
        World w = g.world;
        String event = w.bloodMoon && g.isNight() ? "Blood Moon"
                : w.slimeRain ? "Slime Rain " + w.slimeRainKills + "/" + Events.SLIME_RAIN_GOAL : null;
        if (event != null)
            s.textShadow(event, s.w - Font.width(event) - 8, ly + 22, w.bloodMoon ? Pal.UI_EVENT : Events.SLIME_RAIN_COLOR);
    }

    private void tooltip(Screen s, int mx, int my, int item, Recipes.Recipe r, Inventory inv, boolean shop, Game g) {
        List<String> lines = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        lines.add(Items.NAME[item]);
        cols.add(Pal.RARITY[Items.RARITY[item]]);
        int kind = Items.KIND[item];
        if (Items.DAMAGE[item] > 0 && kind != Items.K_AMMO) {
            lines.add(Items.DAMAGE[item] + (Items.isMagic(item) ? " magic damage" : Items.STYLE[item] == Items.S_SHOOT
                    ? " ranged damage" : " melee damage"));
            cols.add(Pal.UI_TEXT);
            if (Items.isMagic(item)) {
                lines.add("Uses " + Items.MANA[item] + " mana");
                cols.add(Pal.UI_MANA);
            }
            double spd = Items.USE_TIME[item];
            String speed = spd <= 15 ? "Fast speed" : spd <= 22 ? "Average speed" : "Slow speed";
            lines.add(speed + ", " + knockText(Items.KNOCK[item]));
            cols.add(Pal.UI_DIM);
        }
        if (kind == Items.K_AMMO) {
            lines.add(Items.DAMAGE[item] + " ranged damage");
            cols.add(Pal.UI_TEXT);
        }
        if (kind == Items.K_TOOL) {
            String what = Items.TOOL[item] == Tiles.TOOL_PICK ? "Pickaxe power " : Items.TOOL[item] == Tiles.TOOL_AXE
                    ? "Axe power " : "Hammer power ";
            lines.add(what + Items.POWER[item] + "%");
            cols.add(Pal.UI_TEXT);
        }
        if (kind == Items.K_ARMOR) {
            lines.add(Items.DEFENSE[item] + " defense");
            cols.add(Pal.UI_TEXT);
            if (inv.armor[Items.SLOT[item]] == item && !g.player.setBonus.isEmpty()) {
                lines.add("Set bonus: " + g.player.setBonus);
                cols.add(Pal.UI_GOOD);
            }
        }
        if (kind == Items.K_ARMOR || kind == Items.K_ACCESSORY) {
            if (!inv.wears(item)) {
                lines.add("Right-click to equip");
                cols.add(Pal.UI_DIM);
            }
        }
        if (kind == Items.K_MATERIAL && Items.MAX_STACK[item] > 1) {
            lines.add("Material");
            cols.add(Pal.UI_DIM);
        }
        if (!Items.DESC[item].isEmpty()) {
            lines.add(Items.DESC[item]);
            cols.add(Pal.UI_DIM);
        }
        if (r != null) {
            lines.add("Needs: " + Recipes.stationName(r.station));
            cols.add(Pal.UI_TEXT);
            for (int i = 0; i < r.items.length; i++) {
                int have = Recipes.have(inv, r.items[i]);
                String name = r.items[i] == Items.WOOD ? "Wood (any)" : Items.NAME[r.items[i]];
                lines.add(" " + r.counts[i] + " × " + name + "  (" + have + ")");
                cols.add(have >= r.counts[i] ? Pal.UI_GOOD : Pal.UI_BAD);
            }
        } else if (shop) {
            lines.add("Buy for " + Items.money(Items.VALUE[item]));
            cols.add(inv.money() >= Items.VALUE[item] ? Pal.UI_SEL : Pal.UI_BAD);
        } else if (Items.sellPrice(item) > 0 && !Items.isCoin(item)) {
            lines.add("Sells for " + Items.money(Items.sellPrice(item)));
            cols.add(Pal.UI_DIM);
        }
        int w = 0;
        for (String l : lines) w = Math.max(w, Font.width(l));
        w += 10;
        int h = lines.size() * 10 + 6;
        int x = mx + 12, y = my + 10;
        if (x + w > s.w) x = mx - w - 4;
        if (y + h > s.h) y = s.h - h;
        s.panel(x, y, w, h);
        for (int i = 0; i < lines.size(); i++) s.text(lines.get(i), x + 5, y + 4 + i * 10, cols.get(i));
    }

    private static String knockText(double kb) {
        if (kb <= 0.5) return "no knockback";
        if (kb < 3) return "weak knockback";
        if (kb < 5) return "average knockback";
        return "strong knockback";
    }

    /** Mouse cursor (and the item it carries). */
    static void drawCursor(Screen s, int mx, int my, int item, int count) {
        String[] arrow = {
            "X.......", "XX......", "XWX.....", "XWWX....", "XWWWX...", "XWWWWX..", "XWWWWWX.",
            "XWWWXXXX", "XWXWX...", "XX.XWX..", "....XX..",
        };
        for (int y = 0; y < arrow.length; y++)
            for (int x = 0; x < arrow[y].length(); x++) {
                char c = arrow[y].charAt(x);
                if (c == 'X') s.pset(mx + x, my + y, Pal.BLACK);
                else if (c == 'W') s.pset(mx + x, my + y, y < 4 ? 0xffe890 : Pal.UI_SEL);
            }
        if (item != 0) {
            Sprite ic = Items.ICON[item];
            s.draw(ic, mx + 7, my + 7, false, 256);
            if (count > 1) Font.drawSmallNumber(s, count, mx + 7 + ic.w + 4, my + 7 + ic.h - 3, Pal.WHITE, Pal.BLACK);
        }
    }
}
