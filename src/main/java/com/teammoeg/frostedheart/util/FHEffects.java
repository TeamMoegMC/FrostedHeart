/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.HyperthermiaEffect;
import com.teammoeg.frostedheart.climate.HypothermiaEffect;
import com.teammoeg.frostedheart.climate.WetEffect;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

public class FHEffects {
    public static List<Effect> EFFECTS = new ArrayList<Effect>();

    public static final Effect HYPOTHERMIA = register("hypothermia", new HypothermiaEffect(EffectType.HARMFUL, 5750248));
    public static final Effect HYPERTHERMIA = register("hyperthermia", new HyperthermiaEffect(EffectType.HARMFUL, 16750592));
    public static final Effect WET = register("wet", new WetEffect(EffectType.NEUTRAL, 816760296));

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

}
