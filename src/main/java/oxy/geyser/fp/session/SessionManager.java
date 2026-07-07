package oxy.geyser.fp.session;

import org.geysermc.geyser.session.GeyserSession;
import oxy.geyser.fp.util.GeyserUtil;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private final Map<GeyserSession, GeyserFPUser> users = new HashMap<>();

    public void create(GeyserSession session) {
        GeyserFPUser user = new GeyserFPUser(session);
        GeyserUtil.wrapAroundUpstreamHandler(user);
        this.users.put(session, user);
    }

    public GeyserFPUser get(GeyserSession session) {
        return this.users.get(session);
    }

    public void remove(GeyserSession session) {
        this.users.remove(session);
    }
}