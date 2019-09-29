package de.jojomodding.itemtextureexport;

import com.google.common.collect.ImmutableSet;
import de.jojomodding.itemtextureexport.render.ItemTextureRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportingScreen extends Screen {

    private Screen lastScreen;
    private TextFieldWidget textureSize, outputFolder;
    private Button exportButton, exitButton;
    private boolean isProcessing = false;
    private String status = "";
    private Processing process;

    protected ExportingScreen(Minecraft mc, Screen last) {
        super(new TranslationTextComponent("gui.itemtextureexporter.main"));
        this.lastScreen = last;
    }

    @Override
    protected void init() {
        super.init();
        this.addButton(exitButton = new GuiButtonExt(50, this.height - 38, this.width / 2 - 55, 20,
                I18n.format("gui.itemtextureexporter.main.return"), b -> minecraft.displayGuiScreen(lastScreen)));
        this.addButton(exportButton = new GuiButtonExt(this.width / 2 + 5, this.height - 38, this.width / 2 - 55, 20,
                I18n.format("gui.itemtextureexporter.main.startexport"), b -> process = new Processing()));
        this.addButton(textureSize = new TextFieldWidget(font, 55 + font.getStringWidth(I18n.format("gui.itemtextureexporter.main.texsize")),
                56, 50, 16, null, "128"));
        textureSize.setMaxStringLength(4);
        textureSize.setText("128");
        int x;
        this.addButton(outputFolder = new TextFieldWidget(font, x = 55 + font.getStringWidth(I18n.format("gui.itemtextureexporter.main.outfolder1")),
                56+20, Math.max(0, width - x - 20), 16, null, "export"));
        outputFolder.setText("export");
        setProcessing(false);
    }

    private void setProcessing(boolean processing){
        textureSize.active = !processing;
        outputFolder.active = !processing;
        exportButton.active = !processing;
        exitButton.active = !processing;
        isProcessing = processing;
        if(processing){
            textureSize.setValidator(s -> false);
            outputFolder.setValidator(s -> false);
        }else{
            textureSize.setValidator(s -> {
                if(s.length() == 0) return true;
                try {
                    int i = Integer.parseInt(s);
                    return i > 0 && i < GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
                }catch (NumberFormatException e){
                    return false;
                }
            });
            outputFolder.setValidator(s -> true);
        }
    }

    public void receiveUpdate(float done){
        status = I18n.format("gui.itemtextureexporter.processing.progress", done);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !isProcessing;
    }

    private Set<ItemStack> stacksToRender() {
        return ForgeRegistries.ITEMS.getEntries().stream().map(Map.Entry::getValue).map(ItemStack::new).collect(Collectors.toSet());
    }

    @Override
    public void render(int mouseX, int mouseY, float partialT) {
        if (process != null) {
            Minecraft.getInstance().enqueue(process::continueProcessing);
        }
        renderBackground();
        drawCenteredString(font, title.getFormattedText(), width/2, 15, 0x00ffffff);
        drawString(font, status, 50, 40, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.main.texsize"), 50, 60, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.main.outfolder1"), 50, 60+20, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.main.outfolder2"), 50, 60+2*20, 0x00ffffff);
        super.render(mouseX, mouseY, partialT);
    }

    private class Processing {

        private int texSize;
        private File outputFolder;
        private ItemTextureRenderer renderer;
        private Iterator<ItemStack> toProcess;
        private int alreadyProcessed;
        private float totalNumber;

        public Processing() {
            try {
                texSize = Integer.parseInt(textureSize.getText());
                outputFolder = new File(Minecraft.getInstance().gameDir, ExportingScreen.this.outputFolder.getText());
                if (outputFolder.isFile() || !(outputFolder.mkdirs() || outputFolder.isDirectory())) {
                    throw new IOException(I18n.format("gui.itemtextureexporter.error.notadir", ExportingScreen.this.outputFolder.getText()));
                }
                setProcessing(true);
                receiveUpdate(0);
                renderer = new ItemTextureRenderer(Minecraft.getInstance().getItemRenderer());
                Set<ItemStack> toRender = stacksToRender();
                totalNumber = toRender.size();
                toProcess = toRender.iterator();
                alreadyProcessed = 0;
            } catch (NumberFormatException e) {
                status = I18n.format("gui.itemtextureexporter.error.notanum", textureSize.getText());
            } catch (IOException e) {
                status = I18n.format("gui.itemtextureexporter.error.io", e.getMessage());
            }
        }

        private void processSingeItemstack(ItemStack stack) throws IOException {
            BufferedImage bi = new BufferedImage(texSize, texSize, BufferedImage.TYPE_4BYTE_ABGR);
            renderer.renderItemstack(stack, bi, false);
            File outputResLoc = new File(outputFolder, stack.getItem().getRegistryName().getNamespace());
            if (!outputResLoc.mkdir() && !outputResLoc.isDirectory())
                throw new IOException(I18n.format("gui.itemtextureexporter.error.notadir", outputResLoc.getPath().toString()));
            ImageIO.write(bi, "png", new File(outputResLoc, stack.getItem().getRegistryName().getPath() + ".png"));
        }

        private void continueProcessing() {
            try {
                for (int i = 0; i < 1; i++) {
                    if (!toProcess.hasNext()) {
                        stopProcessing(alreadyProcessed);
                        return;
                    }
                    processSingeItemstack(toProcess.next());
                    receiveUpdate(100 * (alreadyProcessed++) / totalNumber);
                }
            } catch (IOException e) {
                stopProcessing(0);
                status = I18n.format("gui.itemtextureexporter.error.io", e.getMessage());
            }
        }

        private void stopProcessing(int totalImgs){
            setProcessing(false);
            process = null;
            status = I18n.format("gui.itemtextureexporter.processing.done", totalImgs);
        }

    }



    private class EnlargedList extends ExtendedList<EnlargedList.Entry>{

        public EnlargedList(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
            super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
        }

        private class Entry extends ExtendedList.AbstractListEntry<Entry>{

            @Override
            public void render(int p_render_1_, int p_render_2_, int p_render_3_, int p_render_4_, int p_render_5_, int p_render_6_, int p_render_7_, boolean p_render_8_, float p_render_9_) {

            }
        }

    }
}
