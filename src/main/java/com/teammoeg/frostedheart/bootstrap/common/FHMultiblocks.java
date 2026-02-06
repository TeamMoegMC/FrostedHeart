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

package com.teammoeg.frostedheart.bootstrap.common;

import java.util.function.Function;

import com.teammoeg.chorda.block.CActiveMultiblockBlock;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorMultiblock;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorMultiblock;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.radiator.RadiatorLogic;
import com.teammoeg.frostedheart.content.climate.block.radiator.RadiatorMultiblock;
import com.teammoeg.frostedheart.content.climate.block.radiator.RadiatorState;
import com.teammoeg.frostedheart.content.robotics.logistics.core.LogisticCoreMultiblock;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockItem;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.IEMultiblockBuilder;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.NonMirrorableWithActiveBlock;
import blusunrize.immersiveengineering.common.register.IEBlocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
public class FHMultiblocks {
	public static class Registration{
		public static final MultiblockRegistration<T1GeneratorState> GENERATOR_T1 = stone(new T1GeneratorLogic(), "generator_t1",false,t->t.lightLevel(bs->bs.getValue(NonMirrorableWithActiveBlock.ACTIVE)?15:0))
			.structure(() -> FHMultiblocks.GENERATOR_T1)
			.notMirrored()
			.component(FHMenuTypes.GENERATOR_T1.createComponent())
			.build();
		public static final MultiblockRegistration<T2GeneratorState> GENERATOR_T2 = metal(new T2GeneratorLogic(), "generator_t2",t->t.lightLevel(bs->bs.getValue(NonMirrorableWithActiveBlock.ACTIVE)?15:0))
			.structure(() -> FHMultiblocks.GENERATOR_T2)
			.notMirrored()
			.redstoneAware()
			.component(FHMenuTypes.GENERATOR_T2.createComponent())
			.build();
		public static final MultiblockRegistration<RadiatorState> RADIATOR = metal(new RadiatorLogic(), "radiator",t->t.lightLevel(bs->bs.getValue(NonMirrorableWithActiveBlock.ACTIVE)?15:0))
			.structure(() -> FHMultiblocks.RADIATOR)
			.notMirrored()
			.build();
		
		public static final MultiblockRegistration<RadiatorState> LOGISTIC_CORE = metal(new RadiatorLogic(), "logistic_core",t->t.lightLevel(bs->bs.getValue(NonMirrorableWithActiveBlock.ACTIVE)?15:0))
			.structure(() -> FHMultiblocks.LOGISTIC_CORE)
			.notMirrored()
			.build();
		private static <S extends IMultiblockState> IEMultiblockBuilder<S> stone(IMultiblockLogic<S> logic, String name, boolean solid,Function<Properties,Properties> modifier) {
			Properties properties = Properties.of()
				.mapColor(MapColor.STONE)
				.instrument(NoteBlockInstrument.BASEDRUM)
				.strength(2, 20);
			if (!solid)
				properties.noOcclusion();
			return new IEMultiblockBuilder<>(logic, name)
				.notMirrored()
				.customBlock(
					FHBlocks.BLOCKS, FHItems.ITEMS,
					r -> new CActiveMultiblockBlock<>(modifier.apply(properties), r),
					MultiblockItem::new)
				.defaultBEs(FHBlockEntityTypes.REGISTER);
		}

		private static <S extends IMultiblockState> IEMultiblockBuilder<S> metal(IMultiblockLogic<S> logic, String name,Function<Properties,Properties> modifier) {
			return new IEMultiblockBuilder<>(logic, name)
				.defaultBEs(FHBlockEntityTypes.REGISTER)
				.customBlock(
					FHBlocks.BLOCKS, FHItems.ITEMS,
					r -> new CActiveMultiblockBlock<>(modifier.apply(IEBlocks.METAL_PROPERTIES_NO_OCCLUSION.get()), r),
					MultiblockItem::new);
		}

	}
	public static final IETemplateMultiblock GENERATOR_T1 = new T1GeneratorMultiblock();
	public static final IETemplateMultiblock GENERATOR_T2 = new T2GeneratorMultiblock();
	public static final IETemplateMultiblock RADIATOR = new RadiatorMultiblock();
	public static final IETemplateMultiblock LOGISTIC_CORE = new LogisticCoreMultiblock();
	public static void registerMultiblocks() {
		MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR_T1);
		MultiblockHandler.registerMultiblock(FHMultiblocks.RADIATOR);
		MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR_T2);
		MultiblockHandler.registerMultiblock(FHMultiblocks.LOGISTIC_CORE);
		//System.out.println("eventBus loaded");
	}


}