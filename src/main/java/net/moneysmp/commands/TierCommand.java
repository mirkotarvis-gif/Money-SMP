package net.moneysmp.commands;

import net.moneysmp.MoneySMP;
import net.moneysmp.tier.Tier;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public final class TierCommand extends BaseCommand {

    public TierCommand(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "get" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "get" -> {
                OfflinePlayer target = args.length >= 2 ? offline(args[1]) : (sender instanceof Player p ? p : null);
                if (target == null) { Text.send(sender, "<red>Usage: /tier get <player>"); return true; }
                Tier t = plugin.tiers().get(target.getUniqueId());
                Text.send(sender, "<gray><name> is <tier>", Text.p("name", nameOf(target)),
                        Text.c("tier", t == null ? Text.mm("<gray>untiered") : Text.mm("<bold>" + t + " Tier").color(plugin.tiers().color(t))));
            }
            case "set" -> {
                if (!admin(sender)) return true;
                if (args.length < 3) { Text.send(sender, "<red>Usage: /tier set <player> <S|A|B|C|D|F>"); return true; }
                OfflinePlayer target = offline(args[1]);
                Tier t = Tier.parse(args[2]);
                if (target == null || t == null) { Text.send(sender, "<red>Unknown player or tier."); return true; }
                plugin.tiers().set(target.getUniqueId(), t);
                Text.send(sender, "<green><name> is now <tier> tier.", Text.p("name", nameOf(target)), Text.p("tier", t));
            }
            case "clear" -> {
                if (!admin(sender)) return true;
                if (args.length < 2) { Text.send(sender, "<red>Usage: /tier clear <player>"); return true; }
                OfflinePlayer target = offline(args[1]);
                if (target == null) { Text.send(sender, "<red>Unknown player."); return true; }
                plugin.tiers().clear(target.getUniqueId());
                Text.send(sender, "<green>Cleared <name>'s tier.", Text.p("name", nameOf(target)));
            }
            case "list" -> {
                for (Tier t : Tier.values()) {
                    List<String> names = new ArrayList<>();
                    for (Map.Entry<UUID, Tier> e : plugin.tiers().all().entrySet()) {
                        if (e.getValue() == t) names.add(nameOf(Bukkit.getOfflinePlayer(e.getKey())));
                    }
                    sender.sendMessage(Text.mm("<bold><tier></bold> <gray>(kill: $<reward>)</gray> <white><names>",
                            Text.p("tier", t), Text.p("reward", plugin.tiers().killReward(t)),
                            Text.p("names", names.isEmpty() ? "-" : String.join(", ", names))).colorIfAbsent(plugin.tiers().color(t)));
                }
            }
            default -> Text.send(sender, "<gray>/tier get [player] | list | set <player> <tier> | clear <player>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return filter(List.of("get", "list", "set", "clear"), args[0]);
        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) return filter(players(), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) return filter(Arrays.stream(Tier.values()).map(Enum::name).toList(), args[2]);
        return List.of();
    }
}
