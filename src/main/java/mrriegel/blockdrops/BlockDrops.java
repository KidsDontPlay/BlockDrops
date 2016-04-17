package mrriegel.blockdrops;

import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = BlockDrops.MODID, name = BlockDrops.MODNAME, version = BlockDrops.VERSION)
public class BlockDrops {
	public static final String MODID = "blockdrops";
	public static final String VERSION = "1.0.0";
	public static final String MODNAME = "BBLOC DOPRS";

	@Instance(BlockDrops.MODID)
	public static BlockDrops instance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		EntityCreeper c = new EntityCreeper(null);
		System.out.println("zik");
		System.out.println(Blocks.anvil.onBlockPlaced(null, BlockPos.ORIGIN, EnumFacing.DOWN, 0, 0, 0, 0, c));
		System.out.println(Blocks.anvil.onBlockPlaced(null, BlockPos.ORIGIN, EnumFacing.DOWN, 0, 0, 0, 2, c));
		System.out.println(Blocks.anvil.onBlockPlaced(null, BlockPos.ORIGIN, EnumFacing.DOWN, 0, 0, 0, 4, c));

	}

}
