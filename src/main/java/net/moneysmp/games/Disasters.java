package net.moneysmp.games;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Disasters. Last team standing wins.
 * TODO gameplay: world border starts at border-start and shrinks every shrink-interval-seconds by
 * shrink-big (or shrink-small once below shrink-threshold). Each interval roll a disaster from
 * `disasterSettings` (meteors, thunder, tornado, lava, dust, water, orbital, greed) and run it with
 * its own settings. Eliminated teams get addScore in reverse order so the survivor ranks first.
 */
public final class Disasters extends MiniGame {

    int countdown, borderStart, shrinkInterval, shrinkBig, shrinkSmall, shrinkThreshold;
    final Map<String, ConfigurationSection> disasterSettings = new LinkedHashMap<>();

    public Disasters(MoneySMP plugin) {
        super(plugin, "disasters", "Disasters");
    }

    @Override
    protected void load(ConfigurationSection s) {
        countdown = s.getInt("countdown-seconds", 30);
        borderStart = s.getInt("border-start", 500);
        shrinkInterval = s.getInt("shrink-interval-seconds", 300);
        shrinkBig = s.getInt("shrink-big", 50);
        shrinkSmall = s.getInt("shrink-small", 10);
        shrinkThreshold = s.getInt("shrink-threshold", 50);
        disasterSettings.clear();
        for (String key : s.getKeys(false)) {
            ConfigurationSection sub = s.getConfigurationSection(key);
            if (sub != null) disasterSettings.put(key, sub);
        }
    }

    @Override protected int countdownSeconds() { return countdown; }

    @Override
    protected void onBegin() {
        Text.broadcast("<gray>Disasters loaded: <white><list>", Text.p("list", String.join(", ", disasterSettings.keySet())));
    }

    @Override
    protected void onEnd() {
    }
}
