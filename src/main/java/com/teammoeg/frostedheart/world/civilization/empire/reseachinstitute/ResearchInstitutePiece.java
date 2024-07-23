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

package com.teammoeg.frostedheart.world.civilization.empire.reseachinstitute;

import java.util.Random;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.FHStructures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;


public class ResearchInstitutePiece extends TemplateStructurePiece {
    public final ResourceLocation resource;
    public final Rotation rotation;

    public ResearchInstitutePiece(StructureManager templateManager, BlockPos pos, Rotation rotation) {
        super(FHStructures.OBSERVATORY_PIECE, 0);
        this.resource = new ResourceLocation(FHMain.MODID, "relic/institute");
        this.templatePosition = pos;
        this.rotation = rotation;
        this.loadTemplate(templateManager);
    }

    public ResearchInstitutePiece(StructureManager templateManager, CompoundTag p_i50566_2_) {
        super(FHStructures.OBSERVATORY_PIECE, p_i50566_2_);
        this.resource = new ResourceLocation(p_i50566_2_.getString("Template"));
        this.rotation = Rotation.valueOf(p_i50566_2_.getString("Rot"));
        this.loadTemplate(templateManager);
    }

    @Override
    protected void handleDataMarker(String function, BlockPos pos, ServerLevelAccessor worldIn, Random rand, BoundingBox sbb) {
    }

    private void loadTemplate(StructureManager manager) {
        StructureTemplate template = manager.getOrCreate(this.resource);
        StructurePlaceSettings placementsettings = (new StructurePlaceSettings()).setRotation(this.rotation).setMirror(Mirror.NONE);
        this.setup(template, this.templatePosition, placementsettings);
    }

    protected void addAdditionalSaveData(CompoundTag tagCompound) {
        super.addAdditionalSaveData(tagCompound);
        tagCompound.putString("Template", this.resource.toString());
        tagCompound.putString("Rot", this.rotation.name());
    }
}
