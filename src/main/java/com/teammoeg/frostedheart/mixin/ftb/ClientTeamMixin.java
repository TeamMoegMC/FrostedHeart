package com.teammoeg.frostedheart.mixin.ftb;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.util.IFTBSecondWritable;

import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.TeamBase;
import dev.ftb.mods.ftbteams.data.TeamType;
import net.minecraft.network.PacketBuffer;

@Mixin(ClientTeam.class)
public abstract class ClientTeamMixin extends TeamBase implements IFTBSecondWritable{
	@Shadow(remap=false)
	protected TeamType type;
	@Override
	public void write2(PacketBuffer buffer, long now) {
		buffer.writeUniqueId(getId());
		buffer.writeByte(type.ordinal());
		properties.write(buffer);
		buffer.writeVarInt(0);
		buffer.writeCompoundTag(((TeamBaseAccess)(Object)(this)).extraData);
	}
}
