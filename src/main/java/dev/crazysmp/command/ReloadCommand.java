package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * /crazysmp reload
 *
 * Reloads config.yml and refreshes currency/baltop settings at runtime.
 * Permission: crazysmp.admin
 */
public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final CrazySMPPlugin plugin;

    public ReloadCommand(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("crazysmp.admin")) {
            sender.sendMessage(CrazySMPPlugin.c("&cNo permission."));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(CrazySMPPlugin.c("&eUsage: /crazysmp reload"));
            return true;
        }

        long start = System.currentTimeMillis();
        try {
            plugin.reload();
            long ms = System.currentTimeMillis() - start;
            sender.sendMessage(CrazySMPPlugin.c(
                "&8[&6CrazySMP&8] &aConfig reloaded successfully in &e" + ms + "ms&a."
            ));
        } catch (Exception ex) {
            sender.sendMessage(CrazySMPPlugin.c(
                "&cReload failed: &e" + ex.getMessage()
            ));
            plugin.getLogger().severe("Reload error: " + ex.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("crazysmp.admin"))
            return List.of("reload");
        return List.of();
    }
}
