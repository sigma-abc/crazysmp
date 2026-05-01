package dev.crazysmp.scoreboard;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.economy.Currency;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI Expansion for CrazySMP Essentials.
 *
 * ── Available placeholders ────────────────────────────────────────────────────
 *
 * %crazysmp_balance%                — Vault currency, formatted with symbol  e.g. $1,234.50
 * %crazysmp_balance_nosym%          — Vault currency, NO symbol               e.g. 1,234.50
 * %crazysmp_balance_raw%            — Vault currency, raw integer             e.g. 1234
 *
 * %crazysmp_balance_shards%         — Shards, formatted with symbol           e.g. 500✦
 * %crazysmp_balance_shards_nosym%   — Shards, NO symbol                       e.g. 500
 * %crazysmp_balance_shards_raw%     — Shards, raw integer                     e.g. 500
 *
 * %crazysmp_balance_<currency>%          — Any currency, formatted
 * %crazysmp_balance_<currency>_nosym%    — Any currency, no symbol
 * %crazysmp_balance_<currency>_raw%      — Any currency, raw number
 *
 * %crazysmp_homes%                  — Number of homes set
 * %crazysmp_homes_max%              — Max homes allowed (∞ if unlimited)
 * %crazysmp_rank%                   — LuckPerms primary group name
 *
 * ── TAB plugin example (scoreboard) ──────────────────────────────────────────
 *
 * Use _nosym% when your TAB config already adds the currency symbol:
 *   title: "&a$%crazysmp_balance_nosym%"        ← symbol in title, not placeholder
 *
 * Use the default (with symbol) when TAB shows it plainly:
 *   title: "%crazysmp_balance%"                 ← outputs "$1,234.50"
 */
public class PAPIExpansion extends PlaceholderExpansion {

    private final CrazySMPPlugin plugin;

    public PAPIExpansion(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "crazysmp"; }
    @Override public @NotNull String getAuthor()     { return "sigma_abc"; }
    @Override public @NotNull String getVersion()    { return "2.0.0"; }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        java.util.UUID uuid = player.getUniqueId();

        // ── Shorthand: %crazysmp_balance% ─────────────────────────
        if (params.equals("balance")) {
            return formatBalance(uuid, getVaultCurrency(), true);
        }
        if (params.equals("balance_nosym")) {
            return formatBalance(uuid, getVaultCurrency(), false);
        }
        if (params.equals("balance_raw")) {
            Currency c = getVaultCurrency();
            if (c == null) return "0";
            return String.valueOf((long) plugin.getCurrencyManager().getBalance(uuid, c.getId()));
        }

        // ── %crazysmp_balance_<currency>%  /  _nosym  /  _raw ─────
        if (params.startsWith("balance_")) {
            String rest = params.substring("balance_".length()); // e.g. "shards", "shards_nosym", "shards_raw"

            if (rest.endsWith("_raw")) {
                String currId = rest.substring(0, rest.length() - "_raw".length());
                Currency c = plugin.getCurrencyManager().getCurrency(currId);
                if (c == null) return "0";
                return String.valueOf((long) plugin.getCurrencyManager().getBalance(uuid, c.getId()));
            }

            if (rest.endsWith("_nosym")) {
                String currId = rest.substring(0, rest.length() - "_nosym".length());
                Currency c = plugin.getCurrencyManager().getCurrency(currId);
                if (c == null) return "?";
                return formatBalance(uuid, c, false);
            }

            // Plain %crazysmp_balance_<currency>% — with symbol
            Currency c = plugin.getCurrencyManager().getCurrency(rest);
            if (c == null) return "?";
            return formatBalance(uuid, c, true);
        }

        // ── Homes ──────────────────────────────────────────────────
        if (params.equals("homes")) {
            return String.valueOf(plugin.getHomeManager().getHomes(uuid).size());
        }
        if (params.equals("homes_max")) {
            org.bukkit.entity.Player online = player.getPlayer();
            if (online == null) return "?";
            int max = plugin.getHomeManager().getMaxHomes(online);
            return max == Integer.MAX_VALUE ? "∞" : String.valueOf(max);
        }

        // ── LuckPerms rank ─────────────────────────────────────────
        if (params.equals("rank")) {
            if (plugin.getLuckPerms() == null) return "";
            try {
                var user = plugin.getLuckPerms().getUserManager().getUser(uuid);
                if (user == null) return "";
                String group = user.getPrimaryGroup();
                return group != null ? group : "";
            } catch (Exception e) {
                return "";
            }
        }

        return null; // unknown placeholder
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Currency getVaultCurrency() {
        return plugin.getCurrencyManager().getVaultCurrency();
    }

    /**
     * Format a balance with or without the currency symbol.
     *
     * @param withSymbol true  → "$1,234.50" or "500✦"
     *                   false → "1,234.50" or "500"  (no symbol — useful when TAB adds it)
     */
    private String formatBalance(java.util.UUID uuid, Currency c, boolean withSymbol) {
        if (c == null) return "0";
        double bal = plugin.getCurrencyManager().getBalance(uuid, c.getId());
        if (withSymbol) return c.format(bal);
        // Strip symbol from formatted output
        String formatted = c.format(bal);
        return formatted.replace(c.getSymbol(), "").trim();
    }
}
