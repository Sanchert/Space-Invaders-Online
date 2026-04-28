package org.example.space_invaders_online.game.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final PlayerStatsDAO dao;

    private DatabaseManager() {
        dao = new PlayerStatsDAO_HB(); // swap to _JDBC here if needed
    }

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public void recordWin(String playerName) {
        PlayerStats stats = dao.getOrCreate(playerName);
        stats.addWin();
        dao.update(stats);
    }

    public void recordShot(String playerName) {
        PlayerStats stats = dao.getOrCreate(playerName);
        stats.addShot();
        dao.update(stats);
    }

    public void recordHit(String playerName) {
        PlayerStats stats = dao.getOrCreate(playerName);
        stats.addHit();
        dao.update(stats);
    }

    public List<PlayerStats> getLeaderboard() {
        return dao.getLeaderboard();
    }

    public void shutdown() {
        HibernateUtil.shutdown();
    }

    public void getOrCreate(String playerName) {
        dao.getOrCreate(playerName);
    }
}