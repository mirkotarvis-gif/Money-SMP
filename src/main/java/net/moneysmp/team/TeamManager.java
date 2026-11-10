package net.moneysmp.team;

import net.kyori.adventure.text.format.NamedTextColor;
import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public final class TeamManager {

    private final MoneySMP plugin;
    private final Map<String, Team> teams = new LinkedHashMap<>();

    public TeamManager(MoneySMP plugin) {
        this.plugin = plugin;
    }

    public int leadersPerTeam() {
        return plugin.getConfig().getInt("teams.leaders-per-team", 1);
    }

    public Collection<Team> all() {
        return Collections.unmodifiableCollection(teams.values());
    }

    public Team get(String name) {
        return name == null ? null : teams.get(name.toLowerCase(Locale.ROOT));
    }

    public Team create(String name) {
        if (get(name) != null) return null;
        Team t = new Team(name);
        teams.put(name.toLowerCase(Locale.ROOT), t);
        return t;
    }

    public boolean delete(String name) {
        return teams.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    /** The team a player belongs to, as member or leader. */
    public Team of(UUID id) {
        for (Team t : teams.values()) if (t.members().contains(id) || t.leaders().contains(id)) return t;
        return null;
    }

    public boolean isLeader(UUID id) {
        for (Team t : teams.values()) if (t.leaders().contains(id)) return true;
        return false;
    }

    public void addMember(Team team, UUID id) {
        remove(id);
        team.members().add(id);
    }

    public void remove(UUID id) {
        for (Team t : teams.values()) {
            t.members().remove(id);
            t.leaders().remove(id);
        }
    }

    /** Makes the player a leader of the team (joining it if needed). False if the leader limit is reached. */
    public boolean makeLeader(Team team, UUID id) {
        if (team.leaders().contains(id)) return true;
        if (team.leaders().size() >= leadersPerTeam()) return false;
        remove(id);
        team.leaders().add(id);
        return true;
    }

    public void demote(Team team, UUID id) {
        if (team.leaders().remove(id)) team.members().add(id);
    }

    public void load(ConfigurationSection root) {
        teams.clear();
        ConfigurationSection s = root.getConfigurationSection("teams");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            ConfigurationSection ts = s.getConfigurationSection(key);
            if (ts == null) continue;
            Team t = new Team(ts.getString("name", key));
            t.color(Text.hex(ts.getString("color"), NamedTextColor.WHITE));
            t.icon(ts.getString("icon", ""));
            t.budget(ts.getLong("budget", 0));
            for (String u : ts.getStringList("members")) parse(u).ifPresent(t.members()::add);
            for (String u : ts.getStringList("leaders")) parse(u).ifPresent(t.leaders()::add);
            teams.put(t.name().toLowerCase(Locale.ROOT), t);
        }
    }

    public void save(ConfigurationSection root) {
        for (Team t : teams.values()) {
            String base = "teams." + t.name().toLowerCase(Locale.ROOT) + ".";
            root.set(base + "name", t.name());
            root.set(base + "color", t.color().asHexString());
            root.set(base + "icon", t.icon());
            root.set(base + "budget", t.budget());
            root.set(base + "members", t.members().stream().map(UUID::toString).toList());
            root.set(base + "leaders", t.leaders().stream().map(UUID::toString).toList());
        }
    }

    private static Optional<UUID> parse(String s) {
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
