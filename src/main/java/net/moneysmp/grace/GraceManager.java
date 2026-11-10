package net.moneysmp.grace;

import net.kyori.adventure.bossbar.BossBar;
import net.moneysmp.MoneySMP;
import net.moneysmp.util.Bars;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/** PvP-free grace period with a boss bar countdown. */
public final class GraceManager {

    private final MoneySMP plugin;
    private BukkitTask task;
    private BossBar bar;
    private int total;
    private int left;

    public GraceManager(MoneySMP plugin) {
        this.plugin = plugin;
    }

    public boolean active() {
        return task != null;
    }

    public int secondsLeft() {
        return left;
    }

    public void start(int seconds) {
        stop(false);
        total = seconds;
        left = seconds;
        bar = plugin.bars().create(Text.mm("<green>Grace period: <white>" + Text.time(left)), BossBar.Color.GREEN);
        Text.broadcast("<green>Grace period started! PvP is disabled for <white><time></white>.", Text.p("time", Text.time(seconds)));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            left--;
            if (left <= 0) {
                end();
                return;
            }
            bar.name(Text.mm("<green>Grace period: <white>" + Text.time(left)));
            bar.progress(Bars.progress(left, total));
        }, 20L, 20L);
    }

    private void end() {
        cleanup();
        Text.broadcast("<red><bold>The grace period is over!</bold> PvP is now enabled.");
        Text.playAll(plugin.getConfig().getString("general.grace.sound"));
    }

    public void stop(boolean announce) {
        if (!active()) return;
        cleanup();
        if (announce) Text.broadcast("<yellow>The grace period was ended early. PvP is enabled.");
    }

    private void cleanup() {
        if (task != null) task.cancel();
        task = null;
        plugin.bars().remove(bar);
        bar = null;
        left = 0;
    }
}
