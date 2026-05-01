package dev.crazysmp.rtp;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RTPManager — updated to block teleportation to the nether roof.
 *
 * The nether roof in Minecraft is Y >= 128 (build limit in the nether).
 * If a randomly chosen coordinate lands above the bedrock ceiling
 * (Y > 127 in the nether), the attempt is discarded and retried.
 */
public class RTPManager {

    private final CrazySMPPlugin plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    /** Nether Y-ceiling — blocks above this are the void roof area */
    private static final int NETHER_ROOF_Y = 127;

    public RTPManager(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    public long getCooldownRemaining(UUID uuid) {
        long expires   = cooldowns.getOrDefault(uuid, 0L);
        long remaining = (expires - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public void teleport(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(plugin.msgRaw("rtp-failed"));
            return;
        }

        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

        int minR        = plugin.getConfig().getInt("rtp.min-radius", 1000);
        int maxR        = plugin.getConfig().getInt("rtp.max-radius", 50000);
        int maxAttempts = plugin.getConfig().getInt("rtp.max-attempts", 50);
        List<String> excludedBiomes = plugin.getConfig().getStringList("rtp.excluded-biomes");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Random rng = new Random();
            Location safe = null;

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int angle = rng.nextInt(360);
                int dist  = minR + rng.nextInt(maxR - minR);
                int x     = (int) (Math.cos(Math.toRadians(angle)) * dist);
                int z     = (int) (Math.sin(Math.toRadians(angle)) * dist);

                // Find highest solid block (sync-friendly via chunk snapshot)
                int y = world.getHighestBlockYAt(x, z);

                // ── Nether roof check ────────────────────────────────────────
                // In the nether, getHighestBlockYAt can return a Y above the
                // bedrock ceiling (≥128) if the roof is solid bedrock.
                // We skip any location above NETHER_ROOF_Y.
                if (isNether && y > NETHER_ROOF_Y) continue;

                // Also skip bedrock layer itself for nether (y == 127 often = roof)
                if (isNether && world.getBlockAt(x, y, z).getType() == Material.BEDROCK) continue;

                if (y < world.getMinHeight() || y > world.getMaxHeight()) continue;

                Location candidate = new Location(world, x + 0.5, y + 1, z + 0.5);
                Material below = world.getBlockAt(x, y, z).getType();

                if (below == Material.LAVA || below == Material.WATER) continue;
                if (below == Material.AIR  || below == Material.VOID_AIR) continue;

                // Biome exclusion
                String biomeName = world.getBiome(x, y, z).name();
                if (excludedBiomes.stream().anyMatch(b -> b.equalsIgnoreCase(biomeName))) continue;

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
                if (cooldown > 0)
                    cooldowns.put(player.getUniqueId(),
                        System.currentTimeMillis() + cooldown * 1000L);

                String msg = plugin.msgRaw("rtp-teleported")
                    .replace("%x%",     String.valueOf(finalLoc.getBlockX()))
                    .replace("%y%",     String.valueOf(finalLoc.getBlockY()))
                    .replace("%z%",     String.valueOf(finalLoc.getBlockZ()))
                    .replace("%world%", world.getName());
                player.sendMessage(msg);
            });
        });
    }
}
