package kdp.blockdrops;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.gui.elements.DrawableBlank;

public class Category implements IRecipeCategory<DropRecipe> {
    private static final ResourceLocation hopper = new ResourceLocation("textures/gui/container/hopper.png");

    @Override
    public ResourceLocation getUid() {
        return BlockDrops.RL;
    }

    @Override
    public Class<? extends DropRecipe> getRecipeClass() {
        return DropRecipe.class;
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
    public IDrawable getIcon() {
        return null;
    }

    @Override
    public void draw(DropRecipe recipe, double mouseX, double mouseY) {
        Minecraft.getInstance().fontRenderer.drawStringWithShadow("Albatros", 10, 10, 0xFFFFFF00);
        drawSlot(81, 1);
        for (int i = 9; i < 170; i += 18)
            drawSlot(i, 20);
    }

    private void drawSlot(int x, int y) {
        Minecraft.getInstance().getTextureManager().bindTexture(hopper);
        GuiUtils.drawTexturedModalRect(x, y, 43, 19, 18, 18, 0);
    }

    @Override
    public void setIngredients(DropRecipe recipe, IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM, recipe.getInputs());
        ingredients.setOutputs(VanillaTypes.ITEM, recipe.getOutputs());
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, DropRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup stackGroup = recipeLayout.getItemStacks();
        stackGroup.init(0, true, 81, 1);
        stackGroup.set(0, recipe.getInputs());
        stackGroup.addTooltipCallback((int slotIndex, boolean input, ItemStack ingredient, List<String> tooltip) -> {
            if (!input) {
                tooltip.add("NICE");
            }
        });
    }
}
