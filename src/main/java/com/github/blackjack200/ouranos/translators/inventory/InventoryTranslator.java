package com.github.blackjack200.ouranos.translators.inventory;

import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import com.github.blackjack200.ouranos.translators.storages.ClientInventoryStorage;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.codec.v407.Bedrock_v407;
import org.cloudburstmc.protocol.bedrock.data.inventory.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.PlaceAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TakeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class InventoryTranslator implements BaseTranslator {
    @Override
    public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final int input = session.getProtocolId(), output = session.getTargetVersion();

        final ClientInventoryStorage storage = session.get(ClientInventoryStorage.class);

        if (bedrockPacket instanceof CreativeContentPacket packet && output < Bedrock_v407.CODEC.getProtocolVersion()) {
            final InventoryContentPacket newPk = new InventoryContentPacket();
            newPk.setContainerId(ContainerId.CREATIVE);
            List<ItemData> items = packet.getContents().stream().map(CreativeItemData::getItem).filter(i -> i.getDefinition().getVersion().equals(ItemVersion.LEGACY)).collect(Collectors.toList());
            newPk.setContents(items);
            return newPk;
        } else if (bedrockPacket instanceof InventoryContentPacket packet) {
            storage.getInventories().put(packet.getContainerId(), new ArrayList<>(packet.getContents()));
        } else if (bedrockPacket instanceof InventorySlotPacket packet) {
            storage.getInventories().putIfAbsent(packet.getContainerId(), new ArrayList<>());
            var inv = storage.getInventories().get(packet.getContainerId());
            while (inv.size() <= packet.getSlot()) {
                inv.add(ItemData.AIR);
            }
            inv.set(packet.getSlot(), packet.getItem());
        } else if (bedrockPacket instanceof MobEquipmentPacket packet) {
            storage.getInventories().putIfAbsent(packet.getContainerId(), new ArrayList<>());
            var inv = storage.getInventories().get(packet.getContainerId());
            while (inv.size() < packet.getInventorySlot()) {
                inv.add(ItemData.AIR);
            }
            inv.set(packet.getInventorySlot(), packet.getItem());
        } else if (bedrockPacket instanceof ItemStackResponsePacket packet) {
            for (var entry : packet.getEntries()) {
                var xa = storage.getStackResponses().get(entry.getRequestId());
                storage.getStackResponses().remove(entry.getRequestId());
                if (entry.getResult() == ItemStackResponseStatus.OK) {
                    if (xa != null) {
                        xa.accept(entry.getContainers());
                    }
                    for (var slot : entry.getContainers()) {
                        var id = parseContainerId(slot.getContainerName().getContainer());
                        var container = storage.getInventories().get(id);
                        if (container != null) {
                            for (var item : slot.getItems()) {
                                container.set(item.getSlot(), container.get(item.getSlot()).toBuilder().count(item.getCount()).damage(item.getDurabilityCorrection()).usingNetId(true).netId(item.getStackNetworkId()).build());
                            }
                        }
                    }
                } else {
                    storage.getInventories().forEach((containerId, contents) -> {
                        var pp = new InventoryContentPacket();
                        pp.setContainerId(containerId);
                        pp.setContents(contents);
                        session.sendUpstreamPacket(pp);
                    });
                    return null;
                }
            }
        }

        return bedrockPacket;
    }

    @Override
    public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final ClientInventoryStorage storage = session.get(ClientInventoryStorage.class);

        if (bedrockPacket instanceof InventoryTransactionPacket packet) {
            if (packet.getActions().size() == 1) {
                var a = packet.getActions().get(0);
                //TODO
            } else if (packet.getActions().size() == 2) {
                var a = packet.getActions().get(0);
                var b = packet.getActions().get(1);

                var newPk = new ItemStackRequestPacket();
                var aInv = parseContainerId(a.getSource().getContainerId());
                var bInv = parseContainerId(b.getSource().getContainerId());

                if (a.getSource().getType() == InventorySource.Type.CONTAINER && b.getSource().getType() == InventorySource.Type.CONTAINER) {
                    var source = storage.getInventories().get(a.getSource().getContainerId()).get(a.getSlot());
                    var destination = storage.getInventories().get(b.getSource().getContainerId()).get(b.getSlot());
                    if (!source.isNull()) {
                        var count = Math.abs(source.getCount() - a.getToItem().getCount());
                        if (destination.isNull()) {
                            newPk.getRequests().add(new ItemStackRequest(0, new ItemStackRequestAction[]{
                                    new TakeAction(
                                            count,
                                            new ItemStackRequestSlotData(aInv, a.getSlot(), source.getNetId(), new FullContainerName(aInv, 0)),
                                            new ItemStackRequestSlotData(bInv, b.getSlot(), destination.getNetId(), new FullContainerName(bInv, 0))
                                    )
                            }, new String[]{}));
                            storage.getStackResponses().put(0, (slots) -> {
                                storage.getInventories().get(a.getSource().getContainerId()).set(a.getSlot(), source.toBuilder().count(source.getCount() - count).build());
                                storage.getInventories().get(b.getSource().getContainerId()).set(b.getSlot(), source.toBuilder().count(count).build());
                            });
                        } else {
                            newPk.getRequests().add(new ItemStackRequest(1, new ItemStackRequestAction[]{
                                    new PlaceAction(
                                            count,
                                            new ItemStackRequestSlotData(aInv, a.getSlot(), source.getNetId(), new FullContainerName(aInv, 0)),
                                            new ItemStackRequestSlotData(bInv, b.getSlot(), destination.getNetId(), new FullContainerName(bInv, 0))
                                    )
                            }, new String[]{}));
                            storage.getStackResponses().put(1, (slots) -> {
                                storage.getInventories().get(a.getSource().getContainerId()).set(a.getSlot(), source.toBuilder().count(source.getCount() - count).build());
                                storage.getInventories().get(b.getSource().getContainerId()).set(b.getSlot(), destination.toBuilder().count(destination.getCount() + count).build());
                            });
                        }
                    } else {
                        newPk.getRequests().add(new ItemStackRequest(2, new ItemStackRequestAction[]{
                                new SwapAction(
                                        new ItemStackRequestSlotData(aInv, a.getSlot(), source.getNetId(), new FullContainerName(aInv, 0)),
                                        new ItemStackRequestSlotData(bInv, b.getSlot(), destination.getNetId(), new FullContainerName(bInv, 0))
                                )
                        }, new String[]{}));
                        storage.getStackResponses().put(2, (slots) -> {
                            storage.getInventories().get(a.getSource().getContainerId()).set(a.getSlot(), destination);
                            storage.getInventories().get(b.getSource().getContainerId()).set(b.getSlot(), source);
                        });
                    }

                    return newPk;
                }
            }
        }

        return bedrockPacket;
    }

    private static ContainerSlotType parseContainerId(int containerId) {
        switch (containerId) {
            case ContainerId.NONE:
                return ContainerSlotType.UNKNOWN;
            case ContainerId.INVENTORY:
                return ContainerSlotType.INVENTORY;
            case ContainerId.HOTBAR:
                return ContainerSlotType.HOTBAR;
            case ContainerId.ARMOR:
                return ContainerSlotType.ARMOR;
            case ContainerId.OFFHAND:
                return ContainerSlotType.OFFHAND;
            case ContainerId.UI:
                return ContainerSlotType.CURSOR;
            case ContainerId.BEACON:
                return ContainerSlotType.BEACON_PAYMENT;
            case ContainerId.ENCHANT_INPUT:
                return ContainerSlotType.ENCHANTING_INPUT;
            case ContainerId.ENCHANT_OUTPUT:
                return ContainerSlotType.ENCHANTING_MATERIAL;
            default:
                log.error("Unknown container id: {}", containerId);
        }
        return ContainerSlotType.UNKNOWN;
    }

    private static int parseContainerId(ContainerSlotType containerId) {
        switch (containerId) {
            case UNKNOWN:
                return ContainerId.NONE;
            case INVENTORY, HOTBAR, HOTBAR_AND_INVENTORY:
                return ContainerId.INVENTORY;
            case ARMOR:
                return ContainerId.ARMOR;
            case OFFHAND:
                return ContainerId.OFFHAND;
            case CURSOR:
                return ContainerId.UI;
            case BEACON_PAYMENT:
                return ContainerId.BEACON;
            case ENCHANTING_INPUT:
                return ContainerId.ENCHANT_INPUT;
            case ENCHANTING_MATERIAL:
                return ContainerId.ENCHANT_OUTPUT;
            default:
                log.error("Unknown container id: {}", containerId);
        }
        return ContainerSlotType.UNKNOWN.ordinal();
    }
}
