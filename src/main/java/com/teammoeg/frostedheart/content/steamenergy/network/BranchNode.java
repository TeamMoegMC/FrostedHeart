package com.teammoeg.frostedheart.content.steamenergy.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class BranchNode extends BaseNode{
	List<BaseNode> children=new ArrayList<>();

	public BranchNode(BranchNode parent, SteamEnergyNetwork network) {
		super(parent, network);
	}

	@Override
	public void drop() {
		for(BaseNode bn:children)
			bn.drop();
	}

	@Override
	public void regist() {
		for(BaseNode bn:children)
			bn.regist();
	}

	@Override
	protected void reduceEndConnection(BlockPos pos) {
		for(BaseNode c:children) {
			c.drop();
		}
		children.clear();
		route.pollLast();
	}

	@Override
	protected void reduceMiddleConnection(BlockPos pos) {
		reduceEndConnection(route.peekLast());
		// TODO Auto-generated method stub
		super.reduceMiddleConnection(pos);
	}

	@Override
	protected void appendEndConnection(BlockPos pos, BlockPos topos) {
		for(BaseNode bn:children) {
			if(bn.route.getFirst().equals(pos))
				return;
		}
		BaseNode child=new BaseNode(this,network);
		children.add(child);
		child.route.add(pos);
		child.regist();
	}
}
