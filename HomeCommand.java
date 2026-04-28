package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.gui.HomeGUI;
import dev.crazysmp.home.HomeEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class HomeCommand implements CommandExecutor {
    private final CrazySMPPlugin p;

    public HomeCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player player)) {
            s.sendMessage(CrazySMPPlugin.c("&cPlayers only."));
            return true;
        }
        if (!player.hasPermission("crazysmp.home")) {
            player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.no-permission", "")));
            return true;
        }

        if (args.length == 0) {
            // Open GUI
            p.getServer().getPluginManager().callEvent(new HomesOpenEvent(player));
            getGUI(p).open(player, 0);
            return true;
        }

        String homeName = args[0];
        Optional<HomeEntry> home = p.getHomeManager().getHome(player.getUniqueId(), homeName);
        if (home.isEmpty()) {
            player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-not-found", "&cNot found.").replace("%name%", homeName)));
            return true;
        }

        int delay = p.getConfig().getInt("homes.teleport-delay", 3);
        if (delay <= 0) {
            player.teleport(home.get().getLocation());
            player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-teleported", "&aTeleported.").replace("%name%", homeName)));
            return true;
        }

        player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-teleporting", "&7Teleporting...").replace("%name%", homeName).replace("%seconds%", String.valueOf(delay))));
        Location startLoc = player.getLocation().clone();
        boolean cancelOnMove = p.getConfig().getBoolean("homes.cancel-on-move", true);
        final HomeEntry entry = home.get();
        Bukkit.getScheduler().runTaskLater(p, () -> {
            if (!player.isOnline()) return;
            if (cancelOnMove && player.getLocation().distanceSquared(startLoc) > 0.5) {
                player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-cancelled", "&cCancelled.")));
                return;
            }
            player.teleport(entry.getLocation());
            player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-teleported", "&aTeleported.").replace("%name%", homeName)));
        }, delay * 20L);
        return true;
    }

    private HomeGUI getGUI(CrazySMPPlugin p) {
        return new HomeGUI(p);
    }
}
