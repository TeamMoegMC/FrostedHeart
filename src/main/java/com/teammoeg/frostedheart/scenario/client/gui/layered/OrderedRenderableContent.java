package com.teammoeg.frostedheart.scenario.client.gui.layered;

public abstract class OrderedRenderableContent implements RenderableContent {
	int z,order;
	public OrderedRenderableContent() {
	}
	public OrderedRenderableContent(int z) {
		this.z=z;
	}

	public final int getZ() {
		// TODO Auto-generated method stub
		return z;
	}

	public final int getOrder() {
		// TODO Auto-generated method stub
		return order;
	}

	public final void setOrder(int value) {
		this.order=value;
	}
	public final void setZ(int z) {
		this.z = z;
	}


}
