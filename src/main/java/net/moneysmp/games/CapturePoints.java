package net.moneysmp.games;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Capture points. Runs until ended with /game capturepoints end.
 * TODO gameplay: spawn a zone-size x zone-size zone with a hologram hologram-height blocks up,
 * a team standing in it alone for capture-seconds captures it, then must hold it control-seconds
 * to earn `reward` per member (use rewardTeam) and addScore. floor-per-tick = blocks of the zone
 * floor recoloured per tick to show progress.
 */
public final class CapturePoints extends MiniGame {

    int zoneSize, controlSeconds, captureSeconds, hologramHeight, floorPerTick;
    long reward;

    public CapturePoints(MoneySMP plugin) {
        super(plugin, "capturepoints", "Capture Points");
    }

    @Override
    protected void load(ConfigurationSection s) {
        zoneSize = s.getInt("zone-size", 15);
        reward = s.getLong("reward", 50);
        controlSeconds = s.getInt("control-seconds", 180);
        captureSeconds = s.getInt("capture-seconds", 180);
        hologramHeight = s.getInt("hologram-height", 5);
        floorPerTick = s.getInt("floor-per-tick", 40);
    }

    @Override
    protected void onBegin() {
        Text.broadcast("<gray>Capture a <white><size>x<size></white> zone in <white><cap>s</white> and hold it <white><ctrl>s</white> for <gold>$<reward></gold>.",
                Text.p("size", zoneSize), Text.p("cap", captureSeconds), Text.p("ctrl", controlSeconds), Text.p("reward", reward));
    }

    @Override
    protected void onEnd() {
    }
}
