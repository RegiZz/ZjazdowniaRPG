package eu.zjazdownia.rpg.level;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;

public class LevelManager {
    private final ZjazdowniaRPG plugin;
    // Konfig
    private int baseExp;
    private int stepExp;
    public int maxLevel;
    private double healthPerLevel;

    // bonusy ataku
    private double attackPerLevelWarrior = 0.5; // flat damage per level
    private double attackPerLevelMage = 0.2;
    private double attackPerLevelArcher = 0.3;
    private double arrowDamageMultiplierPerLevel = 0.05; // 5% per level

    private final Map<EntityType, Integer> mobXp = new EnumMap<>(EntityType.class);
    private final Map<String, List<EffectSpec>> onLevelupEffects = new HashMap<>();

    // stałe UUIDy do identyfikacji modifierów (jedno UUID na klasę)
    private static final UUID MOD_UUID_WARRIOR = UUID.fromString("2b6f3b3a-1f7b-4a3d-9b8d-111111111111");
    private static final UUID MOD_UUID_MAGE = UUID.fromString("2b6f3b3a-1f7b-4a3d-9b8d-222222222222");
    private static final UUID MOD_UUID_ARCHER = UUID.fromString("2b6f3b3a-1f7b-4a3d-9b8d-333333333333");

    public LevelManager(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
        reloadConfigValues();
    }

    public void reloadConfigValues() {
        FileConfiguration cfg = plugin.getConfig();

        baseExp = Math.max(1, cfg.getInt("leveling.base-exp", 100));
        stepExp = Math.max(0, cfg.getInt("leveling.step-exp", 50));
        maxLevel = Math.max(1, cfg.getInt("leveling.max-level", 50));
        healthPerLevel = Math.max(0.0, cfg.getDouble("leveling.health-per-level", 1.0));

        attackPerLevelWarrior = cfg.getDouble("leveling.bonuses.attack-per-level.warrior", 0.5);
        attackPerLevelMage = cfg.getDouble("leveling.bonuses.attack-per-level.mage", 0.2);
        attackPerLevelArcher = cfg.getDouble("leveling.bonuses.attack-per-level.archer", 0.3);
        arrowDamageMultiplierPerLevel = cfg.getDouble("leveling.bonuses.arrow-damage-multiplier-per-level", 0.05);

        mobXp.clear();
        if (cfg.isConfigurationSection("leveling.mob-xp")) {
            Set<String> keys = cfg.getConfigurationSection("leveling.mob-xp").getKeys(false);
            for (String k : keys) {
                Object raw = cfg.get("leveling.mob-xp." + k);
                int val;
                try {
                    val = cfg.getInt("leveling.mob-xp." + k);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Nie można sparsować wartości leveling.mob-xp." + k + "; pomijam", ex);
                    continue;
                }

                String normalized = k.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
                EntityType matched = null;
                try {
                    matched = EntityType.valueOf(normalized);
                } catch (IllegalArgumentException iae) {
                    for (EntityType et : EntityType.values()) {
                        if (et.name().equalsIgnoreCase(normalized) ||
                                et.name().replace('_', ' ').equalsIgnoreCase(normalized.replace('_', ' '))) {
                            matched = et;
                            break;
                        }
                    }
                }

                if (matched != null) {
                    mobXp.put(matched, Math.max(0, val));
                    plugin.getLogger().info("Mapped leveling.mob-xp." + k + " -> " + matched + " = " + val);
                } else {
                    plugin.getLogger().warning("leveling.mob-xp zawiera nieprawidłowy klucz: '" + k + "'");
                }
            }
        } else {
            plugin.getLogger().warning("Brak sekcji leveling.mob-xp w config.yml");
        }

        onLevelupEffects.clear();
        loadEffectsFor("warrior");
        loadEffectsFor("mage");
        loadEffectsFor("archer");

        plugin.getLogger().info("Leveling config: baseExp=" + baseExp + ", stepExp=" + stepExp + ", maxLevel=" + maxLevel + ", healthPerLevel=" + healthPerLevel);
        plugin.getLogger().info("Attack per level: warrior=" + attackPerLevelWarrior + ", mage=" + attackPerLevelMage + ", archer=" + attackPerLevelArcher + ", arrowMultPerLevel=" + arrowDamageMultiplierPerLevel);
    }

    private void loadEffectsFor(String canonical) {
        List<String> list = plugin.getConfig().getStringList("leveling.rewards." + canonical + ".on-levelup-effects");
        List<EffectSpec> specs = new ArrayList<>();
        if (list != null) {
            for (String s : list) {
                EffectSpec spec = EffectSpec.parse(s);
                if (spec != null) specs.add(spec);
                else plugin.getLogger().warning("Niepoprawny efekt w leveling.rewards." + canonical + ": '" + s + "'");
            }
        }
        onLevelupEffects.put(canonical, specs);
    }

    public int requiredExpFor(int level) {
        return baseExp + (Math.max(1, level) - 1) * stepExp;
    }

    public int xpForMob(EntityType type) {
        return mobXp.getOrDefault(type, 0);
    }

    public double getAttackPerLevelWarrior() { return attackPerLevelWarrior; }
    public double getAttackPerLevelMage() { return attackPerLevelMage; }
    public double getAttackPerLevelArcher() { return attackPerLevelArcher; }
    public double getArrowDamageMultiplierPerLevel() { return arrowDamageMultiplierPerLevel; }

