package Domain.Facades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.RestController;

import Domain.ShoppingCart;
import Domain.User;
import Domain.Repositories.MemoryShoppingCartRepository;
import Domain.Repositories.ShoppingCartRepositoryInterface;
import Dtos.PurchaseCartDetailsDto;
import Exceptions.StockMarketException;

@RestController
public class ShoppingCartFacade {
    private static ShoppingCartFacade _shoppingCartFacade;
    Map<String, ShoppingCart> _guestsCarts; // <guestID, ShoppingCart>
    ShoppingCartRepositoryInterface _cartsRepo;
    private static final Logger logger = Logger.getLogger(ShoppingCartFacade.class.getName());

    public ShoppingCartFacade() {
        _guestsCarts = new HashMap<>();
        _cartsRepo = new MemoryShoppingCartRepository();
    }

    // only for tests!
    public ShoppingCartFacade(ShoppingCartRepositoryInterface cartsRepo) {
        _guestsCarts = new HashMap<>();
        _cartsRepo = cartsRepo;
    }

    // Public method to provide access to the _shoppingCartFacade
    public static synchronized ShoppingCartFacade getShoppingCartFacade() {
        if (_shoppingCartFacade == null) {
            _shoppingCartFacade = new ShoppingCartFacade();
        }
        return _shoppingCartFacade;
    }

    /*
     * Add a cart for a guest by token.
     */
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
    public void addCartForUser(String guestID, User user) {
        if (_cartsRepo.getCartByUsername(user.getUserName()) == null) {
            _cartsRepo.addCartForUser(user.getUserName(), _guestsCarts.get(guestID));
        }

        // add the user to the cart
        _cartsRepo.getCartByUsername(user.getUserName()).SetUser(user);
    }

    /*
     * Add a product to a user cart by username.
     * This method called when a user add a product to his cart.
     */
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
    public void removeCartForGuest(String guestID) {
        _guestsCarts.remove(guestID);
    }

    public void purchaseCartGuest(String guestID, PurchaseCartDetailsDto details) throws StockMarketException {
        ArrayList<Integer> allBaskets = new ArrayList<Integer>();

        for (int i = 0; i < _guestsCarts.get(guestID).getCartSize(); i++)
            allBaskets.add(i + 1);
        logger.log(Level.INFO, "Start purchasing cart for guest.");
        details.basketsToBuy = allBaskets;
        _guestsCarts.get(guestID).purchaseCart(details, _cartsRepo.getUniqueOrderID());
    }

    /*
     * Purchase the cart of a user.
     */
    public void purchaseCartUser(String username, PurchaseCartDetailsDto details) throws StockMarketException {
        logger.log(Level.INFO, "Start purchasing cart for user.");
        _cartsRepo.getCartByUsername(username).purchaseCart(details, _cartsRepo.getUniqueOrderID());
    }

    public Map<String, ShoppingCart> get_guestsCarts() {
        return _guestsCarts;
    }

    // this function checks for the product in the past purchases of the user, and if it exists, it returns the shopID.
    // next, this function will add a review on the product in the shop (if he still exists).
    @SuppressWarnings("unlikely-arg-type")
    public void writeReview(String username, int productID, int shopID, String review) throws StockMarketException {
        // check if the user has purchased the product in the past.
        if (!_cartsRepo.getCartByUsername(username).getPurchases().containsKey(productID)) {
            logger.log(Level.WARNING, "User has not purchased the product in the past.");
            throw new StockMarketException("User has not purchased the product in the past.");
        }
        // check if the shop still exists.
        if (_cartsRepo.getCartByUsername(username).getPurchases().get(productID).getShopId() != shopID) {
            logger.log(Level.WARNING, "Shop does not exist.");
            throw new StockMarketException("Shop does not exist.");
        }
        // add the review.
        _cartsRepo.getCartByUsername(username).getPurchases().get(productID).getShop().addReview(username, productID, review);
    }
}
