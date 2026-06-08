package com.github.raspberry1111.create_hyperdrive;

import com.github.raspberry1111.create_hyperdrive.blocks.hyperdrive.HyperdriveBlockItem;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory;
import net.minecraft.world.item.crafting.RecipeHolder;

public class AllRecipes {
    private static final RecipeHolder<ConversionRecipe> HYPERDRIVE = ConversionRecipe.create(HyperdriveBlockItem.emptyStack(), HyperdriveBlockItem.filledStack()); // using empty hyperdrive on shulker
    private static final RecipeHolder<ConversionRecipe> EMPTY_HYPERDRIVE = ConversionRecipe.create(HyperdriveBlockItem.filledStack(), HyperdriveBlockItem.emptyStack()); // using filled hyperdrive on shulker box

    public static void register() {
        MysteriousItemConversionCategory.RECIPES.remove(HYPERDRIVE);
        MysteriousItemConversionCategory.RECIPES.add(HYPERDRIVE);

        MysteriousItemConversionCategory.RECIPES.remove(EMPTY_HYPERDRIVE);
        MysteriousItemConversionCategory.RECIPES.add(HYPERDRIVE);
    }
}