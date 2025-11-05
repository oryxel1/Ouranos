package com.github.blackjack200.ouranos.translators;

import com.github.blackjack200.ouranos.converter.ChunkRewriteException;
import com.github.blackjack200.ouranos.converter.ItemTypeDictionary;
import com.github.blackjack200.ouranos.converter.TypeConverter;
import com.github.blackjack200.ouranos.translators.inventory.InventoryTranslator;
import com.github.blackjack200.ouranos.utils.SimpleBlockDefinition;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.math.immutable.vector.ImmutableVectorProvider;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v408.Bedrock_v408;
import org.cloudburstmc.protocol.bedrock.codec.v419.Bedrock_v419;
import org.cloudburstmc.protocol.bedrock.codec.v475.Bedrock_v475;
import org.cloudburstmc.protocol.bedrock.codec.v503.Bedrock_v503;
import org.cloudburstmc.protocol.bedrock.codec.v527.Bedrock_v527;
import org.cloudburstmc.protocol.bedrock.codec.v534.Bedrock_v534;
import org.cloudburstmc.protocol.bedrock.codec.v544.Bedrock_v544;
import org.cloudburstmc.protocol.bedrock.codec.v554.Bedrock_v554;
import org.cloudburstmc.protocol.bedrock.codec.v560.Bedrock_v560;
import org.cloudburstmc.protocol.bedrock.codec.v575.Bedrock_v575;
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594;
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618;
import org.cloudburstmc.protocol.bedrock.codec.v685.Bedrock_v685;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
import org.cloudburstmc.protocol.bedrock.codec.v776.Bedrock_v776;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.*;
import java.util.List;
import java.util.function.BiFunction;

@SuppressWarnings("deprecation")
@Log4j2
public class Translate {
    public static Collection<BedrockPacket> translate(int input, int output, boolean fromServer, OuranosProxySession player, BedrockPacket p) {
        if (input == output) {
            return List.of(p);
        }

        var list = new HashSet<BedrockPacket>();
        list.add(p);

        rewriteProtocol(input, output, fromServer, player, p, list);
        rewriteBlock(input, output, player, p, list);
        rewriteChunk(input, output, player, p, list);

        InventoryTranslator.rewriteInventory(input, output, fromServer, player, p, list);
        if (output >= Bedrock_v554.CODEC.getProtocolVersion()) {
            list.removeIf((b) -> b instanceof AdventureSettingsPacket);
        }
        if (output > Bedrock_v408.CODEC.getProtocolVersion()) {
            list.removeIf((b) -> b instanceof EntityFallPacket);
        }
        return list;
    }

    private static ItemDescriptorWithCount translateItemDescriptorWithCount(int input, int output, ItemDescriptorWithCount i) {
        var descriptor = TypeConverter.translateItemDescriptor(input, output, i.getDescriptor());
        if (descriptor == null) {
            return null;
        }
        return new ItemDescriptorWithCount(descriptor, i.getCount());
    }

    private static void rewriteProtocol(int input, int output, boolean fromServer, BedrockPacket p) {


        rewriteAdventureSettings(input, output, fromServer, player, p, list);
    }



    private static void rewritePlayerInput(int input, int output, OuranosProxySession player, BedrockPacket p, Collection<BedrockPacket> list) {

    }

