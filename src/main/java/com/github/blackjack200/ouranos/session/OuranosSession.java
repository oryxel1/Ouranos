package com.github.blackjack200.ouranos.session;

import com.github.blackjack200.ouranos.session.storage.OuranosStorage;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import lombok.Getter;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.data.ChatRestrictionLevel;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

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
    private void ignoreClientboundPacket(Class<? extends BedrockPacket> klass) {
        this.translators.add(new BaseTranslator() {
            @Override
            public BedrockPacket translateClientbound(BedrockPacket packet) {
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
            public BedrockPacket translateServerbound(BedrockPacket packet) {
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
            public BedrockPacket translateClientbound(BedrockPacket packet) {
                if (packet.getClass() != klass) {
                    return packet;
                }

                return null;
            }
        });
        this.translators.add(new BaseTranslator() {
            @Override
            public BedrockPacket translateServerbound(BedrockPacket packet) {
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

        this.translators.add(new BaseTranslator() {
            @Override
            public BedrockPacket translateClientbound(BedrockPacket pk) {
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
    }

    public BedrockPacket translateServerbound(BedrockPacket packet) {
        for (BaseTranslator translator : this.translators) {
            packet = translator.translateServerbound(packet);
            if (packet == null) {
                break;
            }
        }

        return packet;
    }

    public BedrockPacket translateClientbound(BedrockPacket packet) {
        for (BaseTranslator translator : this.translators) {
            packet = translator.translateServerbound(packet);
            if (packet == null) {
                break;
            }
        }

        return packet;
    }
}
