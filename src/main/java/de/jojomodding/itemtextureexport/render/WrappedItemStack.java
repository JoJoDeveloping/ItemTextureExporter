package de.jojomodding.itemtextureexport.render;

import net.minecraft.item.ItemStack;

import java.util.Objects;

public class WrappedItemStack {

    public final ItemStack stack;

    public WrappedItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedItemStack that = (WrappedItemStack) o;
        return stack.getItem() == that.stack.getItem()
               && Objects.equals(stack.getTag(), that.stack.getTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack.getItem(), stack.getTag());
    }
}
