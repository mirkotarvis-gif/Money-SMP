package net.moneysmp.commands;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public final class GraceCommand extends BaseCommand {

    public GraceCommand(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!admin(sender)) return true;
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> {
                int secs = plugin.getConfig().getInt("general.grace.duration-seconds", 1800);
                if (args.length >= 2) {
                    Integer v = integer(args[1]);
                    if (v == null || v <= 0) { Text.send(sender, "<red>Invalid seconds."); return true; }
                    secs = v;
                }
                plugin.grace().start(secs);
            }
            case "stop" -> plugin.grace().stop(true);
            default -> Text.send(sender, plugin.grace().active()
                    ? "<green>Grace is active: <white>" + Text.time(plugin.grace().secondsLeft()) + "</white> left."
                    : "<gray>Grace is not active. /grace start [seconds]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? filter(List.of("start", "stop", "status"), args[0]) : List.of();
    }
}
