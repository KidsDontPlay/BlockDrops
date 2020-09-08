package kdp.blockdrops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

@Mod(BlockDrops.MOD_ID)
public class BlockDrops {

    public static final String MOD_ID = "blockdrops";
    public static final Logger LOG = LogManager.getLogger(BlockDrops.class);
    public static final ResourceLocation RL = new ResourceLocation(MOD_ID, "drops");

    public static ForgeConfigSpec.BooleanValue all, showChance, showMinMax, allStates;
    private static ForgeConfigSpec.IntValue iterations;
    private static ForgeConfigSpec.ConfigValue<List<String>> blacklistedMods;

    public static final UUID uuid = UUID.fromString("1ef41968-f9b8-4350-834e-367f49476a56");

    private static final Hash.Strategy<ItemStack> strategy = new Hash.Strategy<ItemStack>() {
        @Override
        public int hashCode(ItemStack o) {
            return o == null || o.isEmpty() ? 0 : o.getItem().hashCode();
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            return a != null && b != null && a.getItem() == b.getItem();
        }
    };
    private static final Tool toolItem = new Tool();

    private static Path recipesPath;

    private static final String VERSION = "1.0";
    private static final SimpleChannel simpleChannel = NetworkRegistry
            .newSimpleChannel(new ResourceLocation(MOD_ID, "ch1"), () -> VERSION, VERSION::equals, VERSION::equals);

    private static List<DropRecipe> recipes = Collections.emptyList();

