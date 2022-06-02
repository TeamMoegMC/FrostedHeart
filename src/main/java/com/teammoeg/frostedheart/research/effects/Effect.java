package com.teammoeg.frostedheart.research.effects;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.research.AutoIDItem;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * "Effect" of an research: how would it becomes when a research is completed ?
 * 
 * */
public abstract class Effect extends AutoIDItem implements Writeable{
	String name="";
	List<String> tooltip;
	
	FHIcon icon;
	//Init globally
	public abstract void init();
	public abstract boolean grant(TeamResearchData team, PlayerEntity triggerPlayer);
	/**
	 * This is not necessary to implement as this is just for debugging propose
	 * */
	public abstract void revoke(TeamResearchData team);
	public Effect(JsonObject jo) {
		if(jo.has("name"))
		name=jo.get("name").getAsString();
		tooltip=SerializeUtil.parseJsonElmList(jo.get("tooltip"),JsonElement::getAsString);
		icon=FHIcons.getIcon(jo.get("icon"));
		
	}
	public Effect(PacketBuffer pb) {
		name=pb.readString();
		tooltip=SerializeUtil.readList(pb,PacketBuffer::readString);
		icon=FHIcons.readIcon(pb);
	}
	public Effect(String name, List<String> tooltip,FHIcon icon) {
		super();
		this.name = name;
		this.tooltip = tooltip;
		this.icon = icon;
	}
	public Effect(String name, List<String> tooltip,ItemStack icon) {
		this(name,tooltip,FHIcons.getIcon(icon));
	}
	public Effect(String name, List<String> tooltip,IItemProvider icon) {
		this(name,tooltip,FHIcons.getIcon(icon));
	}
	public Effect(String name, List<String> tooltip) {
		this(name,tooltip,FHIcons.nop());
	}
	public FHIcon getIcon() {
		return icon;
	}

	public IFormattableTextComponent getName() {
		return (IFormattableTextComponent) FHTextUtil.get(name,this::getLId);
	}

	public List<ITextComponent> getTooltip() {
		return FHTextUtil.get(tooltip,this::getLId);
	}
	public abstract String getId();
	public abstract int getIntID();
	@Override
	public JsonObject serialize() {
		JsonObject jo=new JsonObject();
		jo.addProperty("type",getId());
		jo.addProperty("name",name);
		jo.add("tooltip",SerializeUtil.toJsonStringList(tooltip,e->e));
		if(icon!=null)
			jo.add("icon",icon.serialize());
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeVarInt(getIntID());
		buffer.writeString(name);
		SerializeUtil.writeList2(buffer,tooltip,PacketBuffer::writeString);
		SerializeUtil.writeOptional(buffer, icon,FHIcon::write);
	}
	@Override
	public String getType() {
		return "effects";
	}
}
