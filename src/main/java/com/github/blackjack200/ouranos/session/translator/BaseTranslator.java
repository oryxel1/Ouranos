package com.github.blackjack200.ouranos.session.translator;

import com.github.blackjack200.ouranos.session.OuranosSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

public interface BaseTranslator {
    default BedrockPacket translateClientbound(OuranosSession session, BedrockPacket packet) {
        return packet;
    }
    default BedrockPacket translateServerbound(OuranosSession session, BedrockPacket packet) {
        return packet;
    }
}
