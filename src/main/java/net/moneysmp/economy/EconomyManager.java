package net.moneysmp.economy;

import net.moneysmp.MoneySMP;
import net.moneysmp.events.EventType;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public final class EconomyManager {

    private final MoneySMP plugin;
    private final Map<UUID, Long> balances = new HashMap<>();
    private long starting;
    private long min;

    public EconomyManager(MoneySMP plugin) {
        this.plugin = plugin;
        reloadSettings();
    }

    public void reloadSettings() {
        starting = plugin.getConfig().getLong("economy.starting-balance", 100);
        min = plugin.getConfig().getLong("economy.min-balance", 0);
    }

    public void load(ConfigurationSection root) {
        balances.clear();
        ConfigurationSection s = root.getConfigurationSection("balances");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), s.getLong(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save(ConfigurationSection root) {
        for (Map.Entry<UUID, Long> e : balances.entrySet()) root.set("balances." + e.getKey(), e.getValue());
    }

    public void ensure(UUID id) {
        balances.putIfAbsent(id, starting);
    }

    public long balance(UUID id) {
        return balances.getOrDefault(id, starting);
    }

    public void set(UUID id, long amount) {
        balances.put(id, Math.max(min, amount));
    }

    public void add(UUID id, long amount) {
        set(id, balance(id) + amount);
    }

    /** Removes money if the result stays at or above min-balance. */
    public boolean take(UUID id, long amount) {
        long bal = balance(id);
        if (bal - amount < min) return false;
        balances.put(id, bal - amount);
        return true;
    }

    public long multiplier() {
        return plugin.events().isActive(EventType.DOUBLEDOUGH) ? 2 : 1;
    }

    /** Pays a reward (Double Dough applies) and tells the player. Returns the amount paid. */
    public long reward(UUID id, long amount, String reason) {
        if (amount <= 0) return 0;
        long paid = amount * multiplier();
        add(id, paid);
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            Text.send(p, "<green>+<money></green> <gray><reason></gray>",
                    Text.p("money", format(paid)), Text.p("reason", reason));
        }
        return paid;
    }

    public List<Map.Entry<UUID, Long>> top(int n) {
        return balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(n)
                .toList();
    }

    public static String format(long value) {
        return "$" + String.format(Locale.US, "%,d", value);
    }
}
