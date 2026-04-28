package dev.crazysmp.gui;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.home.HomeEntry;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;

public class HomeGUI {

    private static final int HOMES_PER_PAGE = 21; // rows 1-3 (slots 0-20)
    // Row 4 (slots 21-26) is navigation/actions

    private final CrazySMPPlugin plugin;

    public HomeGUI(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        String title = CrazySMPPlugin.c(plugin.getConfig().getString("homes.gui-title", "&6⌂ Your Homes"))
                + (page > 0 ? " §8[§7" + (page + 1) + "§8]" : "");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        List<HomeEntry> homeList = new ArrayList<>(plugin.getHomeManager().getHomes(player.getUniqueId()).values());
        int start = page * HOMES_PER_PAGE;
        int end   = Math.min(start + HOMES_PER_PAGE, homeList.size());

        // Fill home slots (0-20)
        Material fillerMat = Material.matchMaterial(
                plugin.getConfig().getString("homes.filler-material", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMat == null) fillerMat = Material.GRAY_STAINED_GLASS_PANE;

        for (int slot = 0; slot < HOMES_PER_PAGE; slot++) {
            int homeIdx = start + slot;
            if (homeIdx < end) {
                HomeEntry home = homeList.get(homeIdx);
                inv.setItem(slot, buildHomeItem(home));
            } else {
                // Empty slot filler
                ItemStack filler = new ItemStack(fillerMat);
                ItemMeta fm = filler.getItemMeta();
                fm.setDisplayName(" ");
                filler.setItemMeta(fm);
                inv.setItem(slot, filler);
            }
        }

        // Row 4 — nav/actions (slots 21-26)
        int maxHomes = plugin.getHomeManager().getMaxHomes(player);
        int totalHomes = homeList.size();

        // Slot 21: prev page
        if (page > 0) {
            inv.setItem(21, buildNavItem(Material.ARROW, "&7← Previous Page", ""));
        } else {
            inv.setItem(21, buildFillerPane());
        }

        // Slot 22: info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(CrazySMPPlugin.c("&6Your Homes"));
        im.setLore(List.of(
                CrazySMPPlugin.c("&7Used: &e" + totalHomes + " &7/ &e" +
                        (maxHomes == Integer.MAX_VALUE ? "∞" : maxHomes)),
                CrazySMPPlugin.c("&7Page: &e" + (page + 1)),
                "",
                CrazySMPPlugin.c("&eLeft-click &7a home to teleport"),
                CrazySMPPlugin.c("&eRight-click &7a home to change icon"),
                CrazySMPPlugin.c("&eShift+click &7a home to delete")
        ));
        info.setItemMeta(im);
        inv.setItem(22, info);

        // Slot 23: next page
        if (end < homeList.size()) {
            inv.setItem(23, buildNavItem(Material.ARROW, "&7Next Page →", ""));
        } else {
            inv.setItem(23, buildFillerPane());
        }

        // Slot 24: filler
        inv.setItem(24, buildFillerPane());

        // Slot 25: set home shortcut info
        ItemStack setHomeInfo = new ItemStack(Material.COMPASS);
        ItemMeta shi = setHomeInfo.getItemMeta();
        shi.setDisplayName(CrazySMPPlugin.c("&aSet a Home"));
        shi.setLore(List.of(CrazySMPPlugin.c("&7Use &e/sethome <name> &7to"), CrazySMPPlugin.c("&7create a new home.")));
        setHomeInfo.setItemMeta(shi);
        inv.setItem(25, setHomeInfo);

        // Slot 26: close
        inv.setItem(26, buildNavItem(Material.BARRIER, "&cClose", ""));

        player.openInventory(inv);
        // Store page in metadata
        player.setMetadata("home_gui_page", new org.bukkit.metadata.FixedMetadataValue(plugin, page));
    }

    private ItemStack buildHomeItem(HomeEntry home) {
        ItemStack item = new ItemStack(home.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CrazySMPPlugin.c("&6⌂ &e" + home.getName()));
        Location l = home.getLocation();
        meta.setLore(List.of(
                CrazySMPPlugin.c("&7World: &f" + l.getWorld().getName()),
                CrazySMPPlugin.c("&7X: &f" + Math.round(l.getX()) +
                        " &7Y: &f" + Math.round(l.getY()) +
                        " &7Z: &f" + Math.round(l.getZ())),
                "",
                CrazySMPPlugin.c("&eLeft-click &7to teleport"),
                CrazySMPPlugin.c("&eRight-click &7to change icon"),
                CrazySMPPlugin.c("&cShift+click &7to delete")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CrazySMPPlugin.c(name));
        if (!lore.isEmpty()) meta.setLore(List.of(CrazySMPPlugin.c(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFillerPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(" ");
        item.setItemMeta(m);
        return item;
    }

    // ── Icon picker GUI ───────────────────────────────────────

    public void openIconPicker(Player player, String homeName) {
        Inventory inv = Bukkit.createInventory(null, 27,
                CrazySMPPlugin.c("&8Pick an icon for &6" + homeName));

        Material[] icons = {
                Material.PLAYER_HEAD, Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT,
                Material.IRON_INGOT, Material.NETHERITE_INGOT, Material.BEACON, Material.RESPAWN_ANCHOR,
                Material.ENDER_CHEST, Material.CHEST, Material.RED_BED, Material.COMPASS,
                Material.MAP, Material.GRASS_BLOCK, Material.NETHERRACK, Material.END_STONE,
                Material.CAMPFIRE, Material.LANTERN, Material.TORCH, Material.GLOWSTONE,
                Material.AMETHYST_SHARD, Material.QUARTZ, Material.PRISMARINE_CRYSTALS,
                Material.BLAZE_ROD, Material.NETHER_STAR, Material.CHORUS_FRUIT, Material.APPLE
        };

        for (int i = 0; i < Math.min(icons.length, 27); i++) {
            ItemStack item = new ItemStack(icons[i]);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName(CrazySMPPlugin.c("&e" + icons[i].name().toLowerCase().replace("_", " ")));
            m.setLore(List.of(CrazySMPPlugin.c("&7Click to use as home icon")));
            item.setItemMeta(m);
            inv.setItem(i, item);
        }

        player.setMetadata("home_icon_pick", new org.bukkit.metadata.FixedMetadataValue(plugin, homeName));
        player.openInventory(inv);
    }
}