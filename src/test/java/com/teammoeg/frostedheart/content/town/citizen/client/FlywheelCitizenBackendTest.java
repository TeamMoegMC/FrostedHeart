/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.jozufozu.flywheel.api.vertex.VertexList;
import com.jozufozu.flywheel.backend.gl.buffer.VecBuffer;
import com.jozufozu.flywheel.backend.instancing.instancing.InstancingEngine;
import com.jozufozu.flywheel.core.model.Model;
import com.teammoeg.frostedheart.content.town.resident.Resident;

class FlywheelCitizenBackendTest {

	@Test
	void dynamicInstanceFitsFlywheelBudget() {
		assertEquals(58, FlywheelCitizenBackend.INSTANCE_STRIDE_BYTES);
		assertTrue(FlywheelCitizenBackend.INSTANCE_STRIDE_BYTES <= 64);
		assertEquals(7, CitizenInstanceType.FORMAT.getAttributeCount());
	}

	@Test
	void backendListensForFlywheelOriginInvalidation() {
		assertTrue(InstancingEngine.OriginShiftListener.class.isAssignableFrom(FlywheelCitizenBackend.class));
	}

	@Test
	void sharedBodyContainsSixTaggedBoxes() {
		Model model = FlywheelCitizenBackend.createBodyModel();
		try {
			assertEquals(6 * CitizenFlywheelModels.VERTICES_PER_BOX, model.vertexCount());
			VertexList vertices = model.getReader();
			for (int part = 0; part < 6; part++) {
				for (int vertex = 0; vertex < CitizenFlywheelModels.VERTICES_PER_BOX; vertex++)
					assertEquals(part, vertices.getR(part * CitizenFlywheelModels.VERTICES_PER_BOX + vertex) & 0xFF);
			}
		} finally {
			model.delete();
		}
	}

	@Test
	void billboardUsesSeparateBodyAndHeadQuads() {
		Model model = FlywheelCitizenBackend.createBillboardModel();
		try {
			assertEquals(8, model.vertexCount());
			for (int vertex = 0; vertex < 4; vertex++)
				assertEquals(CitizenFlywheelModels.BILLBOARD_PART, model.getReader().getR(vertex) & 0xFF);
			for (int vertex = 4; vertex < 8; vertex++)
				assertEquals(CitizenFlywheelModels.BILLBOARD_HEAD_PART,
						model.getReader().getR(vertex) & 0xFF);
			assertEquals(0.75f, model.getReader().getY(2));
			assertEquals(0.75f, model.getReader().getY(4));
			assertEquals(1.0f, model.getReader().getY(6));
			assertEquals(16.0f / 64.0f, model.getReader().getU(4));
			assertEquals(16.0f / 64.0f, model.getReader().getV(4));
			assertEquals(8.0f / 64.0f, model.getReader().getU(6));
			assertEquals(8.0f / 64.0f, model.getReader().getV(6));
		} finally {
			model.delete();
		}
	}

	@Test
	void sharedBodyLayoutMatchesTheFlywheelModel() {
		assertEquals(6, CitizenBatchRenderLayout.bodyPartCount());
		for (int index = 0; index < CitizenBatchRenderLayout.bodyPartCount(); index++)
			assertEquals(index, CitizenBatchRenderLayout.bodyPartAt(index).id());

		CitizenBatchRenderLayout.BodyPart torso = CitizenBatchRenderLayout.bodyPartAt(0);
		assertEquals(-4.0f, torso.modelStartX());
		assertEquals(-24.0f, torso.modelStartY());
		assertEquals(-2.0f, torso.modelStartZ());
		assertEquals(8, torso.widthPixels());
		assertEquals(12, torso.heightPixels());
		assertEquals(4, torso.depthPixels());

		CitizenBatchRenderLayout.BodyPart head = CitizenBatchRenderLayout.bodyPartAt(1);
		assertEquals(-4.0f, head.modelStartX());
		assertEquals(-32.0f, head.modelStartY());
		assertEquals(-4.0f, head.modelStartZ());
		assertEquals(2.0f, head.minY() + head.height());

		assertEquals(1.0f, CitizenBatchRenderLayout.bodyPartAt(2).swingSign());
		assertEquals(-1.0f, CitizenBatchRenderLayout.bodyPartAt(3).swingSign());
		assertEquals(-1.4f, CitizenBatchRenderLayout.bodyPartAt(4).swingScale()
				* CitizenBatchRenderLayout.bodyPartAt(4).swingSign());
		assertEquals(1.4f, CitizenBatchRenderLayout.bodyPartAt(5).swingScale()
				* CitizenBatchRenderLayout.bodyPartAt(5).swingSign());
	}

