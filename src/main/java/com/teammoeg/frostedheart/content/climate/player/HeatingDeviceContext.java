package com.teammoeg.frostedheart.content.climate.player;

import java.util.EnumMap;

import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class HeatingDeviceContext {
	public static class BodyPartContext{
		@Getter
		private float bodyTemperature;
		@Getter
		@Setter
		private float effectiveTemperature;
		private BodyPartContext(float bodyTemperature, float effectiveTemperature) {
			super();
			this.bodyTemperature = bodyTemperature;
			this.effectiveTemperature = effectiveTemperature;
		}
		private BodyPartContext() {
			super();
		}
		
	}
	@Getter
	ServerPlayer player;
	public static EnumMap<BodyPart,BodyPartContext> partData=new EnumMap<>(BodyPart.class);
	HeatingDeviceContext(ServerPlayer player) {
		super();
		this.player=player;
		for(BodyPart bp:BodyPart.values()) {
			partData.put(bp, new BodyPartContext());
		}
	}
	public BodyPartContext getPartData(BodyPart part) {
		return partData.get(part);
	}
	public void setPartData(BodyPart part,float bodyTemperature,float effectiveTemperature) {
		BodyPartContext ctx=getPartData(part);
		ctx.bodyTemperature=bodyTemperature;
		ctx.effectiveTemperature=effectiveTemperature;
	}
	public float getEffectiveTemperature(BodyPart part) {
		return getPartData(part).effectiveTemperature;
	}
	public float getBodyTemperature(BodyPart part) {
		return getPartData(part).bodyTemperature;
	}
	public void setEffectiveTemperature(BodyPart part,float value) {
		getPartData(part).effectiveTemperature=value;
	}
	public void addEffectiveTemperature(BodyPart part,float value) {
		getPartData(part).effectiveTemperature+=value;
	}
	public Level getLevel() {
		return player.level();
	}
}
