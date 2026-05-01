package dev.crazysmp;

import dev.crazysmp.economy.CurrencyManager;
import dev.crazysmp.economy.VaultHook;
import dev.crazysmp.home.HomeManager;
import dev.crazysmp.hook.ShardShopHook;
import dev.crazysmp.listener.GUIListener;
import dev.crazysmp.listener.PlayerListener;
import dev.crazysmp.migration.MigrationManager;
import dev.crazysmp.rtp.RTPManager;
import dev.crazysmp.scoreboard.PAPIExpansion;
import dev.crazysmp.command.BalanceCommand;
import dev.crazysmp.command.PayCommand;
import dev.crazysmp.command.EcoCommand;
import dev.crazysmp.command.BalanceTopCommand;
import dev.crazysmp.command.HomeCommand;
import dev.crazysmp.command.SetHomeCommand;
import dev.crazysmp.command.DelHomeCommand;
import dev.crazysmp.command.HomesCommand;
import dev.crazysmp.command.RTPCommand;
import dev.crazysmp.command.MigrateCommand;
import dev.crazysmp.command.ReloadCommand;

import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CrazySMPPlugin extends JavaPlugin {

    private static CrazySMPPlugin instance;
    private CurrencyManager currencyManager;
    private HomeManager homeManager;
    private RTPManager rtpManager;
    private MigrationManager migrationManager;
    private VaultHook vaultHook;
    private LuckPerms luckPerms;

    // ── NEW: tracks players waiting to type a home name in chat ──────────────
    private final Set<UUID> awaitingHomeNameInput = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("  ");
        getLogger().info(" ╔═══════════════════════════════════════╗");
        getLogger().info(" ║    CrazySMP Essentials  v1.0.0        ║");
        getLogger().info(" ║    by sigma_abc                       ║");
        getLogger().info(" ╚═══════════════════════════════════════╝");
        getLogger().info("  ");

        // LuckPerms
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider =
                    getServer().getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPerms = provider.getProvider();
                getLogger().info("LuckPerms hooked.");
            }
        }

        // Managers
        currencyManager  = new CurrencyManager(this);
        homeManager      = new HomeManager(this);
        rtpManager       = new RTPManager(this);
        migrationManager = new MigrationManager(this);

        // Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook(this);
            vaultHook.register();
            getLogger().info("VaultUnlocked hooked — Dollars registered as primary economy.");
        } else {
            getLogger().warning("Vault not found — shop plugins won't work!");
        }

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            getLogger().warning("PlaceholderAPI not found — TAB placeholders won't work.");
        }

        // Commands
        registerCommand("balance",    new BalanceCommand(this));
        registerCommand("pay",        new PayCommand(this));
        registerCommand("eco",        new EcoCommand(this));
        registerCommand("balancetop", new BalanceTopCommand(this));
        registerCommand("home",       new HomeCommand(this));
        registerCommand("sethome",    new SetHomeCommand(this));
        registerCommand("delhome",    new DelHomeCommand(this));
        registerCommand("homes",      new HomesCommand(this));
        registerCommand("rtp",        new RTPCommand(this));
        registerCommand("migrate",    new MigrateCommand(this));
        registerCommand("crazysmp",   new ReloadCommand(this));

        // Listeners
        ShardShopHook.tryRegister(this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("CrazySMP Essentials online!");
    }

    @Override
    public void onDisable() {
        if (currencyManager != null) currencyManager.saveAll();
        if (homeManager != null) homeManager.saveAll();
        getLogger().info("CrazySMP Essentials disabled. Data saved.");
    }

    // ── NEW: called by ReloadCommand ─────────────────────────────────────────
    public void reload() {
        reloadConfig();
        currencyManager.reload();
        getLogger().info("CrazySMP Essentials config reloaded.");
    }

    // ── NEW: used by GUIListener to track chat-input mode ───────────────────
    public Set<UUID> getAwaitingHomeNameInput() {
        return awaitingHomeNameInput;
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor exec) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(exec);
            if (exec instanceof org.bukkit.command.TabCompleter tc) cmd.setTabCompleter(tc);
        }
    }

    public static CrazySMPPlugin getInstance()          { return instance; }
    public CurrencyManager getCurrencyManager()          { return currencyManager; }
    public HomeManager getHomeManager()                  { return homeManager; }
    public RTPManager getRTPManager()                    { return rtpManager; }
    public MigrationManager getMigrationManager()        { return migrationManager; }
    public LuckPerms getLuckPerms()                      { return luckPerms; }

    /** Translate & colour codes */
    public static String c(String s) { return s == null ? "" : s.replace("&", "§"); }

    public String msg(String key) {
        String prefix = c(getConfig().getString("messages.prefix", "&8[&6CrazySMP&8] &r"));
        String val    = getConfig().getString("messages." + key, "&c[missing message: " + key + "]");
        return prefix + c(val);
    }

    public String msgRaw(String key) {
        return c(getConfig().getString("messages." + key, ""));
    }
}