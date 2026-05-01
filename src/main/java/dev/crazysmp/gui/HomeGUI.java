package dev.crazysmp.gui;

import dev.crazysmp.CrazySMPPlugin;
import dev.crazysmp.home.HomeEntry;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;

public class HomeGUI {

    private static final int HOMES_PER_PAGE = 21;

    // Fixed icon list for slot chooser — all guaranteed to be valid items
    private static final Material[] SLOT_COLORS = {
        Material.WHITE_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.MAGENTA_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.LIME_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.GRAY_STAINED_GLASS_PANE,
        Material.CYAN_STAINED_GLASS_PANE
    };

    private final CrazySMPPlugin plugin;

    public HomeGUI(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Main GUI ──────────────────────────────────────────────────

    public void open(Player player, int page) {
        String title = CrazySMPPlugin.c(
            plugin.getConfig().getString("homes.gui-title", "&8⌂ &6Your Homes")
        ) + (page > 0 ? " §8[§7" + (page + 1) + "§8]" : "");

        Inventory inv = Bukkit.createInventory(null, 27, title);

        List<HomeEntry> homeList = new ArrayList<>(
            plugin.getHomeManager().getHomes(player.getUniqueId()).values()
        );

        int start      = page * HOMES_PER_PAGE;
        int end        = Math.min(start + HOMES_PER_PAGE, homeList.size());
        int totalHomes = homeList.size();
        int maxHomes   = plugin.getHomeManager().getMaxHomes(player);

        // Slots 0-20: homes or filler
        Material fillerMat = Material.matchMaterial(
            plugin.getConfig().getString("homes.filler-material", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMat == null || !fillerMat.isItem()) fillerMat = Material.GRAY_STAINED_GLASS_PANE;

        for (int slot = 0; slot < HOMES_PER_PAGE; slot++) {
            int idx = start + slot;
            if (idx < end) {
                inv.setItem(slot, buildHomeItem(homeList.get(idx)));
            } else {
                inv.setItem(slot, buildFiller(fillerMat));
            }
        }

        // Slot 21: prev page
        inv.setItem(21, page > 0
            ? buildNavItem(Material.ARROW, "&7← Previous Page", "")
            : buildFillerPane());

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
            CrazySMPPlugin.c("&cShift+click &7a home to delete")
        ));
        info.setItemMeta(im);
        inv.setItem(22, info);

        // Slot 23: next page
        inv.setItem(23, end < homeList.size()
            ? buildNavItem(Material.ARROW, "&7Next Page →", "")
            : buildFillerPane());

        // Slot 24: pin-home-to-slot button
        ItemStack slotBtn = new ItemStack(Material.COMPARATOR);
        ItemMeta sm = slotBtn.getItemMeta();
        sm.setDisplayName(CrazySMPPlugin.c("&bPin Home to Slot"));
        sm.setLore(List.of(
            CrazySMPPlugin.c("&7Click to pin a home to a"),
            CrazySMPPlugin.c("&7specific inventory slot.")
        ));
        slotBtn.setItemMeta(sm);
        inv.setItem(24, slotBtn);

        // Slot 25: set home here
        boolean canSet = totalHomes < maxHomes || maxHomes == Integer.MAX_VALUE;
        ItemStack setHere = new ItemStack(canSet ? Material.EMERALD : Material.REDSTONE);
        ItemMeta shi = setHere.getItemMeta();
        if (canSet) {
            shi.setDisplayName(CrazySMPPlugin.c("&a✦ Set Home Here"));
            shi.setLore(List.of(
                CrazySMPPlugin.c("&7Click to create a home at"),
                CrazySMPPlugin.c("&7your current location."),
                "",
                CrazySMPPlugin.c("&eThe menu will close — type a name in chat.")
            ));
        } else {
            shi.setDisplayName(CrazySMPPlugin.c("&c✦ Home Limit Reached"));
            shi.setLore(List.of(CrazySMPPlugin.c("&7You have used all &e" + maxHomes + " &7slots.")));
        }
        setHere.setItemMeta(shi);
        inv.setItem(25, setHere);

        // Slot 26: close
        inv.setItem(26, buildNavItem(Material.BARRIER, "&cClose", ""));

        player.openInventory(inv);
        player.setMetadata("home_gui_page", new org.bukkit.metadata.FixedMetadataValue(plugin, page));
        player.setMetadata("home_can_set",  new org.bukkit.metadata.FixedMetadataValue(plugin, canSet));
    }

