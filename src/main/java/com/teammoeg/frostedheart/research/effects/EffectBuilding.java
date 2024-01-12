/*
 * Copyright (c) 2022-2024 TeamMoeg
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
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.items.IEItems;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.gui.ManualScreen;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
        String multiblock = raw.substring(raw.indexOf('/') + 1);
        String key = "block." + namespace + "." + multiblock;
        ar.add(new TranslationTextComponent(key));
        return ar;
    }

    @Override
    public String getBrief() {
        return "Build " + multiblock.getUniqueName();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onClick() {
        if (this.isGranted() && ClientUtils.getPlayer().inventory.hasAny(ImmutableSet.of(IEItems.Tools.manual))) {
            ResourceLocation loc = multiblock.getUniqueName();
            ResourceLocation manual = new ResourceLocation(loc.getNamespace(), loc.getPath().substring(loc.getPath().lastIndexOf("/") + 1));
            ManualScreen screen = ManualHelper.getManual().getGui();
            ManualEntry entry = ManualHelper.getManual().getEntry(manual);
            if (entry != null) {

                ClientUtils.mc().displayGuiScreen(screen);
                //System.out.println(manual);
                screen.setCurrentNode(entry.getTreeNode());
                screen.page = 0;
                screen.fullInit();
            }
        }
    }

    @Override
    public void reload() {
        multiblock = MultiblockHandler.getByUniqueName(multiblock.getUniqueName());
    }

}
