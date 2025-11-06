package com.github.blackjack200.ouranos.protocol;

import com.github.blackjack200.ouranos.base.ProtocolToProtocol;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

// These changes should be applied no matter the protocol version.
public class GlobalProtocolTranslator extends ProtocolToProtocol {
    @Override
    protected void registerProtocol() {
        this.registerClientbound(StartGamePacket.class, wrapped -> {
            final StartGamePacket packet = (StartGamePacket) wrapped.getPacket();
            packet.setBlockRegistryChecksum(0); // Disable BDS server block registry checksum.
        });
        this.registerClientbound(ResourcePackStackPacket.class, wrapped -> {

        });
    }
}
