package net.moneysmp.util;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Server-wide boss bars that are also shown to players who join later. */
public final class Bars implements Listener {

    private final Set<BossBar> active = ConcurrentHashMap.newKeySet();

    public BossBar create(Component name, BossBar.Color color) {
        BossBar bar = BossBar.bossBar(name, 1f, color, BossBar.Overlay.PROGRESS);
        active.add(bar);
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(bar);
        return bar;
    }

    public void remove(BossBar bar) {
        if (bar == null) return;
        active.remove(bar);
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
    }

    public void removeAll() {
        for (BossBar b : Set.copyOf(active)) remove(b);
    }

    public static float progress(long left, long total) {
        if (total <= 0) return 0f;
        return Math.max(0f, Math.min(1f, (float) left / total));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        for (BossBar b : active) e.getPlayer().showBossBar(b);
    }
}
