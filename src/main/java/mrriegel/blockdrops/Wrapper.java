package mrriegel.blockdrops;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import mezz.jei.api.gui.ITooltipCallback;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import mrriegel.blockdrops.util.Drop;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;

public class Wrapper implements IRecipeWrapper, ITooltipCallback<ItemStack> {

	private ItemStack in;
	private List<Drop> out;
	private int index = 0, maxIndex;

	public Wrapper(ItemStack in, List<Drop> out) {
		this.in = in;
		this.out = out;
		this.maxIndex = Math.max(0, out.size() - 9);
	}

	public List<ItemStack> getInputs() {
		return Collections.singletonList(in);
	}

	public List<ItemStack> getOutputs() {
		return out.stream().map(d -> d.out).collect(Collectors.toList());
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
	public String toString() {
		return "Wrapper [in=" + in + ", out=" + out + "]";
	}

	private Pair<Integer, Integer> pair(ItemStack s, int fortune) {
		for (Drop d : out)
			if (d.out.isItemEqual(s)) {
				switch (fortune) {
				case 0:
					return d.pair0;
				case 1:
					return d.pair1;
				case 2:
					return d.pair2;
				case 3:
					return d.pair3;
				default:
					break;
				}
			}
		return MutablePair.of(0, 0);
	}

	@Override
	public void onTooltip(int slotIndex, boolean input, ItemStack ingredient, List<String> tooltip) {
		if (!input) {
			boolean one = IntStream.range(0, 4).mapToObj(i -> Float.floatToIntBits(chance(ingredient, i))).distinct().count() == 1;
			for (int x = 0; x < (one ? 1 : 4); x++) {
				String chance = BlockDrops.showChance ? String.format("%.2f", chance(ingredient, (int) x)) + " %  " : "";
				String minmax = BlockDrops.showMinMax ? "Min: " + pair(ingredient, (int) x).getLeft() + "  Max: " + pair(ingredient, (int) x).getRight() : "";
				if (BlockDrops.showChance || BlockDrops.showMinMax)
					tooltip.add((one ? "" : TextFormatting.BLUE + "Fortune " + (0l != x ? I18n.format("enchantment.level." + x) : 0) + " ") + TextFormatting.GRAY + chance + minmax);
			}
			if (out.size() > 9)
				tooltip.add(TextFormatting.RED + "There are too many possible drops. Use left and right key to cycle.");
		}
	}

	public ItemStack getIn() {
		return in;
	}

	public void setIn(ItemStack in) {
		this.in = in;
	}

	public List<Drop> getOut() {
		return out;
	}

	public void setOut(List<Drop> out) {
		this.out = out;
		this.maxIndex = Math.max(0, out.size() - 9);
	}

	@Override
	public void getIngredients(IIngredients ingredients) {
		ingredients.setInputs(ItemStack.class, getInputs());
		ingredients.setOutputs(ItemStack.class, getOutputs());
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = MathHelper.clamp(index, 0, maxIndex);
	}

	public void increaseIndex() {
		this.index = MathHelper.clamp(index + 1, 0, maxIndex);
	}

	public void decreaseIndex() {
		this.index = MathHelper.clamp(index - 1, 0, maxIndex);
	}

}