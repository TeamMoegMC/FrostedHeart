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

import com.jozufozu.flywheel.api.vertex.VertexList;
import com.jozufozu.flywheel.api.vertex.VertexType;
import com.jozufozu.flywheel.core.Formats;
import com.jozufozu.flywheel.core.hardcoded.ModelPart;
import com.jozufozu.flywheel.core.hardcoded.PartBuilder;
import com.jozufozu.flywheel.core.model.Model;

/** Shared citizen models with rigid-part ids encoded in the unused vertex color R channel. */
final class CitizenFlywheelModels {

	static final int VERTICES_PER_BOX = 6 * 4;
	static final int BILLBOARD_PART = 6;
	static final int BILLBOARD_HEAD_PART = 7;

	private CitizenFlywheelModels() {
	}

	static Model createBody() {
		PartBuilder builder = ModelPart.builder("frostedheart_citizen_body", 64, 64);
		for (int index = 0; index < CitizenBatchRenderLayout.bodyPartCount(); index++) {
			CitizenBatchRenderLayout.BodyPart part = CitizenBatchRenderLayout.bodyPartAt(index);
			builder.cuboid().textureOffset(part.textureU(), part.textureV())
					.start(part.modelStartX(), part.modelStartY(), part.modelStartZ())
					.size(part.widthPixels(), part.heightPixels(), part.depthPixels()).endCuboid();
		}
		ModelPart body = builder.build();
		return new TaggedModel("frostedheart_citizen_body", body, new BodyReader(body.getReader()));
	}

	static Model createBillboard() {
		return new TaggedModel("frostedheart_citizen_billboard", null, new BillboardReader());
	}

	private static final class TaggedModel implements Model {
		private final String name;
		private final ModelPart ownedPart;
		private final VertexList reader;

		private TaggedModel(String name, ModelPart ownedPart, VertexList reader) {
			this.name = name;
			this.ownedPart = ownedPart;
			this.reader = reader;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public VertexList getReader() {
			return reader;
		}

		@Override
		public int vertexCount() {
			return reader.getVertexCount();
		}

		@Override
		public VertexType getType() {
			return Formats.BLOCK;
		}

		@Override
		public void delete() {
			if (ownedPart != null)
				ownedPart.delete();
		}
	}

	private abstract static class WhiteVertexList implements VertexList {
		@Override
		public byte getG(int index) {
			return (byte) 0xFF;
		}

		@Override
		public byte getB(int index) {
			return (byte) 0xFF;
		}

		@Override
		public byte getA(int index) {
			return (byte) 0xFF;
		}

		@Override
		public int getLight(int index) {
			return 0;
		}
	}

	private static final class BodyReader extends WhiteVertexList {
		private final VertexList delegate;

		private BodyReader(VertexList delegate) {
			this.delegate = delegate;
		}

		@Override
		public float getX(int index) {
			return delegate.getX(index);
		}

		@Override
		public float getY(int index) {
			return delegate.getY(index);
		}

		@Override
		public float getZ(int index) {
			return delegate.getZ(index);
		}

		@Override
		public byte getR(int index) {
			return (byte) (index / VERTICES_PER_BOX);
		}

		@Override
		public float getU(int index) {
			return delegate.getU(index);
		}

		@Override
		public float getV(int index) {
			return delegate.getV(index);
		}

		@Override
		public float getNX(int index) {
			return delegate.getNX(index);
		}

		@Override
		public float getNY(int index) {
			return delegate.getNY(index);
		}

		@Override
		public float getNZ(int index) {
			return delegate.getNZ(index);
		}

		@Override
		public int getVertexCount() {
			return delegate.getVertexCount();
		}
	}

	private static final class BillboardReader extends WhiteVertexList {
		@Override
		public float getX(int index) {
			return CitizenBatchRenderLayout.billboardModelX(index);
		}

		@Override
		public float getY(int index) {
			return CitizenBatchRenderLayout.billboardModelY(index);
		}

		@Override
		public float getZ(int index) {
			return 0.0f;
		}

		@Override
		public byte getR(int index) {
			return (byte) (CitizenBatchRenderLayout.billboardQuadIndex(index) == 0
					? BILLBOARD_PART : BILLBOARD_HEAD_PART);
		}

		@Override
		public float getU(int index) {
			return CitizenBatchRenderLayout.billboardModelU(index);
		}

		@Override
		public float getV(int index) {
			return CitizenBatchRenderLayout.billboardModelV(index);
		}

		@Override
		public float getNX(int index) {
			return 0.0f;
		}

		@Override
		public float getNY(int index) {
			return 0.0f;
		}

		@Override
		public float getNZ(int index) {
			return 1.0f;
		}

		@Override
		public int getVertexCount() {
			return CitizenBatchRenderLayout.billboardVertexCount();
		}
	}
}
