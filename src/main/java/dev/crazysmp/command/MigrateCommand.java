package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MigrateCommand implements CommandExecutor {
    private final CrazySMPPlugin p;

    public MigrateCommand(CrazySMPPlugin p) {
        this.p = p;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!s.hasPermission("crazysmp.admin")) {
            s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.no-permission", "")));
            return true;
        }
        if (args.length == 0) {
            s.sendMessage(CrazySMPPlugin.c("&6Migration commands:"));
            s.sendMessage(CrazySMPPlugin.c("  &e/migrate essentials &7— Import Essentials/EssentialsX money"));
            s.sendMessage(CrazySMPPlugin.c("  &e/migrate homes <file> &7— Import homeGUI homes.yml"));
            s.sendMessage(CrazySMPPlugin.c("  &e/migrate all &7— Import everything"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "essentials" -> {
                s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.migrate-start", "&7Migrating...").replace("%source%", "Essentials")));
                Bukkit.getScheduler().runTaskAsynchronously(p, () -> {
                    int n = p.getMigrationManager().migrateEssentialsMoney();
                    Bukkit.getScheduler().runTask(p, () ->
                            s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.migrate-done", "&aDone! %count% migrated.").replace("%count%", String.valueOf(n)))));
                });
            }
            case "homes" -> {
                String path = args.length >= 2 ? args[1] : "plugins/homeGUI/homes.yml";
                java.io.File f = new java.io.File(path);
                if (!f.isAbsolute()) f = new java.io.File(p.getServer().getWorldContainer().getParentFile(), path);
                s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.migrate-start", "&7Migrating...").replace("%source%", "homeGUI")));
                final java.io.File finalF = f;
                Bukkit.getScheduler().runTaskAsynchronously(p, () -> {
                    int n = p.getMigrationManager().migrateHomeGUIHomes(finalF);
                    Bukkit.getScheduler().runTask(p, () ->
                            s.sendMessage(CrazySMPPlugin.c(p.getConfig().getString("messages.migrate-done", "&aDone! %count% migrated.").replace("%count%", String.valueOf(n)))));
                });
            }
            case "all" -> {
                s.sendMessage(CrazySMPPlugin.c("&7Running all migrations..."));
                Bukkit.getScheduler().runTaskAsynchronously(p, () -> {
                    int money = p.getMigrationManager().migrateEssentialsMoney();
                    java.io.File homeFile = new java.io.File(p.getServer().getWorldContainer().getParentFile(), "plugins/homeGUI/homes.yml");
                    int homes = p.getMigrationManager().migrateHomeGUIHomes(homeFile);
                    Bukkit.getScheduler().runTask(p, () ->
                            s.sendMessage(CrazySMPPlugin.c("&aDone! &e" + money + "&a money records, &e" + homes + "&a homes migrated.")));
                });
            }
            default -> s.sendMessage(CrazySMPPlugin.c("&cUnknown: use essentials, homes, or all."));
        }
        return true;
    }
}
