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

package com.teammoeg.frostedheart.world.civilization.orbit.observatory;

import java.util.Random;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.FHStructures;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.feature.structure.TemplateStructurePiece;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;


public class ObservatoryPiece extends TemplateStructurePiece {
    public final ResourceLocation resource;
    public final Rotation rotation;

    public ObservatoryPiece(TemplateManager templateManager, BlockPos pos, Rotation rotation) {
        super(FHStructures.OBSERVATORY_PIECE, 0);
        this.resource = new ResourceLocation(FHMain.MODID, "relic/observatory");
        this.templatePosition = pos;
        this.rotation = rotation;
        this.loadTemplate(templateManager);
    }

    public ObservatoryPiece(TemplateManager templateManager, CompoundNBT p_i50566_2_) {
        super(FHStructures.OBSERVATORY_PIECE, p_i50566_2_);
        this.resource = new ResourceLocation(p_i50566_2_.getString("Template"));
        this.rotation = Rotation.valueOf(p_i50566_2_.getString("Rot"));
        this.loadTemplate(templateManager);
    }

    @Override
    protected void handleDataMarker(String function, BlockPos pos, IServerWorld worldIn, Random rand, MutableBoundingBox sbb) {
    }

    private void loadTemplate(TemplateManager manager) {
        Template template = manager.getTemplateDefaulted(this.resource);
        PlacementSettings placementsettings = (new PlacementSettings()).setRotation(this.rotation).setMirror(Mirror.NONE);
        this.setup(template, this.templatePosition, placementsettings);
    }

    protected void readAdditional(CompoundNBT tagCompound) {
        super.readAdditional(tagCompound);
        tagCompound.putString("Template", this.resource.toString());
        tagCompound.putString("Rot", this.rotation.name());
    }
}
