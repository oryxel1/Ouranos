package com.github.blackjack200.ouranos.session.translator.impl;

import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import com.github.blackjack200.ouranos.translators.ClientMovementTranslator;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.codec.v388.Bedrock_v388;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.data.ClientPlayMode;
import org.cloudburstmc.protocol.bedrock.data.InputInteractionModel;
import org.cloudburstmc.protocol.bedrock.data.InputMode;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

import java.util.Objects;

@SuppressWarnings("ALL")
public class TranslatorsAdder implements BaseTranslator {
    @Override
    public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
        if (bedrockPacket instanceof StartGamePacket packet) {
            if (packet.getAuthoritativeMovementMode() != AuthoritativeMovementMode.CLIENT && session.getProtocolId() < Bedrock_v388.CODEC.getProtocolVersion()) {
                session.put(new ClientMovementTranslator());
            }

            session.put(new BaseTranslator() {
                @Override
                public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket bedrockPacket) {
                    if (bedrockPacket instanceof PlayerAuthInputPacket packet) {
                        packet.setDelta(Objects.requireNonNullElse(packet.getDelta(), Vector3f.ZERO));
                        packet.setMotion(Objects.requireNonNullElse(packet.getMotion(), Vector2f.ZERO));
                        packet.setRawMoveVector(Objects.requireNonNullElse(packet.getRawMoveVector(), Vector2f.ZERO));
                        packet.setInputMode(Objects.requireNonNullElse(packet.getInputMode(), InputMode.UNDEFINED));
                        packet.setPlayMode(Objects.requireNonNullElse(packet.getPlayMode(), ClientPlayMode.NORMAL));
                        packet.setInputInteractionModel(Objects.requireNonNullElse(packet.getInputInteractionModel(), InputInteractionModel.TOUCH));
                        packet.setAnalogMoveVector(Objects.requireNonNullElse(packet.getAnalogMoveVector(), Vector2f.ZERO));

                        packet.setInteractRotation(Objects.requireNonNullElse(packet.getInteractRotation(), Vector2f.ZERO));
                        packet.setCameraOrientation(Objects.requireNonNullElse(packet.getCameraOrientation(), Vector3f.ZERO));
                    }
                    return bedrockPacket;
                }
            });
        }

        return bedrockPacket;
    }
}
