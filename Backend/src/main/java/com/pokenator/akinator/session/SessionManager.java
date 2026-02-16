package com.pokenator.akinator.session;

import com.pokenator.akinator.model.GameState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<String, GameState> sessions = new ConcurrentHashMap<>();

    public GameState create() {
        String id = UUID.randomUUID().toString();
        GameState state = new GameState(id);
        sessions.put(id, state);
        return state;
    }

    public GameState get(String sessionId) {
        GameState state = sessions.get(sessionId);
        if (state == null) throw new IllegalArgumentException("Invalid session: " + sessionId);
        return state;
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
