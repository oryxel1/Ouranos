package com.github.blackjack200.ouranos.session;

import com.github.blackjack200.ouranos.ProtocolInfo;
import com.github.blackjack200.ouranos.utils.BlockDictionaryRegistry;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

// Meant for GeyserReversion, since the protocol is relocated..
@SuppressWarnings("ALL")
@Getter
public abstract class SpecialOuranosSession extends OuranosSession {
    private final BedrockCodec clientCodec;
    private final BedrockCodecHelper clientCodecHelper;

    private final BedrockCodec serverCodec;
    private final BedrockCodecHelper serverCodecHelper;

    public SpecialOuranosSession(int protocolId, int targetVersion) {
        super(protocolId, targetVersion);

        this.clientCodec = ProtocolInfo.getPacketCodec(protocolId);
        this.serverCodec = ProtocolInfo.getPacketCodec(targetVersion);

        this.clientCodecHelper = this.clientCodec.createHelper();
        this.serverCodecHelper = this.serverCodec.createHelper();

        this.clientCodecHelper.setBlockDefinitions(new BlockDictionaryRegistry(protocolId));
        this.serverCodecHelper.setBlockDefinitions(new BlockDictionaryRegistry(targetVersion));
    }

    public final boolean translateClientbound(ByteBuf input, ByteBuf output, int id) {
        BedrockPacket packet = this.serverCodec.tryDecode(this.serverCodecHelper, input, id);
        packet = this.translateClientbound(packet);
        if (packet == null) {
            return false;
        }

        this.serverCodec.tryEncode(this.serverCodecHelper, output, packet);
        return true;
    }

    public final boolean translateServerbound(ByteBuf input, ByteBuf output, int id) {
        BedrockPacket packet = this.clientCodec.tryDecode(this.clientCodecHelper, input, id);
        packet = this.translateClientbound(packet);
        if (packet == null) {
            return false;
        }

        this.clientCodec.tryEncode(this.clientCodecHelper, output, packet);
        return true;
    }

    public final void encodeClient(final BedrockPacket packet, final ByteBuf output) {
        this.clientCodec.tryEncode(this.clientCodecHelper, output, packet);
    }

    public final void encodeServer(final BedrockPacket packet, final ByteBuf output) {
        this.serverCodec.tryEncode(this.serverCodecHelper, output, packet);
    }
}
