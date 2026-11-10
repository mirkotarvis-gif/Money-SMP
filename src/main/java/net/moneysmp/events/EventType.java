package net.moneysmp.events;

import java.util.Locale;

/** Timed server events. Each key matches a section under "events:" in config.yml. */
public enum EventType {
    SHIELDLESS("shieldless", "Shieldless", false, "Shields can't be used."),
    EXHAUSTED("exhausted", "Exhausted", false, "Everyone has Slowness and Mining Fatigue."),
    FORBIDDENFRUIT("forbiddenfruit", "Forbidden Fruit", true, "Leaves can drop golden apples."),
    OPPOSITEDAY("oppositeday", "Opposite Day", false, "Day is night and night is day."),
    MINERSHAVEN("minershaven", "Miner's Haven", true, "Ores drop double and you mine faster."),
    FLIMSYARMOR("flimsyarmor", "Flimsy Armor", false, "Armor enchantments are capped."),
    DOUBLEDOUGH("doubledough", "Double Dough", true, "All money rewards are doubled!"),
    VULNERABLE("vulnerable", "Vulnerable", false, "Everyone's max health is reduced."),
    QUICKMATH("quickmath", "Quick Math", true, "Answer math questions in chat for money."),
    ANONYMOUS("anonymous", "Anonymous", false, "All names are hidden."),
    ULTRAHARDCORE("ultrahardcore", "Ultra Hardcore", false, "No natural health regeneration."),
    GLOWANDBEHOLD("glowandbehold", "Glow and Behold", false, "Everyone is glowing."),
    BREAKTIME("breaktime", "Break Time", true, "PvP is disabled.");

    private final String key;
    private final String display;
    private final boolean good;
    private final String description;

    EventType(String key, String display, boolean good, String description) {
        this.key = key;
        this.display = display;
        this.good = good;
        this.description = description;
    }

    public String key() { return key; }
    public String display() { return display; }
    public boolean good() { return good; }
    public String description() { return description; }

    public static EventType byKey(String key) {
        if (key == null) return null;
        String k = key.toLowerCase(Locale.ROOT);
        for (EventType t : values()) if (t.key.equals(k)) return t;
        return null;
    }
}
