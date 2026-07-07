package oxy.geyser.fp;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.session.GeyserSession;
import oxy.geyser.fp.command.PositionCommand;
import oxy.geyser.fp.network.PacketListenerRegistry;
import oxy.geyser.fp.packets.ActionPacketsRewriter;
import oxy.geyser.fp.packets.EntityPacketsRewriter;
import oxy.geyser.fp.packets.PositionPacketsRewriter;
import oxy.geyser.fp.packets.WorldPacketsRewriter;
import oxy.geyser.fp.session.SessionManager;
import oxy.geyser.fp.util.GeyserUtil;
import oxy.geyser.fp.util.config.Config;
import oxy.geyser.fp.util.config.ConfigLoader;

public class GeyserFloatingPoints implements Extension {
    private static Config CONFIG;
    public static Config config() {
        return CONFIG;
    }

    private final SessionManager sessions = new SessionManager();

    @Subscribe
    public void onGeyserPostInitializeEvent(GeyserPostInitializeEvent event) {
        CONFIG = ConfigLoader.load(this, GeyserFloatingPoints.class, Config.class);

        PacketListenerRegistry.instance().register(new WorldPacketsRewriter());
        PacketListenerRegistry.instance().register(new PositionPacketsRewriter());
        PacketListenerRegistry.instance().register(new EntityPacketsRewriter());
        PacketListenerRegistry.instance().register(new ActionPacketsRewriter());

        // This will force Geyser into using the chunk cache for platform like Spigot so the world manager pick up the correct block.
        GeyserUtil.wrapAroundGeyserBoostrap();
    }

    @Subscribe
    public void onSessionJoin(SessionLoginEvent event) {
        this.sessions.create((GeyserSession) event.connection());
    }

    @Subscribe
    public void onSessionLeave(SessionDisconnectEvent event) {
        this.sessions.remove((GeyserSession) event.connection());
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        new PositionCommand(this, this.sessions).register(event);
    }
}
