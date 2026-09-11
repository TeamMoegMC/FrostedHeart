/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedresearch.mixin.client;

import java.io.IOException;
import java.nio.file.Path;

import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.ResearchCatalog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.fml.loading.FMLPaths;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rejects an invalid local research catalogue before the integrated-server
 * thread is created. A failure inside {@code ServerAboutToStartEvent} occurs
 * before vanilla installs its chunk progress listener, leaving the client in
 * an otherwise unescapable loading loop.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftResearchCatalogPreflightMixin {
    @Inject(method = "doWorldLoad", at = @At("HEAD"), cancellable = true)
    private void fh$validateResearchCatalogBeforeWorldLoad(
            String levelId,
            LevelStorageSource.LevelStorageAccess levelAccess,
            PackRepository packRepository,
            WorldStem worldStem,
            boolean newWorld,
            CallbackInfo callback) {
        Path catalogDirectory = FMLPaths.CONFIGDIR.get().resolve("fhresearches");
        try {
            ResearchCatalog.load(catalogDirectory);
        } catch (ResearchCatalog.ValidationException failure) {
            FRMain.LOGGER.error("World startup rejected by invalid research catalogue:\n - {}",
                    String.join("\n - ", failure.diagnostics()));

            try {
                worldStem.close();
            } catch (RuntimeException closeFailure) {
                FRMain.LOGGER.warn("Could not close world resources after research catalogue rejection",
                        closeFailure);
            }
            try {
                levelAccess.close();
            } catch (IOException closeFailure) {
                FRMain.LOGGER.warn("Could not release world {} after research catalogue rejection",
                        levelId, closeFailure);
            }

            Minecraft minecraft = (Minecraft) (Object) this;
            minecraft.getDownloadedPackSource().clearServerPack().exceptionally(clearFailure -> {
                FRMain.LOGGER.warn("Could not clear bundled world resources after research catalogue rejection",
                        clearFailure);
                return null;
            });
            String firstDiagnostic = failure.diagnostics().isEmpty()
                    ? catalogDirectory.toString()
                    : failure.diagnostics().get(0);
            minecraft.setScreen(new AlertScreen(
                    () -> minecraft.setScreen(new TitleScreen()),
                    Component.translatable("gui.frostedresearch.catalog_error.title"),
                    Component.translatable("gui.frostedresearch.catalog_error.message",
                            failure.diagnostics().size(), firstDiagnostic),
                    CommonComponents.GUI_TO_TITLE,
                    true));
            callback.cancel();
        }
    }
}
