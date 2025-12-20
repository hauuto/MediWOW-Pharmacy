package com.dao;

import com.entities.*;
import com.interfaces.IPromotion;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

            Promotion promotion = session.createQuery(
                            "SELECT DISTINCT p FROM Promotion p " +
                                    "LEFT JOIN FETCH p.conditions c " +
                                    "LEFT JOIN FETCH c.productUOM u " +
                                    "LEFT JOIN FETCH u.measurement " +
                                    "LEFT JOIN FETCH u.product " +
                                    "LEFT JOIN FETCH p.actions a " +
                                    "LEFT JOIN FETCH a.productUOM au " +
                                    "LEFT JOIN FETCH au.measurement " +
                                    "LEFT JOIN FETCH au.product " +
                                    "WHERE p.id = :id",
                            Promotion.class
                    )
                    .setParameter("id", id)
                    .uniqueResult();

            return promotion;

        } finally {
            if (session != null)
                session.close();
        }
    }


    public Promotion getLastPromotion() {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            // Ưu tiên sort theo creationDate nếu có

            return session.createQuery(
                            "FROM Promotion p ORDER BY p.id DESC",
                            Promotion.class
                    )
                    .setMaxResults(1)
                    .uniqueResult();

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi getLastPromotion: " + e.getMessage());
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    public boolean addPromotion(Promotion p) {
        Transaction tx = null;
        Session session = null;
        try{
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            session.persist(p);
            tx.commit();

            Promotion last = getLastPromotion();
            if (last != null) {
                p.setId(last.getId());
                System.out.println("Last Promotion ID: " + last.getId());
            }

            if (addPromotionDetail(p)){
                return true;
            }

            System.err.println("⚠️ Transaction rolled back!");
            return false;
        }
        catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
                System.err.println("⚠️ Transaction rolled back!");
            }
            e.printStackTrace();
            return false;
        } finally {
            if(session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    private boolean addPromotionDetail(Promotion p) {
        Transaction tx = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();

            // Conditions
            for (PromotionCondition c : p.getConditions()) {
                c.setPromotion(p); // FK
                session.persist(c); // trigger sẽ sinh ID
            }

            // Actions
            for (PromotionAction a : p.getActions()) {
                a.setPromotion(p); // FK
                session.persist(a); // trigger sẽ sinh ID
            }

            tx.commit();
            System.out.println("🎯 Saved "
                    + p.getConditions().size() + " conditions and "
                    + p.getActions().size() + " actions!");

            return true;

        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
                System.err.println("⚠️ Rollback detail insert!");
            }
            e.printStackTrace();
            return false;
        } finally {
            if(session != null && session.isOpen()) session.close();
        }
    }



    @Override
    public List<Promotion> getAllPromotions() {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            List<Promotion> promotions = session.createQuery(
                    "FROM Promotion",
                    Promotion.class
            ).getResultList();

            for (Promotion p : promotions) {
                Hibernate.initialize(p.getConditions());
                Hibernate.initialize(p.getActions());

                for (PromotionCondition cond : p.getConditions()) {
                    if (cond.getProductUOM() != null) {
                        Hibernate.initialize(cond.getProductUOM());
                        if (cond.getProductUOM().getProduct() != null)
                            Hibernate.initialize(cond.getProductUOM().getProduct());
                        if (cond.getProductUOM().getMeasurement() != null)
                            Hibernate.initialize(cond.getProductUOM().getMeasurement()); // 🔥 thêm dòng này
                    }
                }

                for (PromotionAction act : p.getActions()) {
                    if (act.getProductUOM() != null) {
                        Hibernate.initialize(act.getProductUOM());
                        if (act.getProductUOM().getProduct() != null)
                            Hibernate.initialize(act.getProductUOM().getProduct());
                        if (act.getProductUOM().getMeasurement() != null)
                            Hibernate.initialize(act.getProductUOM().getMeasurement()); // 🔥 thêm dòng này
                    }
                }
            }

            return promotions;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi load promotions: " + e.getMessage());
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
            // Fetch actions with productUOM and product references
            List<PromotionAction> actions = session.createQuery(
                            "SELECT DISTINCT a FROM PromotionAction a " +
                                    "LEFT JOIN FETCH a.productUOM pu " +
                                    "LEFT JOIN FETCH pu.product " +
                                    "LEFT JOIN FETCH pu.measurement " +   // 🔥 thêm dòng này
                                    "WHERE a.promotion.id = :promotionId " +
                                    "ORDER BY a.actionOrder ASC",
                            PromotionAction.class
                    )
                    .setParameter("promotionId", promotionId)
                    .list();


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
            System.err.println("❌ Lỗi khi load actions by promotionId: " + e.getMessage());
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
            // Fetch conditions with productUOM and product references
            List<PromotionCondition> conditions = session.createQuery(
                            "SELECT DISTINCT c FROM PromotionCondition c " +
                                    "LEFT JOIN FETCH c.productUOM pu " +
                                    "LEFT JOIN FETCH pu.product " +
                                    "LEFT JOIN FETCH pu.measurement " +   // 🔥 thêm dòng này
                                    "WHERE c.promotion.id = :promotionId",
                            PromotionCondition.class
                    )
                    .setParameter("promotionId", promotionId)
                    .list();


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
            System.err.println("❌ Lỗi khi load conditions by promotionId: " + e.getMessage());
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public boolean updatePromotion(Promotion detached) {
        Transaction tx = null;

        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();

            Promotion existing = session.find(Promotion.class, detached.getId());
            if (existing == null) return false;

            // Update scalar
            existing.setName(detached.getName());
            existing.setDescription(detached.getDescription());
            existing.setEffectiveDate(detached.getEffectiveDate());
            existing.setEndDate(detached.getEndDate());
            existing.setIsActive(detached.getIsActive());

            // Update detail without deleting
            mergeConditions(session, existing, detached.getConditions());
            mergeActions(session, existing, detached.getActions());

            session.flush();
            tx.commit();
            return true;

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    private void mergeConditions(Session session, Promotion existing, List<PromotionCondition> incomingList) {
        if (incomingList == null) incomingList = new ArrayList<>();

        // Collect IDs from incoming list (excluding new items without IDs)
        List<String> incomingIds = incomingList.stream()
                .filter(c -> c.getId() != null && !c.getId().isBlank())
                .map(PromotionCondition::getId)
                .toList();

        // Find and delete conditions that are no longer in the incoming list
        List<PromotionCondition> existingConditions = session.createQuery(
                "FROM PromotionCondition c WHERE c.promotion.id = :promotionId",
                PromotionCondition.class
        ).setParameter("promotionId", existing.getId()).getResultList();

        for (PromotionCondition existingCond : existingConditions) {
            if (!incomingIds.contains(existingCond.getId())) {
                // This condition was removed, delete it
                session.remove(existingCond);
            }
        }

        // Now add new or update existing conditions
        for (PromotionCondition incoming : incomingList) {
            if (incoming.getId() == null || incoming.getId().isBlank()) {
                // New condition
                PromotionCondition c = new PromotionCondition();
                c.setPromotion(existing);
                c.setTarget(incoming.getTarget());
                c.setComparator(incoming.getComparator());
                c.setConditionType(incoming.getConditionType());
                c.setValue(incoming.getValue());
                c.setProductUOM(incoming.getProductUOM());

                session.persist(c);
            } else {
                // Update existing
                PromotionCondition existingCond =
                        session.find(PromotionCondition.class, incoming.getId());

                if (existingCond != null) {
                    existingCond.setTarget(incoming.getTarget());
                    existingCond.setComparator(incoming.getComparator());
                    existingCond.setConditionType(incoming.getConditionType());
                    existingCond.setValue(incoming.getValue());
                    existingCond.setProductUOM(incoming.getProductUOM());
                }
            }
        }
    }

    private void mergeActions(Session session, Promotion existing, List<PromotionAction> incomingList) {
        if (incomingList == null) incomingList = new ArrayList<>();

        // Collect IDs from incoming list (excluding new items without IDs)
        List<String> incomingIds = incomingList.stream()
                .filter(a -> a.getId() != null && !a.getId().isBlank())
                .map(PromotionAction::getId)
                .toList();

        // Find and delete actions that are no longer in the incoming list
        List<PromotionAction> existingActions = session.createQuery(
                "FROM PromotionAction a WHERE a.promotion.id = :promotionId",
                PromotionAction.class
        ).setParameter("promotionId", existing.getId()).getResultList();

        for (PromotionAction existingAct : existingActions) {
            if (!incomingIds.contains(existingAct.getId())) {
                session.remove(existingAct);
            }
        }

        // Now add new or update existing actions
        for (PromotionAction incoming : incomingList) {
            if (incoming.getId() == null || incoming.getId().isBlank()) {
                PromotionAction a = new PromotionAction();
                a.setPromotion(existing);
                a.setActionOrder(incoming.getActionOrder());
                a.setType(incoming.getType());
                a.setTarget(incoming.getTarget());
                a.setValue(incoming.getValue());
                a.setProductUOM(incoming.getProductUOM());

                session.persist(a);
            } else {
                PromotionAction existingAct = session.find(PromotionAction.class, incoming.getId());
                if (existingAct != null) {
                    existingAct.setActionOrder(incoming.getActionOrder());
                    existingAct.setType(incoming.getType());
                    existingAct.setTarget(incoming.getTarget());
                    existingAct.setValue(incoming.getValue());
                    existingAct.setProductUOM(incoming.getProductUOM());
                }
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
                    "FROM Promotion p WHERE LOWER(p.name) LIKE :keyword " +
                            "OR LOWER(p.id) LIKE :keyword " +
                            "OR LOWER(p.description) LIKE :keyword",
                    Promotion.class
            ).setParameter("keyword", searchPattern).getResultList();

            for (Promotion p : promotions) {
                Hibernate.initialize(p.getConditions());
                Hibernate.initialize(p.getActions());

                for (PromotionCondition cond : p.getConditions()) {
                    if (cond.getProductUOM() != null) {
                        Hibernate.initialize(cond.getProductUOM());
                        if (cond.getProductUOM().getProduct() != null)
                            Hibernate.initialize(cond.getProductUOM().getProduct());
                        if (cond.getProductUOM().getMeasurement() != null)
                            Hibernate.initialize(cond.getProductUOM().getMeasurement()); // 🔥
                    }
                }
                for (PromotionAction act : p.getActions()) {
                    if (act.getProductUOM() != null) {
                        Hibernate.initialize(act.getProductUOM());
                        if (act.getProductUOM().getProduct() != null)
                            Hibernate.initialize(act.getProductUOM().getProduct());
                        if (act.getProductUOM().getMeasurement() != null)
                            Hibernate.initialize(act.getProductUOM().getMeasurement()); // 🔥
                    }
                }
            }

            return promotions;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tìm kiếm promotions: " + e.getMessage());
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

            StringBuilder queryStr = new StringBuilder("FROM Promotion p WHERE 1=1");
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

            for (Promotion p : promotions) {
                Hibernate.initialize(p.getConditions());
                Hibernate.initialize(p.getActions());

                for (PromotionCondition cond : p.getConditions()) {
                    if (cond.getProductUOM() != null) {
                        Hibernate.initialize(cond.getProductUOM());
                        if (cond.getProductUOM().getProduct() != null)
                            Hibernate.initialize(cond.getProductUOM().getProduct());
                        if (cond.getProductUOM().getMeasurement() != null)
                            Hibernate.initialize(cond.getProductUOM().getMeasurement()); // <-- 🔥 thêm dòng này
                    }
                }

                for (PromotionAction act : p.getActions()) {
                    if (act.getProductUOM() != null) {
                        Hibernate.initialize(act.getProductUOM());
                        if (act.getProductUOM().getProduct() != null)
                            Hibernate.initialize(act.getProductUOM().getProduct());
                        if (act.getProductUOM().getMeasurement() != null)
                            Hibernate.initialize(act.getProductUOM().getMeasurement()); // <-- 🔥 thêm dòng này
                    }
                }
            }

            return promotions;

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lọc promotions: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }




}
