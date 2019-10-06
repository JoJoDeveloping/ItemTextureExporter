package de.jojomodding.itemtextureexport.gui;

import de.jojomodding.itemtextureexport.ItemTextureExporter;
import de.jojomodding.itemtextureexport.render.Processing;
import de.jojomodding.itemtextureexport.render.WrappedItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.opengl.GL11;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExportingScreen extends Screen {

    private ItemTextureExporter mod;
    private Screen lastScreen;
    private TextFieldWidget textureSize, outputFolder;
    private Button exportButton, exitButton, configureListButton;
    private int configureListButtonWidth;
    private boolean isProcessing = false;
    private String status = "";
    private Processing process;
    private LinkedHashMap<WrappedItemStack, Boolean> stacksToRender;

    public ExportingScreen(Minecraft mc, Screen last, ItemTextureExporter mod) {
        super(new TranslationTextComponent("gui.itemtextureexporter.main"));
        minecraft = mc;
        this.lastScreen = last;
        this.mod = mod;
        stacksToRender = defaultStacks();
    }

    @Override
    protected void init() {
        super.init();
        this.addButton(exitButton = new GuiButtonExt(50, this.height - 38, this.width / 2 - 55, 20,
                                                     I18n.format(lastScreen == null ? "gui.itemtextureexporter.main.returnmain" : "gui.itemtextureexporter.main.returnlast"),
                                                     b -> minecraft.displayGuiScreen(lastScreen)));
        this.addButton(exportButton = new GuiButtonExt(this.width / 2 + 5, this.height - 38, this.width / 2 - 55, 20,
                                                       I18n.format("gui.itemtextureexporter.main.startexport"), b -> new Processing(this, mod)));
        this.addButton(textureSize = new TextFieldWidget(font, 55 + font.getStringWidth(I18n.format("gui.itemtextureexporter.main.texsize")),
                                                         56, 50, 16, null, "128"));
        textureSize.setMaxStringLength(4);
        textureSize.setText("128");
        int x;
        this.addButton(outputFolder = new TextFieldWidget(font, x = 55 + font.getStringWidth(I18n.format("gui.itemtextureexporter.main.outfolder1")),
                                                          56 + 20, Math.max(0, width - x - 20), 16, null, "export"));
        outputFolder.setText("export");
        String text = I18n.format("gui.itemtextureexporter.main.configurelist");
        configureListButtonWidth = font.getStringWidth(text) + 16;
        this.addButton(configureListButton = new GuiButtonExt(50, 56 + 60, configureListButtonWidth, 20, text, button -> minecraft.displayGuiScreen(new ExtraItemsScreen(this))));
        setProcessing(false);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setProcessing(boolean processing) {
        textureSize.active = !processing;
        outputFolder.active = !processing;
        exportButton.active = !processing;
        exitButton.active = !processing;
        configureListButton.active = !processing;
        isProcessing = processing;
        if (processing) {
            textureSize.setValidator(s -> false);
            outputFolder.setValidator(s -> false);
        } else {
            textureSize.setValidator(s -> {
                if (s.length() == 0) return true;
                try {
                    int i = Integer.parseInt(s);
                    return i > 0 && i < GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
                } catch (NumberFormatException e) {
                    return false;
                }
            });
            outputFolder.setValidator(s -> true);
            process = null;
        }
    }

    public void receiveUpdate(float done) {
        status = I18n.format("gui.itemtextureexporter.processing.progress", done);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialT) {
        if (process != null) {
            Minecraft.getInstance().enqueue(process::continueProcessing);
        }
        renderBackground();
        drawCenteredString(font, title.getFormattedText(), width / 2, 15, 0x00ffffff);
        drawString(font, status, 50, 40, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.main.texsize"), 50, 60, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.main.outfolder1"), 50, 60 + 20, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.main.outfolder2"), 50, 60 + 2 * 20, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.main.configured", stacksToRender.size()), 52 + configureListButtonWidth, 60 + 3 * 20 + 3, 0x00ffffff);
        super.render(mouseX, mouseY, partialT);
    }

    public String getTextureSize() {
        return textureSize.getText();
    }

    public String getOutputFolder() {
        return outputFolder.getText();
    }

    public void setProcessing(Processing processing) {
        this.process = processing;
        setProcessing(true);
    }


    @Override
    public boolean shouldCloseOnEsc() {
        return !isProcessing;
    }

    public LinkedHashMap<WrappedItemStack, Boolean> stacksToRender() {
        return stacksToRender;
    }

    public void setStacksToRender(LinkedHashMap<WrappedItemStack, Boolean> stacksToRender) {
        this.stacksToRender = stacksToRender;
    }

    public static LinkedHashMap<WrappedItemStack, Boolean> defaultStacks() {
        return Stream.concat(
                ForgeRegistries.ITEMS.getEntries().stream().
                        map(Map.Entry::getValue).
                                             map(ItemStack::new),
                allCreativeTabs()).collect(Collectors.toMap(WrappedItemStack::new, $ -> Boolean.FALSE, (a, b) -> a, LinkedHashMap::new));
    }

    private static Stream<ItemStack> allCreativeTabs() {
        NonNullList<ItemStack> list = NonNullList.create();
        for (ItemGroup group : ItemGroup.GROUPS) {
            if (ItemGroup.HOTBAR == group || ItemGroup.SEARCH == group) {
                continue;
            }
            group.fill(list);
        }
        return list.stream();
    }
}
