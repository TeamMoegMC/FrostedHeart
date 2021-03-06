package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Allows the research team to use certain machines
 */
public class EffectUse extends Effect {

    List<Block> blocks;
    
    EffectUse() {
		super();
		this.blocks = new ArrayList<>();
	}
	public EffectUse(Block... blocks) {
    	super();
        this.blocks = new ArrayList<>();
        for (Block b : blocks) {
            this.blocks.add(b);
        }
    }
    public EffectUse(JsonObject jo) {
    	super(jo);
    	blocks=SerializeUtil.parseJsonElmList(jo.get("blocks"),e->ForgeRegistries.BLOCKS.getValue(new ResourceLocation(e.getAsString())));
    }
    public EffectUse(PacketBuffer pb) {
		super(pb);
		blocks=SerializeUtil.readList(pb, p->p.readRegistryIdUnsafe(ForgeRegistries.BLOCKS));
		
	}
	@Override
    public void init() {
    	ResearchListeners.block.addAll(blocks);
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer, boolean isload) {
    	team.block.addAll(blocks);
		return true;
    }

    @Override
    public void revoke(TeamResearchData team) {
    	team.block.removeAll(blocks);
    }


	@Override
	public String getId() {
		return "use";
	}

	@Override
	public JsonObject serialize() {
		JsonObject jo=super.serialize();
		jo.add("blocks",SerializeUtil.toJsonStringList(blocks,Block::getRegistryName));
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		SerializeUtil.writeList(buffer, blocks,(b,p)->p.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS,b));
	}
	@Override
	public int getIntID() {
		return 5;
	}
	@Override
	public FHIcon getDefaultIcon() {
		return FHIcons.getIcon(FHIcons.getIcon(blocks.toArray(new Block[0])),FHIcons.getIcon(TechIcons.HAND));
	}
	@Override
	public IFormattableTextComponent getDefaultName() {
		return GuiUtils.translateGui("effect.use");
	}
	@Override
	public List<ITextComponent> getDefaultTooltip() {
		List<ITextComponent> tooltip=new ArrayList<>();
		for(Block b:blocks) {
			tooltip.add(b.getTranslatedName());
		}
		
		return tooltip;
	}
	@Override
	public String getBrief() {
		if(blocks.isEmpty())
			return "Use nothing";
		return "Use "+blocks.get(0).getTranslatedName().getString()+(blocks.size()>1?" ...":"");
	}
}
