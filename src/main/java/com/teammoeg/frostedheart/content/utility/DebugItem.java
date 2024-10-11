package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.Waypoint;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

public class DebugItem extends Item {
    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if (worldIn.isClientSide) {
            TipDisplayManager.openDebugScreen();
        } else {
            Random random = new Random();
            String uuid = UUID.randomUUID().toString();
            Waypoint waypoint = new Waypoint(new Vec3((random.nextFloat()-0.5F)*256, (random.nextFloat()-0.5F)*128+128, (random.nextFloat()-0.5F)*256), uuid, FHColorHelper.CYAN);
            WaypointManager.getManager((ServerPlayer) playerIn).putWaypoint(waypoint);
        }
        return super.use(worldIn, playerIn, handIn);
    }
}
