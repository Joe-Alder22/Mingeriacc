package mingeriacc;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import mingeriacc.Entities.Drop;
import mingeriacc.Entities.Particle;

/** Game state and logic for one world. */
final class Game {
    static final int T = Tiles.T;
    static final double HOURS_PER_TICK = 24.0 / (16 * 60 * 60); // 16-minute day
    static final double REACH_X = 5.5, REACH_Y = 4.5;
    static final int STATION_RX = 5, STATION_RY = 4;

    final World world;
    final Player player = new Player();
    final Inventory inv = new Inventory();
    final Lighting light = new Lighting();
    final List<Drop> drops = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    final List<Popup> popups = new ArrayList<>();
    final List<Message> messages = new ArrayList<>();
    final List<Mob> mobs = new ArrayList<>();
    final List<Mob> town = new ArrayList<>();
    final List<Projectile> projectiles = new ArrayList<>();
    /** Current biome around the player (Background.FOREST...). */
    int biome;
    Mob talkTo;
    String talkText = "";
    boolean shopOpen;
    World.Chest openChest;
    final Random rnd = new Random();
    final Audio audio;

    double camX, camY;
    int viewW = 640, viewH = 360;
    boolean inventoryOpen;
    int cursorItem, cursorCount;
    long ticks;
    int autosaveTimer;
    boolean saveRequested;
    boolean hidePlayer; // the player is hidden in the menu background
    String deathText;
    /** Crafting stations near the player, indexed by tile ID. */
    final boolean[] nearStation = new boolean[Tiles.COUNT];

    // targeted tile (under the mouse)
    int aimX, aimY;
    boolean aimInRange;
    double mouseWX, mouseWY;

    private final HashMap<Integer, float[]> tileDamage = new HashMap<>();
    private final HashMap<Integer, float[]> wallDamage = new HashMap<>();
    private int hintCooldown;

    static final class Popup {
        String text;
        int item, count;
        double x, y, vy = -0.35;
        int life, maxLife;
        int color;
        boolean big;
    }

    static final class Message {
        String text;
        int life;
        int color;
    }

    Game(World world, Audio audio) {
        this.world = world;
        this.audio = audio;
    }

    void startNew() {
        player.spawnAt(world);
        inv.giveStarterKit();
        town.add(Town.create(this, Mobs.GUIDE, player.centerX() + 24, player.y + Player.H));
        snapCamera();
        message("Welcome to " + world.name + "!", Pal.UI_SEL);
        message("E = inventory and crafting, ESC = menu", Pal.UI_TEXT);
    }

    void message(String text, int color) {
        Message m = new Message();
        m.text = text;
        m.life = 60 * 6;
        m.color = color;
        messages.add(m);
        if (messages.size() > 5) messages.remove(0);
    }

    // ---- update ------------------------------------------------------------

    void update(Input in, boolean uiBlocksWorld) {
        ticks++;
        world.time += HOURS_PER_TICK;
        if (world.time >= 24) {
            world.time -= 24;
            world.day++;
        }
        if (hintCooldown > 0) hintCooldown--;

        if (ticks % 30 == 0) detectBiome();
        if (player.dead) {
            Combat.updateDead(this);
        } else {
            handleKeys(in);
            applyEquipment();
            boolean wasWet = player.wet;
            player.update(world, in, true);
            if (player.landedHard) playAt(Audio.LAND, player.centerX(), 0.5, 1.0);
            if (player.fallDamage > 0) Combat.hurtPlayer(this, player.fallDamage, 0, null);
            if (player.wet != wasWet && Math.abs(player.vy) > 0.5) splash(player.centerX(), player.y + Player.H - 4);
            if (player.didDoubleJump) {
                for (int i = 0; i < 10; i++) {
                    Particle p = new Particle(player.centerX() + (rnd.nextDouble() - 0.5) * 10, player.y + Player.H,
                            (rnd.nextDouble() - 0.5) * 1.2, rnd.nextDouble() * 0.4, 25 + rnd.nextInt(15), 0xf0f4ff, 0);
                    p.noCollide = true;
                    particles.add(p);
                }
                playAt(Audio.JUMP, player.centerX(), 0.6, 1.2);
            }
            if (player.sprint && player.runTimer > 50 && player.onGround && ticks % 3 == 0)
                dust(player.centerX(), player.y + Player.H - 1, 0xd8d8e0, 1);
            playerEffects();
            player.regenerate();
        }

        updateAim(in);
        if (!player.dead && !hidePlayer) {
            if (in.mouseL && !uiBlocksWorld && cursorItem == 0 && !player.using()) useItem();
            if (in.clickR && !uiBlocksWorld && cursorItem == 0) interact();
        }
        checkDistance();

        Combat.melee(this);
        updateMobs();
        for (Mob m : town) if (!m.dead) Mobs.update(m, this);
        town.removeIf(m -> m.dead);
        if (ticks % 300 == 0 && !hidePlayer) Town.update(this);
        Combat.contact(this);
        Combat.projectiles(this);
        if (!hidePlayer) Mobs.trySpawn(this);

        world.liquids.step(3000, ticks % 5 == 0);
        if (world.liquids.hissX >= 0) {
            playAt(Audio.HISS, world.liquids.hissX * T, 0.5, 1.0);
            for (int i = 0; i < 6; i++) {
                Particle p = new Particle(world.liquids.hissX * T + rnd.nextDouble() * T, world.liquids.hissY * T,
                        (rnd.nextDouble() - 0.5) * 0.4, -0.3 - rnd.nextDouble() * 0.4, 40, 0xb0b0b8, -0.005);
                p.noCollide = true;
                particles.add(p);
            }
            world.liquids.hissX = -1;
        }

        updateDrops();
        particles.removeIf(p -> !p.update(world));
        for (Iterator<Popup> it = popups.iterator(); it.hasNext(); ) {
            Popup p = it.next();
            p.y += p.vy;
            p.vy *= 0.97;
            if (--p.life <= 0) it.remove();
        }
        for (Iterator<Message> it = messages.iterator(); it.hasNext(); ) {
            if (--it.next().life <= 0) it.remove();
        }
        decayDamage(tileDamage);
        decayDamage(wallDamage);
        randomTicks();
        ambient();
        if (ticks % 15 == 0) findStations();
        updateCamera();

        if (++autosaveTimer >= 60 * 60 * 5) {
            autosaveTimer = 0;
            saveRequested = true;
        }
    }

    private void handleKeys(Input in) {
        if (in.pressed(KeyEvent.VK_E) || in.pressed(KeyEvent.VK_I)) toggleInventory();
        for (int k = 0; k < 10; k++) {
            int code = k == 9 ? KeyEvent.VK_0 : KeyEvent.VK_1 + k;
            if (in.pressed(code) && !player.using()) selectSlot(k);
        }
        if (in.wheel != 0 && !player.using() && !inventoryOpen) {
            int s = Math.floorMod(inv.selected + (in.wheel > 0 ? 1 : -1), Inventory.HOTBAR);
            selectSlot(s);
        }
        if (in.pressed(KeyEvent.VK_Q)) throwSelected();
        if (in.pressed(KeyEvent.VK_H)) quickHeal();
    }

