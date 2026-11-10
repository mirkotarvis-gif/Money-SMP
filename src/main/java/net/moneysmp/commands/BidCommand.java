package net.moneysmp.commands;

import net.moneysmp.MoneySMP;
import net.moneysmp.bidding.BiddingManager;
import net.moneysmp.tier.Tier;
import net.moneysmp.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static net.moneysmp.economy.EconomyManager.format;

/** /bid <amount|status>, /bidding <time|stop>, /bidopen <tier>, /bidclose */
public final class BidCommand extends BaseCommand {

    public BidCommand(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        BiddingManager b = plugin.bidding();
        String err = null;
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "bidding" -> {
                if (!admin(sender)) return true;
                String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
                if (sub.equals("time") || sub.equals("start")) err = b.startSession();
                else if (sub.equals("stop")) err = b.stopSession();
                else err = "Usage: /bidding <time|stop>";
            }
            case "bidopen" -> {
                if (!admin(sender)) return true;
                Tier t = args.length == 0 ? null : Tier.parse(args[0]);
                err = t == null ? "Usage: /bidopen <S|A|B|C|D|F>" : b.open(t);
            }
            case "bidclose" -> {
                if (!admin(sender)) return true;
                err = b.close();
            }
            default -> bid(sender, args, b);
        }
        if (err != null) Text.send(sender, "<red>" + err);
        return true;
    }

    private void bid(CommandSender sender, String[] args, BiddingManager b) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            BiddingManager.Lot lot = b.lot();
            if (!b.session()) Text.send(sender, "<gray>No bidding session is running.");
            else if (lot == null) Text.send(sender, "<gray>Session running. Open tier: <white><tier></white>, <count> waiting.",
                    Text.p("tier", b.openTier() == null ? "none" : b.openTier()), Text.p("count", b.queued()));
            else Text.send(sender, "<gray>Up now: <white><name></white> (<tier>) - <high> - <left>s left",
                        Text.p("name", lot.name), Text.p("tier", lot.tier),
                        Text.p("high", lot.team == null ? "no bids" : format(lot.bid) + " by " + lot.team.name()),
                        Text.p("left", lot.left));
            return;
        }
        if (!(sender instanceof Player p)) {
            Text.send(sender, "<red>Only team leaders can bid.");
            return;
        }
        Long amount = amount(args[0]);
        if (amount == null) {
            Text.send(p, "<red>Usage: /bid <amount>");
            return;
        }
        String err = b.bid(p, amount);
        if (err != null) Text.send(p, "<red>" + err);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) return List.of();
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "bidding" -> filter(List.of("time", "stop"), args[0]);
            case "bidopen" -> filter(Arrays.stream(Tier.values()).map(Enum::name).toList(), args[0]);
            case "bid" -> filter(List.of("status"), args[0]);
            default -> List.of();
        };
    }
}
