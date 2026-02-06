/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.registrate;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.infrastructure.gen.FHLootGen;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;
@Mixin(RegistrateBlockLootTables.class)
public class RegistrateBlockLootTablesMixin_RemoveExisted extends VanillaBlockLoot {
	@Shadow(remap=false)
    private AbstractRegistrate<?> parent;
	/**
	 * @author khjxiaogu
	 * @reason Remove warning when we explicitly define the loot table is provided
	 * */

    @Overwrite(remap=false)
    protected Iterable<Block> getKnownBlocks() {
        return parent.getAll(Registries.BLOCK).stream().map(Supplier::get).filter(o->!FHLootGen.existedBlocks.contains(o)).collect(Collectors.toList());
    }
}
