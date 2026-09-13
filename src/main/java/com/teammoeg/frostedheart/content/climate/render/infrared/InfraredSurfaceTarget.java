/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.render.infrared;

import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL33C.*;

/** Owns the temperature image and terrain depth snapshot; FBOs borrow the world's attachments. */
public final class InfraredSurfaceTarget implements AutoCloseable {
    private static final int[] DRAW_BUFFERS = {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1};
    private static final int[] INVALID = {-32768, 0, 0, 0};
    private int temperature, terrainDepth, capture, blend;
    private int width, height, colorAttachment, depthAttachment;

    public boolean ensure(int newWidth, int newHeight, int color, int depth) {
        if (newWidth <= 0 || newHeight <= 0 || color <= 0 || depth <= 0) return false;
        if (temperature != 0 && width == newWidth && height == newHeight
                && colorAttachment == color && depthAttachment == depth) return true;
        int previousFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        try {
            if (temperature == 0) {
                temperature = glGenTextures();
                terrainDepth = glGenTextures();
                capture = glGenFramebuffers();
                blend = glGenFramebuffers();
            }
            glBindTexture(GL_TEXTURE_2D, temperature);
            if (width != newWidth || height != newHeight) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_R16I, newWidth, newHeight, 0, GL_RED_INTEGER, GL_SHORT, 0L);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            }
            glBindTexture(GL_TEXTURE_2D, depth);
            int depthFormat = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_INTERNAL_FORMAT);
            if (depthFormat == GL_DEPTH24_STENCIL8) depthFormat = GL_DEPTH_COMPONENT24;
            if (depthFormat == GL_DEPTH32F_STENCIL8) depthFormat = GL_DEPTH_COMPONENT32F;
            glBindTexture(GL_TEXTURE_2D, terrainDepth);
            glTexImage2D(GL_TEXTURE_2D, 0, depthFormat, newWidth, newHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, capture);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, temperature, 0);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);
            glDrawBuffers(DRAW_BUFFERS);
            requireComplete();
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, blend);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            requireComplete();
            width = newWidth; height = newHeight;
            colorAttachment = color; depthAttachment = depth;
            return true;
        } finally {
            // All temporary raw GL changes are restored, preserving the caller's state caches.
            glBindTexture(GL_TEXTURE_2D, previousTexture);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousFramebuffer);
        }
    }

    public void clear() {
        int previousFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = glIsEnabled(GL_SCISSOR_TEST);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer mask = stack.malloc(4);
            glGetBooleani_v(GL_COLOR_WRITEMASK, 1, mask);
            try {
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, capture);
                glDisable(GL_SCISSOR_TEST);
                glColorMaski(1, true, true, true, true);
                glClearBufferiv(GL_COLOR, 1, INVALID);
            } finally {
                glColorMaski(1, mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
                if (scissor) glEnable(GL_SCISSOR_TEST);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousFramebuffer);
            }
        }
    }

    public int temperatureTexture() { return temperature; }
    public int terrainDepthTexture() { return terrainDepth; }

    /** Copy the actual depth storage, without re-rasterizing or estimating its quantization. */
    public void captureTerrainDepth() {
        int previousRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        try {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, capture);
            glBindTexture(GL_TEXTURE_2D, terrainDepth);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        } finally {
            glBindTexture(GL_TEXTURE_2D, previousTexture);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, previousRead);
        }
    }
    public int captureFramebuffer() { return capture; }
    public int blendFramebuffer() { return blend; }

    @Override public void close() {
        if (capture != 0) glDeleteFramebuffers(capture);
        if (blend != 0) glDeleteFramebuffers(blend);
        if (temperature != 0) glDeleteTextures(temperature);
        if (terrainDepth != 0) glDeleteTextures(terrainDepth);
        capture = blend = temperature = terrainDepth = width = height = colorAttachment = depthAttachment = 0;
    }

    private static void requireComplete() {
        int status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Infrared framebuffer is incomplete: " + status);
    }
}
