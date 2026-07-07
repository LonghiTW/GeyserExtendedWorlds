package oxy.geyser.fp.packets;

import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundPickItemFromBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetJigsawBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetStructureBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundBlockEntityTagQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundJigsawGeneratePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundSignUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;

import java.util.concurrent.TimeUnit;

public class ActionPacketsRewriter implements JavaPacketListener {
    @Override
    public void onSend(GeyserFPUser user, JavaPacketEvent event) {
        if (user.shouldCorrectPredictedBlockBreaks() && event.getPacket() instanceof ServerboundPlayerActionPacket packet && isDiggingAction(packet)) {
            restorePredictedBreak(user, packet.getPosition());
        }

        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet) {
            event.setPacket(new ServerboundPlayerActionPacket(packet.getAction(), packet.getPosition().add(user.offset()), packet.getFace(), packet.getSequence()));
        }

        if (event.getPacket() instanceof ServerboundUseItemOnPacket packet) {
            event.setPacket(new ServerboundUseItemOnPacket(
                    packet.getPosition().add(user.offset()), packet.getFace(), packet.getHand(), packet.getCursorX(),
                    packet.getCursorY(), packet.getCursorZ(), packet.isInsideBlock(), packet.isHitWorldBorder(), packet.getSequence()
            ));
        }

        if (event.getPacket() instanceof ServerboundBlockEntityTagQueryPacket packet) {
            event.setPacket(new ServerboundBlockEntityTagQueryPacket(packet.getTransactionId(), packet.getPosition().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundJigsawGeneratePacket packet) {
            event.setPacket(new ServerboundJigsawGeneratePacket(packet.getPosition().add(user.offset()), packet.getLevels(), packet.isKeepJigsaws()));
        }

        if (event.getPacket() instanceof ServerboundPickItemFromBlockPacket packet) {
            event.setPacket(new ServerboundPickItemFromBlockPacket(packet.getPos().add(user.offset()), packet.isIncludeData()));
        }

        if (event.getPacket() instanceof ServerboundSetCommandBlockPacket packet) {
            event.setPacket(new ServerboundSetCommandBlockPacket(packet.getPosition().add(user.offset()), packet.getCommand(),
                    packet.getMode(), packet.isDoesTrackOutput(), packet.isConditional(), packet.isAutomatic()));
        }

        if (event.getPacket() instanceof ServerboundSetJigsawBlockPacket packet) {
            event.setPacket(new ServerboundSetJigsawBlockPacket(
                    packet.getPosition().add(user.offset()), packet.getName(), packet.getTarget(), packet.getPool(),
                    packet.getFinalState(), packet.getJointType(), packet.getSelectionPriority(), packet.getPlacementPriority()));
        }

        if (event.getPacket() instanceof ServerboundSetStructureBlockPacket packet) {
            event.setPacket(new ServerboundSetStructureBlockPacket(
                    packet.getPosition().add(user.offset()), packet.getAction(), packet.getMode(),
                    packet.getName(), packet.getOffset(), packet.getSize(), packet.getMirror(),
                    packet.getRotation(), packet.getMetadata(), packet.getIntegrity(), packet.getSeed(),
                    packet.isIgnoreEntities(), packet.isShowAir(), packet.isShowBoundingBox(), packet.isStrict()
            ));
        }

        if (event.getPacket() instanceof ServerboundSignUpdatePacket packet) {
            event.setPacket(new ServerboundSignUpdatePacket(packet.getPosition().add(user.offset()), packet.getLines(), packet.isFrontText()));
        }
    }

    private boolean isDiggingAction(ServerboundPlayerActionPacket packet) {
        return switch (packet.getAction()) {
            case START_DIGGING, CANCEL_DIGGING, FINISH_DIGGING -> true;
            default -> false;
        };
    }

    private void restorePredictedBreak(GeyserFPUser user, Vector3i position) {
        user.session().getBlockBreakHandler().reset();
        user.session().scheduleInEventLoop(() -> sendBlockRestore(user, position), 50, TimeUnit.MILLISECONDS);
        user.session().scheduleInEventLoop(() -> sendBlockRestore(user, position), 150, TimeUnit.MILLISECONDS);
        user.session().scheduleInEventLoop(() -> sendBlockRestore(user, position), 300, TimeUnit.MILLISECONDS);
    }

    private void sendBlockRestore(GeyserFPUser user, Vector3i position) {
        BlockState state = BlockState.of(user.chunkCache().getBlock(position.add(user.offset())));
        BlockUtils.sendBedrockStopBlockBreak(user.session(), position.toFloat());
        BlockUtils.restoreCorrectBlock(user.session(), position, state);
    }

    @Override
    public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet) {
            event.setPacket(new ClientboundBlockDestructionPacket(packet.getBreakerEntityId(), packet.getPosition().sub(user.offset()), packet.getStage()));
        }
    }
}
