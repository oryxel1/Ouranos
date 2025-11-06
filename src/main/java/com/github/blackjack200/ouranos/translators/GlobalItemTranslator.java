package com.github.blackjack200.ouranos.translators;

import com.github.blackjack200.ouranos.base.ProtocolToProtocol;
import com.github.blackjack200.ouranos.base.WrappedBedrockPacket;
import com.github.blackjack200.ouranos.converter.TypeConverter;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// These will be translated directly instead of passing through multiple translators.
public class GlobalItemTranslator extends ProtocolToProtocol {
    @Override
    protected void registerProtocol() {
        this.registerServerbound(ItemStackRequestPacket.class, wrapped -> {
            final ItemStackRequestPacket packet = (ItemStackRequestPacket) wrapped.getPacket();

            final List<ItemStackRequest> newRequests = new ArrayList<>();
            for (final ItemStackRequest request : packet.getRequests()) {
                final List<ItemStackRequestAction> newActions = new ArrayList<>();
                final ItemStackRequestAction[] actions = request.getActions();
                for (final ItemStackRequestAction action : actions) {
                    switch (action) {
                        case TakeAction a ->
                                newActions.add(new TakeAction(a.getCount(), translateItemStackRequestSlotData(a.getSource()), translateItemStackRequestSlotData(a.getDestination())));
                        case ConsumeAction a ->
                                newActions.add(new ConsumeAction(a.getCount(), translateItemStackRequestSlotData(a.getSource())));
                        case DestroyAction a ->
                                newActions.add(new DestroyAction(a.getCount(), translateItemStackRequestSlotData(a.getSource())));
                        case DropAction a ->
                                newActions.add(new DropAction(a.getCount(), translateItemStackRequestSlotData(a.getSource()), a.isRandomly()));
                        case PlaceAction a -> {
                            final ItemStackRequestAction newAct = new PlaceAction(a.getCount(), translateItemStackRequestSlotData(a.getSource()), translateItemStackRequestSlotData(a.getDestination()));
                            newActions.add(newAct);
                        }
                        case SwapAction a ->
                                newActions.add(new SwapAction(translateItemStackRequestSlotData(a.getSource()), translateItemStackRequestSlotData(a.getDestination())));
                        case null, default -> newActions.add(action);
                    }
                }
                newRequests.add(new ItemStackRequest(request.getRequestId(), newActions.toArray(new ItemStackRequestAction[0]), request.getFilterStrings()));
            }
            packet.getRequests().clear();
            packet.getRequests().addAll(newRequests);
        });

        this.registerClientbound(InventoryContentPacket.class, wrapped -> {
            final InventoryContentPacket packet = (InventoryContentPacket) wrapped.getPacket();
            packet.getContents().replaceAll(itemData -> TypeConverter.translateItemData(wrapped.getInput(), wrapped.getOutput(), itemData));
        });

        this.registerClientbound(CraftingDataPacket.class, wrapped -> {
            final CraftingDataPacket packet = (CraftingDataPacket) wrapped.getPacket();

            // TODO: Properly translate these... Not too hard however I'm not used to this codebase (ouranos) yet!
            packet.getPotionMixData().clear();
            packet.getMaterialReducers().clear();
            packet.getCraftingData().clear();
            packet.getContainerMixData().clear();
            packet.setCleanRecipes(true);
        });

        this.registerClientbound(CreativeContentPacket.class, wrapped -> {
            final CreativeContentPacket packet = (CreativeContentPacket) wrapped.getPacket();
            final int input = wrapped.getInput(), output = wrapped.getOutput();

            final List<CreativeItemData> contents = new ArrayList<>();
            for (int i = 0; i < packet.getContents().size(); i++) {
                final CreativeItemData old = packet.getContents().get(i);
                contents.add(TypeConverter.translateCreativeItemData(input, output, old));
            }
            packet.getContents().clear();
            packet.getContents().addAll(contents);

            final List<CreativeItemGroup> groups = new ArrayList<>();
            for (var group : packet.getGroups()) {
                groups.add(group.toBuilder().icon(TypeConverter.translateItemData(input, output, group.getIcon())).build());
            }
            packet.getGroups().clear();
            packet.getGroups().addAll(groups);
        });

        this.registerClientbound(AddItemEntityPacket.class, wrapped -> {
            final AddItemEntityPacket packet = (AddItemEntityPacket) wrapped.getPacket();
            packet.setItemInHand(TypeConverter.translateItemData(wrapped.getInput(), wrapped.getOutput(), packet.getItemInHand()));
        });

        this.registerClientbound(InventorySlotPacket.class, wrapped -> {
            final InventorySlotPacket packet = (InventorySlotPacket) wrapped.getPacket();
            final int input = wrapped.getInput(), output = wrapped.getOutput();

            packet.setItem(TypeConverter.translateItemData(input, output, packet.getItem()));
            packet.setStorageItem(TypeConverter.translateItemData(input, output, packet.getStorageItem()));
        });

        this.registerClientbound(AddPlayerPacket.class, wrapped -> {
            final AddPlayerPacket packet = (AddPlayerPacket) wrapped.getPacket();
            packet.setHand(TypeConverter.translateItemData(wrapped.getInput(), wrapped.getOutput(), packet.getHand()));
        });

        this.registerClientbound(InventoryTransactionPacket.class, this::translateBothWay);
        this.registerClientbound(MobEquipmentPacket.class, this::translateBothWay);
        this.registerClientbound(MobArmorEquipmentPacket.class, this::translateBothWay);
    }

