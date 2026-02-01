package eu.zjazdownia.rpg.listener;

import eu.zjazdownia.rpg.cities.City;
import eu.zjazdownia.rpg.commands.CityComands;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class PlayerInCityListener implements Listener {

    private final CityComands cityCommands;
    private final Map<UUID, City> playerCities = new HashMap<>();
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();

    public static final long MESSAGE_COOLDOWN = 3000; // w milisekundach

    public PlayerInCityListener(CityComands cityCommands) {
        this.cityCommands = cityCommands;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
                e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        City currentCity = cityCommands.findCityAt(p.getLocation());
        City lastCity = playerCities.get(p.getUniqueId());

        long now = System.currentTimeMillis();
        long lastMsg = lastMessageTime.getOrDefault(p.getUniqueId(), 0L);

        if (now - lastMsg < MESSAGE_COOLDOWN) {
            return;
        }

        // wszedł do nowego miasta
        if (lastCity == null && currentCity != null) {
            showEnterMessage(p, currentCity);
            playerCities.put(p.getUniqueId(), currentCity);
            lastMessageTime.put(p.getUniqueId(), now);
        }

        // wyszedł z miasta — nic nie pokazujemy
        else if (lastCity != null && currentCity == null) {
            playerCities.remove(p.getUniqueId());
        }

        // przeszedł z jednego miasta do innego
        else if (lastCity != null && currentCity != null && !lastCity.equals(currentCity)) {
            showEnterMessage(p, currentCity);
            playerCities.put(p.getUniqueId(), currentCity);
            lastMessageTime.put(p.getUniqueId(), now);
        }
    }

    private void showEnterMessage(Player p, City city) {
        p.sendTitle("§aWitaj w mieście §e" + city.getName() + "!", "", 10, 60, 10);
        p.playSound(p.getLocation(), "block.ender_chest.open", 1.0f, 1.0f);
    }
}
