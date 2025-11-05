package com.github.blackjack200.ouranos.translators.storages;

import com.github.blackjack200.ouranos.session.OuranosProxySession;
import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.session.storage.OuranosStorage;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

@Getter
@Setter
public class ClientMovementStorage extends OuranosStorage {
    public ClientMovementStorage(OuranosSession user) {
        super(user);
    }


    private Vector3f position, rotation;

    private final Queue<PlayerAuthInputData> inputData = new ArrayDeque<>(16);
    private final Queue<ItemUseTransaction> itemTransactions = new ArrayDeque<>(16);
    private final Queue<ItemStackRequest> stackRequests = new ArrayDeque<>(16);
    private final Queue<PlayerBlockActionData> blockInteractions = new ArrayDeque<>(16);
    private final InputMode inputMode = InputMode.UNDEFINED;

    public PlayerAuthInputPacket toAuthInput(OuranosProxySession player) {
        final PlayerAuthInputPacket packet = new PlayerAuthInputPacket();
        packet.setInputMode(this.inputMode);
        packet.setPosition(this.position);
        packet.setRotation(this.rotation);
        packet.getInputData().addAll(this.inputData);
        this.inputData.clear();

        final ItemUseTransaction itemTransaction = this.itemTransactions.poll();
        if (itemTransaction != null) {
            packet.getInputData().add(PlayerAuthInputData.PERFORM_ITEM_INTERACTION);
            packet.setItemUseTransaction(itemTransaction);
        }
        final ItemStackRequest stackRequest = this.stackRequests.poll();
        if (stackRequest != null) {
            packet.getInputData().add(PlayerAuthInputData.PERFORM_ITEM_STACK_REQUEST);
            packet.setItemStackRequest(stackRequest);
        }
        if (!this.blockInteractions.isEmpty()) {
            packet.getInputData().add(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS);
            packet.getPlayerActions().addAll(this.blockInteractions);
            this.blockInteractions.clear();
        }

        // TODO: This can be done better, we want to reach high accuracy because this matter!
        packet.setDelta(Objects.requireNonNullElse(packet.getDelta(), Vector3f.ZERO));
        packet.setMotion(Objects.requireNonNullElse(packet.getMotion(), Vector2f.ZERO));
        packet.setRawMoveVector(Objects.requireNonNullElse(packet.getRawMoveVector(), Vector2f.ZERO));
        packet.setInputMode(Objects.requireNonNullElse(packet.getInputMode(), this.inputMode));
        packet.setPlayMode(Objects.requireNonNullElse(packet.getPlayMode(), ClientPlayMode.NORMAL));
        packet.setInputInteractionModel(Objects.requireNonNullElse(packet.getInputInteractionModel(), InputInteractionModel.TOUCH));
        packet.setAnalogMoveVector(Objects.requireNonNullElse(packet.getAnalogMoveVector(), Vector2f.ZERO));

        packet.setInteractRotation(Objects.requireNonNullElse(packet.getInteractRotation(), Vector2f.ZERO));
        packet.setCameraOrientation(Objects.requireNonNullElse(packet.getCameraOrientation(), Vector3f.ZERO));
        return packet;
    }
}
