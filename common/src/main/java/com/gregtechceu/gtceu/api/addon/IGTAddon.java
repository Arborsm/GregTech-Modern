package com.gregtechceu.gtceu.api.addon;

public interface IGTAddon {

    void initializeAddon();

    default void registerElements() {

    }

    default void registerMaterials() {

    }

    default void registerSounds() {

    }

    default void registerCovers() {

    }

    default void registerRecipeTypes() {

    }

    default void registerMachines() {

    }
}