    private static void rewriteAdventureSettings(int input, int output, boolean fromServer, OuranosProxySession player, BedrockPacket p, Collection<BedrockPacket> list) {
        if (!fromServer) {
            if (p instanceof RequestAbilityPacket pk) {
                rewriteFlying(output, player, list, pk.getAbility() == Ability.FLYING);
            }
            if (p instanceof AdventureSettingsPacket pk) {
                rewriteFlying(output, player, list, pk.getSettings().contains(AdventureSetting.FLYING));
            }
            if (output < Bedrock_v618.CODEC.getProtocolVersion()) {
                if (p instanceof PlayerActionPacket packet) {
                    if (packet.getAction() == PlayerActionType.START_FLYING) {
                        rewriteFlying(output, player, list, true);
                    }
                    if (packet.getAction() == PlayerActionType.STOP_FLYING) {
                        rewriteFlying(output, player, list, false);
                    }
                }
            }
        }

        //downgrade
        if (output < Bedrock_v534.CODEC.getProtocolVersion()) {
            if (p instanceof UpdateAbilitiesPacket pk) {
                var newPk = new AdventureSettingsPacket();
                newPk.setUniqueEntityId(pk.getUniqueEntityId());
                newPk.setCommandPermission(pk.getCommandPermission());
                newPk.setPlayerPermission(pk.getPlayerPermission());
                newPk.getSettings().clear();
                var abilities = pk.getAbilityLayers().get(0).getAbilityValues();
                BiFunction<Ability, AdventureSetting, Void> f = (Ability b1, AdventureSetting b) -> {
                    if (abilities.contains(b1)) {
                        newPk.getSettings().add(b);
                    }
                    return null;
                };
                f.apply(Ability.BUILD, AdventureSetting.BUILD);
                f.apply(Ability.MINE, AdventureSetting.MINE);
                f.apply(Ability.DOORS_AND_SWITCHES, AdventureSetting.DOORS_AND_SWITCHES);
                f.apply(Ability.OPEN_CONTAINERS, AdventureSetting.OPEN_CONTAINERS);
                f.apply(Ability.ATTACK_PLAYERS, AdventureSetting.ATTACK_PLAYERS);
                f.apply(Ability.ATTACK_MOBS, AdventureSetting.ATTACK_MOBS);
                f.apply(Ability.OPERATOR_COMMANDS, AdventureSetting.OPERATOR);
                f.apply(Ability.TELEPORT, AdventureSetting.TELEPORT);
                f.apply(Ability.FLYING, AdventureSetting.FLYING);
                f.apply(Ability.MAY_FLY, AdventureSetting.MAY_FLY);
                f.apply(Ability.MUTED, AdventureSetting.MUTED);
                f.apply(Ability.WORLD_BUILDER, AdventureSetting.WORLD_BUILDER);
                f.apply(Ability.NO_CLIP, AdventureSetting.NO_CLIP);
                if (!abilities.contains(Ability.MINE) && !abilities.contains(Ability.BUILD)) {
                    newPk.getSettings().add(AdventureSetting.WORLD_IMMUTABLE);
                    newPk.getSettings().remove(AdventureSetting.BUILD);
                    newPk.getSettings().remove(AdventureSetting.MINE);
                }
                list.add(newPk);
            }
        }

        //upgrade
        if (output > Bedrock_v554.CODEC.getProtocolVersion()) {
            if (fromServer && p instanceof AdventureSettingsPacket pk) {
                var newPk = new UpdateAbilitiesPacket();
                newPk.setUniqueEntityId(pk.getUniqueEntityId());
                newPk.setPlayerPermission(pk.getPlayerPermission());
                newPk.setCommandPermission(pk.getCommandPermission());
                var layer = new AbilityLayer();
                layer.setLayerType(AbilityLayer.Type.BASE);
                layer.setFlySpeed(0.05f);
                layer.setWalkSpeed(0.1f);

                var settings = pk.getSettings();
                Collections.addAll(layer.getAbilitiesSet(), Ability.values());
                newPk.setAbilityLayers(List.of(layer));
                BiFunction<AdventureSetting, Ability, Void> f = (AdventureSetting b, Ability b1) -> {
                    if (settings.contains(b)) {
                        layer.getAbilityValues().add(b1);
                    }
                    return null;
                };
                f.apply(AdventureSetting.BUILD, Ability.BUILD);
                f.apply(AdventureSetting.MINE, Ability.MINE);
                f.apply(AdventureSetting.DOORS_AND_SWITCHES, Ability.DOORS_AND_SWITCHES);
                f.apply(AdventureSetting.OPEN_CONTAINERS, Ability.OPEN_CONTAINERS);
                f.apply(AdventureSetting.ATTACK_PLAYERS, Ability.ATTACK_PLAYERS);
                f.apply(AdventureSetting.ATTACK_MOBS, Ability.ATTACK_MOBS);
                f.apply(AdventureSetting.OPERATOR, Ability.OPERATOR_COMMANDS);
                f.apply(AdventureSetting.TELEPORT, Ability.TELEPORT);
                f.apply(AdventureSetting.FLYING, Ability.FLYING);
                f.apply(AdventureSetting.MAY_FLY, Ability.MAY_FLY);
                f.apply(AdventureSetting.MUTED, Ability.MUTED);
                f.apply(AdventureSetting.WORLD_BUILDER, Ability.WORLD_BUILDER);
                f.apply(AdventureSetting.NO_CLIP, Ability.NO_CLIP);
                list.add(newPk);

                var newPk2 = new UpdateAdventureSettingsPacket();
                newPk2.setAutoJump(settings.contains(AdventureSetting.AUTO_JUMP));
                newPk2.setImmutableWorld(settings.contains(AdventureSetting.WORLD_IMMUTABLE));
                newPk2.setNoMvP(settings.contains(AdventureSetting.NO_MVP));
                newPk2.setNoPvM(settings.contains(AdventureSetting.NO_PVM));
                newPk2.setShowNameTags(settings.contains(AdventureSetting.SHOW_NAME_TAGS));
                list.add(newPk2);
            }
        }
    }

