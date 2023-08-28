package mod.packetdump.mixin;

import com.mojang.authlib.GameProfile;
import mod.packetdump.ConnectionHandler;
import mod.packetdump.ConnectionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientGamePacketListenerMixin implements ConnectionListener {
    @Unique
    @Nullable
    private ConnectionHandler connectionHandler = null;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(Minecraft minecraft, Screen screen, Connection connection, ServerData serverData, GameProfile gameProfile, WorldSessionTelemetryManager worldSessionTelemetryManager, CallbackInfo ci) {
        connectionHandler = (ConnectionHandler) connection;
        connectionHandler.startPacketDump(
                minecraft.gameDirectory.toPath().resolve("dumps"),
                gameProfile.getId(),
                false
        );
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        if (connectionHandler != null) connectionHandler.stopPacketDump();
    }

    @Override
    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }
}
