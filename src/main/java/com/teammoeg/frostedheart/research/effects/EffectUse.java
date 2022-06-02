package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchGlobals;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows the research team to use certain machines
 */
public class EffectUse extends Effect {

    List<Block> blocksToUse;
    FHIcon iicons;
    public EffectUse(Block... blocks) {
    	super("@gui." + FHMain.MODID + ".effect.use",new ArrayList<>());
        blocksToUse = new ArrayList<>();
        for (Block b : blocks) {
            blocksToUse.add(b);
            tooltip.add("@"+b.getTranslationKey());
        }
        initIcons();
    }
    private void initIcons() {
    	iicons=FHIcons.getIcon(blocksToUse.toArray(new Block[0]));
    }
    public EffectUse(JsonObject jo) {
    	super(jo);
    	blocksToUse=SerializeUtil.parseJsonElmList(jo.get("blocks"),e->ForgeRegistries.BLOCKS.getValue(new ResourceLocation(e.getAsString())));
    }
    public EffectUse(PacketBuffer pb) {
		super(pb);
		blocksToUse=SerializeUtil.readList(pb, p->p.readRegistryIdUnsafe(ForgeRegistries.BLOCKS));
		
	}
	@Override
    public void init() {
    	ResearchGlobals.block.addAll(blocksToUse);
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	team.block.addAll(blocksToUse);
		return true;
    }

    @Override
    public void revoke(TeamResearchData team) {
    	team.block.removeAll(blocksToUse);
    }


	@Override
	public String getId() {
		return "use";
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo=super.serialize();
		jo.add("blocks",SerializeUtil.toJsonStringList(blocksToUse,Block::getRegistryName));
		return jo;
	}
	@Override
	public FHIcon getIcon() {
		if(super.getIcon()!=FHIcons.nop())
			return super.getIcon();
		return iicons;
	}
	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		SerializeUtil.writeList(buffer, blocksToUse,(b,p)->p.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS,b));
	}
	@Override
	public int getIntID() {
		return 5;
	}
}
