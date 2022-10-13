package com.teammoeg.frostedheart.content.incubator;

import com.teammoeg.frostedheart.content.steamenergy.ISteamEnergyBlock;

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;

public class HeatIncubatorBlock extends IncubatorBlock implements ISteamEnergyBlock {

	public HeatIncubatorBlock(String name, Properties p, RegistryObject<TileEntityType<IncubatorTileEntity>> type) {
		super(name, p, type);
	}

}
