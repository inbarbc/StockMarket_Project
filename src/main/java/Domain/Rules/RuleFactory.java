package Domain.Rules;

import Domain.ShoppingBasket;
import Domain.User;
import Dtos.Rules.AllItemsRuleDto;
import Dtos.Rules.AndRuleDto;
import Dtos.Rules.ConditionRuleDto;
import Dtos.Rules.GenericRuleDto;
import Dtos.Rules.MinAgeRuleDto;
import Dtos.Rules.MinBasketPriceRuleDto;
import Dtos.Rules.MinProductAmountRuleDto;
import Dtos.Rules.OrRuleDto;
import Dtos.Rules.ShoppingBasketRuleDto;
import Dtos.Rules.TimeRangeInDayRuleDto;
import Dtos.Rules.TimeRangeInMonthRuleDto;
import Dtos.Rules.UserRuleDto;

@SuppressWarnings("unchecked")
public class RuleFactory {
    /*********  Shopping Basket Rules **********/
    public static Rule<ShoppingBasket> createShoppingBasketRule(ShoppingBasketRuleDto dto) {
        if (dto instanceof GenericRuleDto)
            return createGenericRule(ShoppingBasket.class, (GenericRuleDto) dto);

        if (dto instanceof MinBasketPriceRuleDto)
            return createShoppingBasketRule((MinBasketPriceRuleDto) dto);
        if (dto instanceof AllItemsRuleDto)
            return createShoppingBasketRule((AllItemsRuleDto) dto);
        if (dto instanceof MinProductAmountRuleDto)
            return createShoppingBasketRule((MinProductAmountRuleDto) dto);
        
        throw new IllegalArgumentException("Unknown shopping basket rule dto type: " + dto.getClass().getName());
    }

    private static Rule<ShoppingBasket> createShoppingBasketRule(MinBasketPriceRuleDto dto) {
        return new MinBasketPriceRule(dto.minPrice);
    }

    private static Rule<ShoppingBasket> createShoppingBasketRule(AllItemsRuleDto dto) {
        return new AllItemsRule(dto.productIds);
    }

    private static Rule<ShoppingBasket> createShoppingBasketRule(MinProductAmountRuleDto dto) {
        return new MinProductAmountRule(dto.productId, dto.minAmount);
    }

    /*********  User Rules **********/
    public static Rule<User> createUserRule(UserRuleDto dto) {
        if (dto instanceof GenericRuleDto)
            return createGenericRule(User.class, (GenericRuleDto) dto);

        if (dto instanceof MinAgeRuleDto)
            return createUserRule((MinAgeRuleDto) dto);

        throw new IllegalArgumentException("Unknown user rule dto type: " + dto.getClass().getName());
    }

    private static Rule<User> createUserRule(MinAgeRuleDto dto) {
        return new MinAgeRule(dto.minAge);
    }

    /*********  Generic Rules **********/
    private static <T> Rule<T> createGenericRule(Class<T> type, GenericRuleDto dto) {
        if (dto instanceof TimeRangeInDayRuleDto)
            return createGenericRule((TimeRangeInDayRuleDto) dto);
        if (dto instanceof TimeRangeInMonthRuleDto)
            return createGenericRule((TimeRangeInMonthRuleDto) dto);
        
        return createCompositeRule(type, dto);
    }

    private static <T> Rule<T> createGenericRule(TimeRangeInDayRuleDto dto) {
        return new TimeRangeInDayRule<T>(dto.startHour, dto.startMinutes, dto.endHour, dto.endMinutes);
    }

    private static <T> Rule<T> createGenericRule(TimeRangeInMonthRuleDto dto) {
        return new TimeRangeInMonthRule<T>(dto.startDay, dto.endDay);
    }

    /*********  Composite Rules **********/
    private static <T> Rule<T> createCompositeRule(Class<T> retType, GenericRuleDto dto) {
        if (dto instanceof AndRuleDto) 
            return createAndRule(retType, (AndRuleDto) dto);
        if (dto instanceof ConditionRuleDto)
            return createConditionRule(retType, (ConditionRuleDto) dto);
        if (dto instanceof OrRuleDto)
            return createOrRule(retType, (OrRuleDto) dto);

        throw new IllegalArgumentException("Unknown rule dto type: " + dto.getClass().getName());
    }

    private static <T> Rule<T> createAndRule(Class<T> retType, AndRuleDto dto) {
        if (retType == ShoppingBasket.class && dto.rule1 instanceof ShoppingBasketRuleDto && dto.rule2 instanceof ShoppingBasketRuleDto) {
            ShoppingBasketRuleDto rule1 = (ShoppingBasketRuleDto) dto.rule1;
            ShoppingBasketRuleDto rule2 = (ShoppingBasketRuleDto) dto.rule2;
            return (Rule<T>) new AndRule<ShoppingBasket>(createShoppingBasketRule(rule1), createShoppingBasketRule(rule2));
        } 
        if (retType == User.class && dto.rule1 instanceof UserRuleDto && dto.rule2 instanceof UserRuleDto) {
            UserRuleDto rule1 = (UserRuleDto) dto.rule1;
            UserRuleDto rule2 = (UserRuleDto) dto.rule2;
            return (Rule<T>) new AndRule<User>(createUserRule(rule1), createUserRule(rule2));
        } 

        throw new IllegalArgumentException("AndRule: rule1 and rule2 should be of the same type");
    }

    private static <T> Rule<T> createOrRule(Class<T> retType, OrRuleDto dto) {
        if (retType == ShoppingBasket.class && dto.rule1 instanceof ShoppingBasketRuleDto && dto.rule2 instanceof ShoppingBasketRuleDto) {
            ShoppingBasketRuleDto rule1 = (ShoppingBasketRuleDto) dto.rule1;
            ShoppingBasketRuleDto rule2 = (ShoppingBasketRuleDto) dto.rule2;
            return (Rule<T>) new OrRule<ShoppingBasket>(createShoppingBasketRule(rule1), createShoppingBasketRule(rule2));
        } 
        if (retType == User.class && dto.rule1 instanceof UserRuleDto && dto.rule2 instanceof UserRuleDto) {
            UserRuleDto rule1 = (UserRuleDto) dto.rule1;
            UserRuleDto rule2 = (UserRuleDto) dto.rule2;
            return (Rule<T>) new OrRule<User>(createUserRule(rule1), createUserRule(rule2));
        } 

        throw new IllegalArgumentException("OrRule: rule1 and rule2 should be of the same type");
    }

    private static <T> Rule<T> createConditionRule(Class<T> retType, ConditionRuleDto dto) {
        if (retType == ShoppingBasket.class && dto.conditionRule instanceof ShoppingBasketRuleDto && dto.thenRule instanceof ShoppingBasketRuleDto) {
            ShoppingBasketRuleDto conditionRule = (ShoppingBasketRuleDto) dto.conditionRule;
            ShoppingBasketRuleDto thenRule = (ShoppingBasketRuleDto) dto.thenRule;
            return (Rule<T>) new ConditionRule<ShoppingBasket>(createShoppingBasketRule(conditionRule), createShoppingBasketRule(thenRule));
        } 
        if (retType == User.class && dto.conditionRule instanceof UserRuleDto && dto.thenRule instanceof UserRuleDto) {
            UserRuleDto conditionRule = (UserRuleDto) dto.conditionRule;
            UserRuleDto thenRule = (UserRuleDto) dto.thenRule;
            return (Rule<T>) new ConditionRule<User>(createUserRule(conditionRule), createUserRule(thenRule));
        } 

        throw new IllegalArgumentException("ConditionRule: conditionRule and thenRule should be of the same type");
    }
}
