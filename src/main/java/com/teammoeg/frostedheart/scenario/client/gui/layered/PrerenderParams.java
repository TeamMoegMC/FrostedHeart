package com.teammoeg.frostedheart.scenario.client.gui.layered;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class PrerenderParams {
	Graphics2D g2d;
	BufferedImage image;
	int width=2048;
	int height=1152;
	public PrerenderParams() {
		image=new BufferedImage(width,width, BufferedImage.TYPE_INT_ARGB);
		g2d=image.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	}
	public Graphics2D getG2d() {
		return g2d;
	}
	public BufferedImage getImage() {
		return image;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public static void freeTexture(int id) {
		GL11.glDeleteTextures(id);
	}
	public int loadTexture() {

		int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

		ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = pixels[y * image.getWidth() + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red component
				buffer.put((byte) ((pixel >> 8) & 0xFF)); // Green component
				buffer.put((byte) (pixel & 0xFF)); // Blue component
				buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha component. Only for RGBA
			}
		}

		buffer.flip(); // FOR THE LOVE OF GOD DO NOT FORGET THIS

		// You now have a ByteBuffer filled with the color data of each pixel.
		// Now just create a texture ID and bind it. Then you can load it using
		// whatever OpenGL method you want, for example:

		int textureID = GL11.glGenTextures(); // Generate texture ID
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID); // Bind texture ID
/*
		// Setup wrap mode
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

		// Setup texture scaling filtering
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
*/
		// Send texel data to OpenGL
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
		g2d.dispose();
		// Return the texture ID so we can bind it later again
		return textureID;
	}
}
