package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.components.press.MechanicalPressTileEntity;
import com.simibubi.create.content.contraptions.components.press.MechanicalPressTileEntity.Mode;
import com.simibubi.create.content.contraptions.processing.BasinTileEntity;
import com.simibubi.create.content.contraptions.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.contraptions.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.content.ProcessingScenes;
import com.simibubi.create.foundation.ponder.elements.InputWindowElement;
import com.simibubi.create.foundation.utility.IntAttached;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.Pointing;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
@Mixin(ProcessingScenes.class)
public class MixinProcessingScenes {
	@Overwrite(remap=false)
	public static void pressing(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("mechanical_press", "Processing Items with the Mechanical Press");
		scene.configureBasePlate(0, 0, 5);
		scene.world.setBlock(util.grid.at(1, 1, 2), AllBlocks.ANDESITE_CASING.getDefaultState(), false);
		scene.world.showSection(util.select.layer(0), Direction.UP);
		scene.idle(5);
		scene.world.showSection(util.select.fromTo(1, 4, 3, 1, 1, 5), Direction.DOWN);
		scene.idle(5);
		scene.world.showSection(util.select.position(1, 1, 2), Direction.DOWN);
		scene.idle(5);
		scene.world.showSection(util.select.position(1, 2, 2), Direction.DOWN);
		scene.idle(5);
		scene.world.showSection(util.select.position(1, 4, 2), Direction.SOUTH);
		scene.idle(5);
		scene.world.showSection(util.select.fromTo(3, 1, 1, 1, 1, 1), Direction.SOUTH);
		scene.world.showSection(util.select.fromTo(3, 1, 5, 3, 1, 2), Direction.SOUTH);
		scene.idle(20);

		BlockPos basin = util.grid.at(1, 2, 2);
		BlockPos pressPos = util.grid.at(1, 4, 2);
		Vector3d basinSide = util.vector.blockSurface(basin, Direction.WEST);

		ItemStack copper = AllItems.COPPER_INGOT.asStack();
		ItemStack copperBlock = AllItems.COPPER_SHEET.asStack();

		scene.overlay.showText(60)
			.pointAt(basinSide)
			.placeNearTarget()
			.attachKeyFrame()
			.text("Pressing items held in a Basin will cause them to be Compacted");
		scene.idle(40);

		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(basin), Pointing.DOWN).withItem(copper),
			30);
		scene.idle(30);
		Class<MechanicalPressTileEntity> type = MechanicalPressTileEntity.class;
		scene.world.modifyTileEntity(pressPos, type, pte -> pte.start(Mode.BASIN));
		scene.idle(30);
		scene.world.modifyTileEntity(pressPos, type,
			pte -> pte.makeCompactingParticleEffect(util.vector.centerOf(basin), copper));
		scene.world.modifyTileNBT(util.select.position(basin), BasinTileEntity.class, nbt -> {
			nbt.put("VisualizedItems",
				NBTHelper.writeCompoundList(ImmutableList.of(IntAttached.with(1, copperBlock)), ia -> ia.getValue()
					.serializeNBT()));
		});
		scene.idle(4);
		scene.world.createItemOnBelt(util.grid.at(1, 1, 1), Direction.UP, copperBlock);
		scene.idle(30);

		scene.overlay.showText(80)
			.pointAt(basinSide)
			.placeNearTarget()
			.attachKeyFrame()
			.text("Compacting includes any filled 2x2 or 3x3 Crafting Recipe, plus a couple extra ones");

		scene.idle(30);
		ItemStack log = new ItemStack(Items.OAK_LOG);
		ItemStack bark = new ItemStack(Items.OAK_WOOD);

		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(basin), Pointing.DOWN).withItem(log), 30);
		scene.idle(30);
		scene.world.modifyTileEntity(pressPos, type, pte -> pte.start(Mode.BASIN));
		scene.idle(30);
		scene.world.modifyTileEntity(pressPos, type,
			pte -> pte.makeCompactingParticleEffect(util.vector.centerOf(basin), log));
		scene.world.modifyTileNBT(util.select.position(basin), BasinTileEntity.class, nbt -> {
			nbt.put("VisualizedItems",
				NBTHelper.writeCompoundList(ImmutableList.of(IntAttached.with(1, bark)), ia -> ia.getValue()
					.serializeNBT()));
		});
		scene.idle(4);
		scene.world.createItemOnBelt(util.grid.at(1, 1, 1), Direction.UP, bark);
		scene.idle(30);

		scene.rotateCameraY(-30);
		scene.idle(10);
		scene.world.setBlock(util.grid.at(1, 1, 2), AllBlocks.BLAZE_BURNER.getDefaultState()
			.with(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED), true);
		scene.idle(10);

		scene.overlay.showText(80)
			.pointAt(basinSide.subtract(0, 1, 0))
			.placeNearTarget()
			.text("Some of those recipes may require the heat of a Blaze Burner");
		scene.idle(40);

		scene.rotateCameraY(30);

		scene.idle(60);
		Vector3d filterPos = util.vector.of(1, 2.75f, 2.5f);
		scene.overlay.showFilterSlotInput(filterPos, 100);
		scene.overlay.showText(120)
			.pointAt(filterPos)
			.placeNearTarget()
			.attachKeyFrame()
			.text("The filter slot can be used in case two recipes are conflicting.");
		scene.idle(60);
	}
}
