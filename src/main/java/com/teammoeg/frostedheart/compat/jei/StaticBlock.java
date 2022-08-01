package com.teammoeg.frostedheart.compat.jei;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.vector.Vector3f;

//borrowed from create
public class StaticBlock extends AnimatedKinetics {
    BlockState bs;

    public StaticBlock(BlockState bs) {
        this.bs = bs;
    }

    @Override
    public void draw(MatrixStack matrixStack, int xOffset, int yOffset) {
        matrixStack.push();
        matrixStack.translate(xOffset, yOffset, 0);
        matrixStack.translate(0, 0, 200);
        matrixStack.translate(2, 22, 0);
        matrixStack.rotate(Vector3f.XP.rotationDegrees(-15.5f));
        matrixStack.rotate(Vector3f.YP.rotationDegrees(22.5f + 90));
        int scale = 30;

        blockElement(bs)
                .rotateBlock(0, 0, 0)
                .scale(scale)
                .render(matrixStack);

        matrixStack.pop();
    }

}
