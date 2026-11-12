/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.health;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.teammoeg.caupona.item.StewItem;
import com.teammoeg.caupona.fluid.SoupFluid;
import com.teammoeg.caupona.util.FloatemStack;
import com.teammoeg.caupona.util.StewInfo;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionResolver;
import com.teammoeg.frostedheart.content.health.nutrition.PlayerNutritionState;
import com.teammoeg.frostedheart.content.health.screen.HealthStatMenu;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Actual item use, loaded food recipes, dynamic soups, and the production Finish event. */
@GameTestHolder("frostedheart_nutrition_eating")
@PrefixGameTestTemplate(false)
public final class NutritionEatingGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void realMealsResolveNutritionAndReachThePlayerCapability(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        double oldGain = FHConfig.SERVER.NUTRITION.nutritionGainRate.get();
        double oldLoss = FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.get();
        boolean regeneration = level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        List<Object> results = new ArrayList<>();
        try {
            FHConfig.SERVER.NUTRITION.nutritionGainRate.set(0.12D);
            FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.set(0.03D);
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, level.getServer());
            results.add(meal(helper, "beef-last-item", new ItemStack(Items.COOKED_BEEF), 12, 15));
            results.add(meal(helper, "beef-two-items", new ItemStack(Items.COOKED_BEEF, 2), 12, 15));
            results.add(meal(helper, "beef-one-missing-hunger", new ItemStack(Items.COOKED_BEEF), 19, 15));
            results.add(meal(helper, "carrot-one-missing-hunger", new ItemStack(Items.CARROT), 19, 15));
            results.add(meal(helper, "bread", new ItemStack(Items.BREAD), 12, 15));
            results.add(meal(helper, "apple", new ItemStack(Items.APPLE), 12, 15));
            results.add(meal(helper, "baked-potato", new ItemStack(Items.BAKED_POTATO), 12, 15));
            results.add(meal(helper, "golden-apple-full-hunger", new ItemStack(Items.GOLDEN_APPLE), 20, 15));
            results.add(meal(helper, "beef-at-zero-reserves", new ItemStack(Items.COOKED_BEEF), 12, 0));
            ItemStack soup = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("caupona:meat_soup")));
            helper.assertTrue(soup.getItem() instanceof StewItem, "real Caupona meat soup is registered");
            StewInfo info = new StewInfo(new ArrayList<>(List.of(new FloatemStack(new ItemStack(Items.COOKED_BEEF)))),
                    new ArrayList<>(), 8, 0.8F, new ResourceLocation("minecraft:water"));
            StewItem.setInfo(soup, info);
            results.add(meal(helper, "dynamic-meat-soup-bowl", soup.copy(), 12, 15));
            FluidStack fluid = new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("caupona:meat_soup")), 250);
            SoupFluid.setInfo(fluid, info);
            ItemStack thermos = new ItemStack(FHItems.thermos.get());
            int filled = thermos.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new)
                    .fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 250, "the thermos accepts one real serving of soup");
            results.add(meal(helper, "dynamic-meat-soup-last-thermos-serving", thermos, 12, 15));
            results.add(hungerAndBeefCycle(helper));
        } finally {
            FHConfig.SERVER.NUTRITION.nutritionGainRate.set(oldGain);
            FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.set(oldLoss);
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(regeneration, level.getServer());
            Path output = Path.of("nutrition-eating");
            Files.createDirectories(output);
            Files.writeString(output.resolve("results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(results));
            FHMain.LOGGER.info("NUTRITION_REAL_EATING,{}", new GsonBuilder().create().toJson(results));
        }
        helper.succeed();
    }

    private static Object meal(GameTestHelper helper, String name, ItemStack stack, int hunger, float reserves) {
        try (Fixture fixture = new Fixture(helper)) {
            ServerPlayer player = fixture.player;
            NutritionCapability cap = NutritionCapability.getCapability(player).orElseThrow(IllegalStateException::new);
            PlayerNutritionState before = PlayerNutritionState.uniform(reserves);
            cap.set(before);
            cap.addAttributes(player);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(hunger);
            player.getFoodData().setSaturation(0);
            player.getFoodData().setExhaustion(0);
            var profile = FoodNutritionResolver.resolve(player.level(), stack);
            var properties = stack.getFoodProperties(player);
            helper.assertTrue(!profile.isZero() && properties != null && properties.getNutrition() > 0,
                    name + " has a nonzero loaded nutrition profile and actual food properties");
            var expected = before.afterEating(profile, properties.getNutrition(), hunger, 0.12D);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.isUsingItem(), name + " starts the real item-use lifecycle");
            int ticks = 0;
            while (player.isUsingItem() && ticks < 256) {
                player.tickCount++;
                player.tick();
                player.doTick();
                ticks++;
            }
            helper.assertTrue(!player.isUsingItem(), name + " completes item use");
            helper.assertTrue(player.getFoodData().getFoodLevel() == Math.min(20, hunger + properties.getNutrition()),
                    name + " restores the expected real hunger");
            PlayerNutritionState after = cap.get();
            helper.assertTrue(near(after, expected), name + " reaches nutrition through the actual Forge Finish event: " + after + " vs " + expected);
            cap.addAttributes(player);
            HealthStatMenu menu = new HealthStatMenu(1, player.getInventory());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("case", name);
            row.put("profile", profile);
            row.put("foodPropertiesHunger", properties.getNutrition());
            row.put("hungerBefore", hunger);
            row.put("hungerAfter", player.getFoodData().getFoodLevel());
            row.put("before", before);
            row.put("after", after);
            row.put("expected", expected);
            row.put("maxHealthAfter", player.getMaxHealth());
            row.put("menuProteinEncoded", ((DataSlot) menu.protein).get());
            int vegetableEncoded = ((DataSlot) menu.vegetable).get();
            row.put("menuVegetableEncoded", vegetableEncoded);
            row.put("menuVegetableDisplayed", new java.text.DecimalFormat("0.0%").format(vegetableEncoded / 10000F));
            row.put("remainingHandStack", player.getMainHandItem().toString());
            row.put("elapsedPlayerTicks", ticks);
            return row;
        }
    }

    private static Object hungerAndBeefCycle(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            ServerPlayer player = fixture.player;
            NutritionCapability cap = NutritionCapability.getCapability(player).orElseThrow(IllegalStateException::new);
            PlayerNutritionState before = PlayerNutritionState.uniform(70);
            cap.set(before);
            for (int lost = 0; lost < 8; lost++) {
                player.getFoodData().setSaturation(0);
                player.getFoodData().setExhaustion(4.1F);
                player.tickCount++;
                player.tick();
                player.doTick();
            }
            helper.assertTrue(player.getFoodData().getFoodLevel() == 12, "one meal cycle loses exactly eight hunger");
            PlayerNutritionState afterLoss = cap.get();
            ItemStack beef = new ItemStack(Items.COOKED_BEEF);
            var profile = FoodNutritionResolver.resolve(player.level(), beef);
            player.setItemInHand(InteractionHand.MAIN_HAND, beef);
            beef.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            for (int ticks = 0; player.isUsingItem() && ticks < 256; ticks++) {
                player.tickCount++;
                player.tick();
                player.doTick();
            }
            PlayerNutritionState expected = before.afterHungerLoss(8, 0.03D).afterEating(profile, 8, 12, 0.12D);
            helper.assertTrue(near(cap.get(), expected) && player.getFoodData().getFoodLevel() == 20,
                    "real hunger depletion and eating obey the existing net nutrition formula");
            return Map.of("case", "real-eight-hunger-beef-cycle", "before", before,
                    "afterHungerLoss", afterLoss, "afterMeal", cap.get(), "expected", expected);
        }
    }

    private static boolean near(PlayerNutritionState a, PlayerNutritionState b) {
        return Math.abs(a.fat() - b.fat()) < 1e-4F && Math.abs(a.carbohydrate() - b.carbohydrate()) < 1e-4F
                && Math.abs(a.protein() - b.protein()) < 1e-4F && Math.abs(a.vegetable() - b.vegetable()) < 1e-4F;
    }

    private static final class Fixture implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;

        Fixture(GameTestHelper helper) {
            var level = helper.getLevel();
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "nutrition-meal"));
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            connection.setProtocol(ConnectionProtocol.PLAY);
            level.getServer().getPlayerList().placeNewPlayer(connection, player);
            player.setGameMode(GameType.SURVIVAL);
            player.setInvulnerable(true);
            BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
            player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }

        @Override public void close() {
            player.server.getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        }
    }
}
