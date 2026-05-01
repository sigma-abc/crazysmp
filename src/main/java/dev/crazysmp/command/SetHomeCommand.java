package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand implements CommandExecutor {
    private final CrazySMPPlugin p;

    public SetHomeCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player player)) {
            s.sendMessage(CrazySMPPlugin.c("&cPlayers only."));
            return true;
        }
        if (!player.hasPermission("crazysmp.sethome")) {
            player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.no-permission", "")));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(CrazySMPPlugin.c("&cUsage: /sethome <n>"));
            return true;
        }

        String name = args[0];
        int max = p.getHomeManager().getMaxHomes(player);
        int current = p.getHomeManager().getHomes(player.getUniqueId()).size();

        // If home already exists, overwriting is fine
        boolean exists = p.getHomeManager().getHome(player.getUniqueId(), name).isPresent();
        if (!exists && current >= max) {
            player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-limit", "&cLimit reached.").replace("%max%", max == Integer.MAX_VALUE ? "∞" : String.valueOf(max))));
            return true;
        }

        p.getHomeManager().setHome(player.getUniqueId(), name, player.getLocation(), Material.PLAYER_HEAD);
        player.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.home-set", "&aHome set!").replace("%name%", name)));
        return true;
    }
}
