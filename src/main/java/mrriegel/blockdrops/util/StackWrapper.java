package mrriegel.blockdrops.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class StackWrapper {
	public ItemStack stack;
	public int size;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StackWrapper)
			return ItemHandlerHelper.canItemStacksStack(stack, ((StackWrapper) obj).stack);
		return false;
	}

	public StackWrapper(ItemStack stack, int num) {
		super();
		this.stack = stack;
		this.size = num;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Item.getIdFromItem(stack.getItem());
		result = prime * result + stack.getItemDamage();
		result = prime * result + (stack.getTagCompound() == null ? 0 : stack.getTagCompound().hashCode());
		return result;
	}
}
