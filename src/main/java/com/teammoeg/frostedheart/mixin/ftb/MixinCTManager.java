package com.teammoeg.frostedheart.mixin.ftb;

import dev.ftb.mods.ftbteams.FTBTeams;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
@Mixin(value = ClientTeamManagerImpl.class, remap = false)
public abstract class MixinCTManager {
    @Shadow private ClientTeam selfTeam;
    @Shadow private KnownClientPlayer selfKnownPlayer;
    @Shadow @Final
    private Map<UUID, ClientTeam> teamMap;
    @Shadow @Final
    private Map<UUID, KnownClientPlayer> knownPlayers;

    @Inject(method = "initSelfDetails", at = @At("HEAD"), cancellable = true, remap = false)
    private void onInitSelfDetails(UUID selfTeamID, CallbackInfo ci) {
        this.selfTeam = this.teamMap.get(selfTeamID);
        UUID userId = Minecraft.getInstance().getUser().getGameProfile().getId();
        this.selfKnownPlayer = this.knownPlayers.get(userId);

        if (this.selfKnownPlayer == null) {
            String username = Minecraft.getInstance().getUser().getGameProfile().getName();
            UUID offlineId = UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
            );
            this.selfKnownPlayer = this.knownPlayers.get(offlineId);
        }
        if (this.selfKnownPlayer == null) {
            FTBTeams.LOGGER.error(
                    "Local player id {} was not found in the known players list [{}]!",
                    userId,
                    String.join(",", this.knownPlayers.keySet().stream()
                            .map(UUID::toString).toList())
            );
        }
        ci.cancel();
    }
}
