/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch.research.effects;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.register.IEItems;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.gui.ManualScreen;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedresearch.ResearchListeners;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.data.TeamResearchData;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows forming multiblock
 */
public class EffectBuilding extends Effect {
    public static final MapCodec<EffectBuilding> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(Effect.BASE_CODEC.forGetter(Effect::getBaseData),
                    ResourceLocation.CODEC.xmap(MultiblockHandler::getByUniqueName, IMultiblock::getUniqueName).fieldOf("multiblock").forGetter(o -> o.multiblock))
            .apply(t, EffectBuilding::new));
    IMultiblock multiblock;

    EffectBuilding() {
        super();
    }

    public EffectBuilding(IETemplateMultiblock s, Block b) {
        super();
        super.icon = CIcons.getIcon(b);
        tooltip.add("@" + b.getDescriptionId());
        multiblock = s;

    }

    public EffectBuilding(BaseData data, IMultiblock multiblock) {
        super(data);
        this.multiblock = multiblock;
    }

    @Override
    public String getBrief() {
        return "Build " + multiblock.getUniqueName();
    }

    @Override
    public CIcon getDefaultIcon() {
    	if(multiblock instanceof IETemplateMultiblock mb)
    		return CIcons.getIcon(mb.getBlock());
        return CIcons.getIcon(IEItems.Tools.HAMMER.get());
    }

    @Override
    public MutableComponent getDefaultName() {
        return Lang.translateGui("effect.building");
    }

    @Override
    public List<Component> getDefaultTooltip() {
        ArrayList<Component> ar = new ArrayList<>();
        ar.add(multiblock.getDisplayName());
        return ar;
    }

    public IMultiblock getMultiblock() {
        return multiblock;
    }

    @Override
    public boolean grant(TeamDataHolder team, TeamResearchData trd, Player triggerPlayer, boolean isload) {
        trd.building.add(multiblock);
        return true;

    }


    @Override
    public void init() {
        ResearchListeners.multiblock.add(multiblock);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onClick(ResearchData data) {
        if (data.isEffectGranted(this) && ClientUtils.getPlayer().getInventory().hasAnyOf(ImmutableSet.of(IEItems.Tools.MANUAL.asItem()))) {
            ResourceLocation loc = multiblock.getUniqueName();
            ResourceLocation manual = new ResourceLocation(loc.getNamespace(), loc.getPath().substring(loc.getPath().lastIndexOf("/") + 1));
            ManualScreen screen = ManualHelper.getManual().getGui();
            ManualEntry entry = ManualHelper.getManual().getEntry(manual);
            if (entry != null) {

                ClientUtils.mc().setScreen(screen);
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

    @Override
    public void revoke(TeamResearchData team) {
        team.building.remove(multiblock);
    }

}
