package com.teammoeg.frostedheart.content.scenario.runner.target.trigger;

import com.teammoeg.frostedheart.content.scenario.runner.target.SingleExecuteTargetTrigger;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;

public class MovementTrigger extends SingleExecuteTargetTrigger {
	Vector3d pos;
	public MovementTrigger(PlayerEntity pe) {
		super(null);
		this.test=t->t.getPlayer().getPositionVec().squareDistanceTo(this.pos)>0.25;
		pos=pe.getPositionVec();
	}

}
