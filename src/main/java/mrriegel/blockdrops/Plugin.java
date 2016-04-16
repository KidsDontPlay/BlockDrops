package mrriegel.blockdrops;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IItemRegistry;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ITooltipCallback;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import mezz.jei.api.recipe.BlankRecipeCategory;
import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@JEIPlugin
public class Plugin implements IModPlugin {
	@Override
	public void register(IModRegistry registry) {
		registry.addRecipeCategories(new Category(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeHandlers(new Handler());
		registry.addRecipes(getRecipes());
	}

	static class BlockWrapper {
		Block block;
		int meta;

		public BlockWrapper(Block block, int meta) {
			this.block = block;
			this.meta = meta;
		}

		@Override
		public String toString() {
			return "BlockWrapper [block=" + block + ", meta=" + meta + "]";
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof BlockWrapper)
				return block == ((BlockWrapper) obj).block && meta == ((BlockWrapper) obj).meta;
			return false;
		}

		IBlockState getState() {
			// int m = block instanceof BlockAnvil ? meta << 2 : meta;
			int m = meta;
			return block.onBlockPlaced(Minecraft.getMinecraft().theWorld, BlockPos.ORIGIN, EnumFacing.UP, 0, 0, 0, m, Minecraft.getMinecraft().thePlayer);
		}

		ItemStack getStack() {
			return new ItemStack(block, 1, meta);
		}

	}

	private List<Wrapper> getRecipes() {
		List<Wrapper> res = Lists.newArrayList();
		Set<BlockWrapper> blocks = Sets.newHashSet();
		for (ResourceLocation r : Block.blockRegistry.getKeys()) {
			Block b = Block.blockRegistry.getObject(r);
			List<ItemStack> lis = Lists.newArrayList();
			if (Item.getItemFromBlock(b) == null || b.getCreativeTabToDisplayOn() == null)
				continue;
			b.getSubBlocks(Item.getItemFromBlock(b), b.getCreativeTabToDisplayOn(), lis);
			for (ItemStack s : lis)
				blocks.add(new BlockWrapper(b, s.getItemDamage()));
		}
		List<BlockWrapper> x = Lists.newArrayList(blocks);
		x.sort(new Comparator<BlockWrapper>() {
			@Override
			public int compare(BlockWrapper o1, BlockWrapper o2) {
				int id = Integer.compare(Block.getIdFromBlock(o1.block), Block.getIdFromBlock(o2.block));
				int meta = Integer.compare(o1.meta, o2.meta);
				return id != 0 ? id : meta;
			}
		});
		for (BlockWrapper w : x) {
			Block b = w.block;
			IBlockState state = w.getState();
			List<ItemStack> list = Lists.newArrayList();
			for (int i = 0; i < 1000; i++) {
				List<ItemStack> lis = b.getDrops(Minecraft.getMinecraft().theWorld, BlockPos.ORIGIN, state, 3);
				// if (lis.size() > list.size())
				// list = lis;
				list.addAll(lis);
			}
			list.removeAll(Collections.singleton(null));
			Iterator<ItemStack> it = list.iterator();
			while (it.hasNext()) {
				ItemStack tmp = it.next();
				if (tmp.isItemEqual(w.getStack()))
					it.remove();
			}
//			list = merge(list);
			if (list.isEmpty())
				continue;
			if (w.getStack().getItem() == null)
				continue;
			List<Drop> ds = Lists.newArrayList();
			for (ItemStack s : list)
				ds.add(new Drop(s, new Random().nextFloat()));
			res.add(new Wrapper(w.getStack(), ds));
		}
		return res;

	}

	private List<Drop> getList(IBlockState state) {
		List<Drop> drops = Lists.newArrayList();
	}

	static class StackWrapper {
		ItemStack stack;
		int num;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Drop)
				return stack.isItemEqual(((StackWrapper) obj).stack) && num == ((StackWrapper) obj).num;
			return false;
		}

