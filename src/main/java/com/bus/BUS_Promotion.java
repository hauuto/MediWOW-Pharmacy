package com.bus;

import com.dao.DAO_Promotion;
import com.entities.Promotion;
import com.entities.PromotionAction;
import com.entities.PromotionCondition;
import com.enums.PromotionEnum;
import com.interfaces.IPromotion;

import java.time.LocalDate;
import java.util.List;
/**
 * @author Nguyễn Thanh Khôi
 */
public class BUS_Promotion implements IPromotion {

    private final DAO_Promotion dao;

    public BUS_Promotion() {
        this.dao = new DAO_Promotion();
    }

    @Override
    public Promotion getPromotionById(String id) {
        return dao.getPromotionById(id);
    }

    @Override
    public boolean addPromotion(Promotion p) {

        // ===== VALIDATION =====

        if (p.getName() == null || p.getName().isBlank()) {
            System.err.println("❌ Tên khuyến mãi không được để trống");
            return false;
        }

        if (p.getEffectiveDate() == null || p.getEndDate() == null) {
            System.err.println("❌ Thiếu ngày bắt đầu hoặc kết thúc");
            return false;
        }

        if (p.getEndDate().isBefore(p.getEffectiveDate())) {
            System.err.println("❌ Ngày kết thúc không thể trước ngày bắt đầu");
            return false;
        }

        // Tự động gán trạng thái
        LocalDate now = LocalDate.now();
        if (p.getEndDate().isBefore(now)) {
            p.setIsActive(false);
        } else if (p.getEffectiveDate().isAfter(now)) {
            p.setIsActive(false);
        } else {
            p.setIsActive(true);
        }

        // Validate conditions
        for (PromotionCondition c : p.getConditions()) {
            if (c.getTarget() == null) {
                System.err.println("❌ Condition thiếu Target");
                return false;
            }
            if (c.getComparator() == null) {
                System.err.println("❌ Condition thiếu Comparator");
                return false;
            }
        }

        // Validate actions
        for (PromotionAction a : p.getActions()) {
            if (a.getType() == null) {
                System.err.println("❌ Action thiếu Type");
                return false;
            }
            if (a.getTarget() == null) {
                System.err.println("❌ Action thiếu Target");
                return false;
            }
        }

        return dao.addPromotion(p);
    }

    @Override
    public List<Promotion> getAllPromotions() {
        return dao.getAllPromotions();
    }

    @Override
    public List<PromotionAction> getActionsByPromotionId(String promotionId) {
        return dao.getActionsByPromotionId(promotionId);
    }

    @Override
    public List<PromotionCondition> getConditionsByPromotionId(String promotionId) {
        return dao.getConditionsByPromotionId(promotionId);
    }

    @Override
    public boolean updatePromotion(Promotion p) {
        // ===== VALIDATION =====
        if (p.getId() == null || p.getId().isBlank()) {
            System.err.println("❌ Mã khuyến mãi không được để trống");
            return false;
        }

        if (p.getName() == null || p.getName().isBlank()) {
            System.err.println("❌ Tên khuyến mãi không được để trống");
            return false;
        }

        if (p.getEffectiveDate() == null || p.getEndDate() == null) {
            System.err.println("❌ Thiếu ngày bắt đầu hoặc kết thúc");
            return false;
        }

        if (p.getEndDate().isBefore(p.getEffectiveDate())) {
            System.err.println("❌ Ngày kết thúc không thể trước ngày bắt đầu");
            return false;
        }

        // Validate conditions
        for (PromotionCondition c : p.getConditions()) {
            if (c.getTarget() == null) {
                System.err.println("❌ Condition thiếu Target");
                return false;
            }
            if (c.getComparator() == null) {
                System.err.println("❌ Condition thiếu Comparator");
                return false;
            }
        }

        // Validate actions
        for (PromotionAction a : p.getActions()) {
            if (a.getType() == null) {
                System.err.println("❌ Action thiếu Type");
                return false;
            }
            if (a.getTarget() == null) {
                System.err.println("❌ Action thiếu Target");
                return false;
            }
        }

        return dao.updatePromotion(p);
    }


    @Override
    public List<Promotion> searchPromotions(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllPromotions();
        }
        return dao.searchPromotions(keyword);
    }

    @Override
    public List<Promotion> filterPromotions(Boolean isActive, Boolean isValid) {
        return dao.filterPromotions(isActive, isValid);
    }

    // Helper cho GUI
    public List<PromotionCondition> getConditions(String id) {
        return dao.getConditionsByPromotionId(id);
    }

    public List<PromotionAction> getActions(String id) {
        return dao.getActionsByPromotionId(id);
    }
}
