package mrriegel.blockdrops.util;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class Drop {
	public ItemStack out;
	public float chance0, chance1, chance2, chance3;
	public Pair<Integer, Integer> pair0, pair1, pair2, pair3;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Drop other = (Drop) obj;
		if (Float.floatToIntBits(chance0) != Float.floatToIntBits(other.chance0))
			return false;
		if (Float.floatToIntBits(chance1) != Float.floatToIntBits(other.chance1))
			return false;
		if (Float.floatToIntBits(chance2) != Float.floatToIntBits(other.chance2))
			return false;
		if (Float.floatToIntBits(chance3) != Float.floatToIntBits(other.chance3))
			return false;
		if (out == null) {
			if (other.out != null)
				return false;
		} else if (!out.isItemEqual(other.out))
			return false;
		return true;
	}

	public Drop(ItemStack out, float chance0, float chance1, float chance2, float chance3, Pair<Integer, Integer> pair0, Pair<Integer, Integer> pair1, Pair<Integer, Integer> pair2, Pair<Integer, Integer> pair3) {
		super();
		this.out = ItemHandlerHelper.copyStackWithSize(out, 1);
		this.chance0 = chance0;
		this.chance1 = chance1;
		this.chance2 = chance2;
		this.chance3 = chance3;

		this.pair0 = pair0;
		if (this.pair0 == null)
			this.pair0 = Pair.of(0, 0);
		this.pair1 = pair1;
		if (this.pair1 == null)
			this.pair1 = Pair.of(0, 0);
		this.pair2 = pair2;
		if (this.pair2 == null)
			this.pair2 = Pair.of(0, 0);
		this.pair3 = pair3;
		if (this.pair3 == null)
			this.pair3 = Pair.of(0, 0);

		if (this.chance0 < 100)
			this.pair0 = Pair.of(0, this.pair0.getRight());
		if (this.chance1 < 100)
			this.pair1 = Pair.of(0, this.pair1.getRight());
		if (this.chance2 < 100)
			this.pair2 = Pair.of(0, this.pair2.getRight());
		if (this.chance3 < 100)
			this.pair3 = Pair.of(0, this.pair3.getRight());
	}

}
