package Domain.Entities;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;

import Domain.ExternalServices.PaymentService.AdapterPaymentImp;
import Domain.ExternalServices.PaymentService.AdapterPaymentInterface;
import Domain.ExternalServices.PaymentService.ProxyPayment;
import Domain.ExternalServices.SupplyService.AdapterSupplyImp;
import Domain.ExternalServices.SupplyService.AdapterSupplyInterface;
import Domain.ExternalServices.SupplyService.ProxySupply;
import Domain.Facades.ShopFacade;
import Domain.Repositories.InterfaceOrderRepository;
import Domain.Repositories.InterfaceShopOrderRepository;
import Domain.Repositories.InterfaceShoppingBasketRepository;
import Dtos.PurchaseCartDetailsDto;

import java.util.Optional;
import Exceptions.PaymentFailedException;
import Exceptions.ProdcutPolicyException;
import Exceptions.ProductDoesNotExistsException;
import Exceptions.ProductOutOfStockExepction;
import Exceptions.ShippingFailedException;
import Exceptions.StockMarketException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.CascadeType;
import Exceptions.ShopPolicyException;

// This class represents a shopping cart that contains a list of shopping baskets.
// The shopping cart connected to one user at any time.
@Entity
@Table(name = "[shopping_cart]")
public class ShoppingCart {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "_shopping_cart_id", nullable = false, updatable = false)
    private Integer shoppingCartId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "shopping_cart_id") // Specifies the foreign key column in ShoppingBasket
    private List<ShoppingBasket> shoppingBaskets;

    @Transient
    @Autowired
    private AdapterPaymentInterface paymentMethod;

    @Transient
    @Autowired
    private AdapterSupplyInterface supplyMethod;

    @Transient
    @Autowired
    private ShopFacade shopFacade;

    @Transient
    private InterfaceOrderRepository orderRepository;

    @Transient
    private InterfaceShoppingBasketRepository basketRepository;

    @Transient
    private InterfaceShopOrderRepository shopOrderRepository;

    @Column(name = "user_or_guest_name")
    private String user_or_guest_name; // or guestToken string

    @OneToOne(mappedBy = "shoppingCart", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Guest guest; // or guestToken string

    @OneToOne(optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user; // if the user is null, the cart is for a guest.

    private static final Logger logger = Logger.getLogger(ShoppingCart.class.getName());

    // Default constructor for hibernate
    public ShoppingCart() {
        shoppingBaskets = new ArrayList<>();
        paymentMethod = AdapterPaymentImp.getRealAdapterPayment();
        supplyMethod = AdapterSupplyImp.getAdapterSupply();
        user = null;
    }

    // Constructor
    public ShoppingCart(User user) {
        this.shoppingBaskets = new ArrayList<>();
        this.paymentMethod = AdapterPaymentImp.getRealAdapterPayment();
        this.supplyMethod = AdapterSupplyImp.getAdapterSupply();
        this.guest = null;
        this.user = user;
        this.user_or_guest_name = user.getUserName();
    }

    // Constructor
    public ShoppingCart(Guest guest) {
        this.shoppingBaskets = new ArrayList<>();
        this.paymentMethod = AdapterPaymentImp.getRealAdapterPayment();
        this.supplyMethod = AdapterSupplyImp.getAdapterSupply();
        this.guest = guest;
        this.user_or_guest_name = guest.getGuestId();
        this.user = null;
    }

    // for tests
    public ShoppingCart(ShopFacade shopFacade, ProxyPayment paymentMethod, ProxySupply supplyMethod) {
        this.shoppingBaskets = new ArrayList<>();
        this.paymentMethod = paymentMethod;
        this.supplyMethod = supplyMethod;
        this.shopFacade = shopFacade;
        this.user_or_guest_name = null;
        this.user = null;
    }

    // for tests
    public ShoppingCart(ShopFacade shopFacade, AdapterPaymentImp paymentMethod, AdapterSupplyImp supplyMethod) {
        this.shoppingBaskets = new ArrayList<>();
        this.paymentMethod = paymentMethod;
        this.supplyMethod = supplyMethod;
        this.shopFacade = shopFacade;
        this.user_or_guest_name = null;
        this.user = null;
    }

    public void emptyCart() {
        shoppingBaskets.clear();
    }

    public void setOrderRepository(InterfaceOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void setShopOrderRepository(InterfaceShopOrderRepository shopOrderRepository) {
        this.shopOrderRepository = shopOrderRepository;
    }

    /*
     * This method is responsible for purchasing the cart.
     * It first calls the purchaseCart method of the shopping cart which reaponsible
     * for changing the item's stock.
     * Then it tries to pay and deliver the items.
     * If the payment or the delivery fails, it cancels the purchase and restock the
     * item.
     */
    public void purchaseCart(PurchaseCartDetailsDto purchaseCartDetailsDto)
            throws PaymentFailedException, ShippingFailedException, StockMarketException {
        try {
            purchaseCartEditStock(purchaseCartDetailsDto.getBasketsToBuy());
        } catch (StockMarketException e) {
            logger.log(Level.SEVERE, "StockMarketException has been thrown: " + e.getMessage(), e);
            throw e;
        }

        Map<Double, String> priceToShopDetails = new HashMap<>();
        double overallPrice = 0;

        for (Integer basketNum : purchaseCartDetailsDto.getBasketsToBuy()) {
            ShoppingBasket shoppingBasket = shoppingBaskets.get(basketNum);
            double amountToPay = shoppingBasket.calculateShoppingBasketPrice();
            overallPrice += amountToPay;
            priceToShopDetails.put(amountToPay, shoppingBasket.getShopBankDetails());
        }

        int paymentTransactionId = -1;
        int supplyTransactionId = -1;

        try {
            if (!paymentMethod.handshake())
                throw new PaymentFailedException("Payment service is not available");

            if (!supplyMethod.handshake())
                throw new ShippingFailedException("Shipping service is not available");

            paymentTransactionId = paymentMethod.payment(purchaseCartDetailsDto.getPaymentInfo(), overallPrice);
            if (paymentTransactionId == -1)
                throw new PaymentFailedException("Payment failed");

            supplyTransactionId = supplyMethod.supply(purchaseCartDetailsDto.getSupplyInfo());
            if (supplyTransactionId == -1)
                throw new ShippingFailedException("Shipping failed");

            List<ShoppingBasket> shoppingBasketsForOrder = new ArrayList<>();
            for (Integer basketNum : purchaseCartDetailsDto.getBasketsToBuy()) {
                ShoppingBasket shoppingBasket = shoppingBaskets.get(basketNum);
                shoppingBasketsForOrder.add(shoppingBasket);
            }

            if (user != null) {
                Order order = new Order(shoppingBasketsForOrder, paymentTransactionId, supplyTransactionId);
                order = orderRepository.save(order);
                user.addOrder(order);
            }

            for (ShoppingBasket shoppingBasket : shoppingBasketsForOrder) {
                ShopOrder shopOrder = new ShopOrder(shoppingBasket.getShop().getShopId(), shoppingBasket);
                shopOrder = shopOrderRepository.save(shopOrder);
                shoppingBasket.getShop().addOrderToOrderHistory(shopOrder);
            }

        } catch (PaymentFailedException e) {
            logger.log(Level.SEVERE, "Payment has been failed with exception: " + e.getMessage(), e);
            cancelPurchaseEditStock(purchaseCartDetailsDto.getBasketsToBuy());
            throw new PaymentFailedException("Payment failed");
        } catch (ShippingFailedException e) {
            logger.log(Level.SEVERE, "Shipping has been failed with exception: " + e.getMessage(), e);
            cancelPurchaseEditStock(purchaseCartDetailsDto.getBasketsToBuy());
            throw new ShippingFailedException("Shipping failed");
        }
    }

    public String getUsernameString() {
        return user_or_guest_name;
    }

    /*
     * Go thorugh the list of baskets to buy and purchase them.
     * If an exception is thrown, cancel the purchase of all the baskets that were
     * bought.
     * This function only updates the item's stock.
     */
    public void purchaseCartEditStock(List<Integer> busketsToBuy) throws StockMarketException {
        logger.log(Level.FINE, "ShoppingCart - purchaseCart - Start purchasing cart.");
        List<Integer> boughtBasketList = new ArrayList<>();

        for (Integer basketId : busketsToBuy) {
            if (basketId == -1) {
                continue;
            }
            try {
                String buyinguser;
                if (user == null) {
                    buyinguser = "Guest";
                } else {
                    buyinguser = getUsernameString();
                }
                if (!shoppingBaskets.get(basketId).purchaseBasket(buyinguser))
                    throw new ProductOutOfStockExepction("One of the products in the basket is out of stock");
                boughtBasketList.add(basketId);
            } catch (ProductOutOfStockExepction e) {
                logger.log(Level.SEVERE, "ShoppingCart - purchaseCart - Product out of stock for basket number: "
                        + basketId + ". Exception: " + e.getMessage(), e);
                logger.log(Level.FINE, "ShoppingCart - purchaseCart - Canceling purchase of all baskets.");
                for (Integer basket : boughtBasketList) {
                    shoppingBaskets.get(basket).cancelPurchase();
                }
                throw e;
            } catch (ShopPolicyException e) {
                logger.log(Level.SEVERE,
                        "ShoppingCart - purchaseCart - Basket " + basketId + " Validated the policy of the shop.");
                logger.log(Level.FINE, "ShoppingCart - purchaseCart - Canceling purchase of all baskets.");
                for (Integer basket : boughtBasketList) {
                    shoppingBaskets.get(basket).cancelPurchase();
                }
                throw e;
            }
        }
    }

    /*
     * Go through the list of baskets to cancel and cancel the purchase of them.
     * This function only updates the item's stock.
     */
    public void cancelPurchaseEditStock(List<Integer> busketsToBuy) throws StockMarketException {
        logger.log(Level.FINE, "ShoppingCart - cancelPurchase - Canceling purchase of all baskets.");
        for (Integer basketId : busketsToBuy) {
            getShoppingBasket(basketId).cancelPurchase();
        }
    }

    public int getCartSize() {
        return shoppingBaskets.size();
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        for (ShoppingBasket shoppingBasket : shoppingBaskets) {
            output.append(shoppingBasket.toString()).append("\n");
        }
        return output.toString(); // Convert StringBuilder to String
    }

    /**
     * Add a product to the shopping cart of a user.
     * 
     * @param productID the product to add.
     * @param shopID    the shop the product is from.
     * @param user      the user that wants to add the prodcut.
     * @throws ProdcutPolicyException
     * @throws ProductDoesNotExistsException
     */
    public void addProduct(int productID, int shopID, int quantity) throws StockMarketException {
        // Check if the product exists in the shop.
        if (shopFacade.getShopByShopId(shopID).getProductById(productID) == null) {
            logger.log(Level.SEVERE, "Product does not exists in shop: " + shopID);
            throw new ProductDoesNotExistsException("Product does not exists in shop: " + shopID);
        }

        // basketOptional is the basket of the user for the shop.
        Optional<ShoppingBasket> basketOptional = shoppingBaskets.stream()
                .filter(basket -> basket.getShop().getShopId() == shopID).findFirst();

        // create a new basket if the user does not have a basket for this shop.
        ShoppingBasket basket;
        if (basketOptional.isPresent()) {
            basket = basketOptional.get();
        } else {
            basket = new ShoppingBasket(shopFacade.getShopByShopId(shopID));
            basket = basketRepository.save(basket);
        }

        // add the product to the basket.
        try {
            basket.addProductToShoppingBasket(user, productID, quantity);
            if (!basketOptional.isPresent())
                shoppingBaskets.add(basket);
        } catch (ProductOutOfStockExepction e) {
            logger.log(Level.SEVERE, "Product out of stock in shop: " + shopID);
            throw e;
        } catch (ShopPolicyException e) {
            logger.log(Level.SEVERE, "Shop policy exception in shop: " + shopID);
            throw e;
        }

        logger.log(Level.INFO, "Product added to shopping basket: " + productID + " in shop: " + shopID);
        basketRepository.flush();
    }

    // Remove a product from the shopping cart of a user.
    public void removeProduct(Product product, int shopID, int quantity) throws StockMarketException {
        Optional<ShoppingBasket> basketOptional = shoppingBaskets.stream()
                .filter(basket -> basket.getShop().getShopId() == shopID).findFirst();

        if (basketOptional.isPresent()) {
            ShoppingBasket basket = basketOptional.get();
            basket.removeProductFromShoppingBasket(product, quantity);
            logger.log(Level.INFO,
                    "Product removed from shopping basket: " + product.getProductId() + " in shop: " + shopID);
            if (basket.isEmpty()) {
                shoppingBaskets.remove(basket);
                logger.log(Level.INFO, "Shopping basket for shop: " + shopID + " is empty and has been removed.");
            }
        } else {
            logger.log(Level.WARNING, "No shopping basket found for shop: " + shopID);
            throw new StockMarketException(
                    "Trying to remove product from shopping cart, but no shopping basket found for shop: " + shopID);
        }
    }

    // Set the user of the cart.
    @Transient
    public void SetUser(User user) {
        guest = null;
        this.user = user;
        user_or_guest_name = user.getUserName();
    }

    // Get shopping baskets of the cart.
    public List<ShoppingBasket> getShoppingBaskets() {
        return shoppingBaskets;
    }

    // Get a shopping basket by index.
    public ShoppingBasket getShoppingBasket(int i) {
        return getShoppingBaskets().get(i);
    }

    public ShoppingBasket getShoppingBasketByShopId(int shopId) {
        for (ShoppingBasket basket : shoppingBaskets) {
            if (basket.getShop().getShopId() == shopId) {
                return basket;
            }
        }
        return null;
    }

    // for tests
    public void addShoppingBasket(ShoppingBasket basket) {
        shoppingBaskets.add(basket);
    }

    // return all the products in the cart
    public Map<Integer, Product> getProducts() throws StockMarketException {
        Map<Integer, Product> products = new HashMap<Integer, Product>();
        for (ShoppingBasket basket : shoppingBaskets) {
            for (Product product : basket.getProductsList()) {
                products.put(product.getProductId(), product);
            }
        }
        return products;
    }

    // return all the purchases in the cart
    public Map<String, ShoppingBasket> getPurchases() {
        Map<String, ShoppingBasket> purchases = new HashMap<String, ShoppingBasket>();
        for (ShoppingBasket basket : shoppingBaskets) {
            purchases.put(basket.getShop().getShopName(), basket);
        }
        return purchases;
    }

    // return true if the cart has a basket with the given shopID
    public boolean containsKey(int shopID) {
        for (ShoppingBasket basket : shoppingBaskets) {
            if (basket.getShop().getShopId() == shopID) {
                return true;
            }
        }
        return false;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getId() {
        return shoppingCartId;
    }

    public double getTotalPrice() throws StockMarketException {
        double totalPrice = 0;
        for (ShoppingBasket basket : shoppingBaskets) {
            totalPrice += basket.calculateShoppingBasketPrice();
        }
        return totalPrice;
    }

    public List<Integer> getShoppingBasketIdList() {
        List<Integer> shoppingBasketIdList = new ArrayList<>();
        for (ShoppingBasket basket : shoppingBaskets) {
            shoppingBasketIdList.add(basket.getShoppingBasketId());
        }
        return shoppingBasketIdList;
    }

    public void setId(int i) {
        shoppingCartId = i;
    }

    public void setShopFacade(ShopFacade shopFacade) {
        this.shopFacade = shopFacade;
    }

    public void setPaymentMethod(AdapterPaymentImp paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setSupplyMethod(AdapterSupplyImp supplyMethod) {
        this.supplyMethod = supplyMethod;
    }

    public void setShoppingBaskets(List<ShoppingBasket> shoppingBaskets) {
        this.shoppingBaskets = shoppingBaskets;
    }

    public void setShoppingBasketsRepository(InterfaceShoppingBasketRepository basketRepository) {
        this.basketRepository = basketRepository;
    }

    public void setPaymentMocksForGuestCart(ProxyPayment paymentMethod, ProxySupply supplyMethod) {
        this.paymentMethod = paymentMethod;
        this.supplyMethod = supplyMethod;
    }
}
