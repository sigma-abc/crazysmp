package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.economy.Currency;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BalanceTopCommand implements CommandExecutor {
    private final CrazySMPPlugin p;

    public BalanceTopCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        Currency currency = args.length >= 1 ? p.getCurrencyManager().resolveCurrency(args[0]) : p.getCurrencyManager().getVaultCurrency();
        if (currency == null) {
            s.sendMessage(CrazySMPPlugin.c("&cUnknown currency."));
            return true;
        }
        int page = args.length >= 2 ? Math.max(1, parseInt(args[1], 1)) : 1;
        int perPage = 10;

        var top = p.getCurrencyManager().getTopBalances(currency.getId(), page * perPage);
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, top.size());

        s.sendMessage(CrazySMPPlugin.c("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        s.sendMessage(CrazySMPPlugin.c("  &6" + currency.getDisplayName() + " &eTop " + perPage + " &8— &7Page " + page));
        s.sendMessage(CrazySMPPlugin.c("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        for (int i = start; i < end; i++) {
            var entry = top.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            s.sendMessage(CrazySMPPlugin.c("  &e#" + (i + 1) + " &f" + name + " &7— &a" + currency.format(entry.getValue())));
        }
        s.sendMessage(CrazySMPPlugin.c("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return true;
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
