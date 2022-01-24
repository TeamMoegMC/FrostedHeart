package com.teammoeg.frostedheart.world;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.structure.ObservatoryPiece;
import com.teammoeg.frostedheart.world.structure.ObservatoryStructure;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHStructures {
    public static final IStructurePieceType Observatory_PIECE = registerPiece(ObservatoryPiece::new, "observatory");

    public static final DeferredRegister<Structure<?>> STRUCTURE_DEFERRED_REGISTER = DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, FHMain.MODID);

    public static final RegistryObject<Structure<NoFeatureConfig>> Observatory = registerStructure("observatory", new ObservatoryStructure(NoFeatureConfig.CODEC));

    private static <F extends Structure<?>> RegistryObject<F> registerStructure(String name, F structure) {
        return STRUCTURE_DEFERRED_REGISTER.register(name,() ->structure);
    }
    private static IStructurePieceType registerPiece(IStructurePieceType type, String key) {
        return Registry.register(Registry.STRUCTURE_PIECE,  key, type);
    }
}
