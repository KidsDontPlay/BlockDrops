package kdp.blockdrops;

import java.util.Comparator;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

@JeiPlugin
public class Plugin implements IModPlugin {

    static List<DropRecipe> recipes = null;

    @Override
    public ResourceLocation getPluginUid() {
        return BlockDrops.RL;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (recipes != null) {
            registration.addRecipes(recipes, BlockDrops.RL);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ForgeRegistries.ITEMS.getValues().stream()//
                .filter(i -> i instanceof PickaxeItem && i.getRegistryName().getNamespace().equals("minecraft"))//
                .sorted(Comparator.comparingInt(i -> ((PickaxeItem) i).getTier().getMaxUses()).reversed())//
                .forEach(i -> registration.addRecipeCatalyst(new ItemStack(i), BlockDrops.RL));
    }
}
