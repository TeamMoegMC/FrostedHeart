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

package com.teammoeg.frostedheart.content.scenario.client.gui.layered;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class OrderedRenderableContent implements RenderableContent {
	protected int z;
	protected int order;
	public OrderedRenderableContent() {
	}
	public OrderedRenderableContent(int z) {
		this.z=z;
	}

	public final int getZ() {
		return z;
	}

	public final int getOrder() {
		return order;
	}

	public final void setOrder(int value) {
		this.order=value;
	}
	public final void setZ(int z) {
		this.z = z;
	}
	public CompletableFuture<Void> prepare(ExecutorService threadPool){
		return null;
	}

}
