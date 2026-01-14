package eu.zjazdownia.rpg.classes;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Logger;

public class ClassAbilities implements Listener {

    private final ZjazdowniaRPG plugin;
    private final Logger log;
    private final NamespacedKey MAGE_BOLT_KEY;
    private final NamespacedKey ARCHER_ARROW_KEY;

    // Mapowanie alias -> nazwa kanoniczna
    private final Map<String, String> aliasToCanonical = new HashMap<>();

    // Konfigurowalne wartości (z domyślnymi)
    private double warriorMeleeDamageMultiplier = 1.20;
    private double warriorDamageTakenMultiplier = 0.90;

    private double mageBoltDamage = 6.0;
    private long mageCooldownMs = 5000L;
    private double mageBoltSpeed = 1.4;
    private int mageWeaknessSeconds = 4;
    private int mageSlowSeconds = 3;

    private double archerArrowDamageMultiplier = 1.25;
    private double archerArrowSpeedMultiplier = 1.05;
    private int archerSlowSeconds = 2;
    private int archerSlowAmplifier = 1;

    private final Map<UUID, Long> mageCooldown = new HashMap<>();

    public ClassAbilities(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.MAGE_BOLT_KEY = new NamespacedKey(plugin, "mage_bolt");
        this.ARCHER_ARROW_KEY = new NamespacedKey(plugin, "archer_arrow");
        reloadFromConfig();
    }

    // Wywołaj po plugin.reloadConfig()
    public void reloadFromConfig() {
        FileConfiguration cfg = plugin.getConfig();

// Mapowanie aliasów
        aliasToCanonical.clear();
        loadAliases(cfg, "abilities.mapping.warrior", "warrior", Arrays.asList("wojownik", "warrior"));
        loadAliases(cfg, "abilities.mapping.mage", "mage", Arrays.asList("mag", "mage"));
        loadAliases(cfg, "abilities.mapping.archer", "archer", Arrays.asList("łucznik", "lucznik", "archer"));

// Warrior
        warriorMeleeDamageMultiplier = positiveOrDefault(cfg.getDouble("abilities.warrior.melee-damage-multiplier", 1.20), 1.20, "abilities.warrior.melee-damage-multiplier");
        warriorDamageTakenMultiplier = positiveOrDefault(cfg.getDouble("abilities.warrior.damage-taken-multiplier", 0.90), 0.90, "abilities.warrior.damage-taken-multiplier");

// Mage
        mageBoltDamage = positiveOrDefault(cfg.getDouble("abilities.mage.bolt.damage", 6.0), 6.0, "abilities.mage.bolt.damage");
        double cdSec = cfg.getDouble("abilities.mage.bolt.cooldown-seconds", 5.0);
        if (Double.isNaN(cdSec) || Double.isInfinite(cdSec) || cdSec < 0) {
            log.warning("abilities.mage.bolt.cooldown-seconds jest nieprawidłowe; używam domyślnego 5.0");
            cdSec = 5.0;
        }
        mageCooldownMs = (long) (cdSec * 1000.0);
        mageBoltSpeed = positiveOrDefault(cfg.getDouble("abilities.mage.bolt.speed", 1.4), 1.4, "abilities.mage.bolt.speed");
        mageWeaknessSeconds = nonNegativeOrDefault(cfg.getInt("abilities.mage.bolt.weakness-seconds", 4), 4, "abilities.mage.bolt.weakness-seconds");
        mageSlowSeconds = nonNegativeOrDefault(cfg.getInt("abilities.mage.bolt.slow-seconds", 3), 3, "abilities.mage.bolt.slow-seconds");

// Archer
        archerArrowDamageMultiplier = positiveOrDefault(cfg.getDouble("abilities.archer.arrow-damage-multiplier", 1.25), 1.25, "abilities.archer.arrow-damage-multiplier");
        archerArrowSpeedMultiplier = positiveOrDefault(cfg.getDouble("abilities.archer.arrow-speed-multiplier", 1.05), 1.05, "abilities.archer.arrow-speed-multiplier");
        archerSlowSeconds = nonNegativeOrDefault(cfg.getInt("abilities.archer.slow-seconds", 2), 2, "abilities.archer.slow-seconds");
        archerSlowAmplifier = Math.max(0, cfg.getInt("abilities.archer.slow-amplifier", 1));
    }

