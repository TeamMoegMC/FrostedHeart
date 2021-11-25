package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Map;
import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.util.StructureUtils;

import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.util.ResourceLocation;

@Mixin(TemplateManager.class)
public abstract class SingleJigsawPieceMixin {
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
			StructureUtils.handlePalette(((TemplateAccess) template).getBlocks());
			return template != null ? template : this.loadTemplateResource(p_209204_1_);
		});
	}

	@Shadow
	@Nullable
	abstract Template loadTemplateResource(ResourceLocation p_209201_1_);

	@Shadow
	@Nullable
	abstract Template loadTemplateFile(ResourceLocation locationIn);
}
