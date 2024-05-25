package Domain.Discounts;

import java.sql.Date;
import java.util.SortedMap;

import Domain.ShoppingBasket;
import Domain.Rules.Rule;

public class FixedDiscount extends BaseDiscount {
    private double _discountTotal;
    private Rule<ShoppingBasket> _rule;
    private int _productId;

    /**
     * Represents a fixed discount for a specific product.
     */
    public FixedDiscount(Date expirationDate, double discountTotal, int productId) {
        super(expirationDate);
        _discountTotal = discountTotal;
        _productId = productId;

        _rule = (basket) -> basket.getProductCount(productId) > 0;
    }

    /**
     * Applies the fixed discount to the products in the shopping basket.
     * If the product is not in the basket, the discount is not applied.
     * The discount is applied to the most expensive product in the basket.
     * The price and amount of the product are updated based on the discount in the
     * productToPriceToAmount mapping.
     *
     * @param basket The shopping basket to apply the discount to.
     */
    @Override

    public void applyDiscountLogic(ShoppingBasket basket) {
        if (!_rule.predicate(basket))
            return;

        SortedMap<Double, Integer> priceToAmount = basket.productToPriceToAmount.get(_productId);

        // get most expensive price and amount
        double price = priceToAmount.firstKey();
        int amount = priceToAmount.get(price);

        // calculate discount, and amount of the product at the discounted price
        double postPrice = Math.max(price - _discountTotal, 0.0);
        int postAmount = priceToAmount.get(postPrice);

        // update the price to amount mapping
        priceToAmount.put(postPrice, postAmount + 1);
        priceToAmount.put(price, amount - 1);
    }
}
