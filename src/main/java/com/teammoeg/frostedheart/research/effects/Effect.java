package com.teammoeg.frostedheart.research.effects;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * "Effect" of an research: how would it becomes when a research is completed ?
 * 
 * */
public abstract class Effect implements Writeable{
	TranslationTextComponent name;
	List<TranslationTextComponent> tooltip;

	FHIcon icon;
	public abstract void init();
	public abstract void grant();
	public abstract void revoke();
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

	public Effect(TranslationTextComponent name, List<TranslationTextComponent> tooltip, ItemStack icon) {
		super();
		this.name = name;
		this.tooltip = tooltip;
		this.icon = FHIcons.getIcon(icon);
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
	public abstract ResourceLocation getId();
	@Override
	public JsonObject serialize() {
		JsonObject jo=new JsonObject();
		jo.addProperty("type",getId().toString());
		jo.addProperty("name",name.getKey());
		jo.add("tooltip",SerializeUtil.toJsonList(tooltip,p->new JsonPrimitive(p.getKey())));
		jo.add("icon",icon.serialize());
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeResourceLocation(getId());
		buffer.writeString(name.getKey());
		SerializeUtil.writeList(buffer,tooltip,(e,p)->p.writeString(e.getKey()));
		icon.write(buffer);
	}
	
}
