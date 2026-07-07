package oxy.geyser.fp.util.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Config(@JsonProperty("show-position-by-default")
                     boolean showPositionByDefault, @JsonProperty("capped-value") int maxPosition,
                     @JsonProperty("capped-height") Integer maxHeight,
                     @JsonProperty("vertical-remap-threshold") Integer verticalRemapThreshold) {
    public int effectiveMaxHeight() {
        return maxHeight != null ? maxHeight : 256;
    }

    public int effectiveVerticalRemapThreshold() {
        int threshold = verticalRemapThreshold != null ? verticalRemapThreshold : 96;
        return Math.max(1, Math.min(192, threshold));
    }
}
