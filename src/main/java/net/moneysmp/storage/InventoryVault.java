package net.moneysmp.storage;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * Saves players' inventories, location, xp, health and gamemode before an event and gives it all back after.
 * Stored in vault.yml so nothing is lost if the server crashes; offline players get theirs on join.
 */
public final class InventoryVault implements Listener {

    private final MoneySMP plugin;
    private final YamlFile file;

    public InventoryVault(MoneySMP plugin) {
        this.plugin = plugin;
        this.file = new YamlFile(plugin, "vault.yml");
    }

    public boolean has(UUID id) {
        return file.yaml().isConfigurationSection(id.toString());
    }

    public void store(Player p) {
        if (has(p.getUniqueId())) return;
        ConfigurationSection s = file.yaml().createSection(p.getUniqueId().toString());
        s.set("items", YamlFile.encode(p.getInventory().getContents()));
        s.set("location", p.getLocation());
        s.set("level", p.getLevel());
        s.set("exp", (double) p.getExp());
        s.set("health", p.getHealth());
        s.set("food", p.getFoodLevel());
        s.set("gamemode", p.getGameMode().name());
        file.save();
    }

    public void storeAndClear(Player p) {
        store(p);
        p.getInventory().clear();
        p.setLevel(0);
        p.setExp(0);
    }

    public boolean restore(Player p) {
        ConfigurationSection s = file.yaml().getConfigurationSection(p.getUniqueId().toString());
        if (s == null) return false;
        p.getInventory().setContents(YamlFile.decode(s.getStringList("items"), p.getInventory().getSize()));
        Location loc = s.getLocation("location");
        if (loc != null && loc.getWorld() != null) p.teleport(loc);
        p.setLevel(s.getInt("level"));
        p.setExp((float) s.getDouble("exp"));
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20 : max.getValue();
        p.setHealth(Math.max(1, Math.min(cap, s.getDouble("health", 20))));
        p.setFoodLevel(s.getInt("food", 20));
        try {
            p.setGameMode(GameMode.valueOf(s.getString("gamemode", "SURVIVAL")));
        } catch (IllegalArgumentException ignored) {
        }
        file.yaml().set(p.getUniqueId().toString(), null);
        file.save();
        Text.send(p, "<green>Your items and position were given back.");
        return true;
    }

    public int restoreAll() {
        int n = 0;
        for (Player p : Bukkit.getOnlinePlayers()) if (restore(p)) n++;
        return n;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!has(e.getPlayer().getUniqueId())) return;
        boolean eventRunning = plugin.games().all().stream().anyMatch(g -> g.state() != net.moneysmp.games.MiniGame.State.IDLE);
        if (!eventRunning) Bukkit.getScheduler().runTaskLater(plugin, () -> restore(e.getPlayer()), 20L);
    }
}
