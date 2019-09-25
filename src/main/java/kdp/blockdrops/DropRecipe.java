package kdp.blockdrops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.INBTSerializable;

public class DropRecipe implements INBTSerializable<CompoundNBT> {

    private ItemStack in;
    private List<Drop> drops;

    public DropRecipe(ItemStack in, List<Drop> drops) {
        this.in = in;
        this.drops = drops;
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
    }
}
