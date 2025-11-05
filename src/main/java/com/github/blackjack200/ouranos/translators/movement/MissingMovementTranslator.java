package com.github.blackjack200.ouranos.translators.movement;

import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ClientPlayMode;
import org.cloudburstmc.protocol.bedrock.data.InputInteractionModel;
import org.cloudburstmc.protocol.bedrock.data.InputMode;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

import java.util.Objects;

public class MissingMovementTranslator implements BaseTranslator {
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

            final ItemUseTransaction transaction = packet.getItemUseTransaction();
            if (transaction != null) {
                transaction.setTriggerType(ItemUseTransaction.TriggerType.PLAYER_INPUT);
                transaction.setClientInteractPrediction(ItemUseTransaction.PredictedResult.SUCCESS);
            }
        }
        return bedrockPacket;
    }
}
