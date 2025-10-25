package com.interfaces;

import com.entities.*;

import java.util.List;

public interface IPromotion {
    public Promotion getPromotionById(String id);
    public boolean addPromotion(Promotion p);
    public List<Promotion> getAllPromotions();
    public List<PromotionAction> getActionsByPromotionId(String promotionId);
    public List<PromotionCondition> getConditionsByPromotionId(String promotionId);

}
