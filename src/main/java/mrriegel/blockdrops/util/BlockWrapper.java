package mrriegel.blockdrops.util;

import mezz.jei.plugins.vanilla.util.FakeClientWorld;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

public class BlockWrapper {
	public Block block;
	public int meta;

	public BlockWrapper(Block block, int meta) {
		this.block = block;
		this.meta = meta;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BlockWrapper)
			return block == ((BlockWrapper) obj).block && meta == ((BlockWrapper) obj).meta;
		return false;
	}

	public IBlockState getState() {
		int m = Item.getItemFromBlock(block).getMetadata(meta);
		try {
			return block.getStateForPlacement(FakeClientWorld.getInstance(), BlockPos.ORIGIN, EnumFacing.UP, 0, 0, 0, m, new EntityZombie(FakeClientWorld.getInstance()), EnumHand.MAIN_HAND);
		} catch (Exception e) {
			return block.getStateFromMeta(m);
		}
	}

	public ItemStack getStack() {
		return new ItemStack(block, 1, meta);
	}
}
