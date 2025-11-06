package com.github.blackjack200.ouranos.translators;

import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.translators.storages.EntityMetadataStorage;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.Set;

public class EntityMetadataTranslator implements BaseTranslator {
    public EntityMetadataTranslator(OuranosSession user) {
        user.put(new EntityMetadataStorage(user));
    }

    @Override
    public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final EntityMetadataStorage storage = session.get(EntityMetadataStorage.class);
        if (storage == null) {
            return bedrockPacket;
        }

        if (bedrockPacket instanceof SetEntityDataPacket packet) {
            cleanMetadata(storage, packet.getMetadata());
        } else if (bedrockPacket instanceof AddEntityPacket packet) {
            cleanMetadata(storage, packet.getMetadata());
        } else if (bedrockPacket instanceof AddPlayerPacket packet) {
            cleanMetadata(storage, packet.getMetadata());
        } else if (bedrockPacket instanceof AddItemEntityPacket packet) {
            cleanMetadata(storage, packet.getMetadata());
        }
        return bedrockPacket;
    }

    private void cleanMetadata(final EntityMetadataStorage storage, EntityDataMap metadata) {
        if (metadata == null) {
            return;
        }

        for (EntityDataType<?> type : metadata.keySet()) {
            if (storage.getDataTypeMap().fromType(type) == null) {
                metadata.remove(type);
            }
        }

        final Set<EntityFlag> flags = metadata.getFlags();
        if (flags != null) {
            flags.removeIf(flag -> storage.getFlags().getIdUnsafe(flag) == -1 && storage.getFlags_2().getIdUnsafe(flag) == -1);
        }
    }
}
