package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * "Effect" of an research: how would it becomes when a research is completed ?
 * 
 * */
public abstract class Effect implements Writeable{
	TranslationTextComponent name;
	List<TranslationTextComponent> tooltip;

	FHIcon icon;
	//Init globally
	public abstract void init();
	public abstract void grant(TeamResearchData team, PlayerEntity triggerPlayer);
	/**
	 * This is not necessary to implement as this is just for debugging propose
	 * */
	public abstract void revoke(TeamResearchData team);
	public Effect(JsonObject jo) {
		name=new TranslationTextComponent(jo.get("name").getAsString());
		tooltip=SerializeUtil.parseJsonElmList(jo.get("tooltip"),e->new TranslationTextComponent(e.getAsString()));
		icon=FHIcons.getIcon(jo.get("icon"));
		
	}
	public Effect(PacketBuffer pb) {
		name=new TranslationTextComponent(pb.readString());
		tooltip=SerializeUtil.readList(pb,b->new TranslationTextComponent(b.readString()));
		icon=FHIcons.readIcon(pb);
	}
	public Effect(TranslationTextComponent name, List<TranslationTextComponent> tooltip,FHIcon icon) {
		super();
		this.name = name;
		this.tooltip = tooltip;
		this.icon = icon;
	}
	public Effect(TranslationTextComponent name, List<TranslationTextComponent> tooltip,ItemStack icon) {
		this(name,tooltip,FHIcons.getIcon(icon));
	}
	public Effect(TranslationTextComponent name, List<TranslationTextComponent> tooltip,IItemProvider icon) {
		this(name,tooltip,FHIcons.getIcon(icon));
	}
	public Effect(TranslationTextComponent name, List<TranslationTextComponent> tooltip) {
		this(name,tooltip,FHIcons.nop());
	}
	public FHIcon getIcon() {
		return icon;
	}

	public IFormattableTextComponent getName() {
		return name;
	}

	public List<TranslationTextComponent> getTooltip() {
		return tooltip;
	}
	public abstract String getId();
	@Override
	public JsonObject serialize() {
		JsonObject jo=new JsonObject();
		jo.addProperty("type",getId());
		jo.addProperty("name",name.getKey());
		jo.add("tooltip",SerializeUtil.toJsonList(tooltip,p->new JsonPrimitive(p.getKey())));
		if(icon!=null)
			jo.add("icon",icon.serialize());
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeString(getId());
		buffer.writeString(name.getKey());
		SerializeUtil.writeList(buffer,tooltip,(e,p)->p.writeString(e.getKey()));
		SerializeUtil.writeOptional(buffer, icon,FHIcon::write);
	}
	
}
