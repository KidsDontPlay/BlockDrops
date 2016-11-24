package mrriegel.blockdrops;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreenDemo;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;

@Mod(modid = BlockDrops.MODID, name = BlockDrops.MODNAME, version = BlockDrops.VERSION, dependencies = "after:JEI@[3.0.0,);", clientSideOnly = true)
public class BlockDrops {
	public static final String MODID = "blockdrops";
	public static final String VERSION = "1.0.11";
	public static final String MODNAME = "Block Drops";

	@Instance(BlockDrops.MODID)
	public static BlockDrops instance;

	public static boolean all, showChance, showMinMax;
	public static int iteration;
	public static List<String> blacklist;

	public static List<Wrapper> recipeWrappers;
	public static Gson gson;
	public static Logger logger;
	public static WorldClient world;
	public static EntityPlayerSP player;

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
		blacklist = Lists.newArrayList(config.getStringList("blacklist", Configuration.CATEGORY_CLIENT, new String[] { "flatcoloredblocks", "chisel" }, "Mod IDs of mods that won't scanned."));

		if (config.hasChanged()) {
			config.save();
		}
		gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Wrapper.class, new WrapperJson()).create();
		logger = LogManager.getLogger();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) throws FileNotFoundException {
		if (recipeWrapFile.exists()) {
			recipeWrappers = gson.fromJson(
					new BufferedReader(new FileReader(recipeWrapFile)),
					new TypeToken<List<Wrapper>>(){}.getType());
		}
		else {
			recipeWrappers = Lists.newArrayList();
		}
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) throws IOException {
		NetHandlerPlayClient netHandler = new NetHandlerPlayClient(Minecraft.getMinecraft(), new GuiScreenDemo(), new NetworkManager(EnumPacketDirection.CLIENTBOUND), new GameProfile(UUID.randomUUID(), this.toString().toLowerCase().concat(MODID)));
		try {
			world = new WorldClient(netHandler, new WorldSettings(new WorldInfo(new NBTTagCompound())), 0, EnumDifficulty.HARD, new Profiler());
		} catch (Throwable t) {
		}
		try {
			player = new EntityPlayerSP(Minecraft.getMinecraft(), world, netHandler, new StatisticsManager());
		} catch (Throwable t) {
		}

		Map<String, String> expectedModVersions;
		boolean hasChanged = false;
		if (modHashFile.exists()) {
			expectedModVersions = gson.fromJson(
					new BufferedReader(new FileReader(modHashFile)),
					new TypeToken<Map<String, String>>(){}.getType());
		}
		else {
			expectedModVersions = new HashMap<String, String>();
			hasChanged = true;
		}

		Map<String, String> modVersions = new HashMap<String, String>();
		modVersions.put("minecraft", "");

		Set<String> updatedMods = new HashSet<String>();
		if (!expectedModVersions.containsKey("minecraft")) {
			updatedMods.add("minecraft");
		}

		for (ModContainer modContainer : Loader.instance().getActiveModList()) {
			if (blacklist.contains(modContainer.getModId())) {
				continue;
			}

			String modId = modContainer.getModId();
			String version = modContainer.getVersion();
			modVersions.put(modContainer.getModId(), modContainer.getVersion());

			if (expectedModVersions.containsKey(modId) && version.equals(expectedModVersions.get(modId))) {
				expectedModVersions.remove(modId);
				logger.info("Skipping cached mod version {}@{}", modId, version);
				continue;
			}

			updatedMods.add(modId);
			hasChanged = true;
		}

		hasChanged = hasChanged || !expectedModVersions.isEmpty();

		if (!recipeWrapFile.exists() || hasChanged) {
			logger.info("Updating mod block drops for: {}", updatedMods);
			recipeWrappers.addAll(Plugin.getRecipes(updatedMods));

			FileWriter fw = new FileWriter(modHashFile);
			fw.write(gson.toJson(modVersions));
			fw.close();

			fw = new FileWriter(recipeWrapFile);
			fw.write(gson.toJson(recipeWrappers));
			fw.close();
		}
	}

}
