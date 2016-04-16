package mrriegel.blockdrops;

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
	}

}
