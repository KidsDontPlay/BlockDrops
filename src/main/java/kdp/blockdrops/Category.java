package kdp.blockdrops;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.gui.elements.DrawableBlank;
import mezz.jei.gui.recipes.IRecipeLogicStateListener;

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
                Drop d = recipe.getDropForItem(ingredient);
                boolean one = d.getChances().values().stream().distinct().count() == 1;
                for (int x = 0; x < (one ? 1 : 4); x++) {
                    String chance = BlockDrops.showChance.get() ?
                            String.format("%.2f", d.getChances().get(x) * 100F) + " %  " :
                            "";
                    String minmax = BlockDrops.showMinMax.get() ?
                            "Min: " + d.getMins().get(x) + " Max: " + d.getMaxs().get(x) :
                            "";
                    if (!chance.isEmpty() || !minmax.isEmpty()) {
                        tooltip.add((one ?
                                "" :
                                TextFormatting.BLUE + "Fortune " + (0 != x ?
                                        I18n.format("enchantment.level." + x) :
                                        0) + " ") + TextFormatting.GRAY + chance + minmax);
                    }
                }
                if (recipe.getOutputs().size() > 9) {
                    tooltip.add(
                            TextFormatting.RED + "There are too many possible drops. Use left and right key to cycle.");
                }
            }
        });
        for (int i = 0; i < Math.min(recipe.getOutputs().size(), 9); i++) {
            stackGroup.init(i + 1, false, 9 + i * 18, 20);
            stackGroup.set(i + 1, recipe.getOutputs().get((i + recipe.getIndex())));
        }
    }

    @Override
    public boolean handleClick(DropRecipe recipe, double mouseX, double mouseY, int mouseButton) {
        System.out.println(mouseX + " " + mouseY);
        ((IRecipeLogicStateListener) Minecraft.getInstance().currentScreen).onStateChange();
        return false;
    }
}