    private void translateBothWay(WrappedBedrockPacket wrapped) {
        final int input = wrapped.getInput(), output = wrapped.getOutput();

        final BedrockPacket bedrockPacket = wrapped.getPacket();
        if (bedrockPacket instanceof InventoryTransactionPacket packet) {
            final List<InventoryActionData> newActions = new ArrayList<>(packet.getActions().size());
            for (var action : packet.getActions()) {
                newActions.add(new InventoryActionData(action.getSource(), action.getSlot(), TypeConverter.translateItemData(input, output, action.getFromItem()), TypeConverter.translateItemData(input, output, action.getToItem()), action.getStackNetworkId()));
            }
            packet.getActions().clear();
            packet.getActions().addAll(newActions);

            if (packet.getBlockDefinition() != null) {
                packet.setBlockDefinition(TypeConverter.translateBlockDefinition(input, output, packet.getBlockDefinition()));
            }
            if (packet.getItemInHand() != null) {
                packet.setItemInHand(TypeConverter.translateItemData(input, output, packet.getItemInHand()));
            }
        } else if (bedrockPacket instanceof MobEquipmentPacket packet) {
            packet.setItem(TypeConverter.translateItemData(input, output, packet.getItem()));
        } else if (bedrockPacket instanceof MobArmorEquipmentPacket packet) {
            if (packet.getBody() != null) {
                packet.setBody(TypeConverter.translateItemData(input, output, packet.getBody()));
            }
            packet.setChestplate(TypeConverter.translateItemData(input, output, packet.getChestplate()));
            packet.setHelmet(TypeConverter.translateItemData(input, output, packet.getHelmet()));
            packet.setBoots(TypeConverter.translateItemData(input, output, packet.getBoots()));
            packet.setLeggings(TypeConverter.translateItemData(input, output, packet.getLeggings()));
        }
    }

    private ItemStackRequestSlotData translateItemStackRequestSlotData(ItemStackRequestSlotData dest) {
        return new ItemStackRequestSlotData(
                dest.getContainer(),
                dest.getSlot(),
                dest.getStackNetworkId(),
                Optional.ofNullable(dest.getContainerName())
                        .orElse(new FullContainerName(dest.getContainer(), 0))
        );
    }
}
