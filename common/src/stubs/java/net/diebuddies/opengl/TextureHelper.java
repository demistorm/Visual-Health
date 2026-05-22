package net.diebuddies.opengl;

import com.mojang.blaze3d.textures.GpuTextureView;

public class TextureHelper {
    private static GpuTextureView loadedTexture;

    public static void setLoadedTexture(GpuTextureView loadedTexture) {
        TextureHelper.loadedTexture = loadedTexture;
    }

    public static GpuTextureView getLoadedTextures() {
        return loadedTexture;
    }
}