    // ── Slot Chooser GUI ──────────────────────────────────────────
    // Fixed: uses explicit safe Material list instead of ordinal arithmetic

    public void openSlotChooser(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            CrazySMPPlugin.c("&8⌂ Pin Home to Slot"));

        List<HomeEntry> homes = new ArrayList<>(
            plugin.getHomeManager().getHomes(player.getUniqueId()).values());

        if (homes.isEmpty()) {
            ItemStack none = new ItemStack(Material.PAPER);
            ItemMeta nm = none.getItemMeta();
            nm.setDisplayName(CrazySMPPlugin.c("&cYou have no homes to pin."));
            none.setItemMeta(nm);
            inv.setItem(13, none);
        } else {
            for (int i = 0; i < Math.min(homes.size(), 18); i++) {
                HomeEntry h = homes.get(i);
                ItemStack it = new ItemStack(h.getIcon());
                ItemMeta m = it.getItemMeta();
                m.setDisplayName(CrazySMPPlugin.c("&6⌂ &e" + h.getName()));
                m.setLore(List.of(
                    CrazySMPPlugin.c("&7Click to select, then pick a slot below.")
                ));
                it.setItemMeta(m);
                inv.setItem(i, it);
            }
        }

        // Bottom row (slots 18-26): hotbar slot 1-9 selectors
        // Uses SLOT_COLORS[] — all are confirmed valid items
        for (int hs = 0; hs < 9; hs++) {
            ItemStack slotItem = new ItemStack(SLOT_COLORS[hs]);
            ItemMeta slotMeta = slotItem.getItemMeta();
            slotMeta.setDisplayName(CrazySMPPlugin.c("&bSlot &f" + (hs + 1)));
            slotMeta.setLore(List.of(CrazySMPPlugin.c("&7Assign selected home to hotbar slot " + (hs + 1))));
            slotItem.setItemMeta(slotMeta);
            inv.setItem(18 + hs, slotItem);
        }

        player.setMetadata("slot_chooser_open",
            new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        player.openInventory(inv);
    }

    // ── Icon Picker GUI ───────────────────────────────────────────

    public void openIconPicker(Player player, String homeName) {
        Inventory inv = Bukkit.createInventory(null, 27,
            CrazySMPPlugin.c("&8Pick an icon for &6" + homeName));

        // All verified valid item materials
        Material[] icons = {
            Material.PLAYER_HEAD,       Material.DIAMOND,            Material.EMERALD,
            Material.GOLD_INGOT,        Material.IRON_INGOT,         Material.NETHERITE_INGOT,
            Material.BEACON,            Material.RESPAWN_ANCHOR,     Material.ENDER_CHEST,
            Material.CHEST,             Material.RED_BED,            Material.COMPASS,
            Material.MAP,               Material.GRASS_BLOCK,        Material.NETHERRACK,
            Material.END_STONE,         Material.CAMPFIRE,           Material.LANTERN,
            Material.TORCH,             Material.GLOWSTONE,          Material.AMETHYST_SHARD,
            Material.QUARTZ,            Material.PRISMARINE_CRYSTALS,Material.BLAZE_ROD,
            Material.NETHER_STAR,       Material.CHORUS_FRUIT,       Material.APPLE
        };

        for (int i = 0; i < Math.min(icons.length, 27); i++) {
            ItemStack item = new ItemStack(icons[i]);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName(CrazySMPPlugin.c("&e" +
                icons[i].name().toLowerCase().replace("_", " ")));
            m.setLore(List.of(CrazySMPPlugin.c("&7Click to use as home icon")));
            item.setItemMeta(m);
            inv.setItem(i, item);
        }

        player.setMetadata("home_icon_pick",
            new org.bukkit.metadata.FixedMetadataValue(plugin, homeName));
        player.openInventory(inv);
    }

    // ── Item builders ─────────────────────────────────────────────

    private ItemStack buildHomeItem(HomeEntry home) {
        ItemStack item = new ItemStack(home.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CrazySMPPlugin.c("&6⌂ &e" + home.getName()));
        Location l = home.getLocation();
        meta.setLore(List.of(
            CrazySMPPlugin.c("&7World: &f" + l.getWorld().getName()),
            CrazySMPPlugin.c("&7X: &f" + Math.round(l.getX())
                + " &7Y: &f" + Math.round(l.getY())
                + " &7Z: &f" + Math.round(l.getZ())),
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
        return buildFiller(Material.BLACK_STAINED_GLASS_PANE);
    }

    private ItemStack buildFiller(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(" ");
        item.setItemMeta(m);
        return item;
    }
}
