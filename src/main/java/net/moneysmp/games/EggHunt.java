package net.moneysmp.games;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Egg hunt.
 * TODO gameplay: place egg-count eggs (display entities, `scale`) at random spots within `radius`
 * of center-x/center-z in `world`, at least min-distance apart (up to place-attempts tries each).
 * Picking one up: give its items, addScore(team, type.points), sink it over sink-ticks and
 * respawn a new egg after respawn-seconds. Use pickType() for the weighted egg rarity.
 */
public final class EggHunt extends MiniGame {

    public record EggType(String id, String display, TextColor color, int weight, int points, List<Material> items) {}

    String world;
    int centerX, centerZ, radius, minDistance, placeAttempts, duration, eggCount, respawnSeconds, sinkTicks, boxPad, starWidth;
    double scale;
    final List<EggType> types = new ArrayList<>();

    public EggHunt(MoneySMP plugin) {
        super(plugin, "egghunt", "Egg Hunt");
    }

    @Override
    protected void load(ConfigurationSection s) {
        world = s.getString("world", "");
        centerX = s.getInt("center-x", 0);
        centerZ = s.getInt("center-z", 0);
        radius = s.getInt("radius", 400);
        minDistance = s.getInt("min-distance", 60);
        placeAttempts = s.getInt("place-attempts", 80);
        duration = s.getInt("duration-seconds", 3600);
        eggCount = s.getInt("egg-count", 30);
        respawnSeconds = s.getInt("respawn-seconds", 20);
        sinkTicks = s.getInt("sink-ticks", 24);
        scale = s.getDouble("scale", 1.0);
        boxPad = s.getInt("box-pad", 2);
        starWidth = s.getInt("star-width", 8);
        types.clear();
        ConfigurationSection ts = s.getConfigurationSection("types");
        if (ts != null) {
            for (String id : ts.getKeys(false)) {
                ConfigurationSection t = ts.getConfigurationSection(id);
                if (t == null) continue;
                List<Material> items = new ArrayList<>();
                for (String m : t.getStringList("items")) {
                    Material mat = Material.matchMaterial(m);
                    if (mat != null) items.add(mat);
                    else plugin.getLogger().warning("egghunt." + id + ": unknown item " + m);
                }
                types.add(new EggType(id, t.getString("display", id), Text.hex(t.getString("color"), NamedTextColor.WHITE),
                        t.getInt("weight", 1), t.getInt("points", 1), items));
            }
        }
    }

    @Override protected int durationSeconds() { return duration; }

    public EggType pickType() {
        int total = types.stream().mapToInt(EggType::weight).sum();
        if (total <= 0) return types.isEmpty() ? null : types.get(0);
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (EggType t : types) {
            roll -= t.weight();
            if (roll < 0) return t;
        }
        return types.get(types.size() - 1);
    }

    @Override
    protected void onBegin() {
        Text.broadcast("<gray><count> eggs are hidden within <white><radius></white> blocks of <white><x>, <z></white>. Find them!",
                Text.p("count", eggCount), Text.p("radius", radius), Text.p("x", centerX), Text.p("z", centerZ));
    }

    @Override
    protected void onEnd() {
    }
}