    void selectSlot(int s) {
        if (s != inv.selected) {
            inv.selected = s;
            audio.play(Audio.TICK, 0.25, 1.4);
        }
    }

    void toggleInventory() {
        inventoryOpen = !inventoryOpen;
        audio.play(Audio.OPEN, 0.4, inventoryOpen ? 1.0 : 0.8);
        if (!inventoryOpen) {
            returnCursorItem();
            if (openChest != null) closeChest();
            shopOpen = false;
        }
    }

    void closeChest() {
        if (openChest == null) return;
        playAt(Audio.CHEST, openChest.x * T, 0.4, 0.8);
        openChest = null;
    }

    void closeTalk() {
        if (talkTo != null) talkTo.talking = false;
        talkTo = null;
        shopOpen = false;
    }

    /** Closes chests and conversations when the player walks away. */
    private void checkDistance() {
        if (openChest != null) {
            double dx = openChest.x * T + T - player.centerX(), dy = openChest.y * T + T - player.centerY();
            if (Math.abs(dx) > 8 * T || Math.abs(dy) > 7 * T || !Tiles.isChest(world.tile(openChest.x, openChest.y))) {
                closeChest();
                if (inventoryOpen) toggleInventory();
            }
        }
        if (talkTo != null && (talkTo.dead || Math.abs(talkTo.centerX() - player.centerX()) > 10 * T
                || Math.abs(talkTo.centerY() - player.centerY()) > 8 * T || player.dead)) {
            closeTalk();
        }
    }

    boolean nearTown() {
        for (Mob m : town)
            if (m.homeX >= 0 && Math.abs(m.centerX() - player.centerX()) < 60 * T
                    && Math.abs(m.centerY() - player.centerY()) < 40 * T) return true;
        return false;
    }

    // ---- equipment and effects --------------------------------------------------

    /** Sets the player's stats from worn armour and accessories. */
    void applyEquipment() {
        Player p = player;
        p.defense = 0;
        p.moveSpeed = 1;
        p.jumpBoost = 0;
        p.regenBonus = 0;
        p.damageBonus = 0;
        p.sprint = p.doubleJump = p.noFall = p.fireImmune = p.swim = p.headLight = false;
        p.setBonus = "";
        int[] a = inv.armor;
        for (int id : a) if (id != 0) p.defense += Items.DEFENSE[id];
        p.look.helm = a[0] != 0 ? Items.ARMOR_RAMP[a[0]] : null;
        p.look.body = a[1] != 0 ? Items.ARMOR_RAMP[a[1]] : null;
        p.look.legs = a[2] != 0 ? Items.ARMOR_RAMP[a[2]] : null;
        if (a[0] == Items.MINING_HELMET) p.headLight = true;
        // set bonus: three pieces of the same metal
        if (a[0] >= Items.COPPER_HELMET && a[0] <= Items.MOLTEN_GREAVES && a[0] != 0) {
            int set = (a[0] - Items.COPPER_HELMET) / 3;
            if (a[1] == Items.COPPER_MAIL + set * 3 && a[2] == Items.COPPER_GREAVES + set * 3) {
                switch (set) {
                    case 0: case 1: p.defense += 2; p.setBonus = "+2 defense"; break;
                    case 2: case 3: p.defense += 3; p.setBonus = "+3 defense"; break;
                    case 4: p.moveSpeed += 0.15; p.setBonus = "15% faster movement"; break;
                    default: p.damageBonus += 0.1; p.fireImmune = true; p.setBonus = "10% more damage, immune to burning";
                }
            }
        }
        for (int id : inv.acc) {
            switch (id) {
                case Items.HERMES_BOOTS: p.sprint = true; break;
                case Items.CLOUD_BOTTLE: p.doubleJump = true; break;
                case Items.BAND_REGEN: p.regenBonus += 1; break;
                case Items.HORSESHOE: p.noFall = true; break;
                case Items.AGLET: p.moveSpeed += 0.05; break;
                case Items.BALLOON: p.jumpBoost += 5; break;
                case Items.ANKLET: p.moveSpeed += 0.1; break;
                case Items.OBSIDIAN_SKULL: p.fireImmune = true; break;
                case Items.FLIPPER: p.swim = true; break;
                default: break;
            }
        }
    }

    /** Lava, burning, poison and drowning. */
    private void playerEffects() {
        Player p = player;
        if (p.lavaWet) {
            Combat.hurtPlayer(this, 40, 0, null);
            if (!p.fireImmune) p.onFire = Math.max(p.onFire, 420);
        }
        if (p.wet) p.onFire = 0;
        if (p.onFire > 0) {
            if (ticks % 12 == 0) Combat.dotPlayer(this, 1, "burned to death");
            if (rnd.nextInt(2) == 0) flame(p.x + rnd.nextDouble() * p.w, p.y + rnd.nextDouble() * p.h);
        }
        if (p.poisoned > 0 && ticks % 20 == 0) Combat.dotPlayer(this, 1, "poisoned");
        if (p.breath <= 0 && ticks % 20 == 0) Combat.dotPlayer(this, 2, "drowned");
    }

    /** A flame particle. */
    void flame(double x, double y) {
        Particle p = new Particle(x, y, (rnd.nextDouble() - 0.5) * 0.4, -0.4 - rnd.nextDouble() * 0.5, 20 + rnd.nextInt(15),
                rnd.nextBoolean() ? Pal.FLAME : Pal.FLAME_R, -0.01);
        p.glow = true;
        p.noCollide = true;
        particles.add(p);
    }

    void splash(double x, double y) {
        playAt(Audio.SPLASH, x, 0.5, 0.9 + rnd.nextDouble() * 0.2);
        for (int i = 0; i < 10; i++) {
            Particle p = new Particle(x + (rnd.nextDouble() - 0.5) * 8, y, (rnd.nextDouble() - 0.5) * 1.6,
                    -1 - rnd.nextDouble() * 1.5, 30, Pal.WATER_L, 0.12);
            particles.add(p);
        }
    }

    void shootHostile(int type, double x, double y, double vx, double vy, int damage) {
        Projectile pr = new Projectile(type, x, y, vx, vy);
        pr.hostile = true;
        pr.damage = damage;
        projectiles.add(pr);
    }

    /** Works out the biome from the tiles around the player (or the camera on the title screen). */
    void detectBiome() {
        int cx = (int) ((hidePlayer ? camX + viewW / 2.0 : player.centerX()) / T);
        int cy = (int) ((hidePlayer ? camY + viewH / 2.0 : player.centerY()) / T);
        if (cy >= world.underworld) {
            biome = Background.UNDERWORLD;
            return;
        }
        int jungle = 0, corrupt = 0, sand = 0;
        for (int y = cy - 28; y <= cy + 28; y += 2)
            for (int x = cx - 45; x <= cx + 45; x += 2) {
                int t = world.tile(x, y);
                switch (t) {
                    case Tiles.MUD: case Tiles.JUNGLE_GRASS: case Tiles.MAHOGANY_TRUNK: case Tiles.MAHOGANY_LEAVES:
                        jungle++;
                        break;
                    case Tiles.EBONSTONE: case Tiles.CORRUPT_GRASS: case Tiles.EBONSAND: case Tiles.EBON_TRUNK:
                    case Tiles.EBON_LEAVES: case Tiles.DEMONITE:
                        corrupt++;
                        break;
                    case Tiles.SAND:
                        sand++;
                        break;
                    default:
                        break;
                }
            }
        boolean surface = cy < world.surfaceLevel + 10;
        if (corrupt >= 90) biome = Background.CORRUPTION;
        else if (jungle >= 130) biome = Background.JUNGLE;
        else if (surface && (cx < 110 || cx > world.w - 110)) biome = Background.OCEAN;
        else if (surface && sand >= 160) biome = Background.DESERT;
        else biome = Background.FOREST;
    }

