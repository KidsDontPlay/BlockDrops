package kdp.blockdrops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.INBTSerializable;

public class DropRecipe implements INBTSerializable<CompoundNBT> {

    private ItemStack in;
    private List<Drop> drops;
    private final Cache<ItemStack, Drop> cache = CacheBuilder.newBuilder().build();
    //client only
    private int index, maxIndex;

    public DropRecipe(ItemStack in, List<Drop> drops) {
        this.in = in;
        this.drops = drops;
        this.maxIndex = Math.max(0, drops.size() - 9);
    }

    public DropRecipe() {
    }

    public List<ItemStack> getInputs() {
        return Collections.singletonList(in);
    }

    public List<ItemStack> getOutputs() {
        return drops.stream().map(Drop::getOut).collect(Collectors.toList());
    }

    public void setIn(ItemStack in) {
        this.in = in;
    }

    public void setDrops(List<Drop> drops) {
        this.drops = drops;
        this.maxIndex = Math.max(0, drops.size() - 9);
    }

    public Drop getDropForItem(ItemStack stack) {
        try {
            return cache.get(stack, () -> drops.stream().filter(drop -> drop.getOut().isItemEqual(stack)).findFirst().orElse(Drop.EMPTY));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public int getIndex() {
        return this.index;
    }

    public int getMaxIndex() {
        return maxIndex;
    }

    public void increaseIndex() {
        this.index = MathHelper.clamp(index + 1, 0, maxIndex);
    }

    public void decreaseIndex() {
        this.index = MathHelper.clamp(index - 1, 0, maxIndex);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("in", in.write(new CompoundNBT()));
        ListNBT listNBT = new ListNBT();
        drops.forEach(d -> listNBT.add(d.serializeNBT()));
        nbt.put("drops", listNBT);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        in = ItemStack.read(nbt.getCompound("in"));
        drops = new ArrayList<>();
        nbt.getList("drops", 10).forEach(n -> {
            Drop d = new Drop();
            d.deserializeNBT((CompoundNBT) n);
            drops.add(d);
        });
        maxIndex = Math.max(0, drops.size() - 9);
    }
}
