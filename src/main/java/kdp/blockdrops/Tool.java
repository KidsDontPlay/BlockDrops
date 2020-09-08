package kdp.blockdrops;

import java.util.Collections;

import net.minecraft.block.BlockState;
import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.item.crafting.Ingredient;

public class Tool extends ToolItem {
    private static final IItemTier tier = new IItemTier() {
        @Override
        public int getMaxUses() {
            return Integer.MAX_VALUE;
        }

        @Override
        public float getEfficiency() {
            return 100F;
        }

        @Override
        public float getAttackDamage() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getHarvestLevel() {
            return 10;
        }

        @Override
        public int getEnchantability() {
            return Integer.MAX_VALUE;
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public Ingredient getRepairMaterial() {
            return null;
        }
    };

    public Tool() {
        super(1000F, 1000F, tier, Collections.emptySet(), new Item.Properties());
        setRegistryName("tool");
    }

    @Override
    public boolean canHarvestBlock(ItemStack stack, BlockState state) {
        return true;
    }
}
