/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FHMain.MODID);
    public static final RegistryObject<SoundEvent> MC_BELL = SOUNDS.register("mc_bell", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FHMain.MODID, "mc_bell")));
    public static final RegistryObject<SoundEvent> MC_ROLL = SOUNDS.register("mc_roll", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FHMain.MODID, "mc_roll")));
    public static final RegistryObject<SoundEvent> ICE_CRACKING = SOUNDS.register("ice_cracking", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FHMain.MODID, "ice_cracking")));
    public FHSoundEvents() {
    }

}
