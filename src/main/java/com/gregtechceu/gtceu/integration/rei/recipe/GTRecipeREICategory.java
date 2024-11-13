package com.gregtechceu.gtceu.integration.rei.recipe;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.rei.IGui2Renderer;
import com.lowdragmc.lowdraglib.rei.ModularUIDisplayCategory;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GTRecipeREICategory extends ModularUIDisplayCategory<GTRecipeDisplay> {

    public static final Function<GTRecipeCategory, CategoryIdentifier<GTRecipeDisplay>> CATEGORIES = Util
            .memoize(recipeCategory -> CategoryIdentifier.of(recipeCategory.getResourceLocation()));

    private final GTRecipeType recipeType;

    private final GTRecipeCategory category;
    private static final Map<GTRecipeCategory, GTRecipeREICategory> gtCategories = new Object2ObjectOpenHashMap<>();
    private static final Map<net.minecraft.world.item.crafting.RecipeType<?>, List<GTRecipeREICategory>> recipeTypeCategories = new Object2ObjectOpenHashMap<>();
    @Getter
    private final Renderer icon;
    @Getter
    private final Size size;

    public GTRecipeREICategory(GTRecipeType recipeType, @NotNull GTRecipeCategory category) {
        this.recipeType = recipeType;
        this.category = category;
        var size = recipeType.getRecipeUI().getJEISize();
        this.size = new Size(size.width + 8, size.height + 8);
        if (category.getIcon() instanceof ResourceTexture tex) {
            icon = IGui2Renderer.toDrawable(tex);
        } else if (recipeType.getIconSupplier() != null) {
            icon = IGui2Renderer.toDrawable(new ItemStackTexture(recipeType.getIconSupplier().get()));
        } else {
            icon = IGui2Renderer.toDrawable(new ItemStackTexture(Items.BARRIER.getDefaultInstance()));
        }

        gtCategories.put(category, this);
        recipeTypeCategories.compute(recipeType, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(this);
            return v;
        });
    }

    @Override
    public CategoryIdentifier<? extends GTRecipeDisplay> getCategoryIdentifier() {
        return CATEGORIES.apply(category);
    }

    @Override
    public int getDisplayHeight() {
        return getSize().height;
    }

    @Override
    public int getDisplayWidth(GTRecipeDisplay display) {
        return getSize().width;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.translatable(recipeType.registryName.toLanguageKey());
    }

    public static void registerDisplays(DisplayRegistry registry) {
        for (RecipeType<?> recipeType : BuiltInRegistries.RECIPE_TYPE) {
            if (recipeType instanceof GTRecipeType gtRecipeType) {
                if (gtRecipeType == GTRecipeTypes.FURNACE_RECIPES)
                    continue;
                if (Platform.isDevEnv() || gtRecipeType.getRecipeUI().isXEIVisible()) {
                    for (Map.Entry<GTRecipeCategory, List<GTRecipe>> entry : gtRecipeType.getRecipesByCategory()
                            .entrySet()) {
                        registry.registerRecipeFiller(GTRecipe.class, gtRecipeType,
                                recipe -> new GTRecipeDisplay(gtCategories.get(entry.getKey()), recipe));

                        if (gtRecipeType.isScanner()) {
                            List<GTRecipe> scannerRecipes = gtRecipeType.getRepresentativeRecipes();
                            if (!scannerRecipes.isEmpty()) {
                                scannerRecipes.stream()
                                        .map(recipe -> new GTRecipeDisplay(gtCategories.get(entry.getKey()), recipe))
                                        .forEach(registry::add);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void registerWorkStations(CategoryRegistry registry) {
        for (GTRecipeType gtRecipeType : GTRegistries.RECIPE_TYPES) {
            if (Platform.isDevEnv() || gtRecipeType.getRecipeUI().isXEIVisible()) {
                for (MachineDefinition machine : GTRegistries.MACHINES) {
                    if (machine.getRecipeTypes() != null) {
                        for (GTRecipeType type : machine.getRecipeTypes()) {
                            for (GTRecipeCategory category : type.getRecipeByCategory().keySet()) {
                                var reiCategory = GTRecipeREICategory.getCategoryFor(category);
                                if (reiCategory != null) {
                                    if (type == gtRecipeType) {
                                        registry.addWorkstations(GTRecipeREICategory.CATEGORIES.apply(category),
                                                EntryStacks.of(machine.asStack()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static GTRecipeREICategory getCategoryFor(GTRecipeCategory category) {
        return gtCategories.get(category);
    }
}