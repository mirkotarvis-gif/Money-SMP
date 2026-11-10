package net.moneysmp.games;

import net.moneysmp.MoneySMP;

import java.util.*;

public final class GameManager {

    private final Map<String, MiniGame> games = new LinkedHashMap<>();

    public GameManager(MoneySMP plugin) {
        for (MiniGame g : List.of(new CapturePoints(plugin), new Titans(plugin), new EggHunt(plugin),
                new AceHunt(plugin), new Disasters(plugin), new Gladiators(plugin))) {
            games.put(g.id(), g);
        }
        reload();
    }

    public void reload() {
        games.values().forEach(MiniGame::reload);
    }

    public MiniGame get(String id) {
        return id == null ? null : games.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<MiniGame> all() {
        return games.values();
    }

    public void stopAll() {
        for (MiniGame g : games.values()) {
            if (g.state() != MiniGame.State.IDLE) g.stop();
        }
    }
}
