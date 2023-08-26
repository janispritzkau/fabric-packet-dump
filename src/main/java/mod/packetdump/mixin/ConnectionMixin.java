package mod.packetdump.mixin;

import com.github.luben.zstd.ZstdOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import mod.packetdump.ConnectionHandler;
import mod.packetdump.ConnectionListener;
import net.minecraft.SharedConstants;
import net.minecraft.network.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Mixin(Connection.class)
public class ConnectionMixin implements ConnectionHandler {
    @Unique
    private OutputStream stream = null;
    @Unique
    private boolean isServer = false;
    @Unique
    private Instant startTime = null;
    @Unique
    private Instant lastFlush = null;

    @Override
    public void startPacketDump(Path path, UUID profileId, boolean isServer) {
        File file = path.resolve(
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss_")
                        .withZone(ZoneOffset.UTC)
                        .format(Instant.now())
                        + profileId.toString()
                        + ".dump.zst"
        ).toFile();

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();

            stream = new ZstdOutputStream(new BufferedOutputStream(new FileOutputStream(file), 1024 * 1024));
            startTime = Instant.now();

            FriendlyByteBuf headerBuf = new FriendlyByteBuf(Unpooled.buffer());
            headerBuf.writeInt(SharedConstants.getCurrentVersion().getProtocolVersion());
            headerBuf.writeLong(startTime.toEpochMilli());

            stream.write(headerBuf.array());
            stream.flush();

            this.isServer = isServer;
            lastFlush = Instant.now();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stopPacketDump() {
        if (stream == null) return;
        try {
            stream.flush();
            stream.close();
            stream = null;
            lastFlush = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dumpPacket(Packet<?> packet, boolean isReceiving) {
        if (stream == null) return;

        PacketFlow flow = isServer
                ? (isReceiving ? PacketFlow.SERVERBOUND : PacketFlow.CLIENTBOUND)
                : (isReceiving ? PacketFlow.CLIENTBOUND : PacketFlow.SERVERBOUND);

        ConnectionProtocol protocol = ConnectionProtocol.getProtocolForPacket(packet);

        FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
        packetBuf.writeLong(Duration.between(startTime, Instant.now()).toMillis());
        packetBuf.writeBoolean(flow == PacketFlow.CLIENTBOUND);
        packetBuf.writeInt(protocol.getPacketId(flow, packet));
        packet.write(packetBuf);

        FriendlyByteBuf headerBuf = new FriendlyByteBuf(Unpooled.buffer());
        headerBuf.writeInt(packetBuf.readableBytes());

        try {
            stream.write(headerBuf.array());
            stream.write(packetBuf.array());
            if (lastFlush.plusSeconds(5).isBefore(Instant.now())) {
                stream.flush();
                lastFlush = Instant.now();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method = "doSendPacket", at = @At("TAIL"))
    private void onSendPacket(Packet<?> packet, PacketSendListener packetSendListener, ConnectionProtocol connectionProtocol, ConnectionProtocol connectionProtocol2, CallbackInfo ci) {
        dumpPacket(packet, false);
    }

    @Inject(method = "genericsFtw", at = @At("HEAD"))
    private static void onReceivePacket(Packet<?> packet, PacketListener packetListener, CallbackInfo ci) {
        if (packetListener instanceof ConnectionListener connectionListener) {
            connectionListener.getConnectionHandler().dumpPacket(packet, true);
        }
    }

    @Inject(method = "handleDisconnection", at = @At("TAIL"))
    private void onDisconnect(CallbackInfo ci) {
        stopPacketDump();
    }
}
