package net.moneysmp.games;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Ace hunt.
 * TODO gameplay: copy template-world to world-prefix + id, give setup-seconds to prepare, then a
 * grace-seconds PvP grace, then a world border starting at border-start shrinking border-shrink-amount
 * every border-shrink-seconds down to border-min. dig-limit / sky-limit = how far below/above the
 * surface players may go. Placement order -> addScore, rewards are paid on end.
 */
public final class AceHunt extends MiniGame {

    String templateWorld, worldPrefix;
    int setupSeconds, duration, graceSeconds, borderStart, borderShrinkSeconds, borderShrinkAmount, borderMin, digLimit, skyLimit;

    public AceHunt(MoneySMP plugin) {
        super(plugin, "acehunt", "Ace Hunt");
    }

    @Override
    protected void load(ConfigurationSection s) {
        templateWorld = s.getString("template-world", "");
        worldPrefix = s.getString("world-prefix", "acehunt_");
        setupSeconds = s.getInt("setup-seconds", 21600);
        duration = s.getInt("duration-seconds", 21600);
        graceSeconds = s.getInt("grace-seconds", 60);
        borderStart = s.getInt("border-start", 1000);
        borderShrinkSeconds = s.getInt("border-shrink-seconds", 120);
        borderShrinkAmount = s.getInt("border-shrink-amount", 50);
        borderMin = s.getInt("border-min", 100);
        digLimit = s.getInt("dig-limit", 20);
        skyLimit = s.getInt("sky-limit", 20);
    }

    @Override protected int countdownSeconds() { return setupSeconds; }
    @Override protected int durationSeconds() { return duration; }

    @Override
    protected void onBegin() {
        if (templateWorld.isBlank()) Text.broadcast("<red>acehunt.template-world is not set in config.yml.");
    }

    @Override
    protected void onEnd() {
    }
}
