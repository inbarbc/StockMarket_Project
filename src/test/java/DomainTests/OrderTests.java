package DomainTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import Domain.Entities.Order;
import Domain.Entities.Product;
import Domain.Entities.Shop;
import Domain.Entities.ShoppingBasket;
import Domain.Entities.User;
import Domain.Entities.enums.Category;
import Exceptions.ProductDoesNotExistsException;
import Exceptions.StockMarketException;

public class OrderTests {

    @Mock
    private Shop shopMock;

    @Mock
    private User userMock;

    @BeforeEach
    public void setUp() {
        shopMock = mock(Shop.class);
        userMock = mock(User.class);
    }

    @AfterEach
    public void tearDown() {
        shopMock = null;
        userMock = null;
    }

    @SuppressWarnings("null")
    @Test
    public void testGetOrderTotalAmount_whenNoProductsInShoppingBasket_thenReturnZero() {
        // Arrage
        List<ShoppingBasket> shoppingBasket = new ArrayList<>();
        Order orderUnderTest = null;
        try {
            orderUnderTest = new Order(shoppingBasket, 1, 1);
        } catch (StockMarketException e) {
            e.printStackTrace();
            fail("Expected StockMarketException to be thrown");
        }

        // Act
        double result = -1;
        try {
            result = orderUnderTest.getOrderTotalAmount();
        } catch (StockMarketException e) {
            e.printStackTrace();
            fail("Expected StockMarketException to be thrown");
        }

        // Assert
        assertEquals(0.0, result);
    }

    @SuppressWarnings("null")
    @Test
    public void testGetOrderTotalAmount_whenOneProductInShoppingBasket_thenReturnProductPrice() {
        // Arrage
        List<ShoppingBasket> shoppingBasket = new ArrayList<>();
        ShoppingBasket basket = new ShoppingBasket(shopMock);

        Product product = new Product("product1", Category.CLOTHING , 1.0, shopMock);
        try {
            when(shopMock.getProductById(1)).thenReturn(product);
        } catch (ProductDoesNotExistsException e) {
            e.printStackTrace();
            fail("Expected ProductDoesNotExistsException to be thrown");
        }

        try {
            basket.addProductToShoppingBasket(userMock, 1, 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Expected ProdcutPolicyException or PoductDoesNotExistsException to be thrown");
        }
        shoppingBasket.add(basket);
        Order orderUnderTest = null;
        try {
            orderUnderTest = new Order(shoppingBasket, 2, 2);
        } catch (StockMarketException e) {
            e.printStackTrace();
            fail("Expected StockMarketException to be thrown");
        }

        // Act
        double result = -1;
        try {
            result = orderUnderTest.getOrderTotalAmount();
        } catch (StockMarketException e) {
            e.printStackTrace();
            fail("Expected StockMarketException to be thrown");
        }

        // Assert
        assertEquals(1.0, result);
    }

    @SuppressWarnings("null")
    @Test
    public void testGetOrderTotalAmount_whenMultipleProductsInShoppingBasket_thenReturnTotalPrice() {
        // Arrage
        List<ShoppingBasket> shoppingBasket = new ArrayList<>();
        ShoppingBasket basket = new ShoppingBasket(shopMock);

        Product product1 = new Product( "product1", Category.CLOTHING , 1.0, shopMock);
        Product product2 = new Product("product2", Category.ELECTRONICS , 2.0, shopMock);
        try {
            when(shopMock.getProductById(1)).thenReturn(product1);
            when(shopMock.getProductById(2)).thenReturn(product2);
        } catch (ProductDoesNotExistsException e) {
            e.printStackTrace();
            fail("Expected ProductDoesNotExistsException to be thrown");
        }

        try {
            basket.addProductToShoppingBasket(userMock, 1, 1);
            basket.addProductToShoppingBasket(userMock, 2, 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Expected ProdcutPolicyException or PoductDoesNotExistsException to be thrown");
        }
        shoppingBasket.add(basket);
        Order orderUnderTest = null;
        try {
            orderUnderTest = new Order(shoppingBasket, 3, 3);
            // orderUnderTest = new Order(1, shoppingBasket, 3, 3);
        } catch (StockMarketException e) {
            e.printStackTrace();
            fail("Expected StockMarketException to be thrown");
        }

        // Act
        double result = -1;
        try {
            result = orderUnderTest.getOrderTotalAmount();
        } catch (StockMarketException e) {
            e.printStackTrace();
            fail("Expected StockMarketException to be thrown");
        }

        // Assert
        assertEquals(3.0, result);
    }
}
