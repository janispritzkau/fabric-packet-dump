package mod.packetdump;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import java.nio.file.Path;
import java.util.UUID;

public interface ConnectionHandler {
    void startPacketDump(Path path, UUID profileId, boolean isServer);
    void stopPacketDump();
    void dumpPacket(Packet<?> packet, boolean isReceiving);
}
