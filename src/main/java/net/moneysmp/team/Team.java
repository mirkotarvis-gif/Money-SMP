package net.moneysmp.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.moneysmp.tier.Tier;

import java.util.*;

public final class Team {

    private final String name;
    private TextColor color = NamedTextColor.WHITE;
    private String icon = "";
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> leaders = new LinkedHashSet<>();
    private long budget;
    private final Map<Tier, Integer> bought = new EnumMap<>(Tier.class);

    public Team(String name) {
        this.name = name;
    }

    public String name() { return name; }
    public TextColor color() { return color; }
    public void color(TextColor color) { this.color = color; }
    public String icon() { return icon; }
    public void icon(String icon) { this.icon = icon == null ? "" : icon; }
    public Set<UUID> members() { return members; }
    public Set<UUID> leaders() { return leaders; }
    public long budget() { return budget; }
    public void budget(long budget) { this.budget = budget; }

    public int bought(Tier t) { return bought.getOrDefault(t, 0); }
    public void addBought(Tier t) { bought.merge(t, 1, Integer::sum); }
    public void clearBought() { bought.clear(); }

    /** Members plus leaders. */
    public Set<UUID> everyone() {
        Set<UUID> all = new LinkedHashSet<>(leaders);
        all.addAll(members);
        return all;
    }

    public Component display() {
        return Component.text(name, color);
    }
}