    public BlockDrops() {
        Pair<Object, ForgeConfigSpec> pairCommon = new ForgeConfigSpec.Builder().configure(b -> {
            all = b.comment("Show block drops of any block").define("allBlocks", false);
            iterations = b
                    .comment("Amount of calculation iterations. The higher the more precise the calculation results")
                    .defineInRange("iterations", 4000, 500, 20000);
            blacklistedMods = b.comment("Mod IDs of mods that won't be scanned").define("blacklistedMods",
                    Arrays.asList("flatcoloredblocks", "chisel", "xtones", "wallpapercraft", "sonarcore",
                            "microblockcbe"));
            allStates = b.comment("Only one blockstate of a block is used to calculate the drops",
                    "Should ordinarily not affect the calculation", "(enable this if you miss some drops)")
                    .define("allStates", false);
            return null;
        });
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, pairCommon.getValue());
        Pair<Object, ForgeConfigSpec> pairClient = new ForgeConfigSpec.Builder().configure(b -> {
            showChance = b.comment("Show chance of drops").define("showChance", true);
            showMinMax = b.comment("Show minimum and maximum of drops").define("showMinMax", true);
            return null;
        });
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, pairClient.getValue());
        MinecraftForge.EVENT_BUS.register(this);

        recipesPath = Paths.get("config", MOD_ID + ".txt");
        simpleChannel.registerMessage(0, SyncMessage.class, (m, pb) -> {
            ListNBT listNBT = new ListNBT();
            m.recipes.forEach(recipe -> listNBT.add(recipe.serializeNBT()));
            CompoundNBT tag = new CompoundNBT();
            tag.put("list", listNBT);
            pb.writeCompoundTag(tag);
        }, pb -> {
            SyncMessage m = new SyncMessage();
            CompoundNBT nbt = pb.readCompoundTag();
            if (nbt != null) {
                ListNBT listNBT = (ListNBT) nbt.get("list");
                if (listNBT != null) {
                    m.recipes = listNBT.stream().map(n -> {
                        DropRecipe r = new DropRecipe();
                        r.deserializeNBT((CompoundNBT) n);
                        return r;
                    }).collect(Collectors.toList());
                }
            }
            return m;
        }, (m, s) -> {
            s.get().enqueueWork(() -> Plugin.recipes = m.recipes);
            s.get().setPacketHandled(true);
        });
    }

    public static List<DropRecipe> getAllRecipes(Set<String> allowedIDs, FMLServerStartingEvent event) {
        List<DropRecipe> result = new ArrayList<>();
        ServerWorld world = event.getServer().getWorld(World.field_234918_g_);
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(uuid, "Hacker"));
        Stopwatch sw = Stopwatch.createStarted();
        LOG.info("Block drop calculation started...");
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block.getRegistryName() != null && allowedIDs.contains(block.getRegistryName().getNamespace())) {
                List<BlockState> validStates = block.getStateContainer().getValidStates();
                if (!allStates.get()) {
                    validStates = Collections.singletonList(validStates.get(validStates.size() - 1));
                }
                if (validStates.size() > 20) {
                    validStates = Collections.singletonList(block.getDefaultState());
                }
                Set<String> dropStrings = new HashSet<>();
                for (BlockState state : validStates) {
                    List<Drop> drops = getDrops(state, world, player);
                    String ds = drops.toString();
                    if (drops.isEmpty() || dropStrings.contains(ds)) {
                        continue;
                    }
                    dropStrings.add(ds);
                    result.add(new DropRecipe(getItemForBlock(state, world), drops));
                }
            }
        }
        LOG.info("Block drop calculation finished after {} milliseconds.", sw.elapsed(TimeUnit.MILLISECONDS));
        return result;
    }

    private static List<Drop> getDrops(BlockState state, ServerWorld world, FakePlayer player) {
        try {
            List<Drop> result = new ArrayList<>();
            ItemStack show;
            try {
                show = getItemForBlock(state, world);
            } catch (RuntimeException | NoSuchMethodError e) {
                return result;
            }
            if (show.isEmpty()) {
                return result;
            }
            int iteration = iterations.get();
            Int2ObjectOpenHashMap<Object2IntOpenCustomHashMap<ItemStack>> resultMap = new Int2ObjectOpenHashMap<>();
            Int2ObjectOpenHashMap<Object2ObjectOpenCustomHashMap<ItemStack, MutablePair<Integer, Integer>>> minmaxs = new Int2ObjectOpenHashMap<>();
            for (int fortune = 0; fortune < 4; fortune++) {
                ItemStack tool = new ItemStack(toolItem);
                if (fortune > 0) {
                    tool.addEnchantment(Enchantments.FORTUNE, fortune);
                }
                TileEntity tile = null;
                try {
                    tile = state.createTileEntity(world);
                    if (tile != null) tile.setWorldAndPos(world, BlockPos.ZERO);
                } catch (Exception ignored) {
                }
                player.setItemStackToSlot(EquipmentSlotType.MAINHAND, tool);
                LootContext.Builder builder = new LootContext.Builder(world)//
                        .withParameter(LootParameters.POSITION, BlockPos.ZERO)//
                        .withParameter(LootParameters.BLOCK_STATE, state)//
                        .withNullableParameter(LootParameters.BLOCK_ENTITY, tile)//
                        .withNullableParameter(LootParameters.THIS_ENTITY, player)//
                        .withParameter(LootParameters.TOOL, tool);
                Object2IntOpenCustomHashMap<ItemStack> stacks = new Object2IntOpenCustomHashMap<>(strategy);
                Object2ObjectOpenCustomHashMap<ItemStack, MutablePair<Integer, Integer>> minmax = new Object2ObjectOpenCustomHashMap<>(
                        strategy);
                minmax.defaultReturnValue(MutablePair.of(9999, 0));
                for (int i = 0; i < iteration; i++) {
                    NonNullList<ItemStack> drops = NonNullList.create();
                    drops.addAll(state.getDrops(builder));
                    for (ItemStack drop : drops) {
                        if (drop.isEmpty()) continue;
                        if (all.get() || drop.getItem() != show.getItem() || !(show.getItem() instanceof BlockItem)) {
                            stacks.addTo(drop, drop.getCount());
                            minmax.merge(drop, MutablePair.of(drop.getCount(), drop.getCount()), (pOld, pNew) -> {
                                pOld.setLeft(Math.min(pNew.getLeft(), pOld.getLeft()));
                                pOld.setRight(Math.max(pNew.getRight(), pOld.getRight()));
                                return pOld;
                            });
                        }
                    }
                }
                minmaxs.put(fortune, minmax);
                resultMap.put(fortune, stacks);
            }
            ObjectOpenCustomHashSet<ItemStack> allStacks = new ObjectOpenCustomHashSet<>(strategy);
            resultMap.values().forEach(map -> allStacks.addAll(map.keySet()));
            allStacks.stream()//
                    .sorted(Comparator.comparingInt(s -> Item.getIdFromItem(s.getItem())))//
                    .forEach(s -> {
                        Drop drop = new Drop(s);
                        for (int fortune = 0; fortune < 4; fortune++) {
                            drop.getChances().put(fortune, resultMap.get(fortune).getInt(s) / (float) iteration);
                            drop.getMaxs().put(fortune, minmaxs.get(fortune).get(s).getRight().intValue());
                            drop.getMins().put(fortune, drop.getChances().get(fortune) < 1F ?
                                0 : minmaxs.get(fortune).get(s).getLeft());
                        }
                        result.add(drop);
                    });
            return result;
        } catch (Exception e) {
            LOG.info("Error ({} : {}) while calculating drops for {}", e.getClass().getSimpleName(), e.getMessage(),
                    state);
            return Collections.emptyList();
        }
    }

    private static ItemStack getItemForBlock(BlockState state, World world) {
        @SuppressWarnings("deprecation")
        ItemStack item2 = state.getBlock().getItem(world, BlockPos.ZERO, state);
        if (item2.getItem() instanceof BlockItem && ((BlockItem) item2.getItem()).getBlock() == state.getBlock()) {
            return item2;
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public void serverStart(FMLServerStartingEvent event) throws IOException, CommandSyntaxException {
        List<DropRecipe> allRecipes;
        String hash = ModList.get().getMods().stream().map(mi -> "{" + mi.getModId() + ":" + mi.getVersion() + "}")
                .sorted().collect(Collectors.joining(","));
        List<String> strings = null;
        boolean calculate = Files.notExists(recipesPath);
        if (!calculate) {
            strings = Files.readAllLines(recipesPath);
            String stringHash = strings.get(0);
            if (!Objects.equals(stringHash, hash)) {
                calculate = true;
            }
        }
        if (calculate) {
            allRecipes = getAllRecipes(ModList.get().getMods().stream().map(ModInfo::getModId)
                    .filter(s -> !blacklistedMods.get().contains(s)).collect(Collectors.toSet()), event);
            List<String> nbts = allRecipes.stream().map(DropRecipe::serializeNBT).map(Object::toString)
                    .collect(Collectors.toList());
            nbts.add(0, hash);
            Files.write(recipesPath, nbts);
        } else {
            allRecipes = new ArrayList<>();
            for (int i = 1; i < strings.size(); i++) {
                DropRecipe dropRecipe = new DropRecipe();
                dropRecipe.deserializeNBT(JsonToNBT.getTagFromJson(strings.get(i)));
                allRecipes.add(dropRecipe);
            }
        }
        recipes = allRecipes;
        if (event.getServer().isSinglePlayer()) {
            Plugin.recipes = recipes;
        }
    }

    @SubscribeEvent
    public void construct(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity && !((ServerPlayerEntity) event.getEntity()).server
                .isSinglePlayer()) {
            simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getEntity()),
                    new SyncMessage(recipes));
        }
    }
}
