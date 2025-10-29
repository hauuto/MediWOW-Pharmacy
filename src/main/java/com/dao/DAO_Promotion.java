package com.dao;

import com.entities.*;
import com.interfaces.IPromotion;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.ArrayList;
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
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            // Quan trọng: đảm bảo quan hệ 2 chiều
            if (p.getConditions() != null) {
                p.getConditions().forEach(c -> c.setPromotion(p));
            }
            if (p.getActions() != null) {
                p.getActions().forEach(a -> a.setPromotion(p));
            }

            session.persist(p);
            transaction.commit();

            System.out.println("✅ Đã lưu promotion thành công: " + p.getId());
            return true;

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                    System.err.println("⚠️ Đã rollback transaction");
                } catch (Exception rollbackEx) {
                    System.err.println("❌ Lỗi khi rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("❌ Lỗi khi thêm promotion: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    @Override
    public List<Promotion> getAllPromotions() {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            // Fetch promotions với JOIN FETCH để load cả conditions và actions
            List<Promotion> promotions = session.createQuery(
                "SELECT DISTINCT p FROM Promotion p " +
                "LEFT JOIN FETCH p.conditions " +
                "LEFT JOIN FETCH p.actions",
                Promotion.class
            ).getResultList();

            // Force initialize tất cả lazy collections trước khi đóng session
            for (Promotion p : promotions) {
                // Initialize conditions và product của condition
                if (p.getConditions() != null) {
                    p.getConditions().size(); // Force initialize
                    for (PromotionCondition cond : p.getConditions()) {
                        if (cond.getProduct() != null) {
                            cond.getProduct().getId(); // Force load product
                        }
                    }
                }

                // Initialize actions và product của action
                if (p.getActions() != null) {
                    p.getActions().size(); // Force initialize
                    for (PromotionAction act : p.getActions()) {
                        if (act.getProduct() != null) {
                            act.getProduct().getId(); // Force load product
                        }
                    }
                }
            }

            return promotions;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi load promotions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
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

    @Override
    public boolean updatePromotion(Promotion p) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            // Load existing promotion
            Promotion existing = session.get(Promotion.class, p.getId());
            if (existing == null) {
                System.err.println("❌ Không tìm thấy promotion với ID: " + p.getId());
                return false;
            }

            // Clear existing conditions and actions
            existing.getConditions().clear();
            existing.getActions().clear();
            session.flush();

            // Update basic fields
            existing.setName(p.getName());
            existing.setDescription(p.getDescription());
            existing.setEffectiveDate(p.getEffectiveDate());
            existing.setEndDate(p.getEndDate());
            existing.setIsActive(p.getIsActive());

            // Add new conditions and actions
            if (p.getConditions() != null) {
                for (PromotionCondition c : p.getConditions()) {
                    c.setPromotion(existing);
                    existing.getConditions().add(c);
                }
            }
            if (p.getActions() != null) {
                for (PromotionAction a : p.getActions()) {
                    a.setPromotion(existing);
                    existing.getActions().add(a);
                }
            }

            session.merge(existing);
            transaction.commit();

            System.out.println("✅ Đã cập nhật promotion thành công: " + p.getId());
            return true;

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                    System.err.println("⚠️ Đã rollback transaction");
                } catch (Exception rollbackEx) {
                    System.err.println("❌ Lỗi khi rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("❌ Lỗi khi cập nhật promotion: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    @Override
    public List<Promotion> searchPromotions(String keyword) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            String searchPattern = "%" + keyword.toLowerCase() + "%";
            List<Promotion> promotions = session.createQuery(
                "SELECT DISTINCT p FROM Promotion p " +
                "LEFT JOIN FETCH p.conditions " +
                "LEFT JOIN FETCH p.actions " +
                "WHERE LOWER(p.name) LIKE :keyword " +
                "OR LOWER(p.id) LIKE :keyword " +
                "OR LOWER(p.description) LIKE :keyword",
                Promotion.class
            ).setParameter("keyword", searchPattern).getResultList();

            // Force initialize lazy collections
            for (Promotion p : promotions) {
                if (p.getConditions() != null) {
                    p.getConditions().size();
                    for (PromotionCondition cond : p.getConditions()) {
                        if (cond.getProduct() != null) {
                            cond.getProduct().getId();
                        }
                    }
                }
                if (p.getActions() != null) {
                    p.getActions().size();
                    for (PromotionAction act : p.getActions()) {
                        if (act.getProduct() != null) {
                            act.getProduct().getId();
                        }
                    }
                }
            }

            return promotions;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tìm kiếm promotions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Promotion> filterPromotions(Boolean isActive, Boolean isValid) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            StringBuilder queryStr = new StringBuilder(
                "SELECT DISTINCT p FROM Promotion p " +
                "LEFT JOIN FETCH p.conditions " +
                "LEFT JOIN FETCH p.actions " +
                "WHERE 1=1"
            );

            if (isActive != null) {
                queryStr.append(" AND p.isActive = :isActive");
            }

            if (isValid != null) {
                if (isValid) {
                    queryStr.append(" AND p.endDate >= CURRENT_DATE AND p.effectiveDate <= CURRENT_DATE");
                } else {
                    queryStr.append(" AND (p.endDate < CURRENT_DATE OR p.effectiveDate > CURRENT_DATE)");
                }
            }

            var query = session.createQuery(queryStr.toString(), Promotion.class);

            if (isActive != null) {
                query.setParameter("isActive", isActive);
            }

            List<Promotion> promotions = query.getResultList();

            // Force initialize lazy collections
            for (Promotion p : promotions) {
                if (p.getConditions() != null) {
                    p.getConditions().size();
                    for (PromotionCondition cond : p.getConditions()) {
                        if (cond.getProduct() != null) {
                            cond.getProduct().getId();
                        }
                    }
                }
                if (p.getActions() != null) {
                    p.getActions().size();
                    for (PromotionAction act : p.getActions()) {
                        if (act.getProduct() != null) {
                            act.getProduct().getId();
                        }
                    }
                }
            }

            return promotions;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lọc promotions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
