package com.teammoeg.frostedheart.scenario.runner.target;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

import net.minecraft.nbt.CompoundNBT;

public class ExecuteStackElement extends ScenarioTarget{
	private final int nodeNum;
	public ExecuteStackElement(String name, int nodeNum) {
		super(name);
		this.nodeNum = nodeNum;
	}
	public ExecuteStackElement(Scenario sc, int nodeNum) {
		super(sc);
		this.nodeNum = nodeNum;
	}
	public ExecuteStackElement(CompoundNBT n) {
		this(n.getString("storage"),n.getInt("node"));
	}
	@Override
	public void accept(ScenarioConductor runner) {
		super.accept(runner);
		runner.gotoNode(nodeNum);
	}
	public CompoundNBT save() {
		CompoundNBT nbt=new CompoundNBT();
		nbt.putString("storage", getName());
		nbt.putInt("node", nodeNum);
		return nbt;
	}

}