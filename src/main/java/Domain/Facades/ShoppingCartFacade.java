package Domain.Facades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import Domain.Order;
import Domain.ShoppingBasket;
import Domain.ShoppingCart;
import Domain.User;
import Domain.Repositories.MemoryShoppingCartRepository;
import Domain.Repositories.InterfaceShoppingCartRepository;
import Dtos.BasketDto;
import Dtos.PurchaseCartDetailsDto;
import Exceptions.StockMarketException;
import jakarta.transaction.Transactional;

@Service
public class ShoppingCartFacade {
    private static ShoppingCartFacade _shoppingCartFacade;
    Map<String, ShoppingCart> _guestsCarts; // <guestID, ShoppingCart>
    InterfaceShoppingCartRepository _cartsRepo;
    private static final Logger logger = Logger.getLogger(ShoppingCartFacade.class.getName());

    public ShoppingCartFacade() {
        _guestsCarts = new HashMap<>();
        _cartsRepo = new MemoryShoppingCartRepository();

        // For testing UI
        // try {
        //     initUI();
        // }
        // catch (StockMarketException e) {
        //     e.printStackTrace();
        // }
    }

    public ShoppingCartFacade(InterfaceShoppingCartRepository cartsRepo) {
        _cartsRepo = cartsRepo;
        _guestsCarts = new HashMap<>();

        // For testing UI
        // try {
        //     initUI();
        // }
        // catch (StockMarketException e) {
        //     e.printStackTrace();
        // }
    }

    // Public method to provide access to the _shoppingCartFacade
    public static synchronized ShoppingCartFacade getShoppingCartFacade() {
        if (_shoppingCartFacade == null) {
            _shoppingCartFacade = new ShoppingCartFacade();
        }
        return _shoppingCartFacade;
    }

    // set shopping cart repository to be used in real system
    public void setShoppingCartRepository(InterfaceShoppingCartRepository cartsRepo) {
        _cartsRepo = cartsRepo;
    }

    // Add a cart for a guest by token.
    @Transactional
    public void addCartForGuest(String guestID) {
        ShoppingCart cart = new ShoppingCart();
        _guestsCarts.put(guestID, cart);
    }

    /*
     * Add a cart for a user.
     * If user already has a cart - we will use the same cart as before.
     * If user don't have a cart (Just registerd/ already purchase the cart) - we
     * will use it's guest cart
     */
    @Transactional
    public void addCartForUser(String guestID, User user) {
        if (_cartsRepo.getCartByUsername(user.getUserName()) == null) {
            _cartsRepo.addCartForUser(user.getUserName(), _guestsCarts.get(guestID));
        }
        System.out.println("test"+_cartsRepo.getCartByUsername(user.getUserName()));
        // add the user to the cart
        _cartsRepo.getCartByUsername(user.getUserName()).SetUser(user);
    }

    /*
     * Add a product to a user cart by username.
     * This method called when a user add a product to his cart.
     */
    @Transactional
    public void addProductToUserCart(String userName, int productID, int shopID) throws StockMarketException {
        ShoppingCart cart = _cartsRepo.getCartByUsername(userName);
        if (cart != null) {
            cart.addProduct(productID, shopID);
            logger.log(Level.INFO, "Product added to user's cart: " + userName);
        } else {
            logger.log(Level.WARNING, "User cart not found: " + userName);
        }
    }

    /*
     * Add a product to a guest cart by token.
     * This method called when a guest user add a product to his cart.
     */
    @Transactional
    public void addProductToGuestCart(String guestID, int productID, int shopID) throws StockMarketException {
        ShoppingCart cart = _guestsCarts.get(guestID);
        if (cart != null) {
            cart.addProduct(productID, shopID);
            logger.log(Level.INFO, "Product added to guest's cart: " + guestID);
        } else {
            logger.log(Level.WARNING, "Guest cart not found: " + guestID);
        }
    }

    /*
     * Remove a product from a user cart by username.
     * This method called when a user remove a product from his cart.
     */
    @Transactional
    public void removeProductFromUserCart(String userName, int productID, int shopID) throws StockMarketException {
        ShoppingCart cart = _cartsRepo.getCartByUsername(userName);
        if (cart != null) {
            cart.removeProduct(productID, shopID);
            logger.log(Level.INFO, "Product removed from guest's cart: " + userName);
        } else {
            logger.log(Level.WARNING, "User cart not found: " + userName);
        }
    }

    /*
     * Remove a product from a guest user cart by token.
     * This method called when a guest user remove a product from his cart.
     */
    @Transactional
    public void removeProductFromGuestCart(String guestID, int productID, int shopID) throws StockMarketException {
        ShoppingCart cart = _guestsCarts.get(guestID);
        if (cart != null) {
            cart.removeProduct(productID, shopID);
            logger.log(Level.INFO, "Product removed from guest's cart: " + guestID);
        } else {
            logger.log(Level.WARNING, "Guest cart not found: " + guestID);
        }
    }

