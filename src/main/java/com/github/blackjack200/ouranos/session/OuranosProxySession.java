package com.github.blackjack200.ouranos.session;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

import java.security.KeyPair;
import java.util.List;

@Log4j2
public class OuranosProxySession {
    public int lastFormId = -1;
    public long uniqueEntityId;
    public long runtimeEntityId;
    public MovementData movement = new MovementData();
    public InventoryData inventory = new InventoryData();
    @Getter
    private int targetProtocolId, upstreamProtocolId;

    public List<BedrockPacket> tickMovement() {
        return List.of(this.movement.tick(this));
    }

    public OuranosProxySession(KeyPair keyPair, ProxyClientSession upstreamSession, ProxyServerSession downstreamSession) {
        this.keyPair = keyPair;
        this.upstream = upstreamSession;
        this.downstream = downstreamSession;
        OuranosProxySession.ouranosPlayers.add(this);
        this.downstream.addDisconnectListener(this::disconnect);
        this.upstream.addDisconnectListener(this::disconnect);
        this.targetProtocolId = this.downstream.getCodec().getProtocolVersion();
        this.upstreamProtocolId = this.upstream.getCodec().getProtocolVersion();
    }

}
