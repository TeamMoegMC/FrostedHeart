package com.teammoeg.frostedheart.research.effects;

import java.text.NumberFormat;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.TechIcons;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;

/**
 * Effect on numerical stats of the team's machines or abilities
 */
public class EffectStats extends Effect {
	private static FHIcon addIcon=FHIcons.getIcon(TechIcons.ADD);
    String name;
    double val;
    boolean isPercentage=false;
    public EffectStats(String name,double add) {
    	super("@gui." + FHMain.MODID + ".effect.stats",new ArrayList<>(),FHIcons.nop());
    	tooltip.add("@gui." + FHMain.MODID + ".effect.stats."+name);
    	val=add;
    	String vtext;
    	if(isPercentage) {
    		vtext=NumberFormat.getPercentInstance().format(val);
    	}else
    		vtext=NumberFormat.getInstance().format(val);
    	if(val>0) {
    		tooltip.add("+"+vtext);
    	}else
    		tooltip.add(vtext);
    	
    	
    }

    public EffectStats(JsonObject jo) {
    	super(jo);
    	name=jo.get("name").getAsString();
    	val=jo.get("val").getAsDouble();
    	if(jo.has("percent"))
    		isPercentage=jo.get("percent").getAsBoolean();
    }
    public EffectStats(PacketBuffer pb) {
		super(pb);
		name=pb.readString();
		val=pb.readDouble();
		isPercentage=pb.readBoolean();
	}

	@Override
    public void init() {

    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	double var=team.getVariants().getDouble(name);
    	var+=val;
    	team.getVariants().putDouble(name, var);
		return true;
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

	@Override
	public FHIcon getIcon() {
		if(super.getIcon()==FHIcons.nop())
			return addIcon;
		return super.getIcon();
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo=super.serialize();
		jo.addProperty("name",name);
		jo.addProperty("val",val);
		if(isPercentage)
			jo.addProperty("percent",true);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		buffer.writeString(name);
		buffer.writeDouble(val);
		buffer.writeBoolean(isPercentage);
	}

	@Override
	public int getIntID() {
		return 4;
	}

}