    private void loadAliases(FileConfiguration cfg, String path, String canonical, List<String> defaults) {
        List<String> list = cfg.getStringList(path);
        if (list == null || list.isEmpty()) list = defaults;
        for (String s : list) {
            if (s == null) continue;
            aliasToCanonical.put(s.toLowerCase(Locale.ROOT), canonical);
        }
    }

    private double positiveOrDefault(double value, double def, String path) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            log.warning(path + " jest nieprawidłowe; używam domyślnego " + def);
            return def;
        }
        return value;
    }

    private int nonNegativeOrDefault(int value, int def, String path) {
        if (value < 0) {
            log.warning(path + " jest nieprawidłowe; używam domyślnego " + def);
            return def;
        }
        return value;
    }

    private String clsOf(Player p) {
        try {
            return plugin.accounts().getSelectedClass(p.getUniqueId());
        } catch (Throwable t) {
            return null;
        }
    }

    private String canonicalClass(String raw) {
        if (raw == null) return null;
        String key = aliasToCanonical.get(raw.toLowerCase(Locale.ROOT));
        return key != null ? key : raw.toLowerCase(Locale.ROOT);
    }

    public boolean isClass(Player p, String canonical) {
        String c = canonicalClass(clsOf(p));
        return canonical.equals(c);
    }

    // WOJOWNIK: bonus do obrażeń wręcz (teraz uwzględnia level)
    @EventHandler(ignoreCancelled = true)
    public void onMeleeDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isClass(p, "warrior")) return;

        ItemStack hand = p.getInventory().getItemInMainHand();
        Material type = hand == null ? Material.AIR : hand.getType();
        boolean meleeWeapon = type.name().endsWith("_SWORD") || type.name().endsWith("_AXE");
        if (!meleeWeapon) return;

// podstawowy mnożnik z abilities
        double damage = e.getDamage() * warriorMeleeDamageMultiplier;

// flat bonus z levelu (LevelManager musi expose getter getAttackPerLevelWarrior())
        int lvl = plugin.accounts().getLevel(p.getUniqueId());
        double flat = plugin.levels().getAttackPerLevelWarrior() * Math.max(0, lvl - 1);

        damage += flat;

        e.setDamage(damage);

        p.getWorld().spawnParticle(Particle.CRIT, e.getEntity().getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
    }

    // WOJOWNIK: redukcja otrzymywanych obrażeń
    @EventHandler(ignoreCancelled = true)
    public void onWarriorDefense(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!isClass(p, "warrior")) return;

// można rozbudować o defensę zależną od levelu (opcjonalnie)
        e.setDamage(e.getDamage() * warriorDamageTakenMultiplier);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMageCast(PlayerInteractEvent e) {
// reagujemy tylko na main hand
        if (e.getHand() != EquipmentSlot.HAND) return;

        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        if (!isClass(p, "mage")) return;

        ItemStack handItem = p.getInventory().getItemInMainHand();
        Material t = handItem == null ? Material.AIR : handItem.getType();

// wymóg: tylko blaze_rod/stick lub (lepiej) oznaczona różdżka przez PDC
        boolean isWandType = (t == Material.STICK || t == Material.BLAZE_ROD);
        if (!isWandType) return;

// opcjonalnie: wymagaj konkretnej różdżki oznaczonej PDC (bez tego każdy patyk zadziała)
        NamespacedKey wandKey = new NamespacedKey(plugin, "mage_wand");
        if (handItem == null || handItem.getItemMeta() == null ||
                !handItem.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE)) {
// jeśli chcesz, by każdy patyk/blaze_rod działał, usuń tę sekcję
            return;
        }

        long now = System.currentTimeMillis();
        long next = mageCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now < next) {
            long left = (next - now + 999) / 1000;
            p.sendMessage(ChatColor.RED + "Umiejętność jeszcze się odświeża (" + left + "s).");
            p.playSound(p, Sound.UI_BUTTON_CLICK, 0.7f, 1.5f);
            return;
        }

