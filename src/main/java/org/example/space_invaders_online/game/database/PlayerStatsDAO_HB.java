package org.example.space_invaders_online.game.database;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class PlayerStatsDAO_HB implements PlayerStatsDAO {
    @Override
    public PlayerStats getOrCreate(String playerName) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().getCurrentSession();
            tx = session.beginTransaction();

            session.createNativeMutationQuery(
                            "INSERT INTO player_stats (playerName, totalhits, totalshots, wins) " +
                                    "VALUES (:name, 0, 0, 0) ON CONFLICT (playerName) DO NOTHING")
                    .setParameter("name", playerName)
                    .executeUpdate();

            PlayerStats stats = session.createQuery(
                            "FROM PlayerStats WHERE playerName = :name", PlayerStats.class)
                    .setParameter("name", playerName)
                    .uniqueResult();

            tx.commit();
            return stats;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return new PlayerStats(playerName);
        }
    }

    @Override
    public void update(PlayerStats stats) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().getCurrentSession();
            tx = session.beginTransaction();
            session.merge(stats);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public List<PlayerStats> getLeaderboard() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().getCurrentSession();
            tx = session.beginTransaction();
            List<PlayerStats> result = session.createQuery(
                            "FROM PlayerStats ORDER BY wins DESC", PlayerStats.class)
                    .setMaxResults(10)
                    .list();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return List.of();
        }
    }
}