    /** Puts the item held on the cursor back into the inventory (or on the ground if full). */
    void returnCursorItem() {
        if (cursorItem == 0) return;
        int left = inv.add(cursorItem, cursorCount);
        if (left > 0) spawnThrown(cursorItem, left);
        cursorItem = 0;
        cursorCount = 0;
    }

    private void throwSelected() {
        int item = inv.selectedItem();
        if (item == 0) return;
        inv.consumeSelected();
        spawnThrown(item, 1);
    }

    void spawnThrown(int item, int count) {
        Drop d = new Drop(player.centerX() - 3 + player.dir * 6, player.y + 6, item, count);
        d.vx = player.dir * 2.2;
        d.vy = -1.4;
        d.pickupDelay = 90;
        drops.add(d);
        audio.play(Audio.SWING, 0.3, 1.2);
    }

    private void updateAim(Input in) {
        mouseWX = camX + in.mouseX;
        mouseWY = camY + in.mouseY;
        aimX = (int) Math.floor(mouseWX / T);
        aimY = (int) Math.floor(mouseWY / T);
        double tcx = aimX * T + T / 2.0, tcy = aimY * T + T / 2.0;
        aimInRange = Math.abs(tcx - player.centerX()) <= REACH_X * T + Player.W / 2.0
                && Math.abs(tcy - player.centerY()) <= REACH_Y * T + Player.H / 2.0;
    }

    // ---- using items ---------------------------------------------------

    private void startUse(int item) {
        Player p = player;
        p.useItem = item;
        p.useStyle = Items.STYLE[item];
        p.useDuration = Math.max(1, Items.USE_TIME[item]);
        p.useTimer = p.useDuration;
        p.swingId++;
        p.dir = mouseWX >= p.centerX() ? 1 : -1;
        p.aimAngle = Math.atan2(mouseWY - p.shoulderY(), mouseWX - p.shoulderX());
    }

    private void useItem() {
        int item = inv.selectedItem();
        if (item == 0) return;
        int kind = Items.KIND[item];
        if (kind == Items.K_TOOL) {
            startUse(item);
            playAt(Audio.SWING, player.centerX(), 0.3, 0.9 + rnd.nextDouble() * 0.2);
            if (aimInRange) useTool(item, aimX, aimY);
        } else if (kind == Items.K_WEAPON) {
            if (Items.STYLE[item] == Items.S_SHOOT) {
                int slot = inv.findAmmo(Items.USE_AMMO[item]);
                if (slot < 0) {
                    hint("You need arrows (craft them at a work bench)");
                    return;
                }
                int ammo = inv.id[slot];
                inv.consume(slot);
                startUse(item);
                shoot(item, ammo);
            } else {
                startUse(item);
                playAt(Audio.SWING, player.centerX(), 0.35, 0.85 + rnd.nextDouble() * 0.2);
                if (aimInRange) {
                    int t = world.tile(aimX, aimY);
                    if (Tiles.FRAGILE[t] && t != Tiles.TORCH && t != Tiles.SAPLING) breakTile(aimX, aimY, true);
                }
            }
        } else if (kind == Items.K_TILE) {
            if (aimInRange && placeTile(Items.PLACE[item], aimX, aimY)) {
                startUse(item);
                inv.consumeSelected();
            }
        } else if (kind == Items.K_WALL) {
            if (aimInRange && placeWall(Items.PLACE[item], aimX, aimY)) {
                startUse(item);
                inv.consumeSelected();
            }
        } else if (kind == Items.K_CONSUMABLE) {
            if (consume(item)) {
                startUse(item);
                inv.consumeSelected();
            }
        } else if (kind == Items.K_BUCKET) {
            if (aimInRange && useBucket(item, aimX, aimY)) startUse(item);
        }
    }

    private boolean useBucket(int item, int tx, int ty) {
        if (!world.inside(tx, ty) || world.solid(tx, ty)) return false;
        int amount = world.liquidAt(tx, ty), type = world.liquidType(tx, ty);
        int result;
        if (item == Items.EMPTY_BUCKET) {
            if (amount < 100) return false;
            world.liquids.set(tx, ty, 0, type);
            result = type == Liquids.LAVA ? Items.LAVA_BUCKET : Items.WATER_BUCKET;
        } else {
            int want = item == Items.LAVA_BUCKET ? Liquids.LAVA : Liquids.WATER;
            if (amount > 0 && type != want) return false;
            if (amount > 200) return false;
            world.liquids.set(tx, ty, Liquids.FULL, want);
            result = Items.EMPTY_BUCKET;
        }
        inv.consumeSelected();
        int left = inv.add(result, 1);
        if (left > 0) spawnThrown(result, left);
        playAt(Audio.SPLASH, tx * T, 0.5, 1.0);
        return true;
    }

    /** Right-click: doors, chests, beds, chairs and talking to town folk. */
    private void interact() {
        // town folk under the mouse
        for (Mob m : town) {
            if (m.contains(mouseWX, mouseWY) && Math.abs(m.centerX() - player.centerX()) < 8 * T
                    && Math.abs(m.centerY() - player.centerY()) < 6 * T) {
                if (talkTo == m) return;
                closeTalk();
                talkTo = m;
                m.talking = true;
                talkText = Town.greeting(this, m);
                audio.play(Audio.MENU, 0.4, 1.0);
                return;
            }
        }
        if (!aimInRange) return;
        int tx = aimX, ty = aimY;
        int t = world.tile(tx, ty);
        if (!Tiles.isFurniture(t)) return;
        int ox = world.originX(tx, ty), oy = world.originY(tx, ty);
        switch (t) {
            case Tiles.DOOR_CLOSED: {
                int side = player.centerX() < ox * T + T / 2.0 ? 1 : -1;
                if (!openDoor(ox, oy, side)) openDoor(ox, oy, -side);
                break;
            }
            case Tiles.DOOR_OPEN: {
                int hinge = (world.meta(ox, oy) >> 6 & 1) == 1 ? ox + 1 : ox;
                for (int dy = 0; dy < 3; dy++) {
                    if (player.overlapsTile(hinge, oy + dy)) return;
                    for (Mob m : mobs) if (m.overlapsTile(hinge, oy + dy)) return;
                }
                world.removeFurniture(ox, oy);
                world.placeFurniture(Tiles.DOOR_CLOSED, hinge, oy, 0);
                playAt(Audio.DOOR, hinge * T, 0.5, 0.8);
                break;
            }
            case Tiles.CHEST: case Tiles.GOLD_CHEST: {
                World.Chest c = world.chests.get(ox + oy * world.w);
                if (c == null) {
                    c = new World.Chest(ox, oy);
                    world.chests.put(ox + oy * world.w, c);
                }
                if (openChest == c) {
                    closeChest();
                    return;
                }
                closeTalk();
                openChest = c;
                if (!inventoryOpen) toggleInventory();
                playAt(Audio.CHEST, ox * T, 0.5, 1.0);
                break;
            }
            case Tiles.BED:
                player.spawnX = ox + 1;
                player.spawnY = oy;
                message("Spawn point set", Pal.UI_GOOD);
                break;
            case Tiles.CHAIR: {
                Town.Room r = Town.check(world, ox, oy);
                message(r.reason, r.ok ? Pal.UI_GOOD : Pal.UI_BAD);
                break;
            }
            default:
                break;
        }
    }

