package com.github.blackjack200.ouranos.session;

import com.github.blackjack200.ouranos.session.storage.OuranosStorage;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import com.github.blackjack200.ouranos.session.translator.impl.TranslatorsAdder;
import lombok.Getter;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.codec.v389.Bedrock_v389;
import org.cloudburstmc.protocol.bedrock.codec.v390.Bedrock_v390;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.data.ChatRestrictionLevel;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.*;

public class OuranosSession {
    @Getter
    private long uniqueId, runtimeId;
    @Getter
    private final int protocolId, targetVersion;

    private final Map<Class<?>, OuranosStorage> storages = new HashMap<>();

    public void put(OuranosStorage storage) {
        this.storages.put(storage.getClass(), storage);
    }
    @SuppressWarnings("unchecked")
    public final <T extends OuranosStorage> T get(Class<T> klass) {
        return (T) this.storages.get(klass);
    }

    private final List<BaseTranslator> translators = new ArrayList<>();
    public void put(BaseTranslator translator) {
        this.translators.add(translator);
    }
    private void ignoreClientboundPacket(Class<? extends BedrockPacket> klass) {
        this.translators.add(new BaseTranslator() {
            @Override
            public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket packet) {
                if (packet.getClass() != klass) {
                    return packet;
                }

                return null;
            }
        });
    }
    private void ignoreServerboundPacket(Class<? extends BedrockPacket> klass) {
        this.translators.add(new BaseTranslator() {
            @Override
            public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket packet) {
                if (packet.getClass() != klass) {
                    return packet;
                }

                return null;
            }
        });
    }
    private void ignorePacket(Class<? extends BedrockPacket> klass) {
        this.translators.add(new BaseTranslator() {
            @Override
            public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket packet) {
                if (packet.getClass() != klass) {
                    return packet;
                }

                return null;
            }
        });
        this.translators.add(new BaseTranslator() {
            @Override
            public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket packet) {
                if (packet.getClass() != klass) {
                    return packet;
                }

                return null;
            }
        });
    }

    public OuranosSession(int protocolId, int targetVersion) {
        this.protocolId = protocolId;
        this.targetVersion = targetVersion;

        this.translators.add(new TranslatorsAdder());
        this.translators.add(new BaseTranslator() {
            @Override
            public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket pk) {
                if (pk instanceof StartGamePacket packet) {
                    packet.setBlockRegistryChecksum(0);
                    packet.setServerId(Optional.ofNullable(packet.getServerId()).orElse(""));
                    packet.setWorldId(Optional.ofNullable(packet.getWorldId()).orElse(""));
                    packet.setScenarioId(Optional.ofNullable(packet.getScenarioId()).orElse(""));
                    packet.setChatRestrictionLevel(Optional.ofNullable(packet.getChatRestrictionLevel()).orElse(ChatRestrictionLevel.NONE));
                    packet.setPlayerPropertyData(Optional.ofNullable(packet.getPlayerPropertyData()).orElse(NbtMap.EMPTY));
                    packet.setWorldTemplateId(Optional.ofNullable(packet.getWorldTemplateId()).orElse(UUID.randomUUID()));
                    packet.setOwnerId(Objects.requireNonNullElse(packet.getOwnerId(), ""));
                    packet.setAuthoritativeMovementMode(Objects.requireNonNullElse(packet.getAuthoritativeMovementMode(), AuthoritativeMovementMode.SERVER_WITH_REWIND));
                } else if (pk instanceof AddPlayerPacket packet) {
                    packet.setGameType(Optional.ofNullable(packet.getGameType()).orElse(GameType.DEFAULT));
                } else if (pk instanceof ResourcePacksInfoPacket packet) {
                    packet.setWorldTemplateId(Objects.requireNonNullElseGet(packet.getWorldTemplateId(), UUID::randomUUID));
                    packet.setWorldTemplateVersion(Objects.requireNonNullElse(packet.getWorldTemplateVersion(), "0.0.0"));
                }

                return pk;
            }
        });

        // Well this is sure is an odd bug, I don't know, haven't test, so I will trust the original owner of this code.
        if (this.targetVersion >= Bedrock_v389.CODEC.getProtocolVersion() && this.targetVersion <= Bedrock_v390.CODEC.getProtocolVersion()) {
            this.translators.add(new BaseTranslator() {
                @Override
                public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
                    if (bedrockPacket instanceof MoveEntityAbsolutePacket packet) {
                        final MovePlayerPacket movePacket = new MovePlayerPacket();
                        movePacket.setRuntimeEntityId(packet.getRuntimeEntityId());
                        movePacket.setPosition(packet.getPosition());
                        movePacket.setRotation(packet.getRotation());
                        movePacket.setOnGround(packet.isOnGround());
                        if (packet.isTeleported()) {
                            movePacket.setMode(MovePlayerPacket.Mode.TELEPORT);
                        } else {
                            movePacket.setMode(MovePlayerPacket.Mode.NORMAL);
                        }
                        return movePacket;
                    }

                    return bedrockPacket;
                }
            });
        }
    }

    public BedrockPacket translateServerbound(BedrockPacket packet) {
        for (BaseTranslator translator : this.translators) {
            packet = translator.translateServerbound(this, packet);
            if (packet == null) {
                break;
            }
        }

        return packet;
    }

    public BedrockPacket translateClientbound(BedrockPacket packet) {
        for (BaseTranslator translator : this.translators) {
            packet = translator.translateServerbound(this, packet);
            if (packet == null) {
                break;
            }
        }

        return packet;
    }
}
