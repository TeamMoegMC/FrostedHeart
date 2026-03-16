package com.teammoeg.chorda.client;

import org.joml.Matrix4f;

/**
 * 类CSS变换工具类，提供类似CSS transform属性的矩阵变换操作，用于游戏渲染。
 * 支持skew（倾斜）、perspective（透视）、rotate（旋转）和scale（缩放）等变换。
 * <p>
 * CSS-like transform utility class providing matrix transformation operations similar to
 * CSS transform properties for game rendering. Supports skew, perspective, rotate,
 * and scale transformations.
 */
public class CSSStylingUtil {

	private CSSStylingUtil() {
	}

	public static Matrix4f skewY(float degrees) {
		Matrix4f matrix=new Matrix4f();
		matrix.m01((float) Math.tan(Math.toRadians(degrees)));
		return matrix;
	}
	public static Matrix4f skewX(float degrees) {
		Matrix4f matrix=new Matrix4f();
		matrix.m10((float) Math.tan(Math.toRadians(degrees)));
		return matrix;
	}
	public static Matrix4f perspective(float distance) {
		Matrix4f matrix=new Matrix4f();
		matrix.m32(-1f/distance);
		return matrix;
	}
	public static Matrix4f rotate(float degrees) {
		Matrix4f matrix=new Matrix4f();
		matrix.rotateZ((float) Math.toRadians(degrees));
		return matrix;
	}
	public static Matrix4f rotateX(float degrees) {
		Matrix4f matrix=new Matrix4f();
		matrix.rotateX((float) Math.toRadians(degrees));
		return matrix;
	}
	public static Matrix4f rotateY(float degrees) {
		Matrix4f matrix=new Matrix4f();
		matrix.rotateY((float) Math.toRadians(degrees));
		return matrix;
	}
	public static Matrix4f rotateZ(float degrees) {
		Matrix4f matrix=new Matrix4f();
		matrix.rotateZ((float) Math.toRadians(degrees));
		return matrix;
	}
	public static Matrix4f rotate3d(float x,float y,float z,float degrees) {
		Matrix4f matrix=new Matrix4f();
		matrix.rotation((float) Math.toRadians(degrees),x,y,z);
		return matrix;
	}
	public static Matrix4f scale(float x,float y) {
		Matrix4f matrix=new Matrix4f();
		matrix.scale(x,y,1f);
		return matrix;
	}
	public static Matrix4f scale(float x,float y,float z) {
		Matrix4f matrix=new Matrix4f();
		matrix.scale(x,y,z);
		return matrix;
	}
	public static Matrix4f scaleX(float value) {
		Matrix4f matrix=new Matrix4f();
		matrix.scale(value,1,1);
		return matrix;
	}
	public static Matrix4f scaleY(float value) {
		Matrix4f matrix=new Matrix4f();
		matrix.scale(1,value,1);
		return matrix;
	}
	public static Matrix4f scaleZ(float value) {
		Matrix4f matrix=new Matrix4f();
		matrix.scale(1,1,value);
		return matrix;
	}

}
