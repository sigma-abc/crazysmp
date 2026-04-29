package dev.crazysmp.command;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /forcedmg <player> <amount>
 *
 * Forces a player to take damage, bypassing Paper/Purpur invincibility
 * ticks and NoDamage game rules.  Uses the server-side EntityDamageEvent
 * mechanism to ensure damage visually registers (red flash, sounds, etc.)
 * while forcibly resetting the noDamageTicks counter before applying.
 *
 * Permission: crazysmp.forcedmg
 */
public class ForceDamageCommand implements CommandExecutor, TabCompleter {

    private final CrazySMPPlugin plugin;

    public ForceDamageCommand(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("crazysmp.forcedmg")) {
            sender.sendMessage(CrazySMPPlugin.c("&cNo permission."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(CrazySMPPlugin.c("&cUsage: /forcedmg <player> <amount>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CrazySMPPlugin.c("&cPlayer not found: &e" + args[0]));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(CrazySMPPlugin.c("&cInvalid amount."));
            return true;
        }

        forceDamage(target, amount);

        sender.sendMessage(CrazySMPPlugin.c(
            "&aForce-dealt &e" + amount + " &adamage to &6" + target.getName() + "&a."
        ));
        return true;
    }

    /**
     * Applies damage that cannot be cancelled by Paper's invincibility system.
     *
     * Steps:
     *  1. Zero out noDamageTicks so Paper won't skip the hit.
     *  2. Call player.damage() — this fires EntityDamageEvent properly,
     *     giving other plugins a chance to react (e.g. armour, shields)
     *     while guaranteeing the hit lands.
     *  3. If the player is still alive and somehow the damage didn't land
     *     (e.g. a protection plugin cancelled it), we fall back to directly
     *     setting health.
     */
    public static void forceDamage(Player player, double amount) {
        if (player == null || !player.isOnline()) return;
        if (amount <= 0) return;

        double healthBefore = player.getHealth();

        // Reset invincibility ticks — this is the key bypass for Paper
        player.setNoDamageTicks(0);

        // Apply damage through the normal Bukkit API so modifiers apply
        player.damage(amount);

        // Fallback: if health didn't change (likely cancelled by another plugin
        // or God-mode), force it directly.
        if (player.getHealth() >= healthBefore && player.isOnline()) {
            double newHealth = Math.max(0.0, player.getHealth() - amount);
            player.setHealth(newHealth);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        if (args.length == 2) return List.of("1", "5", "10", "20");
        return List.of();
    }
}
