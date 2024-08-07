package Domain.Entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import Exceptions.StockMarketException;

// class that represents an order for the user
@Entity
@Table(name = "[order]")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;

    @ElementCollection
    @CollectionTable(name = "order_shopping_baskets", joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyColumn(name = "shop_id")
    @Column(name = "shopping_basket_id") // Corrected to @Column instead of @JoinColumn
    private Map<Integer, ShoppingBasket> _shoppingBasketMap; // <ShopId, ShoppingBasketPerShop>

    @Column(name = "totalOrderAmount", nullable = false)
    private double totalOrderAmount;

    @Column(name = "paymentId", nullable = false)
    private int paymentId;

    @Column(name = "SupplyId", nullable = false)
    private int SupplyId;

    // Default constructor
    public Order() { }
    
    // Constructor
    public Order(List<ShoppingBasket> shoppingBasket, int paymentId, int supplyId) throws StockMarketException {
        this._shoppingBasketMap = new HashMap<>();
        setShoppingBasketMap(shoppingBasket);
        this.totalOrderAmount = 0.0;
        setTotalOrderAmount();
        this.paymentId = paymentId;
        this.SupplyId = supplyId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public Object getId() {
        return orderId;
    }

    // This method is used to set the shoppingBasketMap when creating the order
    private void setShoppingBasketMap(List<ShoppingBasket> shoppingBaskets){
        for (ShoppingBasket basket : shoppingBaskets) {
            _shoppingBasketMap.put(basket.getShopId(), basket);
        }
    }

    // This method is used to set the total order amount when creating the order
    private void setTotalOrderAmount() throws StockMarketException {
        totalOrderAmount = 0.0;
        for (Map.Entry<Integer, ShoppingBasket> entry : _shoppingBasketMap.entrySet()) {
            totalOrderAmount += entry.getValue().getShoppingBasketPrice();
        }
    }

    // This method is used to calculate the total order amount
    public void calcTotalAmount() throws StockMarketException { 
        totalOrderAmount = 0.0;
        for (Map.Entry<Integer, ShoppingBasket> entry : _shoppingBasketMap.entrySet()) {
            totalOrderAmount += entry.getValue().getShoppingBasketPrice();
        }
    }

    // Helper method to print all products in the order by shopId
    private String printAllShopAndProducts() 
    {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, ShoppingBasket> entry : _shoppingBasketMap.entrySet()) {
            sb.append("ShopId: " + entry.getKey() + "\n");
            sb.append(printAllProducts(entry.getValue()));
        }
        return sb.toString();
    }

    private String printAllProducts(ShoppingBasket shoppingBasket) {
        return shoppingBasket.printAllProducts();
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", totalAmount=" + totalOrderAmount +
                ", products= \n" + printAllShopAndProducts() +
                ", paymentId=" + paymentId +
                ", SupplyId=" + SupplyId +
                '}';
    }

    // Getters

    public Map<Integer ,ShoppingBasket> getProductsByShoppingBasket() {
        return _shoppingBasketMap;
    }

    public double getOrderTotalAmount() throws StockMarketException { 
        if(totalOrderAmount == 0.0)
            calcTotalAmount();
        return totalOrderAmount; 
    }

    public Map<Integer, ShoppingBasket> getShoppingBasketMap() {
        return _shoppingBasketMap;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public int getSupplyId() {
        return SupplyId;
    }

    // for tests - get all product ids
    public List<Integer> getAllProductIds() throws StockMarketException {
        List<Integer> allProductIds = new java.util.ArrayList<>();
        for (Map.Entry<Integer, ShoppingBasket> entry : _shoppingBasketMap.entrySet()) {
            allProductIds.addAll(entry.getValue().getProductIdsList());
        }
        return allProductIds;
    }

    public void setId(int i) {
        orderId = i;
    }
}
