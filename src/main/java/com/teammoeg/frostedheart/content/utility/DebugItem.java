package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.content.tips.TipDisplayManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.waypoint;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;

import java.util.Random;
import java.util.UUID;

public class DebugItem extends Item {
    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (worldIn.isRemote) {
            TipDisplayManager.openDebugScreen();
        } else {
            Random random = new Random();
            String uuid = UUID.randomUUID().toString();
            waypoint waypoint = new waypoint(new Vector3f((random.nextFloat()-0.5F)*256, (random.nextFloat()-0.5F)*128+128, (random.nextFloat()-0.5F)*256), uuid, FHColorHelper.CYAN);
            WaypointManager.getManager((ServerPlayerEntity) playerIn).putWaypoint(waypoint);
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }
}
