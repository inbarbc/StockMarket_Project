package Domain.Discounts;

import java.util.Date;
import java.util.List;

import Domain.ShoppingBasket;
import Domain.Rules.Rule;
import Exceptions.DiscountExpiredException;

/**
 * Represents a conditional discount that applies a base discount to a shopping
 * basket
 * if certain products are present in the basket.
 */
public class ConditionalDiscount extends Discount {
    private BaseDiscount _discount;

    /**
     * Constructs a new ConditionalDiscount object with the specified must-have
     * products
     * and base discount.
     * 
     * @param mustHaveProducts the list of product IDs that must be present in the
     *                         basket
     * @param discount         the base discount to apply if the must-have products
     *                         are present
     */
    public ConditionalDiscount(List<Integer> mustHaveProducts, BaseDiscount discount) {
        super(discount.getExpirationDate());
        _discount = discount;
        _rule = (basket) -> mustHaveProducts.stream().allMatch((productId) -> basket.getProductCount(productId) > 0);
    }

    /**
     * Applies the discount to the given shopping basket if the rule is satisfied.
     * 
     * @param basket the shopping basket to apply the discount to
     * @throws DiscountExpiredException if the discount has expired
     */
    @Override
    protected void applyDiscountLogic(ShoppingBasket basket) throws DiscountExpiredException {
        if (_rule.predicate(basket))
            _discount.applyDiscount(basket);
    }

}