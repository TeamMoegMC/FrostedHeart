package com.teammoeg.frostedheart.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.util.IOwnerTile;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TemplateMultiblock.class)
public abstract class TemplateMultiblockMixin implements IMultiblock {
    private ServerPlayerEntity pe;

    public TemplateMultiblockMixin() {
    }

    @Inject(at = @At(value = "INVOKE", target = "Lblusunrize/immersiveengineering/api/multiblocks/TemplateMultiblock;form"), method = "createStructure", remap = false)
    public void fh$on$createStructure(World world, BlockPos pos, Direction side, PlayerEntity player, CallbackInfoReturnable<Boolean> cbi) {
        if (!world.isRemote)
            pe = (ServerPlayerEntity) player;
        else
            pe = null;
    }

    @Inject(at = @At("RETURN"), remap = false, method = "form", locals = LocalCapture.CAPTURE_FAILHARD)
    public void fh$on$form(World world, BlockPos pos, Rotation rot, Mirror mirror, Direction sideHit, CallbackInfo cbi, BlockPos master) {
        if (pe != null)
            IOwnerTile.setOwner(Utils.getExistingTileEntity(world, master), FTBTeamsAPI.getPlayerTeam(pe).getId());
    }
}
