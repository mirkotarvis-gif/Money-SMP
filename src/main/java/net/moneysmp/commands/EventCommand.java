package net.moneysmp.commands;

import net.moneysmp.MoneySMP;
import net.moneysmp.events.EventManager;
import net.moneysmp.events.EventType;
import net.moneysmp.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class EventCommand extends BaseCommand {

    public EventCommand(MoneySMP plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EventManager em = plugin.events();
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                for (EventType t : EventType.values()) {
                    String state = em.isActive(t) ? "<green>ACTIVE " + Text.time(em.secondsLeft(t)) : "<dark_gray>off";
                    sender.sendMessage(Text.mm((t.good() ? "<green>" : "<red>") + "● <white><id></white> <gray>- <desc></gray> " + state,
                            Text.p("id", t.key()), Text.p("desc", t.description())));
                }
            }
            case "start", "random" -> {
                if (!admin(sender)) return true;
                EventType t;
                int argIndex;
                if (sub.equals("random")) {
                    List<EventType> pool = new ArrayList<>(Arrays.asList(EventType.values()));
                    pool.removeAll(em.activeTypes());
                    if (pool.isEmpty()) { Text.send(sender, "<red>Every event is already active."); return true; }
                    t = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
                    argIndex = 1;
                } else {
                    if (args.length < 2) { Text.send(sender, "<red>Usage: /event start <event> [seconds]"); return true; }
                    t = EventType.byKey(args[1]);
                    if (t == null) { Text.send(sender, "<red>Unknown event. /event list"); return true; }
                    argIndex = 2;
                }
                int secs = em.defaultDuration(t);
                if (args.length > argIndex) {
                    Integer v = integer(args[argIndex]);
                    if (v == null || v <= 0) { Text.send(sender, "<red>Invalid seconds."); return true; }
                    secs = v;
                }
                if (sub.equals("random")) roulette(t, secs);
                else if (!em.start(t, secs)) Text.send(sender, "<red>That event is already active.");
            }
            case "stop" -> {
                if (!admin(sender)) return true;
                if (args.length < 2 || args[1].equalsIgnoreCase("all")) {
                    em.stopAll(true);
                    return true;
                }
                EventType t = EventType.byKey(args[1]);
                if (t == null || !em.stop(t, true)) Text.send(sender, "<red>That event isn't active.");
            }
            default -> Text.send(sender, "<gray>/event list | start <event> [seconds] | random [seconds] | stop <event|all>");
        }
        return true;
    }

    /** Spins through event names on everyone's screen, slowing down, then starts the chosen one. */
    private void roulette(EventType result, int seconds) {
        EventType[] all = EventType.values();
        int spins = 22;
        Runnable[] step = new Runnable[1];
        int[] i = {0};
        step[0] = () -> {
            boolean last = i[0] >= spins;
            EventType shown = last ? result : all[ThreadLocalRandom.current().nextInt(all.length)];
            net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
                    Text.mm((shown.good() ? "<green>" : "<red>") + "<bold>" + shown.display()),
                    Text.mm(last ? "<gold>★ Selected! ★" : "<gray>Spinning..."),
                    net.kyori.adventure.title.Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(last ? 2500 : 600), java.time.Duration.ofMillis(200)));
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) p.showTitle(title);
            Text.playAll(last ? "ENTITY_PLAYER_LEVELUP" : "UI_BUTTON_CLICK");
            if (last) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.events().start(result, seconds), 50L);
                return;
            }
            long delay = 2 + (long) (i[0] * i[0] / 30.0);
            i[0]++;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, step[0], delay);
        };
        Text.broadcast("<yellow><bold>Spinning the event roulette...");
        step[0].run();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return filter(List.of("list", "start", "random", "stop"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filter(Arrays.stream(EventType.values()).map(EventType::key).toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stop")) {
            List<String> opts = new ArrayList<>(plugin.events().activeTypes().stream().map(EventType::key).toList());
            opts.add("all");
            return filter(opts, args[1]);
        }
        return List.of();
    }
}
