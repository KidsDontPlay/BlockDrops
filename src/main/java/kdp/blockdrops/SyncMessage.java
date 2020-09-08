package kdp.blockdrops;

import java.util.List;

public class SyncMessage {

    public List<DropRecipe> recipes;

    public SyncMessage() {
    }

    public SyncMessage(List<DropRecipe> recipes) {
        this.recipes = recipes;
    }
}
