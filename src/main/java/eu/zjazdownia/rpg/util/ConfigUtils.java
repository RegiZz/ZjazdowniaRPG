package eu.zjazdownia.rpg.util;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

public class ConfigUtils {
    public static void migrateAndFillDefaults(FileConfiguration c) {
        c.addDefault("server.ip", "zjazdownia.eu");
        c.addDefault("gui.account.title", "&8Wybierz konto");
        c.addDefault("gui.account.free.icon", "LIME_STAINED_GLASS_PANE");
        c.addDefault("gui.account.free.name", "&aKonto #1 &7(&fDarmowe&7)");
        c.addDefault("gui.account.free.lore", List.of("&7Kliknij, aby wybrać."));
        c.addDefault("gui.account.paid.icon_locked", "RED_STAINED_GLASS_PANE");
        c.addDefault("gui.account.paid.name_locked", "&cKonto #2 &8(&7Zablokowane&8)");
        c.addDefault("gui.account.paid.lore_locked", List.of("&7Odblokuj to konto po &6zakupie&7."));
        c.addDefault("gui.account.paid.icon_unlocked", "YELLOW_STAINED_GLASS_PANE");
        c.addDefault("gui.account.paid.name_unlocked", "&eKonto #2 &7(&aOdblokowane&7)");
        c.addDefault("gui.account.paid.lore_unlocked", List.of("&7Kliknij, aby wybrać."));
        c.addDefault("gui.class.title", "&8Wybierz klasę");
        c.addDefault("classes.warrior.display", "&cWojownik");
        c.addDefault("classes.warrior.short", "&cWoj");
        c.addDefault("classes.warrior.icon", "IRON_SWORD");
        c.addDefault("classes.warrior.lore", List.of("&7Mistrz miecza.", "&7Wytrzymały, walka w zwarciu."));
        c.addDefault("classes.warrior.spawn.world", "world");
        c.addDefault("classes.warrior.spawn.x", 0);
        c.addDefault("classes.warrior.spawn.y", 64);
        c.addDefault("classes.warrior.spawn.z", 0);
        c.addDefault("classes.warrior.spawn.yaw", 0);
        c.addDefault("classes.warrior.spawn.pitch", 0);
        c.addDefault("classes.mage.display", "&bMag");
        c.addDefault("classes.mage.short", "&bMag");
        c.addDefault("classes.mage.icon", "BLAZE_ROD");
        c.addDefault("classes.mage.lore", List.of("&7Władca żywiołów.", "&7Wysokie obrażenia, niska obrona."));
        c.addDefault("classes.mage.spawn.world", "world");
        c.addDefault("classes.mage.spawn.x", 10);
        c.addDefault("classes.mage.spawn.y", 64);
        c.addDefault("classes.mage.spawn.z", 0);
        c.addDefault("classes.mage.spawn.yaw", 90);
        c.addDefault("classes.mage.spawn.pitch", 0);
        c.addDefault("classes.archer.display", "&aŁucznik");
        c.addDefault("classes.archer.short", "&aŁucz");
        c.addDefault("classes.archer.icon", "BOW");
        c.addDefault("classes.archer.lore", List.of("&7Z dystansu zawsze w punkt.", "&7Zwinny i precyzyjny."));
        c.addDefault("classes.archer.spawn.world", "world");
        c.addDefault("classes.archer.spawn.x", -10);
        c.addDefault("classes.archer.spawn.y", 64);
        c.addDefault("classes.archer.spawn.z", 0);
        c.addDefault("classes.archer.spawn.yaw", -90);
        c.addDefault("classes.archer.spawn.pitch", 0);
        c.addDefault("scoreboard.title", "&6&lZJAZDOWNIA &7| &f%ip%");
        c.addDefault("scoreboard.lines", List.of(
                "&8-------------------",
                "&7Nick: &f%player%",
                "&7Klasa: &f%class%",
                "&7Poziom: &f%level%",
                "&8-------------------",
                "&7IP: &f%ip%"
        ));
        c.addDefault("firstSpawn.world", "world");
        c.addDefault("firstSpawn.x", 100.5);
        c.addDefault("firstSpawn.y", 10);
        c.addDefault("firstSpawn.z", 730.2);
        c.addDefault("firstSpawn.yaw", 0);
        c.addDefault("firstSpawn.pitch", 0);

// abilities (jeśli nie masz już w configu)
        c.addDefault("abilities.warrior.melee-damage-multiplier", 1.20);
        c.addDefault("abilities.warrior.damage-taken-multiplier", 0.90);
        c.addDefault("abilities.mage.bolt.damage", 6.0);
        c.addDefault("abilities.mage.bolt.cooldown-seconds", 5.0);
        c.addDefault("abilities.mage.bolt.speed", 1.4);
        c.addDefault("abilities.mage.bolt.weakness-seconds", 4);
        c.addDefault("abilities.mage.bolt.slow-seconds", 3);
        c.addDefault("abilities.archer.arrow-damage-multiplier", 1.25);
        c.addDefault("abilities.archer.arrow-speed-multiplier", 1.05);
        c.addDefault("abilities.archer.slow-seconds", 2);
        c.addDefault("abilities.archer.slow-amplifier", 1);

// NOWE: leveling defaults
        c.addDefault("leveling.base-exp", 100);
        c.addDefault("leveling.step-exp", 50);
        c.addDefault("leveling.max-level", 50);
        c.addDefault("leveling.health-per-level", 1.0);
// przykładowe xp za moby (klucze muszą odpowiadać EntityType)
        c.addDefault("leveling.mob-xp.ZOMBIE", 15);
        c.addDefault("leveling.mob-xp.SKELETON", 18);
        c.addDefault("leveling.mob-xp.SPIDER", 12);
        c.addDefault("leveling.mob-xp.CREEPER", 22);
        c.addDefault("leveling.mob-xp.ENDERMAN", 35);
        c.addDefault("leveling.mob-xp.PLAYER", 30);
// nagrody/efekty przy awansie - format EFFECT:durationTicks:amplifier
        c.addDefault("leveling.rewards.warrior.on-levelup-effects", List.of(
                "INCREASE_DAMAGE:200:0" // Siła I przez 10s (200 ticków)
        ));
        c.addDefault("leveling.rewards.mage.on-levelup-effects", List.of(
                "REGENERATION:160:0" // Regen I przez 8s
        ));
        c.addDefault("leveling.rewards.archer.on-levelup-effects", List.of(
                "SPEED:200:1" // Szybkość II przez 10s
        ));

        c.addDefault("leveling.bonuses.attack-per-level.warrior", 0.5);
        c.addDefault("leveling.bonuses.attack-per-level.mage", 0.2);
        c.addDefault("leveling.bonuses.attack-per-level.archer", 0.3);
        c.addDefault("leveling.bonuses.arrow-damage-multiplier-per-level", 0.05);

// kopiuj domyślne bez nadpisywania istniejącego configu
        c.options().copyDefaults(true);
    }
}