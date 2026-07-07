package oxy.geyser.fp.world;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import oxy.geyser.fp.GeyserFloatingPoints;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.util.MathUtil;

public final class CoordinateRemapper {
    private CoordinateRemapper() {
    }

    public static boolean shouldReOffsetFakePosition(GeyserFPUser user, double fakeX, double fakeY, double fakeZ) {
        double realY = fakeY + user.offset().getY();
        return Math.abs(fakeX) >= GeyserFloatingPoints.config().maxPosition()
                || Math.abs(fakeZ) >= GeyserFloatingPoints.config().maxPosition()
                || shouldReOffsetY(user, fakeY, realY);
    }

    public static boolean shouldReOffsetRealPosition(GeyserFPUser user, double realX, double realY, double realZ) {
        double fakeY = realY - user.offset().getY();
        return Math.abs(realX - user.offset().getX()) > GeyserFloatingPoints.config().maxPosition()
                || Math.abs(realZ - user.offset().getZ()) > GeyserFloatingPoints.config().maxPosition()
                || shouldReOffsetY(user, fakeY, realY);
    }

    public static Vector3d toRealPosition(GeyserFPUser user, double fakeX, double fakeY, double fakeZ) {
        return Vector3d.from(
                fakeX + user.offset().getX(),
                fakeY + user.offset().getY(),
                fakeZ + user.offset().getZ());
    }

    public static Vector3d toFakePosition(GeyserFPUser user, Vector3d realPosition) {
        return realPosition.sub(user.offset().toDouble());
    }

    public static Vector3d toFakePosition(GeyserFPUser user, double realX, double realY, double realZ) {
        return Vector3d.from(realX, realY, realZ).sub(user.offset().toDouble());
    }

    public static Vector3i toRealBlockPosition(GeyserFPUser user, Vector3i fakePosition) {
        return fakePosition.add(user.offset());
    }

    public static Vector3i toFakeBlockPosition(GeyserFPUser user, Vector3i realPosition) {
        return realPosition.sub(user.offset());
    }

    public static Vector3i calculateOffsetForFakePosition(GeyserFPUser user, double fakeX, double fakeY, double fakeZ) {
        return MathUtil.calculateOffset(toRealPosition(user, fakeX, fakeY, fakeZ), VerticalWindow.TARGET_FAKE_Y);
    }

    public static Vector3i calculateOffsetForRealPosition(GeyserFPUser user, double realX, double realY, double realZ) {
        return MathUtil.calculateOffset(Vector3d.from(realX, realY, realZ), VerticalWindow.TARGET_FAKE_Y);
    }

    private static boolean shouldReOffsetY(GeyserFPUser user, double fakeY, double realY) {
        if (VerticalWindow.isNativeBedrockY(realY)) {
            return user.offset().getY() != 0;
        }

        return user.offset().getY() == 0
                || fakeY <= VerticalWindow.reOffsetLowerBound()
                || fakeY >= VerticalWindow.reOffsetUpperBound();
    }
}