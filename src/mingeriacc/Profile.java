package mingeriacc;

import java.io.File;
import java.util.HashMap;

/**
 * A character, saved apart from the worlds: its name and looks, and where it
 * was and where its bed is in each world. Life, mana and the inventory are
 * read from its file straight into the Player and Inventory of a game.
 */
final class Profile {
    File file;
    String name = "";
    final Humanoid.Look look = new Humanoid.Look();
    long playTicks;
    /** Shown in the character list. */
    int lifeMax = 100, manaMax = Player.BASE_MANA;
    /** Per world id: {x, y, bed x, bed y}. */
    final HashMap<Long, double[]> worlds = new HashMap<>();

    String playTimeText() {
        long min = playTicks / (60 * 60);
        return min < 60 ? min + " min" : (min / 60) + " h " + (min % 60) + " min";
    }
}
