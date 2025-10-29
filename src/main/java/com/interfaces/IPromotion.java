package com.interfaces;

import com.entities.*;

import java.util.List;

public interface IPromotion {
    public Promotion getPromotionById(String id);
    public boolean addPromotion(Promotion p);
    public boolean updatePromotion(Promotion p);
    public List<Promotion> getAllPromotions();
    public List<Promotion> searchPromotions(String keyword);
    public List<Promotion> filterPromotions(Boolean isActive, Boolean isValid);
    public List<PromotionAction> getActionsByPromotionId(String promotionId);
    public List<PromotionCondition> getConditionsByPromotionId(String promotionId);
}
