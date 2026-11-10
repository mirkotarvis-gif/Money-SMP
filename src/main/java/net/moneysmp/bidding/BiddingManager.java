package net.moneysmp.bidding;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.moneysmp.MoneySMP;
import net.moneysmp.economy.EconomyManager;
import net.moneysmp.team.Team;
import net.moneysmp.tier.Tier;
import net.moneysmp.util.Bars;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

/**
 * Player draft auction.
 *  /bidding time   - starts a session: every team gets bidding.start-budget to spend
 *  /bidopen <tier> - puts every teamless player of that tier up, one at a time, open-seconds each
 *  /bidclose       - sells the current player to the highest bid and closes the tier
 *  /bidding stop   - cancels the whole session
 * Team leaders bid with /bid <amount>, inside the tier's range, max bidding.max-per-tier per tier.
 */
public final class BiddingManager {

    private final MoneySMP plugin;
    private final Deque<UUID> queue = new ArrayDeque<>();
    private final List<UUID> unsold = new ArrayList<>();
    private boolean session;
    private Tier openTier;
    private Lot lot;
    private BossBar bar;
    private BukkitTask ticker;
    private BukkitTask pending;

    public static final class Lot {
        public final UUID player;
        public final String name;
        public final Tier tier;
        public long bid;
        public Team team;
        public int left;
        final int total;

        Lot(UUID player, String name, Tier tier, int seconds) {
            this.player = player;
            this.name = name;
            this.tier = tier;
            this.left = seconds;
            this.total = seconds;
        }
    }

    public BiddingManager(MoneySMP plugin) {
        this.plugin = plugin;
    }

    public boolean session() { return session; }
    public boolean running() { return session; }
    public Tier openTier() { return openTier; }
    public Lot lot() { return lot; }
    public int queued() { return queue.size(); }

    private TagResolver colors() {
        TextColor fb = NamedTextColor.WHITE;
        var c = plugin.getConfig();
        return TagResolver.resolver(
                Placeholder.styling("alert", Text.hex(c.getString("bidding.colors.alert"), fb)),
                Placeholder.styling("headline", Text.hex(c.getString("bidding.colors.headline"), fb)),
                Placeholder.styling("label", Text.hex(c.getString("bidding.colors.label"), fb)),
                Placeholder.styling("tiername", Text.hex(c.getString("bidding.colors.tier-name"), fb)),
                Placeholder.styling("money", Text.hex(c.getString("bidding.colors.money"), fb)));
    }

    private void say(String mm, TagResolver... extra) {
        TagResolver[] all = Arrays.copyOf(extra, extra.length + 1);
        all[extra.length] = colors();
        Text.broadcast(mm, all);
    }

    // ------------------------------------------------------------------ session

