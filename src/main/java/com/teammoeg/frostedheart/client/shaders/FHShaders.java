package com.teammoeg.frostedheart.client.shaders;

import com.lowdragmc.lowdraglib.client.shader.Shaders;
import com.lowdragmc.lowdraglib.client.shader.management.Shader;
import com.teammoeg.frostedheart.FHMain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FHShaders extends Shaders {
    private static Shader INFRARED_VIEW;

    public static Shader getInfraredView() {
        if (INFRARED_VIEW == null) {
            INFRARED_VIEW = load(Shader.ShaderType.FRAGMENT, FHMain.rl("infrared_view"));
            addReloadListener(() -> INFRARED_VIEW = load(Shader.ShaderType.FRAGMENT, FHMain.rl("infrared_view")));
        }
        return INFRARED_VIEW;
    }

}
