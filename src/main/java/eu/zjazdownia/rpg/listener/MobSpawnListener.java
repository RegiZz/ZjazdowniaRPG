package eu.zjazdownia.rpg.listener;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.cities.City;
import eu.zjazdownia.rpg.level.MobLevel;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import eu.zjazdownia.rpg.commands.CityComands;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class MobSpawnListener implements Listener {
    private final CityComands cityCommands;
    private ZjazdowniaRPG plugin;

    public MobSpawnListener(CityComands cityCommands,  ZjazdowniaRPG plugin) {
        this.cityCommands = cityCommands;
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        LivingEntity mob = e.getEntity();

        if(mob instanceof Animals || mob instanceof Player) {
            return;
        }

        City city = cityCommands.findCityAt(mob.getLocation());
        if (city == null) {
            mob.setCustomName("§7[1]" + " §c" + mob.getType().name());
            mob.setCustomNameVisible(true);
            return;
        };

        int mobLevelValue = city.getMoblvl();

        MobLevel mobLevel = new MobLevel(mob.getType(), mobLevelValue);
        mobLevel.applyTo(mob);

        NamespacedKey key = new NamespacedKey(plugin, "mob_level");
        mob.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, mobLevelValue);


        mob.setCustomName("§7[" + mobLevelValue + "] §c" + mob.getType().name());
        mob.setCustomNameVisible(true);
    }
}
