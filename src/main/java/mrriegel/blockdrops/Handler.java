package mrriegel.blockdrops;

import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;

public class Handler implements IRecipeHandler<Wrapper> {

	@Override
	public Class<Wrapper> getRecipeClass() {
		return Wrapper.class;
	}

	@Override
	public IRecipeWrapper getRecipeWrapper(Wrapper recipe) {
		return recipe;
	}

	@Override
	public boolean isRecipeValid(Wrapper recipe) {
		return !recipe.getOutputs().isEmpty();
	}

	@Override
	public String getRecipeCategoryUid(Wrapper recipe) {
		return BlockDrops.MODID;
	}

}