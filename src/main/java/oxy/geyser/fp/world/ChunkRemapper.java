package oxy.geyser.fp.world;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import oxy.geyser.fp.session.GeyserFPUser;

import java.util.ArrayList;
import java.util.List;

public final class ChunkRemapper {
    private ChunkRemapper() {
    }

    public static ClientboundLevelChunkWithLightPacket remapLevelChunk(GeyserFPUser user, ClientboundLevelChunkWithLightPacket packet) {
        user.worldHeight().storeOnce(
                user.chunkCache().getChunkHeightY(),
                user.chunkCache().getMinY()
        );

        final int javaChunkSize = user.worldHeight().chunkHeightY();
        final int worldMinY = user.worldHeight().minY();
        final ChunkSection[] sections = readSections(packet.getChunkData(), javaChunkSize, user.session());
        user.chunkCache().addToCache(packet, sections);

        int x = (packet.getX() << 4) - user.offset().getX();
        int z = (packet.getZ() << 4) - user.offset().getZ();

        byte[] chunkData = packet.getChunkData();
        BlockEntityInfo[] blockEntities = packet.getBlockEntities();

        if (javaChunkSize > VerticalWindow.BEDROCK_SECTION_COUNT || user.offset().getY() != 0) {
            user.geyserChunkCache().saveOnce(
                    user.session().getChunkCache().getChunkMinY() << 4,
                    user.session().getChunkCache().getChunkHeightY() << 4
            );

            user.session().getChunkCache().setMinY(VerticalWindow.BEDROCK_MIN_Y);
            user.session().getChunkCache().setHeightY(VerticalWindow.BEDROCK_HEIGHT);

            ByteBuf byteBuf = Unpooled.buffer();
            try {
                writeVisibleSections(byteBuf, sections, worldMinY, user.offset().getY(), user.session());
                chunkData = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(chunkData);
            } finally {
                byteBuf.release();
            }

            blockEntities = remapBlockEntities(packet.getBlockEntities(), user.offset().getY());
        }

        return new ClientboundLevelChunkWithLightPacket(
                x >> 4, z >> 4, chunkData, packet.getHeightMaps(), blockEntities, packet.getLightData());
    }

    private static ChunkSection[] readSections(byte[] chunkData, int sectionCount, GeyserSession session) {
        final ChunkSection[] sections = new ChunkSection[sectionCount];
        final ByteBuf byteBuf = Unpooled.wrappedBuffer(chunkData);
        try {
            for (int sectionY = 0; sectionY < sectionCount; sectionY++) {
                sections[sectionY] = MinecraftTypes.readChunkSection(byteBuf, BlockRegistries.BLOCK_STATES.get().size(),
                        session.getRegistryCache().registry(JavaRegistries.BIOME).size());
            }
        } finally {
            byteBuf.release();
        }
        return sections;
    }

    public static void writeVisibleSections(ByteBuf byteBuf, ChunkSection[] sections, int worldMinY, int offsetY, GeyserSession session) {
        int sourceStartSection = VerticalWindow.sourceStartSection(worldMinY, offsetY);

        final int blockStateCount = BlockRegistries.BLOCK_STATES.get().size();
        final JavaRegistry<Integer> biomeRegistry = session.getRegistryCache().registry(JavaRegistries.BIOME);
        final int biomeId = biomeRegistry.values().getFirst();
        final int biomeCount = biomeRegistry.size();

        for (int i = 0; i < VerticalWindow.BEDROCK_SECTION_COUNT; i++) {
            int sourceIndex = sourceStartSection + i;
            ChunkSection sourceSection = sourceIndex >= 0 && sourceIndex < sections.length ? sections[sourceIndex] : null;
            if (sourceSection != null && !sourceSection.isBlockCountEmpty()) {
                MinecraftTypes.writeChunkSection(byteBuf, sourceSection);
            } else {
                MinecraftTypes.writeChunkSection(byteBuf, forcedNetworkAirSection(
                        sourceSection, blockStateCount, biomeId, biomeCount));
            }
        }
    }

    public static BlockEntityInfo[] remapBlockEntities(BlockEntityInfo[] blockEntities, int offsetY) {
        List<BlockEntityInfo> remapped = new ArrayList<>();
        for (BlockEntityInfo blockEntity : blockEntities) {
            int fakeY = blockEntity.getY() - offsetY;
            if (fakeY < VerticalWindow.BEDROCK_MIN_Y || fakeY >= VerticalWindow.BEDROCK_MAX_Y_EXCLUSIVE) {
                continue;
            }

            NbtMap nbt = blockEntity.getNbt();
            if (nbt != null && nbt.containsKey("y")) {
                nbt = nbt.toBuilder().putInt("y", fakeY).build();
            }

            remapped.add(new BlockEntityInfo(
                    blockEntity.getX(), fakeY, blockEntity.getZ(), blockEntity.getType(), nbt));
        }
        return remapped.toArray(new BlockEntityInfo[0]);
    }

    private static ChunkSection forcedNetworkAirSection(ChunkSection sourceSection, int blockStateCount, int biomeId, int biomeCount) {
        return new ChunkSection(
                1,
                0,
                DataPalette.createForBlockState(Block.JAVA_AIR_ID, blockStateCount),
                sourceSection != null ? new DataPalette(sourceSection.getBiomeData()) : DataPalette.createForBiome(biomeId, biomeCount));
    }
}