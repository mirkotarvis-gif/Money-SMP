package net.moneysmp.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;

/** A yml file in the plugin folder plus item (de)serialisation helpers. */
public final class YamlFile {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public YamlFile(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), name);
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration yaml() {
        return yaml;
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), e);
        }
    }

    public static List<String> encode(ItemStack[] items) {
        List<String> out = new ArrayList<>(items.length);
        for (ItemStack it : items) {
            out.add(it == null || it.getType().isAir() ? "" : Base64.getEncoder().encodeToString(it.serializeAsBytes()));
        }
        return out;
    }

    public static ItemStack[] decode(List<String> data, int size) {
        ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < Math.min(size, data.size()); i++) {
            String s = data.get(i);
            if (s == null || s.isEmpty()) continue;
            try {
                items[i] = ItemStack.deserializeBytes(Base64.getDecoder().decode(s));
            } catch (Exception ignored) {
            }
        }
        return items;
    }
}
