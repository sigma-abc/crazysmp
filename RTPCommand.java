package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.gui.RTPGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RTPCommand implements CommandExecutor {

    private final CrazySMPPlugin p;

    public RTPCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only.");
            return true;
        }

        if (!player.hasPermission("crazysmp.rtp")) {
            player.sendMessage("§cYou do not have permission.");
            return true;
        }

        new RTPGUI(p).open(player);
        return true;
    }
}
