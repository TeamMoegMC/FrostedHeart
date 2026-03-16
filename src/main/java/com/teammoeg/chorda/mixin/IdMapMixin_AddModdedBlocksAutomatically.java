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

package com.teammoeg.chorda.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.teammoeg.chorda.util.ShaderCompatHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.irisshaders.iris.shaderpack.materialmap.BlockEntry;
import net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping;
import net.minecraft.world.level.block.state.BlockState;
/**
 * Iris着色器兼容Mixin，自动将模组方块的材质映射添加到着色器的方块状态ID映射中。
 * 通过将模组方块状态映射到对应的原版方块状态ID，使着色器包能正确渲染模组方块。
 * <p>
 * Iris shader compatibility Mixin that automatically adds mod block material mappings
 * to the shader's block state ID map. Maps modded block states to corresponding vanilla
 * block state IDs so shader packs can render modded blocks correctly.
 */
@Mixin(BlockMaterialMapping.class)
public class IdMapMixin_AddModdedBlocksAutomatically {

	/**
	 * 在创建方块状态ID映射后注入，将模组方块状态重映射到对应的原版方块状态ID。
	 * <p>
	 * Injects after block state ID map creation to remap modded block states
	 * to their corresponding vanilla block state IDs.
	 *
	 * @param blockPropertiesMap 方块属性映射 / The block properties map
	 * @param cbi 回调信息 / The callback info
	 * @param blockStateIds 方块状态ID映射 / The block state ID map
	 */
	@Inject(at=@At(value="TAIL"),method="createBlockStateIdMap",remap=false,locals=LocalCapture.CAPTURE_FAILSOFT)
	private static void fh$createBlockStateIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap,CallbackInfoReturnable<Object2IntMap<BlockState>> cbi,Object2IntMap<BlockState> blockStateIds) {
		//reverse collect entry to map
		//ShaderCompatHelper.tryInit();
		/*Reference2IntMap<BlockState> map=new Reference2IntOpenHashMap<>(entriesById.size()*10);
		for(Entry<List<BlockEntry>> entry:entriesById.int2ObjectEntrySet()) {
			for(BlockEntry be:entry.getValue()) {
				Block block=CRegistryHelper.getBlock(new ResourceLocation(be.id().getNamespace(),be.id().getName()));
				if(block==Blocks.AIR)continue;
				StateDefinition<Block, BlockState> states=block.getStateDefinition();
				//.getStateDefinition()
				if(be.propertyPredicates().isEmpty()) {
					for(BlockState bs:states.getPossibleStates())
						map.put(bs, entry.getIntKey());
				}else {
					Map<Property<?>, String> propertiesPredicate = new HashMap<>();

					be.propertyPredicates().forEach((key, value) -> {
						Property<?> property = states.getProperty(key);

						if (property == null) {
							return;
						}

						propertiesPredicate.put(property, value);
					});
					for(BlockState bs:states.getPossibleStates()) {
						for(java.util.Map.Entry<Property<?>, String> pred:propertiesPredicate.entrySet()) {
							Property p=pred.getKey();
							if(p.getName(bs.getValue(p)).equals(pred.getValue()))
								map.put(bs, entry.getIntKey());
						}
					}
				}
			}
			
			
		}*/
		if(FHConfig.CLIENT.enableShaderPackCompat.get()) {
			FHMain.LOGGER.info("loading "+ShaderCompatHelper.modBlockState2VanillaBlockMap.size()+" shader compats.");
			//IntFunction<List<BlockEntry>> creator=k->new ArrayList<>();
			ShaderCompatHelper.modBlockState2VanillaBlockMap.forEach((k,v)->{
				FHMain.LOGGER.debug("trying to remap "+k+" to "+v);
				if(blockStateIds.containsKey(k)) {
					FHMain.LOGGER.debug("already existed, skipped");
					return;
				}
				if(blockStateIds.containsKey(v)) {
					int key=blockStateIds.getInt(v);
					FHMain.LOGGER.debug("found key id "+key);
					blockStateIds.put(k, key);
					
				}
				
			});
		}
	}
}