    private boolean openDoor(int ox, int oy, int side) {
        int col = side > 0 ? ox + 1 : ox - 1;
        for (int dy = 0; dy < 3; dy++) if (!Tiles.PLACE_OVER[world.tile(col, oy + dy)]) return false;
        world.removeFurniture(ox, oy);
        if (side > 0) world.placeFurniture(Tiles.DOOR_OPEN, ox, oy, 0);
        else world.placeFurniture(Tiles.DOOR_OPEN, ox - 1, oy, 1);
        playAt(Audio.DOOR, ox * T, 0.5, 1.0);
        return true;
    }

    /** Buys an item from the merchant. */
    void buy(int item) {
        int price = Items.VALUE[item];
        if (!inv.spend(price)) {
            hint("You don't have enough money");
            return;
        }
        int left = inv.add(item, 1);
        if (left > 0) spawnThrown(item, left);
        playAt(Audio.COIN, player.centerX(), 0.5, 1.0);
    }

    /** Sells an item stack to the merchant. */
    void sell(int item, int count) {
        long v = (long) Items.sellPrice(item) * count;
        if (v > 0) inv.giveMoney(v);
        playAt(Audio.COIN, player.centerX(), 0.5, 1.2);
    }

    /** Heals the player at the nurse. */
    void nurseHeal() {
        int cost = Town.healCost(player);
        if (cost <= 0) {
            talkText = "You look healthy enough to me.";
            return;
        }
        if (!inv.spend(cost)) {
            talkText = "I'm afraid you can't afford it. It costs " + Items.money(cost) + ".";
            return;
        }
        player.life = player.lifeMax;
        player.onFire = player.poisoned = 0;
        talkText = "All better! Try to be more careful.";
        playAt(Audio.HEAL, player.centerX(), 0.6, 1.0);
    }

    private void shoot(int weapon, int ammo) {
        Player p = player;
        double speed = Items.SHOOT_SPEED[weapon] + Items.SHOOT_SPEED[ammo];
        double sx = p.shoulderX(), sy = p.shoulderY();
        int type = Items.PROJ[ammo];
        if (Items.EFFECT[weapon] == Items.FX_FIRE && type == Projectile.WOOD_ARROW) type = Projectile.FLAMING_ARROW;
        Projectile pr = new Projectile(type, sx + Math.cos(p.aimAngle) * 6, sy + Math.sin(p.aimAngle) * 6,
                Math.cos(p.aimAngle) * speed, Math.sin(p.aimAngle) * speed);
        pr.damage = (int) Math.round((Items.DAMAGE[weapon] + Items.DAMAGE[ammo]) * (1 + p.damageBonus));
        pr.knock = Items.KNOCK[weapon];
        pr.crit = Items.CRIT[weapon];
        projectiles.add(pr);
        playAt(Audio.BOW, p.centerX(), 0.5, 0.95 + rnd.nextDouble() * 0.1);
    }

    /** Uses a consumable item; returns false if it could not be used. */
    private boolean consume(int item) {
        Player p = player;
        if (item == Items.LIFE_CRYSTAL) {
            if (p.lifeMax >= Player.MAX_LIFE_CAP) {
                hint("Your maximum life can't grow any more");
                return false;
            }
            p.lifeMax += 20;
            p.life = Math.min(p.lifeMax, p.life + 20);
            number(p.centerX(), p.y - 4, 20, Pal.UI_HEAL, false);
            playAt(Audio.CRYSTAL, p.centerX(), 0.7, 1.0);
            for (int i = 0; i < 20; i++) {
                Particle pa = new Particle(p.centerX() + (rnd.nextDouble() - 0.5) * 12, p.centerY() + (rnd.nextDouble() - 0.5) * 18,
                        (rnd.nextDouble() - 0.5) * 0.6, -0.4 - rnd.nextDouble() * 0.8, 40 + rnd.nextInt(30), Pal.CRYSTAL_L, 0);
                pa.glow = true;
                pa.noCollide = true;
                particles.add(pa);
            }
            message("Maximum life is now " + p.lifeMax, Pal.CRYSTAL_L);
            return true;
        }
        int heal = Items.HEAL[item];
        if (heal > 0) {
            if (p.potionSickness > 0) {
                hint("You can't heal again yet (" + (p.potionSickness / 60 + 1) + " s)");
                return false;
            }
            if (p.life >= p.lifeMax) {
                hint("Your life is already full");
                return false;
            }
            int got = Math.min(heal, p.lifeMax - p.life);
            p.life += got;
            p.potionSickness = item == Items.MUSHROOM ? 60 * 30 : 60 * 60;
            number(p.centerX(), p.y - 4, got, Pal.UI_HEAL, false);
            playAt(Audio.HEAL, p.centerX(), 0.6, 1.0);
            return true;
        }
        return false;
    }

    private void quickHeal() {
        if (player.using()) return;
        int best = -1, bestHeal = 0;
        for (int i = 0; i < Inventory.SIZE; i++) {
            int id = inv.id[i];
            if (id != 0 && Items.HEAL[id] > bestHeal) {
                best = i;
                bestHeal = Items.HEAL[id];
            }
        }
        if (best < 0) {
            hint("You have nothing to heal with");
            return;
        }
        int item = inv.id[best];
        if (consume(item)) {
            startUse(item);
            inv.consume(best);
        }
    }

