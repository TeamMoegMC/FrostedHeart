package com.teammoeg.frostedheart.mixin.client;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightTexture.class)
public class MixinLightTexture {

    public MixinLightTexture() {
    }

    @ModifyVariable(method = "updateLightmap", index = 16, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/vector/Vector3f;apply(Lit/unimi/dsi/fastutil/floats/Float2FloatFunction;)V"))
    public float modifygamma(float val) {
        if (ClientUtils.applyspg)
            return ClientUtils.spgamma;
        return val;
    }
}
