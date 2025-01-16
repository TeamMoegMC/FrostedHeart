package com.teammoeg.chorda.block;
/**
 * Replaces TickableBlockEntity for easier intergration and migration.
 * Corrensponding block must implements FHEntityBlock.
 * */
public interface FHTickableBlockEntity {
	void tick();
}