		public StackWrapper(ItemStack stack, int num) {
			super();
			this.stack = stack;
			this.num = num;
		}

	}

	private List<StackWrapper> merge(List<StackWrapper> lis) {
		List<StackWrapper> res = Lists.newArrayList();
		for (StackWrapper s : lis) {
			int contains = contains(res, s);
			if (contains == -1)
				res.add(s);
			else {
				StackWrapper tmp = res.get(contains);
				tmp.num += s.num;
				res.set(contains, tmp);
			}
		}
		return res;
	}

	private int contains(List<StackWrapper> lis, StackWrapper stack) {
		for (int i = 0; i < lis.size(); i++)
			if (lis.get(i).stack.isItemEqual(stack.stack))
				return i;
		return -1;
	}
	
	private void add(List<StackWrapper> lis, StackWrapper stack) {
		for (int i = 0; i < lis.size(); i++)
			if (lis.get(i).stack.isItemEqual(stack.stack))
				return i;
		return -1;
	}

	@Override
	public void onJeiHelpersAvailable(IJeiHelpers jeiHelpers) {
	}

	@Override
	public void onItemRegistryAvailable(IItemRegistry itemRegistry) {
	}

	@Override
	public void onRecipeRegistryAvailable(IRecipeRegistry recipeRegistry) {
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
	}

	public static class Wrapper extends BlankRecipeWrapper implements ITooltipCallback<ItemStack> {

		private ItemStack in;
		private List<Drop> out;

		public Wrapper(ItemStack in, List<Drop> out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public List getInputs() {
			return Collections.singletonList(in);
		}

		@Override
		public List getOutputs() {
			List<ItemStack> lis = Lists.newArrayList();
			for (Drop d : out)
				lis.add(d.out);
			return lis;
		}

		private float chance(ItemStack s) {
			for (Drop d : out)
				if (d.out.isItemEqual(s))
					return d.chance;
			return 0f;
		}

		@Override
		public void onTooltip(int slotIndex, boolean input, ItemStack ingredient, List<String> tooltip) {
			if (!input)
				tooltip.add(chance(ingredient) + " %");
		}

	}

	public static class Handler implements IRecipeHandler<Wrapper> {

		@Override
		public Class<Wrapper> getRecipeClass() {
			return Wrapper.class;
		}

		@Override
		public String getRecipeCategoryUid() {
			return BlockDrops.MODID;
		}

		@Override
		public IRecipeWrapper getRecipeWrapper(Wrapper recipe) {
			return recipe;
		}

		@Override
		public boolean isRecipeValid(Wrapper recipe) {
			return !recipe.getOutputs().isEmpty();
		}

	}

	public static class Category extends BlankRecipeCategory {
		private final IDrawable background;

		public Category(IGuiHelper h) {
			background = h.createBlankDrawable(80, 80);
		}

		@Override
		public String getUid() {
			return BlockDrops.MODID;
		}

		@Override
		public String getTitle() {
			return "Block Drops";
		}

		@Override
		public IDrawable getBackground() {
			return background;
		}

		@Override
		public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper) {
			if (!(recipeWrapper instanceof Wrapper))
				return;
			IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
			itemStacks.init(0, true, 50, 0);
			itemStacks.setFromRecipe(0, recipeWrapper.getInputs());
			itemStacks.addTooltipCallback((ITooltipCallback<ItemStack>) recipeWrapper);
			for (int i = 0; i < recipeWrapper.getOutputs().size(); i++) {
				itemStacks.init(i + 1, false, 1 + i * 20, 50);
				itemStacks.set(i + 1, (ItemStack) recipeWrapper.getOutputs().get(i));
			}

		}
	}

	static class Drop {
		ItemStack out;
		float chance;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Drop)
				return out.isItemEqual(((Drop) obj).out) && chance == ((Drop) obj).chance;
			return false;
		}

		public Drop(ItemStack out, float chance) {
			super();
			this.out = out;
			this.chance = chance;
		}

	}
}