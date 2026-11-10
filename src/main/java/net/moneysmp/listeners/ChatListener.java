package net.moneysmp.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.moneysmp.MoneySMP;
import net.moneysmp.team.Team;
import net.moneysmp.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Chat format: [ADMIN] [STAFF] [Team] ♚ Name » message. (Anonymous event overrides this.) */
public final class ChatListener implements Listener {

    private final MoneySMP plugin;

    public ChatListener(MoneySMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        Component prefix = plugin.roles().prefix(p.getUniqueId());
        Team team = plugin.teams().of(p.getUniqueId());
        if (team != null) {
            prefix = team.icon().isEmpty()
                    ? prefix.append(Component.text("[" + team.name() + "] ", team.color()))
                    : prefix.append(Component.text(team.icon() + " ", NamedTextColor.WHITE));
        }
        if (plugin.teams().isLeader(p.getUniqueId())) {
            String crown = plugin.getConfig().getString("general.tab.leader-icon", "\uE000");
            prefix = prefix.append(Component.text(crown + " ", Text.isGlyph(crown) ? NamedTextColor.WHITE : NamedTextColor.GOLD));
        }
        Component name = Component.text(p.getName(), team != null ? team.color() : NamedTextColor.WHITE);
        Component full = prefix.append(name).append(Component.text(" » ", NamedTextColor.DARK_GRAY));
        e.renderer((source, displayName, message, viewer) -> full.append(message.colorIfAbsent(NamedTextColor.WHITE)));
    }
}