    private static void rewriteFlying(int output, OuranosProxySession player, Collection<BedrockPacket> list, boolean flying) {
        if (output < Bedrock_v527.CODEC.getProtocolVersion()) {
            var newPk = new AdventureSettingsPacket();
            newPk.setUniqueEntityId(player.uniqueEntityId);
            if (flying) {
                newPk.getSettings().add(AdventureSetting.FLYING);
            }
            list.add(newPk);
        } else {
            var newPk = new RequestAbilityPacket();
            newPk.setAbility(Ability.FLYING);
            newPk.setType(Ability.Type.BOOLEAN);
            newPk.setBoolValue(flying);
            list.add(newPk);
        }
    }

    @SuppressWarnings("deprecation")


    private static void removeNewEntityData(BedrockPacket p, int output, BedrockCodec codec, EntityDataType<?>... types) {
        if (output < codec.getProtocolVersion()) {
            if (p instanceof SetEntityDataPacket pk) {
                for (val typ : types) {
                    pk.getMetadata().remove(typ);
                }
            } else if (p instanceof AddEntityPacket pk) {
                for (val typ : types) {
                    pk.getMetadata().remove(typ);
                }
            } else if (p instanceof AddPlayerPacket pk) {
                for (val typ : types) {
                    pk.getMetadata().remove(typ);
                }
            } else if (p instanceof AddItemEntityPacket pk) {
                for (val typ : types) {
                    pk.getMetadata().remove(typ);
                }
            }
        }
    }

    private static void rewriteBlock(int input, int output, OuranosProxySession player, BedrockPacket p, Collection<BedrockPacket> list) {
        if (p instanceof UpdateBlockPacket packet) {
            var runtimeId = packet.getDefinition().getRuntimeId();
            var translated = TypeConverter.translateBlockRuntimeId(input, output, runtimeId);
            packet.setDefinition(new SimpleBlockDefinition(translated));
        } else if (p instanceof LevelEventPacket packet) {
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
        } else if (p instanceof LevelSoundEventPacket pk) {
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
                        list.clear();
                        var newPk = new LevelEventPacket();
                        newPk.setType(LevelEvent.SOUND_DOOR_OPEN);
                        newPk.setPosition(pk.getPosition());
                        list.add(newPk);
                    }
                    break;
                case PLACE:
                case BREAK_BLOCK:
                case ITEM_USE_ON:
                    pk.setExtraData(TypeConverter.translateBlockRuntimeId(input, output, runtimeId));
            }
        } else if (p instanceof EntityEventPacket pk) {
            var type = pk.getType();
            if (type == EntityEventType.EATING_ITEM) {
                var data = pk.getData();
                var newItem = TypeConverter.translateItemRuntimeId(input, output, data >> 16, data & 0xFFFF);
                pk.setData((newItem[0] << 16) | newItem[1]);
            }
        } else if (p instanceof AddEntityPacket pk) {
            if (pk.getIdentifier().equals("minecraft:falling_block")) {
                var metaData = pk.getMetadata();
                int runtimeId = metaData.get(EntityDataTypes.VARIANT);
                metaData.put(EntityDataTypes.VARIANT, TypeConverter.translateBlockRuntimeId(input, output, runtimeId));
                pk.setMetadata(metaData);
            }
        } else if (p instanceof UpdateSubChunkBlocksPacket pk) {
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
        }
    }

    private static void rewriteChunk(int input, int output, OuranosProxySession player, BedrockPacket p, Collection<BedrockPacket> list) {
        if (p instanceof LevelChunkPacket packet) {
            var from = packet.getData();
            var to = AbstractByteBufAllocator.DEFAULT.buffer(from.readableBytes()).touch();
            try {
                var newSubChunkCount = TypeConverter.rewriteFullChunk(input, output, from, to, packet.getDimension(), packet.getSubChunksLength());
                packet.setSubChunksLength(newSubChunkCount);
                packet.setData(to.retain());
            } catch (ChunkRewriteException exception) {
                log.error("Failed to rewrite chunk: ", exception);
            } finally {
                ReferenceCountUtil.release(from);
                ReferenceCountUtil.release(to);
            }
            return;
        }
        if (p instanceof SubChunkPacket packet) {
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
                        log.error("Failed to rewrite chunk: ", exception);
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
    }
}