package oxy.geyser.fp.session;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import oxy.geyser.fp.GeyserFloatingPoints;
import oxy.geyser.fp.session.cache.ChunkCache;
import oxy.geyser.fp.world.VerticalWindow;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class GeyserFPUser {
    public static final String SHOW_COORDINATES_GAME_RULE = "showcoordinates";

    private final GeyserSession session;
    public GeyserSession session() {
        return session;
    }

    @Setter
    private boolean positionShown = GeyserFloatingPoints.config().showPositionByDefault();
    public boolean shouldShowPosition() {
        return this.positionShown;
    }

    private final ChunkCache chunkCache = new ChunkCache(this);
    public ChunkCache chunkCache() {
        return this.chunkCache;
    }

    private final SessionOffsetApplier offsetApplier = new SessionOffsetApplier();

    private final JavaWorldHeightState worldHeight = new JavaWorldHeightState();
    private final GeyserChunkCacheState geyserChunkCache = new GeyserChunkCacheState();
    public JavaWorldHeightState worldHeight() {
        return this.worldHeight;
    }

    public GeyserChunkCacheState geyserChunkCache() {
        return this.geyserChunkCache;
    }

    public boolean shouldCorrectPredictedBlockBreaks() {
        return this.worldHeight.isExtendedHeight()
                || this.worldHeight.chunkHeightY() > 24
                || this.session.getChunkCache().getChunkHeightY() > 24
                || !this.offset.equals(Vector3i.ZERO);
    }

    public void resetWorldHeight() {
        this.worldHeight.reset();
        this.offset = Vector3i.ZERO;
        this.prevPosition = Vector3i.ZERO;
    }

    private Vector3i offset = Vector3i.from(0, 0 ,0);
    public Vector3i offset() {
        return offset;
    }

    public Vector3i prevPosition = Vector3i.ZERO;

    public void offset(Vector3i offset, boolean teleport) {
        Vector3i oldOffset = this.offset;
        boolean wasHidingCoordinates = !this.offset.equals(Vector3i.ZERO);
        this.offsetApplier.apply(this.session, oldOffset, offset, teleport);
        if (!oldOffset.equals(offset)) {
            this.remapSkulls(oldOffset, offset);
        }
        this.offset = offset;
        chunkCache.sendChunksWithOffset();

        boolean shouldHideCoordinates = !this.offset.equals(Vector3i.ZERO);
        if (wasHidingCoordinates != shouldHideCoordinates) {
            session.sendGameRule(SHOW_COORDINATES_GAME_RULE, shouldShowCoordinates());
        }
    }

    private void remapSkulls(Vector3i oldOffset, Vector3i newOffset) {
        SkullCache skullCache = this.session.getSkullCache();
        List<SkullCache.Skull> skulls = new ArrayList<>(skullCache.getSkulls().values());

        for (SkullCache.Skull skull : skulls) {
            skullCache.removeSkull(skull.getPosition());
        }

        for (SkullCache.Skull skull : skulls) {
            Vector3i oldPosition = skull.getPosition();
            Vector3i newPosition = Vector3i.from(
                    oldPosition.getX() + oldOffset.getX() - newOffset.getX(),
                    oldPosition.getY() + oldOffset.getY() - newOffset.getY(),
                    oldPosition.getZ() + oldOffset.getZ() - newOffset.getZ());
            if (newPosition.getY() < VerticalWindow.BEDROCK_MIN_Y
                    || newPosition.getY() >= VerticalWindow.BEDROCK_MAX_Y_EXCLUSIVE) {
                continue;
            }

            skullCache.putSkull(newPosition, skull.getUuid(), skull.getSkinUrl(),
                    skull.getSkinHash(), skull.getBlockState());
        }
    }

    private boolean shouldShowCoordinates() {
        return this.offset.equals(Vector3i.ZERO)
                // From https://github.com/GeyserMC/Geyser/blob/fc2681ada4e0b5e344d64927f978ec7ac751fea5/core/src/main/java/org/geysermc/geyser/session/cache/PreferencesCache.java#L78
                && !session.isReducedDebugInfo()
                && session.getGeyser().config().gameplay().showCoordinates()
                && session.getPreferencesCache().isPrefersShowCoordinates();
    }
}
