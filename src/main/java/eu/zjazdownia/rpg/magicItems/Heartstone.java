package eu.zjazdownia.rpg.magicItems;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.cities.City;
import eu.zjazdownia.rpg.commands.CityComands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class Heartstone implements Listener {

    private final ZjazdowniaRPG plugin;
    private final CityComands cityCommands;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private final long COOLDOWN_MS = 30_000; // 30 sekund

    public Heartstone(ZjazdowniaRPG plugin, CityComands cityCommands) {
        this.plugin = plugin;
        this.cityCommands = cityCommands;
    }

    public static ItemStack createHeartstone() {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "❤ Heartstone");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Kliknij prawym, aby teleportować się do najbliższego miasta"));
            star.setItemMeta(meta);
        }
        return star;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().name().contains("RIGHT_CLICK")) return;

        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.NETHER_STAR) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().getDisplayName().contains("Heartstone")) return;

        // Sprawdzenie cooldownu
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long end = cooldowns.getOrDefault(uuid, 0L);

        if (now < end) {
            long secondsLeft = (end - now) / 1000;
            player.sendMessage(ChatColor.RED + "Musisz poczekać " + secondsLeft + " sekund, aby ponownie użyć Heartstone!");
            e.setCancelled(true);
            return;
        }

        // Szukamy najbliższego miasta
        City nearest = cityCommands.findNearestCity(player.getLocation());
        if (nearest == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono żadnego miasta w pobliżu!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            e.setCancelled(true);
            return;
        }

        // Teleportacja do środka miasta
        Location center = nearest.getLocation();
        player.teleport(center);
        Component teleport = mm.deserialize("<gradient:#00FF00:#55FF55:#AAFF55:#00AA00>☑ Teleportowano do najbliższego miasta: </gradient>" +"<gradient:#ff00ff:#00ffff>" +nearest.getName() + "</gradient>");
        player.sendMessage(teleport);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        // Ustawienie cooldownu
        cooldowns.put(uuid, now + COOLDOWN_MS);

        e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cooldowns.remove(e.getPlayer().getUniqueId());
    }
}
