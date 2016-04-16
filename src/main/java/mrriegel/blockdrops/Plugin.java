package mrriegel.blockdrops;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
import mezz.jei.api.recipe.BlankRecipeCategory;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

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
			int m = block instanceof BlockAnvil ? meta << 2 : meta;
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
			List<Drop> drops = getList(w);
			if (drops.isEmpty())
				continue;
			res.add(new Wrapper(w.getStack(), drops));
		}
		return res;

	}

	private List<Drop> getList(BlockWrapper wrap) {
		List<Drop> drops = Lists.newArrayList();
		if (wrap.getStack().getItem() == null)
			return drops;
		List<StackWrapper> stacks0 = Lists.newArrayList(), stacks1 = Lists.newArrayList(), stacks2 = Lists.newArrayList(), stacks3 = Lists.newArrayList();
		final int num = 6000;
		for (int i = 0; i < num; i++) {
			for (int j = 0; j < 4; j++) {
				List<ItemStack> lis = wrap.block.getDrops(Minecraft.getMinecraft().theWorld, BlockPos.ORIGIN, wrap.getState(), j);
				for (ItemStack s : lis) {
					if (s == null)
						continue;
					switch (j) {
					case 0:
						stacks0 = add(stacks0, s);
						break;
					case 1:
						stacks1 = add(stacks1, s);
						break;
					case 2:
						stacks2 = add(stacks2, s);
						break;
					case 3:
						stacks3 = add(stacks3, s);
						break;
					}
				}
			}
		}
		Iterator<StackWrapper> it0 = stacks0.iterator();
		while (it0.hasNext()) {
			StackWrapper tmp = it0.next();
			if (tmp.stack.isItemEqual(wrap.getStack()))
				it0.remove();
		}
		Iterator<StackWrapper> it1 = stacks1.iterator();
		while (it1.hasNext()) {
			StackWrapper tmp = it1.next();
			if (tmp.stack.isItemEqual(wrap.getStack()))
				it1.remove();
		}
		Iterator<StackWrapper> it2 = stacks2.iterator();
		while (it2.hasNext()) {
			StackWrapper tmp = it2.next();
			if (tmp.stack.isItemEqual(wrap.getStack()))
				it2.remove();
		}
		Iterator<StackWrapper> it3 = stacks3.iterator();
		while (it3.hasNext()) {
			StackWrapper tmp = it3.next();
			if (tmp.stack.isItemEqual(wrap.getStack()))
				it3.remove();
		}
		Comparator<StackWrapper> comp = new Comparator<Plugin.StackWrapper>() {
			@Override
			public int compare(StackWrapper o1, StackWrapper o2) {
				return o1.stack.toString().compareTo(o2.stack.toString());
			}
		};
		stacks0.sort(comp);
		stacks1.sort(comp);
		stacks2.sort(comp);
		stacks3.sort(comp);
		if (!(stacks0.size() == stacks1.size() && stacks1.size() == stacks2.size() && stacks2.size() == stacks3.size()))
			throw new RuntimeException("bug");
		for (int i = 0; i < stacks0.size(); i++) {
			float s0 = 100 * ((float) stacks0.get(i).num / (float) num);
			float s1 = 100 * ((float) stacks1.get(i).num / (float) num);
			float s2 = 100 * ((float) stacks2.get(i).num / (float) num);
			float s3 = 100 * ((float) stacks3.get(i).num / (float) num);
			drops.add(new Drop(stacks0.get(i).stack, s0, s1, s2, s3));
		}
		return drops;

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

		@Override
		public String toString() {
			return "StackWrapper [stack=" + stack + ", num=" + num + "]";
		}

	}

	private int contains(List<StackWrapper> lis, ItemStack stack) {
		for (int i = 0; i < lis.size(); i++)
			if (lis.get(i).stack.isItemEqual(stack))
				return i;
		return -1;
	}

	private List<StackWrapper> add(List<StackWrapper> lis, ItemStack stack) {
		if (lis == null)
			lis = Lists.newArrayList();
		int con = contains(lis, stack);
		if (con == -1)
			lis.add(new StackWrapper(stack, stack.stackSize));
		else {
			StackWrapper tmp = lis.get(con);
			tmp.num += stack.stackSize;
			lis.set(con, tmp);
		}
		return lis;
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

		private float chance(ItemStack s, int fortune) {
			for (Drop d : out)
				if (d.out.isItemEqual(s)) {
					switch (fortune) {
					case 0:
						return d.chance0;
					case 1:
						return d.chance1;
					case 2:
						return d.chance2;
					case 3:
						return d.chance3;
					default:
						break;
					}
				}
			return 0f;
		}

		@Override
		public void onTooltip(int slotIndex, boolean input, ItemStack ingredient, List<String> tooltip) {
			if (!input) {
				long x = (System.currentTimeMillis() / 1500l) % 4;
				tooltip.add(EnumChatFormatting.BLUE + "Fortune " + (StatCollector.canTranslate("enchantment.level." + x) ? StatCollector.translateToLocal("enchantment.level." + x) : 0) + " " + EnumChatFormatting.GRAY + String.format("%.1f", chance(ingredient, (int) x)) + " %");
			}
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
		float chance0, chance1, chance2, chance3;

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Drop other = (Drop) obj;
			if (!out.isItemEqual(other.out))
				return false;
			if (Float.floatToIntBits(chance0) != Float.floatToIntBits(other.chance0))
				return false;
			if (Float.floatToIntBits(chance1) != Float.floatToIntBits(other.chance1))
				return false;
			if (Float.floatToIntBits(chance2) != Float.floatToIntBits(other.chance2))
				return false;
			if (Float.floatToIntBits(chance3) != Float.floatToIntBits(other.chance3))
				return false;
			return true;
		}

		public Drop(ItemStack out, float chance0, float chance1, float chance2, float chance3) {
			this.out = out;
			this.chance0 = chance0;
			this.chance1 = chance1;
			this.chance2 = chance2;
			this.chance3 = chance3;
		}

	}
}