package AcceptanceTests.ProjectTests;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import AcceptanceTests.Implementor.BridgeInterface;
import AcceptanceTests.Implementor.RealBridge;
import enums.Category;

@ExtendWith(RealBridge.class)

public class ShoppingGuestAcceptanceTests {

    // Fields.
    private BridgeInterface _bridge;

    // constructor.
    public ShoppingGuestAcceptanceTests(RealBridge bridge) {
        _bridge = bridge;
    }

    // Test search a shop and display its products by shop ID as a Guest in the system.
    @Test
    public void testSearchAndDisplayShopByIDAsGuest() {
        assertTrue(_bridge.testSearchAndDisplayShopByIDAsGuest("0", true) ); // success - exist shop, has products
        assertTrue(_bridge.testSearchAndDisplayShopByIDAsGuest("0", false) ); // success - exist shop, no products
        assertFalse(_bridge.testSearchAndDisplayShopByIDAsGuest("1", false) ); // fail - non exist shop, no products
    }

    // Test search a shop and display its products by shop name as a Guest in the system.
    @Test
    public void testSearchAndDisplayShopByNameAsGuest() {
        assertTrue(_bridge.testSearchAndDisplayShopByNameAsGuest("shopName1", true) ); // success - exist shop, has products
        assertTrue(_bridge.testSearchAndDisplayShopByNameAsGuest("shopName1", false) ); // success - exist shop, no products
        assertFalse(_bridge.testSearchAndDisplayShopByNameAsGuest("shopName2", false) ); // fail - no exist shop, no products
    }

    // Test get information about a shop as a guest in the system.
    @Test
    public void testGetShopInfoAsGuest() {
        assertTrue(_bridge.testGetShopInfoAsGuest("0")); // success - exist shop
        assertFalse(_bridge.testGetShopInfoAsGuest("1")); // fail - non exist shop
    }
    
    // Test search product information according to product name as a guest in the system.
    @Test
    public void testGetProductInfoUsingProductNameAsGuest() {
        assertTrue(_bridge.testGetProductInfoUsingProductNameAsGuest("productName1") ); // success - exist product
        assertFalse(_bridge.testGetProductInfoUsingProductNameAsGuest("productName2") ); // fail - non exist product
    }

    // Test search product information according to product category as a guest in the system.
    @Test
    public void testGetProductInfoUsingProductCategoryAsGuest() {
        assertTrue(_bridge.testGetProductInfoUsingProductCategoryAsGuest(Category.CLOTHING) ); // success - exist category
        assertFalse(_bridge.testGetProductInfoUsingProductCategoryAsGuest(Category.GROCERY) ); // fail - non exist category
    }

    // Test search product information according to key words as a guest in the system.
    @Test
    public void testGetProductInfoUsingKeywordsAsGuest() {
        assertTrue(_bridge.testGetProductInfoUsingKeywordsAsGuest(List.of("keyword1"))); // success - exist key word
        assertTrue(_bridge.testGetProductInfoUsingKeywordsAsGuest(List.of("keyword1", "keyword2"))); // success - one key word exist
                                                                                      // and one not
        assertFalse(_bridge.testGetProductInfoUsingKeywordsAsGuest(List.of("keyword2"))); // fail - non exist key word
    }

    // TODO: VERSION 2: add tests for filter out products by there price range,
    // rating, category, and store rating.
    // TODO: GILI
    
    // Test search product information in a specific shop, according to product name as a guest in the system.
    @Test
    public void testGetProductInfoUsingProductNameInShopAsGuest() {
        assertTrue(_bridge.testGetProductInfoUsingProductNameInShopAsGuest("productName1", "0") ); // success - exist product and exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductNameInShopAsGuest("productName2", "0") ); // fail - non exist product but exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductNameInShopAsGuest("productName1", "1") ); // fail - exist product but non exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductNameInShopAsGuest("productName2", "1") ); // fail - non exist product and non exist shop
    }

    // Test search product information in a specific shop, according to product category as a guest in the system.
    @Test
    public void testGetProductInfoUsingProductCategoryInShopAsGuest() {
        assertTrue(_bridge.testGetProductInfoUsingProductCategoryInShopAsGuest(Category.CLOTHING, "0")); // success - exist category and exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductCategoryInShopAsGuest(Category.GROCERY, "0")); // fail - non exist category but exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductCategoryInShopAsGuest(Category.CLOTHING, "1")); // fail - exist category but non exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductCategoryInShopAsGuest(Category.GROCERY, "1")); // fail - non exist category and non exist shop
    }

    // Test search product information in a specific shop, according to key words as a guest in the system.
    @Test
    public void testGetProductInfoUsingKeywordsInShopAsGuest() {
        assertTrue(_bridge.testGetProductInfoUsingKeywordsInShopAsGuest(List.of("keyword1"), "0")); // success - exist keyword and exist shop
        assertFalse(_bridge.testGetProductInfoUsingKeywordsInShopAsGuest(List.of("keyword2"), "0")); // fail - non exist keyword but exist shop
        assertFalse(_bridge.testGetProductInfoUsingKeywordsInShopAsGuest(List.of("keyword1"), "1")); // fail - exist keyword but non exist shop
        assertFalse(_bridge.testGetProductInfoUsingKeywordsInShopAsGuest(List.of("keyword2"), "1")); // fail - non exist keyword and non exist shop
    }

    // Test a guest can watch his items in the shopping cart.
    @Test
    public void testCheckAndViewItemsInShoppingCartAsGuest() {
        assertTrue(_bridge.testCheckAndViewItemsInShoppingCartAsGuest("seccess")); // success
        assertFalse(_bridge.testCheckAndViewItemsInShoppingCartAsGuest("fail")); // fail
    }

    // Test the buying senerio of a shopping cart (all or nothing) as a guest.
    // TODO: TAL
    @Test
    public void testBuyingShoppingCartAsGuest() {
        assertTrue(_bridge.testCheckAllOrNothingBuyingShoppingCartGuest("success", new ArrayList<Integer>(), "123456789", "address")); // success - all products are available to buy them
        assertFalse(_bridge.testCheckAllOrNothingBuyingShoppingCartGuest("fail", new ArrayList<Integer>(), "123456789", "address")); // fail - one of the pruducts (or more) is not available
        assertTrue(_bridge.testCheckAllOrNothingBuyingShoppingCartGuestThreading("success", new ArrayList<Integer>(), "123456789", "address")); // success - all products are available to buy only for 1 guest.
        assertFalse(_bridge.testCheckAllOrNothingBuyingShoppingCartGuestThreading("fail", new ArrayList<Integer>(), "123456789", "address")); // fail - some products are not available to buy for any guest.
    }

    // Test the buying senerio of a shopping cart (all or nothing) as a user.
    @Test
    public void testBuyingShoppingCartAsUser() {
        assertTrue(_bridge.testCheckAllOrNothingBuyingShoppingCartUser(new ArrayList<Integer>(List.of(0, 1)), "123456789", "address")); // success - all products are available to buy them
        assertFalse(_bridge.testCheckAllOrNothingBuyingShoppingCartUser(new ArrayList<Integer>(List.of(0, 2)), "123456789", "address")); // fail - one of the pruducts (or more) is not available
        // assertTrue(_bridge.testBuyingShoppingCartPoliciesUser() ); // success - all
        // shop policies are valid
        // assertFalse(_bridge.testBuyingShoppingCartPoliciesUser() ); // fail - one of
        // the shop policies (or more) is not available
    }

}
