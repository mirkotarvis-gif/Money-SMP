package net.moneysmp.commands;

import net.kyori.adventure.title.Title;
import net.moneysmp.MoneySMP;
import net.moneysmp.roles.RoleManager.Role;
import net.moneysmp.team.Team;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

import static net.moneysmp.economy.EconomyManager.format;

/** /assassin, /money, /teamleader, /teamreset, /admin, /staff, /broadcast, /moneystart */
public final class SetupCommands extends BaseCommand {

    public SetupCommands(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (!name.equals("money") && !admin(sender)) return true;
        switch (name) {
            case "assassin" -> assassin(sender, args);
            case "money" -> money(sender, args);
            case "teamleader" -> teamLeader(sender, args);
            case "teamreset" -> teamReset(sender);
            case "admin" -> role(sender, args, Role.ADMIN, "admin");
            case "staff" -> role(sender, args, Role.STAFF, "staff");
            case "broadcast" -> broadcast(sender, args);
            case "moneystart" -> moneyStart(sender, args);
            default -> { return false; }
        }
        return true;
    }

    // /assassin set [player] | remove [player] | list
    private void assassin(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "set" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            List<String> names = plugin.roles().members(Role.ASSASSIN).stream().map(u -> nameOf(Bukkit.getOfflinePlayer(u))).toList();
            Text.send(sender, "<gray>Assassins: <white><list>", Text.p("list", names.isEmpty() ? "none" : String.join(", ", names)));
            return;
        }
        OfflinePlayer target = args.length >= 2 ? offline(args[1]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            Text.send(sender, "<red>Usage: /assassin <set|remove> [player] | list");
            return;
        }
        if (sub.equals("remove")) {
            plugin.roles().remove(target.getUniqueId(), Role.ASSASSIN);
            Text.send(sender, "<green><name> is no longer an assassin.", Text.p("name", nameOf(target)));
        } else {
            plugin.roles().add(target.getUniqueId(), Role.ASSASSIN);
            Text.send(sender, "<green><name> is now an assassin for Disasters.", Text.p("name", nameOf(target)));
        }
    }

    // /money view <player|team> | set|add|remove <player> <amount>
    private void money(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Text.send(sender, "<gray>/money view <player|team>" + (sender.hasPermission("moneysmp.admin") ? " | /money <set|add|remove> <player> <amount>" : ""));
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("view")) {
            Team team = plugin.teams().get(args[1]);
            if (team != null) {
                long total = team.everyone().stream().mapToLong(u -> plugin.economy().balance(u)).sum();
                Text.send(sender, "<team> <gray>has <gold><money></gold> in total.", Text.c("team", team.display()), Text.p("money", format(total)));
                for (UUID u : team.everyone()) {
                    sender.sendMessage(Text.mm("<gray>- <white><name></white> <gold><money>",
                            Text.p("name", nameOf(Bukkit.getOfflinePlayer(u))), Text.p("money", format(plugin.economy().balance(u)))));
                }
                return;
            }
            OfflinePlayer p = offline(args[1]);
            if (p == null) {
                Text.send(sender, "<red>No player or team with that name.");
                return;
            }
            Text.send(sender, "<white><name></white> <gray>has <gold><money></gold>.", Text.p("name", nameOf(p)), Text.p("money", format(plugin.economy().balance(p.getUniqueId()))));
            return;
        }
        if (!admin(sender)) return;
        if (args.length < 3) {
            Text.send(sender, "<red>Usage: /money <set|add|remove> <player> <amount>");
            return;
        }
        OfflinePlayer p = offline(args[1]);
        Long amount = amount(args[2]);
        if (p == null || amount == null) {
            Text.send(sender, "<red>Unknown player or invalid amount.");
            return;
        }
        UUID id = p.getUniqueId();
        switch (sub) {
            case "set" -> plugin.economy().set(id, amount);
            case "add" -> plugin.economy().add(id, amount);
            case "remove" -> plugin.economy().set(id, plugin.economy().balance(id) - amount);
            default -> {
                Text.send(sender, "<red>Usage: /money <set|add|remove> <player> <amount>");
                return;
            }
        }
        Text.send(sender, "<green><name> now has <money>.", Text.p("name", nameOf(p)), Text.p("money", format(plugin.economy().balance(id))));
    }

    // /teamleader set <player> <team>
    private void teamLeader(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[0].equalsIgnoreCase("set")) {
            Text.send(sender, "<red>Usage: /teamleader set <player> <team>");
            return;
        }
        OfflinePlayer p = offline(args[1]);
        Team team = plugin.teams().get(args[2]);
        if (team == null && args.length >= 3) team = plugin.teams().create(args[2]);
        if (p == null || team == null) {
            Text.send(sender, "<red>Unknown player.");
            return;
        }
        if (!plugin.teams().makeLeader(team, p.getUniqueId())) {
            UUID old = team.leaders().iterator().next();
            plugin.teams().demote(team, old);
            plugin.teams().makeLeader(team, p.getUniqueId());
            Text.send(sender, "<gray><old> was moved back to member.", Text.p("old", nameOf(Bukkit.getOfflinePlayer(old))));
        }
        Text.broadcast("<gold>♚</gold> <white><name></white> <gray>is now the leader of</gray> <team>", Text.p("name", nameOf(p)), Text.c("team", team.display()));
    }

    private void teamReset(CommandSender sender) {
        int n = 0;
        for (Team t : plugin.teams().all()) {
            n += t.everyone().size();
            t.members().clear();
            t.leaders().clear();
            t.clearBought();
        }
        Text.broadcast("<red>All teams have been reset.");
        Text.send(sender, "<green>Removed <count> players from their teams.", Text.p("count", n));
    }

    // /admin set|remove <player>, /staff set|remove <player>
    private void role(CommandSender sender, String[] args, Role role, String label) {
        if (args.length < 2) {
            Text.send(sender, "<red>Usage: /" + label + " <set|remove> <player>");
            return;
        }
        OfflinePlayer p = offline(args[1]);
        if (p == null) {
            Text.send(sender, "<red>Unknown player.");
            return;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            plugin.roles().remove(p.getUniqueId(), role);
            Text.send(sender, "<green>Removed " + label + " from <name>.", Text.p("name", nameOf(p)));
        } else {
            plugin.roles().add(p.getUniqueId(), role);
            Text.send(sender, "<green><name> is now " + label + ".", Text.p("name", nameOf(p)));
        }
        plugin.tab().update();
    }

    private void broadcast(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Text.send(sender, "<red>Usage: /broadcast <message>");
            return;
        }
        Text.broadcast("<white>" + String.join(" ", args));
        Text.playAll(plugin.getConfig().getString("general.broadcast-sound"));
    }

    private void moneyStart(CommandSender sender, String[] args) {
        int secs = plugin.getConfig().getInt("general.grace.duration-seconds", 1800);
        if (args.length >= 1) {
            Integer v = integer(args[0]);
            if (v != null && v > 0) secs = v;
        }
        Title title = Title.title(Text.mm("<gradient:#8bebf0:#4588e3><bold>MONEY SMP"), Text.mm("<gray>has started! Grace: <white>" + Text.time(secs)),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1)));
        for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(title);
        plugin.grace().start(secs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        switch (name) {
            case "assassin" -> {
                if (args.length == 1) return filter(List.of("set", "remove", "list"), args[0]);
                if (args.length == 2) return filter(players(), args[1]);
            }
            case "money" -> {
                if (args.length == 1) return filter(List.of("view", "set", "add", "remove"), args[0]);
                if (args.length == 2) {
                    List<String> opts = new ArrayList<>(players());
                    if (args[0].equalsIgnoreCase("view")) opts.addAll(teamNames());
                    return filter(opts, args[1]);
                }
            }
            case "teamleader" -> {
                if (args.length == 1) return filter(List.of("set"), args[0]);
                if (args.length == 2) return filter(players(), args[1]);
                if (args.length == 3) return filter(teamNames(), args[2]);
            }
            case "admin", "staff" -> {
                if (args.length == 1) return filter(List.of("set", "remove"), args[0]);
                if (args.length == 2) return filter(players(), args[1]);
            }
            default -> {
            }
        }
        return List.of();
    }
}
