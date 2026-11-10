package net.moneysmp.tab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.moneysmp.MoneySMP;
import net.moneysmp.economy.EconomyManager;
import net.moneysmp.events.EventType;
import net.moneysmp.team.Team;
import net.moneysmp.tier.Tier;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Tab line: [team icon] [leader icon] [rank icon] name ........ $money
 * Money is right-aligned to general.tab.width pixels.
 */
public final class TabManager {

    private final MoneySMP plugin;
    private BukkitTask task;

    public TabManager(MoneySMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("general.tab.enabled", true)) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        for (Player p : Bukkit.getOnlinePlayers()) p.playerListName(null);
    }

    public void update() {
        for (Player p : Bukkit.getOnlinePlayers()) p.playerListName(line(p));
    }

    private Component line(Player p) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("general.tab");
        int width = cfg == null ? 130 : cfg.getInt("width", 130);
        int gap = cfg == null ? 2 : cfg.getInt("money-gap", 2);
        int teamW = cfg == null ? 11 : cfg.getInt("team-icon-width", 11);
        int leaderW = cfg == null ? 10 : cfg.getInt("leader-icon-width", 10);
        int rankW = cfg == null ? 38 : cfg.getInt("rank-icon-width", 38);
        TextColor moneyColor = TextColor.color(cfg == null ? 6736191 : cfg.getInt("money-color", 6736191));
        String leaderIcon = cfg == null ? "♚" : cfg.getString("leader-icon", "♚");

        if (plugin.events().isActive(EventType.ANONYMOUS)) {
            String name = "???";
            String money = "$???";
            int spaces = Math.max(gap, (width - Text.pixelWidth(name) - Text.pixelWidth(money)) / 4);
            return Component.text(name, NamedTextColor.GRAY)
                    .append(Component.text(" ".repeat(spaces)))
                    .append(Component.text(money, moneyColor));
        }

        Team team = plugin.teams().of(p.getUniqueId());
        Tier tier = plugin.tiers().get(p.getUniqueId());
        Component out = plugin.roles().prefix(p.getUniqueId());
        int used = plugin.roles().prefixWidth(p.getUniqueId());

        if (team != null) {
            String icon = team.icon();
            if (!icon.isEmpty()) {
                out = out.append(Component.text(icon, NamedTextColor.WHITE));
                used += width(icon, teamW);
            } else {
                String tag = "[" + team.name() + "]";
                out = out.append(Component.text(tag, team.color()));
                used += Text.pixelWidth(tag);
            }
            out = out.append(Component.space());
            used += 4;
        }
        if (plugin.teams().isLeader(p.getUniqueId())) {
            out = out.append(Component.text(leaderIcon, Text.isGlyph(leaderIcon) ? NamedTextColor.WHITE : NamedTextColor.GOLD)).append(Component.space());
            used += width(leaderIcon, leaderW) + 4;
        }
        if (tier != null) {
            String icon = plugin.tiers().icon(tier);
            out = out.append(Component.text(icon, Text.isGlyph(icon) ? NamedTextColor.WHITE : plugin.tiers().color(tier))).append(Component.space());
            used += width(icon, rankW) + 4;
        }

        String name = p.getName();
        out = out.append(Component.text(name, team != null ? team.color() : NamedTextColor.WHITE));
        used += Text.pixelWidth(name);

        String money = EconomyManager.format(plugin.economy().balance(p.getUniqueId()));
        int free = width - used - Text.pixelWidth(money);
        int spaces = Math.max(gap, Math.round(free / 4f));
        return out.append(Component.text(" ".repeat(spaces))).append(Component.text(money, moneyColor));
    }

    private static int width(String icon, int configured) {
        return Text.isGlyph(icon) ? configured : Text.pixelWidth(icon);
    }
}