    /*
     * Remove a cart from a guest user by token.
     * This method called when a guest user leave the system.
     */
    @Transactional
    public void removeCartForGuest(String guestID) {
        _guestsCarts.remove(guestID);
    }

    /*
     * Remove a cart from a user by username.
     * This method called when a user leave the system.
     */
     @Transactional
    public void purchaseCartGuest(String guestID, PurchaseCartDetailsDto details) throws StockMarketException {
        ArrayList<Integer> allBaskets = new ArrayList<Integer>();

        for (int i = 0; i < _guestsCarts.get(guestID).getCartSize(); i++)
            allBaskets.add(i);
        logger.log(Level.INFO, "Start purchasing cart for guest.");
        details.basketsToBuy = allBaskets;
        _guestsCarts.get(guestID).purchaseCart(details, _cartsRepo.getUniqueOrderID());
    }

    /*
     * Purchase the cart of a user.
     */
    @Transactional
    public void purchaseCartUser(String username, PurchaseCartDetailsDto details) throws StockMarketException {
        logger.log(Level.INFO, "Start purchasing cart for user.");
        _cartsRepo.getCartByUsername(username).purchaseCart(details, _cartsRepo.getUniqueOrderID());
    }

    // Getters
    public Map<String, ShoppingCart> get_guestsCarts() {
        return _guestsCarts;
    }

    /*
     * get user cart.
     * If user already has a cart - we will return the same cart as before.
     * If user don't have a cart (Just registerd/ already purchase the cart) - we
     * will use it's guest cart
     */
    @Transactional
    public ShoppingCart getUserCart(String username) throws StockMarketException {
        if (_cartsRepo.getCartByUsername(username) == null) {
            throw new StockMarketException("user does not have a cart");
        }
        return _cartsRepo.getCartByUsername(username);
    }

    /*
     * get guest cart.
     */
    @Transactional
    public ShoppingCart getGuestCart(String guest) throws StockMarketException {
        if (_guestsCarts.get(guest) == null) {
            throw new StockMarketException("guest does not have a cart");
        }
        return _guestsCarts.get(guest);
    }
  
    // this function checks for the product in the past purchases of the user, and if it exists, it returns the shopID.
    // next, this function will add a review on the product in the shop (if he still exists).
    @SuppressWarnings({ "null" })
    @Transactional
    public void writeReview(String username, List<Order> purchaseHistory, int productID, int shopID, String review) throws StockMarketException {
        // check if the user has purchased the product in the past using purchaseHistory.
        boolean foundProduct = false;
        ShoppingBasket shoppingBasket = null;
        for (Order order : purchaseHistory) {
            Map<Integer, ShoppingBasket> productsByShoppingBasket = order.getProductsByShoppingBasket();
            if (productsByShoppingBasket.containsKey(productID)){
                shoppingBasket = productsByShoppingBasket.get(productID);
                foundProduct = true;
            }
        }
        if (!foundProduct) {
            logger.log(Level.WARNING, "User has not purchased the product in the past.");
            throw new StockMarketException("User has not purchased the product in the past.");
        }
        // check if the shop still exists.
        if (shoppingBasket.getShopId() != shopID) {
            logger.log(Level.WARNING, "Shop does not exist.");
            throw new StockMarketException("Shop does not exist.");
        }
        // add the review.
        shoppingBasket.getShop().addReview(username, productID, review);
    }

    // this function returns the cart of the user by username.
    @Transactional
    public Object getCartByUsername(String username) {
        return _cartsRepo.getCartByUsername(username);
    }

    /*
     * view the shopping cart of a user.
     */
    @Transactional
    public List<BasketDto> viewShoppingCart(String token, String username) throws StockMarketException {
        ShoppingCart cart;

        if (username == null) {
            cart = _guestsCarts.get(token);
        } else {
            cart = _cartsRepo.getCartByUsername(username);
        }
        List<BasketDto> baskets = new ArrayList<>();
        for (ShoppingBasket basket : cart.getShoppingBaskets()) {
            baskets.add(new BasketDto(basket.getShopId(), basket.getProductIdList(), basket.calculateShoppingBasketPrice()));
        }
        return baskets;
    }

    // for tests
    public void addCartForGuestForTests(String guestID, ShoppingCart cart) {
        _guestsCarts.put(guestID, cart);
    }

    // // function to initilaize data for UI testing
    // public void initUI() throws StockMarketException {
    //     ShoppingCart cartUI = new ShoppingCart();
    //     _cartsRepo.addCartForUser("tal", cartUI);
    //     addProductToUserCart("tal", 0, 0);
    //     addProductToUserCart("tal", 0, 0);
    //     addProductToUserCart("tal", 1, 1);
    //     addProductToUserCart("tal", 2, 1);    
    // }
}
