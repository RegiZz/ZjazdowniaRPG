package eu.zjazdownia.rpg.listener;

/**
 * Listener spawnu mobów: w obrębie miasta ustawia poziom moba (MobLevel) i PDC mob_level;
 * poza miastem ustawia poziom 1 i custom name. Zwierzęta i gracze są pomijane.
 */
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
    private final ZjazdowniaRPG plugin;

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
        int defaultLevel = Math.max(1, plugin.getConfig().getInt("mob-level.default-level-outside-city", 1));
        double healthPerLevel = plugin.getConfig().getDouble("mob-level.health-per-level", 0.2);
        double attackPerLevel = plugin.getConfig().getDouble("mob-level.attack-per-level", 0.15);

        if (city == null) {
            mob.setCustomName("§7[" + defaultLevel + "]" + " §c" + mob.getType().name());
            mob.setCustomNameVisible(true);
            MobLevel mobLevel = new MobLevel(mob.getType(), defaultLevel, healthPerLevel, attackPerLevel);
            mobLevel.applyTo(mob);
            mob.getPersistentDataContainer().set(new NamespacedKey(plugin, "mob_level"), PersistentDataType.INTEGER, defaultLevel);
            return;
        }

        int mobLevelValue = city.getMoblvl();

        MobLevel mobLevel = new MobLevel(mob.getType(), mobLevelValue, healthPerLevel, attackPerLevel);
        mobLevel.applyTo(mob);

        NamespacedKey key = new NamespacedKey(plugin, "mob_level");
        mob.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, mobLevelValue);


        mob.setCustomName("§7[" + mobLevelValue + "] §c" + mob.getType().name());
        mob.setCustomNameVisible(true);
    }
}
