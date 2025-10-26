package com.dao;

import com.entities.*;
import com.interfaces.IPromotion;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

/**
 * @author Nguyễn Thanh Khôi
 */
public class DAO_Promotion implements IPromotion {
    private final SessionFactory sessionFactory;

    public DAO_Promotion() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }


    @Override
    public Promotion getPromotionById(String id) {
        try (Session session = sessionFactory.openSession()) {
            // FETCH JOIN để load điều kiện và hành động 1 lần
            Query<Promotion> q = session.createQuery(
                    "SELECT p FROM Promotion p " +
                            "LEFT JOIN FETCH p.conditions " +
                            "LEFT JOIN FETCH p.actions " +
                            "WHERE p.id = :id", Promotion.class);
            q.setParameter("id", id);
            return q.uniqueResult();
        }
    }


    @Override
    public boolean addPromotion(Promotion p) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            // Quan trọng: đảm bảo quan hệ 2 chiều
            p.getConditions().forEach(c -> c.setPromotion(p));
            p.getActions().forEach(a -> a.setPromotion(p));

            session.persist(p);
            transaction.commit();
            return true;

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public List<Promotion> getAllPromotions() {
        try (Session session = sessionFactory.openSession()) {
            // Load hết để tránh lazy error khi show UI
            Query<Promotion> query = session.createQuery(
                    "SELECT DISTINCT p FROM Promotion p " +
                            "LEFT JOIN FETCH p.conditions " +
                            "LEFT JOIN FETCH p.actions",
                    Promotion.class
            );
            return query.list();
        }
    }


    @Override
    public List<PromotionAction> getActionsByPromotionId(String promotionId) {
        try (Session session = sessionFactory.openSession()) {
            Query<PromotionAction> query = session.createQuery(
                    "FROM PromotionAction a WHERE a.promotion.id = :promotionId ORDER BY a.actionOrder ASC",
                    PromotionAction.class
            );
            query.setParameter("promotionId", promotionId);
            return query.list();
        }
    }


    @Override
    public List<PromotionCondition> getConditionsByPromotionId(String promotionId) {
        try (Session session = sessionFactory.openSession()) {
            Query<PromotionCondition> query = session.createQuery(
                    "FROM PromotionCondition c WHERE c.promotion.id = :promotionId",
                    PromotionCondition.class
            );
            query.setParameter("promotionId", promotionId);
            return query.list();
        }
    }
}
