package de.jojomodding.itemtextureexport.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class ItemTextureRenderer implements Closeable {

    private ItemRenderer renderer;
    private Framebuffer smallBuffer, largeBuffer;

    public ItemTextureRenderer(ItemRenderer renderer, int imageSize) {
        this.renderer = renderer;
        smallBuffer = new Framebuffer(imageSize, imageSize, true, true);
        largeBuffer = new Framebuffer(3 * imageSize, 3 * imageSize, true, true);
    }

    public void renderItemstack(ItemStack is, File output, boolean includesBorders) throws IOException {
        Framebuffer fb = includesBorders ? largeBuffer : smallBuffer;
//        if (output.getWidth() != fb.framebufferWidth || output.getHeight() != fb.framebufferHeight) {
//            throw new IllegalArgumentException("Image is not sized correctly!");
//        }
        fb.bindFramebuffer(true);
        GlStateManager.enableDepthTest();
        GlStateManager.enableBlend();
        GlStateManager.clearColor(0.f, 0.f, 0.f, 0.f);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT, true);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT, true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        if (includesBorders) {
            GlStateManager.ortho(-16, 32, 32, -16, -1000.0D, 3000.0D);
        } else {
            GlStateManager.ortho(0, 16, 16, 0, -1000.0D, 3000.0D);
        }
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translatef(0.f, 0.f, -2000.f);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        renderer.renderItemIntoGUI(is, 0, 0);


        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();


        fb.bindFramebufferTexture();
        try (NativeImage ni = new NativeImage(fb.framebufferWidth, fb.framebufferHeight, true)) {
            ni.downloadFromFramebuffer(false);
            ni.flip();
            ni.write(output);
//            int[] data = ni.makePixelArray();
//            assert data.length != output.getHeight() * output.getWidth();
//            for (int x = 0; x < output.getWidth(); x++)
//                for (int y = 0; y < output.getHeight(); y++)
//                    output.setRGB(x, output.getHeight() - y - 1, abgr2argb(ni.getPixelRGBA(x, y))); //getPixelRGBA actually returns abgr / big endian stuff
        }
        fb.unbindFramebufferTexture();
        fb.unbindFramebuffer();
    }

    public void close() {
        smallBuffer.deleteFramebuffer();
        largeBuffer.deleteFramebuffer();
    }

    private static final int abgr2argb(int rgba) {
        return (rgba & 0xff000000)
                | (rgba & 0x00ff0000) >> 16
                | (rgba & 0x0000ff00)
                | (rgba & 0x000000ff) << 16;
    }

}