    private void useTool(int item, int tx, int ty) {
        int tool = Items.TOOL[item];
        int power = Items.POWER[item];
        if (!world.inside(tx, ty)) return;
        int t = world.tile(tx, ty);

        if (tool != Tiles.TOOL_HAMMER && Tiles.FRAGILE[t]) {
            breakTile(tx, ty, true);
            return;
        }
        if (tool == Tiles.TOOL_PICK) {
            if (t == Tiles.AIR || Tiles.TOOL[t] != Tiles.TOOL_PICK) {
                if (Tiles.isTrunk(t)) hint("Trees are chopped down with an axe");
                return;
            }
            if (ty >= world.h - 1) {
                hint("The bottom of the world cannot be broken");
                return;
            }
            if (power < Tiles.MIN_PICK[t]) {
                hint("Your pickaxe is not strong enough for " + Tiles.NAME[t]);
                return;
            }
            if (Tiles.isTrunk(world.tile(tx, ty - 1))) {
                hint("Chop down the tree with an axe first");
                return;
            }
            if (Tiles.isChest(world.tile(tx, ty - 1)) && !Tiles.isChest(t)) {
                hint("There is a chest on it");
                return;
            }
            if (Tiles.isChest(t)) {
                World.Chest c = world.chestAt(tx, ty);
                if (c != null && !c.isEmpty()) {
                    hint("Empty the chest first");
                    return;
                }
            }
            // furniture is damaged as a whole
            int kx = tx, ky = ty;
            if (Tiles.isFurniture(t)) {
                kx = world.originX(tx, ty);
                ky = world.originY(tx, ty);
            }
            if (damage(tileDamage, kx, ky, power, Tiles.HP[t])) {
                breakTile(tx, ty, true);
            } else {
                hitParticles(tx, ty, Tiles.COLOR[t], 3);
                playAt(digSound(t), tx * T, 0.45, 0.9 + rnd.nextDouble() * 0.25);
            }
        } else if (tool == Tiles.TOOL_AXE) {
            if (!Tiles.isTrunk(t)) return;
            if (damage(tileDamage, tx, ty, power, Tiles.HP[t])) {
                chopTree(tx, ty);
            } else {
                hitParticles(tx, ty, Pal.BARK_L, 3);
                playAt(Audio.CHOP, tx * T, 0.5, 0.9 + rnd.nextDouble() * 0.2);
            }
        } else if (tool == Tiles.TOOL_HAMMER) {
            int wl = world.wall(tx, ty);
            if (wl == 0 || Tiles.SOLID[t]) return;
            if (damage(wallDamage, tx, ty, power, Tiles.WALL_HP[wl])) {
                world.setWall(tx, ty, 0);
                spawnDrop(tx, ty, Tiles.WALL_DROP[wl], 1);
                hitParticles(tx, ty, Tiles.WALL_COLOR[wl], 8);
                playAt(Audio.BREAK, tx * T, 0.4, 0.8);
                checkTorch(tx, ty);
            } else {
                hitParticles(tx, ty, Tiles.WALL_COLOR[wl], 2);
                playAt(Audio.DIG_SOFT, tx * T, 0.4, 0.7);
            }
        }
    }

    private int digSound(int t) {
        switch (Tiles.SOUND[t]) {
            case Tiles.SND_SOFT: return Audio.DIG_SOFT;
            case Tiles.SND_WOOD: return Audio.CHOP;
            case Tiles.SND_GLASS: return Audio.GLASS;
            case Tiles.SND_METAL: return Audio.METAL;
            case Tiles.SND_PLANT: return Audio.GRASS;
            default: return Audio.DIG_HARD;
        }
    }

    void hint(String s) {
        if (hintCooldown == 0) {
            message(s, Pal.UI_BAD);
            hintCooldown = 120;
        }
    }

    /** Adds damage; returns true if the tile breaks. */
    private boolean damage(HashMap<Integer, float[]> map, int tx, int ty, int power, int hp) {
        int key = tx + ty * world.w;
        float[] d = map.get(key);
        if (d == null) {
            d = new float[2];
            map.put(key, d);
        }
        d[0] += power;
        d[1] = 0;
        if (d[0] >= hp) {
            map.remove(key);
            return true;
        }
        return false;
    }

    private void decayDamage(HashMap<Integer, float[]> map) {
        if (map.isEmpty()) return;
        map.values().removeIf(d -> ++d[1] > 60 * 4);
    }

    /** Crack stage 0..3 for drawing. */
    int crackStage(int tx, int ty) {
        if (tileDamage.isEmpty()) return 0;
        int t = world.tile(tx, ty);
        int kx = tx, ky = ty;
        if (Tiles.isFurniture(t)) {
            kx = world.originX(tx, ty);
            ky = world.originY(tx, ty);
        }
        float[] d = tileDamage.get(kx + ky * world.w);
        int hp = Tiles.HP[t];
        if (d == null || hp <= 0) return 0;
        return 1 + Math.min(2, (int) (d[0] * 3 / hp));
    }

    int wallCrackStage(int tx, int ty) {
        if (wallDamage.isEmpty()) return 0;
        float[] d = wallDamage.get(tx + ty * world.w);
        int hp = Tiles.WALL_HP[world.wall(tx, ty)];
        if (d == null || hp <= 0) return 0;
        return 1 + Math.min(2, (int) (d[0] * 3 / hp));
    }

    void breakTile(int tx, int ty, boolean drop) {
        int t = world.tile(tx, ty);
        if (t == Tiles.AIR) return;
        if (Tiles.isFurniture(t)) {
            breakFurniture(tx, ty, drop);
            return;
        }
        world.set(tx, ty, Tiles.AIR);
        tileDamage.remove(tx + ty * world.w);
        if (drop && Tiles.DROP[t] != 0) spawnDrop(tx, ty, Tiles.DROP[t], 1);
        hitParticles(tx, ty, Tiles.COLOR[t], Tiles.SOLID[t] ? 9 : 4);
        if (Tiles.SOUND[t] == Tiles.SND_PLANT) playAt(Audio.GRASS, tx * T, 0.3, 1.0 + rnd.nextDouble() * 0.3);
        else if (Tiles.SOUND[t] == Tiles.SND_GLASS) playAt(Audio.GLASS, tx * T, 0.5, 1.2);
        else playAt(Audio.BREAK, tx * T, 0.5, Tiles.SOLID[t] ? 0.9 + rnd.nextDouble() * 0.2 : 1.4);
        neighborsChanged(tx, ty);
    }

    private void breakFurniture(int tx, int ty, boolean drop) {
        int ox = world.originX(tx, ty), oy = world.originY(tx, ty);
        int t0 = world.tile(tx, ty);
        if (Tiles.isChest(t0)) {
            World.Chest c = world.chests.get(ox + oy * world.w);
            if (c != null && !c.isEmpty()) return;
            world.chests.remove(ox + oy * world.w);
            if (openChest == c) closeChest();
        }
        int t = world.removeFurniture(tx, ty);
        if (t == Tiles.AIR) return;
        tileDamage.remove(ox + oy * world.w);
        int fw = Tiles.FURN_W[t], fh = Tiles.FURN_H[t];
        for (int dy = 0; dy < fh; dy++)
            for (int dx = 0; dx < fw; dx++) hitParticles(ox + dx, oy + dy, Tiles.COLOR[t], 3);
        if (t == Tiles.POT) {
            potLoot(ox, oy);
            playAt(Audio.POT, ox * T, 0.6, 0.9 + rnd.nextDouble() * 0.2);
        } else {
            if (drop && Tiles.DROP[t] != 0) spawnDrop(ox, oy, Tiles.DROP[t], 1);
            playAt(t == Tiles.LIFE_CRYSTAL ? Audio.GLASS : Audio.BREAK, ox * T, 0.5, 0.9);
        }
        for (int dx = 0; dx < fw; dx++) neighborsChanged(ox + dx, oy);
    }

    private void potLoot(int tx, int ty) {
        int depth = Mobs.zone(world, ty);
        int roll = rnd.nextInt(100);
        if (roll < 12) spawnDrop(tx, ty, Items.HEALING_POTION, 1);
        else if (roll < 32) spawnDrop(tx, ty, Items.TORCH, 3 + rnd.nextInt(6));
        else if (roll < 47) spawnDrop(tx, ty, Items.WOOD_ARROW, 8 + rnd.nextInt(12));
        else if (roll < 55) spawnDrop(tx, ty, Items.MUSHROOM, 1 + rnd.nextInt(2));
        int coins = (int) ((30 + rnd.nextInt(90)) * (1 + depth * 0.8));
        spawnCoins(tx * T + T, ty * T + T, coins);
    }

