package mrriegel.blockdrops;

import java.util.Map;

import mrriegel.blockdrops.Plugin.StackWrapper;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;

@Mod(modid = BlockDrops.MODID, name = BlockDrops.MODNAME, version = BlockDrops.VERSION, dependencies = "after:JEI@[2.0.0,);", clientSideOnly = true)
public class BlockDrops {
	public static final String MODID = "blockdrops";
	public static final String VERSION = "1.0.0";
	public static final String MODNAME = "Block Drops";

	@Instance(BlockDrops.MODID)
	public static BlockDrops instance;

	public static boolean all;
	public static int iteration;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();

		all = config.getBoolean("allDrops", Configuration.CATEGORY_CLIENT, false, "Show block drops of any block.");
		iteration = config.getInt("iteration", Configuration.CATEGORY_CLIENT, 6000, 1, 99999, "Number of calculation. The higher the more precise the chance.");

		if (config.hasChanged()) {
			config.save();
		}
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		EntityCreeper c = new EntityCreeper(null);
		System.out.println("zik");
		System.out.println(Blocks.anvil.onBlockPlaced(null, BlockPos.ORIGIN, EnumFacing.DOWN, 0, 0, 0, 0, c));
		System.out.println(Blocks.anvil.onBlockPlaced(null, BlockPos.ORIGIN, EnumFacing.DOWN, 0, 0, 0, 2, c));
		System.out.println(Blocks.anvil.onBlockPlaced(null, BlockPos.ORIGIN, EnumFacing.DOWN, 0, 0, 0, 4, c));
		Map<StackWrapper, Pair<Integer, Integer>> m = Maps.newHashMap();
		StackWrapper x = new StackWrapper(new ItemStack(Items.coal), 33);
		StackWrapper y = new StackWrapper(new ItemStack(Items.coal), 33);
		System.out.println(x.equals(y));
		m.put(x, null);
		m.put(y, null);
		System.out.println("mm: " + m.size());
	}

}
