package net.moneysmp.storage;

import net.moneysmp.MoneySMP;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/** Event maps (/eventmain <event> setmap) and capture points (/eventmain point ...). Saved in locations.yml. */
public final class LocationStore {

    private final YamlFile file;
    private final Map<String, Location> maps = new LinkedHashMap<>();
    private final Map<String, Location> points = new LinkedHashMap<>();

    public LocationStore(MoneySMP plugin) {
        file = new YamlFile(plugin, "locations.yml");
        read("maps", maps);
        read("points", points);
    }

    private void read(String path, Map<String, Location> into) {
        ConfigurationSection s = file.yaml().getConfigurationSection(path);
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            Location l = s.getLocation(key);
            if (l != null) into.put(key, l);
        }
    }

    private void write() {
        file.yaml().set("maps", null);
        file.yaml().set("points", null);
        maps.forEach((k, v) -> file.yaml().set("maps." + k, v));
        points.forEach((k, v) -> file.yaml().set("points." + k, v));
        file.save();
    }

    public Location map(String event) { return maps.get(event.toLowerCase(Locale.ROOT)); }

    public void setMap(String event, Location l) {
        maps.put(event.toLowerCase(Locale.ROOT), l.clone());
        write();
    }

    public Map<String, Location> points() { return Collections.unmodifiableMap(points); }

    public void setPoint(String name, Location l) {
        points.put(name.toLowerCase(Locale.ROOT), l.clone());
        write();
    }

    public boolean removePoint(String name) {
        boolean removed = points.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) write();
        return removed;
    }
}
