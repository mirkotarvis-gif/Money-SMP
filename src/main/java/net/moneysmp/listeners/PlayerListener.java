package net.moneysmp.listeners;

import net.moneysmp.MoneySMP;
import net.moneysmp.events.EventType;
import net.moneysmp.team.Team;
import net.moneysmp.tier.Tier;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/** Starting balance, PvP blocking (grace / Break Time) and tier kill bounties. */
public final class PlayerListener implements Listener {

    private final MoneySMP plugin;

    public PlayerListener(MoneySMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.economy().ensure(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = attacker(e.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        if (plugin.grace().active() || plugin.events().isActive(EventType.BREAKTIME)) e.setCancelled(true);
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        Team kt = plugin.teams().of(killer.getUniqueId());
        if (kt != null && kt == plugin.teams().of(victim.getUniqueId())) return;
        Tier tier = plugin.tiers().get(victim.getUniqueId());
        if (tier == null) return;
        long reward = plugin.tiers().killReward(tier);
        String who = plugin.events().isActive(EventType.ANONYMOUS) ? "???" : victim.getName();
        plugin.economy().reward(killer.getUniqueId(), reward, "for killing " + who + " (" + tier + " tier)");
    }
}
