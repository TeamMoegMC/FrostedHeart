package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.DrawDeskIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;

/**
 * Effect on numerical stats of the team's machines or abilities
 */
public class EffectStats extends Effect {
	private static FHIcon addIcon=FHIcons.getIcon(DrawDeskIcons.ADD);
    String name;
    double val;
    public EffectStats(String name,double add) {
    	super(GuiUtils.translateGui("effect.stats"),new ArrayList<>(),addIcon);
    	GuiUtils.translateGui("effect.stats."+name,add);
    	val=add;
    }

    public EffectStats(JsonObject jo) {
    	super(jo);
    	super.icon=addIcon;
    }
    @Override
    public void init() {

    }

    @Override
    public void grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	double var=team.getVariants().getDouble(name);
    	var+=val;
    	team.getVariants().putDouble(name, var);
    }

    @Override
    public void revoke(TeamResearchData team) {
    	double var=team.getVariants().getDouble(name);
    	var-=val;
    	team.getVariants().putDouble(name, var);
    }

	@Override
	public String getId() {
		return "stats";
	}

}
