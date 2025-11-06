package com.github.blackjack200.ouranos.translators;

import com.github.blackjack200.ouranos.ProtocolInfo;
import com.github.blackjack200.ouranos.base.ProtocolToProtocol;
import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.translators.global_storage.EntityMetadataStorage;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.awt.*;
import java.util.*;

// These changes should be applied no matter the protocol version.
public class GlobalProtocolTranslator extends ProtocolToProtocol {
    @Override
    public void init(OuranosSession session) {
        session.put(new EntityMetadataStorage(session));
    }

    @Override
    protected void registerProtocol() {
        // These should be applied to all version to bypass some checking stuff that don't need to be translated.
        this.registerClientbound(StartGamePacket.class, wrapped -> {
            final StartGamePacket packet = (StartGamePacket) wrapped.getPacket();
            packet.setBlockRegistryChecksum(0); // Disable BDS server block registry checksum.
        });
        this.registerClientbound(ResourcePackStackPacket.class, wrapped -> {
            final ResourcePackStackPacket packet = (ResourcePackStackPacket) wrapped.getPacket();
            packet.setGameVersion("*");
        });
        this.registerServerbound(ClientCacheStatusPacket.class, wrapped -> {
            final ClientCacheStatusPacket packet = (ClientCacheStatusPacket) wrapped.getPacket();
            packet.setSupported(false);
        });

        // Delete all the new/old metadata when translating back and forth.
        this.registerClientbound(SetEntityDataPacket.class, wrapped -> {
            final EntityMetadataStorage storage = wrapped.session().get(EntityMetadataStorage.class);
            if (storage == null) {
                return;
            }

            final SetEntityDataPacket packet = (SetEntityDataPacket) wrapped.getPacket();
            cleanMetadata(storage, packet.getMetadata());
        });
        this.registerClientbound(AddEntityPacket.class, wrapped -> {
            final EntityMetadataStorage storage = wrapped.session().get(EntityMetadataStorage.class);
            if (storage == null) {
                return;
            }

            final AddEntityPacket packet = (AddEntityPacket) wrapped.getPacket();
            cleanMetadata(storage, packet.getMetadata());
        });
        this.registerClientbound(AddPlayerPacket.class, wrapped -> {
            final EntityMetadataStorage storage = wrapped.session().get(EntityMetadataStorage.class);
            if (storage == null) {
                return;
            }

            final AddPlayerPacket packet = (AddPlayerPacket) wrapped.getPacket();
            cleanMetadata(storage, packet.getMetadata());
        });
        this.registerClientbound(AddItemEntityPacket.class, wrapped -> {
            final EntityMetadataStorage storage = wrapped.session().get(EntityMetadataStorage.class);
            if (storage == null) {
                return;
            }

            final AddItemEntityPacket packet = (AddItemEntityPacket) wrapped.getPacket();
            cleanMetadata(storage, packet.getMetadata());
        });

        this.registerClientbound(PlayerListPacket.class, wrapped -> {
            final PlayerListPacket packet = (PlayerListPacket) wrapped.getPacket();
            for (final PlayerListPacket.Entry entry : packet.getEntries()) {
                entry.setColor(Objects.requireNonNullElse(entry.getColor(), Color.WHITE));
                if (entry.getSkin() != null) {
                    entry.setSkin(entry.getSkin().toBuilder().geometryDataEngineVersion(ProtocolInfo.getPacketCodec(wrapped.getOutput()).getMinecraftVersion()).build());
                }
            }
        });
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
