package com.github.blackjack200.ouranos.session.translator.impl;

import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import com.github.blackjack200.ouranos.translators.movement.ClientMovementTranslator;
import com.github.blackjack200.ouranos.translators.movement.MissingMovementTranslator;
import org.cloudburstmc.protocol.bedrock.codec.v388.Bedrock_v388;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

@SuppressWarnings("ALL")
public class TranslatorsAdder implements BaseTranslator {
    @Override
    public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
        if (bedrockPacket instanceof StartGamePacket packet) {
            if (packet.getAuthoritativeMovementMode() != AuthoritativeMovementMode.CLIENT && session.getProtocolId() < Bedrock_v388.CODEC.getProtocolVersion()) {
                session.put(new ClientMovementTranslator());
            }

            session.put(new MissingMovementTranslator());
        }

        return bedrockPacket;
    }
}
