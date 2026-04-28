package dev.crazysmp.rtp;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RTPManager {

    private final CrazySMPPlugin plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public RTPManager(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    public long getCooldownRemaining(UUID uuid) {
        long expires = cooldowns.getOrDefault(uuid, 0L);
        long remaining = (expires - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public void teleport(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(plugin.msgRaw("rtp-failed"));
            return;
        }

        int minR = plugin.getConfig().getInt("rtp.min-radius", 1000);
        int maxR = plugin.getConfig().getInt("rtp.max-radius", 50000);
        int maxAttempts = plugin.getConfig().getInt("rtp.max-attempts", 50);
        List<String> excludedBiomes = plugin.getConfig().getStringList("rtp.excluded-biomes");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Random rng = new Random();
            Location safe = null;

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int range = maxR - minR;
                int x = (rng.nextBoolean() ? 1 : -1) * (minR + rng.nextInt(range));
                int z = (rng.nextBoolean() ? 1 : -1) * (minR + rng.nextInt(range));

                // Check biome (async safe in modern Paper)
                try {
                    Biome biome = world.getBiome(x, 64, z);
                    if (excludedBiomes.contains(biome.name())) continue;
                } catch (Exception ignored) {}

                int y = world.getHighestBlockYAt(x, z);
                if (y < world.getMinHeight() || y > world.getMaxHeight()) continue;

                Location candidate = new Location(world, x + 0.5, y + 1, z + 0.5);
                Material below = world.getBlockAt(x, y, z).getType();
                // Exclude lava/water surface
                if (below == Material.LAVA || below == Material.WATER) continue;
                if (below == Material.AIR || below == Material.VOID_AIR) continue;

                safe = candidate;
                break;
            }

            final Location finalLoc = safe;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalLoc == null) {
                    player.sendMessage(plugin.msgRaw("rtp-failed"));
                    return;
                }
                player.teleport(finalLoc);
                int cooldown = plugin.getConfig().getInt("rtp.cooldown", 300);
                if (cooldown > 0) cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);

                String msg = plugin.msgRaw("rtp-teleported")
                        .replace("%x%", String.valueOf(finalLoc.getBlockX()))
                        .replace("%y%", String.valueOf(finalLoc.getBlockY()))
                        .replace("%z%", String.valueOf(finalLoc.getBlockZ()))
                        .replace("%world%", world.getName());
                player.sendMessage(msg);
            });
        });
    }
}