    /** Returns an error message or null. */
    public String startSession() {
        if (session) return "A bidding session is already running.";
        if (plugin.teams().all().stream().allMatch(t -> t.leaders().isEmpty())) return "No team has a leader yet. Use /teamleader set.";
        long budget = plugin.getConfig().getLong("bidding.start-budget", 500);
        for (Team t : plugin.teams().all()) {
            t.budget(budget);
            t.clearBought();
        }
        session = true;
        unsold.clear();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        say("<headline><bold>Bidding time!</bold></headline> <label>Every team has</label> <money><budget></money><label>. Admins open a tier with /bidopen.</label>",
                Text.p("budget", EconomyManager.format(budget)));
        Title title = Title.title(Text.mm("<gradient:#8bebf0:#4588e3><bold>BIDDING TIME"), Text.mm("<gray>Captains, get ready"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700)));
        for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(title);
        Text.playAll(plugin.getConfig().getString("general.broadcast-sound"));
        return null;
    }

    public String stopSession() {
        if (!session) return "No bidding session is running.";
        session = false;
        openTier = null;
        queue.clear();
        cancelPending();
        if (ticker != null) ticker.cancel();
        ticker = null;
        plugin.bars().remove(bar);
        bar = null;
        lot = null;
        Text.broadcast("<red>The bidding session was cancelled.");
        return null;
    }

    // ------------------------------------------------------------------ tiers

    public String open(Tier tier) {
        if (!session) return "Start a session first with /bidding time.";
        if (openTier != null) return openTier + " tier is still open. Use /bidclose first.";
        List<UUID> pool = new ArrayList<>();
        for (Map.Entry<UUID, Tier> e : plugin.tiers().all().entrySet()) {
            if (e.getValue() == tier && plugin.teams().of(e.getKey()) == null) pool.add(e.getKey());
        }
        if (pool.isEmpty()) return "There are no teamless " + tier + " tier players.";
        pool.sort(Comparator.comparing(this::nameOf, String.CASE_INSENSITIVE_ORDER));
        queue.clear();
        queue.addAll(pool);
        openTier = tier;
        long min = plugin.tiers().minBid(tier), max = plugin.tiers().maxBid(tier);
        say("<alert><bold>Bidding is open for</bold></alert> <tiername><tier> Tier</tiername><label>:</label> <headline><count> players</headline> <label>Range</label> <money><min> - <max></money>",
                Text.p("tier", tier), Text.p("count", pool.size()),
                Text.p("min", EconomyManager.format(min)), Text.p("max", EconomyManager.format(max)));
        schedule(this::next, 40L);
        return null;
    }

    public String close() {
        if (openTier == null) return "No tier is open.";
        Tier t = openTier;
        queue.clear();
        cancelPending();
        if (lot != null) sell();
        openTier = null;
        say("<label>Bidding for</label> <tiername><tier> Tier</tiername> <label>is closed.</label>", Text.p("tier", t));
        return null;
    }

    private void next() {
        if (!session || openTier == null || lot != null) return;
        UUID id = queue.poll();
        if (id == null) {
            Tier t = openTier;
            openTier = null;
            say("<label>Every</label> <tiername><tier> Tier</tiername> <label>player has been auctioned.</label>", Text.p("tier", t));
            if (!unsold.isEmpty()) {
                say("<label>Unsold so far:</label> <white><list></white>", Text.p("list", String.join(", ", unsold.stream().map(this::nameOf).toList())));
            }
            return;
        }
        if (plugin.teams().of(id) != null) {
            next();
            return;
        }
        int seconds = plugin.getConfig().getInt("bidding.open-seconds", 30);
        lot = new Lot(id, nameOf(id), openTier, seconds);
        TextColor tc = plugin.tiers().color(openTier);
        Title title = Title.title(Component.text(openTier.name() + " Tier", tc), Component.text(lot.name, NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500)));
        for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(title);
        say("<alert>Now bidding:</alert> <headline><name></headline> <tiername>(<tier> Tier)</tiername> <gray>- /bid \\<amount></gray>",
                Text.p("name", lot.name), Text.p("tier", openTier));
        Text.playAll(plugin.getConfig().getString("general.broadcast-sound"));
        bar = plugin.bars().create(barName(), BossBar.Color.YELLOW);
    }

    private Component barName() {
        String high = lot.team == null ? "no bids" : EconomyManager.format(lot.bid) + " by " + lot.team.name();
        return Text.mm("<yellow>" + lot.name + " <gray>(" + lot.tier.name() + ")</gray> - <white>" + high + "</white> - " + lot.left + "s");
    }

    private void tick() {
        if (lot == null) return;
        lot.left--;
        if (lot.left <= 0) {
            sell();
            schedule(this::next, 60L);
            return;
        }
        bar.name(barName());
        bar.progress(Bars.progress(lot.left, lot.total));
    }

    private void sell() {
        Lot l = lot;
        lot = null;
        plugin.bars().remove(bar);
        bar = null;
        if (l.team != null) {
            plugin.teams().addMember(l.team, l.player);
            l.team.budget(l.team.budget() - l.bid);
            l.team.addBought(l.tier);
            say("<alert><bold>SOLD!</bold></alert> <headline><name></headline> <label>joins</label> <team> <label>for</label> <money><bid></money>",
                    Text.p("name", l.name), Text.c("team", l.team.display()), Text.p("bid", EconomyManager.format(l.bid)));
            Text.playAll("ENTITY_PLAYER_LEVELUP");
        } else {
            unsold.add(l.player);
            say("<gray><name> received no bids.", Text.p("name", l.name));
        }
    }

    public String bid(Player p, long amount) {
        if (lot == null) return "Nobody is up for bidding right now.";
        Team team = plugin.teams().of(p.getUniqueId());
        if (team == null || !team.leaders().contains(p.getUniqueId())) return "Only team leaders can bid.";
        long min = plugin.tiers().minBid(lot.tier), max = plugin.tiers().maxBid(lot.tier);
        int perTier = plugin.getConfig().getInt("bidding.max-per-tier", 1);
        if (team.bought(lot.tier) >= perTier) return "Your team already has " + perTier + " " + lot.tier + " tier player(s).";
        if (amount < min) return "The minimum bid for " + lot.tier + " tier is " + EconomyManager.format(min) + ".";
        if (amount > max) return "The maximum bid for " + lot.tier + " tier is " + EconomyManager.format(max) + ".";
        if (lot.team != null && amount <= lot.bid) return "You must bid more than " + EconomyManager.format(lot.bid) + ".";
        if (amount > team.budget()) return "Your team only has " + EconomyManager.format(team.budget()) + " left.";
        if (lot.team == team) return "Your team already has the highest bid.";
        lot.bid = amount;
        lot.team = team;
        say("<team> <label>bids</label> <money><bid></money> <label>on</label> <headline><name></headline>",
                Text.c("team", team.display()), Text.p("bid", EconomyManager.format(amount)), Text.p("name", lot.name));
        Text.playAll(plugin.getConfig().getString("general.broadcast-sound"));
        bar.name(barName());
        return null;
    }

    /** Used on shutdown. */
    public void stop() {
        if (session) stopSession();
    }

    private void schedule(Runnable r, long delay) {
        cancelPending();
        pending = Bukkit.getScheduler().runTaskLater(plugin, r, delay);
    }

    private void cancelPending() {
        if (pending != null) pending.cancel();
        pending = null;
    }

    private String nameOf(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : id.toString().substring(0, 8);
    }
}
