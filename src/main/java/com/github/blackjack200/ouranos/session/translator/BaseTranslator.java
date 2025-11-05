package com.github.blackjack200.ouranos.session.translator;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

public interface BaseTranslator {
    default BedrockPacket translateClientbound(BedrockPacket packet) {
        return packet;
    }
    default BedrockPacket translateServerbound(BedrockPacket packet) {
        return packet;
    }
}
