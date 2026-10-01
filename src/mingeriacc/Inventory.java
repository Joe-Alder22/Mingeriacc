package mingeriacc;

/** The player's inventory: 50 slots, the first 10 of which are the hotbar, and a trash slot. */
final class Inventory {
    static final int SIZE = 50, HOTBAR = 10;
    static final int[] COINS = {Items.COPPER_COIN, Items.SILVER_COIN, Items.GOLD_COIN, Items.PLATINUM_COIN};
    final int[] id = new int[SIZE];
    final int[] count = new int[SIZE];
    int selected;
    int trashId, trashCount;
    /** Worn armour (head, body, legs) and accessories. */
    final int[] armor = new int[3];
    final int[] acc = new int[5];

    boolean wears(int item) {
        for (int a : acc) if (a == item) return true;
        for (int a : armor) if (a == item) return true;
        return false;
    }

    /** Equips an item from an inventory slot (swapping with what was worn). Returns true if it was equipped. */
    boolean equipFrom(int slot) {
        int item = id[slot];
        if (item == 0) return false;
        if (Items.KIND[item] == Items.K_ARMOR) {
            int s = Items.SLOT[item];
            id[slot] = armor[s];
            count[slot] = armor[s] != 0 ? 1 : 0;
            armor[s] = item;
            return true;
        }
        if (Items.KIND[item] == Items.K_ACCESSORY) {
            if (wears(item)) return false;
            int free = -1;
            for (int i = 0; i < acc.length; i++) if (acc[i] == 0) { free = i; break; }
            if (free < 0) free = 0;
            id[slot] = acc[free];
            count[slot] = acc[free] != 0 ? 1 : 0;
            acc[free] = item;
            return true;
        }
        return false;
    }

    /** Adds items; returns the amount that did not fit. */
    int add(int item, int n) {
        if (item <= 0 || n <= 0) return 0;
        int max = Items.MAX_STACK[item];
        for (int i = 0; i < SIZE && n > 0; i++) {
            if (id[i] == item && count[i] < max) {
                int put = Math.min(n, max - count[i]);
                count[i] += put;
                n -= put;
            }
        }
        // coins and ammo prefer the end of the inventory, other items the start
        boolean back = Items.isCoin(item) || Items.KIND[item] == Items.K_AMMO;
        for (int k = 0; k < SIZE && n > 0; k++) {
            int i = back ? SIZE - 1 - k : k;
            if (back && i < HOTBAR) continue;
            if (id[i] == 0) {
                int put = Math.min(n, max);
                id[i] = item;
                count[i] = put;
                n -= put;
            }
        }
        if (back && n > 0) {
            for (int i = 0; i < HOTBAR && n > 0; i++)
                if (id[i] == 0) {
                    int put = Math.min(n, max);
                    id[i] = item;
                    count[i] = put;
                    n -= put;
                }
        }
        if (Items.isCoin(item)) normalizeCoins();
        return n;
    }

    boolean canAccept(int item) {
        if (Items.KIND[item] == Items.K_PICKUP) return true;
        int max = Items.MAX_STACK[item];
        for (int i = 0; i < SIZE; i++) {
            if (id[i] == 0) return true;
            if (id[i] == item && count[i] < max) return true;
        }
        return Items.isCoin(item) && countOf(item) > 0;
    }

    int countOf(int item) {
        int n = 0;
        for (int i = 0; i < SIZE; i++) if (id[i] == item) n += count[i];
        return n;
    }

    /** Removes items starting from the end so the hotbar is kept. */
    boolean remove(int item, int n) {
        if (countOf(item) < n) return false;
        for (int i = SIZE - 1; i >= 0 && n > 0; i--) {
            if (id[i] != item) continue;
            int take = Math.min(n, count[i]);
            count[i] -= take;
            n -= take;
            if (count[i] == 0) id[i] = 0;
        }
        return true;
    }

    int selectedItem() {
        return id[selected];
    }

    void consumeSelected() {
        consume(selected);
    }

    void consume(int slot) {
        if (id[slot] == 0) return;
        count[slot]--;
        if (count[slot] <= 0) {
            count[slot] = 0;
            id[slot] = 0;
        }
    }

    /** Slot of the first ammo of a class, or -1. */
    int findAmmo(int ammoClass) {
        for (int i = 0; i < SIZE; i++)
            if (id[i] != 0 && Items.AMMO[id[i]] == ammoClass) return i;
        return -1;
    }

    /** First slot holding an item, or -1. */
    int find(int item) {
        for (int i = 0; i < SIZE; i++) if (id[i] == item) return i;
        return -1;
    }

    // ---- coins ------------------------------------------------------------

    /** Total value of carried coins in copper. */
    long money() {
        long v = 0;
        for (int i = 0; i < SIZE; i++) if (Items.isCoin(id[i])) v += (long) Items.VALUE[id[i]] * count[i];
        return v;
    }

    /** Turns every 100 coins into one coin of the next kind. */
    void normalizeCoins() {
        for (int k = 0; k < COINS.length - 1; k++) {
            int c = countOf(COINS[k]);
            if (c >= 100) {
                int up = c / 100;
                remove(COINS[k], up * 100);
                add(COINS[k + 1], up);
            }
        }
    }

    /** Removes coins worth the given value (with change). Returns false if there is not enough. */
    boolean spend(long copper) {
        long have = money();
        if (have < copper) return false;
        for (int c : COINS) remove(c, countOf(c));
        giveMoney(have - copper);
        return true;
    }

    void giveMoney(long copper) {
        for (int k = COINS.length - 1; k >= 0; k--) {
            long v = Items.VALUE[COINS[k]];
            int n = (int) (copper / v);
            copper -= n * v;
            while (n > 0) {
                int put = Math.min(n, Items.MAX_STACK[COINS[k]]);
                add(COINS[k], put);
                n -= put;
            }
        }
    }

    void clear() {
        java.util.Arrays.fill(id, 0);
        java.util.Arrays.fill(count, 0);
        java.util.Arrays.fill(armor, 0);
        java.util.Arrays.fill(acc, 0);
        trashId = trashCount = 0;
    }

    Inventory copy() {
        Inventory o = new Inventory();
        System.arraycopy(id, 0, o.id, 0, SIZE);
        System.arraycopy(count, 0, o.count, 0, SIZE);
        System.arraycopy(armor, 0, o.armor, 0, armor.length);
        System.arraycopy(acc, 0, o.acc, 0, acc.length);
        o.selected = selected;
        o.trashId = trashId;
        o.trashCount = trashCount;
        return o;
    }

    void giveStarterKit() {
        clear();
        id[0] = Items.COPPER_SHORTSWORD; count[0] = 1;
        id[1] = Items.COPPER_PICK; count[1] = 1;
        id[2] = Items.COPPER_AXE; count[2] = 1;
        id[3] = Items.WOOD_HAMMER; count[3] = 1;
        id[4] = Items.TORCH; count[4] = 20;
        selected = 1;
    }
}
