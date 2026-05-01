package dev.crazysmp.gui;

import dev.crazysmp.CrazySMPPlugin;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.List;

public class RTPGUI {

    private final CrazySMPPlugin plugin;

    public RTPGUI(CrazySMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = CrazySMPPlugin.c(plugin.getConfig().getString("rtp.gui-title", "&6✈ Random Teleport"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Fill with glass panes
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane.clone());

        // Overworld — slot 10
        placeOption(inv, 10, "overworld", player);
        // Nether — slot 13
        placeOption(inv, 13, "nether", player);
        // End — slot 16
        placeOption(inv, 16, "end", player);

        // Centre info — slot 4
        ItemStack info = new ItemStack(Material.COMPASS);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(CrazySMPPlugin.c("&6✈ Random Teleport"));
        long cooldownLeft = plugin.getRTPManager().getCooldownRemaining(player.getUniqueId());
        if (cooldownLeft > 0) {
            im.setLore(List.of(
                    CrazySMPPlugin.c("&cOn cooldown: &e" + cooldownLeft + "s remaining")
            ));
        } else {
            im.setLore(List.of(CrazySMPPlugin.c("&aReady! Pick a world.")));
        }
        info.setItemMeta(im);
        inv.setItem(4, info);

        // Close — slot 26
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName(CrazySMPPlugin.c("&cClose"));
        close.setItemMeta(cm);
        inv.setItem(26, close);

        player.openInventory(inv);
    }

    private void placeOption(Inventory inv, int slot, String key, Player player) {
        String path = "rtp.options." + key;
        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) return;

        String matStr = plugin.getConfig().getString(path + ".material", "GRASS_BLOCK");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.GRASS_BLOCK;

        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(CrazySMPPlugin.c(plugin.getConfig().getString(path + ".name", "&aRTP")));

        List<String> lore = plugin.getConfig().getStringList(path + ".lore");
        m.setLore(lore.stream().map(CrazySMPPlugin::c).toList());

        long cd = plugin.getRTPManager().getCooldownRemaining(player.getUniqueId());
        if (cd > 0) {
            List<String> existingLore = m.getLore() != null
                    ? new ArrayList<>(m.getLore())
                    : new ArrayList<>();
        }

        item.setItemMeta(m);
        inv.setItem(slot, item);
    }
}