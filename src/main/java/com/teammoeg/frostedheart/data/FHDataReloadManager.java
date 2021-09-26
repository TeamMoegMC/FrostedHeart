package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Predicate;


public class FHDataReloadManager implements ISelectiveResourceReloadListener {
    public static final FHDataReloadManager INSTANCE = new FHDataReloadManager();
    private static final JsonParser parser = new JsonParser();

    @Override
    public void onResourceManagerReload(IResourceManager manager, Predicate<IResourceType> resourcePredicate) {
        for (FHDataTypes dat : FHDataTypes.values()) {
            for (ResourceLocation rl : manager.getAllResourceLocations(dat.type.getLocation(), (s) -> s.endsWith(".json"))) {
                try {
                    try (IResource rc = manager.getResource(rl);
                         InputStream stream = rc.getInputStream();
                         InputStreamReader reader = new InputStreamReader(stream)) {
                        JsonObject object = parser.parse(reader).getAsJsonObject();
                        FHDataManager.register(dat, object);
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}