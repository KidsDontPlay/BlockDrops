package mrriegel.blockdrops;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fml.common.ProgressManager;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@JEIPlugin
public class Plugin implements IModPlugin {
	@Override
	public void register(IModRegistry registry) {
		registry.addRecipeCategories(new Category(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeHandlers(new Handler());
		registry.addRecipes(BlockDrops.wrappers);

		for (ResourceLocation r : Item.REGISTRY.getKeys()) {
			Item i = Item.REGISTRY.getObject(r);
			if (i instanceof ItemPickaxe)
				registry.addRecipeCategoryCraftingItem(new ItemStack(i), BlockDrops.MODID);
		}
	}

	public static List<Wrapper> getRecipes() {
		List<Wrapper> res = Lists.newArrayList();
		Set<BlockWrapper> blocks = Sets.newHashSet();
		for (ResourceLocation r : Block.REGISTRY.getKeys()) {
			Block b = Block.REGISTRY.getObject(r);
			if (Item.getItemFromBlock(b) == null || b == Blocks.BEDROCK)
				continue;
			List<ItemStack> lis = Lists.newArrayList();
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

		ProgressManager.ProgressBar bar = ProgressManager.push("Analysing Drops", x.size());
		for (BlockWrapper w : x) {
			List<Drop> drops;
			bar.step(w.block.getRegistryName().toString());
			try {
				drops = getList(w);
			} catch (Exception e) {
				// e.printStackTrace();
				drops = Collections.EMPTY_LIST;
			}
			if (drops.isEmpty())
				continue;
			res.add(new Wrapper(w.getStack(), drops));
		}
		ProgressManager.pop(bar);
		return res;

	}

	private static List<Drop> getList(BlockWrapper wrap) {
		List<Drop> drops = Lists.newArrayList();
		if (wrap.getStack().getItem() == null)
			return drops;
		List<StackWrapper> stacks0 = Lists.newArrayList(), stacks1 = Lists.newArrayList(), stacks2 = Lists.newArrayList(), stacks3 = Lists.newArrayList();
		Map<StackWrapper, Pair<Integer, Integer>> pairs0 = Maps.newHashMap(), pairs1 = Maps.newHashMap(), pairs2 = Maps.newHashMap(), pairs3 = Maps.newHashMap();
		for (int i = 0; i < BlockDrops.iteration; i++) {
			for (int j = 0; j < 4; j++) {
				List<ItemStack> list = wrap.block.getDrops(Minecraft.getMinecraft().theWorld, BlockPos.ORIGIN, wrap.getState(), j);
				List<ItemStack> lis = Lists.newArrayList(list);
				lis.removeAll(Collections.singleton(null));
				if (BlockDrops.showMinMax)
					if (i % 2 == 0)
						switch (j) {
						case 0:
							add(pairs0, lis);
							break;
						case 1:
							add(pairs1, lis);
							break;
						case 2:
							add(pairs2, lis);
							break;
						case 3:
							add(pairs3, lis);
							break;
						}

				for (ItemStack s : lis) {
					if (s == null)
						continue;
					switch (j) {
					case 0:
						add(stacks0, s);
						break;
					case 1:
						add(stacks1, s);
						break;
					case 2:
						add(stacks2, s);
						break;
					case 3:
						add(stacks3, s);
						break;
					}
				}
			}
		}

		Comparator<StackWrapper> comp = new Comparator<Plugin.StackWrapper>() {
			@Override
			public int compare(StackWrapper o1, StackWrapper o2) {
				int id = Integer.compare(Item.getIdFromItem(o1.stack.getItem()), Item.getIdFromItem(o2.stack.getItem()));
				int meta = Integer.compare(o1.stack.getItemDamage(), o2.stack.getItemDamage());
				return id != 0 ? id : meta;
			}
		};

		List<StackWrapper> stacks = Lists.newArrayList();
		for (StackWrapper w : stacks0)
			add(stacks, w.stack);
		for (StackWrapper w : stacks1)
			add(stacks, w.stack);
		for (StackWrapper w : stacks2)
			add(stacks, w.stack);
		for (StackWrapper w : stacks3)
			add(stacks, w.stack);

		if (!BlockDrops.all) {
			Iterator<StackWrapper> it = stacks.iterator();
			while (it.hasNext()) {
				StackWrapper tmp = it.next();
				if (tmp.stack.isItemEqual(wrap.getStack()))
					it.remove();
			}
		}
		stacks.sort(comp);

		for (int i = 0; i < stacks.size(); i++) {
			StackWrapper stack = stacks.get(i);
			float s0 = getChance(stacks0, stack.stack);
			float s1 = getChance(stacks1, stack.stack);
			float s2 = getChance(stacks2, stack.stack);
			float s3 = getChance(stacks3, stack.stack);
			drops.add(new Drop(stack.stack, s0, s1, s2, s3, pairs0.get(stack), pairs1.get(stack), pairs2.get(stack), pairs3.get(stack)));
		}
		return drops;

	}

	private static float getChance(List<StackWrapper> stacks, ItemStack stack) {
		if (!BlockDrops.showChance)
			return 0f;
		int con = contains(stacks, stack);
		if (con == -1)
			return 0F;
		return 100F * ((float) stacks.get(con).size / (float) BlockDrops.iteration);
	}

	private static int contains(List<StackWrapper> lis, ItemStack stack) {
		for (int i = 0; i < lis.size(); i++)
			if (lis.get(i).stack.isItemEqual(stack))
				return i;
		return -1;
	}

	private static void add(List<StackWrapper> lis, ItemStack stack) {
		if (lis == null)
			lis = Lists.newArrayList();
		int con = contains(lis, stack);
		if (con == -1)
			lis.add(new StackWrapper(stack, stack.stackSize));
		else {
			StackWrapper tmp = lis.get(con);
			tmp.size += stack.stackSize;
			lis.set(con, tmp);
		}
	}

	private static void add(Map<StackWrapper, Pair<Integer, Integer>> map, List<ItemStack> lis) {
		if (map == null)
			map = Maps.newHashMap();
		List<StackWrapper> list = Lists.newArrayList();
		for (ItemStack s : lis)
			add(list, s);
		for (StackWrapper w : list) {
			if (map.get(w) == null)
				map.put(w, new MutablePair<Integer, Integer>(10000, 0));
			int min = map.get(w).getLeft();
			int max = map.get(w).getRight();
			Pair<Integer, Integer> pair = new MutablePair<Integer, Integer>(Math.min(min, w.size), Math.max(max, w.size));
			map.put(w, pair);
		}
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
	}

	static class Drop {
		public ItemStack out;
		public float chance0, chance1, chance2, chance3;
		public Pair<Integer, Integer> pair0, pair1, pair2, pair3;

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Drop other = (Drop) obj;
			if (Float.floatToIntBits(chance0) != Float.floatToIntBits(other.chance0))
				return false;
			if (Float.floatToIntBits(chance1) != Float.floatToIntBits(other.chance1))
				return false;
			if (Float.floatToIntBits(chance2) != Float.floatToIntBits(other.chance2))
				return false;
			if (Float.floatToIntBits(chance3) != Float.floatToIntBits(other.chance3))
				return false;
			if (out == null) {
				if (other.out != null)
					return false;
			} else if (!out.isItemEqual(other.out))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Drop [out=" + out + ", chance0=" + chance0 + ", chance1=" + chance1 + ", chance2=" + chance2 + ", chance3=" + chance3 + ", pair0=" + pair0 + ", pair1=" + pair1 + ", pair2=" + pair2 + ", pair3=" + pair3 + "]";
		}

		public Drop(ItemStack out, float chance0, float chance1, float chance2, float chance3, Pair<Integer, Integer> pair0, Pair<Integer, Integer> pair1, Pair<Integer, Integer> pair2, Pair<Integer, Integer> pair3) {
			super();
			this.out = out;
			this.chance0 = chance0;
			this.chance1 = chance1;
			this.chance2 = chance2;
			this.chance3 = chance3;
			this.pair0 = pair0;
			this.pair1 = pair1;
			this.pair2 = pair2;
			this.pair3 = pair3;
		}

	}

	static class StackWrapper {
		ItemStack stack;
		int size;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof StackWrapper)
				return stack.isItemEqual(((StackWrapper) obj).stack);
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
			return result;
		}

		@Override
		public String toString() {
			return "StackWrapper [stack=" + stack + ", num=" + size + "]";
		}

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
			int m = Item.getItemFromBlock(block).getMetadata(meta);
			return block.onBlockPlaced(Minecraft.getMinecraft().theWorld, BlockPos.ORIGIN, EnumFacing.UP, 0, 0, 0, m, Minecraft.getMinecraft().thePlayer);
		}

		ItemStack getStack() {
			return new ItemStack(block, 1, meta);
		}
	}
}