    /** Checks things attached to tiles (torches, plants, furniture) after a change. */
    private void neighborsChanged(int tx, int ty) {
        int above = world.tile(tx, ty - 1);
        if (Tiles.NEEDS_GROUND[above] && !Tiles.isGrass(world.tile(tx, ty))) breakTile(tx, ty - 1, true);
        // vines hang from jungle grass
        int below = world.tile(tx, ty + 1);
        int here = world.tile(tx, ty);
        if (below == Tiles.VINE && here != Tiles.JUNGLE_GRASS && here != Tiles.VINE) breakTile(tx, ty + 1, true);
        if (Tiles.isFurniture(above)) checkFurniture(tx, ty - 1);
        checkTorch(tx, ty - 1);
        checkTorch(tx - 1, ty);
        checkTorch(tx + 1, ty);
        checkTorch(tx, ty);
    }

    private void checkFurniture(int x, int y) {
        int t = world.tile(x, y);
        int ox = world.originX(x, y), oy = world.originY(x, y);
        int by = oy + Tiles.FURN_H[t];
        for (int dx = 0; dx < Tiles.FURN_W[t]; dx++) {
            if (!world.solid(ox + dx, by) && !world.isPlatform(ox + dx, by)) {
                breakFurniture(x, y, true);
                return;
            }
        }
    }

    private boolean torchSupported(int x, int y) {
        return world.solid(x, y + 1) || world.isPlatform(x, y + 1) || world.solid(x - 1, y)
                || world.solid(x + 1, y) || world.wall(x, y) != 0;
    }

    private void checkTorch(int x, int y) {
        if (world.tile(x, y) == Tiles.TORCH && !torchSupported(x, y)) breakTile(x, y, true);
    }

    private void chopTree(int tx, int ty) {
        int trunkType = world.tile(tx, ty);
        int base = ty;
        while (Tiles.isTrunk(world.tile(tx, base + 1))) base++;
        int top = base;
        while (Tiles.isTrunk(world.tile(tx, top - 1))) top--;
        int wood = 0;
        for (int y = top; y <= base; y++) {
            world.set(tx, y, Tiles.AIR);
            tileDamage.remove(tx + y * world.w);
            wood++;
            if (Tiles.isLeaves(world.tile(tx - 1, y))) removeLeaf(tx - 1, y);
            if (Tiles.isLeaves(world.tile(tx + 1, y))) removeLeaf(tx + 1, y);
            hitParticles(tx, y, Pal.BARK, 2);
        }
        // canopy: connected leaves from the top of the trunk
        ArrayList<int[]> stack = new ArrayList<>();
        stack.add(new int[]{tx, top - 1});
        int guard = 0;
        while (!stack.isEmpty() && guard++ < 400) {
            int[] p = stack.remove(stack.size() - 1);
            if (!Tiles.isLeaves(world.tile(p[0], p[1]))) continue;
            if (Math.abs(p[0] - tx) > 5 || Math.abs(p[1] - top) > 6) continue;
            removeLeaf(p[0], p[1]);
            stack.add(new int[]{p[0] + 1, p[1]});
            stack.add(new int[]{p[0] - 1, p[1]});
            stack.add(new int[]{p[0], p[1] + 1});
            stack.add(new int[]{p[0], p[1] - 1});
        }
        spawnDrop(tx, base, Tiles.DROP[trunkType], wood);
        if (trunkType == Tiles.TRUNK && rnd.nextDouble() < 0.8) spawnDrop(tx, top, Items.ACORN, 1 + rnd.nextInt(2));
        playAt(Audio.TREE, tx * T, 0.7, 1.0);
        neighborsChanged(tx, base);
    }

    private void removeLeaf(int x, int y) {
        world.set(x, y, Tiles.AIR);
        if (rnd.nextInt(3) == 0) hitParticles(x, y, rnd.nextBoolean() ? Pal.LEAF : Pal.LEAF_L, 1);
    }

