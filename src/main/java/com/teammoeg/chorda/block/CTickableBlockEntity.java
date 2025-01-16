package com.teammoeg.chorda.block;
/**
 * Replaces TickableBlockEntity for easier intergration and migration.
 * Corrensponding block must implements CEntityBlock.
 * */
public interface CTickableBlockEntity {
	void tick();
}
