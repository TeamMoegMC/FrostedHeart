package com.teammoeg.chorda.util.client;

import java.util.Arrays;
import java.util.List;

import com.teammoeg.chorda.util.IterateUtils;
import com.teammoeg.chorda.util.utility.MutablePair;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class PointSet {
	List<Point> points;
	public PointSet(Point...points) {
		this.points=Arrays.asList(points);
	}
	public void drawUVs(List<TexturedUV> uvs,GuiGraphics stack,int x,int y) {
		for(MutablePair<TexturedUV, Point> p:IterateUtils.joinAnd(uvs, points)){
			p.getFirst().blit(stack, x, y, p.getSecond());
		}
	}
	public void drawUVs(List<UV> uvs,GuiGraphics stack,ResourceLocation texture,int x,int y) {
		for(MutablePair<UV, Point> p:IterateUtils.joinAnd(uvs, points)){
			p.getFirst().blit(stack,texture, x, y, p.getSecond());
		}
	}
}
