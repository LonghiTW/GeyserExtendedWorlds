package oxy.geyser.fp.session;

import org.geysermc.geyser.session.GeyserSession;

public class GeyserChunkCacheState {
    private int originalMinY = Integer.MIN_VALUE;
    private int originalHeightY = 0;

    public boolean hasOriginalValues() {
        return this.originalMinY != Integer.MIN_VALUE;
    }

    public void saveOnce(int minY, int heightY) {
        if (this.originalMinY != Integer.MIN_VALUE) {
            return;
        }

        this.originalMinY = minY;
        this.originalHeightY = heightY;
    }

    public void restore(GeyserSession session) {
        if (!this.hasOriginalValues()) {
            return;
        }

        session.getChunkCache().setMinY(this.originalMinY);
        session.getChunkCache().setHeightY(this.originalHeightY);
    }
}