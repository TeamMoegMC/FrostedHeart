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

package com.teammoeg.frostedheart.bootstrap.common;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.effect.BaseEffect;
import com.teammoeg.frostedheart.content.climate.effect.HyperthermiaEffect;
import com.teammoeg.frostedheart.content.climate.effect.HypothermiaEffect;
import com.teammoeg.frostedheart.content.health.effect.AnemiaEffect;
import com.teammoeg.frostedheart.content.health.effect.IonEffect;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaEffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FHMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FHMain.MODID);
    public static final RegistryObject<MobEffect> HYPOTHERMIA = register("hypothermia",()->  new HypothermiaEffect(MobEffectCategory.HARMFUL, 0x57BDE8));
    public static final RegistryObject<MobEffect> HYPERTHERMIA = register("hyperthermia",()->  new HyperthermiaEffect(MobEffectCategory.HARMFUL, 0xFF9800));
    public static final RegistryObject<MobEffect> NYCTALOPIA = register("nyctalopia",()->  new BaseEffect(MobEffectCategory.HARMFUL, 0x787dab) {
    });
    public static final RegistryObject<MobEffect> SCURVY = register("scurvy",()->  new BaseEffect(MobEffectCategory.HARMFUL, 0xc47b34) {
    });
    public static final RegistryObject<MobEffect> ANEMIA = register("anemia",()->  new AnemiaEffect(MobEffectCategory.HARMFUL, 0x571b1c) {
    });
    public static final RegistryObject<MobEffect> ION = register("ionizing_radiation",()->  new IonEffect(MobEffectCategory.NEUTRAL, 0x92cbe5) {
    });
    public static final RegistryObject<MobEffect> WET = register("wet",()->  new BaseEffect(MobEffectCategory.NEUTRAL, 816760296) {
    });
    public static final RegistryObject<MobEffect> SAD = register("lethargic",()->  new BaseEffect(MobEffectCategory.NEUTRAL, 816760296) {
    });
    public static final RegistryObject<MobEffect> SAUNA = register("sauna",()-> new SaunaEffect(MobEffectCategory.BENEFICIAL, 816760296) {
    });
    public static final RegistryObject<MobEffect> THIRST = register("thirst",() -> new BaseEffect(MobEffectCategory.BENEFICIAL, 0xE4D49F));
    public static final RegistryObject<MobEffect> WATER_RESTORING = register("water_restoring",() -> new BaseEffect(MobEffectCategory.BENEFICIAL, 0x379AD6));
    // Insulation: Perform no heat exchange with the environment during body temperature update. Useful for admins and creative mode.
    public static final RegistryObject<MobEffect> INSULATION = register("insulation",() -> new BaseEffect(MobEffectCategory.BENEFICIAL, 0x379AD6));

    public static <T extends MobEffect> RegistryObject<T> register(String name, Supplier<T> effect) {
    	return EFFECTS.register(name, effect);
    }

}
