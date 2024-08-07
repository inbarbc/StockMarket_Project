package DomainTests;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.sql.exec.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import Domain.Entities.Product;
import Domain.Entities.Shop;
import Domain.Entities.ShoppingBasket;
import Domain.Entities.User;
import Domain.Entities.enums.Category;
import Exceptions.ProdcutPolicyException;
import Exceptions.ProductDoesNotExistsException;
import Exceptions.ShopPolicyException;
import Exceptions.StockMarketException;
import Server.notifications.NotificationHandler;

public class ShoppingBasketTests {

    private ShoppingBasket shoppingBasketUnderTest;

    @Mock
    private Shop shopMock;

    @Mock
    private User userMock;

    @Mock
    private NotificationHandler _notificationHandlerMock;

    @BeforeEach
    public void setUp() {
        shopMock = Mockito.mock(Shop.class);
        userMock = Mockito.mock(User.class);
        _notificationHandlerMock = Mockito.mock(NotificationHandler.class);
        shoppingBasketUnderTest = null;
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testAddProductToShoppingBasket_whenProductIsNotInBasket_shouldAddProductToBasket() throws StockMarketException {
        // Arrange
        Product product = new Product( "Product 1", Category.ELECTRONICS, 100.0, shopMock,1);
        when(shopMock.getProductById(1)).thenReturn(product);
        
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);
        
        // Act
        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }
        
