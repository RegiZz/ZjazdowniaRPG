package eu.zjazdownia.rpg.magicItems;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.cities.City;
import eu.zjazdownia.rpg.commands.CityComands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Heartstone implements Listener {

    private final ZjazdowniaRPG plugin;
    private final CityComands cityCommands;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> teleporting = new HashSet<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private final long COOLDOWN_MS = 30_000; // 30 sekund
    private final int TELEPORT_DELAY = 10; // sekundy na odliczanie

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

        e.setCancelled(true);

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long end = cooldowns.getOrDefault(uuid, 0L);

        if (now < end) {
            long secondsLeft = (end - now) / 1000;
            player.sendMessage(ChatColor.RED + "Musisz poczekać " + secondsLeft + " sekund, aby ponownie użyć Heartstone!");
            return;
        }

        City nearest = cityCommands.findNearestCity(player.getLocation());
        if (nearest == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono żadnego miasta w pobliżu!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Rozpocznij odliczanie
        startTeleportCountdown(player, nearest);
    }

    private void startTeleportCountdown(Player player, City city) {
        UUID uuid = player.getUniqueId();
        teleporting.add(uuid);
        player.sendMessage(ChatColor.AQUA + "Rozpoczynasz teleportację do " + ChatColor.LIGHT_PURPLE + city.getName() + ChatColor.AQUA + "...");
        player.sendMessage(ChatColor.GRAY + "Nie ruszaj się przez " + TELEPORT_DELAY + " sekund!");

        Location startLoc = player.getLocation().clone();

        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int secondsLeft = TELEPORT_DELAY;

            @Override
            public void run() {
                if (!player.isOnline() || !teleporting.contains(uuid)) {
                    teleporting.remove(uuid);
                    return;
                }

                if (secondsLeft <= 0 && teleporting.contains(uuid)) {
                    teleporting.remove(uuid);
                    Location dest = city.getLocation();
                    player.teleport(dest);
                    Component teleport = mm.deserialize("<gradient:#00FF00:#55FF55:#AAFF55:#00AA00>☑ Teleportowano do miasta: </gradient>"
                            + "<gradient:#ff00ff:#00ffff>" + city.getName() + "</gradient>");
                    player.sendMessage(teleport);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                    // cooldown
                    cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_MS);
                    player.sendMessage(ChatColor.GRAY + "(Heartstone dostępny ponownie za 30 sekund)");
                    Bukkit.getScheduler().cancelTask(this.hashCode());
                    return;
                }

                // Pokazanie odliczania nad hotbarem (ActionBar)
                String legacy = "§eTeleportacja za §6" + secondsLeft + "s §7- §oNie ruszaj się!";
                player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(legacy));

                secondsLeft--;
            }

            @Override
            public int hashCode() {
                return uuid.hashCode() ^ city.getName().hashCode();
            }
        }, 0L, 20L);
    }

    private boolean hasMovedSignificantly(Location from, Location to) {
        if (from == null || to == null) return false;
        if (!from.getWorld().equals(to.getWorld())) return true;
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();
        return (dx * dx + dy * dy + dz * dz) > 0.01;
    }

    // Jeśli gracz się ruszy — przerwij teleport
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (teleporting.contains(p.getUniqueId())) {
            if (hasMovedSignificantly(e.getFrom(), e.getTo())) {
                cooldowns.remove(p.getUniqueId());
                teleporting.remove(p.getUniqueId());
                p.sendMessage(ChatColor.RED + "❌ Teleportacja przerwana – poruszyłeś się!");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }
}
