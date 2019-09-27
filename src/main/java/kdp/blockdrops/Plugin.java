package kdp.blockdrops;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import net.minecraft.item.Item;
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
        List<DropRecipe> list = new ArrayList<>();
        Random random = new Random();
        List<Item> items = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        for (int i = 0; i < 8; i++) {
            DropRecipe a = new DropRecipe();
            list.add(a);
            a.setIn(new ItemStack(items.get(random.nextInt(items.size()))));
            List<Drop> drops = new ArrayList<>();
            a.setDrops(drops);
            for (int j = 0; j < random.nextInt(8) + 1; j++) {
                Drop d = new Drop(new ItemStack(items.get(random.nextInt(items.size()))));
                for (int k = 0; k < 4; k++) {
                    d.getChances().put(k, random.nextFloat());
                    d.getMins().put(k, random.nextInt(5));
                    d.getMaxs().put(k, random.nextInt(5));
                }
                drops.add(d);

            }
        }
        registration.addRecipes(list, BlockDrops.RL);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ForgeRegistries.ITEMS.getValues().stream()//
                .filter(i -> i instanceof PickaxeItem && i.getRegistryName().getNamespace().equals("minecraft"))//
                .sorted(Comparator.comparingInt(i -> ((PickaxeItem) i).getTier().getMaxUses()).reversed())//
                .forEach(i -> registration.addRecipeCatalyst(new ItemStack(i), BlockDrops.RL));
    }
}
