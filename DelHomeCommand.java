package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DelHomeCommand implements CommandExecutor, TabCompleter {
    private final CrazySMPPlugin p;

    public DelHomeCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player player)) {
            s.sendMessage(CrazySMPPlugin.c("&cPlayers only."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(CrazySMPPlugin.c("&cUsage: /delhome <n>"));
            return true;
        }
        boolean deleted = p.getHomeManager().deleteHome(player.getUniqueId(), args[0]);
        if (deleted)
            player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-deleted", "&aDeleted.").replace("%name%", args[0])));
        else
            player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-not-found", "&cNot found.").replace("%name%", args[0])));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] args) {
        if (args.length == 1 && s instanceof Player pl)
            return new ArrayList<>(p.getHomeManager().getHomes(pl.getUniqueId()).keySet());
        return Collections.emptyList();
    }
}
