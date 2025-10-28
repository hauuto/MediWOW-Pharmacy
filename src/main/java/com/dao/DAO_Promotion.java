package com.dao;

import com.entities.*;
import com.interfaces.IPromotion;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

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
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch promotion with conditions first
            Promotion promotion = session.createQuery(
                "SELECT DISTINCT p FROM Promotion p " +
                "LEFT JOIN FETCH p.conditions " +
                "WHERE p.id = :id",
                Promotion.class
            ).setParameter("id", id).uniqueResult();

            // Then fetch actions for the same promotion
            if (promotion != null) {
                session.createQuery(
                    "SELECT DISTINCT p FROM Promotion p " +
                    "LEFT JOIN FETCH p.actions " +
                    "WHERE p.id = :id",
                    Promotion.class
                ).setParameter("id", id).uniqueResult();

                // Fetch products referenced in conditions
                session.createQuery(
                    "SELECT DISTINCT pc FROM PromotionCondition pc " +
                    "LEFT JOIN FETCH pc.product " +
                    "WHERE pc.promotion.id = :id",
                    PromotionCondition.class
                ).setParameter("id", id).list();

                // Fetch products referenced in actions
                session.createQuery(
                    "SELECT DISTINCT pa FROM PromotionAction pa " +
                    "LEFT JOIN FETCH pa.product " +
                    "WHERE pa.promotion.id = :id",
                    PromotionAction.class
                ).setParameter("id", id).list();
            }

            return promotion;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
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
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch promotions with conditions first
            List<Promotion> promotions = session.createQuery(
                "SELECT DISTINCT p FROM Promotion p " +
                "LEFT JOIN FETCH p.conditions",
                Promotion.class
            ).list();

            // Then fetch actions for all promotions
            if (!promotions.isEmpty()) {
                session.createQuery(
                    "SELECT DISTINCT p FROM Promotion p " +
                    "LEFT JOIN FETCH p.actions " +
                    "WHERE p IN :promotions",
                    Promotion.class
                ).setParameter("promotions", promotions).list();

                // Fetch products referenced in conditions
                session.createQuery(
                    "SELECT DISTINCT pc FROM PromotionCondition pc " +
                    "LEFT JOIN FETCH pc.product " +
                    "WHERE pc.promotion IN :promotions",
                    PromotionCondition.class
                ).setParameter("promotions", promotions).list();

                // Fetch products referenced in actions
                session.createQuery(
                    "SELECT DISTINCT pa FROM PromotionAction pa " +
                    "LEFT JOIN FETCH pa.product " +
                    "WHERE pa.promotion IN :promotions",
                    PromotionAction.class
                ).setParameter("promotions", promotions).list();
            }

            return promotions;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    @Override
    public List<PromotionAction> getActionsByPromotionId(String promotionId) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch actions with product references
            List<PromotionAction> actions = session.createQuery(
                "SELECT DISTINCT a FROM PromotionAction a " +
                "LEFT JOIN FETCH a.product " +
                "WHERE a.promotion.id = :promotionId " +
                "ORDER BY a.actionOrder ASC",
                PromotionAction.class
            ).setParameter("promotionId", promotionId).list();

            // Fetch promotion reference
            if (!actions.isEmpty()) {
                session.createQuery(
                    "SELECT DISTINCT a FROM PromotionAction a " +
                    "LEFT JOIN FETCH a.promotion " +
                    "WHERE a.promotion.id = :promotionId",
                    PromotionAction.class
                ).setParameter("promotionId", promotionId).list();
            }

            return actions;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    @Override
    public List<PromotionCondition> getConditionsByPromotionId(String promotionId) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch conditions with product references
            List<PromotionCondition> conditions = session.createQuery(
                "SELECT DISTINCT c FROM PromotionCondition c " +
                "LEFT JOIN FETCH c.product " +
                "WHERE c.promotion.id = :promotionId",
                PromotionCondition.class
            ).setParameter("promotionId", promotionId).list();

            // Fetch promotion reference
            if (!conditions.isEmpty()) {
                session.createQuery(
                    "SELECT DISTINCT c FROM PromotionCondition c " +
                    "LEFT JOIN FETCH c.promotion " +
                    "WHERE c.promotion.id = :promotionId",
                    PromotionCondition.class
                ).setParameter("promotionId", promotionId).list();
            }

            return conditions;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
