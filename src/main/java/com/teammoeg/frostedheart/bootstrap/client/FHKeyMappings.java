/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.bootstrap.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class FHKeyMappings {
    public static Lazy<KeyMapping> key_skipDialog = createKey("skip_dialog",GLFW.GLFW_KEY_Z,KeyConflictContext.IN_GAME);
    public static Lazy<KeyMapping> key_InfraredView = createKey("infrared_view",GLFW.GLFW_KEY_I,KeyConflictContext.IN_GAME);
    public static Lazy<KeyMapping> key_openWheelMenu = createKey("open_wheel_menu",GLFW.GLFW_KEY_R,KeyConflictContext.IN_GAME);
    public static Lazy<KeyMapping> key_health = createKey("health",GLFW.GLFW_KEY_H,KeyConflictContext.IN_GAME);
    public static Lazy<KeyMapping> key_clothes = createKey("clothes",GLFW.GLFW_KEY_Y,KeyConflictContext.IN_GAME);
    public static void init() {

    }
    public static Lazy<KeyMapping> createKey(String category,String name,int keyCode,IKeyConflictContext conflictType){
    	return Lazy.of(()->{
    		KeyMapping km=new KeyMapping("key."+category+"."+name,
    			keyCode, "key.categories."+category);
    		km.setKeyConflictContext(conflictType);
    		return km;
    	});
    }
    public static Lazy<KeyMapping> createKey(String name,int keyCode,IKeyConflictContext conflictType){
    	return createKey(FHMain.MODID,name,keyCode,conflictType);
    }
    public static boolean hasSDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_S);
    }

}
