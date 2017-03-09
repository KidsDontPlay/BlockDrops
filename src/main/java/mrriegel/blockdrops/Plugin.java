package mrriegel.blockdrops;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mezz.jei.api.BlankModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.util.FakeClientPlayer;
import mezz.jei.util.FakeClientWorld;
import mrriegel.blockdrops.util.BlockWrapper;
import mrriegel.blockdrops.util.Drop;
import mrriegel.blockdrops.util.StackWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.ProgressManager;

import org.apache.commons.lang3.tuple.MutablePair;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@JEIPlugin
public class Plugin extends BlankModPlugin {
	@Override
	public void register(IModRegistry registry) {
		registry.addRecipeCategories(new Category(registry.getJeiHelpers().getGuiHelper()));
		registry.addRecipeHandlers(new Handler());
		registry.addRecipes(BlockDrops.recipeWrappers);

		for (Item i : Item.REGISTRY) {
			if (i instanceof ItemPickaxe)
				registry.addRecipeCategoryCraftingItem(new ItemStack(i), BlockDrops.MODID);
		}
	}

	public static List<Wrapper> getRecipes(Set<String> mods, Set<String> blacklist, boolean all) {
		List<Wrapper> res = Lists.newArrayList();
		List<BlockWrapper> blocks = Lists.newArrayList();
		for (ResourceLocation r : Block.REGISTRY.getKeys()) {
			if (!all && !mods.contains(r.getResourceDomain()))
				continue;
			Block b = Block.REGISTRY.getObject(r);
			if (Item.getItemFromBlock(b) == null || b == Blocks.BEDROCK)
				continue;
			if (blacklist.stream().anyMatch(s -> s.equalsIgnoreCase(r.getResourceDomain())))
				continue;
			NonNullList<ItemStack> lis = NonNullList.create();
			b.getSubBlocks(Item.getItemFromBlock(b), b.getCreativeTabToDisplayOn(), lis);
			for (ItemStack s : lis)
				blocks.add(new BlockWrapper(b, s.getItemDamage()));
		}
		blocks.sort(new Comparator<BlockWrapper>() {
			@Override
			public int compare(BlockWrapper o1, BlockWrapper o2) {
				int id = Integer.compare(Block.getIdFromBlock(o1.block), Block.getIdFromBlock(o2.block));
				int meta = Integer.compare(o1.meta, o2.meta);
				return id != 0 ? id : meta;
			}
		});

		ProgressManager.ProgressBar bar = ProgressManager.push("Analysing Drops", blocks.size());
		for (BlockWrapper w : blocks) {
			List<Drop> drops;
			bar.step(w.block.getRegistryName().toString());
			try {
				drops = getList(w);
			} catch (Throwable e) {
				BlockDrops.logger.error("An error occured while calculating drops for " + w.block.getLocalizedName() + " (" + e.getClass() + ")");
				drops = Collections.EMPTY_LIST;
			}
			if (drops.isEmpty() || w.getStack().isEmpty())
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
		Map<StackWrapper, MutablePair<Integer, Integer>> pairs0 = Maps.newHashMap(), pairs1 = Maps.newHashMap(), pairs2 = Maps.newHashMap(), pairs3 = Maps.newHashMap();
		IBlockState state = wrap.getState();
		boolean crashed = false;
		for (int i = 0; i < BlockDrops.iteration; i++) {
			for (int j = 0; j < 4; j++) {
				List<ItemStack> list = wrap.block.getDrops(FakeClientWorld.getInstance(), BlockPos.ORIGIN, state, j);
				List<ItemStack> lis = Lists.newArrayList(list);
				try {
					if (!crashed)
						net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(lis, FakeClientWorld.getInstance(), BlockPos.ORIGIN, state, j, 1f, false, FakeClientPlayer.getInstance());
				} catch (Throwable t) {
					crashed = true;
				}
				lis.removeAll(Collections.singleton(null));
				Iterables.removeIf(lis, s -> s.isEmpty());
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
					if (s.isEmpty())
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

		Comparator<StackWrapper> comp = (o1, o2) -> {
			int id = Integer.compare(Item.getIdFromItem(o1.stack.getItem()), Item.getIdFromItem(o2.stack.getItem()));
			int meta = Integer.compare(o1.stack.getItemDamage(), o2.stack.getItemDamage());
			return id != 0 ? id : meta;
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
			if (stack.stack != null && stack.stack.getItem() != null)
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
		if (stack == null || stack.getItem() == null)
			return;
		int con = contains(lis, stack);
		if (con == -1)
			lis.add(new StackWrapper(stack, stack.getCount()));
		else {
			StackWrapper tmp = lis.get(con);
			tmp.size += stack.getCount();
			lis.set(con, tmp);
		}
	}

	private static void add(Map<StackWrapper, MutablePair<Integer, Integer>> map, List<ItemStack> lis) {
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
			MutablePair<Integer, Integer> pair = new MutablePair<Integer, Integer>(Math.min(min, w.size), Math.max(max, w.size));
			map.put(w, pair);
		}
	}
}