// użyj kierunku z oczu — dokładniejsze celowanie
        Vector dir = p.getEyeLocation().getDirection().normalize().multiply(mageBoltSpeed);
        Snowball bolt = p.launchProjectile(Snowball.class);
        bolt.setVelocity(dir);

// oznacz pocisk, aby rozpoznać go później
        bolt.getPersistentDataContainer().set(MAGE_BOLT_KEY, PersistentDataType.BYTE, (byte) 1);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.2f);
        p.getWorld().spawnParticle(Particle.SPELL_WITCH, p.getEyeLocation(), 20, 0.3, 0.3, 0.3, 0.01);

        mageCooldown.put(p.getUniqueId(), now + mageCooldownMs);
    }

    // MAG: trafienie pociskiem (uwzględnia level)
    @EventHandler(ignoreCancelled = true)
    public void onMageBoltHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Snowball sb)) return;
        if (!(sb.getShooter() instanceof Player p)) return;
        if (!sb.getPersistentDataContainer().has(MAGE_BOLT_KEY, PersistentDataType.BYTE)) return;
        if (!isClass(p, "mage")) return;
        if (!(e.getEntity() instanceof LivingEntity le)) return;

        int lvl = plugin.accounts().getLevel(p.getUniqueId());
        double flat = plugin.levels().getAttackPerLevelMage() * Math.max(0, lvl - 1);

        e.setDamage(mageBoltDamage + flat);

        if (mageWeaknessSeconds > 0) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, mageWeaknessSeconds * 20, 0, true, true, true));
        }
        if (mageSlowSeconds > 0) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, mageSlowSeconds * 20, 0, true, true, true));
        }
        le.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, le.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        le.getWorld().playSound(le.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8f, 1.6f);
    }

    // ŁUCZNIK: oznacz strzały i lekko je przyspiesz
    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!isClass(p, "archer")) return;
        if (!(e.getProjectile() instanceof Projectile proj)) return;

        proj.getPersistentDataContainer().set(ARCHER_ARROW_KEY, PersistentDataType.BYTE, (byte) 1);
        Vector v = proj.getVelocity();
        proj.setVelocity(v.multiply(archerArrowSpeedMultiplier));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);
    }

    // ŁUCZNIK: dodatkowe obrażenia i spowolnienie przy trafieniu (uwzględnia level)
    @EventHandler(ignoreCancelled = true)
    public void onArrowDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Arrow a)) return;
        if (!(a.getShooter() instanceof Player p)) return;
        if (!isClass(p, "archer")) return;

        int lvl = plugin.accounts().getLevel(p.getUniqueId());

// mnożnik z abilities oraz mnożnik procentowy z poziomu (LevelManager.arrowDamageMultiplierPerLevel)
        double arrowLevelMult = 1.0 + (plugin.levels().getArrowDamageMultiplierPerLevel() * Math.max(0, lvl - 1));

// dodatkowy flat bonus z poziomu (attackPerLevelArcher)
        double flat = plugin.levels().getAttackPerLevelArcher() * Math.max(0, lvl - 1);

        e.setDamage(e.getDamage() * archerArrowDamageMultiplier * arrowLevelMult + flat);

        if (e.getEntity() instanceof LivingEntity le) {
            if (archerSlowSeconds > 0) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, archerSlowSeconds * 20, Math.max(0, archerSlowAmplifier), true, true, true));
            }
            le.getWorld().spawnParticle(Particle.CRIT_MAGIC, le.getLocation().add(0, 1, 0), 12, 0.2, 0.2, 0.2, 0.05);
        }
    }
}