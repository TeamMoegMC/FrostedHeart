/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.render.infrared;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.lwjgl.system.MemoryUtil;

/** Keeps the base shader's attribute values while giving ownership its own integer attribute. */
public final class OwnedChunkVertexType implements ChunkVertexType {
    public static final int OWNER_ATTRIBUTE = 4;
    public static final OwnedChunkVertexType COMPACT = new OwnedChunkVertexType(ChunkMeshFormats.COMPACT, true);
    public static final OwnedChunkVertexType HIGH_PRECISION = new OwnedChunkVertexType(ChunkMeshFormats.VANILLA_LIKE, false);

    private final ChunkVertexType base;
    private final GlVertexFormat<ChunkMeshAttribute> format;
    private final GlVertexAttributeBinding[] bindings;
    private final ChunkVertexEncoder encoder;

    private OwnedChunkVertexType(ChunkVertexType base, boolean compact) {
        this.base = base;
        int stride = compact ? 20 : 32;
        format = GlVertexFormat.builder(ChunkMeshAttribute.class, stride)
                .addElement(ChunkMeshAttribute.POSITION_MATERIAL_MESH, 0,
                        compact ? GlVertexAttributeFormat.UNSIGNED_SHORT : GlVertexAttributeFormat.FLOAT,
                        compact ? 4 : 3, false, compact)
                .addElement(ChunkMeshAttribute.COLOR_SHADE, compact ? 8 : 12,
                        GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
                .addElement(ChunkMeshAttribute.BLOCK_TEXTURE, compact ? 12 : 16,
                        compact ? GlVertexAttributeFormat.UNSIGNED_SHORT : GlVertexAttributeFormat.FLOAT, 2, false, false)
                .addElement(ChunkMeshAttribute.LIGHT_TEXTURE, compact ? 16 : 24,
                        compact ? GlVertexAttributeFormat.UNSIGNED_BYTE : GlVertexAttributeFormat.UNSIGNED_INT,
                        compact ? 2 : 1, false, true)
                .build();
        bindings = new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID, format.getAttribute(ChunkMeshAttribute.POSITION_MATERIAL_MESH)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR, format.getAttribute(ChunkMeshAttribute.COLOR_SHADE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE, format.getAttribute(ChunkMeshAttribute.BLOCK_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE, format.getAttribute(ChunkMeshAttribute.LIGHT_TEXTURE)),
                new GlVertexAttributeBinding(OWNER_ATTRIBUTE,
                        new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_SHORT, 1, false, compact ? 18 : 28, stride, true))
        };
        // The two supported base encoders are stateless; the only context is thread-local.
        ChunkVertexEncoder original = base.getEncoder();
        encoder = compact ? (ptr, material, vertex, section) -> {
            long end = original.write(ptr, material, vertex, section);
            int lights = (vertex.light & 255) | ((vertex.light >>> 8) & 0xff00);
            MemoryUtil.memPutInt(ptr + 16, lights | (BlockOwnerScope.current() << 16));
            return end;
        } : (ptr, material, vertex, section) -> {
            original.write(ptr, material, vertex, section);
            MemoryUtil.memPutInt(ptr + 28, BlockOwnerScope.current());
            return ptr + 32;
        };
    }

    public ChunkVertexType base() { return base; }
    public GlVertexAttributeBinding[] bindings() { return bindings; }
    @Override public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() { return format; }
    @Override public ChunkVertexEncoder getEncoder() { return encoder; }
    @Override public float getPositionScale() { return base.getPositionScale(); }
    @Override public float getPositionOffset() { return base.getPositionOffset(); }
    @Override public float getTextureScale() { return base.getTextureScale(); }
}
