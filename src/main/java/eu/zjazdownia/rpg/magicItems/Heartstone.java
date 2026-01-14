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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Heartstone implements Listener {

    private final ZjazdowniaRPG plugin;
    private final CityComands cityCommands;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> teleporting = new HashSet<>();
    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();
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
            NamespacedKey key = new NamespacedKey("zjazdownia", "undroppable");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

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
        if(teleportTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Poczekaj na poprzednią teleportacje!");
            return;
        }

        e.setCancelled(true);

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long end = cooldowns.getOrDefault(uuid, Long.valueOf(0L));

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

        // jeśli już istnieje aktywny task dla tego gracza, anuluj go (zapobiega nakładaniu)
        if (teleportTasks.containsKey(uuid)) {
            teleportTasks.get(uuid).cancel();
            teleportTasks.remove(uuid);
            teleporting.remove(uuid);
        }

        teleporting.add(uuid);
        player.sendMessage(ChatColor.AQUA + "Rozpoczynasz teleportację do " + ChatColor.LIGHT_PURPLE + city.getName() + ChatColor.AQUA + "...");
        player.sendMessage(ChatColor.GRAY + "Nie ruszaj się przez " + TELEPORT_DELAY + " sekund!");

        Location startLoc = player.getLocation().clone();

        // Tworzymy BukkitRunnable, żeby móc bezpiecznie anulować z zewnątrz
        BukkitRunnable runnable = new BukkitRunnable() {
            int secondsLeft = TELEPORT_DELAY;

            @Override
            public void run() {
                // jeśli gracz offline lub przestał teleportować -> zakończ task
                if (!player.isOnline() || !teleporting.contains(uuid)) {
                    cleanup();
                    return;
                }

                // Sprawdzenie ruchu - tylko znaczący ruch (ignorujemy obrót głowy)
                if (hasMovedSignificantly(startLoc, player.getLocation())) {
                    // przerwij teleportację
                    player.sendMessage(ChatColor.RED + "❌ Teleportacja przerwana – poruszyłeś się!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    cleanup();
                    return;
                }

                if (secondsLeft <= 0) {
                    // zakończ odliczanie i teleportuj
                    cleanup();

                    Location dest = city.getLocation();
                    player.teleport(dest);
                    Component teleport = mm.deserialize("<gradient:#00FF00:#55FF55:#AAFF55:#00AA00>☑ Teleportowano do miasta: </gradient>"
                            + "<gradient:#ff00ff:#00ffff>" + city.getName() + "</gradient>");
                    player.sendMessage(teleport);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                    // ustaw cooldown
                    cooldowns.put(uuid, Long.valueOf(System.currentTimeMillis() + COOLDOWN_MS));
                    player.sendMessage(ChatColor.GRAY + "(Heartstone dostępny ponownie za 30 sekund)");
                    return;
                }

                // ActionBar z odliczaniem
                String legacy = "§eTeleportacja za §6" + secondsLeft + "s §7- §oNie ruszaj się!";
                player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(legacy));

                secondsLeft--;
            }

            private void cleanup() {
                BukkitTask bt = teleportTasks.remove(uuid);
                if (bt != null) bt.cancel();
                teleporting.remove(uuid);
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendActionBar(Component.empty()), 2L);
                this.cancel();
            }
        };

        // uruchom task i zapisz go w mapie
        BukkitTask bt = runnable.runTaskTimer(plugin, 0L, 20L);
        teleportTasks.put(uuid, bt);
    }

    private boolean hasMovedSignificantly(Location from, Location to) {
        if (from == null || to == null) return false;
        if (!Objects.equals(from.getWorld(), to.getWorld())) return true;
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();
        return (dx * dx + dy * dy + dz * dz) > 0.01;
    }

    // Jeśli gracz się ruszy — przerwij teleport (zadziała szybko)
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!teleporting.contains(uuid)) return;

        if (hasMovedSignificantly(e.getFrom(), e.getTo())) {
            // anuluj task i posprzątaj
            BukkitTask bt = teleportTasks.remove(uuid);
            if (bt != null) bt.cancel();
            teleporting.remove(uuid);
            // czyść actionbar
            p.sendActionBar(Component.empty());
            p.sendMessage(ChatColor.RED + "❌ Teleportacja przerwana – poruszyłeś się!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("zjazdownia", "undroppable");

        if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "❌ Nie możesz wyrzucić Heartstone!");
        }
    }

}
