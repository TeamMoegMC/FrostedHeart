package com.teammoeg.frostedheart.base.item.rankine.alloys;

import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.List;

public class AlloyModifier {

    private final String name;
    private final ModifierType type;
    private final float value;
    private final ModifierCondition condition;

    private final static List<ModifierType> toolStats = Arrays.asList(ModifierType.DURABILITY, ModifierType.ENCHANTABILITY, ModifierType.MINING_SPEED, ModifierType.HARVEST_LEVEL,
            ModifierType.ATTACK_SPEED, ModifierType.ATTACK_DAMAGE, ModifierType.CORROSION_RESISTANCE, ModifierType.HEAT_RESISTANCE, ModifierType.TOUGHNESS);
    private final static List<ModifierType> otherStats = Arrays.asList(ModifierType.DURABILITY, ModifierType.ENCHANTABILITY, ModifierType.HARVEST_LEVEL,
            ModifierType.CORROSION_RESISTANCE, ModifierType.HEAT_RESISTANCE, ModifierType.TOUGHNESS);

        public AlloyModifier(String name, ModifierType type, ModifierCondition condition, float value) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.condition = condition;
        }

    public AlloyModifier(String name, String type, String condition, float value) {
        this.name = name;
        this.type = ModifierType.valueOf(type);
        this.value = value;
        this.condition = ModifierCondition.valueOf(condition);
    }

        public String getName() {
            return this.name;
        }


        public ModifierType getType() {
            return this.type;
        }

        public ModifierCondition getCondition() {
            return condition;
        }

        public float getValue() {
                return this.value;
            }

        public boolean canApplyModification(Item item) {
            if (item instanceof IAlloySpecialItem) {
                return ((IAlloySpecialItem) item).getDefaultStats().contains(this.getType());
            } else {
                return false;
            }
        }

        public float returnModification(float original, boolean shouldRound) {
            float val = original;
            switch (this.getCondition()) {
                case ADDITIVE:
                    val = original + this.value;
                    break;
                case MAX:
                    val = Math.max(original,this.value);
                    break;
                case MIN:
                    val = Math.min(original,this.value);
                    break;
                case MULTIPLICATIVE:
                    val = original * this.value;
                    break;
            }
            return shouldRound ? Math.round(val) : val;
        }

        public float returnModification (float original) {
            return returnModification(original,false);
    }


    public enum ModifierType {
        DURABILITY,
        ENCHANTABILITY,
        MINING_SPEED,
        HARVEST_LEVEL,
        ATTACK_DAMAGE,
        ATTACK_SPEED,
        CORROSION_RESISTANCE,
        HEAT_RESISTANCE,
        KNOCKBACK_RESISTANCE,
        TOUGHNESS
    }

    public enum ModifierCondition {
        ADDITIVE,
        MULTIPLICATIVE,
        MIN,
        MAX
    }
}
