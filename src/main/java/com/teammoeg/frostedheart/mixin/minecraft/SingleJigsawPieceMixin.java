package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.datafixers.util.Either;
import com.teammoeg.frostedheart.util.StructureUtils;

import net.minecraft.world.gen.feature.jigsaw.JigsawPattern.PlacementBehaviour;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;
import net.minecraft.world.gen.feature.jigsaw.SingleJigsawPiece;

@Mixin(SingleJigsawPiece.class)
public abstract class SingleJigsawPieceMixin extends JigsawPiece {
	protected SingleJigsawPieceMixin(PlacementBehaviour projection) {
		super(projection);
	}
	@Shadow
	protected Either<ResourceLocation, Template> field_236839_c_;
	/**
	 * @author khjxiaogu
	 * @reason auto remake structures
	 */
	@Overwrite
	private Template func_236843_a_(TemplateManager p_236843_1_) {
		Template t=this.field_236839_c_.map(p_236843_1_::getTemplateDefaulted, Function.identity());
		//StructureUtils.handlePalette(t.);
		StructureUtils.handlePalette(((TemplateAccess)t).getBlocks());
		return t;
	}
}
