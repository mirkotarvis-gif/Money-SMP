package net.moneysmp.commands;

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

/** /balance, /pay, /baltop, /eco */
public final class EconomyCommands extends BaseCommand {

    public EconomyCommands(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "balance" -> balance(sender, args);
            case "pay" -> pay(sender, args);
            case "baltop" -> baltop(sender);
            case "eco" -> eco(sender, args);
            default -> { return false; }
        }
        return true;
    }

    private void balance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                Text.send(sender, "<red>Usage: /balance <player>");
                return;
            }
            Text.send(p, "<gray>Balance: <gold><money>", Text.p("money", format(plugin.economy().balance(p.getUniqueId()))));
            return;
        }
        OfflinePlayer target = offline(args[0]);
        if (target == null) {
            Text.send(sender, "<red>Unknown player.");
            return;
        }
        Text.send(sender, "<gray><name>'s balance: <gold><money>", Text.p("name", nameOf(target)),
                Text.p("money", format(plugin.economy().balance(target.getUniqueId()))));
    }

    private void pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            Text.send(sender, "<red>Only players can pay.");
            return;
        }
        if (args.length < 2) {
            Text.send(p, "<red>Usage: /pay <player> <amount>");
            return;
        }
        OfflinePlayer target = offline(args[0]);
        Long amount = amount(args[1]);
        if (target == null) {
            Text.send(p, "<red>Unknown player.");
            return;
        }
        if (target.getUniqueId().equals(p.getUniqueId())) {
            Text.send(p, "<red>You can't pay yourself.");
            return;
        }
        if (amount == null || amount <= 0) {
            Text.send(p, "<red>Enter a positive amount.");
            return;
        }
        if (!plugin.economy().take(p.getUniqueId(), amount)) {
            Text.send(p, "<red>You don't have enough money.");
            return;
        }
        plugin.economy().add(target.getUniqueId(), amount);
        Text.send(p, "<gray>You paid <white><name></white> <gold><money></gold>.", Text.p("name", nameOf(target)), Text.p("money", format(amount)));
        Player online = target.getPlayer();
        if (online != null) {
            Text.send(online, "<white><name></white> <gray>paid you <gold><money></gold>.", Text.p("name", p.getName()), Text.p("money", format(amount)));
        }
    }

    private void baltop(CommandSender sender) {
        Text.send(sender, "<gold><bold>Richest players");
        int i = 1;
        for (Map.Entry<UUID, Long> e : plugin.economy().top(10)) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            sender.sendMessage(Text.mm("<gray>#<rank></gray> <white><name></white> <gold><money>",
                    Text.p("rank", i++), Text.p("name", nameOf(op)), Text.p("money", format(e.getValue()))));
        }
    }

    private void eco(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 3) {
            Text.send(sender, "<red>Usage: /eco <give|take|set> <player> <amount> | /eco giveteam <team> <amount>");
            return;
        }
        Long amount = amount(args[2]);
        if (amount == null) {
            Text.send(sender, "<red>Invalid amount.");
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("giveteam")) {
            Team team = plugin.teams().get(args[1]);
            if (team == null) {
                Text.send(sender, "<red>Unknown team.");
                return;
            }
            for (UUID u : team.everyone()) plugin.economy().add(u, amount);
            Text.send(sender, "<green>Gave <money> to every member of <team>.", Text.p("money", format(amount)), Text.c("team", team.display()));
            return;
        }
        OfflinePlayer target = offline(args[1]);
        if (target == null) {
            Text.send(sender, "<red>Unknown player.");
            return;
        }
        UUID id = target.getUniqueId();
        switch (sub) {
            case "give" -> plugin.economy().add(id, amount);
            case "take" -> plugin.economy().set(id, plugin.economy().balance(id) - amount);
            case "set" -> plugin.economy().set(id, amount);
            default -> {
                Text.send(sender, "<red>Unknown action.");
                return;
            }
        }
        Text.send(sender, "<green><name> now has <money>.", Text.p("name", nameOf(target)), Text.p("money", format(plugin.economy().balance(id))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("balance") || name.equals("pay")) {
            return args.length == 1 ? filter(players(), args[0]) : List.of();
        }
        if (name.equals("eco")) {
            if (args.length == 1) return filter(List.of("give", "take", "set", "giveteam"), args[0]);
            if (args.length == 2) return filter(args[0].equalsIgnoreCase("giveteam") ? teamNames() : players(), args[1]);
        }
        return List.of();
    }
}
