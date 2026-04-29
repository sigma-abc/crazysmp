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

public class EcoCommand implements CommandExecutor, TabCompleter {
    private final CrazySMPPlugin p;

    public EcoCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!s.hasPermission("crazysmp.eco.admin")) {
            s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }
        if (args.length < 3) {
            s.sendMessage(CrazySMPPlugin.c("&cUsage: /eco <give|take|set|reset> <player> <amount> [currency]"));
            return true;
        }

        String action = args[0].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID uuid = target.getUniqueId();
        Currency currency = args.length >= 4 ? p.getCurrencyManager().resolveCurrency(args[3]) : p.getCurrencyManager().getVaultCurrency();
        if (currency == null) {
            s.sendMessage(CrazySMPPlugin.c("&cUnknown currency."));
            return true;
        }

        double amount = 0;
        if (!action.equals("reset")) {
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                s.sendMessage(CrazySMPPlugin.c("&cInvalid amount."));
                return true;
            }
        }

        String name = target.getName() != null ? target.getName() : uuid.toString();
        switch (action) {
            case "give" -> {
                p.getCurrencyManager().deposit(uuid, currency.getId(), amount);
                s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.eco-give", "&aGave.").replace("%player%", name).replace("%symbol%", currency.getSymbol()).replace("%amount%", currency.format(amount)).replace("%currency%", currency.getDisplayName())));
            }
            case "take" -> {
                p.getCurrencyManager().withdraw(uuid, currency.getId(), amount);
                s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.eco-take", "&aTook.").replace("%player%", name).replace("%symbol%", currency.getSymbol()).replace("%amount%", currency.format(amount)).replace("%currency%", currency.getDisplayName())));
            }
            case "set" -> {
                p.getCurrencyManager().setBalance(uuid, currency.getId(), amount);
                s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.eco-set", "&aSet.").replace("%player%", name).replace("%symbol%", currency.getSymbol()).replace("%amount%", currency.format(amount)).replace("%currency%", currency.getDisplayName())));
            }
            case "reset" -> {
                p.getCurrencyManager().resetBalance(uuid, currency.getId());
                s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.eco-reset", "&aReset.").replace("%player%", name).replace("%currency%", currency.getDisplayName())));
            }
            default -> s.sendMessage(CrazySMPPlugin.c("&cUnknown action. Use give/take/set/reset."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] args) {
        if (args.length == 1) return List.of("give", "take", "set", "reset");
        if (args.length == 2)
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        if (args.length == 4)
            return p.getCurrencyManager().getCurrencies().stream().map(Currency::getId).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
