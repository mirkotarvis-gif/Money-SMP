package net.moneysmp.commands;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;

public abstract class BaseCommand implements TabExecutor {

    protected final MoneySMP plugin;

    protected BaseCommand(MoneySMP plugin) {
        this.plugin = plugin;
    }

    protected boolean admin(CommandSender s) {
        if (s.hasPermission("moneysmp.admin")) return true;
        Text.send(s, "<red>You don't have permission to do that.");
        return false;
    }

    protected static OfflinePlayer offline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    protected static Long amount(String s) {
        try {
            long v = Long.parseLong(s.replace(",", "").replace("$", ""));
            return v < 0 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static Integer integer(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static String nameOf(OfflinePlayer p) {
        return p.getName() != null ? p.getName() : p.getUniqueId().toString().substring(0, 8);
    }

    protected static List<String> filter(Collection<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).sorted().toList();
    }

    protected static List<String> players() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    protected List<String> teamNames() {
        return plugin.teams().all().stream().map(t -> t.name()).toList();
    }
}
