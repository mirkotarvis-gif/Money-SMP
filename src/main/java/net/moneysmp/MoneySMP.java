package net.moneysmp;

import net.moneysmp.bidding.BiddingManager;
import net.moneysmp.commands.*;
import net.moneysmp.economy.EconomyManager;
import net.moneysmp.events.EventManager;
import net.moneysmp.games.GameManager;
import net.moneysmp.grace.GraceManager;
import net.moneysmp.build.BuildManager;
import net.moneysmp.listeners.ChatListener;
import net.moneysmp.listeners.PlayerListener;
import net.moneysmp.roles.RoleManager;
import net.moneysmp.storage.InventoryVault;
import net.moneysmp.storage.KitManager;
import net.moneysmp.storage.LocationStore;
import net.moneysmp.tab.TabManager;
import net.moneysmp.team.TeamManager;
import net.moneysmp.tier.TierManager;
import net.moneysmp.util.Bars;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class MoneySMP extends JavaPlugin {

    private File dataFile;
    private Bars bars;
    private EconomyManager economy;
    private TierManager tiers;
    private TeamManager teams;
    private GraceManager grace;
    private EventManager events;
    private BiddingManager bidding;
    private TabManager tab;
    private GameManager games;
    private RoleManager roles;
    private InventoryVault vault;
    private KitManager kits;
    private LocationStore locations;
    private BuildManager builds;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Text.load(getConfig());
        dataFile = new File(getDataFolder(), "data.yml");

        bars = new Bars();
        economy = new EconomyManager(this);
        tiers = new TierManager(this);
        teams = new TeamManager(this);
        grace = new GraceManager(this);
        events = new EventManager(this);
        bidding = new BiddingManager(this);
        tab = new TabManager(this);
        games = new GameManager(this);
        roles = new RoleManager(this);
        vault = new InventoryVault(this);
        kits = new KitManager(this);
        locations = new LocationStore(this);
        builds = new BuildManager(this);

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        economy.load(data);
        tiers.load(data);
        teams.load(data);
        roles.load(data);
        for (Player p : Bukkit.getOnlinePlayers()) economy.ensure(p.getUniqueId());

        var pm = getServer().getPluginManager();
        pm.registerEvents(bars, this);
        pm.registerEvents(events, this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(vault, this);
        pm.registerEvents(kits, this);

        EconomyCommands eco = new EconomyCommands(this);
        register("balance", eco);
        register("pay", eco);
        register("baltop", eco);
        register("eco", eco);
        register("team", new TeamCommand(this));
        register("tier", new TierCommand(this));
        BidCommand bid = new BidCommand(this);
        register("bid", bid);
        register("bidding", bid);
        register("bidopen", bid);
        register("bidclose", bid);
        SetupCommands setup = new SetupCommands(this);
        for (String c : new String[]{"assassin", "money", "teamleader", "teamreset", "admin", "staff", "broadcast", "moneystart"}) {
            register(c, setup);
        }
        register("eventmain", new EventMainCommand(this));
        register("grace", new GraceCommand(this));
        register("event", new EventCommand(this));
        register("game", new GameCommand(this));
        register("moneysmp", new AdminCommand(this));

        tab.start();
        Bukkit.getScheduler().runTaskTimer(this, this::saveAll, 6000L, 6000L);
        getLogger().info("MoneySMP enabled.");
    }

    @Override
    public void onDisable() {
        if (games != null) games.stopAll();
        if (bidding != null) bidding.stop();
        if (events != null) events.shutdown();
        if (grace != null) grace.stop(false);
        if (tab != null) tab.stop();
        if (bars != null) bars.removeAll();
        if (economy != null) saveAll();
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().warning("Command missing from plugin.yml: " + name);
            return;
        }
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }

    public void saveAll() {
        YamlConfiguration data = new YamlConfiguration();
        economy.save(data);
        tiers.save(data);
        teams.save(data);
        roles.save(data);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    public void reloadAll() {
        reloadConfig();
        Text.load(getConfig());
        economy.reloadSettings();
        games.reload();
        tab.start();
    }

    public Bars bars() { return bars; }
    public EconomyManager economy() { return economy; }
    public TierManager tiers() { return tiers; }
    public TeamManager teams() { return teams; }
    public GraceManager grace() { return grace; }
    public EventManager events() { return events; }
    public BiddingManager bidding() { return bidding; }
    public TabManager tab() { return tab; }
    public GameManager games() { return games; }
    public RoleManager roles() { return roles; }
    public InventoryVault vault() { return vault; }
    public KitManager kits() { return kits; }
    public LocationStore locations() { return locations; }
    public BuildManager builds() { return builds; }
}
