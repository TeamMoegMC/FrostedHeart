package com.teammoeg.frostedheart.content.scenario.runner.trigger;

import com.teammoeg.frostedheart.content.scenario.runner.target.SingleExecuteTargetTrigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class MovementTrigger extends SingleExecuteTargetTrigger {
	Vec3 pos;
	public MovementTrigger(Player pe) {
		super(null);
		this.test=t->t.player().position().distanceToSqr(this.pos)>0.25;
		pos=pe.position();
	}

}
