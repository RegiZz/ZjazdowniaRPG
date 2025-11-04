package eu.zjazdownia.rpg.level;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;
import java.util.UUID;

public class MobLevel {

    private static final UUID MOD_UUID_MOB_ATTACK = UUID.fromString("7b9c7d0c-b0d4-4a5c-8c1b-100000000001");

    private final EntityType type;
    private int level;

    // proste parametry skali
    private double baseHealth;          // bazowe HP
    private double healthPerLevel = 0.2;       // +HP na poziom
    private double baseAttack;           // bazowy atak (2 dmg to 1 serce)
    private double attackPerLevel = 0.15;       // +atak na poziom

    public MobLevel(EntityType type, int level) {
        this.type = Objects.requireNonNull(type, "type");
        setLevel(level);
    }

    public MobLevel(EntityType type) {
        this(type, 1);
    }

    public EntityType getType() { return type; }

    public int getLevel() { return level; }

    public void setLevel(int level) { this.level = Math.max(1, level); }


    /**
     * Zastosuj poziom do podanego moba: ustawia max HP (i aktualne HP proporcjonalnie) oraz obrażenia ataku.
     */
    public void applyTo(LivingEntity mob) {
        if (mob == null) return;

        // Zainicjuj bazowe statystyki z oryginalnego moba
        initBaseStats(mob);

        // HEALTH
        AttributeInstance maxHp = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) {
            double base = baseHealth;
            double targetMax = base * (1.0 + (level - 1) * healthPerLevel);
            double prevMax = maxHp.getBaseValue();
            double current = Math.min(mob.getHealth(), prevMax);
            double percent = prevMax > 0 ? current / prevMax : 1.0;

            maxHp.setBaseValue(targetMax);
            mob.setHealth(Math.max(1.0, targetMax * percent));
        }

        // ATTACK DAMAGE
        AttributeInstance atk = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (atk != null) {
            atk.getModifiers().stream()
                    .filter(m -> m.getUniqueId().equals(MOD_UUID_MOB_ATTACK))
                    .toList()
                    .forEach(atk::removeModifier);

            double base = baseAttack;
            double desired = base * (1.0 + (level - 1) * attackPerLevel);
            double addNumber = Math.max(0.0, desired - atk.getBaseValue());

            if (addNumber != 0.0) {
                AttributeModifier mod = new AttributeModifier(
                        MOD_UUID_MOB_ATTACK,
                        "mob-level-attack",
                        addNumber,
                        AttributeModifier.Operation.ADD_NUMBER
                );
                atk.addModifier(mod);
            }
        }
    }

    private void initBaseStats(LivingEntity mob) {
        if (mob == null) return;

        AttributeInstance hp = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        baseHealth = (hp != null) ? hp.getBaseValue() : 20.0;

        AttributeInstance atk = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        baseAttack = (atk != null) ? atk.getBaseValue() : 2.0;
    }

    public double computeMaxHealth() {
        int lv = Math.max(1, level);
        return Math.max(1.0, baseHealth + (lv - 1) * healthPerLevel);
    }

    public double computeAttackDamage() {
        int lv = Math.max(1, level);
        return Math.max(0.0, baseAttack + (lv - 1) * attackPerLevel);
    }

    @Override
    public String toString() {
        return "MobLevel{type=" + type + ", level=" + level + ", maxHp=" + computeMaxHealth() + ", atk=" + computeAttackDamage() + "}";
    }
}