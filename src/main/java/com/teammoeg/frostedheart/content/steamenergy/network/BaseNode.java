package com.teammoeg.frostedheart.content.steamenergy.network;

import java.util.ArrayList;
import java.util.LinkedList;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class BaseNode {
	public LinkedList<BlockPos> route=new LinkedList<>();
	private BranchNode parent;
	SteamEnergyNetwork network;


	public BaseNode(BranchNode parent, SteamEnergyNetwork network) {
		super();
		this.parent = parent;
		this.network = network;
	}


	public void drop() {
		network.tiles.remove(route.getLast());
	}
	public void regist() {
		network.tiles.add(route.getLast());
	}
	public void remove() {
		for(BlockPos bp:route)
			network.
	}
	public void breakAt(BlockPos pos) {
		ArrayList<BlockPos> fore=new ArrayList<>(route.size());
		while(true) {
			BlockPos bp=route.pollFirst();
			if(bp.equals(pos)) {
				break;
			}
			fore.add(pos);
		}
		//create new node
		BranchNode bn=new BranchNode(parent,network);
		for(BlockPos pos2:fore)
			network.setPosNode(pos2, bn);
		bn.route.addAll(fore);
		bn.children.add(this);
		//replace parent to new node
		parent.children.remove(this);
		parent.children.add(bn);
		parent=bn;
	}
	protected void appendEndConnection(BlockPos pos,BlockPos topos) {
		drop();
		route.add(pos);
		regist();
	}
	protected void appendMiddleConnection(BlockPos pos,BlockPos topos) {
		breakAt(topos);
		parent.appendEndConnection(pos, topos);
	}
	public void appendConnection(BlockPos pos,BlockPos topos) {
		if(topos.equals(route.getLast())) {
			appendEndConnection(pos,topos);
		}else {
			appendMiddleConnection(pos,topos);
		}
		
	}
	protected void reduceEndConnection(BlockPos pos) {
		drop();
		route.pollLast();
		regist();
	}
	protected void reduceMiddleConnection(BlockPos pos) {
		drop();
		while(true) {
			if(pos.equals(route.pollLast()))
				break;
		}
		regist();
	}
	public void reduceConnection(BlockPos pos) {
		if(pos.equals(route.getLast())) {
			reduceEndConnection(pos);
		}else {
			reduceMiddleConnection(pos);
		}
		
	}
}
