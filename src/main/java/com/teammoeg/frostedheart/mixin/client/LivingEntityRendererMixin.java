package com.teammoeg.frostedheart.mixin.client;

import com.teammoeg.frostedheart.content.utility.seld.SledEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {

    @Shadow
    protected M model;

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }


    //animations for sled pullers
    //injects in all calls, so it works with optishit code. needs redirect
    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "net/minecraft/world/entity/LivingEntity.isPassenger ()Z"))
    private boolean isPassenger(LivingEntity instance) {
        Entity vehicle = instance.getVehicle();
        if (vehicle instanceof SledEntity sledEntity) {
            if (sledEntity.isMyPuller(instance)) {
                this.model.riding = false;
                return false;
            }
        }
        return instance.isPassenger();
    }

}
