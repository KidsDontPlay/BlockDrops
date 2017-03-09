package mrriegel.blockdrops;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Mod(modid = BlockDrops.MODID, name = BlockDrops.MODNAME, version = BlockDrops.VERSION, dependencies = "after:JEI@[4.2.0,);", clientSideOnly = true)
public class BlockDrops {
	public static final String MODID = "blockdrops";
	public static final String VERSION = "1.2.0";
	public static final String MODNAME = "Block Drops";

	@Instance(BlockDrops.MODID)
	public static BlockDrops instance;

	public static boolean all, showChance, showMinMax;
	public static int iteration;
	public static Set<String> blacklist;

	public static List<Wrapper> recipeWrappers;
	public static Gson gson;
	public static Logger logger;

	private File recipeWrapFile;
	private File modHashFile;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		File configDir = new File(event.getModConfigurationDirectory(), "BlockDrops");
		recipeWrapFile = new File(configDir, "blockdrops.key");
		modHashFile = new File(configDir, "modVersions.key");
		Configuration config = new Configuration(new File(configDir, "config.cfg"));
		config.load();
		all = config.getBoolean("allDrops", Configuration.CATEGORY_CLIENT, false, "Show block drops of any block.");
		showChance = config.getBoolean("showChance", Configuration.CATEGORY_CLIENT, true, "Show chance of drops.");
		showMinMax = config.getBoolean("showMinMax", Configuration.CATEGORY_CLIENT, true, "Show minimum and maximum of drops.");
		iteration = config.getInt("iteration", Configuration.CATEGORY_CLIENT, 5000, 1, 99999, "Number of calculation. The higher the more precise the chance.");
		blacklist = Sets.newHashSet(config.getStringList("blacklist", Configuration.CATEGORY_CLIENT, new String[] { "flatcoloredblocks", "chisel", "xtones", "wallpapercraft", "sonarcore", "microblockcbe" }, "Mod IDs of mods that won't be scanned."));

		if (config.hasChanged()) {
			config.save();
		}
		gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Wrapper.class, new WrapperJson()).create();
		logger = event.getModLog();
	}

	@SuppressWarnings("serial")
	@EventHandler
	public void init(FMLInitializationEvent event) throws FileNotFoundException {
		if (recipeWrapFile.exists()) {
			recipeWrappers = gson.fromJson(new BufferedReader(new FileReader(recipeWrapFile)), new TypeToken<List<Wrapper>>() {
			}.getType());
		} else {
			recipeWrappers = Lists.newArrayList();
		}
	}

	@SuppressWarnings("serial")
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) throws IOException {
		Map<String, String> expectedModVersions;
		boolean hasChanged = false;
		if (modHashFile.exists()) {
			expectedModVersions = gson.fromJson(new BufferedReader(new FileReader(modHashFile)), new TypeToken<Map<String, String>>() {
			}.getType());
		} else {
			expectedModVersions = Maps.newHashMap();
			hasChanged = true;
		}

		Map<String, String> modVersions = Maps.newHashMap();
		Set<String> updatedMods = Sets.newHashSet();

		for (ModContainer modContainer : Loader.instance().getActiveModList()) {
			if (blacklist.contains(modContainer.getModId())) {
				continue;
			}

			String modId = modContainer.getModId();
			String version = modContainer.getVersion();
			modVersions.put(modId, version);

			if (expectedModVersions.containsKey(modId) && version.equals(expectedModVersions.get(modId))) {
				expectedModVersions.remove(modId);
				logger.info("Skipping cached mod version {}@{}", modId, version);
				continue;
			}

			updatedMods.add(modId);
			hasChanged = true;
		}

		hasChanged = hasChanged || !expectedModVersions.isEmpty();

		if (!recipeWrapFile.exists() || recipeWrappers == null || hasChanged) {
			logger.info("Updating mod block drops for: {}", updatedMods);
			updatedMods.add("minecraft");
			if (recipeWrappers == null || !recipeWrapFile.exists()) {
				recipeWrappers = Lists.newArrayList(Plugin.getRecipes(updatedMods, blacklist, true));
			} else {
				recipeWrappers.addAll(Plugin.getRecipes(updatedMods, blacklist, false));
				List<Wrapper> wraps = Lists.newArrayList();

				for (Wrapper w : recipeWrappers)
					if (w.getIn() != null && w.getIn().getItem() != null && w.getOutputs().stream().allMatch(s -> s != null && s.getItem() != null))
						wraps.add(w);
				recipeWrappers = wraps;
			}

			FileWriter fw = new FileWriter(modHashFile);
			fw.write(gson.toJson(modVersions));
			fw.close();

			fw = new FileWriter(recipeWrapFile);
			fw.write(gson.toJson(recipeWrappers));
			fw.close();
		}
	}

}