	@Test
	void sharedBillboardLayoutMatchesTheAcceptedFlywheelSilhouette() {
		assertEquals(2, CitizenBatchRenderLayout.billboardQuadCount());
		assertEquals(8, CitizenBatchRenderLayout.billboardVertexCount());

		CitizenBatchRenderLayout.BillboardQuad body = CitizenBatchRenderLayout.billboardQuadAt(0);
		assertEquals(1.0f, body.halfWidth());
		assertEquals(0.0f, body.minY());
		assertEquals(0.75f, body.maxY());
		assertEquals(20, body.minU());
		assertEquals(20, body.minV());
		assertEquals(28, body.maxU());
		assertEquals(32, body.maxV());

		CitizenBatchRenderLayout.BillboardQuad head = CitizenBatchRenderLayout.billboardQuadAt(1);
		assertEquals(0.75f, head.halfWidth());
		assertEquals(0.75f, head.minY());
		assertEquals(1.0f, head.maxY());
		assertEquals(8, head.minU());
		assertEquals(8, head.minV());
		assertEquals(16, head.maxU());
		assertEquals(16, head.maxV());

		assertEquals(0.30f, CitizenBatchRenderLayout.standingBillboardHalfWidth(body));
		assertEquals(1.35f, CitizenBatchRenderLayout.standingBillboardY(body.maxY()), 1.0e-6f);
		assertEquals(0.225f, CitizenBatchRenderLayout.standingBillboardHalfWidth(head), 1.0e-6f);
		assertEquals(1.80f, CitizenBatchRenderLayout.standingBillboardY(head.maxY()), 1.0e-6f);
		assertEquals(0.38f, CitizenBatchRenderLayout.sleepingBillboardLength(body.minY()), 1.0e-6f);
		assertEquals(-0.79f, CitizenBatchRenderLayout.sleepingBillboardLength(body.maxY()), 1.0e-6f);
		assertEquals(-1.18f, CitizenBatchRenderLayout.sleepingBillboardLength(head.maxY()), 1.0e-6f);
	}

	@Test
	void animationClockWrapAndYawDeltaUseShortestPaths() {
		assertEquals(5.0f, FlywheelCitizenBackend.wrapAnimationTime(
				FlywheelCitizenBackend.ANIMATION_PERIOD_TICKS + 5.0f));
		assertEquals(FlywheelCitizenBackend.ANIMATION_PERIOD_TICKS - 5.0f,
				FlywheelCitizenBackend.wrapAnimationTime(-5.0f));
		assertEquals(11, FlywheelCitizenBackend.shortYawDelta(250, 5));
		assertEquals(-127, FlywheelCitizenBackend.shortYawDelta(0, 129));
		assertEquals(128, FlywheelCitizenBackend.shortYawDelta(0, 128));
	}

	@Test
	void backendReferencesFlywheelAnimationClock() throws IOException {
		try (InputStream classStream = FlywheelCitizenBackend.class.getResourceAsStream("FlywheelCitizenBackend.class")) {
			assertNotNull(classStream);
			String bytecode = new String(classStream.readAllBytes(), StandardCharsets.ISO_8859_1);
			assertTrue(bytecode.contains("com/jozufozu/flywheel/util/AnimationTickHolder"));
			assertFalse(bytecode.contains("com/simibubi/create/foundation/utility/AnimationTickHolder"));
		}
	}

