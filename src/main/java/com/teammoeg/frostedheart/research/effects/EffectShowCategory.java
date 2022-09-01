package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows the research team to use certain machines
 */
public class EffectShowCategory extends Effect {

    ResourceLocation cate;

    EffectShowCategory() {
        super();
    }

    public EffectShowCategory(ResourceLocation cat) {
        super();
        cate = cat;
    }

    public EffectShowCategory(JsonObject jo) {
        super(jo);
        cate = new ResourceLocation(jo.get("category").getAsString());
    }

    public EffectShowCategory(PacketBuffer pb) {
        super(pb);
        cate=pb.readResourceLocation();

    }

    @Override
    public void init() {
        ResearchListeners.categories.add(cate);
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer, boolean isload) {
        team.categories.add(cate);
        return true;
    }

    @Override
    public void revoke(TeamResearchData team) {
        team.categories.remove(cate);
    }


    @Override
    public String getId() {
        return "category";
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.addProperty("category", cate.toString());
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeResourceLocation(cate);
    }

    @Override
    public int getIntID() {
        return 6;
    }

    @Override
    public FHIcon getDefaultIcon() {
        return FHIcons.getIcon(Blocks.CRAFTING_TABLE);
    }

    @Override
    public IFormattableTextComponent getDefaultName() {
        return GuiUtils.translateGui("effect.category");
    }

    @Override
    public List<ITextComponent> getDefaultTooltip() {
        List<ITextComponent> tooltip = new ArrayList<>();
        return tooltip;
    }

    @Override
    public String getBrief() {
        return "JEI Category " + cate.toString();
    }
}
