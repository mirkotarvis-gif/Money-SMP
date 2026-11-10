package net.moneysmp.tier;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public final class TierManager {

    private final MoneySMP plugin;
    private final Map<UUID, Tier> tiers = new HashMap<>();

    public TierManager(MoneySMP plugin) {
        this.plugin = plugin;
    }

    public Tier get(UUID id) {
        return tiers.get(id);
    }

    public void set(UUID id, Tier tier) {
        tiers.put(id, tier);
    }

    public void clear(UUID id) {
        tiers.remove(id);
    }

    public Map<UUID, Tier> all() {
        return Collections.unmodifiableMap(tiers);
    }

    /** Money paid to whoever kills a player of this tier. */
    public long killReward(Tier t) {
        return plugin.getConfig().getLong("tiers.rewards." + t.name(), 0);
    }

    public TextColor color(Tier t) {
        return Text.hex(plugin.getConfig().getString("bidding.title-colors." + t.name()), NamedTextColor.WHITE);
    }

    public long minBid(Tier t) {
        return plugin.getConfig().getLong("bidding.ranges." + t.name() + ".min", 0);
    }

    public long maxBid(Tier t) {
        return plugin.getConfig().getLong("bidding.ranges." + t.name() + ".max", Long.MAX_VALUE);
    }

    public String icon(Tier t) {
        return plugin.getConfig().getString("tiers.icons." + t.name(), t.name());
    }

    public void load(ConfigurationSection root) {
        tiers.clear();
        ConfigurationSection s = root.getConfigurationSection("tiers");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            Tier t = Tier.parse(s.getString(key));
            if (t == null) continue;
            try {
                tiers.put(UUID.fromString(key), t);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save(ConfigurationSection root) {
        for (Map.Entry<UUID, Tier> e : tiers.entrySet()) root.set("tiers." + e.getKey(), e.getValue().name());
    }
}
