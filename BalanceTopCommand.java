package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.economy.Currency;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

/**
 * /baltop [currency] [page]
 *
 * Changes vs original:
 *  - Reads baltop.blacklist from config.yml and skips those UUIDs/names.
 *  - Uses Currency#formatCompact() for numbers ≥ 1000 (e.g. 4.3k, 1.2M).
 */
public class BalanceTopCommand implements CommandExecutor {

    private final CrazySMPPlugin p;

    public BalanceTopCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        Currency currency = args.length >= 1
            ? p.getCurrencyManager().resolveCurrency(args[0])
            : p.getCurrencyManager().getVaultCurrency();
        if (currency == null) {
            s.sendMessage(CrazySMPPlugin.c("&cUnknown currency."));
            return true;
        }

        int page    = args.length >= 2 ? Math.max(1, parseInt(args[1], 1)) : 1;
        int perPage = 10;

        // Load blacklisted UUIDs from config
        List<String> blacklistRaw = p.getConfig().getStringList("baltop.blacklist");

        var top    = p.getCurrencyManager().getTopBalances(currency.getId(), Integer.MAX_VALUE);
        int rank   = 0;
        int shown  = 0;
        int skip   = (page - 1) * perPage;

        s.sendMessage(CrazySMPPlugin.c("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        s.sendMessage(CrazySMPPlugin.c("  &6" + currency.getDisplayName()
            + " &eTop &8— &7Page " + page));
        s.sendMessage(CrazySMPPlugin.c("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        for (var entry : top) {
            UUID uuid = entry.getKey();
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);

            // Blacklist check — supports both UUID strings and player names
            if (isBlacklisted(uuid, name, blacklistRaw)) continue;

            rank++;
            if (rank <= skip) continue;
            if (shown >= perPage) break;

            // Use compact format for large numbers
            String formatted = currency.formatCompact(entry.getValue());
            s.sendMessage(CrazySMPPlugin.c(
                "  &e#" + rank + " &f" + name + " &7— &a" + formatted
            ));
            shown++;
        }

        if (shown == 0) s.sendMessage(CrazySMPPlugin.c("  &7No entries on this page."));
        s.sendMessage(CrazySMPPlugin.c("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return true;
    }

    private boolean isBlacklisted(UUID uuid, String name, List<String> list) {
        String uuidStr = uuid.toString();
        for (String entry : list) {
            if (entry.equalsIgnoreCase(name) || entry.equalsIgnoreCase(uuidStr)) return true;
        }
        return false;
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return def; }
    }
}
