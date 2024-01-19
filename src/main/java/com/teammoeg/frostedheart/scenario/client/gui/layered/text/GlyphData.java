package com.teammoeg.frostedheart.scenario.client.gui.layered.text;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class GlyphData {
	int width;
	int height;
	int x;
	int y;
	BufferedImage image;
	public GlyphData(int width, int height, int x, int y) {
		super();
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
	}
	
	public GlyphData(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	public GlyphData() {
	}
	public int renderFont(Graphics2D g2d,int x,int y,int hsize) {
		g2d.drawImage(image, x, y, x+hsize,(int)(y+(hsize*1f)/height*width),this.x, this.y,this.x+width,this.y+height, null);
		return width;
	}
	public void parseSize(byte data) {
		x+=(data>>4)&15;
		width=data&15+1;
		height=16;
	}
}
