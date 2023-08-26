package mod.packetdump.mixin;

import mod.packetdump.ConnectionHandler;
import mod.packetdump.ConnectionListener;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerMixin implements ConnectionListener {
    private ConnectionHandler connectionHandler = null;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(MinecraftServer minecraftServer, Connection connection, ServerPlayer serverPlayer, CallbackInfo ci) {
        connectionHandler = (ConnectionHandler) connection;
        connectionHandler.startPacketDump(
                minecraftServer.getWorldPath(LevelResource.ROOT).resolve("dumps"),
                serverPlayer.getGameProfile().getId(),
                true
        );
    }

    @Override
    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }
}