    public void addExp(Player p, int amount) {
        if (amount <= 0) return;

        var am = plugin.accounts();
        var uuid = p.getUniqueId();

        int level = am.getLevel(uuid);
        int exp = am.getExp(uuid);


        int cap = Math.max(1, maxLevel);
        boolean leveledUp = false;
        exp += amount;

        while (level < cap) {
            int req = requiredExpFor(level);
            if (exp >= req) {
                exp -= req;
                level++;
                leveledUp = true;
                plugin.getLogger().info("Player " + p.getName() + " leveled up to " + level + " (left exp=" + exp + ")");
                applyLevelup(p, level);
            } else break;
        }

        am.setLevel(uuid, level);
        am.setExp(uuid, exp);
        am.flush(uuid);

        if (leveledUp) {
            refreshAttributes(p);
        }
    }

    private void applyLevelup(Player p, int newLevel) {
        String raw = plugin.accounts().getSelectedClass(p.getUniqueId());
        String canonical = canonicalClass(raw);

        List<EffectSpec> list = onLevelupEffects.getOrDefault(canonical, Collections.emptyList());
        for (EffectSpec spec : list) {
            try {
                p.addPotionEffect(new PotionEffect(spec.type, spec.durationTicks, spec.amplifier, true, true, true));
            } catch (Exception ex) {
                plugin.getLogger().warning("Nie można dodać efektu przy levelup dla " + p.getName() + ": " + ex.getMessage());
            }
        }

        p.sendTitle("§6Poziom §e" + newLevel, "§aGratulacje!", 10, 40, 10);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.1f);
    }

    public void refreshAttributes(Player p) {
        int lvl = plugin.accounts().getLevel(p.getUniqueId());
        double base = 20.0; // 10 serc
        double bonus = Math.max(0.0, healthPerLevel) * Math.max(0, (lvl - 1));
        double targetHealth = base + bonus;

        // HEALTH
        if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            double prevMax = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double currentHealth = Math.min(p.getHealth(), prevMax);
            double percent = prevMax > 0 ? currentHealth / prevMax : 1.0;
            p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(targetHealth);
            p.setHealth(Math.max(1.0, Math.min(targetHealth, targetHealth * percent)));
        }

        // ATTACK DAMAGE - usuń poprzednie modifiery i dodaj nowy zgodny z klasą i poziomem
        String raw = plugin.accounts().getSelectedClass(p.getUniqueId());
        String canonical = canonicalClass(raw);
        double attackBonus = 0.0;
        UUID useUuid = null;
        String modName = "lvl-attack-bonus";

        if ("warrior".equals(canonical)) {
            attackBonus = attackPerLevelWarrior * Math.max(0, (lvl - 1));
            useUuid = MOD_UUID_WARRIOR;
            modName = "warrior-level-attack";
        } else if ("mage".equals(canonical)) {
            attackBonus = attackPerLevelMage * Math.max(0, (lvl - 1));
            useUuid = MOD_UUID_MAGE;
            modName = "mage-level-attack";
        } else if ("archer".equals(canonical)) {
            attackBonus = attackPerLevelArcher * Math.max(0, (lvl - 1));
            useUuid = MOD_UUID_ARCHER;
            modName = "archer-level-attack";
        }

        if (p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            try {
                var attr = p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (attr.getModifiers() != null) {
                    attr.getModifiers().stream()
                            .filter(m -> m.getUniqueId().equals(MOD_UUID_WARRIOR)
                                    || m.getUniqueId().equals(MOD_UUID_MAGE)
                                    || m.getUniqueId().equals(MOD_UUID_ARCHER))
                            .toList()
                            .forEach(attr::removeModifier);
                }

                if (useUuid != null && attackBonus != 0.0) {
                    AttributeModifier mod = new AttributeModifier(useUuid, modName, attackBonus, AttributeModifier.Operation.ADD_NUMBER);
                    p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addModifier(mod);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Błąd przy ustawianiu attack modifier dla " + p.getName() + ": " + ex.getMessage());
            }
        }
    }

    private String canonicalClass(String raw) {
        if (raw == null) return "";
        String r = raw.toLowerCase(Locale.ROOT);
        if (r.equals("wojownik") || r.equals("warrior")) return "warrior";
        if (r.equals("mag") || r.equals("mage")) return "mage";
        if (r.equals("łucznik") || r.equals("lucznik") || r.equals("archer")) return "archer";
        return r;
    }

    private static class EffectSpec {
        final PotionEffectType type;
        final int durationTicks;
        final int amplifier;

        private EffectSpec(PotionEffectType type, int durationTicks, int amplifier) {
            this.type = type;
            this.durationTicks = durationTicks;
            this.amplifier = amplifier;
        }

        static EffectSpec parse(String s) {
            if (s == null || s.isEmpty()) return null;
            String[] parts = s.split(":");
            try {
                PotionEffectType t = PotionEffectType.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
                if (t == null) return null;
                int dur = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 200;
                int amp = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
                return new EffectSpec(t, Math.max(1, dur), Math.max(0, amp));
            } catch (Exception e) {
                return null;
            }
        }
    }
}
