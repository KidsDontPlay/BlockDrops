package mrriegel.blockdrops;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

@JEIPlugin
public class Plugin implements IModPlugin {
	@Override
	public void register(IModRegistry registry) {
		registry.handleRecipes(Wrapper.class, r -> r, BlockDrops.MODID);
		registry.addRecipes(BlockDrops.recipeWrappers, BlockDrops.MODID);
		for (Item i : ForgeRegistries.ITEMS) {
			if (i instanceof ItemPickaxe)
				registry.addRecipeCatalyst(new ItemStack(i), BlockDrops.MODID);
		}
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry) {
		registry.addRecipeCategories(new Category());
	}

}
