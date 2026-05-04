package com.studyflow.game;

/**
 * Building model for the StudyFlow city-builder game.
 * Each Building.Type represents a level unlocked by accumulating points.
 */
public class Building {

    public enum Type {
        TENT       ("🏕️", "Tent",        0,    "Your very first shelter — the journey begins!"),
        HUT        ("🛖", "Hut",          50,   "A simple wooden hut. Progress is happening!"),
        COTTAGE    ("🏠", "Cottage",      150,  "A cozy cottage. You're building momentum."),
        HOUSE      ("🏡", "House",        300,  "A comfortable house with a garden."),
        SCHOOL     ("🏫", "School",       500,  "A village school. Knowledge is power!"),
        LIBRARY    ("📚", "Library",      800,  "A great library full of wisdom."),
        TOWER      ("🗼", "Tower",        1200, "A proud tower overlooking the land."),
        CASTLE     ("🏰", "Castle",       2000, "A majestic castle. You are a true scholar!"),
        UNIVERSITY ("🎓", "University",   3000, "A renowned university of excellence."),
        PALACE     ("🏯", "Palace",       5000, "A legendary palace — the pinnacle of knowledge!");

        public final String emoji;
        public final String name;
        public final int    pointsRequired;
        public final String description;

        Type(String emoji, String name, int pts, String desc) {
            this.emoji          = emoji;
            this.name           = name;
            this.pointsRequired = pts;
            this.description    = desc;
        }
    }

    /** Returns the highest unlocked building for the given points total. */
    public static Type getBuildingForPoints(int points) {
        Type best = Type.TENT;
        for (Type t : Type.values())
            if (points >= t.pointsRequired) best = t;
        return best;
    }

    /** Returns the next building to unlock, or null if already at maximum. */
    public static Type getNextBuilding(int points) {
        for (Type t : Type.values())
            if (points < t.pointsRequired) return t;
        return null;
    }

    /**
     * Returns the percentage progress toward the next building (0–100).
     * Returns 100 if the player is already at maximum level.
     */
    public static int getProgressToNext(int points) {
        Type next = getNextBuilding(points);
        if (next == null) return 100;
        Type current = getBuildingForPoints(points);
        int from = current.pointsRequired;
        int to   = next.pointsRequired;
        if (to == from) return 100;
        return (int)(((double)(points - from) / (to - from)) * 100);
    }
}