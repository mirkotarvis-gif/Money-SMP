package net.moneysmp.games;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Titans. /game titans titan <player> turns a player into a titan (scale, health, strength, reach,
 * step height from config); /game titans untitan <player> reverts.
 * TODO gameplay: titan abilities (pickaxe, leap, sonic boom, echo) on their cooldowns,
 * sword-damage bonus vs titans, and scoring (addScore) for damage/kills.
 */
public final class Titans extends MiniGame {

    public record Ability(int cooldownTicks, ConfigurationSection raw) {}

    int countdown, duration, strengthLevel;
    double swordDamage, scale, hearts, reach, stepHeight;
    final Map<String, Ability> abilities = new LinkedHashMap<>();
    private final Set<UUID> titans = new HashSet<>();
    private final NamespacedKey key;

    public Titans(MoneySMP plugin) {
        super(plugin, "titans", "Titans");
        key = new NamespacedKey(plugin, "titan");
    }

    @Override
    protected void load(ConfigurationSection s) {
        countdown = s.getInt("countdown-seconds", 30);
        duration = s.getInt("duration-seconds", 1800);
        swordDamage = s.getDouble("sword-damage", 3.0);
        scale = s.getDouble("titan.scale", 5.0);
        hearts = s.getDouble("titan.hearts", 70);
        strengthLevel = s.getInt("titan.strength-level", 3);
        reach = s.getDouble("titan.reach", 10);
        stepHeight = s.getDouble("titan.step-height", 2.0);
        abilities.clear();
        for (String a : List.of("pickaxe", "leap", "sonic", "echo")) {
            ConfigurationSection as = s.getConfigurationSection(a);
            if (as != null) abilities.put(a, new Ability(as.getInt("cooldown-ticks", 20), as));
        }
    }

    @Override protected int countdownSeconds() { return countdown; }
    @Override protected int durationSeconds() { return duration; }

    @Override
    protected void onBegin() {
        if (titans.isEmpty()) Text.broadcast("<gray>No titans chosen yet. Use <white>/game titans titan <player></white>.");
    }

    @Override
    protected void onEnd() {
        for (UUID u : new ArrayList<>(titans)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) revert(p);
        }
        titans.clear();
    }

    @Override
    public List<String> extraSubcommands() {
        return List.of("titan", "untitan");
    }

    @Override
    public boolean handle(CommandSender sender, String[] args) {
        if (args.length < 3 || !(args[1].equalsIgnoreCase("titan") || args[1].equalsIgnoreCase("untitan"))) return false;
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Text.send(sender, "<red>Player not online.");
            return true;
        }
        if (args[1].equalsIgnoreCase("titan")) {
            makeTitan(target);
            Text.broadcast("<dark_red><bold><name> has become a TITAN!", Text.p("name", target.getName()));
        } else {
            revert(target);
            Text.send(sender, "<green>Reverted <name>.", Text.p("name", target.getName()));
        }
        return true;
    }

    public void makeTitan(Player p) {
        revert(p);
        set(p, Attribute.SCALE, scale);
        set(p, Attribute.MAX_HEALTH, hearts * 2);
        set(p, Attribute.ENTITY_INTERACTION_RANGE, reach);
        set(p, Attribute.BLOCK_INTERACTION_RANGE, reach);
        set(p, Attribute.STEP_HEIGHT, stepHeight);
        AttributeInstance hp = p.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) p.setHealth(hp.getValue());
        if (strengthLevel > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, strengthLevel - 1, true, false, true));
        }
        titans.add(p.getUniqueId());
    }

    public void revert(Player p) {
        for (Attribute a : List.of(Attribute.SCALE, Attribute.MAX_HEALTH, Attribute.ENTITY_INTERACTION_RANGE,
                Attribute.BLOCK_INTERACTION_RANGE, Attribute.STEP_HEIGHT)) {
            AttributeInstance inst = p.getAttribute(a);
            if (inst == null) continue;
            for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) if (key.equals(m.getKey())) inst.removeModifier(m);
        }
        if (titans.remove(p.getUniqueId())) p.removePotionEffect(PotionEffectType.STRENGTH);
        AttributeInstance hp = p.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null && p.getHealth() > hp.getValue()) p.setHealth(hp.getValue());
    }

    public boolean isTitan(UUID id) {
        return titans.contains(id);
    }

    /** Adds a modifier so the attribute ends up at exactly `target`. */
    private void set(Player p, Attribute attribute, double target) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        double diff = target - inst.getValue();
        if (diff != 0) inst.addModifier(new AttributeModifier(key, diff, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }
}
