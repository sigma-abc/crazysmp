package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.gui.HomeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomesCommand implements CommandExecutor {
    private final CrazySMPPlugin p;

    public HomesCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player player)) {
            s.sendMessage(CrazySMPPlugin.c("&cPlayers only."));
            return true;
        }
        new HomeGUI(p).open(player, 0);
        return true;
    }
}
