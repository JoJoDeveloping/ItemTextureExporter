package de.jojomodding.itemtextureexport.gui;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.jojomodding.itemtextureexport.render.WrappedItemStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.config.GuiButtonExt;


public class ExtraItemsScreen extends Screen {


    private ItemStackList list;
    private TextFieldWidget itemEntry;
    private Button adder;
    private ExportingScreen parent;
    private TextComponent status;

    protected ExtraItemsScreen(ExportingScreen exportingScreen) {
        super(new TranslationTextComponent("gui.itemtextureexporter.extra"));
        this.parent = exportingScreen;
    }

    @Override
    protected void init() {
        children.add(list = new ItemStackList(minecraft, width, height, 100, height - 40, this));
        this.addButton(new GuiButtonExt(50, this.height - 38, this.width / 2 - 55, 20,
                                        I18n.format("gui.itemtextureexporter.extra.apply"),
                                        b -> minecraft.displayGuiScreen(parent)));
        this.addButton(new GuiButtonExt(this.width / 2 + 5, this.height - 38, this.width / 2 - 55, 20,
                                        I18n.format("gui.itemtextureexporter.extra.reset"), b -> reset()));
        int x;
        String text = I18n.format("gui.itemtextureexporter.extra.add");
        this.addButton(itemEntry = new TextFieldWidget(font, x = 22 + font.getStringWidth(I18n.format("gui.itemtextureexporter.extra.additem1")), 56,
                                                       Math.max(0, width - x - 20 - 10 - 4 - font.getStringWidth(text)),
                                                       16, null, "additem"));
        itemEntry.setMaxStringLength(Integer.MAX_VALUE);
        this.addButton(adder = new GuiButtonExt(x + itemEntry.getWidth() + 4, 54,
                                                10 + font.getStringWidth(text), 20,
                                                text, b -> addItem()) {
            @Override
            public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
                super.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
                return false;
            }
        });
    }

    private void addItem() {
        StringReader reader = new StringReader(itemEntry.getText());
        ItemParser parser = new ItemParser(reader, false);
        try {
            parser.parse();
            ItemStack is = new ItemStack(parser.getItem(), 1);
            is.setTag(parser.getNbt());
            WrappedItemStack wis = new WrappedItemStack(is);
            if (parent.stacksToRender().containsKey(wis)) {
                status = new TranslationTextComponent("gui.itemtextureexporter.extra.alreadyinlist");
                list.centerScrollOn(wis);
                return;
            }
            parent.stacksToRender().put(wis, false);
            list.added(wis);
            list.setScrollAmount(Double.MAX_VALUE);
            itemEntry.setText("");
            status = null;
        } catch (CommandSyntaxException e) {
            status = new TranslationTextComponent("gui.itemtextureexporter.extra.invalidspec", e.getMessage());
            setFocused(itemEntry);
            itemEntry.setFocused2(true);
            itemEntry.setCursorPosition(e.getCursor());
        }

    }

    public ExportingScreen getParent() {
        return parent;
    }

    private void reset() {
        parent.setStacksToRender(ExportingScreen.defaultStacks());
        list.refresh();
    }

    @Override
    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        renderBackground();
        list.render(p_render_1_, p_render_2_, p_render_3_);
        super.render(p_render_1_, p_render_2_, p_render_3_);
        drawCenteredString(font, title.getFormattedText(), width / 2, 15, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.extra.additem1"), 20, 60, 0x00ffffff);
        drawString(font, I18n.format("gui.itemtextureexporter.extra.additem2"), 20, 80, 0x00ffffff);
        if(status != null)
            drawString(font, status.getFormattedText(), 20, 40, 0x00ffffff);
        list.renderMouseover(p_render_1_, p_render_2_, p_render_3_);
    }

    @Override
    public void renderTooltip(ItemStack is, int mouseX, int mouseY) {
        super.renderTooltip(is, mouseX, mouseY);
    }


}