    private boolean placeTile(int t, int tx, int ty) {
        if (!world.inside(tx, ty) || ty >= world.h - 1) return false;
        if (Tiles.isFurniture(t)) return placeFurniture(t, tx, ty);
        int cur = world.tile(tx, ty);
        if (!Tiles.PLACE_OVER[cur]) return false;
        if (Tiles.SOLID[t] && player.overlapsTile(tx, ty)) return false;
        if (Tiles.SOLID[t]) {
            for (Mob m : mobs) if (m.overlapsTile(tx, ty)) return false;
        }

        if (t == Tiles.TORCH) {
            if (!torchSupported(tx, ty)) return false;
        } else if (t == Tiles.SAPLING) {
            if (world.tile(tx, ty + 1) != Tiles.GRASS || cur != Tiles.AIR) return false;
        } else {
            boolean anchor = world.wall(tx, ty) != 0;
            int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : n) {
                int nt = world.tile(tx + d[0], ty + d[1]);
                if (Tiles.SOLID[nt] || nt == Tiles.PLATFORM || Tiles.isTrunk(nt) || Tiles.isFurniture(nt)) anchor = true;
            }
            if (!anchor) return false;
        }
        world.set(tx, ty, t);
        playAt(Audio.PLACE, tx * T, 0.45, 0.9 + rnd.nextDouble() * 0.2);
        if (Tiles.SOLID[t]) neighborsChanged(tx, ty);
        return true;
    }

    private boolean placeFurniture(int t, int tx, int ty) {
        int fw = Tiles.FURN_W[t], fh = Tiles.FURN_H[t];
        int ox = tx - (fw - 1) / 2, oy = ty - (fh - 1);
        for (int dy = 0; dy < fh; dy++)
            for (int dx = 0; dx < fw; dx++) {
                int x = ox + dx, y = oy + dy;
                if (!world.inside(x, y) || !Tiles.PLACE_OVER[world.tile(x, y)]) return false;
                if (Tiles.SOLID[t] && player.overlapsTile(x, y)) return false;
            }
        for (int dx = 0; dx < fw; dx++) {
            int x = ox + dx, y = oy + fh;
            if (!world.solid(x, y) && !world.isPlatform(x, y)) return false;
        }
        if (t == Tiles.DOOR_CLOSED && !world.solid(ox, oy - 1)) return false;
        world.placeFurniture(t, ox, oy, mouseWX < player.centerX() ? 1 : 0);
        if (Tiles.isChest(t)) world.chests.put(ox + oy * world.w, new World.Chest(ox, oy));
        playAt(Audio.PLACE, tx * T, 0.5, 0.8 + rnd.nextDouble() * 0.1);
        findStations();
        return true;
    }

    private boolean placeWall(int wl, int tx, int ty) {
        if (!world.inside(tx, ty)) return false;
        if (world.wall(tx, ty) != 0 || Tiles.SOLID[world.tile(tx, ty)]) return false;
        boolean anchor = false;
        int[][] n = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : n) {
            if (world.wall(tx + d[0], ty + d[1]) != 0 || Tiles.SOLID[world.tile(tx + d[0], ty + d[1])]) anchor = true;
        }
        if (!anchor) return false;
        world.setWall(tx, ty, wl);
        playAt(Audio.PLACE, tx * T, 0.35, 0.75 + rnd.nextDouble() * 0.15);
        return true;
    }

    /** Looks for crafting stations around the player. */
    void findStations() {
        java.util.Arrays.fill(nearStation, false);
        int px = (int) (player.centerX() / T), py = (int) (player.centerY() / T);
        for (int y = py - STATION_RY; y <= py + STATION_RY; y++)
            for (int x = px - STATION_RX; x <= px + STATION_RX; x++) {
                int t = world.tile(x, y);
                if (Tiles.isFurniture(t)) nearStation[t] = true;
            }
    }

    // ---- creatures -----------------------------------------------------------

    private void updateMobs() {
        for (Mob m : mobs) {
            if (m.dead) continue;
            Mobs.update(m, this);
            if (m.type == Mobs.ZOMBIE && rnd.nextInt(900) == 0) playAt(Audio.ZOMBIE, m.centerX(), 0.4, 0.8 + rnd.nextDouble() * 0.3);
            if (Mobs.shouldDespawn(m, this)) m.dead = true;
        }
        mobs.removeIf(m -> m.dead);
    }

    // ---- dropped items -------------------------------------------------------

    void spawnDrop(int tx, int ty, int item, int count) {
        if (item <= 0 || count <= 0) return;
        Drop d = new Drop(tx * T + 1 + rnd.nextDouble(), ty * T + 1, item, count);
        d.vx = (rnd.nextDouble() - 0.5) * 1.6;
        d.vy = -1.2 - rnd.nextDouble();
        d.pickupDelay = 12;
        drops.add(d);
    }

    /** Drops coins worth the given value (in copper) at a point. */
    void spawnCoins(double x, double y, long value) {
        for (int k = Inventory.COINS.length - 1; k >= 0 && value > 0; k--) {
            int coin = Inventory.COINS[k];
            long v = Items.VALUE[coin];
            int n = (int) (value / v);
            value -= n * v;
            while (n > 0) {
                int put = Math.min(n, Items.MAX_STACK[coin]);
                Drop d = new Drop(x - 3, y - 3, coin, put);
                d.vx = (rnd.nextDouble() - 0.5) * 2;
                d.vy = -1.5 - rnd.nextDouble();
                d.pickupDelay = 20;
                drops.add(d);
                n -= put;
            }
        }
    }

    private void updateDrops() {
        for (Drop d : drops) {
            if (d.dead) continue;
            d.update(world, player, inv.canAccept(d.item));
            int dtx = (int) ((d.x + 3) / T), dty = (int) ((d.y + 3) / T);
            if (world.isLava(dtx, dty) && world.liquidAt(dtx, dty) > 60 && d.item != Items.OBSIDIAN
                    && d.item != Items.HELLSTONE && d.item != Items.LAVA_BUCKET) {
                d.dead = true;
                flame(d.x + 3, d.y);
                playAt(Audio.HISS, d.x, 0.3, 1.3);
                continue;
            }
            double dx = player.centerX() - (d.x + 3), dy = player.centerY() - (d.y + 3);
            if (!player.dead && d.pickupDelay == 0 && Math.abs(dx) < 8 && Math.abs(dy) < 13) {
                int left = inv.add(d.item, d.count);
                int got = d.count - left;
                if (got > 0) {
                    if (Items.isCoin(d.item)) {
                        playAt(Audio.COIN, player.centerX(), 0.4, 0.95 + rnd.nextDouble() * 0.15);
                    } else {
                        pickupPopup(d.item, got);
                        playAt(Audio.PICKUP, player.centerX(), 0.35, 1.0 + rnd.nextDouble() * 0.1);
                    }
                }
                d.count = left;
                if (left == 0) d.dead = true;
            }
        }
        // merge identical items lying close together
        if (ticks % 20 == 0) {
            for (int i = 0; i < drops.size(); i++) {
                Drop a = drops.get(i);
                if (a.dead) continue;
                for (int j = i + 1; j < drops.size(); j++) {
                    Drop b = drops.get(j);
                    if (b.dead || b.item != a.item) continue;
                    if (Math.abs(a.x - b.x) < 12 && Math.abs(a.y - b.y) < 12
                            && a.count + b.count <= Items.MAX_STACK[a.item]) {
                        a.count += b.count;
                        b.dead = true;
                    }
                }
            }
        }
        drops.removeIf(d -> d.dead);
        while (drops.size() > 500) drops.remove(0);
    }

    private void pickupPopup(int item, int count) {
        for (Popup p : popups) {
            if (p.item == item && p.life > 40) {
                p.count += count;
                p.text = Items.NAME[item] + (p.count > 1 ? " (" + p.count + ")" : "");
                p.life = 90;
                return;
            }
        }
        Popup p = new Popup();
        p.item = item;
        p.count = count;
        p.text = Items.NAME[item] + (count > 1 ? " (" + count + ")" : "");
        p.x = player.centerX();
        p.y = player.y - 6;
        p.life = p.maxLife = 90;
        p.color = Pal.RARITY[Items.RARITY[item]];
        popups.add(p);
    }

    /** A floating number (damage or healing). */
    void number(double x, double y, int value, int color, boolean big) {
        Popup p = new Popup();
        p.text = Integer.toString(value);
        p.x = x + (rnd.nextDouble() - 0.5) * 6;
        p.y = y;
        p.vy = -1.1;
        p.life = p.maxLife = 60;
        p.color = color;
        p.big = big;
        popups.add(p);
    }

    // ---- particles and sounds ---------------------------------------------------

    private void hitParticles(int tx, int ty, int color, int n) {
        for (int i = 0; i < n; i++) {
            Particle p = new Particle(tx * T + rnd.nextDouble() * T, ty * T + rnd.nextDouble() * T,
                    (rnd.nextDouble() - 0.5) * 2.0, -rnd.nextDouble() * 2.0, 18 + rnd.nextInt(18),
                    rnd.nextBoolean() ? color : Pal.shade(color, 180), 0.15);
            particles.add(p);
        }
        while (particles.size() > 800) particles.remove(0);
    }

    /** A burst of particles flying out from a point. */
    void burst(double x, double y, int color, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, v = speed * (0.3 + rnd.nextDouble() * 0.7);
            Particle p = new Particle(x, y, Math.cos(a) * v, Math.sin(a) * v - 0.6, 20 + rnd.nextInt(25),
                    rnd.nextBoolean() ? color : Pal.shade(color, 170), 0.12);
            particles.add(p);
        }
        while (particles.size() > 800) particles.remove(0);
    }

    void dust(double x, double y, int color, int n) {
        for (int i = 0; i < n; i++) {
            Particle p = new Particle(x, y, (rnd.nextDouble() - 0.5) * 1.2, -rnd.nextDouble(), 12 + rnd.nextInt(10),
                    color, 0.08);
            particles.add(p);
        }
    }

    /** Plays a sound at a world position (panned and quieter far away). */
    void playAt(int sound, double x, double vol, double pitch) {
        double cx = camX + viewW / 2.0;
        double dx = x - cx;
        double pan = Math.max(-1, Math.min(1, dx / (viewW * 0.6)));
        double far = Math.abs(dx) - viewW * 0.5;
        if (far > 0) vol *= Math.max(0, 1 - far / 300.0);
        if (vol > 0.01) audio.play(sound, vol, pitch, pan);
    }

    /** Fireflies at night and falling leaves by day. */
    private void ambient() {
        if (hidePlayer && ticks % 2 == 0) return;
        if (ticks % 6 != 0) return;
        int tx = (int) (camX / T) + rnd.nextInt(Math.max(1, viewW / T));
        int ty = (int) (camY / T) + rnd.nextInt(Math.max(1, viewH / T));
        if (!world.inside(tx, ty)) return;
        int t = world.tile(tx, ty);
        if (Tiles.isLeaves(t) && world.tile(tx, ty + 1) == Tiles.AIR && rnd.nextInt(3) == 0) {
            Particle p = new Particle(tx * T + rnd.nextDouble() * T, ty * T + T, 0.3, 0.25, 240,
                    rnd.nextBoolean() ? Pal.LEAF_L : Pal.LEAF, 0);
            p.noCollide = false;
            particles.add(p);
        } else if (isNight() && t == Tiles.AIR && ty < world.surfaceLevel && ty < world.skyTop[tx]
                && ty > world.surfaceAt(tx) - 8) {
            Particle p = new Particle(tx * T + rnd.nextDouble() * T, ty * T + rnd.nextDouble() * T,
                    (rnd.nextDouble() - 0.5) * 0.3, (rnd.nextDouble() - 0.5) * 0.2, 200 + rnd.nextInt(200), 0xd8f070, 0);
            p.glow = true;
            p.noCollide = true;
            p.drag = 0.995;
            particles.add(p);
        }
    }

    // ---- living world --------------------------------------------------------

    private void randomTicks() {
        int ptx = (int) (player.centerX() / T), pty = (int) (player.centerY() / T);
        for (int i = 0; i < 40; i++) {
            int x = ptx - 80 + rnd.nextInt(160);
            int y = pty - 55 + rnd.nextInt(110);
            if (!world.inside(x, y) || y < 1) continue;
            int t = world.tile(x, y);
            if (t == Tiles.DIRT && y <= world.surfaceLevel + 12) {
                boolean exposed = world.tile(x, y - 1) == Tiles.AIR || world.tile(x - 1, y) == Tiles.AIR
                        || world.tile(x + 1, y) == Tiles.AIR || world.tile(x, y + 1) == Tiles.AIR;
                if (!exposed) continue;
                boolean grassNear = false;
                for (int dy = -1; dy <= 1 && !grassNear; dy++)
                    for (int dx = -1; dx <= 1; dx++)
                        if (world.tile(x + dx, y + dy) == Tiles.GRASS) { grassNear = true; break; }
                if (grassNear && rnd.nextInt(3) == 0) world.set(x, y, Tiles.GRASS);
            } else if (t == Tiles.GRASS) {
                if (world.tile(x, y - 1) == Tiles.AIR && y - 1 < world.surfaceLevel + 5) {
                    int r = rnd.nextInt(200);
                    if (r < 4) world.set(x, y - 1, Tiles.TUFT);
                    else if (r == 4 && rnd.nextInt(4) == 0) world.set(x, y - 1, Tiles.FLOWER);
                    else if (r == 5 && rnd.nextInt(6) == 0) world.set(x, y - 1, Tiles.MUSHROOM);
                }
            } else if (t == Tiles.SAPLING) {
                if (rnd.nextInt(12) == 0) {
                    world.set(x, y, Tiles.AIR);
                    if (!WorldGen.growTree(world, x, y + 1, rnd)) world.set(x, y, Tiles.SAPLING);
                }
            } else if (t == Tiles.MUD || t == Tiles.DIRT) {
                // jungle grass on mud, corrupt grass on dirt
                int grass = t == Tiles.MUD ? Tiles.JUNGLE_GRASS : Tiles.CORRUPT_GRASS;
                if (world.tile(x, y - 1) != Tiles.AIR) continue;
                for (int d = -1; d <= 1; d++)
                    if (world.tile(x + d, y - 1) == grass || world.tile(x + d, y) == grass || world.tile(x + d, y + 1) == grass) {
                        if (rnd.nextInt(4) == 0) world.set(x, y, grass);
                        break;
                    }
            } else if (t == Tiles.JUNGLE_GRASS) {
                int r = rnd.nextInt(300);
                if (world.tile(x, y - 1) == Tiles.AIR) {
                    if (r < 4) world.set(x, y - 1, Tiles.JUNGLE_PLANT);
                    else if (r == 5 && y > world.surfaceLevel) world.set(x, y - 1, Tiles.JUNGLE_SPORE);
                }
                if (r == 6 && world.tile(x, y + 1) == Tiles.AIR) world.set(x, y + 1, Tiles.VINE);
            } else if (t == Tiles.VINE && rnd.nextInt(8) == 0 && world.tile(x, y + 1) == Tiles.AIR) {
                int len = 0;
                while (len < 10 && world.tile(x, y - len) == Tiles.VINE) len++;
                if (len < 8) world.set(x, y + 1, Tiles.VINE);
            } else if (t == Tiles.CORRUPT_GRASS && world.tile(x, y - 1) == Tiles.AIR && rnd.nextInt(150) < 3) {
                world.set(x, y - 1, Tiles.CORRUPT_PLANT);
            }
        }
    }

    // ---- camera ----------------------------------------------------------

    private double[] cameraTarget() {
        double tx = player.centerX() - viewW / 2.0;
        double ty = player.centerY() - viewH / 2.0;
        tx = Math.max(0, Math.min(world.w * T - viewW, tx));
        ty = Math.max(0, Math.min(world.h * T - viewH, ty));
        return new double[]{tx, ty};
    }

    void snapCamera() {
        double[] t = cameraTarget();
        camX = t[0];
        camY = t[1];
    }

    private void updateCamera() {
        double[] t = cameraTarget();
        camX += (t[0] - camX) * 0.22;
        camY += (t[1] - camY) * 0.22;
        if (Math.abs(t[0] - camX) > viewW || Math.abs(t[1] - camY) > viewH) snapCamera();
    }

    // ---- light and day cycle --------------------------------------------------

    /** Sunlight level for the time of day. */
    int skyLevel() {
        double h = world.time;
        double t;
        if (h >= 5.8 && h < 18.3) t = 1;
        else if (h >= 4.4 && h < 5.8) t = (h - 4.4) / 1.4;
        else if (h >= 18.3 && h < 19.6) t = 1 - (h - 18.3) / 1.3;
        else t = 0;
        return (int) Math.round(6 + t * 9);
    }

    String clockText() {
        int hh = (int) world.time;
        int mm = (int) ((world.time - hh) * 60) / 10 * 10;
        return String.format("Day %d, %02d:%02d", world.day, hh, mm);
    }

    String depthText() {
        int d = (int) (player.centerY() / T) - world.spawnY;
        if (Math.abs(d) <= 3) return "Surface";
        if (d < 0) return (-d) + " m above the surface";
        return d + " m underground";
    }

    boolean isNight() {
        return world.time >= 19.5 || world.time < 4.5;
    }

    boolean isUnderground() {
        return player.centerY() / T > (world.surfaceLevel + world.rockLevel) / 2.0;
    }
}
