package com.github.blackjack200.ouranos.translators;

import com.github.blackjack200.ouranos.ProtocolInfo;
import com.github.blackjack200.ouranos.converter.ItemTypeDictionary;
import com.github.blackjack200.ouranos.converter.biome.BiomeDefinitionRegistry;
import com.github.blackjack200.ouranos.session.OuranosSession;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.codec.v419.Bedrock_v419;
import org.cloudburstmc.protocol.bedrock.codec.v527.Bedrock_v527;
import org.cloudburstmc.protocol.bedrock.codec.v544.Bedrock_v544;
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589;
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649;
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671;
import org.cloudburstmc.protocol.bedrock.codec.v685.Bedrock_v685;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
import org.cloudburstmc.protocol.bedrock.codec.v776.Bedrock_v776;
import org.cloudburstmc.protocol.bedrock.codec.v800.Bedrock_v800;
import org.cloudburstmc.protocol.bedrock.data.InputInteractionModel;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.awt.*;
import java.util.*;
import java.util.List;

@SuppressWarnings("ALL")
public class ProtocolRewriterTranslator implements BaseTranslator {
    @Override
    public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final int input = session.getTargetVersion(), output = session.getProtocolId();

        if (bedrockPacket instanceof BiomeDefinitionListPacket packet) {
            // TODO fix biome for v800
            if (output >= Bedrock_v800.CODEC.getProtocolVersion()) {
                if (packet.getBiomes() == null && packet.getDefinitions() != null) {
                    BiomeDefinitions defs = new BiomeDefinitions(new HashMap<>());
                    packet.getDefinitions().forEach((id, n) -> {
                        var def = BiomeDefinitionRegistry.getInstance(input).fromStringId(id);
                        if (def != null) {
                            defs.getDefinitions().put(id, def);
                        }
                    });
                    packet.setBiomes(defs);
                }
            } else {
                if (packet.getBiomes() != null && packet.getDefinitions() == null) {
                    packet.setDefinitions(downgradeBiomeDefinition(output, packet.getBiomes().getDefinitions()));
                }
            }
        } else if (bedrockPacket instanceof PlayerListPacket packet) {
            for (final PlayerListPacket.Entry entry : packet.getEntries()) {
                entry.setColor(Objects.requireNonNullElse(entry.getColor(), Color.WHITE));
                if (entry.getSkin() != null) {
                    entry.setSkin(entry.getSkin().toBuilder().geometryDataEngineVersion(ProtocolInfo.getPacketCodec(output).getMinecraftVersion()).build());
                }
                if (input < Bedrock_v649.CODEC.getProtocolVersion()) {
                    entry.setSubClient(false);
                }
            }
        } else if (bedrockPacket instanceof LevelChunkPacket packet && input < Bedrock_v649.CODEC.getProtocolVersion()) {
            // FIXME overworld?
            packet.setDimension(0);
        } if (input < Bedrock_v544.CODEC.getProtocolVersion() && bedrockPacket instanceof ModalFormResponsePacket packet) {
            if (session.prevFormId == packet.getFormId()) {
                return null;
            }

            session.prevFormId = packet.getFormId();
        }

        if (input < Bedrock_v729.CODEC.getProtocolVersion() && bedrockPacket instanceof TransferPacket packet) {
            packet.setReloadWorld(true);
        }

        if (bedrockPacket instanceof StartGamePacket packet && output >= Bedrock_v776.CODEC.getProtocolVersion()) {
            final ItemComponentPacket componentPacket = new ItemComponentPacket();

            List<ItemDefinition> def = ItemTypeDictionary.getInstance(output).getEntries().entrySet().stream().<ItemDefinition>map((e) -> e.getValue().toDefinition(e.getKey())).toList();
            componentPacket.getItems().addAll(def);

            session.sendUpstreamPacket(componentPacket);
        }
        if (bedrockPacket instanceof ItemStackResponsePacket packet && input >= Bedrock_v419.CODEC.getProtocolVersion() && output < Bedrock_v419.CODEC.getProtocolVersion()) {
            final List<ItemStackResponse> translated = packet.getEntries().stream().map((entry) -> {
                return new ItemStackResponse(entry.getResult().equals(ItemStackResponseStatus.OK), entry.getRequestId(), entry.getContainers());
            }).toList();
            packet.getEntries().clear();
            packet.getEntries().addAll(translated);
        }

