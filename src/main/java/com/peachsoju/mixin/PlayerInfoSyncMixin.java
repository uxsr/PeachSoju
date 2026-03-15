package com.peachsoju.mixin;

import com.peachsoju.sync.PeachSojuSync;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Mixin to replace other players' display names with their PeachSoju synced nicks.
 * This affects tab list and nametags.
 */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoSyncMixin {

    @Shadow
    public abstract UUID getUUID();

    @Shadow
    public abstract String getUsername();

    /**
     * Intercept getTabListDisplayName to show synced nicks in tab list
     */
    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void peachsoju$injectSyncedTabName(CallbackInfoReturnable<Component> cir) {
        UUID uuid = this.getUUID();
        if (uuid == null) return;
        
        String syncedNick = PeachSojuSync.INSTANCE.buildNickForPlayer(uuid);
        if (syncedNick != null) {
            cir.setReturnValue(Component.literal(syncedNick));
        }
    }
}
