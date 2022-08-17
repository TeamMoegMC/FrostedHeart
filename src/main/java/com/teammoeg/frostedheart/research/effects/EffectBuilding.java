package com.teammoeg.frostedheart.research.effects;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.items.IEItems;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {

    IMultiblock multiblock;

    public EffectBuilding(IETemplateMultiblock s, Block b) {
        super();
        super.icon = FHIcons.getIcon(b);
        tooltip.add("@" + b.getTranslationKey());
        multiblock = s;

    }

    EffectBuilding() {
        super();
    }

    public EffectBuilding(JsonObject jo) {
        super(jo);
        multiblock = MultiblockHandler.getByUniqueName(new ResourceLocation(jo.get("multiblock").getAsString()));
    }

    public EffectBuilding(PacketBuffer pb) {
        super(pb);
        multiblock = MultiblockHandler.getByUniqueName(pb.readResourceLocation());
    }

    public IMultiblock getMultiblock() {
        return multiblock;
    }

    @Override
    public void init() {
        ResearchListeners.multiblock.add(multiblock);
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer, boolean isload) {
        team.building.add(multiblock);
        return true;

    }

    @Override
    public void revoke(TeamResearchData team) {
        team.building.remove(multiblock);
    }

    @Override
    public String getId() {
        return "multiblock";
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.addProperty("multiblock", multiblock.getUniqueName().toString());
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeResourceLocation(multiblock.getUniqueName());
    }

    @Override
    public int getIntID() {
        return 1;
    }

    @Override
    public FHIcon getDefaultIcon() {
        return FHIcons.getIcon(IEItems.Tools.hammer);
    }

    @Override
    public IFormattableTextComponent getDefaultName() {
        return GuiUtils.translateGui("effect.building");
    }

    @Override
    public List<ITextComponent> getDefaultTooltip() {
        ArrayList<ITextComponent> ar = new ArrayList<>();
        String raw = multiblock.getUniqueName().toString();
        String namespace = raw.substring(0, raw.indexOf(':'));
        String multiblock = raw.substring(raw.indexOf('/')+1);
        String key = "block." + namespace + "." + multiblock;
        ar.add(new TranslationTextComponent(key));
        return ar;
    }

    @Override
    public String getBrief() {
        return "Build " + multiblock.getUniqueName();
    }

}
