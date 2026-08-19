/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import com.jozufozu.flywheel.api.struct.Instanced;
import com.jozufozu.flywheel.api.struct.StructWriter;
import com.jozufozu.flywheel.backend.gl.GlNumericType;
import com.jozufozu.flywheel.backend.gl.buffer.VecBuffer;
import com.jozufozu.flywheel.core.layout.BufferLayout;
import com.jozufozu.flywheel.core.layout.CommonItems;
import com.jozufozu.flywheel.core.layout.PrimitiveItem;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.resources.ResourceLocation;

/** Flywheel instance layout for GPU snapshot interpolation and rigid-part animation. */
final class CitizenInstanceType implements Instanced<CitizenInstanceData> {

	static final CitizenInstanceType INSTANCE = new CitizenInstanceType();
	static final PrimitiveItem FLAGS = new PrimitiveItem(GlNumericType.UBYTE, 4);
	static final BufferLayout FORMAT = BufferLayout.builder()
			.addItems(CommonItems.LIGHT)
			.addItems(CommonItems.VEC3, CommonItems.VEC3, CommonItems.VEC2,
					CommonItems.VEC2, CommonItems.VEC3, FLAGS)
			.build();
	private static final ResourceLocation PROGRAM = new ResourceLocation(FHMain.MODID, "citizen");

	private CitizenInstanceType() {
	}

	@Override
	public CitizenInstanceData create() {
		return new CitizenInstanceData();
	}

	@Override
	public BufferLayout getLayout() {
		return FORMAT;
	}

	@Override
	public StructWriter<CitizenInstanceData> getWriter(VecBuffer buffer) {
		return new Writer(buffer);
	}

	@Override
	public ResourceLocation getProgramSpec() {
		return PROGRAM;
	}

	private static final class Writer implements StructWriter<CitizenInstanceData> {
		private final VecBuffer buffer;

		private Writer(VecBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public void write(CitizenInstanceData data) {
			buffer.put((byte) (data.blockLight << 4));
			buffer.put((byte) (data.skyLight << 4));
			buffer.putVec3(data.pos0X, data.pos0Y, data.pos0Z);
			buffer.putVec3(data.pos1X, data.pos1Y, data.pos1Z);
			buffer.putVec2(data.snapshotTime, data.snapshotDuration);
			buffer.putVec2(data.velocityX, data.velocityZ);
			buffer.putVec3(data.yawStart, data.yawDelta, data.yawTime);
			buffer.put(data.moving);
			buffer.put(data.sleeping);
			buffer.put(data.phase);
			buffer.put(data.reserved);
		}

		@Override
		public void seek(int index) {
			buffer.position(index * FORMAT.getStride());
		}
	}
}
