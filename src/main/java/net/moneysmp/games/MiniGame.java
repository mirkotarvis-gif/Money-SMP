package net.moneysmp.games;

import net.kyori.adventure.bossbar.BossBar;
import net.moneysmp.MoneySMP;
import net.moneysmp.economy.EconomyManager;
import net.moneysmp.team.Team;
import net.moneysmp.util.Bars;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

/**
 * Base for every minigame: config loading, countdown, timed duration, team scores and placement rewards.
 * Subclasses read their section in {@link #load} and put the actual gameplay in {@link #onBegin}/{@link #onEnd}.
 */
public abstract class MiniGame {

    public enum State { IDLE, COUNTDOWN, RUNNING }

    protected final MoneySMP plugin;
    private final String id;
    private final String display;
    protected State state = State.IDLE;
    protected List<Integer> rewards = List.of();
    protected final Map<String, Long> scores = new LinkedHashMap<>();
    private BukkitTask timer;
    private BossBar bar;

    protected MiniGame(MoneySMP plugin, String id, String display) {
        this.plugin = plugin;
        this.id = id;
        this.display = display;
    }

    public String id() { return id; }
    public String display() { return display; }
    public State state() { return state; }

    protected ConfigurationSection section() {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection(id);
        return s != null ? s : new MemoryConfiguration();
    }

    public final void reload() {
        ConfigurationSection s = section();
        rewards = s.getIntegerList("rewards");
        load(s);
    }

    /** Read this game's config section into fields. */
    protected abstract void load(ConfigurationSection s);

    /** Seconds of countdown before the game begins (0 = start right away). */
    protected int countdownSeconds() { return 0; }

    /** Game length in seconds (0 = runs until /game <id> end). */
    protected int durationSeconds() { return 0; }

    protected abstract void onBegin();

    protected abstract void onEnd();

    /** Extra subcommands for /game <id> ... Return true if handled. */
    public boolean handle(CommandSender sender, String[] args) { return false; }

    public List<String> extraSubcommands() { return List.of(); }

    // ------------------------------------------------------------------ lifecycle

    public String start() {
        if (state != State.IDLE) return display + " is already running.";
        scores.clear();
        int cd = countdownSeconds();
        if (cd > 0) {
            state = State.COUNTDOWN;
            Text.broadcast("<yellow><bold><name></bold> starts in <white><time></white>!", Text.p("name", display), Text.p("time", Text.time(cd)));
            runTimer("<yellow>" + display + " starts in ", cd, BossBar.Color.YELLOW, true, this::begin);
        } else {
            begin();
        }
        return null;
    }

    private void begin() {
        state = State.RUNNING;
        Text.broadcast("<green><bold><name> has begun!", Text.p("name", display));
        Text.playAll(plugin.getConfig().getString("general.broadcast-sound"));
        safely(this::onBegin);
        int d = durationSeconds();
        if (d > 0) runTimer("<aqua>" + display + " <white>", d, BossBar.Color.BLUE, false, this::finish);
    }

    /** Ends normally: runs onEnd, ranks teams by score and pays placement rewards. */
    public void finish() {
        if (state == State.COUNTDOWN) {
            stop();
            return;
        }
        if (state != State.RUNNING) return;
        cancelTimer();
        state = State.IDLE;
        safely(this::onEnd);
        Text.broadcast("<gold><bold><name> is over!", Text.p("name", display));
        awardPlacements();
    }

    /** Cancels without rewards. */
    public void stop() {
        if (state == State.IDLE) return;
        boolean wasRunning = state == State.RUNNING;
        cancelTimer();
        state = State.IDLE;
        if (wasRunning) safely(this::onEnd);
        Text.broadcast("<red><name> was cancelled.", Text.p("name", display));
    }

    private void runTimer(String label, int seconds, BossBar.Color color, boolean beep, Runnable done) {
        cancelTimer();
        int[] left = {seconds};
        bar = plugin.bars().create(Text.mm(label + Text.time(seconds)), color);
        timer = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            left[0]--;
            if (left[0] <= 0) {
                cancelTimer();
                done.run();
                return;
            }
            bar.name(Text.mm(label + Text.time(left[0])));
            bar.progress(Bars.progress(left[0], seconds));
            if (beep && left[0] <= 5) Text.playAll(plugin.getConfig().getString("general.broadcast-sound"));
        }, 20L, 20L);
    }

    private void cancelTimer() {
        if (timer != null) timer.cancel();
        timer = null;
        plugin.bars().remove(bar);
        bar = null;
    }

    private void safely(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in " + display, e);
        }
    }

    // ------------------------------------------------------------------ scoring

    public void addScore(Team team, long points) {
        scores.merge(team.name().toLowerCase(Locale.ROOT), points, Long::sum);
    }

    public long score(Team team) {
        return scores.getOrDefault(team.name().toLowerCase(Locale.ROOT), 0L);
    }

    /** Teams with a score, best first. */
    public List<Team> ranking() {
        List<Team> list = new ArrayList<>();
        for (Team t : plugin.teams().all()) if (scores.containsKey(t.name().toLowerCase(Locale.ROOT))) list.add(t);
        list.sort(Comparator.comparingLong((Team t) -> score(t)).reversed());
        return list;
    }

    protected void awardPlacements() {
        List<Team> ranking = ranking();
        if (ranking.isEmpty()) {
            Text.broadcast("<gray>No team scored any points.");
            return;
        }
        for (int i = 0; i < ranking.size(); i++) {
            Team t = ranking.get(i);
            long amount = i < rewards.size() ? rewards.get(i) : 0;
            Text.broadcast("<gold>#<place></gold> <team> <gray>- <points> pts</gray> <green><money></green> <gray>each</gray>",
                    Text.p("place", i + 1), Text.c("team", t.display()), Text.p("points", score(t)),
                    Text.p("money", EconomyManager.format(amount * plugin.economy().multiplier())));
            rewardTeam(t, amount, display + " #" + (i + 1));
        }
    }

    /** Pays every member and leader of a team. */
    protected void rewardTeam(Team team, long amount, String reason) {
        for (UUID u : team.everyone()) plugin.economy().reward(u, amount, reason);
    }
}
