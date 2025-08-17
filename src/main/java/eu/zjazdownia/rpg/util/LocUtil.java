package eu.zjazdownia.rpg.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class LocUtil {
    public static ConfigurationSection toSection(Location loc) {
        ConfigurationSection sec = new org.bukkit.configuration.MemoryConfiguration();
        sec.set("world", loc.getWorld().getName());
        sec.set("x", loc.getX());
        sec.set("y", loc.getY());
        sec.set("z", loc.getZ());
        sec.set("yaw", loc.getYaw());
        sec.set("pitch", loc.getPitch());
        return sec;
    }

    public static Location fromSection(ConfigurationSection sec) {
        if (sec == null) return null;
        String w = sec.getString("world");
        if (w == null || Bukkit.getWorld(w) == null) return null;
        return new Location(
                Bukkit.getWorld(w),
                sec.getDouble("x"),
                sec.getDouble("y"),
                sec.getDouble("z"),
                (float) sec.getDouble("yaw"),
                (float) sec.getDouble("pitch")
        );
    }
}
