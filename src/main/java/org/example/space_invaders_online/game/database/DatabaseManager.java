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

            // Настройки PostgreSQL
            configuration.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            configuration.setProperty("hibernate.connection.url", "jdbc:postgresql://localhost:5432/space_invaders_db");
            configuration.setProperty("hibernate.connection.username", "postgres");
            configuration.setProperty("hibernate.connection.password", "1234");
            configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            configuration.setProperty("hibernate.hbm2ddl.auto", "update");
            configuration.setProperty("hibernate.show_sql", "false");
            configuration.setProperty("hibernate.format_sql", "false");

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

    public List<PlayerStats> getLeaderboard() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "FROM PlayerStats ORDER BY wins DESC", PlayerStats.class)
                    .setMaxResults(10)
                    .list();
        }
    }

    public void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    public void recordShot(String playerName) {
        PlayerStats stats = getOrCreatePlayer(playerName);
        stats.addShot();
        updatePlayerStats(stats);
    }

    public void recordHit(String playerName) {
        PlayerStats stats = getOrCreatePlayer(playerName);
        stats.addShot();
        stats.addHit();
        updatePlayerStats(stats);
    }
}