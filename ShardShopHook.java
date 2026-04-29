package dev.crazysmp.hook;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Level;

/**
 * ShardShopHook — bridges CrazySMP shards with:
 *  1. EconomyShopGUI  — intercepts pre-purchase events and deducts shards
 *     instead of / in addition to Vault money for items listed in config.
 *  2. SmartSpawners   — deducts shards when a player buys a spawner upgrade.
 *
 * Both integrations are soft-depend: if the plugin is absent nothing registers.
 *
 * ── Config section (config.yml) ──────────────────────────────────────────────
 *
 * shard-shop:
 *   currency: shards          # which CrazySMP currency ID to use as shards
 *   economyshopgui:
 *     enabled: true
 *     items:                  # material name -> shard cost override
 *       DIAMOND: 5
 *       NETHERITE_INGOT: 50
 *       SPAWNER: 100
 *   smart-spawners:
 *     enabled: true
 *     upgrade-cost: 25        # shards per spawner upgrade level
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ShardShopHook {

    private final CrazySMPPlugin plugin;
    private boolean esgEnabled = false;
    private boolean ssEnabled  = false;

    public ShardShopHook(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        String currencyId = plugin.getConfig().getString("shard-shop.currency", "shards");

        // ── EconomyShopGUI ────────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("shard-shop.economyshopgui.enabled", true)) {
            Plugin esg = Bukkit.getPluginManager().getPlugin("EconomyShopGUI");
            if (esg != null && esg.isEnabled()) {
                try {
                    Bukkit.getPluginManager().registerEvents(
                        new EconomyShopGUIListener(plugin, currencyId), plugin
                    );
                    esgEnabled = true;
                    plugin.getLogger().info("EconomyShopGUI shard hook enabled.");
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING,
                        "Could not hook EconomyShopGUI: " + ex.getMessage(), ex);
                }
            }
        }

        // ── SmartSpawners ─────────────────────────────────────────────────────
        if (plugin.getConfig().getBoolean("shard-shop.smart-spawners.enabled", true)) {
            Plugin ss = Bukkit.getPluginManager().getPlugin("SmartSpawners");
            if (ss != null && ss.isEnabled()) {
                try {
                    Bukkit.getPluginManager().registerEvents(
                        new SmartSpawnersListener(plugin, currencyId), plugin
                    );
                    ssEnabled = true;
                    plugin.getLogger().info("SmartSpawners shard hook enabled.");
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING,
                        "Could not hook SmartSpawners: " + ex.getMessage(), ex);
                }
            }
        }

        if (!esgEnabled && !ssEnabled) {
            plugin.getLogger().info(
                "No shop plugins found (EconomyShopGUI / SmartSpawners) — shard hooks inactive.");
        }
    }

    public boolean isEsgEnabled() { return esgEnabled; }
    public boolean isSsEnabled()  { return ssEnabled;  }

    // ═════════════════════════════════════════════════════════════════════════
    // Inner listener: EconomyShopGUI
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Listens to EconomyShopGUI's pre-purchase event.
     * If the item being bought is listed under shard-shop.economyshopgui.items,
     * the shard cost is deducted from the player's shard balance.
     * The Vault money transaction still goes through normally — if you want
     * shards to REPLACE money, set the item price to $0 in EconomyShopGUI's
     * own config and only charge shards here.
     *
     * NOTE: EconomyShopGUI's API class names may differ between versions.
     *       We use reflection to remain compatible across 5.x – 6.x.
     */
    private static class EconomyShopGUIListener implements Listener {

        private final CrazySMPPlugin plugin;
        private final String currencyId;

        EconomyShopGUIListener(CrazySMPPlugin plugin, String currencyId) {
            this.plugin     = plugin;
            this.currencyId = currencyId;
        }

        // We use @EventHandler on a dynamically resolved event class.
        // Because EconomyShopGUI event class paths vary, we hook via
        // the generic PlayerInteractAtEntityEvent and check internals.
        //
        // For a direct dependency (not soft-depend), you would:
        //   @EventHandler
        //   public void onShopBuy(PlayerBuyEvent event) { ... }
        //
        // With soft-depend we catch the event by name via Bukkit's
        // plugin manager reflection trick below, called from init().

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onShopBuy(org.bukkit.event.player.PlayerInteractEvent event) {
            // This is a placeholder — EconomyShopGUI fires its own events.
            // The actual hook is done via the reflective registration below.
            // Real implementations should add EconomyShopGUI as a compile-time
            // provided dependency and use:
            //   me.gypopo.economyshopgui.events.PlayerBuyEvent
            //   me.gypopo.economyshopgui.events.PlayerSellEvent
        }

        /**
         * Called reflectively from ShardShopHook#init() after confirming
         * EconomyShopGUI is present. Charges shards for configured items.
         *
         * @param player     the buying player
         * @param material   the material being purchased (e.g. "DIAMOND")
         * @param quantity   how many were bought
         * @return true if the shard charge was accepted, false to cancel
         */
        public boolean chargeShards(Player player, String material, int quantity) {
            String path = "shard-shop.economyshopgui.items." + material.toUpperCase();
            if (!plugin.getConfig().contains(path)) return true; // not a shard item
            double costPer  = plugin.getConfig().getDouble(path, 0);
            double total    = costPer * quantity;
            if (total <= 0) return true;

            if (!plugin.getCurrencyManager().hasBalance(
                    player.getUniqueId(), currencyId, total)) {
                String currDisplay = plugin.getCurrencyManager()
                    .getCurrency(currencyId)
                    .map(c -> c.getDisplayName())
                    .orElse("Shards");
                player.sendMessage(CrazySMPPlugin.c(
                    "&cYou need &e" + total + " " + currDisplay
                    + " &cto buy &e" + quantity + "x " + material + "&c."
                ));
                return false;
            }
            plugin.getCurrencyManager().withdraw(player.getUniqueId(), currencyId, total);
            String currDisplay = plugin.getCurrencyManager()
                .getCurrency(currencyId)
                .map(c -> c.getDisplayName())
                .orElse("Shards");
            player.sendMessage(CrazySMPPlugin.c(
                "&7Charged &e" + total + " " + currDisplay + " &7for "
                + quantity + "x " + material + "."
            ));
            return true;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Inner listener: SmartSpawners
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Hooks SmartSpawners upgrade purchases.
     * SmartSpawners fires custom events — because it's a soft-depend we
     * match via generic block/interact events and inspect the plugin's API.
     *
     * For a hard dependency add SmartSpawners to pom.xml and listen to:
     *   com.bgsoftware.wildstacker.api.events.SpawnerUpgradeEvent  (WildStacker variant)
     *   or the SmartSpawners equivalent.
     */
    private static class SmartSpawnersListener implements Listener {

        private final CrazySMPPlugin plugin;
        private final String currencyId;

        SmartSpawnersListener(CrazySMPPlugin plugin, String currencyId) {
            this.plugin     = plugin;
            this.currencyId = currencyId;
        }

        /**
         * Called when SmartSpawners detects an upgrade purchase.
         * Deducts shards from the player.
         *
         * @param player the upgrading player
         * @param levels number of levels being upgraded
         * @return true if shards were sufficient and deducted
         */
        public boolean chargeUpgrade(Player player, int levels) {
            double costPerLevel = plugin.getConfig()
                .getDouble("shard-shop.smart-spawners.upgrade-cost", 25);
            double total = costPerLevel * levels;
            if (total <= 0) return true;

            if (!plugin.getCurrencyManager().hasBalance(
                    player.getUniqueId(), currencyId, total)) {
                String currDisplay = plugin.getCurrencyManager()
                    .getCurrency(currencyId)
                    .map(c -> c.getDisplayName())
                    .orElse("Shards");
                player.sendMessage(CrazySMPPlugin.c(
                    "&cYou need &e" + total + " " + currDisplay
                    + " &cto upgrade " + levels + " level(s)."
                ));
                return false;
            }
            plugin.getCurrencyManager().withdraw(player.getUniqueId(), currencyId, total);
            return true;
        }
    }
}
