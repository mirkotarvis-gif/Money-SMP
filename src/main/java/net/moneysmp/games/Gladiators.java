package net.moneysmp.games;

import net.moneysmp.MoneySMP;
import net.moneysmp.tier.Tier;
import net.moneysmp.util.Text;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gladiators: teams fight through the configured stages.
 * TODO gameplay: build arenas arena-gap apart (pairs pair-spacing apart), prepare-seconds before each
 * round, roulette-ticks of spinning to pick fighters from the stage's tiers (empty = whole team),
 * each round win = points-per-win via addScore and `reward` money to the winners. Final placements
 * are announced award-gap-ticks apart and paid from `rewards`.
 */
public final class Gladiators extends MiniGame {

    public record Stage(String name, int players, int rounds, List<Tier> tiers) {}

    int arenaGap, pairSpacing, prepareSeconds, rouletteTicks, pointsPerWin, awardGapTicks;
    long reward;
    final List<Stage> stages = new ArrayList<>();

    public Gladiators(MoneySMP plugin) {
        super(plugin, "gladiators", "Gladiators");
    }

    @Override
    protected void load(ConfigurationSection s) {
        arenaGap = s.getInt("arena-gap", 100);
        pairSpacing = s.getInt("pair-spacing", 260);
        prepareSeconds = s.getInt("prepare-seconds", 30);
        rouletteTicks = s.getInt("roulette-ticks", 70);
        reward = s.getLong("reward", 100);
        pointsPerWin = s.getInt("points-per-win", 2);
        awardGapTicks = s.getInt("award-gap-ticks", 80);
        stages.clear();
        for (Map<?, ?> m : s.getMapList("stages")) {
            List<Tier> tiers = new ArrayList<>();
            if (m.get("tiers") instanceof List<?> l) for (Object o : l) {
                Tier t = Tier.parse(String.valueOf(o));
                if (t != null) tiers.add(t);
            }
            stages.add(new Stage(String.valueOf(m.get("name")), toInt(m.get("players"), 1), toInt(m.get("rounds"), 1), tiers));
        }
    }

    private static int toInt(Object o, int def) {
        return o instanceof Number n ? n.intValue() : def;
    }

    @Override
    protected void onBegin() {
        for (int i = 0; i < stages.size(); i++) {
            Stage st = stages.get(i);
            Text.broadcast("<gold>Stage <stage>:</gold> <white><name></white> <gray>(<size>v<size>, <rounds> round(s))",
                    Text.p("stage", i + 1), Text.p("name", st.name()), Text.p("size", st.players()), Text.p("rounds", st.rounds()));
        }
    }

    @Override
    protected void onEnd() {
    }
}
