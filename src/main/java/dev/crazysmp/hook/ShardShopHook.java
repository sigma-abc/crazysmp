package dev.crazysmp.hook;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.economy.Currency;
import me.gypopo.economyshopgui.api.events.PreTransactionEvent;
import me.gypopo.economyshopgui.objects.ShopItem;
import me.gypopo.economyshopgui.util.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ShardShopHook implements Listener {

    private final CrazySMPPlugin plugin;
    private final String currencyId;

    public ShardShopHook(CrazySMPPlugin plugin, String currencyId) {
        this.plugin     = plugin;
        this.currencyId = currencyId;
    }

    public static void tryRegister(CrazySMPPlugin plugin) {
        if (!plugin.getConfig().getBoolean("shard-shop.economyshopgui.enabled", true)) return;

        Plugin esg = Bukkit.getPluginManager().getPlugin("EconomyShopGUI");
        if (esg == null || !esg.isEnabled()) {
            plugin.getLogger().info("EconomyShopGUI not found — shard shop hook skipped.");
            return;
        }

        String currencyId = plugin.getConfig().getString("shard-shop.currency", "shards");
        Currency shardCurrency = plugin.getCurrencyManager().getCurrency(currencyId);
        if (shardCurrency == null) {
            plugin.getLogger().warning("Shard shop currency '" + currencyId +
                "' not found in your currencies config! Shard shop disabled.");
            return;
        }

        Bukkit.getPluginManager().registerEvents(new ShardShopHook(plugin, currencyId), plugin);
        plugin.getLogger().info("Shard shop hook enabled! Currency: " + shardCurrency.getDisplayName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreTransaction(PreTransactionEvent event) {
        Transaction.Type type = event.getTransactionType();
        boolean isBuy = type == Transaction.Type.BUY_SCREEN
                || type == Transaction.Type.BUY_STACKS_SCREEN
                || type == Transaction.Type.QUICK_BUY
                || type == Transaction.Type.SHOPSTAND_BUY_SCREEN;
        if (!isBuy) return;

        Player player = event.getPlayer();
        String material = resolveMaterial(event);
        if (material == null) return;

        String configPath = "shard-shop.economyshopgui.items." + material;
        if (!plugin.getConfig().contains(configPath)) return;

        double costPer = plugin.getConfig().getDouble(configPath, 0);
        if (costPer <= 0) return;

        int amount = event.getAmount();
        double totalCost = costPer * amount;

        if (!plugin.getCurrencyManager().hasBalance(player.getUniqueId(), currencyId, totalCost)) {
            Currency c = plugin.getCurrencyManager().getCurrency(currencyId);
            String currName = c != null ? c.getDisplayName() : currencyId;
            String formatted = c != null ? c.format(totalCost) : String.valueOf((long) totalCost);

            player.sendMessage(CrazySMPPlugin.c(
                "&cYou need &e" + formatted + " &6" + currName +
                " &cto buy &e" + amount + "x " +
                material.toLowerCase().replace("_", " ") + "&c."
            ));
            event.setCancelled(true);
            return;
        }

        plugin.getCurrencyManager().withdraw(player.getUniqueId(), currencyId, totalCost);
        
        Currency c = plugin.getCurrencyManager().getCurrency(currencyId);
        String currName  = c != null ? c.getDisplayName() : currencyId;
        String formatted = c != null ? c.format(totalCost) : String.valueOf((long) totalCost);

        player.sendMessage(CrazySMPPlugin.c(
            "&7Charged &e" + formatted + " &6" + currName +
            " &7for " + amount + "x " +
            material.toLowerCase().replace("_", " ") + "."
        ));
    }

    private String resolveMaterial(PreTransactionEvent event) {
        try {
            ItemStack stack = event.getItemStack();
            if (stack != null && stack.getType() != org.bukkit.Material.AIR) {
                return stack.getType().name();
            }
        } catch (Exception ignored) {}

        ShopItem shopItem = event.getShopItem();
        if (shopItem != null) {
            try {
                ItemStack give = shopItem.getItemToGive();
                if (give != null && give.getType() != org.bukkit.Material.AIR) {
                    return give.getType().name();
                }
            } catch (Exception ignored) {}

            try {
                ItemStack display = shopItem.getShopItem();
                if (display != null && display.getType() != org.bukkit.Material.AIR) {
                    return display.getType().name();
                }
            } catch (Exception ignored) {}
        }

        return null;
    }
}