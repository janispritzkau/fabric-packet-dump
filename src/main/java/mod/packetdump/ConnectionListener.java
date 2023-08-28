package mod.packetdump;

import org.jetbrains.annotations.Nullable;

public interface ConnectionListener {
    @Nullable
    ConnectionHandler getConnectionHandler();
}
