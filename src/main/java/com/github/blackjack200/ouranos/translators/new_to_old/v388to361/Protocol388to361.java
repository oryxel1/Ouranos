package com.github.blackjack200.ouranos.translators.new_to_old.v388to361;

import com.github.blackjack200.ouranos.base.ProtocolToProtocol;
import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.translators.new_to_old.v388to361.storage.ClientAuthMovementStorage;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;

public class Protocol388to361 extends ProtocolToProtocol {
    @Override
    public void init(OuranosSession session) {
        session.put(new ClientAuthMovementStorage(session));
    }

    @Override
    protected void registerProtocol() {
        this.registerServerbound(PlayerActionPacket.class, wrapped -> {
            if (wrapped.session().getAuthoritativeMovementMode() == AuthoritativeMovementMode.CLIENT) {
                return;
            }

            final ClientAuthMovementStorage storage = wrapped.session().get(ClientAuthMovementStorage.class);

            final PlayerActionPacket packet = (PlayerActionPacket) wrapped.getPacket();
            final PlayerActionType actionType = packet.getAction();
            switch (actionType) {
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

                case START_BREAK, STOP_BREAK, ABORT_BREAK -> {
                    storage.setBreaking(actionType == PlayerActionType.START_BREAK);

                    final PlayerBlockActionData data = new PlayerBlockActionData();
                    data.setAction(switch (actionType) {
                        case START_BREAK -> PlayerActionType.START_BREAK;
                        case STOP_BREAK -> PlayerActionType.STOP_SLEEP;
                        case ABORT_BREAK -> PlayerActionType.ABORT_BREAK;
                        default -> null;
                    });
                    data.setBlockPosition(packet.getBlockPosition());
                    data.setFace(packet.getFace());
                    storage.getBlockInteractions().add(data);

                    wrapped.cancel();
                }
            }
        });

        this.registerServerbound(MovePlayerPacket.class, wrapped -> {
            if (wrapped.session().getAuthoritativeMovementMode() == AuthoritativeMovementMode.CLIENT) {
                return;
            }

            final ClientAuthMovementStorage storage = wrapped.session().get(ClientAuthMovementStorage.class);

            final MovePlayerPacket packet = (MovePlayerPacket) wrapped.getPacket();
            storage.setPosition(packet.getPosition());
            storage.setRotation(packet.getRotation());

            wrapped.setPacket(storage.toAuthInput());
        });

        this.registerServerbound(LevelSoundEventPacket.class, wrapped -> {
            if (wrapped.session().getAuthoritativeMovementMode() == AuthoritativeMovementMode.CLIENT) {
                return;
            }

            final LevelSoundEventPacket packet = (LevelSoundEventPacket) wrapped.getPacket();

            if (packet.getSound() == SoundEvent.ATTACK_NODAMAGE) {
                final ClientAuthMovementStorage storage = wrapped.session().get(ClientAuthMovementStorage.class);
                storage.getInputData().add(PlayerAuthInputData.MISSED_SWING);
            }
        });
    }
}
