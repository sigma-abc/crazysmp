package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.economy.Currency;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {
    private final CrazySMPPlugin p;

    public PayCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player sender)) {
            s.sendMessage(CrazySMPPlugin.c("&cPlayers only."));
            return true;
        }
        if (args.length < 2) {
            s.sendMessage(CrazySMPPlugin.c("&cUsage: /pay <player> <amount> [currency]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.player-not-found", "&cNot found").replace("%player%", args[0])));
            return true;
        }
        if (target.equals(sender)) {
            sender.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.pay-self", "&cCan't pay yourself.")));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(CrazySMPPlugin.c("&cInvalid amount."));
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(CrazySMPPlugin.c("&cAmount must be positive."));
            return true;
        }

        Currency currency = args.length >= 3 ? p.getCurrencyManager().resolveCurrency(args[2]) : p.getCurrencyManager().getVaultCurrency();
        if (currency == null) {
            sender.sendMessage(CrazySMPPlugin.c("&cUnknown currency."));
            return true;
        }

        if (!p.getCurrencyManager().hasBalance(sender.getUniqueId(), currency.getId(), amount)) {
            sender.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.pay-not-enough", "&cNot enough.").replace("%currency%", currency.getDisplayName())));
            return true;
        }

        p.getCurrencyManager().withdraw(sender.getUniqueId(), currency.getId(), amount);
        p.getCurrencyManager().deposit(target.getUniqueId(), currency.getId(), amount);

        sender.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.pay-sent", "&aSent.")
                .replace("%symbol%", currency.getSymbol()).replace("%amount%", currency.format(amount))
                .replace("%currency%", currency.getDisplayName()).replace("%player%", target.getName())));
        target.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.pay-received", "&aReceived.")
                .replace("%symbol%", currency.getSymbol()).replace("%amount%", currency.format(amount))
                .replace("%currency%", currency.getDisplayName()).replace("%player%", sender.getName())));
        return true;
    }
}
