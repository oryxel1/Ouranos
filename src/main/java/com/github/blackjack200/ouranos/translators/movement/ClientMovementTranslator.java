package com.github.blackjack200.ouranos.translators.movement;

import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import com.github.blackjack200.ouranos.translators.storages.ClientMovementStorage;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;

public class ClientMovementTranslator implements BaseTranslator {
    @Override
    public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket bedrockPacket) {
        if (bedrockPacket instanceof PlayerActionPacket packet) {
            final ClientMovementStorage storage = session.get(ClientMovementStorage.class);
            
            boolean cancel = false;
            switch (packet.getAction()) {
                case START_SPRINT -> storage.getInputData().add(PlayerAuthInputData.START_SPRINTING);
                case STOP_SPRINT -> storage.getInputData().add(PlayerAuthInputData.STOP_SPRINTING);
                case START_SNEAK -> storage.getInputData().add(PlayerAuthInputData.START_SNEAKING);
                case STOP_SNEAK -> storage.getInputData().add(PlayerAuthInputData.STOP_SNEAKING);
                case START_SWIMMING -> storage.getInputData().add(PlayerAuthInputData.START_SWIMMING);
                case STOP_SWIMMING -> storage.getInputData().add(PlayerAuthInputData.STOP_SWIMMING);
                case START_GLIDE -> storage.getInputData().add(PlayerAuthInputData.START_GLIDING);
                case STOP_GLIDE -> storage.getInputData().add(PlayerAuthInputData.STOP_GLIDING);
                case START_CRAWLING -> storage.getInputData().add(PlayerAuthInputData.START_CRAWLING);
                case STOP_CRAWLING -> storage.getInputData().add(PlayerAuthInputData.STOP_CRAWLING);
                case START_FLYING -> storage.getInputData().add(PlayerAuthInputData.START_FLYING);
                case STOP_FLYING -> storage.getInputData().add(PlayerAuthInputData.STOP_FLYING);
                case JUMP -> storage.getInputData().add(PlayerAuthInputData.START_JUMPING);
                default -> cancel = true;
            }
            if (cancel) {
                return null;
            }
        } else if (bedrockPacket instanceof MovePlayerPacket packet) {
            final ClientMovementStorage storage = session.get(ClientMovementStorage.class);

            storage.setPosition(packet.getPosition());
            storage.setRotation(packet.getRotation());

            if (packet.isOnGround() && session.getTargetVersion() >= Bedrock_v729.CODEC.getProtocolVersion()) {
                storage.getInputData().add(PlayerAuthInputData.VERTICAL_COLLISION);
            }

            return storage.toAuthInput();
        } else if (bedrockPacket instanceof LevelSoundEventPacket packet && packet.getSound().equals(SoundEvent.ATTACK_NODAMAGE)) {
            final ClientMovementStorage storage = session.get(ClientMovementStorage.class);
            storage.getInputData().add(PlayerAuthInputData.MISSED_SWING);
            return null;
        }

        return bedrockPacket;
    }
}
