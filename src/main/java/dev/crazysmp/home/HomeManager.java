package dev.crazysmp.home;

import dev.crazysmp.CrazySMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class HomeManager {

    private final CrazySMPPlugin plugin;
    // uuid -> (homeName -> HomeEntry)
    private final Map<UUID, Map<String, HomeEntry>> homes = new HashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public HomeManager(CrazySMPPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    // ── Persistence ───────────────────────────────────────────

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (Exception e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        homes.clear();

        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, HomeEntry> playerHomes = new LinkedHashMap<>();
                ConfigurationSection sec = dataConfig.getConfigurationSection(uuidStr);
                if (sec == null) continue;
                for (String homeName : sec.getKeys(false)) {
                    String base = uuidStr + "." + homeName;
                    String worldName = dataConfig.getString(base + ".world");
                    double x = dataConfig.getDouble(base + ".x");
                    double y = dataConfig.getDouble(base + ".y");
                    double z = dataConfig.getDouble(base + ".z");
                    float pitch = (float) dataConfig.getDouble(base + ".pitch");
                    float yaw   = (float) dataConfig.getDouble(base + ".yaw");
                    String iconStr = dataConfig.getString(base + ".icon", "PLAYER_HEAD");
                    Material icon = Material.matchMaterial(iconStr);
                    if (icon == null) icon = Material.PLAYER_HEAD;
                    World world = Bukkit.getWorld(worldName != null ? worldName : "world");
                    if (world == null) continue;
                    Location loc = new Location(world, x, y, z, yaw, pitch);
                    playerHomes.put(homeName, new HomeEntry(homeName, loc, icon));
                }
                homes.put(uuid, playerHomes);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load home for " + uuidStr + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded homes for " + homes.size() + " players.");
    }

    public void saveAll() {
        dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, HomeEntry>> entry : homes.entrySet()) {
            String uuid = entry.getKey().toString();
            for (HomeEntry home : entry.getValue().values()) {
                String base = uuid + "." + home.getName();
                Location l = home.getLocation();
                dataConfig.set(base + ".world", l.getWorld().getName());
                dataConfig.set(base + ".x", l.getX());
                dataConfig.set(base + ".y", l.getY());
                dataConfig.set(base + ".z", l.getZ());
                dataConfig.set(base + ".pitch", l.getPitch());
                dataConfig.set(base + ".yaw", l.getYaw());
                dataConfig.set(base + ".icon", home.getIcon().name());
            }
        }
        try { dataConfig.save(dataFile); } catch (Exception e) { e.printStackTrace(); }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }

    // ── API ───────────────────────────────────────────────────

    public Map<String, HomeEntry> getHomes(UUID uuid) {
        return homes.getOrDefault(uuid, Collections.emptyMap());
    }

    public Optional<HomeEntry> getHome(UUID uuid, String name) {
        return Optional.ofNullable(homes.getOrDefault(uuid, Collections.emptyMap()).get(name));
    }

    public boolean setHome(UUID uuid, String name, Location loc, Material icon) {
        homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                .put(name, new HomeEntry(name, loc, icon));
        saveAsync();
        return true;
    }

    public boolean deleteHome(UUID uuid, String name) {
        Map<String, HomeEntry> playerHomes = homes.get(uuid);
        if (playerHomes == null) return false;
        boolean removed = playerHomes.remove(name) != null;
        if (removed) saveAsync();
        return removed;
    }

    public void updateIcon(UUID uuid, String homeName, Material icon) {
        HomeEntry entry = homes.getOrDefault(uuid, Collections.emptyMap()).get(homeName);
        if (entry != null) { entry.setIcon(icon); saveAsync(); }
    }

    /** Max homes allowed for player via LuckPerms permissions */
    public int getMaxHomes(Player player) {
        if (player.hasPermission("crazysmp.homes.unlimited")) return Integer.MAX_VALUE;
        for (int i = 9; i >= 1; i--) {
            if (player.hasPermission("crazysmp.homes." + i)) return i;
        }
        return plugin.getConfig().getInt("homes.default-max", 1);
    }

    /** Import a home directly (used by migration) */
    public void importHome(UUID uuid, String name, Location loc, Material icon) {
        homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                .put(name, new HomeEntry(name, loc, icon));
    }

    public void flushSave() { saveAll(); }
}