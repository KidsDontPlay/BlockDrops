package mrriegel.blockdrops;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ITooltipCallback;
import mezz.jei.api.recipe.BlankRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class Category extends BlankRecipeCategory {
	private final IDrawable background;

	public Category(IGuiHelper h) {
		background = h.createDrawable(new ResourceLocation(BlockDrops.MODID + ":gui/GUI.png"), 0, 0, 166, 74);
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
		itemStacks.init(0, true, 74, 5);
		itemStacks.setFromRecipe(0, recipeWrapper.getInputs());
		itemStacks.addTooltipCallback((ITooltipCallback<ItemStack>) recipeWrapper);
		for (int i = 0; i < recipeWrapper.getOutputs().size(); i++) {
			if (i < 8)
				itemStacks.init(i + 1, false, 11 + i * 18, 51);
			else
				itemStacks.init(i + 1, false, 11 + (i - 8) * 18, 69);
			itemStacks.set(i + 1, (ItemStack) recipeWrapper.getOutputs().get(i));
		}

	}
}