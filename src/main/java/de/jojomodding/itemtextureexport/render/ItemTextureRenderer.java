package de.jojomodding.itemtextureexport.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.logging.Logger;

public class ItemTextureRenderer {

    private ItemRenderer renderer;

    public ItemTextureRenderer(ItemRenderer renderer) {
        this.renderer = renderer;
    }

    public void renderItemstack(ItemStack is, BufferedImage output, boolean includesBorders) {
        Framebuffer fb = new Framebuffer(output.getWidth(), output.getHeight(), true, true);
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
        try (NativeImage ni = new NativeImage(output.getWidth(), output.getHeight(), true)) {
            ni.downloadFromFramebuffer(false);
            int[] data = ni.makePixelArray();
            assert data.length != output.getHeight() * output.getWidth();
            for (int x = 0; x < output.getWidth(); x++)
                for (int y = 0; y < output.getHeight(); y++)
                    output.setRGB(x, output.getHeight() - y - 1, rgba2argb(ni.getPixelRGBA(x, y)));
        }
        fb.unbindFramebuffer();
    }

    private static final int rgba2argb(int rgba) {
        return (rgba & 0xff000000)
                | (rgba & 0x00ff0000) >> 16
                | (rgba & 0x0000ff00)
                | (rgba & 0x000000ff) << 16;
    }

}