package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.data.TeamResearchData;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.ForgeRegistries;

public class KillClue extends ListenerClue {
    EntityType<?> type;

    public KillClue(EntityType<?> t, float contribution) {
        super("@clue." + FHMain.MODID + ".kill", "@" + t.getTranslationKey(), "", contribution);
    }


    public KillClue(JsonObject jo) {
        super(jo);
        type = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(jo.get("entity").getAsString()));
    }

    public KillClue(PacketBuffer pb) {
        super(pb);
        type = pb.readRegistryIdUnsafe(ForgeRegistries.ENTITIES);
    }

    KillClue() {
        super();
    }

    @Override
    public ITextComponent getName() {
        if (name != null && !name.isEmpty())
            return super.getName();
        return GuiUtils.translate("clue." + FHMain.MODID + ".kill");
    }

    @Override
    public ITextComponent getDescription() {
        ITextComponent itc = super.getDescription();
        if (itc != null || type == null) return itc;
        return type.getName();
    }

    @Override
    public void initListener(Team t) {
        ResearchListeners.getKillClues().add(this, t);
    }

    @Override
    public void removeListener(Team t) {
        ResearchListeners.getKillClues().remove(this, t);
    }

    @Override
    public String getId() {
        return "kill";
    }

    public boolean isCompleted(TeamResearchData trd, LivingEntity e) {
        if (type != null && type.equals(e.getType())) {
            this.setCompleted(trd, true);
            return true;
        }
        return false;
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.addProperty("entity", type.getRegistryName().toString());
        return jo;
    }


    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeRegistryIdUnsafe(ForgeRegistries.ENTITIES, type);
    }



}
