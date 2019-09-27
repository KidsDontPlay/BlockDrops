package kdp.blockdrops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
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

    public static ForgeConfigSpec.BooleanValue all, showChance, showMinMax, multithreaded;
    private static ForgeConfigSpec.IntValue iterations;
    private static ForgeConfigSpec.ConfigValue<List<String>> blacklistedMods;

    public static UUID uuid = UUID.fromString("1ef41968-f9b8-4350-834e-367f49476a56");

    private static Hash.Strategy<ItemStack> strategy = new Hash.Strategy<ItemStack>() {
        @Override
        public int hashCode(ItemStack o) {
            return o == null || o.isEmpty() ? 0 : o.getItem().hashCode();
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            return a != null && b != null && a.getItem() == b.getItem();
        }
    };

    public BlockDrops() {
        Pair<Object, ForgeConfigSpec> pairCommon = new ForgeConfigSpec.Builder().configure(b -> {
            all = b.comment("Show block drops of any block").define("allBlocks", false);
            multithreaded = b.comment("Multithreaded calculation of drops").define("multithreaded", true);
            iterations = b
                    .comment("Amount of calculation iterations. The higher the more precise the calculation results")
                    .defineInRange("iterations", 4000, 1, 50000);
            blacklistedMods = b.comment("Mod IDs of mods that won't be scanned").define("blacklistedMods",
                    Arrays.asList("flatcoloredblocks", "chisel", "xtones", "wallpapercraft", "sonarcore",
                            "microblockcbe"));
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
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    public static List<DropRecipe> getAllRecipes(Set<String> allowedIDs, FMLServerStartingEvent event) {
        List<DropRecipe> result = new ArrayList<>();
        ServerWorld world = event.getServer().getWorld(DimensionType.OVERWORLD);
        for (Block block : ForgeRegistries.BLOCKS) {
            if (allowedIDs.contains(block.getRegistryName().getNamespace())) {
                ImmutableList<BlockState> validStates = block.getStateContainer().getValidStates();
                Set<String> dropStrings = new HashSet<>();
                for (BlockState state : validStates) {
                    List<Drop> drops = getDrops(state, event);
                    String ds = drops.toString();
                    if (drops.isEmpty() || dropStrings.contains(ds)) {
                        continue;
                    }
                    dropStrings.add(ds);
                    result.add(new DropRecipe(state.getBlock().getItem(world, BlockPos.ZERO, state), drops));
                }
            }
        }
        return result;
    }

    private static List<Drop> getDrops(BlockState state, FMLServerStartingEvent event) {
        try {
            List<Drop> result = new ArrayList<>();
            ServerWorld world = event.getServer().getWorld(DimensionType.OVERWORLD);
            ServerPlayerEntity player = FakePlayerFactory.get(world, new GameProfile(uuid, "Hacker"));
            ItemStack show = state.getBlock().getItem(world, BlockPos.ZERO, state);
            if (show.isEmpty()) {
                return result;
            }
            boolean eventCrashed = false;
            int iteration = iterations.get();
            Int2ObjectOpenHashMap<Object2IntOpenCustomHashMap<ItemStack>> resultMap = new Int2ObjectOpenHashMap<>();
            Int2ObjectOpenHashMap<Object2ObjectOpenCustomHashMap<ItemStack, MutablePair<Integer, Integer>>> minmaxs = new Int2ObjectOpenHashMap<>();
            for (int fortune = 0; fortune < 4; fortune++) {
                ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
                if (fortune > 0) {
                    tool.addEnchantment(Enchantments.FORTUNE, fortune);
                }
                TileEntity tile = null;
                try {
                    tile = state.createTileEntity(world);
                } catch (Exception e) {
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
                for (int i = 0; i < iteration; i++) {
                    NonNullList<ItemStack> drops = NonNullList.create();
                    drops.addAll(state.getDrops(builder));
                    if (!eventCrashed) {
                        try {
                            ForgeEventFactory
                                    .fireBlockHarvesting(drops, world, BlockPos.ZERO, state, fortune, 1F, false,
                                            player);
                        } catch (Exception e) {
                            eventCrashed = true;
                        }
                    }
                    drops.removeIf(ItemStack::isEmpty);
                    for (ItemStack drop : drops) {
                        if (all.get() || drop.getItem() != show.getItem()) {
                            stacks.addTo(drop, drop.getCount());
                        }
                        /*minmax.compute(drop, (s, p) -> {
                            if (p == null) {
                                return MutablePair.of(drop.getCount(), drop.getCount());
                            }
                            p.setLeft(Math.min(drop.getCount(), p.getLeft()));
                            p.setRight(Math.max(drop.getCount(), p.getRight()));
                            return p;
                        });*/
                        minmax.merge(drop, MutablePair.of(drop.getCount(), drop.getCount()), (pOld, pNew) -> {
                            pOld.setLeft(Math.min(pNew.getLeft(), pOld.getLeft()));
                            pOld.setRight(Math.max(pNew.getRight(), pOld.getLeft()));
                            return pOld;
                        });
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
                        for (int i = 0; i < 4; i++) {
                            drop.getChances().put(i, resultMap.get(i).getInt(s) / (float) iteration);
                            drop.getMaxs().put(i, minmaxs.get(i).get(s).getRight().intValue());
                            drop.getMins().put(i, minmaxs.get(i).get(s).getLeft().intValue());
                        }
                        result.add(drop);
                    });
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @SubscribeEvent
    public void serverStart(FMLServerStartingEvent event) {
        if(true)return;
        List<DropRecipe> allRecipes = getAllRecipes(
                ModList.get().getMods().stream().map(ModInfo::getModId).filter(s -> !blacklistedMods.get().contains(s))
                        .collect(Collectors.toSet()), event);
        allRecipes.forEach(System.out::println);
    }

}