	@Test
	void walkPhaseAdvancesBySnapshotDistanceAtVanillaScale() {
		assertEquals(1.0f + 5.0f * CitizenBatchRenderLayout.WALK_PHASE_PER_BLOCK,
				CitizenBatchRenderLayout.advanceWalkPhase(1.0f, 1.0, 2.0, 4.0, 6.0), 1.0e-6f);
		assertEquals(128, FlywheelCitizenBackend.encodeWalkPhase((float) Math.PI) & 0xFF);
	}

	@Test
	void instanceWriterMatchesTheShaderFieldOrder() {
		CitizenInstanceData data = new CitizenInstanceData();
		data.blockLight = 3;
		data.skyLight = 14;
		data.pos0X = 1.0f;
		data.pos1X = 2.0f;
		data.snapshotTime = 3.0f;
		data.velocityX = 4.0f;
		data.yawStart = 5.0f;
		data.moving = 6;
		data.sleeping = 7;
		data.phase = 8;
		data.age = (byte) Resident.AGE_CHILD;

		ByteBuffer bytes = ByteBuffer.allocateDirect(FlywheelCitizenBackend.INSTANCE_STRIDE_BYTES)
				.order(ByteOrder.nativeOrder());
		CitizenInstanceType.INSTANCE.getWriter(new VecBuffer(bytes)).write(data);
		assertEquals(3 << 4, bytes.get(0) & 0xFF);
		assertEquals(14 << 4, bytes.get(1) & 0xFF);
		assertEquals(1.0f, bytes.getFloat(2));
		assertEquals(2.0f, bytes.getFloat(14));
		assertEquals(3.0f, bytes.getFloat(26));
		assertEquals(4.0f, bytes.getFloat(34));
		assertEquals(5.0f, bytes.getFloat(42));
		assertEquals(6, bytes.get(54));
		assertEquals(7, bytes.get(55));
		assertEquals(8, bytes.get(56));
		assertEquals(Resident.AGE_CHILD, bytes.get(57));
	}

	@Test
	void citizenProgramResourcesArePackaged() throws IOException {
		try (InputStream shaderStream = getClass().getResourceAsStream(
				"/assets/frostedheart/flywheel/shaders/citizen.vert");
				InputStream programStream = getClass().getResourceAsStream(
						"/assets/frostedheart/flywheel/programs/citizen.json")) {
			assertNotNull(shaderStream);
			assertNotNull(programStream);
			String shader = new String(shaderStream.readAllBytes(), StandardCharsets.UTF_8);
			String program = new String(programStream.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(shader.contains("struct Citizen"));
			assertTrue(shader.contains("citizenElapsed"));
			assertTrue(shader.contains("float snapshotSpeed = snapshotDistance / duration;"));
			assertTrue(shader.contains("const float CITIZEN_WALK_PHASE_PER_BLOCK = "
					+ CitizenBatchRenderLayout.WALK_PHASE_PER_BLOCK + ";"));
			assertTrue(shader.contains("const float CITIZEN_LEG_SWING_SCALE = "
					+ CitizenBatchRenderLayout.bodyPartAt(4).swingScale() + ";"));
			assertTrue(shader.contains("const float CITIZEN_SLEEP_SCALE = "
					+ CitizenBatchRenderLayout.SLEEP_SCALE + ";"));
			assertTrue(shader.contains("const float CITIZEN_SLEEP_ORIGIN = "
					+ CitizenBatchRenderLayout.SLEEP_MODEL_ORIGIN + ";"));
			assertTrue(shader.contains("const float CITIZEN_SLEEP_SURFACE_Y = "
					+ CitizenBatchRenderLayout.SLEEP_SURFACE_Y + ";"));
			assertFalse(shader.contains("uTime * 0.6"));
			assertFalse(shader.contains("float bob ="));
			assertTrue(shader.contains("part > 6.5 ? 0.375 : 0.8125"));
			assertTrue(shader.contains("float ageScale = citizen.flags.w"));
			assertTrue(shader.contains("localPos *= ageScale;"));
			assertTrue(program.contains("frostedheart:citizen.vert"));
		}
	}
}
