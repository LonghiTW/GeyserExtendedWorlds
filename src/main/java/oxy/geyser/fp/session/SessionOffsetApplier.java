package oxy.geyser.fp.session;

import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityCache;
import org.geysermc.geyser.session.cache.TeleportCache;
import org.geysermc.geyser.session.cache.WorldBorder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

public class SessionOffsetApplier {
    public void apply(GeyserSession session, Vector3i oldOffset, Vector3i newOffset, boolean teleport) {
        final SessionPlayerEntity entity = session.getPlayerEntity();

        entity.setPosition(entity.position().add(oldOffset.toFloat()).sub(newOffset.toFloat()));

        if (teleport) {
            entity.moveAbsolute(
                    entity.position(),
                    entity.getYaw(), entity.getPitch(), entity.isOnGround(), true
            );

            session.setUnconfirmedTeleport(new TeleportCache(session, entity.position(), entity.getPitch(), entity.getYaw(), 0));
        }

        this.applyWorldBorderOffset(session, oldOffset, newOffset);
        this.applyEntityOffsets(session, entity, oldOffset, newOffset);
    }

    private void applyWorldBorderOffset(GeyserSession session, Vector3i oldOffset, Vector3i newOffset) {
        try {
            Field field = WorldBorder.class.getDeclaredField("center");
            field.setAccessible(true);
            final Vector2d center = (Vector2d) field.get(session.getWorldBorder());

            final WorldBorder border = session.getWorldBorder();
            border.setCenter(center.add(oldOffset.getX(), oldOffset.getZ()).sub(newOffset.getX(), newOffset.getZ()));

            border.update();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private void applyEntityOffsets(GeyserSession session, SessionPlayerEntity player, Vector3i oldOffset, Vector3i newOffset) {
        try {
            final Field entitiesField = EntityCache.class.getDeclaredField("entities");
            entitiesField.setAccessible(true);
            final Object entities = entitiesField.get(session.getEntityCache());

            final Method valuesMethod = entities.getClass().getDeclaredMethod("values");
            valuesMethod.setAccessible(true);
            final Collection<Entity> values = (Collection<Entity>) valuesMethod.invoke(entities);

            for (Entity entity : values) {
                if (entity == player) {
                    continue;
                }
                entity.setPosition(entity.position().add(oldOffset.toFloat()).sub(newOffset.toFloat()));
                entity.despawnEntity();
                entity.spawnEntity();
            }
        } catch (Exception ignored) {
        }
    }
}