/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.stratadrill.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.model.BakedMaterialVariants;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillAssembly;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillData;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillMaterials;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.CompositeModel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Bakes V3.7 parts once per resource reload and shares them between every display and working pose. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StrataDrillModels {
    private StrataDrillModels() {}

    private static final ModelResourceLocation INVENTORY =
            new ModelResourceLocation(FHMain.rl("stratadrill"), "inventory");
    private static final ResourceLocation PARTS = FHMain.rl("item/stratadrill/parts");
    private static final Map<Direction, List<BakedQuad>> NO_CULLED_FACES;
    static {
        EnumMap<Direction, List<BakedQuad>> sides = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            sides.put(side, List.of());
        }
        NO_CULLED_FACES = Map.copyOf(sides);
    }

    // Matches the explicit face role emitted by export-stratadrill.py, not an ItemColor index.
    private static final int CUTTER_MARKER = 31;

    private static volatile Generation loaded;

    enum Part {
        STEAM_FIXED("steam_fixed"),
        STEAM_CRANK("steam_crank"),
        STEAM_ROD("steam_rod"),
        STEAM_PISTON("steam_piston"),
        DIESEL_FIXED("diesel_fixed"),
        DIESEL_FLYWHEEL("diesel_flywheel"),
        SPIRAL("spiral"),
        WHEEL_FIXED("wheel_fixed"),
        WHEEL_ROTOR("wheel_rotor");

        final String name;

        Part(String name) {
            this.name = name;
        }
    }

    /** Ready to draw in the stock item path, or as the same parts with rigid working transforms. */
    record AssemblyModels(BakedModel complete, BakedModel fixed, BakedModel head,
                          BakedModel shaft, @Nullable BakedModel rod, @Nullable BakedModel piston,
                          boolean steam, boolean wheel) {}

    @Nullable
    static AssemblyModels get(StrataDrillAssembly assembly, ItemStack stack) {
        Generation baked = loaded;
        return baked == null ? null : baked.resolve(assembly, stack);
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(PARTS);
    }

    @SubscribeEvent
    public static void bake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> baked = event.getModels();
        BakedModel root = baked.get(INVENTORY);
        if (root == null) return;
        if (!(baked.get(PARTS) instanceof CompositeModel.Baked group)) {
            FHMain.LOGGER.warn("Missing or invalid StrataDrill assembly model {}", PARTS);
            return;
        }

        // Unpack the named parts once at bake time; no composite draw path is used.
        EnumMap<Part, BakedModel> parts = new EnumMap<>(Part.class);
        for (Part part : Part.values()) {
            BakedModel model = group.getPart(part.name);
            if (model == null) {
                FHMain.LOGGER.warn("Missing StrataDrill model part {} in {}", part.name, PARTS);
                return;
            }
            parts.put(part, model);
        }

        RenderType type = ForgeRenderTypes.ITEM_LAYERED_SOLID.get();
        RenderTypeGroup renderType = new RenderTypeGroup(RenderType.solid(), type);
        BakedMaterialVariants<BakedModel> spiral = tintable(parts.get(Part.SPIRAL), root, renderType);
        BakedMaterialVariants<BakedModel> wheel = tintable(parts.get(Part.WHEEL_ROTOR), root, renderType);
        AssemblyVariants[] assemblies = new AssemblyVariants[StrataDrillAssembly.values().length];
        for (StrataDrillAssembly assembly : StrataDrillAssembly.values()) {
            BakedModel shaft = parts.get(assembly.steam() ? Part.STEAM_CRANK : Part.DIESEL_FLYWHEEL);
            BakedModel rod = assembly.steam() ? parts.get(Part.STEAM_ROD) : null;
            BakedModel piston = assembly.steam() ? parts.get(Part.STEAM_PISTON) : null;
            BakedMaterialVariants<BakedModel> head = assembly.wheel() ? wheel : spiral;
            // The inventory root already baked the same body OBJ with the authored display metadata.
            BakedModel fixed = combine(root, renderType, root,
                    parts.get(assembly.steam() ? Part.STEAM_FIXED : Part.DIESEL_FIXED),
                    assembly.wheel() ? parts.get(Part.WHEEL_FIXED) : null);
            assemblies[assembly.ordinal()] =
                    new AssemblyVariants(root, renderType, assembly, head, fixed, shaft, rod, piston);
        }
        baked.put(INVENTORY, new AssemblyRoot(root, new Generation(spiral, wheel, assemblies)));
    }

    @SubscribeEvent
    public static void baked(ModelEvent.BakingCompleted event) {
        // Only switch the held path when the model manager has also applied the new atlas.
        loaded = event.getModels().get(INVENTORY) instanceof AssemblyRoot root ? root.generation : null;
    }

    @SuppressWarnings("deprecation")
    private static BakedModel combine(BakedModel reference, RenderTypeGroup type, BakedModel... parts) {
        List<BakedQuad> quads = new ArrayList<>();
        RandomSource random = RandomSource.create(42L);
        for (BakedModel part : parts) {
            if (part == null) continue;
            // The generated OBJ files intentionally have no directional culling.
            quads.addAll(part.getQuads(null, null, random));
        }
        return withQuads(reference, type, List.copyOf(quads));
    }

    private static BakedModel withQuads(BakedModel reference, RenderTypeGroup type, List<BakedQuad> quads) {
        return new SimpleBakedModel(quads, NO_CULLED_FACES, reference.useAmbientOcclusion(),
                reference.usesBlockLight(), true, reference.getParticleIcon(), reference.getTransforms(),
                ItemOverrides.EMPTY, type);
    }

    /** Both power boxes and all displays share these two Chorda geometry caches. */
    @SuppressWarnings("deprecation")
    private static BakedMaterialVariants<BakedModel> tintable(BakedModel source, BakedModel reference,
                                                              RenderTypeGroup type) {
        return new BakedMaterialVariants<>(source.getQuads(null, null, RandomSource.create(42)), CUTTER_MARKER,
                quads -> withQuads(reference, type, quads));
    }

    /** Neutral assembly is built once; rare colors build a full list of references, not mesh copies. */
    private static final class AssemblyVariants {
        private final BakedModel reference;
        private final RenderTypeGroup type;
        private final BakedMaterialVariants<BakedModel> head;
        private final AssemblyModels neutral;
        private final Int2ObjectOpenHashMap<AssemblyModels> byColor = new Int2ObjectOpenHashMap<>();

        AssemblyVariants(BakedModel reference, RenderTypeGroup type, StrataDrillAssembly assembly,
                         BakedMaterialVariants<BakedModel> head, BakedModel fixed, BakedModel shaft,
                         @Nullable BakedModel rod, @Nullable BakedModel piston) {
            this.reference = reference;
            this.type = type;
            this.head = head;
            BakedModel neutralHead = head.resolve(BakedMaterialVariants.WHITE);
            BakedModel complete = new GrippedModel(combine(reference, type, fixed, shaft, rod, piston, neutralHead));
            this.neutral = new AssemblyModels(complete, fixed, neutralHead, shaft, rod, piston,
                    assembly.steam(), assembly.wheel());
        }

        AssemblyModels resolve(int rgb) {
            if (rgb == BakedMaterialVariants.WHITE) return neutral;
            AssemblyModels ready = byColor.get(rgb);
            if (ready != null) return ready;
            BakedModel moving = head.resolve(rgb);
            if (moving == neutral.head()) return neutral;
            BakedModel complete = new GrippedModel(combine(reference, type, neutral.fixed(),
                    neutral.shaft(), neutral.rod(), neutral.piston(), moving));
            ready = new AssemblyModels(complete, neutral.fixed(), moving, neutral.shaft(), neutral.rod(),
                    neutral.piston(), neutral.steam(), neutral.wheel());
            byColor.put(rgb, ready);
            return ready;
        }
    }

    /** Models are scoped to the atlas generation; changing synced palettes invalidates colors only. */
    private static final class Generation {
        private final BakedMaterialVariants<BakedModel> spiral, wheel;
        private final AssemblyVariants[] assemblies;
        private StrataDrillMaterials.Catalog palette = StrataDrillMaterials.clientCatalog();

        Generation(BakedMaterialVariants<BakedModel> spiral, BakedMaterialVariants<BakedModel> wheel,
                   AssemblyVariants[] assemblies) {
            this.spiral = spiral;
            this.wheel = wheel;
            this.assemblies = assemblies;
        }

        AssemblyModels resolve(StrataDrillAssembly assembly, ItemStack stack) {
            StrataDrillMaterials.Catalog active = StrataDrillMaterials.clientCatalog();
            if (active != palette) {
                spiral.clearColors();
                wheel.clearColors();
                for (AssemblyVariants variants : assemblies) variants.byColor.clear();
                palette = active;
            }
            return assemblies[assembly.ordinal()].resolve(active.rgb(StrataDrillData.cutterId(stack)));
        }
    }

    private static final class AssemblyRoot extends BakedModelWrapper<BakedModel> {
        final Generation generation;
        private final ItemOverrides selection = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                StrataDrillAssembly assembly = StrataDrillAssembly.read(stack);
                return assembly == null ? model : generation.resolve(assembly, stack).complete();
            }
        };

        AssemblyRoot(BakedModel root, Generation generation) {
            super(root);
            this.generation = generation;
        }

        @Override
        public ItemOverrides getOverrides() {
            return selection;
        }
    }

    /** Keep V3.7 display transforms except the worn third-person grip, which is anchored to the player model. */
    private static final class GrippedModel extends BakedModelWrapper<BakedModel> {
        GrippedModel(BakedModel base) {
            super(base);
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poses, boolean leftHand) {
            if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                StrataDrillHands.applyThirdPersonGrip(poses, leftHand);
                return this;
            }
            return super.applyTransform(context, poses, leftHand);
        }
    }
}
