package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.server.packs.resources.ResourceManager;

public class AnimatedDynamicTexture extends AbstractTexture implements Tickable {

    private NativeImage spriteSheet;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final int defaultFrameTime;
    private int currentFrame;
    private int ticksInFrame;

    public AnimatedDynamicTexture(NativeImage spriteSheet, int frameWidth, int frameHeight, int defaultFrameTime) {
        this.spriteSheet = spriteSheet;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = spriteSheet.getHeight() / frameHeight;
        this.defaultFrameTime = Math.max(1, defaultFrameTime);
        this.currentFrame = 0;
        this.ticksInFrame = 0;

        TextureUtil.prepareImage(this.getId(), frameWidth, frameHeight);
        uploadCurrentFrame();
    }

    @Override
    public void load(ResourceManager manager) {
    }

    private void uploadCurrentFrame() {
        int srcY = currentFrame * frameHeight;
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> {
                this.bind();
                this.spriteSheet.upload(0, 0, 0, 0, srcY, this.frameWidth, this.frameHeight, false, false);
            });
        } else {
            this.bind();
            this.spriteSheet.upload(0, 0, 0, 0, srcY, this.frameWidth, this.frameHeight, false, false);
        }
    }

    @Override
    public void tick() {
        ticksInFrame++;
        if (ticksInFrame >= defaultFrameTime) {
            ticksInFrame = 0;
            currentFrame = (currentFrame + 1) % frameCount;
            uploadCurrentFrame();
        }
    }

    @Override
    public void close() {
        if (spriteSheet != null) {
            spriteSheet.close();
            spriteSheet = null;
        }
        this.releaseId();
    }
}
