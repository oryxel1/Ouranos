package com.github.blackjack200.ouranos.session;

import com.github.blackjack200.ouranos.ProtocolInfo;
import com.github.blackjack200.ouranos.base.ProtocolToProtocol;
import com.github.blackjack200.ouranos.base.WrappedBedrockPacket;
import com.github.blackjack200.ouranos.translators.GlobalItemTranslator;
import com.github.blackjack200.ouranos.translators.GlobalProtocolTranslator;
import com.github.blackjack200.ouranos.translators.GlobalWorldTranslator;
import com.github.blackjack200.ouranos.session.storage.OuranosStorage;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.*;

public abstract class OuranosSession {
    @Getter @Setter
    private long uniqueId, runtimeId;
    @Getter
    private final int protocolId, targetVersion;

    @Getter
    private boolean serverAuthoritativeInventories, serverAuthoritativeBlockBreaking;
    @Getter
    private AuthoritativeMovementMode authoritativeMovementMode;

    private final Map<Class<?>, OuranosStorage> storages = new HashMap<>();

    public void put(OuranosStorage storage) {
        this.storages.put(storage.getClass(), storage);
    }
    @SuppressWarnings("unchecked")
    public final <T extends OuranosStorage> T get(Class<T> klass) {
        return (T) this.storages.get(klass);
    }

    private final List<ProtocolToProtocol> translators = new ArrayList<>();
    private final List<ProtocolToProtocol> translatorsReversed;

    public OuranosSession(int protocolId, int targetVersion) {
        this.protocolId = protocolId;
        this.targetVersion = targetVersion;

        this.translators.add(new GlobalProtocolTranslator());
        this.translators.add(new GlobalWorldTranslator());
        this.translators.add(new GlobalItemTranslator());

        this.translators.addAll(ProtocolInfo.getTranslators(targetVersion, protocolId));
        this.translators.forEach(translator -> translator.init(this));

        this.translatorsReversed = new ArrayList<>(this.translators);
        this.translatorsReversed.reversed();
        System.out.println(Arrays.toString(this.translatorsReversed.toArray()));
    }

    public abstract void sendUpstreamPacket(BedrockPacket packet);
    public abstract void sendDownstreamPacket(BedrockPacket packet);

    public final BedrockPacket translateClientbound(BedrockPacket packet) {
        if (packet instanceof StartGamePacket startGamePacket) {
            this.uniqueId = startGamePacket.getUniqueEntityId();
            this.runtimeId = startGamePacket.getRuntimeEntityId();
            this.serverAuthoritativeInventories = startGamePacket.isInventoriesServerAuthoritative();
            this.authoritativeMovementMode = startGamePacket.getAuthoritativeMovementMode();
            this.serverAuthoritativeInventories = startGamePacket.isServerAuthoritativeBlockBreaking();
        }

        final WrappedBedrockPacket wrapped = new WrappedBedrockPacket(this, this.getTargetVersion(), this.getProtocolId(), packet, false);
        for (ProtocolToProtocol translator : this.translators) {
            translator.passthroughClientbound(wrapped);
        }
        return wrapped.isCancelled() ? null : wrapped.getPacket();
    }

    public final BedrockPacket translateServerbound(BedrockPacket packet) {
        final WrappedBedrockPacket wrapped = new WrappedBedrockPacket(this, this.getProtocolId(), this.getTargetVersion(), packet, false);
        if (packet instanceof MovePlayerPacket) {
            System.out.println("Start----");
        }
        for (ProtocolToProtocol translator : this.translatorsReversed) {
            translator.passthroughServerbound(wrapped);
            if (packet instanceof MovePlayerPacket || packet instanceof PlayerAuthInputPacket) {
                System.out.println("Passthrough " + translator.getClass() + " : " + wrapped.getPacket());
            }
        }
        return wrapped.isCancelled() ? null : wrapped.getPacket();
    }
}
