package mrriegel.blockdrops;

import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.gui.elements.DrawableBlank;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;

public class Category implements IRecipeCategory<Wrapper> {

	private ResourceLocation hopper = new ResourceLocation("textures/gui/container/hopper.png");

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
		return new DrawableBlank(180, 40);
	}

	@Override
	public void drawExtras(Minecraft minecraft) {
		//		IDrawable draw = getBackground();
		//		GuiUtils.drawGradientRect(0, 0, 0, draw.getWidth(), draw.getHeight(), Color.RED.getRGB(), Color.GREEN.getRGB());
		drawSlot(81, 1);
		for (int i = 9; i < 170; i += 18)
			drawSlot(i, 20);
	}

	private void drawSlot(int x, int y) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(hopper);
		GuiUtils.drawTexturedModalRect(x, y, 43, 19, 18, 18, 0);
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, Wrapper recipeWrapper, IIngredients ingredients) {
		IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
		itemStacks.init(0, true, 81, 1);
		itemStacks.set(0, recipeWrapper.getInputs());
		itemStacks.addTooltipCallback(recipeWrapper);
		for (int i = 0; i < recipeWrapper.getOutputs().size(); i++) {
			if (i < 9)
				itemStacks.init(i + 1, false, 9 + i * 18, 20);
			else
				itemStacks.init(i + 1, false, 9 + (i - 9) * 18, 38);
			itemStacks.set(i + 1, recipeWrapper.getOutputs().get(i));
		}
	}

	@Override
	public String getModName() {
		return BlockDrops.MODNAME;
	}
}