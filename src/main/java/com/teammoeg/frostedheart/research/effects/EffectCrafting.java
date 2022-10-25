/*
 * Copyright (c) 2022 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectCrafting extends Effect {
    List<IRecipe<?>> unlocks = new ArrayList<>();
    ItemStack itemStack = null;
    Item item = null;

    public EffectCrafting(ItemStack item) {
        super();

        this.itemStack = item;
    }

    public EffectCrafting(IItemProvider item) {
        super();
        this.item = item.asItem();
        initItem();
    }

    private void initItem() {
        for (IRecipe<?> r : FHResearchDataManager.getRecipeManager().getRecipes()) {
            if (r.getRecipeOutput().getItem().equals(this.item)) {
                unlocks.add(r);
            }
        }
    }

    private void initStack() {
        for (IRecipe<?> r : FHResearchDataManager.getRecipeManager().getRecipes()) {
            if (r.getRecipeOutput().equals(item)) {
                unlocks.add(r);
            }
        }
    }

    public EffectCrafting(ResourceLocation recipe) {
        super("@gui." + FHMain.MODID + ".effect.crafting", new ArrayList<>());
        Optional<? extends IRecipe<?>> r = FHResearchDataManager.getRecipeManager().getRecipe(recipe);

        if (r.isPresent()) {
            unlocks.add(r.get());
        }
    }

    public void setList(Collection<String> ls) {
        unlocks.clear();
        for (String s : ls) {
            Optional<? extends IRecipe<?>> r = FHResearchDataManager.getRecipeManager().getRecipe(new ResourceLocation(s));

            if (r.isPresent()) {
                unlocks.add(r.get());
            }
        }
    }

    public EffectCrafting(JsonObject jo) {
        super(jo);
        if (jo.has("item")) {
            JsonElement je = jo.get("item");
            if (je.isJsonPrimitive()) {
                item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(je.getAsString()));
                initItem();
            } else {
                itemStack = SerializeUtil.fromJson(je);
                initStack();
            }
        } else if (jo.has("recipes")) {
            unlocks = SerializeUtil.parseJsonElmList(jo.get("recipes"), e -> FHResearchDataManager.getRecipeManager().getRecipe(new ResourceLocation(e.getAsString())).orElse(null));
            unlocks.removeIf(Objects::isNull);
        }
    }

    public EffectCrafting(PacketBuffer pb) {
        super(pb);
        item = SerializeUtil.readOptional(pb, p -> p.readRegistryIdUnsafe(ForgeRegistries.ITEMS)).orElse(null);
        if (item == null) {
            itemStack = SerializeUtil.readOptional(pb, PacketBuffer::readItemStack).orElse(null);
            if (itemStack == null) {
                unlocks = SerializeUtil.readList(pb, p -> FHResearchDataManager.getRecipeManager().getRecipe(p.readResourceLocation()).orElse(null));
                unlocks.removeIf(Objects::isNull);
            } else initStack();
        } else initItem();
    }

    EffectCrafting() {
    }

    @Override
    public void init() {
        ResearchListeners.recipe.addAll(unlocks);
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer, boolean isload) {
        team.crafting.addAll(unlocks);
        return true;
    }

    @Override
    public void revoke(TeamResearchData team) {
        team.crafting.removeAll(unlocks);
    }


    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        if (item != null)
            jo.addProperty("item", item.getRegistryName().toString());
        else if (itemStack != null)
            jo.add("item", SerializeUtil.toJson(itemStack));
        else if (unlocks.size() == 1)
            jo.addProperty("recipes", unlocks.get(1).getId().toString());
        else if (unlocks.size() > 1)
            jo.add("recipes", SerializeUtil.toJsonStringList(unlocks, IRecipe<?>::getId));
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        SerializeUtil.writeOptional(buffer, item, (o, b) -> b.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, o));
        if (item == null) {
            SerializeUtil.writeOptional2(buffer, itemStack, PacketBuffer::writeItemStack);
            if (itemStack == null)
                SerializeUtil.writeList(buffer, unlocks, (o, b) -> b.writeResourceLocation(o.getId()));
        }
    }

    @Override
    public FHIcon getDefaultIcon() {
        if (item != null)
            return FHIcons.getIcon(FHIcons.getIcon(item), FHIcons.getIcon(Items.CRAFTING_TABLE));
        else if (itemStack != null)
            return FHIcons.getIcon(FHIcons.getIcon(itemStack), FHIcons.getIcon(Items.CRAFTING_TABLE));
        else {
            Set<ItemStack> stacks = new HashSet<>();
            for (IRecipe<?> r : unlocks) {
                if (!r.getRecipeOutput().isEmpty()) {
                    stacks.add(r.getRecipeOutput());
                }
            }
            if (!stacks.isEmpty())
                return FHIcons.getIcon(FHIcons.getStackIcons(stacks), FHIcons.getIcon(Items.CRAFTING_TABLE));
        }
        return FHIcons.getIcon(FHIcons.getIcon(TechIcons.Question), FHIcons.getIcon(Items.CRAFTING_TABLE));
    }

    @Override
    public IFormattableTextComponent getDefaultName() {
        return GuiUtils.translateGui("effect.crafting");
    }

    @Override
    public List<ITextComponent> getDefaultTooltip() {
        List<ITextComponent> tooltip = new ArrayList<>();

        if (item != null)
            tooltip.add(new TranslationTextComponent(item.getTranslationKey()));
        else if (itemStack != null)
            tooltip.add(itemStack.getDisplayName());
        else {
            Set<ItemStack> stacks = new HashSet<>();
            for (IRecipe<?> r : unlocks) {
                if (!r.getRecipeOutput().isEmpty()) {
                    stacks.add(r.getRecipeOutput());
                }
            }
            if (stacks.isEmpty())
                tooltip.add(GuiUtils.translateGui("effect.recipe.error"));
            else
                for (ItemStack is : stacks) {
                    tooltip.add(is.getDisplayName());
                }
        }

        return tooltip;
    }

    @Override
    public String getBrief() {
        if (item != null)
            return "Craft " + new TranslationTextComponent(item.getTranslationKey()).getString();
        if (itemStack != null)
            return "Craft " + itemStack.getDisplayName().getString();
        if (!unlocks.isEmpty())
            return "Craft" + unlocks.get(0).getId() + (unlocks.size() > 1 ? " ..." : "");
        return "Craft nothing";
    }

}
