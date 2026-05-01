package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.economy.Currency;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BalanceCommand implements CommandExecutor, TabCompleter {
    private final CrazySMPPlugin p;

    public BalanceCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        UUID target;
        Currency currency;

        if (args.length >= 1) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
            target = op.getUniqueId();
            currency = args.length >= 2 ? p.getCurrencyManager().resolveCurrency(args[1])
                    : p.getCurrencyManager().getVaultCurrency();
        } else if (s instanceof Player pl) {
            target = pl.getUniqueId();
            currency = p.getCurrencyManager().getVaultCurrency();
        } else {
            s.sendMessage(CrazySMPPlugin.c("&cUsage: /balance <player> [currency]"));
            return true;
        }

        if (currency == null) {
            s.sendMessage(CrazySMPPlugin.c("&cUnknown currency."));
            return true;
        }

        double bal = p.getCurrencyManager().getBalance(target, currency.getId());
        boolean isSelf = (s instanceof Player pl) && pl.getUniqueId().equals(target);
        String msgKey = isSelf ? "balance-self" : "balance-other";
        String name = Bukkit.getOfflinePlayer(target).getName();

        s.sendMessage(CrazySMPPlugin.c(
                p.getConfig().getString("messages." + msgKey, "&7Balance: %amount%")
                        .replace("%currency%", currency.getDisplayName())
                        .replace("%symbol%", currency.getSymbol())
                        .replace("%amount%", currency.format(bal))
                        .replace("%player%", name != null ? name : target.toString())
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return p.getCurrencyManager().getCurrencies().stream()
                .map(Currency::getId).filter(n -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
