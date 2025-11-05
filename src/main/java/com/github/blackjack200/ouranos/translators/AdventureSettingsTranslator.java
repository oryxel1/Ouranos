package com.github.blackjack200.ouranos.translators;

import com.github.blackjack200.ouranos.session.OuranosSession;
import com.github.blackjack200.ouranos.session.translator.BaseTranslator;
import org.cloudburstmc.protocol.bedrock.codec.v527.Bedrock_v527;
import org.cloudburstmc.protocol.bedrock.codec.v534.Bedrock_v534;
import org.cloudburstmc.protocol.bedrock.codec.v554.Bedrock_v554;
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer;
import org.cloudburstmc.protocol.bedrock.data.AdventureSetting;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

@SuppressWarnings("ALL")
public class AdventureSettingsTranslator implements BaseTranslator {
    @Override
    public BedrockPacket translateServerbound(OuranosSession session, BedrockPacket bedrockPacket) {
        if (bedrockPacket instanceof RequestAbilityPacket packet) {
            return rewriteFlying(session, packet.getAbility() == Ability.FLYING);
        }
        if (bedrockPacket instanceof AdventureSettingsPacket packet) {
            return rewriteFlying(session, packet.getSettings().contains(AdventureSetting.FLYING));
        }

        final int output = session.getTargetVersion();
        if (output < Bedrock_v618.CODEC.getProtocolVersion() && bedrockPacket instanceof PlayerActionPacket packet) {
            if (packet.getAction() == PlayerActionType.START_FLYING) {
                rewriteFlying(session, true);
            }
            if (packet.getAction() == PlayerActionType.STOP_FLYING) {
                rewriteFlying(session, false);
            }
        }

        return translateBothWay(session, bedrockPacket);
    }

    @Override
    public BedrockPacket translateClientbound(OuranosSession session, BedrockPacket bedrockPacket) {
        final int output = session.getTargetVersion();
        if (output > Bedrock_v554.CODEC.getProtocolVersion() && bedrockPacket instanceof AdventureSettingsPacket packet) {
            var newPk = new UpdateAbilitiesPacket();
            newPk.setUniqueEntityId(packet.getUniqueEntityId());
            newPk.setPlayerPermission(packet.getPlayerPermission());
            newPk.setCommandPermission(packet.getCommandPermission());
            var layer = new AbilityLayer();
            layer.setLayerType(AbilityLayer.Type.BASE);
            layer.setFlySpeed(0.05f);
            layer.setWalkSpeed(0.1f);

            var settings = packet.getSettings();
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
            session.sendUpstreamPacket(newPk);

            var newPk2 = new UpdateAdventureSettingsPacket();
            newPk2.setAutoJump(settings.contains(AdventureSetting.AUTO_JUMP));
            newPk2.setImmutableWorld(settings.contains(AdventureSetting.WORLD_IMMUTABLE));
            newPk2.setNoMvP(settings.contains(AdventureSetting.NO_MVP));
            newPk2.setNoPvM(settings.contains(AdventureSetting.NO_PVM));
            newPk2.setShowNameTags(settings.contains(AdventureSetting.SHOW_NAME_TAGS));
            session.sendUpstreamPacket(newPk2);
            return null;
        }

        return translateBothWay(session, bedrockPacket);
    }

    private BedrockPacket translateBothWay(OuranosSession session, final BedrockPacket bedrockPacket) {
        final int output = session.getTargetVersion();
        if (output < Bedrock_v534.CODEC.getProtocolVersion() && bedrockPacket instanceof UpdateAbilitiesPacket packet) {
            final AdventureSettingsPacket adventurePacket = new AdventureSettingsPacket();
            adventurePacket.setUniqueEntityId(packet.getUniqueEntityId());
            adventurePacket.setCommandPermission(packet.getCommandPermission());
            adventurePacket.setPlayerPermission(packet.getPlayerPermission());
            adventurePacket.getSettings().clear();
            var abilities = packet.getAbilityLayers().get(0).getAbilityValues();
            BiFunction<Ability, AdventureSetting, Void> f = (Ability b1, AdventureSetting b) -> {
                if (abilities.contains(b1)) {
                    adventurePacket.getSettings().add(b);
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
                adventurePacket.getSettings().add(AdventureSetting.WORLD_IMMUTABLE);
                adventurePacket.getSettings().remove(AdventureSetting.BUILD);
                adventurePacket.getSettings().remove(AdventureSetting.MINE);
            }
            return adventurePacket;
        }

        return bedrockPacket;
    }

    private static BedrockPacket rewriteFlying(final OuranosSession session, boolean flying) {
        if (session.getTargetVersion() < Bedrock_v527.CODEC.getProtocolVersion()) {
            final AdventureSettingsPacket packet = new AdventureSettingsPacket();
            packet.setUniqueEntityId(session.getUniqueId());
            if (flying) {
                packet.getSettings().add(AdventureSetting.FLYING);
            }
            return packet;
        }

        final RequestAbilityPacket packet = new RequestAbilityPacket();
        packet.setAbility(Ability.FLYING);
        packet.setType(Ability.Type.BOOLEAN);
        packet.setBoolValue(flying);
        return packet;
    }
}
