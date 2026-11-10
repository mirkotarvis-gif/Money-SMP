package net.moneysmp.roles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/** Admin / staff icons and the hidden assassin role for Disasters. */
public final class RoleManager {

    public enum Role { ADMIN, STAFF, ASSASSIN }

    private final MoneySMP plugin;
    private final Map<Role, Set<UUID>> roles = new EnumMap<>(Role.class);

    public RoleManager(MoneySMP plugin) {
        this.plugin = plugin;
        for (Role r : Role.values()) roles.put(r, new LinkedHashSet<>());
    }

    public boolean has(UUID id, Role r) { return roles.get(r).contains(id); }
    public boolean add(UUID id, Role r) { return roles.get(r).add(id); }
    public boolean remove(UUID id, Role r) { return roles.get(r).remove(id); }
    public Set<UUID> members(Role r) { return Collections.unmodifiableSet(roles.get(r)); }

    private String iconRaw(Role r) {
        return switch (r) {
            case ADMIN -> plugin.getConfig().getString("roles.admin-icon", "<red><bold>ADMIN</bold></red>");
            case STAFF -> plugin.getConfig().getString("roles.staff-icon", "<aqua><bold>STAFF</bold></aqua>");
            case ASSASSIN -> "";
        };
    }

    /** Visible role icons (admin, staff) followed by a space, or empty. */
    public Component prefix(UUID id) {
        Component out = Component.empty();
        for (Role r : List.of(Role.ADMIN, Role.STAFF)) {
            if (has(id, r)) out = out.append(Text.mm(iconRaw(r))).append(Component.space());
        }
        return out;
    }

    /** Approximate pixel width of {@link #prefix}. */
    public int prefixWidth(UUID id) {
        int w = 0;
        for (Role r : List.of(Role.ADMIN, Role.STAFF)) {
            if (!has(id, r)) continue;
            String raw = iconRaw(r);
            String plain = PlainTextComponentSerializer.plainText().serialize(Text.mm(raw));
            w += Text.pixelWidth(plain) + (raw.contains("bold") || raw.contains("<b>") ? plain.length() : 0) + 4;
        }
        return w;
    }

    public void load(ConfigurationSection root) {
        for (Role r : Role.values()) {
            Set<UUID> set = roles.get(r);
            set.clear();
            for (String s : root.getStringList("roles." + r.name().toLowerCase(Locale.ROOT))) {
                try {
                    set.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save(ConfigurationSection root) {
        for (Role r : Role.values()) {
            root.set("roles." + r.name().toLowerCase(Locale.ROOT), roles.get(r).stream().map(UUID::toString).toList());
        }
    }
}
