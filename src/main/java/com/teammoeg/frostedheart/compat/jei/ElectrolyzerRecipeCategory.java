/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.compat.jei;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.FHBlocks;
import electrodynamics.compatability.jei.recipecategories.FluidItem2FluidRecipeCategory;
import mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;

public class ElectrolyzerRecipeCategory extends FluidItem2FluidRecipeCategory {

    // JEI Window Parameters
    private static int[] GUI_BACKGROUND = {0, 0, 132, 64};
    private static int[] MAJOR_PROCESSING_ARROW_LOCATION = {0, 64, 68, 15};
    private static int[] MINOR_PROCESSING_ARROW_LOCATION = {0, 79, 20, 15};

    private static int[] INPUT_ITEM_OFFSET = {60, 16};
    private static int[] INPUT_FLUID_BUCKET_OFFSET = {60, 36};
    private static int[] INPUT_FLUID_TANK = {15, 52, 12, 47, 5000};
    private static int[] OUTPUT_FLUID_TANK = {109, 52, 12, 47, 5000};
    private static int[] OUTPUT_FLUID_BUCKET_OFFSET = {88, 36};

    private static int[] MAJOR_PROCESSING_ARROW_OFFSET = {34, 17};
    private static int[] MINOR_PROCESSING_ARROW_OFFSET = {34, 37};

    private static int[] FLUID_BAR_LOCATION = {0, 0, 0, 0};

    private static int TEXT_Y_HEIGHT = 48;
    private static int SMELT_TIME = 50;

    private static String MOD_ID = FHMain.MODID;
    private static String RECIPE_GROUP = "electrolyzer";
    private static String GUI_TEXTURE = "textures/gui/sol_and_liq_to_liq_recipe_gui.png";

    private static ItemStack INPUT_MACHINE = new ItemStack(FHBlocks.electrolyzer);

    private static StartDirection MAJOR_ARROW_START_DIRECTION = StartDirection.LEFT;
    private static StartDirection MINOR_ARROW_START_DIRECTION = StartDirection.RIGHT;

    public static ResourceLocation UID = new ResourceLocation(MOD_ID, RECIPE_GROUP);

    private static ArrayList<int[]> INPUT_COORDINATES = new ArrayList<>(Arrays.asList(GUI_BACKGROUND, MAJOR_PROCESSING_ARROW_LOCATION,
            MINOR_PROCESSING_ARROW_LOCATION, INPUT_ITEM_OFFSET, INPUT_FLUID_BUCKET_OFFSET, INPUT_FLUID_TANK, OUTPUT_FLUID_TANK,
            OUTPUT_FLUID_BUCKET_OFFSET, MAJOR_PROCESSING_ARROW_OFFSET, MINOR_PROCESSING_ARROW_OFFSET, FLUID_BAR_LOCATION));

    public ElectrolyzerRecipeCategory(IGuiHelper guiHelper) {
        super(guiHelper, MOD_ID, RECIPE_GROUP, GUI_TEXTURE, INPUT_MACHINE, INPUT_COORDINATES, SMELT_TIME, MAJOR_ARROW_START_DIRECTION,
                MINOR_ARROW_START_DIRECTION, TEXT_Y_HEIGHT);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

}
