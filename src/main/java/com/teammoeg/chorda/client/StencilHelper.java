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

package com.teammoeg.chorda.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
/**
 * 模板测试辅助工具类，用于在Minecraft 1.20.1渲染中管理OpenGL模板测试的状态栈。
 * 支持嵌套的模板区域绘制，通过栈结构保存和恢复渲染状态，避免状态污染。
 * <p>
 * Helper class for OpenGL stencil test management in Minecraft 1.20.1 rendering.
 * Supports nested stencil region drawing, uses a stack to save and restore rendering states to avoid pollution.
 */
public class StencilHelper {
	/**
     * 模板栈元素类，保存单一层模板测试的渲染状态、颜色遮罩以及用于绘制模板形状的顶点资源。
     * <p>
     * Stencil stack element class, holds rendering states, color mask, and vertex resources for drawing stencil shapes for a single layer.
     */
	public static class StencilStackElement{
		private boolean stencilEnabled=false;
	    //stencil state
		private int stencilMask=0,stencilFunc=0,stencilRef=0,stencilValueMask=0,stencilFail=0,stencilDepthFail=0,stencilPass=0;
	    private int currentStencil=0;
	    //color mask
	    private ByteBuffer colorMask=ByteBuffer.allocateDirect(4);
	    //stencil buffer
	    private BufferBuilder builder=new BufferBuilder(100);
	    private boolean isBufferFinished=false;
	    private ShaderInstance shader;
	    private VertexBuffer vertexBuffer=new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
	    private StencilStackElement() {
	    	
	    }
	    /**
         * 保存当前OpenGL的模板测试状态、颜色遮罩，并初始化当前层的模板参考值。
         * 会检查模板层数不超过0xFF（模板缓冲通常为8位）。
         * <p>
         * Saves current OpenGL stencil test states and color mask, initializes the stencil reference value for current layer.
         * Checks that stencil layer count does not exceed 0xFF (stencil buffer is usually 8-bit).
         */
	    private void push() {
	    	
		    stencilEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
		    stencilMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
		    stencilFunc = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
		    stencilRef = GL11.glGetInteger(GL11.GL_STENCIL_REF);
		    stencilValueMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
		    stencilFail = GL11.glGetInteger(GL11.GL_STENCIL_FAIL);
		    stencilDepthFail = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL);
		    stencilPass = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS);
		    GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
		    currentStencil=stencilRef+1;
		    if(currentStencil>0xFF)
		    	throw new IllegalStateException("Stencil stack must not exceed 0xff");
		    isBufferFinished=false;
		    shader=null;
	    }
	    public BufferBuilder getBuilder(VertexFormat.Mode mode) {
	    	return getBuilder(mode,DefaultVertexFormat.POSITION,GameRenderer.getPositionShader());
	    }
	    /**
         * 获取用于构建模板形状顶点数据的{@link BufferBuilder}。
         * <p>
         * Gets the {@link BufferBuilder} for building vertex data of the stencil shape.
         * @return 用于构建模板形状的BufferBuilder (BufferBuilder for building the stencil shape)
         */
	    public BufferBuilder getBuilder(VertexFormat.Mode mode,VertexFormat format,ShaderInstance shader) {
	    	this.shader=shader;
	    	builder.begin(mode, format);
	    	return builder;
	    }
	    /**
         * 结束顶点构建并绘制模板形状到模板缓冲，递增模板值，之后切换到新的模板层状态。
         * 必须在{@link #getBuilder()}开始构建后调用，且仅能调用一次。
         * <p>
         * Ends vertex building and draws the stencil shape to stencil buffer, increments stencil value, then switches to new stencil layer state.
         * Must be called after starting building with {@link #getBuilder()}, and can only be called once.
         * @throws IllegalStateException 如果BufferBuilder未在构建、已绘制过或渲染缓冲为空 (If BufferBuilder is not building, already drawn, or rendered buffer is empty)
         */
	    public void drawStencil() {
	    	if (!builder.building()) {
	    	    throw new IllegalStateException("BufferBuilder is empty");
	    	}
	    	if(isBufferFinished)
	    		throw new IllegalStateException("drawStencil called twice");
	    	RenderedBuffer buffer=builder.end();
	    	if(buffer.isEmpty()) {
	    		buffer.release();
	    		throw new IllegalStateException("RenderedBuffer is Empty");
	    	}
	        GL11.glEnable(GL11.GL_STENCIL_TEST);
	        RenderSystem.stencilFunc(GL11.GL_EQUAL, stencilRef, 0xFF);
	        RenderSystem.stencilMask(0xFF);
	        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_INCR, GL11.GL_INCR);
	        RenderSystem.colorMask(false, false, false, false);
	    	vertexBuffer.bind();
	    	vertexBuffer.upload(buffer);
	    	vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
	    	isBufferFinished=true;
	    	RenderSystem.colorMask(colorMask.get(0)!=0, colorMask.get(1)!=0, colorMask.get(2)!=0, colorMask.get(3)!=0);
	        //切换到新stencil byte
	    	RenderSystem.stencilMask(0x00);
	    	RenderSystem.stencilFunc(GL11.GL_EQUAL, currentStencil, 0xFF);
	    }
	    /**
         * 恢复之前保存的模板测试状态和颜色遮罩。如果当前层已绘制模板形状，会递减模板缓冲的值来清理当前层。
         * <p>
         * Restores previously saved stencil test states and color mask. If stencil shape was drawn for this layer, decrements stencil buffer values to clean up current layer.
         */
	    private void pop() {
	    	if(builder.building())
	    		builder.clear();
	    	if(isBufferFinished) {//clear drawn stencil only if stencil drawn
				GL11.glEnable(GL11.GL_STENCIL_TEST);
				RenderSystem.stencilFunc(GL11.GL_EQUAL, currentStencil, 0xFF);
				RenderSystem.stencilMask(0xFF); 
		        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_DECR, GL11.GL_DECR);
		        RenderSystem.colorMask(false, false, false, false);
				vertexBuffer.bind();
				vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
				RenderSystem.colorMask(colorMask.get(0)!=0, colorMask.get(1)!=0, colorMask.get(2)!=0, colorMask.get(3)!=0);
	    	}
			if(!stencilEnabled)
				GL11.glDisable(GL11.GL_STENCIL_TEST);
			RenderSystem.stencilMask(stencilMask);
			RenderSystem.stencilFunc(stencilFunc, stencilRef, stencilValueMask);
			RenderSystem.stencilOp(stencilFail, stencilDepthFail, stencilPass);
	    }
	}
	private static List<StencilStackElement> stencilStack=new ArrayList<>(256);
	private static int currentIndex;
	private StencilHelper() {
	}
	 /**
     * 推入新的模板层到栈中，返回对应的栈元素用于构建模板形状。
     * 必须在渲染线程调用。
     * <p>
     * Pushes a new stencil layer onto the stack, returns the corresponding stack element for building stencil shape.
     * Must be called on render thread.
     * @return 新的模板栈元素 (New stencil stack element)
     */
	public static StencilStackElement pushStencil() {
		RenderSystem.assertOnRenderThread();
		StencilStackElement elem=null;
		if(currentIndex>=stencilStack.size()) {
			stencilStack.add(elem=new StencilStackElement());
		}else {
			elem=stencilStack.get(currentIndex);
		}
		elem.push();
		currentIndex++;
		return elem;
	}
	/**
     * 弹出当前模板层，恢复之前的渲染状态。
     * 必须在渲染线程调用。
     * <p>
     * Pops current stencil layer, restores previous rendering states.
     * Must be called on render thread.
     * @throws IllegalStateException 如果模板栈为空 (If stencil stack is empty)
     */
	public static void popStencil(StencilStackElement element) {
		RenderSystem.assertOnRenderThread();
		if(currentIndex<=0) {
			throw new IllegalStateException("Empty stencil stack");
		}
		if(stencilStack.get(currentIndex-1)!=element) {
			throw new IllegalStateException("Stencil stack mismatch");
		}
		currentIndex--;
		element.pop();
		
	}
	 /**
     * 清空模板缓冲为0。需在使用本辅助类前后调用，且调用时模板栈必须为空。
     * <p>
     * Clears stencil buffer to 0. Should be called before and after using this helper, and stencil stack must be empty when called.
     */
	public static void clearStencil() {
		assertStencilEmpty();
		int stencilMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
		RenderSystem.stencilMask(0xFF);
		RenderSystem.clearStencil(0);
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, false);
		RenderSystem.stencilMask(stencilMask);
	}
	/**
     * 断言模板栈为空，否则抛出异常。
     * <p>
     * Asserts that stencil stack is empty, throws exception otherwise.
     * @throws IllegalStateException 如果模板栈不为空 (If stencil stack is not empty)
     */
	public static void assertStencilEmpty() {
		if(currentIndex>0) {
			throw new IllegalStateException("Stencil stack not empty, curr size="+currentIndex);
		}
	}
	public static void main(String[] args) {
		//1.000E+0 -3.492E-2  0.000E+0 -4.190E+0
		//0.000E+0  1.000E+0  0.000E+0  0.000E+0
		//0.000E+0  0.000E+0  1.000E+0  0.000E+0
		//0.000E+0  0.000E+0  0.000E+0  1.000E+0
		Matrix4f m4f=new Matrix4f();
		m4f.translate(0, 0, 2000);
		System.out.println(m4f);
		
	}
}
