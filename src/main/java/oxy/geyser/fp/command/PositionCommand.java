package oxy.geyser.fp.command;

import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.session.GeyserSession;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.session.SessionManager;

public class PositionCommand {
    private final Extension extension;
    private final SessionManager sessions;

    public PositionCommand(Extension extension, SessionManager sessions) {
        this.extension = extension;
        this.sessions = sessions;
    }

    public void register(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this.extension).source(CommandSource.class)
                .name("position")
                .playerOnly(true).bedrockOnly(true).permission("geyserfloatingpoints.position", TriState.TRUE)
                .description("Toggle off/on title to show your real position, won't show anything if your current position is in fact real.")
                .executor((source, cmd, args) -> {
                    if (source.connection() instanceof GeyserSession session) {
                        GeyserFPUser user = this.sessions.get(session);
                        if (user == null) {
                            return;
                        }
                        user.setPositionShown(!user.shouldShowPosition());
                        if (!user.shouldShowPosition()) {
                            source.sendMessage("Stop showing your current position.");
                        } else {
                            source.sendMessage("You should now be able to see your current position.");
                        }
                    }
                })
                .build());
    }
}