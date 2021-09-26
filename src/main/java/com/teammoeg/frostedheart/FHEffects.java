package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.climate.HyperthermiaEffect;
import com.teammoeg.frostedheart.climate.HypothermiaEffect;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.DamageSource;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

public class FHEffects {
    public static List<Effect> EFFECTS = new ArrayList<Effect>();

    public static final Effect HYPOTHERMIA = register("hypothermia", new HypothermiaEffect(EffectType.HARMFUL, 5750248));
    public static final Effect HYPERTHERMIA = register("hyperthermia", new HyperthermiaEffect(EffectType.HARMFUL, 16750592));

    public static void registerAll(IForgeRegistry<Effect> registry) {
        for (Effect effect : EFFECTS) {
            registry.register(effect);
        }
    }

    public static Effect register(String name, Effect effect) {
        effect.setRegistryName(FHMain.rl(name));
        EFFECTS.add(effect);
        return effect;
    }

    public static class FHDamageSources {
        public static final DamageSource HYPOTHERMIA = (new DamageSource("hypothermia")).setDamageBypassesArmor().setDifficultyScaled();
        public static final DamageSource HYPERTHERMIA = (new DamageSource("hyperthermia")).setDamageBypassesArmor().setDifficultyScaled();
    }
}