        return translateBothWay(session, bedrockPacket, input, output);
    }

    @Override
    public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final int input = session.getProtocolId(), output = session.getTargetVersion();

        if (input < Bedrock_v527.CODEC.getProtocolVersion()) {
            if (bedrockPacket instanceof PlayerAuthInputPacket packet) {
                packet.setInputInteractionModel(Optional.ofNullable(packet.getInputInteractionModel()).orElse(InputInteractionModel.CLASSIC));
            } else if (bedrockPacket instanceof PlayerActionPacket packet) {
                packet.setResultPosition(packet.getBlockPosition());
            }
        }

        if (input < Bedrock_v712.CODEC.getProtocolVersion() && bedrockPacket instanceof InventoryTransactionPacket packet) {
            packet.setTriggerType(ItemUseTransaction.TriggerType.PLAYER_INPUT);
            packet.setClientInteractPrediction(ItemUseTransaction.PredictedResult.SUCCESS);
        }

        if (bedrockPacket instanceof ItemStackRequestPacket packet) {
            final List<ItemStackRequest> newRequests = new ArrayList<>();
            for (final ItemStackRequest reqest : packet.getRequests()) {
                List<ItemStackRequestAction> newActions = new ArrayList<ItemStackRequestAction>();
                final ItemStackRequestAction[] actions = reqest.getActions();
                for (final ItemStackRequestAction action : actions) {
                    if (action instanceof TakeAction a) {
                        newActions.add(new TakeAction(a.getCount(), translateItemStackRequestSlotData(a.getSource()), translateItemStackRequestSlotData(a.getDestination())));
                    } else if (action instanceof ConsumeAction a) {
                        newActions.add(new ConsumeAction(a.getCount(), translateItemStackRequestSlotData(a.getSource())));
                    } else if (action instanceof DestroyAction a) {
                        newActions.add(new DestroyAction(a.getCount(), translateItemStackRequestSlotData(a.getSource())));
                    } else if (action instanceof DropAction a) {
                        newActions.add(new DropAction(a.getCount(), translateItemStackRequestSlotData(a.getSource()), a.isRandomly()));
                    } else if (action instanceof PlaceAction a) {
                        final ItemStackRequestAction newAct = new PlaceAction(a.getCount(), translateItemStackRequestSlotData(a.getSource()), translateItemStackRequestSlotData(a.getDestination()));
                        newActions.add(newAct);
                    } else if (action instanceof SwapAction a) {
                        newActions.add(new SwapAction(translateItemStackRequestSlotData(a.getSource()), translateItemStackRequestSlotData(a.getDestination())));
                    } else {
                        newActions.add(action);
                    }
                }
                newRequests.add(new ItemStackRequest(reqest.getRequestId(), newActions.toArray(new ItemStackRequestAction[0]), reqest.getFilterStrings()));
            }
            packet.getRequests().clear();
            packet.getRequests().addAll(newRequests);
        }

        return translateBothWay(session, bedrockPacket, input, output);
    }

    private BedrockPacket translateBothWay(OuranosSession session, final BedrockPacket bedrockPacket, int input, int output) {
        if (bedrockPacket instanceof PlayerSkinPacket packet && ProtocolInfo.getPacketCodec(output) != null) {
            packet.setSkin(packet.getSkin().toBuilder().geometryDataEngineVersion(ProtocolInfo.getPacketCodec(output).getMinecraftVersion()).build());
        } else if (input < Bedrock_v685.CODEC.getProtocolVersion() && bedrockPacket instanceof ContainerClosePacket packet) {
            packet.setType(ContainerType.NONE);
        } else if (input < Bedrock_v671.CODEC.getProtocolVersion() && bedrockPacket instanceof ResourcePackStackPacket packet) {
            packet.setHasEditorPacks(false);
        } else if (input < Bedrock_v589.CODEC.getProtocolVersion() && bedrockPacket instanceof EmotePacket packet) {
            packet.setXuid("");
            packet.setPlatformId("");
            packet.setEmoteDuration(20);
        }

        return bedrockPacket;
    }

    private static NbtMap downgradeBiomeDefinition(int output, Map<String, BiomeDefinitionData> definitions) {
        var builder = NbtMap.builder();
        if (definitions.isEmpty()) {
            definitions = BiomeDefinitionRegistry.getInstance(output).getEntries();
        }
        definitions.forEach((id, def) -> {
            var d = NbtMap.builder();
            d.putString("name_hash", id);
            d.putFloat("temperature", def.getTemperature());
            d.putFloat("downfall", def.getDownfall());
            d.putBoolean("rain", def.isRain());
            builder.putCompound(id, d.build());
        });
        NbtMap build = builder.build();
        return build;
    }

    private static ItemStackRequestSlotData translateItemStackRequestSlotData(ItemStackRequestSlotData dest) {
        return new ItemStackRequestSlotData(
                dest.getContainer(),
                dest.getSlot(),
                dest.getStackNetworkId(),
                Optional.ofNullable(dest.getContainerName())
                        .orElse(new FullContainerName(dest.getContainer(), 0))
        );
    }
}
