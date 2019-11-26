package de.jojomodding.itemtextureexport.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import de.jojomodding.itemtextureexport.render.WrappedItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ItemStackList extends ExtendedList<ItemStackList.Entry> {

    private ExtraItemsScreen parent;

    public ItemStackList(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, ExtraItemsScreen parent) {
        super(mcIn, widthIn, heightIn, topIn, bottomIn, 24);
        this.parent = parent;
        refresh();
    }

    @Override
    public int getRowWidth() {
        return width - 10;
    }

    @Override
    protected int getScrollbarPosition() {
        return width - 5;
    }

    @Override
    protected void renderBackground() {
        parent.renderBackground();
    }

    public void refresh() {
        clearEntries();
        parent.getParent().stacksToRender().keySet().stream().map(Entry::new).forEach(this::addEntry);
    }

    public void centerScrollOn(WrappedItemStack wis) {
        children().stream().filter(e -> e.is.equals(wis)).findAny().ifPresent(this::centerScrollOn);
    }

    public void renderMouseover(int p_render_1_, int p_render_2_, float p_render_3_) {
        int k = this.getRowLeft();
        int l = this.y0 + 4 - (int)this.getScrollAmount();
        this.renderListMouseOver(k, l, p_render_1_, p_render_2_, p_render_3_);
    }

    private void renderListMouseOver(int p_renderList_1_, int p_renderList_2_, int p_renderList_3_, int p_renderList_4_, float p_renderList_5_) {
        int i = this.getItemCount();
        for(int j = 0; j < i; ++j) {
            int k = this.getRowTop(j);
            int l = this.getRowTop(j) + this.itemHeight;
            if (l >= this.y0 && k <= this.y1) {
                int i1 = p_renderList_2_ + j * this.itemHeight + this.headerHeight;
                int j1 = this.itemHeight - 4;
                Entry e = this.getEntry(j);
                int k1 = this.getRowWidth();
                int j2 = this.getRowLeft();
                e.renderMouseOver(j, k, j2, k1, j1, p_renderList_3_, p_renderList_4_, this.isMouseOver((double)p_renderList_3_, (double)p_renderList_4_) && Objects.equals(this.getEntryAtPosition((double)p_renderList_3_, (double)p_renderList_4_), e), p_renderList_5_);
            }
        }

    }

    public void added(WrappedItemStack wis) {
        addEntry(new Entry(wis));
    }


    protected class Entry extends ExtendedList.AbstractListEntry<Entry> implements INestedGuiEventHandler{

        private WrappedItemStack is;
        private CheckboxButton oversized;
        private ImageButton deleter;
        private TextFieldWidget nbt;

        protected Entry(WrappedItemStack is) {
            this.is = is;
            nbt = new TextFieldWidget(minecraft.fontRenderer, 22, 4, getRowWidth() - 70, 16, null, "text");
            nbt.setMaxStringLength(Integer.MAX_VALUE);
            nbt.setText(is.stack.getTag() == null ? "-" : is.stack.getTag().toString());
            nbt.setValidator(s -> false);
            oversized = new CheckboxButton(getRowWidth() - 44, 2, 20, 20, "", parent.getParent().stacksToRender().get(is)) {
                @Override
                public void onPress() {
                    super.onPress();
                    checkboxChange();
                }
            };
            deleter = new ImageButton(getRowWidth() - 20, 4, 16, 16, 113, 222, 0, new ResourceLocation("textures/gui/container/beacon.png"), this::delete);
        }

        private void delete(Button $) {
            parent.getParent().stacksToRender().remove(is);
            removeEntry(this);
            //resets list scroll
            setScrollAmount(getScrollAmount());
        }

        private void checkboxChange() {
            parent.getParent().stacksToRender().put(is, oversized.func_212942_a());
        }

        @Override
        public void render(int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean p_194999_5_, float partialTicks) {
            GlStateManager.pushMatrix();
            mouseX -= left;
            mouseY -= top;
            GlStateManager.translatef(left, top, 0F);
            nbt.render(mouseX, mouseY, partialTicks);
            oversized.render(mouseX, mouseY, partialTicks);
            deleter.render(mouseX, mouseY, partialTicks);
            RenderHelper.enableGUIStandardItemLighting();
            minecraft.getItemRenderer().renderItemIntoGUI(is.stack, 0, 2);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }

        public void renderMouseOver(int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean p_194999_5_, float partialTicks) {
            mouseX -= left;
            mouseY -= top;
            if (mouseX >= 2 && mouseX < 18 && mouseY >= 2 && mouseY < 18) {
                parent.renderTooltip(is.stack, mouseX + left, mouseY + top);
            } else if (oversized.isHovered()) {
                parent.renderTooltip(I18n.format("gui.itemtextureexporter.extra.oversized"), mouseX + left, mouseY + top);
            } else if (deleter.isHovered()) {
                parent.renderTooltip(I18n.format("gui.itemtextureexporter.extra.delete"), mouseX + left, mouseY + top);
            } else if (nbt.isHovered()) {
                parent.renderTooltip(I18n.format("gui.itemtextureexporter.extra.nbt"), mouseX + left, mouseY + top);
            }
            //render tooltip corrupts GL state, fix it
            GlStateManager.disableLighting();
        }


        //Make the clicky stuff work
        @Override
        public List<? extends IGuiEventListener> children() {
            return ImmutableList.of(nbt, deleter, oversized);
        }

        private boolean dragging;

        @Override
        public boolean isDragging() {
            return dragging;
        }

        @Override
        public void setDragging(boolean p_setDragging_1_) {
            this.dragging = p_setDragging_1_;
        }

        private IGuiEventListener focused;

        @Nullable
        @Override
        public IGuiEventListener getFocused() {
            return focused;
        }

        @Override
        public void setFocused(@Nullable IGuiEventListener p_setFocused_1_) {
            this.focused = p_setFocused_1_;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return INestedGuiEventHandler.super.mouseClicked(mouseX - getRowLeft(), mouseY - getRowTop(ItemStackList.this.children().indexOf(this)), button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return INestedGuiEventHandler.super.mouseDragged(mouseX - getRowLeft(), mouseY - getRowTop(ItemStackList.this.children().indexOf(this)), button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return INestedGuiEventHandler.super.mouseReleased(mouseX - getRowLeft(), mouseY - getRowTop(ItemStackList.this.children().indexOf(this)), button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            return INestedGuiEventHandler.super.mouseScrolled(mouseX - getRowLeft(), mouseY - getRowTop(ItemStackList.this.children().indexOf(this)), amount);
        }
    }
}
