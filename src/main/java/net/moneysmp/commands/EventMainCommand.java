package net.moneysmp.commands;

import net.moneysmp.MoneySMP;
import net.moneysmp.build.Builder;
import net.moneysmp.build.Buildings;
import net.moneysmp.games.MiniGame;
import net.moneysmp.util.Text;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /eventmain <event> setmap|start|stop
 * /eventmain gladiators kit set|edit|remove|give|list [name] [player]
 * /eventmain point set|remove|list [name]
 * /eventmain build <lobby|biddinghall|gladiators|capturepoint|podium> [name] | undo | list
 */
public final class EventMainCommand extends BaseCommand {

    public EventMainCommand(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!admin(sender)) return true;
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        if (first.equals("point")) return point(sender, args);
        if (first.equals("build")) return build(sender, args);
        if (first.equals("gladiators") && args.length >= 2 && args[1].equalsIgnoreCase("kit")) return kit(sender, args);

        MiniGame game = plugin.games().get(first);
        if (game == null || args.length < 2) {
            help(sender);
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "setmap" -> {
                if (!(sender instanceof Player p)) return needPlayer(sender);
                plugin.locations().setMap(game.id(), p.getLocation());
                Text.send(sender, "<green>Map for <white><name></white> set to where you are standing.", Text.p("name", game.display()));
            }
            case "start" -> {
                if (game.state() != MiniGame.State.IDLE) {
                    Text.send(sender, "<red><name> is already running.", Text.p("name", game.display()));
                    return true;
                }
                Location map = plugin.locations().map(game.id());
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    plugin.vault().storeAndClear(p);
                    if (map != null) p.teleport(map);
                }
                if (map == null) Text.send(sender, "<yellow>No map set for this event, players stayed where they are.");
                String err = game.start();
                if (err != null) Text.send(sender, "<red>" + err);
            }
            case "stop" -> {
                if (game.state() == MiniGame.State.RUNNING) game.finish();
                else game.stop();
                int n = plugin.vault().restoreAll();
                Text.send(sender, "<green>Event stopped. Gave <count> players their stuff back.", Text.p("count", n));
            }
            default -> help(sender);
        }
        return true;
    }

    private boolean kit(CommandSender sender, String[] args) {
        String sub = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "list";
        String name = args.length >= 4 ? args[3] : null;
        var kits = plugin.kits();
        switch (sub) {
            case "list" -> Text.send(sender, "<gray>Kits: <white><list>", Text.p("list", kits.names().isEmpty() ? "none" : String.join(", ", kits.names())));
            case "set" -> {
                if (!(sender instanceof Player p)) return needPlayer(sender);
                if (name == null) return usage(sender, "/eventmain gladiators kit set <name>");
                kits.save(name, p);
                Text.send(sender, "<green>Saved your inventory as kit <white><name></white>.", Text.p("name", name));
            }
            case "edit" -> {
                if (!(sender instanceof Player p)) return needPlayer(sender);
                if (name == null) return usage(sender, "/eventmain gladiators kit edit <name>");
                kits.openEditor(p, name);
            }
            case "remove" -> {
                if (name == null) return usage(sender, "/eventmain gladiators kit remove <name>");
                Text.send(sender, kits.remove(name) ? "<green>Kit deleted." : "<red>No kit with that name.");
            }
            case "give" -> {
                if (name == null) return usage(sender, "/eventmain gladiators kit give <name> [player|all]");
                List<Player> targets = new ArrayList<>();
                if (args.length >= 5 && args[4].equalsIgnoreCase("all")) targets.addAll(org.bukkit.Bukkit.getOnlinePlayers());
                else if (args.length >= 5) {
                    Player t = org.bukkit.Bukkit.getPlayerExact(args[4]);
                    if (t != null) targets.add(t);
                } else if (sender instanceof Player p) targets.add(p);
                if (targets.isEmpty()) return usage(sender, "/eventmain gladiators kit give <name> [player|all]");
                for (Player t : targets) {
                    if (!kits.give(name, t)) {
                        Text.send(sender, "<red>No kit with that name.");
                        return true;
                    }
                }
                Text.send(sender, "<green>Gave kit <white><name></white> to <count> player(s).", Text.p("name", name), Text.p("count", targets.size()));
            }
            default -> usage(sender, "/eventmain gladiators kit <set|edit|remove|give|list> [name]");
        }
        return true;
    }

    private boolean point(CommandSender sender, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "list" -> {
                if (plugin.locations().points().isEmpty()) {
                    Text.send(sender, "<gray>No capture points yet.");
                    return true;
                }
                plugin.locations().points().forEach((n, l) -> Text.send(sender, "<white><name></white> <gray><world> <x>, <y>, <z>",
                        Text.p("name", n), Text.p("world", l.getWorld() == null ? "?" : l.getWorld().getName()),
                        Text.p("x", l.getBlockX()), Text.p("y", l.getBlockY()), Text.p("z", l.getBlockZ())));
            }
            case "set" -> {
                if (!(sender instanceof Player p)) return needPlayer(sender);
                if (args.length < 3) return usage(sender, "/eventmain point set <name>");
                plugin.locations().setPoint(args[2], p.getLocation());
                Text.send(sender, "<green>Capture point <white><name></white> created here.", Text.p("name", args[2]));
            }
            case "remove" -> {
                if (args.length < 3) return usage(sender, "/eventmain point remove <name>");
                Text.send(sender, plugin.locations().removePoint(args[2]) ? "<green>Capture point deleted." : "<red>No capture point with that name.");
            }
            default -> usage(sender, "/eventmain point <set|remove|list> [name]");
        }
        return true;
    }

    private boolean build(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return needPlayer(sender);
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            Text.send(sender, "<gray>Builds: <white><list></white>. Use /eventmain build <name>, then /eventmain build undo to remove it.",
                    Text.p("list", String.join(", ", Buildings.NAMES)));
            return true;
        }
        String type = args[1].toLowerCase(Locale.ROOT);
        if (type.equals("undo")) {
            if (!plugin.builds().undo(p)) Text.send(sender, "<red>Nothing to undo (or a build is still being placed).");
            return true;
        }
        if (!Buildings.NAMES.contains(type)) return usage(sender, "/eventmain build <" + String.join("|", Buildings.NAMES) + ">");
        if (plugin.builds().busy()) {
            Text.send(sender, "<red>Another build is still being placed.");
            return true;
        }
        String pointName = args.length >= 3 ? args[2] : "point" + (plugin.locations().points().size() + 1);
        int size = plugin.getConfig().getInt("capturepoints.zone-size", 15);
        int holo = plugin.getConfig().getInt("capturepoints.hologram-height", 5);
        Builder b = new Builder(p.getLocation(), p.getFacing());
        Location centre = Buildings.build(type, b, size, holo, pointName);
        plugin.builds().paste(p, b, type, () -> {
            if (centre == null) return;
            switch (type) {
                case "gladiators" -> {
                    plugin.locations().setMap("gladiators", centre);
                    Text.send(p, "<gray>Gladiators map set to the arena centre.");
                }
                case "capturepoint" -> {
                    plugin.locations().setPoint(pointName, centre);
                    Text.send(p, "<gray>Capture point <white><name></white> registered.", Text.p("name", pointName));
                }
                default -> {
                }
            }
        });
        return true;
    }

    private boolean needPlayer(CommandSender sender) {
        Text.send(sender, "<red>Run this in-game.");
        return true;
    }

    private boolean usage(CommandSender sender, String usage) {
        Text.send(sender, "<red>Usage: " + usage.replace("<", "\\<"));
        return true;
    }

    private void help(CommandSender sender) {
        for (String line : List.of(
                "/eventmain <event> setmap | start | stop",
                "/eventmain gladiators kit <set|edit|remove|give|list> [name]",
                "/eventmain point <set|remove|list> [name]",
                "/eventmain build <" + String.join("|", Buildings.NAMES) + "> [name] | undo | list",
                "Events: " + String.join(", ", plugin.games().all().stream().map(MiniGame::id).toList()))) {
            Text.send(sender, "<gray>" + line.replace("<", "\\<"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> games = plugin.games().all().stream().map(MiniGame::id).toList();
        if (args.length == 1) {
            List<String> opts = new ArrayList<>(games);
            opts.add("point");
            opts.add("build");
            return filter(opts, args[0]);
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (first) {
                case "point" -> filter(List.of("set", "remove", "list"), args[1]);
                case "build" -> {
                    List<String> opts = new ArrayList<>(Buildings.NAMES);
                    opts.add("undo");
                    opts.add("list");
                    yield filter(opts, args[1]);
                }
                case "gladiators" -> filter(List.of("setmap", "start", "stop", "kit"), args[1]);
                default -> games.contains(first) ? filter(List.of("setmap", "start", "stop"), args[1]) : List.of();
            };
        }
        if (args.length == 3) {
            if (first.equals("gladiators") && args[1].equalsIgnoreCase("kit")) return filter(List.of("set", "edit", "remove", "give", "list"), args[2]);
            if (first.equals("point") && args[1].equalsIgnoreCase("remove")) return filter(plugin.locations().points().keySet(), args[2]);
        }
        if (args.length == 4 && first.equals("gladiators") && args[1].equalsIgnoreCase("kit")) return filter(plugin.kits().names(), args[3]);
        if (args.length == 5 && first.equals("gladiators") && args[2].equalsIgnoreCase("give")) {
            List<String> opts = new ArrayList<>(players());
            opts.add("all");
            return filter(opts, args[4]);
        }
        return List.of();
    }
}
