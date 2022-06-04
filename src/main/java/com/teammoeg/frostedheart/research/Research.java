package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.network.FHResearchProgressSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.clues.Clues;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.effects.Effects;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * Only Definition of research.
 * Part of Research Category {@link ResearchCategory}
 */
public class Research extends FHRegisteredItem implements Writeable {
	private String id;// id of this research
	private FHIcon icon;// icon for this research in term of item
	private ResearchCategory category;
	private HashSet<Supplier<Research>> parents = new HashSet<>();// parent researches
	private HashSet<Supplier<Research>> children = new HashSet<>();// child researches, this is set automatically,
																	// should not set manually.

	private HashSet<Clue> clues = new HashSet<>();// research clues
	private List<IngredientWithSize> requiredItems = new ArrayList<>();
	private List<Effect> effects = new ArrayList<>();// effects of this research
	public String name="";
	public List<String> desc;

	private int points = 2000;// research point

	@SafeVarargs
	public Research(String path, ResearchCategory category, Supplier<Research>... parents) {
		this(path, category, new ItemStack(Items.AIR), parents);
		
	}

	public Research(String id, JsonObject jo) {
		this.id = id;
		if(jo.has("name"))
			name=jo.get("name").getAsString();
		desc=SerializeUtil.parseJsonElmList(jo.get("desc"),JsonElement::getAsString);
		icon = FHIcons.getIcon(jo.get("icon"));
		category = ResearchCategories.ALL.get(new ResourceLocation(jo.get("category").getAsString()));
		parents.addAll(
				SerializeUtil.parseJsonElmList(jo.get("parents"), p -> FHResearch.researches.get(p.getAsString())));
		clues.addAll(SerializeUtil.parseJsonList(jo.get("clues"), Clues::read));
		requiredItems = SerializeUtil.parseJsonElmList(jo.get("ingredients"), IngredientWithSize::deserialize);
		effects = SerializeUtil.parseJsonList(jo.get("effects"), Effects::deserialize);
		points = jo.get("points").getAsInt();
		
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo = new JsonObject();
		if(name.length()>0&&!name.equals("@"))
			jo.addProperty("name",name);
		jo.add("desc",SerializeUtil.toJsonStringList(desc,e->e));
		jo.add("icon", icon.serialize());
		jo.addProperty("category", category.getId().toString());
		jo.add("parents", SerializeUtil.toJsonList(parents, p -> new JsonPrimitive(p.get().getId())));
		jo.add("clues", SerializeUtil.toJsonList(clues,Clue::serialize));
		jo.add("ingredients", SerializeUtil.toJsonList(requiredItems, IngredientWithSize::serialize));
		jo.add("effects", SerializeUtil.toJsonList(effects, Writeable::serialize));
		jo.addProperty("points", points);

		return jo;
	}
	public Research(String id, PacketBuffer data) {
		this.id = id;
		name=data.readString();
		desc=SerializeUtil.readList(data,PacketBuffer::readString);
		icon = FHIcons.readIcon(data);
		category = ResearchCategories.ALL.get(data.readResourceLocation());

		parents.addAll(SerializeUtil.readList(data, p -> FHResearch.researches.get(p.readVarInt())));
		clues.addAll(SerializeUtil.readList(data, Clues::read));
		requiredItems = SerializeUtil.readList(data, IngredientWithSize::read);
		effects = SerializeUtil.readList(data, Effects::deserialize);
		points = data.readVarInt();
	}



	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeString(name);
		SerializeUtil.writeList2(buffer, desc,PacketBuffer::writeString);
		icon.write(buffer);
		buffer.writeResourceLocation(category.getId());
		SerializeUtil.writeList(buffer, parents, (e, p) -> p.writeVarInt(e.get().getRId()));
		SerializeUtil.writeList(buffer, clues,Clue::write);
		SerializeUtil.writeList(buffer, requiredItems, (e, p) -> e.write(p));
		SerializeUtil.writeList(buffer, effects, (e, p) -> e.write(p));
		buffer.writeVarInt(points);
	}

	public Set<Clue> getClues() {
		return clues;
	}

	public void attachClue(Clue cl) {
		clues.add(cl);
	}

	public List<Effect> getEffects() {
		return effects;
	}

	public void attachEffect(Effect... effs) {
		for (Effect effect : effs) {
			effects.add(effect);
		}
	}

	public List<IngredientWithSize> getRequiredItems() {
		return Collections.unmodifiableList(requiredItems);
	}

	public void attachRequiredItem(IngredientWithSize... ingredients) {
		for (IngredientWithSize ingredient : ingredients) {
			requiredItems.add(ingredient);
		}
	}

	@SafeVarargs
	public Research(String id, ResearchCategory category, IItemProvider icon, Supplier<Research>... parents) {
		this(id, category, new ItemStack(icon), parents);
	}

	@SafeVarargs
	public Research(String id, ResearchCategory category, ItemStack icon, Supplier<Research>... parents) {
		this.id = id;
		this.parents.addAll(Arrays.asList(parents));
		this.icon = FHIcons.getIcon(icon);
		this.category = category;
		desc=new ArrayList<>();
	}

	/*
	 * public int getTime() {
	 * return time;
	 * }
	 */

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Supplier<Research> getSupplier() {
		return FHResearch.getResearch(this.getLId());

	}

	public void doIndex() {
		Supplier<Research> objthis = getSupplier();
		for (Supplier<Research> r : this.parents) {
			r.get().populateChild(objthis);
		}
		int i=0;
		for(Effect e:effects) {
			e.addID(this.getLId(),i);
			FHResearch.effects.register(e);
			i++;
		}
		i=0;
		for(Clue c:clues) {
			c.addID(this.getLId(), i);
			FHResearch.clues.register(c);
			i++;
		}
	}

	public void populateChild(Supplier<Research> child) {
		children.add(child);
	}

	public Set<Research> getChildren() {
		return children.stream().map(r -> r.get()).collect(Collectors.toSet());
	}

	public Set<Research> getParents() {
		return parents.stream().map(r -> r.get()).collect(Collectors.toSet());
	}

	@SafeVarargs
	public final void setParents(Supplier<Research>... parents) {
		this.parents.clear();
		this.parents.addAll(Arrays.asList(parents));
	}

	public FHIcon getIcon() {
		return icon;
	}

	public TextComponent getName() {
		return (TextComponent) FHTextUtil.get(name,"research",()-> id + ".name");
	}

	public List<ITextComponent> getDesc() {
		return FHTextUtil.get(desc,"research",()->id + ".desc");
	}

	public ResearchCategory getCategory() {
		return category;
	}

	public int getRequiredPoints() {
		return points;
	}

	@OnlyIn(Dist.CLIENT)
	public int getCurrentPoints() {
		return getData().getTotalCommitted();
	}

	@OnlyIn(Dist.CLIENT)
	public float getProgressFraction() {
		return getData().getProgress();
	}

	public ResearchData getData(Team team) {
		return ResearchDataManager.INSTANCE.getData(team.getId()).getData(this);
	}

	@OnlyIn(Dist.CLIENT)
	public ResearchData getData() {
		return TeamResearchData.getClientInstance().getData(this);
	}

	public void sendProgressPacket(Team team) {
		FHResearchProgressSyncPacket packet = new FHResearchProgressSyncPacket(team.getId(), this);
		for (ServerPlayerEntity spe : team.getOnlineMembers())
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> spe), packet);
	}

	public String toString() {
		return "Research[" + id + "]";
	}

	@Override
	public String getLId() {
		return id.toString();
	}

	public boolean isCompleted(Team t) {
		return getData(t).isCompleted();
	}

	public boolean isUnlocked(Team t) {
		for (Research parent : this.getParents()) {
			if (!parent.getData(t).isCompleted()) {
				return false;
			}
		}
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	public boolean isCompleted() {
		return getData().isCompleted();
	}

	@OnlyIn(Dist.CLIENT)
	public boolean isUnlocked() {
		for (Research parent : this.getParents()) {
			if (!parent.getData().isCompleted()) {
				return false;
			}
		}
		return true;
	}


}
