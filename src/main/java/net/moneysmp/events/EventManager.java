package net.moneysmp.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.moneysmp.MoneySMP;
import net.moneysmp.economy.EconomyManager;
import net.moneysmp.util.Bars;
import net.moneysmp.util.Text;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public final class EventManager implements Listener {

    private static final String ANON_TEAM = "msmp_anon";

    private final MoneySMP plugin;
    private final Map<EventType, Active> active = new ConcurrentHashMap<>();
    private final NamespacedKey vulnerableKey;
    private final NamespacedKey flimsyKey;
    private final AtomicReference<Integer> mathAnswer = new AtomicReference<>();
    private final BukkitTask ticker;
    private int mathCountdown;

    private static final class Active {
        final int total;
        int left;
        final BossBar bar;

        Active(int total, BossBar bar) {
            this.total = total;
            this.left = total;
            this.bar = bar;
        }
    }

    public EventManager(MoneySMP plugin) {
        this.plugin = plugin;
        this.vulnerableKey = new NamespacedKey(plugin, "vulnerable");
        this.flimsyKey = new NamespacedKey(plugin, "flimsy_original");
        this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    // ------------------------------------------------------------------ API

    public boolean isActive(EventType t) {
        return active.containsKey(t);
    }

    public Set<EventType> activeTypes() {
        return EnumSet.copyOf(active.isEmpty() ? EnumSet.noneOf(EventType.class) : active.keySet());
    }

    public int secondsLeft(EventType t) {
        Active a = active.get(t);
        return a == null ? 0 : a.left;
    }

    public int defaultDuration(EventType t) {
        return cfg(t).getInt("duration-seconds", 3600);
    }

    public boolean start(EventType t, int seconds) {
        if (isActive(t)) return false;
        BossBar bar = plugin.bars().create(barName(t, seconds), t.good() ? BossBar.Color.GREEN : BossBar.Color.RED);
        active.put(t, new Active(seconds, bar));
        onStart(t);

        String color = t.good() ? "<green>" : "<red>";
        Text.broadcast(color + "<bold><name></bold></" + (t.good() ? "green" : "red") + "> <gray>has started for <white><time></white>! <desc>",
                Text.p("name", t.display()), Text.p("time", Text.time(seconds)), Text.p("desc", t.description()));
        Title title = Title.title(Text.mm(color + "<bold>" + t.display()), Text.mm("<gray>" + t.description()),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700)));
        for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(title);
        Text.playAll(plugin.getConfig().getString(t.good() ? "events.sound-good" : "events.sound-bad"));
        return true;
    }

    public boolean stop(EventType t, boolean announce) {
        Active a = active.remove(t);
        if (a == null) return false;
        plugin.bars().remove(a.bar);
        onStop(t);
        if (announce) Text.broadcast("<gray><name> has ended.", Text.p("name", t.display()));
        return true;
    }

    public void stopAll(boolean announce) {
        for (EventType t : activeTypes()) stop(t, announce);
    }

    public void shutdown() {
        stopAll(false);
        ticker.cancel();
    }

    // ------------------------------------------------------------------ lifecycle

    private ConfigurationSection cfg(EventType t) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("events." + t.key());
        return s != null ? s : new MemoryConfiguration();
    }

    private Component barName(EventType t, int left) {
        return Text.mm((t.good() ? "<green>" : "<red>") + t.display() + " <white>" + Text.time(left));
    }

    private void tick() {
        for (Map.Entry<EventType, Active> e : new ArrayList<>(active.entrySet())) {
            EventType t = e.getKey();
            Active a = e.getValue();
            a.left--;
            if (a.left <= 0) {
                stop(t, true);
                continue;
            }
            a.bar.name(barName(t, a.left));
            a.bar.progress(Bars.progress(a.left, a.total));
            tickEffect(t);
        }
    }

    private void onStart(EventType t) {
        switch (t) {
            case OPPOSITEDAY -> flipTime();
            case ANONYMOUS -> {
                anonTeam();
                for (Player p : Bukkit.getOnlinePlayers()) anonTeam().addEntry(p.getName());
            }
            case QUICKMATH -> mathCountdown = 5;
            default -> tickEffect(t);
        }
    }

    private void onStop(EventType t) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        switch (t) {
            case SHIELDLESS -> online.forEach(p -> p.setCooldown(Material.SHIELD, 0));
            case EXHAUSTED -> online.forEach(p -> {
                p.removePotionEffect(PotionEffectType.SLOWNESS);
                p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            });
            case MINERSHAVEN -> online.forEach(p -> p.removePotionEffect(PotionEffectType.HASTE));
            case OPPOSITEDAY -> flipTime();
            case FLIMSYARMOR -> online.forEach(this::restoreArmor);
            case VULNERABLE -> online.forEach(this::removeVulnerable);
            case QUICKMATH -> mathAnswer.set(null);
            case ANONYMOUS -> {
                Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
                org.bukkit.scoreboard.Team team = sb.getTeam(ANON_TEAM);
                if (team != null) team.unregister();
            }
            case GLOWANDBEHOLD -> online.forEach(p -> p.setGlowing(false));
            default -> {
            }
        }
    }

    private void tickEffect(EventType t) {
        if (t == EventType.QUICKMATH) {
            if (--mathCountdown <= 0) {
                askMath();
                mathCountdown = Math.max(10, cfg(t).getInt("interval-seconds", 300));
            }
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) applyEffect(t, p);
    }

    private void applyEffect(EventType t, Player p) {
        switch (t) {
            case SHIELDLESS -> p.setCooldown(Material.SHIELD, 40);
            case EXHAUSTED -> {
                int slow = Math.max(1, cfg(t).getInt("slowness-level", 1));
                int fatigue = Math.max(1, cfg(t).getInt("mining-fatigue-level", 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, slow - 1, true, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, fatigue - 1, true, false, true));
            }
            case MINERSHAVEN -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 1, true, false, true));
            case FLIMSYARMOR -> capArmor(p);
            case VULNERABLE -> applyVulnerable(p);
            case GLOWANDBEHOLD -> p.setGlowing(true);
            case ANONYMOUS -> anonTeam().addEntry(p.getName());
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ opposite day

    private void flipTime() {
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) w.setTime((w.getTime() + 12000) % 24000);
        }
    }

    // ------------------------------------------------------------------ anonymous

    private org.bukkit.scoreboard.Team anonTeam() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = sb.getTeam(ANON_TEAM);
        if (team == null) {
            team = sb.registerNewTeam(ANON_TEAM);
            team.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
        }
        return team;
    }

    // ------------------------------------------------------------------ vulnerable

    private void applyVulnerable(Player p) {
        AttributeInstance inst = p.getAttribute(Attribute.MAX_HEALTH);
        if (inst == null) return;
        removeVulnerable(p);
        double target = cfg(EventType.VULNERABLE).getDouble("hearts", 7.5) * 2.0;
        double diff = target - inst.getValue();
        if (diff < 0) {
            inst.addModifier(new AttributeModifier(vulnerableKey, diff, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
        if (p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    private void removeVulnerable(Player p) {
        AttributeInstance inst = p.getAttribute(Attribute.MAX_HEALTH);
        if (inst == null) return;
        for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) {
            if (vulnerableKey.equals(m.getKey())) inst.removeModifier(m);
        }
    }

    // ------------------------------------------------------------------ flimsy armor

    private void capArmor(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] armor = inv.getArmorContents();
        boolean changed = false;
        for (ItemStack it : armor) if (capItem(it)) changed = true;
        if (changed) inv.setArmorContents(armor);
    }

    private boolean capItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        ConfigurationSection c = cfg(EventType.FLIMSYARMOR);
        Map<Enchantment, Integer> caps = Map.of(
                Enchantment.PROTECTION, c.getInt("max-protection", 3),
                Enchantment.UNBREAKING, c.getInt("max-unbreaking", 1));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        StringBuilder original = new StringBuilder(pdc.getOrDefault(flimsyKey, PersistentDataType.STRING, ""));
        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> e : caps.entrySet()) {
            int level = meta.getEnchantLevel(e.getKey());
            if (level > e.getValue()) {
                if (!original.isEmpty()) original.append(';');
                original.append(e.getKey().getKey()).append('=').append(level);
                meta.removeEnchant(e.getKey());
                if (e.getValue() > 0) meta.addEnchant(e.getKey(), e.getValue(), true);
                changed = true;
            }
        }
        if (changed) {
            pdc.set(flimsyKey, PersistentDataType.STRING, original.toString());
            item.setItemMeta(meta);
        }
        return changed;
    }

    private void restoreArmor(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (restoreItem(contents[i])) inv.setItem(i, contents[i]);
        }
    }

    private boolean restoreItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String data = meta.getPersistentDataContainer().get(flimsyKey, PersistentDataType.STRING);
        if (data == null) return false;
        for (String part : data.split(";")) {
            String[] kv = part.split("=");
            if (kv.length != 2) continue;
            NamespacedKey key = NamespacedKey.fromString(kv[0]);
            Enchantment ench = key == null ? null : Registry.ENCHANTMENT.get(key);
            if (ench == null) continue;
            try {
                meta.removeEnchant(ench);
                meta.addEnchant(ench, Integer.parseInt(kv[1]), true);
            } catch (NumberFormatException ignored) {
            }
        }
        meta.getPersistentDataContainer().remove(flimsyKey);
        item.setItemMeta(meta);
        return true;
    }

    // ------------------------------------------------------------------ quick math

    private void askMath() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int a, b, answer;
        String op;
        switch (r.nextInt(3)) {
            case 0 -> { a = r.nextInt(10, 100); b = r.nextInt(10, 100); answer = a + b; op = "+"; }
            case 1 -> { a = r.nextInt(20, 100); b = r.nextInt(1, a); answer = a - b; op = "-"; }
            default -> { a = r.nextInt(2, 13); b = r.nextInt(2, 13); answer = a * b; op = "×"; }
        }
        Integer previous = mathAnswer.getAndSet(answer);
        if (previous != null) Text.broadcast("<gray>Nobody solved the last question. The answer was <white><a></white>.", Text.p("a", previous));
        long reward = cfg(EventType.QUICKMATH).getLong("reward", 25) * plugin.economy().multiplier();
        Text.broadcast("<yellow><bold>Quick Math!</bold></yellow> <gray>First to type the answer to <white><q></white> wins <gold><money></gold>!",
                Text.p("q", a + " " + op + " " + b), Text.p("money", EconomyManager.format(reward)));
        Text.playAll(plugin.getConfig().getString("general.broadcast-sound"));
    }

    // ------------------------------------------------------------------ listeners

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent e) {
        if (isActive(EventType.QUICKMATH)) {
            Integer ans = mathAnswer.get();
            String plain = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
            if (ans != null && plain.equals(String.valueOf(ans)) && mathAnswer.compareAndSet(ans, null)) {
                Player p = e.getPlayer();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String who = isActive(EventType.ANONYMOUS) ? "???" : p.getName();
                    Text.broadcast("<yellow><name></yellow> <gray>answered correctly: <white><a></white>", Text.p("name", who), Text.p("a", ans));
                    plugin.economy().reward(p.getUniqueId(), cfg(EventType.QUICKMATH).getLong("reward", 25), "Quick Math");
                });
            }
        }
        if (isActive(EventType.ANONYMOUS)) {
            e.renderer((source, displayName, message, viewer) ->
                    Component.text("??? ", NamedTextColor.DARK_GRAY)
                            .append(Component.text("» ", NamedTextColor.GRAY))
                            .append(message));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (isActive(EventType.ANONYMOUS)) e.joinMessage(null);
        if (!isActive(EventType.FLIMSYARMOR)) restoreArmor(p);
        if (!isActive(EventType.VULNERABLE)) removeVulnerable(p);
        if (!isActive(EventType.GLOWANDBEHOLD)) p.setGlowing(false);
        for (EventType t : activeTypes()) applyEffect(t, p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (isActive(EventType.ANONYMOUS)) e.quitMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        if (isActive(EventType.ANONYMOUS)) e.deathMessage(Component.text("Someone died.", NamedTextColor.GRAY));
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player) || !isActive(EventType.ULTRAHARDCORE)) return;
        EntityRegainHealthEvent.RegainReason r = e.getRegainReason();
        if (r == EntityRegainHealthEvent.RegainReason.SATIATED || r == EntityRegainHealthEvent.RegainReason.REGEN) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (isActive(EventType.FORBIDDENFRUIT) && Tag.LEAVES.isTagged(e.getBlock().getType())) {
            maybeDropApple(e.getBlock().getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (isActive(EventType.FORBIDDENFRUIT)) maybeDropApple(e.getBlock().getLocation());
    }

    private void maybeDropApple(Location loc) {
        double chance = cfg(EventType.FORBIDDENFRUIT).getDouble("golden-apple-chance", 0.015);
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            loc.getWorld().dropItemNaturally(loc.add(0.5, 0.5, 0.5), new ItemStack(Material.GOLDEN_APPLE));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent e) {
        if (!isActive(EventType.MINERSHAVEN)) return;
        Material m = e.getBlockState().getType();
        if (!m.name().endsWith("_ORE") && m != Material.ANCIENT_DEBRIS) return;
        for (Item item : e.getItems()) {
            ItemStack s = item.getItemStack();
            s.setAmount(Math.min(s.getMaxStackSize(), s.getAmount() * 2));
            item.setItemStack(s);
        }
    }
}