        // Assert
        assertEquals(shoppingBasketUnderTest.getProductsList().size() ,1);
    }

    @Test
    public void testAddProductToShoppingBasket_whenProductIsInBasket_shouldTheProductAddedMoreToBasket() throws StockMarketException {
        // Arrange
        Product product = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock, 1);
        when(shopMock.getProductById(1)).thenReturn(product);

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }

        // Act
        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }
        
        // Assert
        assertTrue(shoppingBasketUnderTest.getProductsList().size() == 2);
        assertTrue(shoppingBasketUnderTest.getProductsList().size() == 2);
    }

    @Test
    public void testAddProductToShoppingBasket_whenProductIsNotInShop_shouldThrowProdcutPolicyException() throws StockMarketException {
        // Arrange
        Product product = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock);
        when(shopMock.getProductById(1)).thenReturn(product);
        when(shopMock.getProductById(2)).thenThrow(new ProductDoesNotExistsException("Product not found in shop"));

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 2, 1);
            fail("Expected ProductDoesNotExistsException exception not thrown");
        } catch (Exception e) {
            // Assert
            assertTrue(e.getMessage().equals("Product not found in shop"));
        }
    }

    @Test
    public void testRemoveProductFromShoppingBasket_whenProductIsInBasket_shouldRemoveProductFromBasket() throws StockMarketException {
        // Arrange
        Product product = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock);
        when(shopMock.getProductById(1)).thenReturn(product);

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }

        // Act
        shoppingBasketUnderTest.removeProductFromShoppingBasket(product, 1);
        
        // Assert
        assertEquals(shoppingBasketUnderTest.getProductsList().size() ,0);

    }

    @Test
    public void testRemoveProductFromShoppingBasket_whenProductIsNotInBasket_shouldNotRemoveProductFromBasket() throws StockMarketException {
        // Arrange
        Product product = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock,1);
        when(shopMock.getProductById(1)).thenReturn(product);

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // try {
        //     shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
        // } catch (ProdcutPolicyException e) {
        //     e.printStackTrace();
        //     fail("Unexpected ProdcutPolicyException exception thrown");
        // }

        // Act
        try {
            shoppingBasketUnderTest.removeProductFromShoppingBasket(product, 1);
            fail("Expected ProductDoesNotExistsException exception not thrown");
        } catch (ProductDoesNotExistsException e) {
            e.printStackTrace();
        }
        
        // Assert
        assertEquals(shoppingBasketUnderTest.getProductsList().size() ,0);
    }

    @Test
    public void testCalculateShoppingBasketPrice_whenBasketIsEmpty_shouldReturnZero() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        double actual = 0.0;
        try {
            actual = shoppingBasketUnderTest.calculateShoppingBasketPrice();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception thrown");
        }
        
        // Assert
        assertTrue(actual == 0.0);
    }

    @Test
    public void testCalculateShoppingBasketPrice_whenBasketIsNotEmpty_shouldReturnTotalPrice() throws StockMarketException {
        // Arrange
        Product product1 = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock);
        Product product2 = new Product("Product 2", Category.ELECTRONICS, 200.0, shopMock);
        when(shopMock.getProductById(1)).thenReturn(product1);
        when(shopMock.getProductById(2)).thenReturn(product2);

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 2, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }

        // Act
        double actual = 0.0;
        try {
            actual = shoppingBasketUnderTest.calculateShoppingBasketPrice();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception thrown");
        }
        
        // Assert
        assertTrue(actual == 300.0);
    }

    @Test
    public void testPrintAllProducts_whenBasketIsEmpty_shouldReturnEmptyString() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        String actual = shoppingBasketUnderTest.printAllProducts();
        
        // Assert
        assertTrue(actual.equals(""));
    }

    @Test
    public void testClone_whenBasketIsEmpty_shouldReturnEmptyBasket() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        ShoppingBasket actual = shoppingBasketUnderTest.clone();
        
        // Assert
        try {
            assertTrue(actual.getProductsList().size() == 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected StockMarketException exception thrown");
        }
    }

    @Test
    public void testClone_whenBasketIsNotEmpty_shouldReturnClonedBasket() throws StockMarketException {
        // Arrange
        Product product1 = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock);
        Product product2 = new Product("Product 2", Category.ELECTRONICS, 200.0, shopMock);
        when(shopMock.getProductById(1)).thenReturn(product1);
        when(shopMock.getProductById(2)).thenReturn(product2);

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 2, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }

        // Act
        ShoppingBasket actual = shoppingBasketUnderTest.clone();
        
        // Assert
        assertTrue(actual.getProductsList().size() == 2);
    }

    @Test
    public void testGetShoppingBasketPrice_whenBasketIsEmpty_shouldReturnZero() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        double actual = -1;
        try {
            actual = shoppingBasketUnderTest.getShoppingBasketPrice();
        } catch (StockMarketException e) {
            e.printStackTrace();
            fail("Unexpected StockMarketException exception thrown");
        }
        
        // Assert
        assertTrue(actual == 0.0);
    }

    @Test
    public void testGetShoppingBasketPrice_whenBasketIsNotEmpty_shouldReturnTotalPrice() throws StockMarketException {
        // Arrange
        Product product1 = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock);
        Product product2 = new Product("Product 2", Category.ELECTRONICS, 200.0, shopMock);
        when(shopMock.getProductById(1)).thenReturn(product1);
        when(shopMock.getProductById(2)).thenReturn(product2);

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 2, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }

        // Act
        double actual = -1;
        try {
            actual = shoppingBasketUnderTest.getShoppingBasketPrice();
        } catch (StockMarketException e) {
            e.printStackTrace();
            fail("Unexpected StockMarketException exception thrown");
        }
        
        // Assert
        assertTrue(actual == 300.0);
    }

    @Test
    public void testGetShop_whenBasketIsNotEmpty_shouldReturnShop() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        Shop actual = shoppingBasketUnderTest.getShop();
        
        // Assert
        assertTrue(actual.equals(shopMock));
    }

    @Test
    public void testGetProductsList_whenBasketIsEmpty_shouldReturnEmptyList() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        int actual = -1;
        try {
            actual = shoppingBasketUnderTest.getProductsList().size();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected StockMarketException exception thrown");
        }
        
        // Assert
        assertTrue(actual == 0);
    }

    @Test
    public void testGetProductsList_whenBasketIsNotEmpty_shouldReturnAllProducts() throws StockMarketException {
        // Arrange
        Product product1 = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock);
        Product product2 = new Product("Product 2", Category.ELECTRONICS, 200.0, shopMock);
        when(shopMock.getProductById(1)).thenReturn(product1);
        when(shopMock.getProductById(2)).thenReturn(product2);

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 2, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }

        // Act
        int actual = shoppingBasketUnderTest.getProductsList().size();
        
        // Assert
        assertTrue(actual == 2);
    }

    @Test
    public void testGetProductIdList_whenBasketIsEmpty_shouldReturnEmptyList() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        int actual = shoppingBasketUnderTest.getProductsList().size();
        
        // Assert
        assertTrue(actual == 0);
    }

    @Test
    public void testGetProductIdList_whenBasketIsNotEmpty_shouldReturnAllProductIds() throws StockMarketException {
        // Arrange
        Product product1 = new Product("Product 1", Category.ELECTRONICS, 100.0, shopMock);
        Product product2 = new Product("Product 2", Category.ELECTRONICS, 200.0, shopMock);
        when(shopMock.getProductById(1)).thenReturn(product1);
        when(shopMock.getProductById(2)).thenReturn(product2);

        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        try {
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 1, 1);
            shoppingBasketUnderTest.addProductToShoppingBasket(userMock, 2, 1);
        } catch (ProdcutPolicyException e) {
            e.printStackTrace();
            fail("Unexpected ProdcutPolicyException exception thrown");
        }

        // Act
        int actual = shoppingBasketUnderTest.getProductsList().size();
        
        // Assert
        assertTrue(actual == 2);
    }

    @Test
    public void testGetShopId_whenBasketIsEmpty_shouldReturnZero() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        int actual = shoppingBasketUnderTest.getShopId();
        
        // Assert
        assertTrue(actual == 0);
    }

    @Test
    public void testGetShopId_whenBasketIsNotEmpty_shouldReturnShopId() {
        // Arrange
        shoppingBasketUnderTest = new ShoppingBasket(shopMock);

        // Act
        int actual = shoppingBasketUnderTest.getShopId();
        
        // Assert
        assertTrue(actual == 0);
    }

    @Test
    public void testPurchaseBasket_whenBasketMeetsShopPolicyAndEverythingInStock_thenReturnedTrue() throws StockMarketException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        Shop shop = new Shop("shopName1", "ownerUsername", "bank1", "address1",1);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shop);
        shop.setNotificationHandler(_notificationHandlerMock);
        Product product = new Product( "product1", Category.ELECTRONICS, 100.0, shop,1);
        product.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product);
        Product product2 = new Product("product2", Category.ELECTRONICS, 100.0, shop,2);
        product2.updateProductQuantity(10);
        shop.addProductToShop("ownerUsername", product2);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);

        // Act
        boolean result = shoppingBasket.purchaseBasket("Guest");
        
        // Assert
        assertTrue(result);
        assertEquals(product.getProductQuantity(), 0);
        assertEquals(product2.getProductQuantity(), 9);
    }

    @Test
    public void testPurchaseBasket_whenBasketMeetsShopPolicyAndFirstProductInStockSecondCompletlyNotInStock_thenReturnedFalseAndRestockFirstProduct() throws StockMarketException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        Shop shop = new Shop("shopName1", "ownerUsername", "bank1", "address1",1);
        shop.setNotificationHandler(_notificationHandlerMock);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shop);
        Product product = new Product( "product1", Category.ELECTRONICS, 100.0, shop,1);
        product.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product);
        Product product2 = new Product("product2", Category.ELECTRONICS, 100.0, shop,2);
        product2.updateProductQuantity(0);
        shop.addProductToShop("ownerUsername", product2);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);

        // Act
        boolean result = shoppingBasket.purchaseBasket(buyer.getUserName());
        
        // Assert
        assertFalse(result);
        assertEquals(product.getProductQuantity(), 3);
        assertEquals(product2.getProductQuantity(), 0);
    }

    @Test
    public void testPurchaseBasket_whenBasketMeetsShopPolicyAndFirstProductInStockSecondSomeInStock_thenReturnedFalseAndRestockProducts() throws StockMarketException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        Shop shop = new Shop("shopName1", "ownerUsername", "bank1", "address1",1);
        shop.setNotificationHandler(_notificationHandlerMock);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shop);
        Product product = new Product("product1", Category.ELECTRONICS, 100.0, shop,1);
        product.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product);
        Product product2 = new Product("product2", Category.ELECTRONICS, 100.0, shop,2);
        product2.updateProductQuantity(1);
        shop.addProductToShop("ownerUsername", product2);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);

        // Act
        boolean result = shoppingBasket.purchaseBasket(buyer.getUserName());
        
        // Assert
        assertFalse(result);
        assertEquals(product.getProductQuantity(), 3);
        assertEquals(product2.getProductQuantity(), 1);
    }

    @Test
    public void testPurchaseBasket_whenBasketDoNotMeetsShopPolicyAndEverythingInStock_thenThrowsShopPolicyException() throws StockMarketException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shopMock);
        Product product = new Product("product1", Category.ELECTRONICS, 100.0,shopMock,1);
        product.updateProductQuantity(3);
        shopMock.addProductToShop("ownerUsername", product);
        Product product2 = new Product("product2", Category.ELECTRONICS, 100.0, shopMock,2);
        product2.updateProductQuantity(10);
        shopMock.addProductToShop("ownerUsername", product2);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);
        doThrow(new ShopPolicyException("Basket does not meet shop policy")).when(shopMock).ValidateBasketMeetsShopPolicy(shoppingBasket);

        // Act & Assert
        assertThrows(ShopPolicyException.class, () -> {
            shoppingBasket.purchaseBasket(buyer.getUserName());
        });
        assertEquals(product.getProductQuantity(), 3);
        assertEquals(product2.getProductQuantity(), 10);
    }

    @Test
    public void testPurchaseBasket_whenBasketMeetsShopPolicyAndThereIsEnogthStockForOneBuyer_thenOneReturnTrueAndOneFalse() throws StockMarketException, java.util.concurrent.ExecutionException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        User buyer2 = new User("username2", "password2", "email2", date);
        Shop shop = new Shop("shopName1", "ownerUsername", "bank1", "address1",1);
        shop.setNotificationHandler(_notificationHandlerMock);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shop);
        ShoppingBasket shoppingBasket2 = new ShoppingBasket(shop);
        Product product = new Product("product1", Category.ELECTRONICS, 100.0, shop,1);
        product.updateProductQuantity(2);
        shop.addProductToShop("ownerUsername", product);
        Product product2 = new Product( "product2", Category.ELECTRONICS, 100.0, shop,2);
        product2.updateProductQuantity(1);
        shop.addProductToShop("ownerUsername", product2);

        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);
        shoppingBasket2.addProductToShoppingBasket(buyer2, product.getProductId(), 1);
        shoppingBasket2.addProductToShoppingBasket(buyer2, product2.getProductId(), 1);
        final boolean[] results = new boolean[2];
        ExecutorService executor = Executors.newFixedThreadPool(2); // create a thread pool with 2 threads

        // Act
        // Task for first thread
        Callable<Boolean> task1 = () -> {
            try {
                return shoppingBasket.purchaseBasket(buyer.getUserName());
            } catch (StockMarketException e) {
                return false;
            }
        };

        // Task for second thread
        Callable<Boolean> task2 = () -> {
            try {
                return shoppingBasket2.purchaseBasket(buyer.getUserName());
            } catch (StockMarketException e) {
                return false;
            }
        };

        // Execute tasks
        Future<Boolean> future1 = executor.submit(task1);
        Future<Boolean> future2 = executor.submit(task2);

        try {
            results[0] = future1.get();
            results[1] = future2.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executor.shutdown(); // shut down executor service
        
        // Assert
        assertTrue((results[0] && !results[1]) || (!results[0] && results[1]));
        assertEquals(product.getProductQuantity(), 1);
        assertEquals(product2.getProductQuantity(), 0);
    }

    @Test
    public void testPurchaseBasket_whenBasketMeetsShopPolicyAndThereIsEnogthStockForEveryOne_thenEveryOneReturnTrue() throws StockMarketException, java.util.concurrent.ExecutionException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        User buyer2 = new User("username2", "password2", "email2", date);
        Shop shop = new Shop("shopName1", "ownerUsername", "bank1", "address1",1);
        shop.setNotificationHandler(_notificationHandlerMock);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shop);
        ShoppingBasket shoppingBasket2 = new ShoppingBasket(shop);
        Product product = new Product("product1", Category.ELECTRONICS, 100.0, shop,1);
        product.updateProductQuantity(2);
        shop.addProductToShop("ownerUsername", product);
        Product product2 = new Product("product2", Category.ELECTRONICS, 100.0,shop,2);
        product2.updateProductQuantity(2);
        shop.addProductToShop("ownerUsername", product2);

        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);
        shoppingBasket2.addProductToShoppingBasket(buyer2, product.getProductId(), 1);
        shoppingBasket2.addProductToShoppingBasket(buyer2, product2.getProductId(), 1);
        final boolean[] results = new boolean[2];
        ExecutorService executor = Executors.newFixedThreadPool(2); // create a thread pool with 2 threads

        // Act
        // Task for first thread
        Callable<Boolean> task1 = () -> {
            try {
                return shoppingBasket.purchaseBasket(buyer.getUserName());
            } catch (StockMarketException e) {
                return false;
            }
        };

        // Task for second thread
        Callable<Boolean> task2 = () -> {
            try {
                return shoppingBasket2.purchaseBasket(buyer.getUserName());
            } catch (StockMarketException e) {
                return false;
            }
        };

        // Execute tasks
        Future<Boolean> future1 = executor.submit(task1);
        Future<Boolean> future2 = executor.submit(task2);

        try {
            results[0] = future1.get();
            results[1] = future2.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        executor.shutdown(); // shut down executor service
        
        // Assert
        assertTrue(results[0]&& results[1]);
        assertEquals(product.getProductQuantity(), 0);
        assertEquals(product2.getProductQuantity(), 0);
    }

    @Test
    public void testCancelPurchase_whenWholeBasketNeedToBeCanceled_thenRestockProducts() throws StockMarketException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        Shop shop = new Shop("shopName1", "ownerUsername", "bank1", "address1",1);
        shop.setNotificationHandler(_notificationHandlerMock);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shop);
        Product product = new Product("product1", Category.ELECTRONICS, 100.0, shop,1);
        product.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product);
        Product product2 = new Product("product2", Category.ELECTRONICS, 100.0, shop,2);
        product2.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product2);

        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);

        // Act
        shoppingBasket.cancelPurchase();
        
        // Assert
        assertEquals(product.getProductQuantity(), 4);
        assertEquals(product2.getProductQuantity(), 4);
    }

    @Test
    public void testCancelPurchase_whenTwoBasketWithSharedProductsNeedToBeCanceled_thenRestockProducts() throws StockMarketException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        User buyer2 = new User("username2", "password2", "email2", date);
        Shop shop = new Shop("shopName1", "ownerUsername", "bank1", "address1",1);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shop);
        ShoppingBasket shoppingBasket2 = new ShoppingBasket(shop);
        Product product = new Product("product1", Category.ELECTRONICS, 100.0, shop,1);
        product.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product);
        Product product2 = new Product("product2", Category.ELECTRONICS, 100.0,shop,2);
        product2.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product2);

        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);
        shoppingBasket2.addProductToShoppingBasket(buyer2, product.getProductId(), 1);
        shoppingBasket2.addProductToShoppingBasket(buyer2, product2.getProductId(), 1);

        ExecutorService executor = Executors.newFixedThreadPool(2); // create a thread pool with 2 threads

        // Act
        // Task for first thread
        Runnable task1 = () -> {
            try {
                shoppingBasket.cancelPurchase();
            } catch (StockMarketException e) {
                e.printStackTrace();
            }
        };

        // Task for second thread
        Runnable task2 = () -> {
            try {
                shoppingBasket2.cancelPurchase();
            } catch (StockMarketException e) {
                e.printStackTrace();
            }
        };

        // Execute tasks
        executor.execute(task1);
        executor.execute(task2);

        // Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted
        executor.shutdown();

        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks

                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();

            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        
        // Assert
        assertEquals(product.getProductQuantity(), 5);
        assertEquals(product2.getProductQuantity(), 5);
    }

    @Test
    public void testCalculateShoppingBasketPrice_whenBasketPriceNeedToBeCalculate_thenReturnBasketPriceAfterDiscounts() throws StockMarketException {
        // Arrange
        Date date = new Date();
        date.setTime(0);
        User buyer = new User("username1", "password1", "email1", date);
        Shop shop = new Shop("shopName1", "ownerUsername", "bank1", "address1",1);
        shop.setNotificationHandler(_notificationHandlerMock);
        ShoppingBasket shoppingBasket = new ShoppingBasket(shop);
        Product product = new Product("product1", Category.ELECTRONICS, 100.0, shop,1);
        product.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product);
        Product product2 = new Product("product2", Category.ELECTRONICS, 100.0,shop,2);
        product2.updateProductQuantity(3);
        shop.addProductToShop("ownerUsername", product2);

        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product.getProductId(), 1);
        shoppingBasket.addProductToShoppingBasket(buyer, product2.getProductId(), 1);

        // Act
        double price = shoppingBasket.calculateShoppingBasketPrice();
        
        // Assert
        assertEquals(price, 300);
    }

}
