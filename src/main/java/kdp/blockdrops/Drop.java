package kdp.blockdrops;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemHandlerHelper;

import it.unimi.dsi.fastutil.ints.Int2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;

public class Drop implements INBTSerializable<CompoundNBT> {
    private ItemStack out;
    private final Int2FloatLinkedOpenHashMap chances = new Int2FloatLinkedOpenHashMap();
    private final Int2IntLinkedOpenHashMap mins = new Int2IntLinkedOpenHashMap();
    private final Int2IntLinkedOpenHashMap maxs = new Int2IntLinkedOpenHashMap();

    public Drop(ItemStack out) {
        this.out = ItemHandlerHelper.copyStackWithSize(out, 1);
    }

    Drop() {
    }

    public ItemStack getOut() {
        return out;
    }

    public Int2FloatLinkedOpenHashMap getChances() {
        return chances;
    }

    public Int2IntLinkedOpenHashMap getMins() {
        return mins;
    }

    public Int2IntLinkedOpenHashMap getMaxs() {
        return maxs;
    }

    @Override
    public String toString() {
        return out.getItem().getRegistryName().toString();
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("out", out.write(new CompoundNBT()));
        IntArrayNBT ck = new IntArrayNBT(chances.keySet().toIntArray());
        IntArrayNBT cv = new IntArrayNBT(chances.values().stream().mapToInt(Float::floatToRawIntBits).toArray());
        IntArrayNBT nk = new IntArrayNBT(mins.keySet().toIntArray());
        IntArrayNBT nv = new IntArrayNBT(mins.values().toIntArray());
        IntArrayNBT xk = new IntArrayNBT(maxs.keySet().toIntArray());
        IntArrayNBT xv = new IntArrayNBT(maxs.values().toIntArray());
        nbt.put("ck", ck);
        nbt.put("cv", cv);
        nbt.put("nk", nk);
        nbt.put("nv", nv);
        nbt.put("xk", xk);
        nbt.put("xv", xv);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        out = ItemStack.read(nbt.getCompound("out"));
        int[] ck = nbt.getIntArray("ck");
        Float[] cv = Arrays.stream(nbt.getIntArray("cv")).mapToObj(Float::intBitsToFloat).toArray(Float[]::new);
        int[] nk = nbt.getIntArray("nk");
        int[] nv = nbt.getIntArray("nv");
        int[] xk = nbt.getIntArray("xk");
        int[] xv = nbt.getIntArray("xv");
        for (int i = 0; i < 4; i++) {
            chances.put(ck[i], cv[i].floatValue());
            mins.put(nk[i], nv[i]);
            maxs.put(xk[i], xv[i]);
        }

    }
}
