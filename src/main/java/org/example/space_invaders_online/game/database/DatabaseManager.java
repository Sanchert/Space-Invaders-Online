package org.example.space_invaders_online.game.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final SessionFactory sessionFactory;

    private DatabaseManager() {
        try {
            Configuration configuration = new Configuration();

            // Настройки H2 (можно заменить на PostgreSQL)
            configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            configuration.setProperty("hibernate.connection.url", "jdbc:h2:./space_invaders_db;DB_CLOSE_DELAY=-1");
            configuration.setProperty("hibernate.connection.username", "sa");
            configuration.setProperty("hibernate.connection.password", "");
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            configuration.setProperty("hibernate.hbm2ddl.auto", "update");
            configuration.setProperty("hibernate.show_sql", "true");
            configuration.setProperty("hibernate.format_sql", "true");

            configuration.addAnnotatedClass(PlayerStats.class);

            sessionFactory = configuration.buildSessionFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public PlayerStats getOrCreatePlayer(String playerName) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            Query<PlayerStats> query = session.createQuery(
                    "FROM PlayerStats WHERE playerName = :name", PlayerStats.class);
            query.setParameter("name", playerName);
            PlayerStats stats = query.uniqueResult();

            if (stats == null) {
                stats = new PlayerStats(playerName);
                session.persist(stats);
            }

            session.getTransaction().commit();
            return stats;
        }
    }

    public void updatePlayerStats(PlayerStats stats) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(stats);
            session.getTransaction().commit();
        }
    }

    public void recordWin(String playerName) {
        PlayerStats stats = getOrCreatePlayer(playerName);
        stats.addWin();
        updatePlayerStats(stats);
    }

    public void recordShot(String playerName, boolean hit) {
        PlayerStats stats = getOrCreatePlayer(playerName);
        stats.addShot();
        if (hit) {
            stats.addHit();
        }
        updatePlayerStats(stats);
    }

    public List<PlayerStats> getLeaderboard() {
        try (Session session = sessionFactory.openSession()) {
            Query<PlayerStats> query = session.createQuery(
                    "FROM PlayerStats ORDER BY wins DESC, getAccuracy() DESC", PlayerStats.class);
            return query.setMaxResults(10).list();
        }
    }

    public void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}