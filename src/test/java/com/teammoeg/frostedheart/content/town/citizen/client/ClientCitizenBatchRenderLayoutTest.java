/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClientCitizenBatchRenderLayoutTest {

	private static final float EPSILON = 0.001f;

	@Test
	void standingAxesKeepSkinFrontForwardAndHeadTopUp() {
		assertStandingAxes(0, 0.0f, 1.0f);
		assertStandingAxes(64, -1.0f, 0.0f);
		assertStandingAxes(128, 0.0f, -1.0f);
		assertStandingAxes(192, 1.0f, 0.0f);
		for (int yaw = 0; yaw < 256; yaw++)
			assertOrthonormal(CitizenBatchRenderLayout.standingAxes(yaw));
	}

	@Test
	void sleepingAxesKeepSkinFrontUpAndHeadTowardBedDirection() {
		assertSleepingAxes(0, 0.0f, 1.0f);
		assertSleepingAxes(64, -1.0f, 0.0f);
		assertSleepingAxes(128, 0.0f, -1.0f);
		assertSleepingAxes(192, 1.0f, 0.0f);
		for (int yaw = 0; yaw < 256; yaw++)
			assertOrthonormal(CitizenBatchRenderLayout.sleepingAxes(yaw));
	}

	private static void assertStandingAxes(int yaw, float forwardX, float forwardZ) {
		CitizenBatchRenderLayout.Axes axes = CitizenBatchRenderLayout.standingAxes(yaw);
		assertVector(axes.xX(), axes.xY(), axes.xZ(), forwardZ, 0.0f, -forwardX);
		assertVector(-axes.yX(), -axes.yY(), -axes.yZ(), 0.0f, 1.0f, 0.0f);
		assertVector(-axes.zX(), -axes.zY(), -axes.zZ(), forwardX, 0.0f, forwardZ);
		assertOrthonormal(axes);
	}

	private static void assertSleepingAxes(int yaw, float forwardX, float forwardZ) {
		CitizenBatchRenderLayout.Axes axes = CitizenBatchRenderLayout.sleepingAxes(yaw);
		assertVector(axes.xX(), axes.xY(), axes.xZ(), forwardZ, 0.0f, -forwardX);
		assertVector(-axes.yX(), -axes.yY(), -axes.yZ(), forwardX, 0.0f, forwardZ);
		assertVector(-axes.zX(), -axes.zY(), -axes.zZ(), 0.0f, 1.0f, 0.0f);
		assertOrthonormal(axes);
	}

	private static void assertOrthonormal(CitizenBatchRenderLayout.Axes axes) {
		assertEquals(1.0f, lengthSquared(axes.xX(), axes.xY(), axes.xZ()), EPSILON);
		assertEquals(1.0f, lengthSquared(axes.yX(), axes.yY(), axes.yZ()), EPSILON);
		assertEquals(1.0f, lengthSquared(axes.zX(), axes.zY(), axes.zZ()), EPSILON);
		assertEquals(0.0f, dot(axes.xX(), axes.xY(), axes.xZ(), axes.yX(), axes.yY(), axes.yZ()), EPSILON);
		assertEquals(0.0f, dot(axes.xX(), axes.xY(), axes.xZ(), axes.zX(), axes.zY(), axes.zZ()), EPSILON);
		assertEquals(0.0f, dot(axes.yX(), axes.yY(), axes.yZ(), axes.zX(), axes.zY(), axes.zZ()), EPSILON);
	}

	private static void assertVector(float actualX, float actualY, float actualZ,
			float expectedX, float expectedY, float expectedZ) {
		assertEquals(expectedX, actualX, EPSILON);
		assertEquals(expectedY, actualY, EPSILON);
		assertEquals(expectedZ, actualZ, EPSILON);
	}

	private static float lengthSquared(float x, float y, float z) {
		return x * x + y * y + z * z;
	}

	private static float dot(float ax, float ay, float az, float bx, float by, float bz) {
		return ax * bx + ay * by + az * bz;
	}
}
