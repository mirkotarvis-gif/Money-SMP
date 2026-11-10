package net.moneysmp.commands;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class AdminCommand extends BaseCommand {

    public AdminCommand(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!admin(sender)) return true;
        String sub = args.length == 0 ? "" : args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                plugin.reloadAll();
                Text.send(sender, "<green>Config reloaded.");
            }
            case "save" -> {
                plugin.saveAll();
                Text.send(sender, "<green>Data saved.");
            }
            default -> Text.send(sender, "<gray>MoneySMP <version> - /moneysmp <reload|save>",
                    Text.p("version", plugin.getPluginMeta().getVersion()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? filter(List.of("reload", "save"), args[0]) : List.of();
    }
}
