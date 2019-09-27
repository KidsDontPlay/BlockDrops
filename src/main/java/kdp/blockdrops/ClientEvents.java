package kdp.blockdrops;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

import mezz.jei.api.runtime.IRecipesGui;

@Mod.EventBusSubscriber(modid = BlockDrops.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    public static void key(InputEvent.KeyInputEvent event) {
        if (Minecraft.getInstance().currentScreen instanceof IRecipesGui && event.getAction() == GLFW.GLFW_RELEASE) {
            boolean left = event.getKey() == GLFW.GLFW_KEY_LEFT;
            boolean right = event.getKey() == GLFW.GLFW_KEY_RIGHT;
        }
    }
}
