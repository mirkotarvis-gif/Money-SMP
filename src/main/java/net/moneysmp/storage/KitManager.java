package net.moneysmp.storage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Gladiators kits. A kit is the 41 player inventory slots: 0-35 storage, 36 boots, 37 leggings,
 * 38 chestplate, 39 helmet, 40 offhand. The edit GUI shows the same layout; slots 41-53 are labels.
 */
public final class KitManager implements Listener {

    private static final int SIZE = 41;
    private final YamlFile file;
    private final Map<String, ItemStack[]> kits = new LinkedHashMap<>();

    public KitManager(MoneySMP plugin) {
        file = new YamlFile(plugin, "kits.yml");
        for (String key : file.yaml().getKeys(false)) kits.put(key, YamlFile.decode(file.yaml().getStringList(key), SIZE));
    }

    private void write() {
        for (String key : new ArrayList<>(file.yaml().getKeys(false))) file.yaml().set(key, null);
        kits.forEach((k, v) -> file.yaml().set(k, YamlFile.encode(v)));
        file.save();
    }

    public Set<String> names() { return Collections.unmodifiableSet(kits.keySet()); }
    public boolean exists(String name) { return kits.containsKey(key(name)); }

    private static String key(String name) { return name.toLowerCase(Locale.ROOT); }

    public void save(String name, Player from) {
        ItemStack[] contents = from.getInventory().getContents();
        ItemStack[] kit = new ItemStack[SIZE];
        for (int i = 0; i < SIZE && i < contents.length; i++) kit[i] = contents[i] == null ? null : contents[i].clone();
        kits.put(key(name), kit);
        write();
    }

    public boolean remove(String name) {
        boolean removed = kits.remove(key(name)) != null;
        if (removed) write();
        return removed;
    }

    public boolean give(String name, Player to) {
        ItemStack[] kit = kits.get(key(name));
        if (kit == null) return false;
        ItemStack[] copy = new ItemStack[to.getInventory().getSize()];
        for (int i = 0; i < SIZE && i < copy.length; i++) copy[i] = kit[i] == null ? null : kit[i].clone();
        to.getInventory().setContents(copy);
        return true;
    }

    // ------------------------------------------------------------------ GUI

    private static final class KitHolder implements InventoryHolder {
        final String name;
        Inventory inventory;

        KitHolder(String name) { this.name = name; }

        @Override
        public Inventory getInventory() { return inventory; }
    }

    public void openEditor(Player p, String name) {
        KitHolder holder = new KitHolder(key(name));
        Inventory inv = Bukkit.createInventory(holder, 54, Text.mm("<dark_aqua>Kit: <white>" + key(name)));
        holder.inventory = inv;
        ItemStack[] kit = kits.getOrDefault(key(name), new ItemStack[SIZE]);
        for (int i = 0; i < SIZE; i++) if (kit[i] != null) inv.setItem(i, kit[i].clone());
        String[] labels = {"↑ Boots / Legs / Chest / Helmet ↑", "↑ Offhand ↑"};
        for (int i = SIZE; i < 54; i++) inv.setItem(i, label(i == 41 ? labels[1] : i == 45 ? labels[0] : "Kit editor - close to save"));
        p.openInventory(inv);
    }

    private static ItemStack label(String text) {
        ItemStack it = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(text, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof KitHolder)) return;
        if (e.getClickedInventory() == e.getView().getTopInventory() && e.getSlot() >= SIZE) e.setCancelled(true);
        if (e.isShiftClick() && e.getClickedInventory() != e.getView().getTopInventory()) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof KitHolder holder)) return;
        ItemStack[] kit = new ItemStack[SIZE];
        for (int i = 0; i < SIZE; i++) {
            ItemStack it = e.getInventory().getItem(i);
            kit[i] = it == null ? null : it.clone();
        }
        kits.put(holder.name, kit);
        write();
        Text.send(e.getPlayer(), "<green>Kit <white><name></white> saved.", Text.p("name", holder.name));
    }
}
