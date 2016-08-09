package mrriegel.blockdrops;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenDemo;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
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
	public static final String VERSION = "1.0.10";
	public static final String MODNAME = "Block Drops";

	@Instance(BlockDrops.MODID)
	public static BlockDrops instance;

	public static boolean all, showChance, showMinMax;
	public static int iteration;
	public static List<String> blacklist;

	public static List<Wrapper> wrappers;
	public static Gson gson;
	public static Logger logger;
	public static WorldClient world;

	File configDir;
	File wraps;
	File hash;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		configDir = new File(event.getModConfigurationDirectory(), "BlockDrops");
		wraps = new File(configDir, "blockdrops.key");
		hash = new File(configDir, "hash.key");
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
	public void postInit(FMLPostInitializationEvent event) throws IOException {
		NetHandlerPlayClient netHandler = new NetHandlerPlayClient(Minecraft.getMinecraft(), new GuiScreenDemo(), new NetworkManager(EnumPacketDirection.CLIENTBOUND), new GameProfile(UUID.randomUUID(), this.toString().toLowerCase().concat(MODID)));
		world = new WorldClient(netHandler, new WorldSettings(new WorldInfo(new NBTTagCompound())), 0, EnumDifficulty.HARD, new Profiler());
		StringBuilder s = new StringBuilder();
		List<ModContainer> mods = Lists.newArrayList(Loader.instance().getActiveModList());
		Collections.sort(mods, new Comparator<ModContainer>() {
			@Override
			public int compare(ModContainer o1, ModContainer o2) {
				return o1.getModId().compareTo(o1.getModId());
			}
		});
		for (ModContainer m : mods) {
			s.append(m.getModId() + m.getVersion());
		}
		int h = s.toString().hashCode();
		boolean hashChanged = false;
		if (!hash.exists()) {
			hash.createNewFile();
			FileWriter fw = new FileWriter(hash);
			fw.write(gson.toJson(h));
			fw.close();
			hashChanged = true;
		} else {
			int x = gson.fromJson(new BufferedReader(new FileReader(hash)), new TypeToken<Integer>() {
			}.getType());
			if (x != h) {
				hash.createNewFile();
				FileWriter fw = new FileWriter(hash);
				fw.write(gson.toJson(h));
				fw.close();
				hashChanged = true;
			}
		}
		if (!wraps.exists() || hashChanged) {
			wraps.createNewFile();
			wrappers = Lists.newArrayList(Plugin.getRecipes());
			FileWriter fw = new FileWriter(wraps);
			fw.write(gson.toJson(wrappers));
			fw.close();
		} else {
			wrappers = gson.fromJson(new BufferedReader(new FileReader(wraps)), new TypeToken<List<Wrapper>>() {
			}.getType());
		}
	}

}
