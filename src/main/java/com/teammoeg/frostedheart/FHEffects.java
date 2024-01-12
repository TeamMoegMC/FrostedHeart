/*
 * Copyright (c) 2021-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.effects.AnemiaEffect;
import com.teammoeg.frostedheart.effects.BaseEffect;
import com.teammoeg.frostedheart.effects.HyperthermiaEffect;
import com.teammoeg.frostedheart.effects.HypothermiaEffect;
import com.teammoeg.frostedheart.effects.IonEffect;
import com.teammoeg.frostedheart.effects.SaunaEffect;

import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraftforge.registries.IForgeRegistry;

public class FHEffects {
    public static List<Effect> EFFECTS = new ArrayList<Effect>();

    public static final Effect HYPOTHERMIA = register("hypothermia", new HypothermiaEffect(EffectType.HARMFUL, 0x57BDE8));
    public static final Effect HYPERTHERMIA = register("hyperthermia", new HyperthermiaEffect(EffectType.HARMFUL, 0xFF9800));
    public static final Effect NYCTALOPIA = register("nyctalopia", new BaseEffect(EffectType.HARMFUL, 0x787dab) {
    });
    public static final Effect SCURVY = register("scurvy", new BaseEffect(EffectType.HARMFUL, 0xc47b34) {
    });
    public static final Effect ANEMIA = register("anemia", new AnemiaEffect(EffectType.HARMFUL, 0x571b1c) {
    });
    public static final Effect ION = register("ionizing_radiation", new IonEffect(EffectType.NEUTRAL, 0x92cbe5) {
    });
    public static final Effect WET = register("wet", new BaseEffect(EffectType.NEUTRAL, 816760296) {
    });
    public static final Effect SAD = register("lethargic", new BaseEffect(EffectType.NEUTRAL, 816760296) {
    });
    public static final Effect SAUNA = register("sauna", new SaunaEffect(EffectType.BENEFICIAL, 816760296) {
    });

    public static Effect register(String name, Effect effect) {
        effect.setRegistryName(FHMain.rl(name));
        EFFECTS.add(effect);
        return effect;
    }

    public static void registerAll(IForgeRegistry<Effect> registry) {
        for (Effect effect : EFFECTS) {
            registry.register(effect);
        }
    }

}
