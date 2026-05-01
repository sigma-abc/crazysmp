package dev.crazysmp.home;

import org.bukkit.Location;
import org.bukkit.Material;

public class HomeEntry {
    private final String name;
    private Location location;
    private Material icon;

    public HomeEntry(String name, Location location, Material icon) {
        this.name     = name;
        this.location = location;
        this.icon     = icon != null ? icon : Material.PLAYER_HEAD;
    }

    public String getName()        { return name; }
    public Location getLocation()  { return location.clone(); }
    public void setLocation(Location l) { this.location = l; }
    public Material getIcon()      { return icon; }
    public void setIcon(Material m){ this.icon = m; }
}