package oxy.geyser.fp.packets;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.world.CoordinateRemapper;

public class PositionPacketsRewriter implements JavaPacketListener {
    @Override
    public void onSend(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPosPacket packet) {
            if (checkForReOffset(user, packet.getX(), packet.getY(), packet.getZ())) {
                event.cancel();
                return;
            }

            Vector3d vector3d = CoordinateRemapper.toRealPosition(user, packet.getX(), packet.getY(), packet.getZ());

            event.setPacket(new ServerboundMovePlayerPosPacket(
                    packet.isOnGround(), packet.isHorizontalCollision(),
                    vector3d.getX(), vector3d.getY(), vector3d.getZ()
            ));
        }

        if (event.getPacket() instanceof ServerboundMovePlayerPosRotPacket packet) {
            if (checkForReOffset(user, packet.getX(), packet.getY(), packet.getZ())) {
                event.cancel();
                return;
            }

            Vector3d vector3d = CoordinateRemapper.toRealPosition(user, packet.getX(), packet.getY(), packet.getZ());

            event.setPacket(new ServerboundMovePlayerPosRotPacket(
                    packet.isOnGround(), packet.isHorizontalCollision(),
                    vector3d.getX(),
                    vector3d.getY(),
                    vector3d.getZ(), packet.getYaw(), packet.getPitch()
            ));
        }

        if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet) {
            Vector3d position = packet.getPosition();
            if (checkForReOffset(user, position.getX(), position.getY(), position.getZ())) {
                event.cancel();
                return;
            }

            event.setPacket(new ServerboundMoveVehiclePacket(
                    CoordinateRemapper.toRealPosition(user, position.getX(), position.getY(), position.getZ()),
                    packet.getYRot(), packet.getXRot(), packet.isOnGround()));
        }
    }

    @Override
    public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        final SessionPlayerEntity entity = user.session().getPlayerEntity();

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            Vector3f currentPosition = entity.getPosition().add(user.offset().toFloat());

            double x = packet.getPosition().getX() + (packet.getRelatives().contains(PositionElement.X) ? currentPosition.getX() : 0);
            double y = packet.getPosition().getY() + (packet.getRelatives().contains(PositionElement.Y) ? currentPosition.getY() : 0);
            double z = packet.getPosition().getZ() + (packet.getRelatives().contains(PositionElement.Z) ? currentPosition.getZ() : 0);

            if (CoordinateRemapper.shouldReOffsetRealPosition(user, x, y, z)) {
                user.offset(CoordinateRemapper.calculateOffsetForRealPosition(user, x, y, z), false);
            }

            // I don't want to deal with relatives.
            packet.getRelatives().remove(PositionElement.X);
            packet.getRelatives().remove(PositionElement.Y);
            packet.getRelatives().remove(PositionElement.Z);

            Vector3d fakePosition = CoordinateRemapper.toFakePosition(user, x, y, z);

            event.setPacket(new ClientboundPlayerPositionPacket(
                    packet.getId(),
                    fakePosition.getX(), fakePosition.getY(), fakePosition.getZ(),
                    packet.getDeltaMovement().getX(), packet.getDeltaMovement().getY(), packet.getDeltaMovement().getZ(),
                    packet.getYRot(), packet.getXRot(), packet.getRelatives().toArray(new PositionElement[0])
            ));
        }
    }

    private boolean checkForReOffset(GeyserFPUser user, double x, double y, double z) {
        Vector3i pos = Vector3i.from(x, y, z);
        if (pos.distance(user.prevPosition) > 0 && user.shouldShowPosition()) {
            SetTitlePacket titlePacket = new SetTitlePacket();
            titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
            titlePacket.setText("XYZ: " + CoordinateRemapper.toRealBlockPosition(user, pos));
            titlePacket.setXuid("");
            titlePacket.setPlatformOnlineId("");
            user.session().sendUpstreamPacket(titlePacket);
        }

        if (!CoordinateRemapper.shouldReOffsetFakePosition(user, x, y, z)) {
            user.prevPosition = pos;
            return false;
        }

        user.offset(CoordinateRemapper.calculateOffsetForFakePosition(user, x, y, z), true);
        user.prevPosition = pos;
        return true;
    }
}
