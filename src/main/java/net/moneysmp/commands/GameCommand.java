package net.moneysmp.commands;

import net.moneysmp.MoneySMP;
import net.moneysmp.games.MiniGame;
import net.moneysmp.team.Team;
import net.moneysmp.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.*;

public final class GameCommand extends BaseCommand {

    private static final List<String> SUBS = List.of("start", "end", "stop", "score", "status");

    public GameCommand(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            for (MiniGame g : plugin.games().all()) {
                Text.send(sender, "<white><id></white> <gray>- <name> (<state>)",
                        Text.p("id", g.id()), Text.p("name", g.display()), Text.p("state", g.state().name().toLowerCase(Locale.ROOT)));
            }
            Text.send(sender, "<gray>/game <id> <start|end|stop|score <team> <points>|status>");
            return true;
        }
        MiniGame g = plugin.games().get(args[0]);
        if (g == null) {
            Text.send(sender, "<red>Unknown game. Use /game to list them.");
            return true;
        }
        String sub = args.length < 2 ? "status" : args[1].toLowerCase(Locale.ROOT);
        if (!sub.equals("status") && !admin(sender)) return true;
        switch (sub) {
            case "start" -> {
                String err = g.start();
                if (err != null) Text.send(sender, "<red>" + err);
            }
            case "end" -> g.finish();
            case "stop" -> g.stop();
            case "score" -> {
                if (args.length < 4) { Text.send(sender, "<red>Usage: /game <id> score <team> <points>"); return true; }
                Team t = plugin.teams().get(args[2]);
                Long pts;
                try { pts = Long.parseLong(args[3]); } catch (NumberFormatException e) { pts = null; }
                if (t == null || pts == null) { Text.send(sender, "<red>Unknown team or invalid points."); return true; }
                g.addScore(t, pts);
                Text.send(sender, "<green><team> now has <points> points.", Text.c("team", t.display()), Text.p("points", g.score(t)));
            }
            case "status" -> {
                Text.send(sender, "<white><name></white> <gray>is <state>.", Text.p("name", g.display()), Text.p("state", g.state().name().toLowerCase(Locale.ROOT)));
                for (Team t : g.ranking()) {
                    sender.sendMessage(Text.mm("<gray>- <team> <white><points>", Text.c("team", t.display()), Text.p("points", g.score(t))));
                }
            }
            default -> {
                if (!g.handle(sender, args)) Text.send(sender, "<red>Unknown subcommand.");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return filter(plugin.games().all().stream().map(MiniGame::id).toList(), args[0]);
        MiniGame g = plugin.games().get(args[0]);
        if (g == null) return List.of();
        if (args.length == 2) {
            List<String> subs = new ArrayList<>(SUBS);
            subs.addAll(g.extraSubcommands());
            return filter(subs, args[1]);
        }
        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("score")) return filter(teamNames(), args[2]);
            if (g.extraSubcommands().contains(args[1].toLowerCase(Locale.ROOT))) return filter(players(), args[2]);
        }
        return List.of();
    }
}
