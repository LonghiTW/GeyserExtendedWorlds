package oxy.geyser.fp.session;

public class JavaWorldHeightState {
    private int chunkHeightY = -1;
    private int minY = 0;
    private boolean extendedHeight = false;

    public int chunkHeightY() {
        return this.chunkHeightY;
    }

    public int minY() {
        return this.minY;
    }

    public boolean isExtendedHeight() {
        return this.extendedHeight;
    }

    public void storeOnce(int chunkHeightY, int minY) {
        if (this.chunkHeightY != -1) {
            return;
        }

        this.chunkHeightY = chunkHeightY;
        this.minY = minY;
        this.extendedHeight = chunkHeightY > 24;
    }

    public void reset() {
        this.chunkHeightY = -1;
        this.minY = 0;
        this.extendedHeight = false;
    }
}