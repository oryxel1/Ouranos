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
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.*;

public abstract class OuranosSession {
    @Getter @Setter
    private long uniqueId, runtimeId;
    @Getter
    private final int protocolId, targetVersion;
    public int prevFormId;

    private final Map<Class<?>, OuranosStorage> storages = new HashMap<>();

    public void put(OuranosStorage storage) {
        this.storages.put(storage.getClass(), storage);
    }
    @SuppressWarnings("unchecked")
    public final <T extends OuranosStorage> T get(Class<T> klass) {
        return (T) this.storages.get(klass);
    }

    private final List<ProtocolToProtocol> translators = new ArrayList<>();

    public OuranosSession(int protocolId, int targetVersion) {
        this.protocolId = protocolId;
        this.targetVersion = targetVersion;

        this.translators.add(new GlobalProtocolTranslator());
        this.translators.add(new GlobalWorldTranslator());
        this.translators.add(new GlobalItemTranslator());

        this.translators.addAll(ProtocolInfo.getTranslators(targetVersion, protocolId));

        this.translators.forEach(translator -> translator.init(this));

//        this.translators.add(new TranslatorsAdder());
//        this.translators.add(new BaseTranslator() {
//            @Override
//            public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
//                if (bedrockPacket instanceof StartGamePacket packet) {
//                    packet.setBlockRegistryChecksum(0);
//                    packet.setServerId(Optional.ofNullable(packet.getServerId()).orElse(""));
//                    packet.setWorldId(Optional.ofNullable(packet.getWorldId()).orElse(""));
//                    packet.setScenarioId(Optional.ofNullable(packet.getScenarioId()).orElse(""));
//                    packet.setChatRestrictionLevel(Optional.ofNullable(packet.getChatRestrictionLevel()).orElse(ChatRestrictionLevel.NONE));
//                    packet.setPlayerPropertyData(Optional.ofNullable(packet.getPlayerPropertyData()).orElse(NbtMap.EMPTY));
//                    packet.setWorldTemplateId(Optional.ofNullable(packet.getWorldTemplateId()).orElse(UUID.randomUUID()));
//                    packet.setOwnerId(Objects.requireNonNullElse(packet.getOwnerId(), ""));
//                    packet.setAuthoritativeMovementMode(Objects.requireNonNullElse(packet.getAuthoritativeMovementMode(), AuthoritativeMovementMode.SERVER_WITH_REWIND));
//                } else if (bedrockPacket instanceof AddPlayerPacket packet) {
//                    packet.setGameType(Optional.ofNullable(packet.getGameType()).orElse(GameType.DEFAULT));
//                } else if (bedrockPacket instanceof ResourcePacksInfoPacket packet) {
//                    packet.setWorldTemplateId(Objects.requireNonNullElseGet(packet.getWorldTemplateId(), UUID::randomUUID));
//                    packet.setWorldTemplateVersion(Objects.requireNonNullElse(packet.getWorldTemplateVersion(), "0.0.0"));
//                } else if (bedrockPacket instanceof ResourcePackStackPacket packet) {
//                    packet.setGameVersion("*");
//                }
//
//                return bedrockPacket;
//            }
//
//            @Override
//            public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket bedrockPacket) {
//                if (bedrockPacket instanceof ClientCacheStatusPacket packet) {
//                    packet.setSupported(false);
//                }
//
//                return bedrockPacket;
//            }
//        });
//
//        // Well this is sure is an odd bug, I don't know, haven't test, so I will trust the original owner of this code.
//        if (this.targetVersion >= Bedrock_v389.CODEC.getProtocolVersion() && this.targetVersion <= Bedrock_v390.CODEC.getProtocolVersion()) {
//            this.translators.add(new BaseTranslator() {
//                @Override
//                public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
//                    if (bedrockPacket instanceof MoveEntityAbsolutePacket packet) {
//                        final MovePlayerPacket movePacket = new MovePlayerPacket();
//                        movePacket.setRuntimeEntityId(packet.getRuntimeEntityId());
//                        movePacket.setPosition(packet.getPosition());
//                        movePacket.setRotation(packet.getRotation());
//                        movePacket.setOnGround(packet.isOnGround());
//                        if (packet.isTeleported()) {
//                            movePacket.setMode(MovePlayerPacket.Mode.TELEPORT);
//                        } else {
//                            movePacket.setMode(MovePlayerPacket.Mode.NORMAL);
//                        }
//                        return movePacket;
//                    }
//
//                    return bedrockPacket;
//                }
//            });
//        }
//
//        this.translators.add(new ProtocolRewriterTranslator());
//        this.translators.add(new AdventureSettingsTranslator());
//        this.translators.add(new ItemRewriterTranslator());
//        this.translators.add(new WorldTranslator());
//
//        if (this.protocolId < this.targetVersion) {
//            this.translators.add(new EntityMetadataTranslator(this));
//        }
    }

    public abstract void sendUpstreamPacket(BedrockPacket packet);
    public abstract void sendDownstreamPacket(BedrockPacket packet);

    public final BedrockPacket translateClientbound(BedrockPacket packet) {
        if (packet instanceof StartGamePacket startGamePacket) {
            this.uniqueId = startGamePacket.getUniqueEntityId();
            this.runtimeId = startGamePacket.getRuntimeEntityId();
        }

        final WrappedBedrockPacket wrapped = new WrappedBedrockPacket(this, this.getTargetVersion(), this.getProtocolId(),  packet, false);
        for (ProtocolToProtocol translator : this.translators) {
            if (!translator.passthroughClientbound(wrapped)) {
                return null;
            }
        }
        return wrapped.isCancelled() ? null : wrapped.getPacket();
    }

    public final BedrockPacket translateServerbound(BedrockPacket packet) {
        final WrappedBedrockPacket wrapped = new WrappedBedrockPacket(this, this.getProtocolId(), this.getTargetVersion(), packet, false);
        for (ProtocolToProtocol translator : this.translators) {
            if (!translator.passthroughServerbound(wrapped)) {
                return null;
            }
        }
        return wrapped.isCancelled() ? null : wrapped.getPacket();
    }
}
