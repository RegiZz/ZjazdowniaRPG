package eu.zjazdownia.rpg.util;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

/**
 * Migracja i uzupełnianie domyślnych wartości konfiguracji pluginu (GUI konta/klasy,
 * scoreboard, firstSpawn, leveling, abilities). Wywoływane przy starcie pluginu.
 */
public class ConfigUtils {
    /** Dodaje domyślne klucze do konfiguracji i włącza copyDefaults. */
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

        c.addDefault("leveling.base-exp", 100);
        c.addDefault("leveling.step-exp", 50);
        c.addDefault("leveling.max-level", 50);
        c.addDefault("leveling.health-per-level", 1.0);
        c.addDefault("leveling.mob-xp.ZOMBIE", 15);
        c.addDefault("leveling.mob-xp.SKELETON", 18);
        c.addDefault("leveling.mob-xp.SPIDER", 12);
        c.addDefault("leveling.mob-xp.CREEPER", 22);
        c.addDefault("leveling.mob-xp.ENDERMAN", 35);
        c.addDefault("leveling.mob-xp.PLAYER", 30);
        c.addDefault("leveling.rewards.warrior.on-levelup-effects", List.of(
                "INCREASE_DAMAGE:200:0"
        ));
        c.addDefault("leveling.rewards.mage.on-levelup-effects", List.of(
                "REGENERATION:160:0"
        ));
        c.addDefault("leveling.rewards.archer.on-levelup-effects", List.of(
                "SPEED:200:1"
        ));

        c.addDefault("leveling.bonuses.attack-per-level.warrior", 0.5);
        c.addDefault("leveling.bonuses.attack-per-level.mage", 0.2);
        c.addDefault("leveling.bonuses.attack-per-level.archer", 0.3);
        c.addDefault("leveling.bonuses.arrow-damage-multiplier-per-level", 0.05);
        c.addDefault("leveling.damage-timeout-ms", 8000);
        c.addDefault("leveling.party-radius-blocks", 70);
        c.addDefault("leveling.party-xp-bonus", 5);
        c.addDefault("leveling.mob-xp-multiplier-per-level", 0.25);

        c.addDefault("account.players-dir", "players");

        c.addDefault("heartstone.cooldown-ms", 30000);
        c.addDefault("heartstone.teleport-delay-seconds", 10);

        c.addDefault("lightning-wand.cooldown-ms", 50000);
        c.addDefault("lightning-wand.lightning-count", 4);
        c.addDefault("lightning-wand.base-damage", 14.0);
        c.addDefault("lightning-wand.radius", 2.5);
        c.addDefault("lightning-wand.spread", 4.5);
        c.addDefault("lightning-wand.raytrace-range", 100);

        c.addDefault("ricochet-bow.default-jumps", 4);
        c.addDefault("ricochet-bow.search-range", 10);
        c.addDefault("ricochet-bow.damage-multiplier-per-bounce", 0.9);
        c.addDefault("ricochet-bow.arrow-speed-multiplier", 2.5);

        c.addDefault("player-in-city.message-cooldown-ms", 3000);

        c.addDefault("cities.showborder-interval-ticks", 10);

        c.addDefault("scoreboard.update-interval-ticks", 40);
        c.addDefault("scoreboard.rotation-interval-ticks", 300);

        c.addDefault("mob-level.default-level-outside-city", 1);
        c.addDefault("mob-level.health-per-level", 0.2);
        c.addDefault("mob-level.attack-per-level", 0.15);

        c.addDefault("nlogin.gui-open-delay-ticks", 20);

        c.options().copyDefaults(true);
    }
}