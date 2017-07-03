package mrriegel.blockdrops.util;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import mrriegel.blockdrops.BlockDrops;
import mrriegel.blockdrops.Wrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class WrapperJson implements JsonDeserializer<Wrapper>, JsonSerializer<Wrapper> {

	@Override
	public JsonElement serialize(Wrapper src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject json = new JsonObject();
		ItemStack stack = src.getIn();
		json.addProperty("name", stack.getItem().getRegistryName().toString());
		json.addProperty("meta", stack.getItemDamage());
		json.addProperty("length", src.getOut().size());
		for (int i = 0; i < src.getOut().size(); i++) {
			Drop d = src.getOut().get(i);
			json.addProperty("name" + i, d.out.getItem().getRegistryName().toString());
			json.addProperty("meta" + i, d.out.getItemDamage());
			json.addProperty("0chance" + i, d.chance0);
			json.addProperty("1chance" + i, d.chance1);
			json.addProperty("2chance" + i, d.chance2);
			json.addProperty("3chance" + i, d.chance3);
			json.addProperty("0pair" + i, BlockDrops.gson.toJson(d.pair0));
			json.addProperty("1pair" + i, BlockDrops.gson.toJson(d.pair1));
			json.addProperty("2pair" + i, BlockDrops.gson.toJson(d.pair2));
			json.addProperty("3pair" + i, BlockDrops.gson.toJson(d.pair3));
		}

		return json;
	}

	@SuppressWarnings("serial")
	@Override
	public Wrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		Wrapper wrap = new Wrapper(null, Collections.EMPTY_LIST);
		String name = json.getAsJsonObject().get("name").getAsString();
		int meta = json.getAsJsonObject().get("meta").getAsInt();
		ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(name)), 1, meta);
		wrap.setIn(stack);

		int length = json.getAsJsonObject().get("length").getAsInt();
		List<Drop> lis = Lists.newArrayList();
		for (int i = 0; i < length; i++) {
			Drop d = new Drop(ItemStack.EMPTY, 0, 0, 0, 0, null, null, null, null);
			String n = json.getAsJsonObject().get("name" + i).getAsString();
			int m = json.getAsJsonObject().get("meta" + i).getAsInt();
			ItemStack st = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(n)), 1, m);
			d.out = st;
			d.chance0 = json.getAsJsonObject().get("0chance" + i).getAsFloat();
			d.chance1 = json.getAsJsonObject().get("1chance" + i).getAsFloat();
			d.chance2 = json.getAsJsonObject().get("2chance" + i).getAsFloat();
			d.chance3 = json.getAsJsonObject().get("3chance" + i).getAsFloat();
			d.pair0 = BlockDrops.gson.fromJson(json.getAsJsonObject().get("0pair" + i).getAsString(), new TypeToken<MutablePair<Integer, Integer>>() {
			}.getType());
			d.pair1 = BlockDrops.gson.fromJson(json.getAsJsonObject().get("1pair" + i).getAsString(), new TypeToken<MutablePair<Integer, Integer>>() {
			}.getType());
			d.pair2 = BlockDrops.gson.fromJson(json.getAsJsonObject().get("2pair" + i).getAsString(), new TypeToken<MutablePair<Integer, Integer>>() {
			}.getType());
			d.pair3 = BlockDrops.gson.fromJson(json.getAsJsonObject().get("3pair" + i).getAsString(), new TypeToken<MutablePair<Integer, Integer>>() {
			}.getType());
			lis.add(d);
		}
		wrap.setOut(lis);
		return wrap;
	}

}
