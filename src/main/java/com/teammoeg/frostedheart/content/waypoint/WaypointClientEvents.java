package com.teammoeg.frostedheart.content.waypoint;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.waypoint.waypoints.Waypoint;
import com.teammoeg.frostedheart.content.wheelmenu.Selection;
import com.teammoeg.frostedheart.content.wheelmenu.WheelMenuSelectionRegisterEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class WaypointClientEvents {

    @SubscribeEvent
    public static void registerSelection(WheelMenuSelectionRegisterEvent event) {
        event.register(FHMain.rl("waypoint/quick_waypoint"), new Selection(Component.translatable("waypoint.frostedheart.quick_waypoint"),
                IconButton.Icon.SIGHT.toCIcon(), s -> {
                        HitResult block = ClientUtils.getPlayer().pick(512, ClientUtils.partialTicks(), false);
                        if (block.getType() == HitResult.Type.BLOCK) {
                            Waypoint waypoint = new Waypoint(((BlockHitResult)block).getBlockPos(), "picked_block", ColorHelper.CYAN);
                            waypoint.focus = true;
                            waypoint.displayName = ClientUtils.getWorld().getBlockState(((BlockHitResult)block).getBlockPos()).getBlock().getName();
                            ClientWaypointManager.putWaypoint(waypoint);
                        }
                }
        ));

        event.register(FHMain.rl("waypoint/del_quick_waypoint"),
                new Selection(Component.translatable("waypoint.frostedheart.del_quick_waypoint"),
                        IconButton.Icon.BOX.toCIcon(),
                        0xFFFFFF,
                        s -> ClientWaypointManager.containsWaypoint("picked_block"),
                        s -> ClientWaypointManager.removeWaypoint("picked_block"),
                        Selection.NO_ACTION
                )
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientWaypointManager.clear();
    }
}
