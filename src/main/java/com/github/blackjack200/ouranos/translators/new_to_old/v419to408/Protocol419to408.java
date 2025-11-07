package com.github.blackjack200.ouranos.translators.new_to_old.v419to408;

import com.github.blackjack200.ouranos.base.ProtocolToProtocol;
import com.github.blackjack200.ouranos.converter.BlockStateDictionary;
import com.github.blackjack200.ouranos.data.LegacyBlockIdToStringIdMap;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;

import java.util.List;
import java.util.Objects;

public class Protocol419to408 extends ProtocolToProtocol {
    @Override
    protected void registerProtocol() {
        this.registerClientbound(ItemStackResponsePacket.class, wrapped -> {
            final ItemStackResponsePacket packet = (ItemStackResponsePacket) wrapped.getPacket();

            packet.getEntries().replaceAll(entry -> new ItemStackResponse(entry.getResult().equals(ItemStackResponseStatus.OK), entry.getRequestId(), entry.getContainers()));
        });

        this.registerClientbound(StartGamePacket.class, wrapped -> {
            final StartGamePacket packet = (StartGamePacket) wrapped.getPacket();

            final List<NbtMap> states = BlockStateDictionary.getInstance(wrapped.getOutput()).getKnownStates().stream().map((e) -> {
                short legacyId = (short) (Objects.requireNonNullElse(LegacyBlockIdToStringIdMap.getInstance().fromString(wrapped.getOutput(), e.name()), 255) & 0xfffffff);
                return NbtMap.builder().putCompound("block", e.rawState()).putShort("id", legacyId).build();
            }).toList();
            packet.setBlockPalette(new NbtList<>(NbtType.COMPOUND, states));
        });
    }
}
