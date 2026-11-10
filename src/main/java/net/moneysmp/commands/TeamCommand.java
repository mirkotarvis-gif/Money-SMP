package net.moneysmp.commands;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.moneysmp.MoneySMP;
import net.moneysmp.team.Team;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

import static net.moneysmp.economy.EconomyManager.format;

public final class TeamCommand extends BaseCommand {

    /** Team shield glyphs from MoneySMP-ResourcePack: /team icon <team> red */
    private static final Map<String, String> ICONS = Map.of(
            "red", "\uE010", "blue", "\uE011", "green", "\uE012",
            "yellow", "\uE013", "purple", "\uE014", "orange", "\uE015");

    private static final List<String> ADMIN = List.of("create", "delete", "add", "remove", "leader", "demote", "icon", "color");

    public TeamCommand(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        if (ADMIN.contains(sub) && !admin(sender)) return true;
        switch (sub) {
            case "list" -> list(sender);
            case "info" -> info(sender, args);
            case "create" -> {
                if (args.length < 2) { usage(sender); return true; }
                Team t = plugin.teams().create(args[1]);
                if (t == null) { Text.send(sender, "<red>That team already exists."); return true; }
                if (args.length >= 3) t.color(parseColor(args[2]));
                Text.send(sender, "<green>Created team <team>.", Text.c("team", t.display()));
            }
            case "delete" -> {
                if (args.length < 2) { usage(sender); return true; }
                Text.send(sender, plugin.teams().delete(args[1]) ? "<green>Team deleted." : "<red>Unknown team.");
            }
            case "add", "leader" -> {
                if (args.length < 3) { usage(sender); return true; }
                OfflinePlayer p = offline(args[1]);
                Team t = plugin.teams().get(args[2]);
                if (p == null || t == null) { Text.send(sender, "<red>Unknown player or team."); return true; }
                if (sub.equals("add")) {
                    plugin.teams().addMember(t, p.getUniqueId());
                    Text.send(sender, "<green><name> joined <team>.", Text.p("name", nameOf(p)), Text.c("team", t.display()));
                } else if (plugin.teams().makeLeader(t, p.getUniqueId())) {
                    Text.send(sender, "<green><name> now leads <team>.", Text.p("name", nameOf(p)), Text.c("team", t.display()));
                } else {
                    Text.send(sender, "<red><team> already has <max> leader(s).", Text.c("team", t.display()), Text.p("max", plugin.teams().leadersPerTeam()));
                }
            }
            case "demote" -> {
                if (args.length < 2) { usage(sender); return true; }
                OfflinePlayer p = offline(args[1]);
                Team t = p == null ? null : plugin.teams().of(p.getUniqueId());
                if (t == null) { Text.send(sender, "<red>That player isn't on a team."); return true; }
                plugin.teams().demote(t, p.getUniqueId());
                Text.send(sender, "<green>Demoted <name>.", Text.p("name", nameOf(p)));
            }
            case "remove" -> {
                if (args.length < 2) { usage(sender); return true; }
                OfflinePlayer p = offline(args[1]);
                if (p == null) { Text.send(sender, "<red>Unknown player."); return true; }
                plugin.teams().remove(p.getUniqueId());
                Text.send(sender, "<green>Removed <name> from their team.", Text.p("name", nameOf(p)));
            }
            case "icon" -> {
                if (args.length < 2) { usage(sender); return true; }
                Team t = plugin.teams().get(args[1]);
                if (t == null) { Text.send(sender, "<red>Unknown team."); return true; }
                String icon = args.length >= 3 ? args[2] : "";
                t.icon(ICONS.getOrDefault(icon.toLowerCase(Locale.ROOT), icon));
                Text.send(sender, "<green>Icon updated.");
            }
            case "color" -> {
                if (args.length < 3) { usage(sender); return true; }
                Team t = plugin.teams().get(args[1]);
                if (t == null) { Text.send(sender, "<red>Unknown team."); return true; }
                t.color(parseColor(args[2]));
                Text.send(sender, "<green>Color updated: <team>", Text.c("team", t.display()));
            }
            default -> usage(sender);
        }
        return true;
    }

    private void list(CommandSender sender) {
        if (plugin.teams().all().isEmpty()) {
            Text.send(sender, "<gray>No teams yet.");
            return;
        }
        for (Team t : plugin.teams().all()) {
            long total = t.everyone().stream().mapToLong(u -> plugin.economy().balance(u)).sum();
            Text.send(sender, "<team> <gray>- <count> players, <gold><money></gold>",
                    Text.c("team", t.display()), Text.p("count", t.everyone().size()), Text.p("money", format(total)));
        }
    }

    private void info(CommandSender sender, String[] args) {
        Team t;
        if (args.length >= 2) t = plugin.teams().get(args[1]);
        else if (sender instanceof Player p) t = plugin.teams().of(p.getUniqueId());
        else t = null;
        if (t == null) {
            Text.send(sender, "<red>No team found. Usage: /team info <team>");
            return;
        }
        Text.send(sender, "<team> <gray>(draft budget <money>)", Text.c("team", t.display()), Text.p("money", format(t.budget())));
        for (UUID u : t.everyone()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(u);
            String role = t.leaders().contains(u) ? "<gold>★ " : "<gray>- ";
            var tier = plugin.tiers().get(u);
            sender.sendMessage(Text.mm(role + "<white><name></white> <gray><tier></gray> <gold><money>",
                    Text.p("name", nameOf(op)), Text.p("tier", tier == null ? "" : "[" + tier + "]"),
                    Text.p("money", format(plugin.economy().balance(u)))));
        }
    }

    private static TextColor parseColor(String s) {
        NamedTextColor named = NamedTextColor.NAMES.value(s.toLowerCase(Locale.ROOT));
        if (named != null) return named;
        return Text.hex(s, NamedTextColor.WHITE);
    }

    private void usage(CommandSender sender) {
        Text.send(sender, "<gray>/team list | info [team]");
        if (sender.hasPermission("moneysmp.admin")) {
            Text.send(sender, "<gray>/team create <name> [color] | delete <team> | add <player> <team> | leader <player> <team>");
            Text.send(sender, "<gray>/team demote <player> | remove <player> | icon <team> <red|blue|green|yellow|purple|orange> | color <team> <color>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("list", "info"));
            if (sender.hasPermission("moneysmp.admin")) subs.addAll(ADMIN);
            return filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "info", "delete", "icon", "color" -> filter(teamNames(), args[1]);
                case "add", "leader", "remove", "demote" -> filter(players(), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            if (sub.equals("add") || sub.equals("leader")) return filter(teamNames(), args[2]);
            if (sub.equals("color") || sub.equals("create")) return filter(NamedTextColor.NAMES.keys(), args[2]);
            if (sub.equals("icon")) return filter(ICONS.keySet(), args[2]);
        }
        return List.of();
    }
}
