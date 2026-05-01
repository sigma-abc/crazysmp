package dev.crazysmp.migration;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationManager {

    private final CrazySMPPlugin plugin;

    public MigrationManager(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Essentials money migration ─────────────────────────────

    public int migrateEssentialsMoney() {
        AtomicInteger count = new AtomicInteger();
        String vaultCurrencyId = plugin.getCurrencyManager().getVaultCurrency() != null
                ? plugin.getCurrencyManager().getVaultCurrency().getId() : "dollars";

        String[] paths = {
                "plugins/Essentials/userdata",
                "plugins/EssentialsX/userdata"
        };

        for (String path : paths) {
            File dir = new File(plugin.getServer().getWorldContainer().getParentFile(), path);
            if (!dir.exists()) dir = new File(path); // try relative
            if (!dir.exists()) continue;

            File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
            if (files == null) continue;

            for (File file : files) {
                try {
                    String uuidStr = file.getName().replace(".yml", "");
                    UUID uuid = UUID.fromString(uuidStr);
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                    // Essentials stores money as a string or number under "money"
                    String moneyStr = cfg.getString("money", "0");
                    double money;
                    try {
                        money = Double.parseDouble(moneyStr.replace(",", ""));
                    } catch (NumberFormatException e) {
                        money = cfg.getDouble("money", 0.0);
                    }

                    if (money > 0) {
                        plugin.getCurrencyManager().importBalance(uuid, vaultCurrencyId, money);
                        count.incrementAndGet();
                        plugin.getLogger().info("Migrated $" + money + " for " + uuidStr);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Skipped file " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        if (count.get() > 0) plugin.getCurrencyManager().flushSave();
        return count.get();
    }

    // ── homeGUI homes migration ────────────────────────────────
    // Reads the homeGUI homes.yml format:
    // <uuid>:
    //   <homeName>:
    //     location:
    //       world: ...
    //       x: ...  y: ...  z: ...  pitch: ...  yaw: ...
    //     icon:
    //       id: minecraft:player_head  (or other material)

    public int migrateHomeGUIHomes(File sourceFile) {
        if (!sourceFile.exists()) {
            plugin.getLogger().warning("Home source file not found: " + sourceFile.getAbsolutePath());
            return 0;
        }

        AtomicInteger count = new AtomicInteger();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(sourceFile);

        for (String uuidStr : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                var playerSection = cfg.getConfigurationSection(uuidStr);
                if (playerSection == null) continue;

                for (String homeName : playerSection.getKeys(false)) {
                    try {
                        String base = uuidStr + "." + homeName;

                        // Location
                        String worldName = cfg.getString(base + ".location.world", "world");
                        double x = cfg.getDouble(base + ".location.x");
                        double y = cfg.getDouble(base + ".location.y");
                        double z = cfg.getDouble(base + ".location.z");
                        float pitch = (float) cfg.getDouble(base + ".location.pitch");
                        float yaw   = (float) cfg.getDouble(base + ".location.yaw");

                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            plugin.getLogger().warning("World not found for home: " + worldName + " (player: " + uuidStr + ")");
                            // Still import with offline world workaround
                            world = Bukkit.getWorlds().get(0);
                        }
                        Location loc = new Location(world, x, y, z, yaw, pitch);

                        // Icon — homeGUI stores it as "minecraft:player_head" etc.
                        String iconId = cfg.getString(base + ".icon.id", "minecraft:player_head");
                        String iconName = iconId.replace("minecraft:", "").toUpperCase().replace("-", "_");
                        Material icon = Material.matchMaterial(iconName);
                        if (icon == null) icon = Material.PLAYER_HEAD;

                        plugin.getHomeManager().importHome(uuid, homeName, loc, icon);
                        count.incrementAndGet();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed home " + homeName + " for " + uuidStr + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed player " + uuidStr + ": " + e.getMessage());
            }
        }

        if (count.get() > 0) plugin.getHomeManager().flushSave();
        return count.get();
    }
}