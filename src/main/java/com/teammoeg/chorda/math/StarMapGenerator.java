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

package com.teammoeg.chorda.math;

import java.util.BitSet;
import java.util.Date;
import java.util.Random;

/**
 * 星图生成器，使用扩散限制聚集(DLA)算法生成星图状的二维图案。
 * 通过多步扩展和粒子聚集生成类似星云的分形结构。
 * <p>
 * Star map generator that uses Diffusion-Limited Aggregation (DLA) algorithm to generate
 * star-map-like 2D patterns. Produces fractal structures resembling nebulae through
 * multi-step expansion and particle aggregation.
 */
public class StarMapGenerator {
	/**
	 * 基于BitSet的二维位图槽，用于存储星图的像素数据。
	 * <p>
	 * A BitSet-based 2D bitmap slot for storing star map pixel data.
	 */
	public static class MapSlot{
		final BitSet set;
		final int width;
		final int height;
		/**
		 * 构造指定宽高的位图槽。
		 * <p>
		 * Constructs a bitmap slot with the specified width and height.
		 *
		 * @param width 宽度 / the width
		 * @param height 高度 / the height
		 */
		public MapSlot(int width,int height){
			super();
			this.set =new BitSet(width*height);
			this.width = width;
			this.height = height;
		}
		/**
		 * 设置指定坐标的像素值。越界时静默忽略。
		 * <p>
		 * Sets the pixel value at the specified coordinates. Silently ignores out-of-bounds coordinates.
		 *
		 * @param x X坐标 / the X coordinate
		 * @param y Y坐标 / the Y coordinate
		 * @param value 像素值 / the pixel value
		 */
		public void set(int x,int y,boolean value) {
			if(x<0||y<0||x>=width||y>=height)return;
			set.set(x+y*width,value);
		}
		/**
		 * 检查坐标是否在位图范围内。
		 * <p>
		 * Checks whether the coordinates are within the bitmap bounds.
		 *
		 * @param x X坐标 / the X coordinate
		 * @param y Y坐标 / the Y coordinate
		 * @return 如果在范围内则返回true / true if within bounds
		 */
		public boolean inRange(int x,int y) {
			if(x<0||y<0||x>=width||y>=height)return false;
			return true;
		}
		/**
		 * 获取指定坐标的像素值。越界时返回false。
		 * <p>
		 * Gets the pixel value at the specified coordinates. Returns false for out-of-bounds coordinates.
		 *
		 * @param x X坐标 / the X coordinate
		 * @param y Y坐标 / the Y coordinate
		 * @return 该位置的像素值 / the pixel value at that position
		 */
		public boolean get(int x,int y) {
			if(x<0||y<0||x>=width||y>=height)return false;
			return set.get(x+y*width);
		}
		/**
		 * 在四个方向上扩展位图，原有数据保持不变。
		 * <p>
		 * Expands the bitmap in four directions, preserving existing data.
		 *
		 * @param east 东（右）侧扩展量 / the east (right) expansion amount
		 * @param north 北（上）侧扩展量 / the north (top) expansion amount
		 * @param west 西（左）侧扩展量 / the west (left) expansion amount
		 * @param south 南（下）侧扩展量 / the south (bottom) expansion amount
		 * @return 扩展后的新位图 / a new expanded bitmap
		 */
		public MapSlot expand(int east,int north,int west,int south) {
			MapSlot map=new MapSlot(east+width+west,north+height+south);
			for(int x=0;x<width;x++)
				for(int y=0;y<height;y++)
					map.set.set(x+east+(y+north)*map.width,set.get(x+y*width));
			return map;
		}
		/**
		 * 按指定宽高居中扩展位图。
		 * <p>
		 * Expands the bitmap centered by the specified width and height.
		 *
		 * @param width 总宽度扩展量 / the total width expansion
		 * @param height 总高度扩展量 / the total height expansion
		 * @return 扩展后的新位图 / a new expanded bitmap
		 */
		public MapSlot expand(int width,int height) {
			int dx=width/2;
			int dy=height/2;
			return expand(dx,dy,width-dx,height-dy);
			
		}
		/**
		 * 将位图扩展到指定的目标尺寸。
		 * <p>
		 * Expands the bitmap to the specified target dimensions.
		 *
		 * @param width 目标宽度 / the target width
		 * @param height 目标高度 / the target height
		 * @return 扩展后的新位图 / a new expanded bitmap
		 */
		public  MapSlot expandTo(int width,int height) {
			return expand(width-this.width,height-this.height);
			
		}
		@Override
		public String toString() {
			StringBuilder sb=new StringBuilder();
			String wstr=width+"";
			int beforew=(width-wstr.length())/2;
			String hstr=height+"";
			int beforeh=(height-hstr.length())/2;
			sb.append("  ");
			for(int i=0;i<width;i++) {
				if(i<beforew) {
					sb.append(" ");
					continue;
				}
				int tidx=i-beforew;
				if(tidx<wstr.length()) {
					sb.append(wstr.charAt(tidx));
					continue;
				}
				sb.append(" ");
			}
			sb.append("\n");
			for(int i=0;i<height;i++) {
				
				if(i<beforeh) {
					sb.append(" ");
				}else {
					int tidx=i-beforeh;
					if(tidx<hstr.length()) {
						sb.append(hstr.charAt(tidx));
					}else {
						sb.append(" ");
					}
				}
				sb.append(" ");
				for(int x=0;x<width;x++) {
					if(set.get(x+i*width)) {
						sb.append("*");
					}else
						sb.append("0");
				}
				sb.append("\n");
			}
			return sb.toString();
		}
		
	}
	private static final int STEP_WIDTH=2;
	private static final int PARTICLE_PER_STEP=8;
	public static void main(String[] args) {
		
		generate(new Date().getTime(),5);
	}
	
