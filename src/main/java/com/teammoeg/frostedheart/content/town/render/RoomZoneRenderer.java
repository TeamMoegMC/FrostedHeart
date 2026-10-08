package com.teammoeg.frostedheart.content.town.render;

import java.util.Map.Entry;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.TesselateHelper.Shape3DTesslator;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RoomZoneRenderer {
	public static boolean visible;

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return;
		if (!visible) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		TeamTown town = TeamTownData.getClientTown().orElse(null);
		if (town == null) return;

		PoseStack poseStack = event.getPoseStack();
		Camera camera = event.getCamera();
		Vec3 cam = camera.getPosition();

		poseStack.pushPose();

		// 事件里的 poseStack 已经应用了相机旋转，
		// 所以这里只做“世界坐标 -> 相机空间”的平移。
		poseStack.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f matrix = poseStack.last().pose();
		boolean blink=(System.currentTimeMillis() / BLINKS_INTERVAL& 1) != 0;
		Object2IntOpenHashMap<BlockPos> index = new Object2IntOpenHashMap<>();
		try (var tesselator = TesselateHelper.getShape3DTesslator()) {
			for (Entry<BlockPos, AbstractTownBuilding> i : town.getTownBuildings().entrySet()) {
				if (i.getKey().distSqr(event.getCamera().getBlockPosition()) < 1024) {
					if(index.addTo(i.getValue().getOccupiedVolume().getSignature(),1)<=0) {
						renderBuilding(mc.level,i.getValue(), tesselator, matrix, blink);
					}
				}
			}
		}
		poseStack.popPose();
	}

	private static final int BLINKS_INTERVAL = 250;

	public static void renderBuilding(ClientLevel level,AbstractTownBuilding building, Shape3DTesslator tesselator, Matrix4f matrix,boolean blink) {

		OccupiedZoneInfo space = building.getOccupiedVolume();
		if (space.getErrorPos() != null || space.getOccupiedCells().size() > 0) {
			if (space.getErrorPos() != null)
				for (BlockPos pos : space.getErrorPos()) {
					tesselator.cube(matrix, new AABB(pos).inflate(0.05f), 0xaaff8888);
				}
			int color = 0xaa88ff88;
			if (space.isOverlapped()) {
				color = 0xaaffff88;
				if(blink)
					return;
			}

			for (OccupiedCell oc : space.getOccupiedCells())
				for (MutableBlockPos pos : oc.pathIterable()) {
					BlockState state=level.getBlockState(pos);
					double y=state.getVisualShape(level, pos, CollisionContext.empty()).max(Axis.Y);
					if(y<=0)
						y=0;
					renderDoubleFloorFace(tesselator, matrix, pos.getX(), (float) (pos.getY() + 0.05f+y), pos.getZ(), color);

				}
			/*
			 * for(Face face:cache.faces) { if(face.dir()==Direction.DOWN) { Vec3i[]
			 * v3is=face.points(); for(int i=3;i>=0;i--) { Vec3i vec=v3is[i];
			 * tesselator.vertex(matrix, vec.getX()+face.voxel().getX(),
			 * vec.getY()+face.voxel().getY(), vec.getZ()+face.voxel().getZ(), 0xaa88ff88);
			 * } }else for(Vec3i vec:face.points()) tesselator.vertex(matrix,
			 * vec.getX()+face.voxel().getX(), vec.getY()+face.voxel().getY()+0.05f,
			 * vec.getZ()+face.voxel().getZ(), 0xaaff8888);
			 * 
			 * }
			 */

		}

	}

	private static void renderDoubleFloorFace(Shape3DTesslator tesselator, Matrix4f matrix, float x, float y, float z, int color) {

		tesselator.vertex(matrix, x, y, z, color);
		tesselator.vertex(matrix, x + 1, y, z, color);
		tesselator.vertex(matrix, x + 1, y, z + 1, color);
		tesselator.vertex(matrix, x, y, z + 1, color);
		// top face
		tesselator.vertex(matrix, x, y, z, color);
		tesselator.vertex(matrix, x, y, z + 1, color);
		tesselator.vertex(matrix, x + 1, y, z + 1, color);
		tesselator.vertex(matrix, x + 1, y, z, color);
	}
}