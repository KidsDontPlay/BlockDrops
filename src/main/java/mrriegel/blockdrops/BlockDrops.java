package mrriegel.blockdrops;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = BlockDrops.MODID, name = BlockDrops.MODNAME, version = BlockDrops.VERSION, dependencies = "after:JEI@[3.0.0,);", clientSideOnly = true)
public class BlockDrops {
	public static final String MODID = "blockdrops";
	public static final String VERSION = "1.0.3";
	public static final String MODNAME = "Block Drops";

	@Instance(BlockDrops.MODID)
	public static BlockDrops instance;

	public static boolean all, showChance, showMinMax;
	public static int iteration;
	
	public static List<Wrapper> wrappers;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();

		all = config.getBoolean("allDrops", Configuration.CATEGORY_CLIENT, false, "Show block drops of any block.");
		showChance = config.getBoolean("showChance", Configuration.CATEGORY_CLIENT, true, "Show chance of drops.");
		showMinMax = config.getBoolean("showMinMax", Configuration.CATEGORY_CLIENT, true, "Show minimum and maximum of drops.");
		iteration = config.getInt("iteration", Configuration.CATEGORY_CLIENT, 6000, 1, 99999, "Number of calculation. The higher the more precise the chance.");

		if (config.hasChanged()) {
			config.save();
		}
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event){
		wrappers=Lists.newArrayList(Plugin.getRecipes());
	}

}
