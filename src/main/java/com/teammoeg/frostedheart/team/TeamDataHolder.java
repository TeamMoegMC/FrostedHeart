package com.teammoeg.frostedheart.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.util.OptionalLazy;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fml.network.PacketDistributor;

public class TeamDataHolder extends BaseDataHolder<TeamDataHolder> {
    UUID id;
    String ownerName;
    private OptionalLazy<Team> team;
	public TeamDataHolder(UUID id,OptionalLazy<Team> team) {
		this.team=team;
		this.id=id;
	}
	@Override
	public void save(CompoundNBT nbt, boolean isPacket) {

		super.save(nbt, isPacket);
        if (ownerName != null)
            nbt.putString("owner", ownerName);
        nbt.putUniqueId("uuid", id);
        team.ifPresent(t->nbt.putUniqueId("teamId", t.getId()));//ftb team id
	}
	@Override
	public void load(CompoundNBT nbt, boolean isPacket) {
		super.load(nbt, isPacket);
		if(nbt.contains("researches")) {
			this.getData(SpecialDataTypes.RESEARCH_DATA).deserialize(nbt, isPacket);
		}
        if (nbt.contains("owner"))
            ownerName = nbt.getString("owner");
        if (nbt.contains("uuid"))
            id = nbt.getUniqueId("uuid");
        //no need to deserialize ftb team
	}
	public void forEachOnline(Consumer<ServerPlayerEntity> consumer) {
        for (ServerPlayerEntity spe : team.get().getOnlineMembers())
        	consumer.accept(spe);
	}
	public void sendToOnline(Object obj) {
        for (ServerPlayerEntity spe : team.get().getOnlineMembers())
        	FHNetwork.send(PacketDistributor.PLAYER.with(()->spe), obj);
	}
    /**
     * Get id.
     * Use this to identify research data as this may transfer across teams.
     *
     * @return team<br>
     */
    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }
    /**
     * Get owner of this storage.
     *
     * @return team<br>
     */
    public Optional<Team> getTeam() {
        if (team == null)
            return Optional.empty();
        return team.resolve();
    }
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setTeam(OptionalLazy<Team> team) {
        this.team = team;
    }
	public List<ServerPlayerEntity> getOnlineMembers() {
		return team.get().getOnlineMembers();
	}
}
