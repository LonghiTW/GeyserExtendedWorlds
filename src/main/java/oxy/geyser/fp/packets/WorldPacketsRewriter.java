package oxy.geyser.fp.packets;

import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundSetBorderCenterPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.world.ChunkRemapper;
import oxy.geyser.fp.world.CoordinateRemapper;
import oxy.geyser.fp.world.VerticalWindow;

import java.util.ArrayList;
import java.util.List;

public class WorldPacketsRewriter implements JavaPacketListener {
    @Override
    public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        // Restore Geyser's original ChunkCache values before dimension switches,
        // so loadDimension can initialize the new world with the correct values.
        if (event.getPacket() instanceof ClientboundRespawnPacket
                || event.getPacket() instanceof ClientboundLoginPacket) {
            user.geyserChunkCache().restore(user.session());
            user.resetWorldHeight();
        }

        if (event.getPacket() instanceof ClientboundSetBorderCenterPacket packet) {
            event.setPacket(new ClientboundSetBorderCenterPacket(
                    packet.getNewCenterX() - user.offset().getX(),
                    packet.getNewCenterZ() - user.offset().getZ()));
        }

        if (event.getPacket() instanceof ClientboundInitializeBorderPacket packet) {
            event.setPacket(new ClientboundInitializeBorderPacket(
                    packet.getNewCenterX() - user.offset().getX(), packet.getNewCenterZ() - user.offset().getZ(),
                    packet.getOldSize(),
                    packet.getNewSize(),
                    packet.getLerpTime(),
                    packet.getNewAbsoluteMaxSize(),
                    packet.getWarningBlocks(),
                    packet.getWarningTime()
            ));
        }

        if (event.getPacket() instanceof ClientboundBlockEntityDataPacket packet) {
            event.setPacket(new ClientboundBlockEntityDataPacket(
                    CoordinateRemapper.toFakeBlockPosition(user, packet.getPosition()), packet.getType(), packet.getNbt()));
        }

        if (event.getPacket() instanceof ClientboundBlockEventPacket packet) {
            event.setPacket(new ClientboundBlockEventPacket(
                    CoordinateRemapper.toFakeBlockPosition(user, packet.getPosition()),
                    packet.getRawType(), packet.getRawValue(), packet.getType(), packet.getValue(), packet.getBlockId()));
        }

        if (event.getPacket() instanceof ClientboundSetChunkCacheCenterPacket packet) {
            int x = (packet.getChunkX() << 4) - user.offset().getX();
            int z = (packet.getChunkZ() << 4) - user.offset().getZ();
            event.setPacket(new ClientboundSetChunkCacheCenterPacket(x >> 4, z >> 4));
        }

        if (event.getPacket() instanceof ClientboundForgetLevelChunkPacket packet) {
            user.chunkCache().remove(packet.getX(), packet.getZ());

            int x = (packet.getX() << 4) - user.offset().getX();
            int z = (packet.getZ() << 4) - user.offset().getZ();
            event.setPacket(new ClientboundForgetLevelChunkPacket(x >> 4, z >> 4));
        }

        if (event.getPacket() instanceof ClientboundLevelChunkWithLightPacket packet) {
            event.setPacket(ChunkRemapper.remapLevelChunk(user, packet));
        }

        if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet) {
            user.chunkCache().updateBlock(packet.getEntry().getPosition(), packet.getEntry().getBlock());
            event.setPacket(new ClientboundBlockUpdatePacket(new BlockChangeEntry(
                    CoordinateRemapper.toFakeBlockPosition(user, packet.getEntry().getPosition()),
                    packet.getEntry().getBlock())));
        }

        if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket packet) {
            user.chunkCache().updateBlockSections(packet);

            final List<BlockChangeEntry> entries = new ArrayList<>();
            for (BlockChangeEntry entry : packet.getEntries()) {
                user.chunkCache().updateBlock(entry.getPosition(), entry.getBlock());
                entries.add(new BlockChangeEntry(CoordinateRemapper.toFakeBlockPosition(user, entry.getPosition()), entry.getBlock()));
            }

            int x = (packet.getChunkX() << 4) - user.offset().getX();
            int z = (packet.getChunkZ() << 4) - user.offset().getZ();
            int newChunkY = packet.getChunkY() - (user.offset().getY() >> 4);

            // Only forward section updates inside Bedrock's valid section range (0..23).
            if (newChunkY >= 0 && newChunkY < VerticalWindow.BEDROCK_SECTION_COUNT) {
                event.setPacket(new ClientboundSectionBlocksUpdatePacket(x >> 4, newChunkY, z >> 4, entries.toArray(new BlockChangeEntry[0])));
            } else {
                event.cancel();
            }
        }
    }
}
