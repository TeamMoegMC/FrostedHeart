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

package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Map;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.util.mixin.StructureUtils;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;

@Mixin(TemplateManager.class)
public abstract class TemplateManagerMixin {
    @Shadow
    private Map<ResourceLocation, Template> templates;

    /**
     * @author khjxiaogu
     * @reason auto remake structures
     */
    @Overwrite
    @Nullable
    public Template getTemplate(ResourceLocation p_200219_1_) {
        return this.templates.computeIfAbsent(p_200219_1_, (p_209204_1_) -> {
            Template template = this.loadTemplateFile(p_209204_1_);

            if (template != null) {
                StructureUtils.handlePalette(((TemplateAccess) template).getBlocks());
                return template;
            }
            template = this.loadTemplateResource(p_209204_1_);
            if (template != null) {
                StructureUtils.handlePalette(((TemplateAccess) template).getBlocks());
                return template;
            }
            return null;
        });
    }

    @Shadow
    @Nullable
    abstract Template loadTemplateFile(ResourceLocation locationIn);

    @Shadow
    @Nullable
    abstract Template loadTemplateResource(ResourceLocation p_209201_1_);
}
