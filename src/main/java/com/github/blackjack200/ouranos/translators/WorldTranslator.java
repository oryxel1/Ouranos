package com.github.blackjack200.ouranos.translators;

import com.github.blackjack200.ouranos.converter.ChunkRewriteException;
import com.github.blackjack200.ouranos.converter.TypeConverter;
import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import com.github.blackjack200.ouranos.utils.SimpleBlockDefinition;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.cloudburstmc.protocol.bedrock.codec.v475.Bedrock_v475;
import org.cloudburstmc.protocol.bedrock.codec.v560.Bedrock_v560;
import org.cloudburstmc.protocol.bedrock.data.BlockChangeEntry;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.ArrayList;

public class WorldTranslator implements BaseTranslator {
    @Override
    public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final int input = session.getProtocolId(), output = session.getTargetVersion();

        if (bedrockPacket instanceof UpdateBlockPacket packet) {
            var runtimeId = packet.getDefinition().getRuntimeId();
            var translated = TypeConverter.translateBlockRuntimeId(input, output, runtimeId);
            packet.setDefinition(new SimpleBlockDefinition(translated));
        } else if (bedrockPacket instanceof LevelEventPacket packet) {
            var type = packet.getType();
            var data = packet.getData();

            if (type == ParticleType.ICON_CRACK) {
                var newItem = TypeConverter.translateItemRuntimeId(input, output, data >> 16, data & 0xFFFF);
                data = newItem[0] << 16 | newItem[1];
            } else if (type == LevelEvent.PARTICLE_DESTROY_BLOCK) {
                data = TypeConverter.translateBlockRuntimeId(input, output, data);
            } else if (type == LevelEvent.PARTICLE_CRACK_BLOCK) {
                var face = data >> 24;
                var runtimeId = data & ~(face << 24);
                data = TypeConverter.translateBlockRuntimeId(input, output, runtimeId) | face << 24;
            }
            packet.setData(data);
        } else if (bedrockPacket instanceof LevelSoundEventPacket pk) {
            var sound = pk.getSound();
            var runtimeId = pk.getExtraData();
            switch (sound) {
                case DOOR_OPEN:
                case DOOR_CLOSE:
                case TRAPDOOR_OPEN:
                case TRAPDOOR_CLOSE:
                case FENCE_GATE_OPEN:
                case FENCE_GATE_CLOSE:
                    pk.setExtraData(TypeConverter.translateBlockRuntimeId(input, output, runtimeId));
                    if (output < Bedrock_v560.CODEC.getProtocolVersion()) {
                        var newPk = new LevelEventPacket();
                        newPk.setType(LevelEvent.SOUND_DOOR_OPEN);
                        newPk.setPosition(pk.getPosition());
                        return newPk;
                    }
                    break;
                case PLACE:
                case BREAK_BLOCK:
                case ITEM_USE_ON:
                    pk.setExtraData(TypeConverter.translateBlockRuntimeId(input, output, runtimeId));
            }
        } else if (bedrockPacket instanceof EntityEventPacket pk) {
            var type = pk.getType();
            if (type == EntityEventType.EATING_ITEM) {
                var data = pk.getData();
                var newItem = TypeConverter.translateItemRuntimeId(input, output, data >> 16, data & 0xFFFF);
                pk.setData((newItem[0] << 16) | newItem[1]);
            }
        } else if (bedrockPacket instanceof AddEntityPacket pk) {
            if (pk.getIdentifier().equals("minecraft:falling_block")) {
                var metaData = pk.getMetadata();
                int runtimeId = metaData.get(EntityDataTypes.VARIANT);
                metaData.put(EntityDataTypes.VARIANT, TypeConverter.translateBlockRuntimeId(input, output, runtimeId));
                pk.setMetadata(metaData);
            }
        } else if (bedrockPacket instanceof UpdateSubChunkBlocksPacket pk) {
            var newExtraBlocks = new ArrayList<BlockChangeEntry>(pk.getExtraBlocks().size());
            for (var entry : pk.getExtraBlocks()) {
                newExtraBlocks.add(new BlockChangeEntry(entry.getPosition(), TypeConverter.translateBlockDefinition(input, output, entry.getDefinition()), entry.getUpdateFlags(), entry.getMessageEntityId(), entry.getMessageType()));
            }
            pk.getExtraBlocks().clear();
            pk.getExtraBlocks().addAll(newExtraBlocks);
            var newStandardBlock = new ArrayList<BlockChangeEntry>(pk.getStandardBlocks().size());
            for (var entry : pk.getStandardBlocks()) {
                newStandardBlock.add(new BlockChangeEntry(entry.getPosition(), TypeConverter.translateBlockDefinition(input, output, entry.getDefinition()), entry.getUpdateFlags(), entry.getMessageEntityId(), entry.getMessageType()));
            }
            pk.getStandardBlocks().clear();
            pk.getStandardBlocks().addAll(newStandardBlock);
        } else if (bedrockPacket instanceof LevelChunkPacket packet) {
            var from = packet.getData();
            var to = AbstractByteBufAllocator.DEFAULT.buffer(from.readableBytes()).touch();
            try {
                var newSubChunkCount = TypeConverter.rewriteFullChunk(input, output, from, to, packet.getDimension(), packet.getSubChunksLength());
                packet.setSubChunksLength(newSubChunkCount);
                packet.setData(to.retain());
            } catch (ChunkRewriteException exception) {
//                log.error("Failed to rewrite chunk: ", exception);
            } finally {
                ReferenceCountUtil.release(from);
                ReferenceCountUtil.release(to);
            }
        } else if (bedrockPacket instanceof SubChunkPacket packet) {
            for (var subChunk : packet.getSubChunks()) {
                if (subChunk.getData().readableBytes() > 0) {
                    var from = subChunk.getData();
                    var to = AbstractByteBufAllocator.DEFAULT.buffer(from.readableBytes());
                    try {
                        TypeConverter.rewriteSubChunk(input, output, from, to);
                        TypeConverter.rewriteBlockEntities(input, output, from, to);
                        to.writeBytes(from);
                        subChunk.setData(to.retain());
                    } catch (ChunkRewriteException exception) {
//                        log.error("Failed to rewrite chunk: ", exception);
                    } finally {
                        ReferenceCountUtil.release(from);
                        ReferenceCountUtil.release(to);
                    }
                }
            }
            if (output < Bedrock_v475.CODEC.getProtocolVersion()) {
                packet.getSubChunks().subList(0, 4).clear();
            }
        }
        
        return bedrockPacket;
    }
}
