package com.teammoeg.frostedheart.mixin.registrate;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.infrastructure.gen.FHLootGen;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;
@Mixin(RegistrateBlockLootTables.class)
public class RegistrateBlockLootTablesMixin_RemoveExisted extends VanillaBlockLoot {
	@Shadow(remap=false)
    private AbstractRegistrate<?> parent;
	/**
	 * @author khjxiaogu
	 * @reason Remove warning when we explicitly define the loot table is provided
	 * */

    @Overwrite(remap=false)
    protected Iterable<Block> getKnownBlocks() {
        return parent.getAll(Registries.BLOCK).stream().map(Supplier::get).filter(o->!FHLootGen.existedBlocks.contains(o)).collect(Collectors.toList());
    }
}
