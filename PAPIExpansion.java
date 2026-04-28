package dev.crazysmp.scoreboard;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.economy.Currency;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Placeholders available for TAB plugin / scoreboard:
 *
 * %crazysmp_balance%             — Vault currency formatted balance
 * %crazysmp_balance_raw%         — Vault currency raw number
 * %crazysmp_balance_<currency>%  — Specific currency formatted
 * %crazysmp_balance_<currency>_raw% — Specific currency raw
 * %crazysmp_homes%               — Number of homes
 * %crazysmp_homes_max%           — Max homes (needs online player)
 * %crazysmp_rank%                — LuckPerms prefix (stripped of colour codes)
 */
public class PAPIExpansion extends PlaceholderExpansion {

    private final CrazySMPPlugin plugin;

    public PAPIExpansion(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "crazysmp"; }
    @Override public @NotNull String getAuthor()     { return "sigma_abc"; }
    @Override public @NotNull String getVersion()    { return "1.0.0"; }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        java.util.UUID uuid = player.getUniqueId();

        // %crazysmp_balance%
        if (params.equals("balance")) {
            Currency c = plugin.getCurrencyManager().getVaultCurrency();
            if (c == null) return "0";
            double bal = plugin.getCurrencyManager().getBalance(uuid, c.getId());
            return c.format(bal);
        }

        // %crazysmp_balance_raw%
        if (params.equals("balance_raw")) {
            Currency c = plugin.getCurrencyManager().getVaultCurrency();
            if (c == null) return "0";
            return String.valueOf((long) plugin.getCurrencyManager().getBalance(uuid, c.getId()));
        }

        // %crazysmp_balance_<currency>%
        if (params.startsWith("balance_") && !params.endsWith("_raw")) {
            String currId = params.substring("balance_".length());
            Currency c = plugin.getCurrencyManager().getCurrency(currId);
            if (c == null) return "?";
            double bal = plugin.getCurrencyManager().getBalance(uuid, c.getId());
            return c.format(bal);
        }

        // %crazysmp_balance_<currency>_raw%
        if (params.startsWith("balance_") && params.endsWith("_raw")) {
            String currId = params.substring("balance_".length(), params.length() - "_raw".length());
            Currency c = plugin.getCurrencyManager().getCurrency(currId);
            if (c == null) return "0";
            return String.valueOf((long) plugin.getCurrencyManager().getBalance(uuid, c.getId()));
        }

        // %crazysmp_homes%
        if (params.equals("homes")) {
            return String.valueOf(plugin.getHomeManager().getHomes(uuid).size());
        }

        // %crazysmp_homes_max%
        if (params.equals("homes_max")) {
            org.bukkit.entity.Player online = player.getPlayer();
            if (online == null) return "?";
            int max = plugin.getHomeManager().getMaxHomes(online);
            return max == Integer.MAX_VALUE ? "∞" : String.valueOf(max);
        }

        // %crazysmp_rank%
        if (params.equals("rank")) {
            if (plugin.getLuckPerms() == null) return "";
            try {
                var user = plugin.getLuckPerms().getUserManager().getUser(uuid);
                if (user == null) return "";
                var group = user.getPrimaryGroup();
                return group != null ? group : "";
            } catch (Exception e) { return ""; }
        }

        return null;
    }
}