	/**
	 * 使用DLA算法生成星图。从中心单点开始，通过多步扩展和粒子聚集生成图案。
	 * <p>
	 * Generates a star map using the DLA algorithm. Starts from a single center point and generates
	 * patterns through multi-step expansion and particle aggregation.
	 *
	 * @param seed 随机种子 / the random seed
	 * @param steps 生成步数，越多图案越大 / number of generation steps, more steps produce larger patterns
	 * @return 生成的星图位图 / the generated star map bitmap
	 */
	public static MapSlot generate(long seed,int steps) {
		MapSlot map=new MapSlot(1,1);
		map.set(0, 0, true);
		Random rnd=new Random(seed);
		for(int i=0;i<steps;i++) {
			map=map.expand(STEP_WIDTH, STEP_WIDTH, STEP_WIDTH, STEP_WIDTH);
			int cparticles=map.width*map.height/PARTICLE_PER_STEP;
			
			for(int j=0;j<cparticles;j++) {
				int x=0;
				int y=0;

				boolean settled=false;
				do {
					if(rnd.nextBoolean()) {
						x=rnd.nextInt(map.width);
						y=rnd.nextBoolean()?0:(map.height-1);
					}else {
						y=rnd.nextInt(map.height);
						x=rnd.nextBoolean()?0:(map.width-1);
						
					}
					//x=(rnd.nextBoolean()?0:(STEP_WIDTH*(i+1)+1))+rnd.nextInt(STEP_WIDTH);
					//y=(rnd.nextBoolean()?0:(STEP_WIDTH*(i+1)+1))+rnd.nextInt(STEP_WIDTH);
					//x=rnd.nextInt(map.width);
					//y=rnd.nextInt(map.height);
				}while(map.get(x, y));
				int is=map.width*2;
				while(--is>0) {
					int cnt=0;
					int cnt1=0;
					if(map.get(x-1, y))
						cnt++;
					if(map.get(x+1, y))
						cnt++;
					if(map.get(x, y-1))
						cnt++;
					if(map.get(x, y+1))
						cnt++;
					if(map.get(x-1, y-1))
						cnt1++;
					if(map.get(x+1, y+1))
						cnt1++;
					if(map.get(x+1, y-1))
						cnt1++;
					if(map.get(x-1, y+1))
						cnt1++;
					if(cnt==1&&cnt1<2) {
						map.set(x, y, true);
						settled=true;
						break;
					}
					int dir=rnd.nextInt(4);
					int dx=0,dy=0;
					switch(dir) {
					case 0:dx=1;break;
					case 1:dx=-1;break;
					case 2:dy=1;break;
					case 3:dy=-1;break;
					}
					
					if(map.inRange(x+dx, y+dy)&&!map.get(x+dx, y+dy)) {
						x+=dx;
						y+=dy;
					}
				}
				if(!settled) {
					j--;
				}else {
					/*
					System.out.println("step "+i+",particle "+j);
					System.out.println(map.expandTo(STEP_WIDTH*steps*2+1, STEP_WIDTH*steps*2+1));
					System.out.flush();
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
				}
			}
			
			//System.out.println(map);
		}
		return map;
	}
}
