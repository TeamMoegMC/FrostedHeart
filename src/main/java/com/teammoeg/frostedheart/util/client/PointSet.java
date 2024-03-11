package com.teammoeg.frostedheart.util.client;

import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.util.IterateUtils;
import com.teammoeg.frostedheart.util.utility.MutablePair;

public class PointSet {
	List<Point> points;
	public PointSet(Point...points) {
		this.points=Arrays.asList(points);
	}
	public void drawUVs(List<UV> uvs,MatrixStack stack,int x,int y) {
		for(MutablePair<UV, Point> p:IterateUtils.joinAnd(uvs, points)){
			p.getFirst().blit(stack, x, y, p.getSecond());
		}
	}
}
