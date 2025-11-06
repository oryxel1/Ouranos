package com.github.blackjack200.ouranos.translators.inventory;

import com.github.blackjack200.ouranos.converter.TypeConverter;
import com.github.blackjack200.ouranos.session.OuranosSession;
import org.cloudburstmc.protocol.bedrock.codec.v776.Bedrock_v776;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.ArrayList;
import java.util.List;

public class ItemRewriterTranslator implements BaseTranslator {
    @Override
    public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final int input = session.getTargetVersion(), output = session.getProtocolId();

        if (bedrockPacket instanceof InventoryContentPacket packet) {
            packet.getContents().replaceAll(itemData -> TypeConverter.translateItemData(input, output, itemData));
        } else if (bedrockPacket instanceof CraftingDataPacket packet) {
            packet.getPotionMixData().clear();
            packet.getMaterialReducers().clear();
            packet.getCraftingData().clear();
            packet.getContainerMixData().clear();
            packet.setCleanRecipes(true);
        } else if (bedrockPacket instanceof CreativeContentPacket packet) {
            final List<CreativeItemData> contents = new ArrayList<>();
            for (int i = 0; i < packet.getContents().size(); i++) {
                var old = packet.getContents().get(i);
                var item = TypeConverter.translateCreativeItemData(input, output, old);
                contents.add(item);
            }
            packet.getContents().clear();
            if (session.getProtocolId() >= session.getTargetVersion() || session.getTargetVersion() < Bedrock_v776.CODEC.getProtocolVersion()) {
                packet.getContents().addAll(contents);
            }
            final List<CreativeItemGroup> groups = new ArrayList<>();
            for (var group : packet.getGroups()) {
                groups.add(group.toBuilder().icon(TypeConverter.translateItemData(input, output, group.getIcon())).build());
            }
            packet.getGroups().clear();
            packet.getGroups().addAll(groups);
        } else if (bedrockPacket instanceof AddItemEntityPacket packet) {
            packet.setItemInHand(TypeConverter.translateItemData(input, output, packet.getItemInHand()));
        } else if (bedrockPacket instanceof InventorySlotPacket packet) {
            packet.setItem(TypeConverter.translateItemData(input, output, packet.getItem()));
            packet.setStorageItem(TypeConverter.translateItemData(input, output, packet.getStorageItem()));
        } else if (bedrockPacket instanceof AddPlayerPacket packet) {
            packet.setHand(TypeConverter.translateItemData(input, output, packet.getHand()));
        }

        return translateBothWay(session, bedrockPacket, input, output);
    }

    @Override
    public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final int input = session.getProtocolId(), output = session.getTargetVersion();
        return translateBothWay(session, bedrockPacket, input, output);
    }

    private BedrockPacket translateBothWay(OuranosSession session, final BedrockPacket bedrockPacket, int input, int output) {
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

        return bedrockPacket;
    }
}
