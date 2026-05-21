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

package com.teammoeg.frostedscenario.client;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.objectweb.asm.Type;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedscenario.FHScenario;
import com.teammoeg.frostedscenario.FSMain;
import com.teammoeg.frostedscenario.ScenarioCommandProvider;
import com.teammoeg.frostedscenario.ScenarioExecutor;
import com.teammoeg.frostedscenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedscenario.commands.client.ClientControl;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.IForgeRegistry;

public class FHScenarioClient {
    static ScenarioExecutor<IClientScene> client = new ScenarioExecutor<>(IClientScene.class);
    public static final String LINK_SYMBOL="fh$scenario$link:";
    public static void callCommand(String name, IClientScene runner, Map<String, String> params) {
        client.callCommand(name, runner, params);
    }
    public static ResourceLocation getPathOf(ResourceLocation orig,String path) {
    	ResourceLocation rl= new ResourceLocation(orig.getNamespace(),path+ClientUtils.getMc().getLanguageManager().getSelected()+"/"+orig.getPath());
    	if(ClientUtils.getMc().getResourceManager().getResource(rl).isPresent()) {
    		return rl;
    	}
    	return new ResourceLocation(orig.getNamespace(),path+orig.getPath());
    }
    public static <T> Optional<Holder<T>> getPathFrom(IForgeRegistry<T> registry,ResourceLocation orig,String path) {
    	ResourceLocation rl= new ResourceLocation(orig.getNamespace(),path+ClientUtils.getMc().getLanguageManager().getSelected()+"/"+orig.getPath());
    	//System.out.println(rl);
    	if(registry.containsKey(rl)) {
    		return registry.getHolder(rl);
    	}
    	//System.out.println(new ResourceLocation(orig.getNamespace(),path+orig.getPath()));
    	return registry.getHolder(new ResourceLocation(orig.getNamespace(),path+orig.getPath()));
    }
    public static void register(Class<?> clazz) {
        client.register(clazz);
    }

    public static void registerCommand(String cmdName, ScenarioMethod<IClientScene> method) {
        client.registerCommand(cmdName, method);
    }

	public static void setup() {
		Type annotationType = Type.getType(ScenarioCommandProvider.class);
		for (ModFileScanData scanData : ModList.get().getAllScanData()) {
			Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();
			for (ModFileScanData.AnnotationData a : annotations) {
				if (Objects.equals(a.annotationType(), annotationType)) {
					try {
						if(!Boolean.TRUE.equals(a.annotationData().get("clientOnly"))){
							continue;
						}
						if(Boolean.TRUE.equals(a.annotationData().get("delegate"))){
							continue;
						}
						if(a.annotationData().get("modid")!=null)
							if(!ModList.get().isLoaded(String.valueOf(a.annotationData().get("modid"))))
								continue;
						Class<?> clazz=Class.forName(a.memberName());
						FHScenario.registerClientDelegate(clazz);
					} catch (ReflectiveOperationException | LinkageError e) {
						FSMain.LOGGER.error("Failed to load: {}", a.memberName(), e);
					}
				}
			}
		}
	}
    static {
    	register(ClientControl.class);
    }
}
