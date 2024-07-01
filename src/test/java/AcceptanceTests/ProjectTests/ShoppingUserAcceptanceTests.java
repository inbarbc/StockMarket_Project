package AcceptanceTests.ProjectTests;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import AcceptanceTests.Implementor.BridgeInterface;
import AcceptanceTests.Implementor.RealBridge;
import enums.Category;

@ExtendWith(RealBridge.class)
public class ShoppingUserAcceptanceTests{

    // Fields.
    private BridgeInterface _bridge;

    // constructor.
    public ShoppingUserAcceptanceTests(RealBridge bridge) {
        _bridge = bridge;
    }

    @BeforeEach
    public void setUp() {
        _bridge.init(); // Ensure mocks are initialized
    }
    
    // Test get search a shop and display its products by shop ID as a User in the system.
    @Test
    public void testSearchAndDisplayShopByIDAsUser() {
        assertTrue(_bridge.testSearchAndDisplayShopByIDAsUser("0", true)); // success - exist shop, has products
        assertTrue(_bridge.testSearchAndDisplayShopByIDAsUser("0", false)); // success - exist shop, no products
        assertFalse(_bridge.testSearchAndDisplayShopByIDAsUser("1", false) ); // fail - non exist shop, no products
    }

    // Test search a shop and display its products by shop name as a User in the system.
    @Test
    public void testSearchAndDisplayShopByNameAsUser() {
        assertTrue(_bridge.testSearchAndDisplayShopByNameAsUser("shopName1", true)); // success - exist shop, has products
        assertTrue(_bridge.testSearchAndDisplayShopByNameAsUser("shopName1", false)); // success - exist shop, no products
        assertFalse(_bridge.testSearchAndDisplayShopByNameAsUser("shopName2", false) ); // fail - non exist shop, no products
    }

    // Test get information about a shop as a User in the system.
    @Test
    public void testGetShopInfoAsUser() {
        assertTrue(_bridge.testGetShopInfoAsUser("0") ); // success - exist shop
        assertFalse(_bridge.testGetShopInfoAsUser("1") ); // fail - non exist shop
    }
    
    // Test search product information according to product name as a User in the system.
    @Test
    public void testGetProductInfoUsingProductNameAsUser() {
        assertTrue(_bridge.testGetProductInfoUsingProductNameAsUser("productName1") ); // success - exist product
        assertFalse(_bridge.testGetProductInfoUsingProductNameAsUser("productName2") ); // fail - non exist product
    }
    
    // Test search product information according to product category as a User in the system.
    @Test
    public void testGetProductInfoUsingProductCategoryAsUser() {
        assertTrue(_bridge.testGetProductInfoUsingProductCategoryAsUser(Category.CLOTHING) ); // success - exist category
        assertFalse(_bridge.testGetProductInfoUsingProductCategoryAsUser(Category.GROCERY) ); // fail - non exist category
    }
    
    // Test search product information according to key words as a User in the system.
    @Test
    public void testGetProductInfoUsingKeywordsAsUser() {
        assertTrue(_bridge.testGetProductInfoUsingKeywordsAsUser(List.of("keyword1"))); // success - exist key word
        assertTrue(_bridge.testGetProductInfoUsingKeywordsAsUser(List.of("keyword1", "keyword2"))); // success - one key word exist and one not
        assertFalse(_bridge.testGetProductInfoUsingKeywordsAsUser(List.of("keyword2"))); // fail - non exist key word
    }
    
    // TODO: VERSION 2: add tests for filter out products by there price range, rating, category, and store rating.
    // TODO: GILI - need to implement this test
    
    // Test search product information in a specific shop, according to product name as a User in the system.
    @Test
    public void testGetProductInfoUsingProductNameInShopAsUser() {
        assertTrue(_bridge.testGetProductInfoUsingProductNameInShopAsUser("productName1", "0") ); // success - exist product and exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductNameInShopAsUser("productName2", "0") ); // fail - non exist product but exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductNameInShopAsUser("productName1", "1") ); // fail - exist product but non exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductNameInShopAsUser("productName2", "1") ); // fail - non exist product and non exist shop
    }
    
    // Test search product information in a specific shop, according to product category as a User in the system.
    @Test
    public void testGetProductInfoUsingProductCategoryInShopAsUser() {
        assertTrue(_bridge.testGetProductInfoUsingProductCategoryInShopAsUser(Category.CLOTHING, "0") ); // success - exist category and exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductCategoryInShopAsUser(Category.GROCERY, "0") ); // fail - non exist category but exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductCategoryInShopAsUser(Category.CLOTHING, "1") ); // fail - exist category but non exist shop
        assertFalse(_bridge.testGetProductInfoUsingProductCategoryInShopAsUser(Category.GROCERY, "1") ); // fail - non exist category and non exist shop
    }
    
    // Test search product information in a specific shop, according to key words as a User in the system.
    @Test
    public void testGetProductInfoUsingKeywordsInShopAsUser() {
        assertTrue(_bridge.testGetProductInfoUsingKeywordsInShopAsUser(List.of("keyword1"), "0") ); // success - exist keyword and exist shop
        assertFalse(_bridge.testGetProductInfoUsingKeywordsInShopAsUser(List.of("keyword2"), "0") ); // fail - non exist keyword but exist shop
        assertFalse(_bridge.testGetProductInfoUsingKeywordsInShopAsUser(List.of("keyword1"), "1") ); // fail - exist keyword but non exist shop
        assertFalse(_bridge.testGetProductInfoUsingKeywordsInShopAsUser(List.of("keyword2"), "1") ); // fail - non exist keyword and non exist shop
    }
    
    // Test when add product to shopping cart- it stays there as a User in the system.
    @Test
    public void testAddProductToShoppingCartAsUser() {
        assertTrue(_bridge.testAddProductToShoppingCartAsUser("0", "0") ); // success
        assertFalse(_bridge.testAddProductToShoppingCartAsUser("1", "1") ); // fail
    }
    
    // Test a User can watch his items in the shopping cart as a User in the system.
    @Test
    public void testCheckAndViewItemsInShoppingCartAsUser() {
        assertTrue(_bridge.testCheckAndViewItemsInShoppingCartAsUser("success") ); // success
        assertFalse(_bridge.testCheckAndViewItemsInShoppingCartAsUser("fail") ); // fail
    }
    
    // Test the buying senerio of a shopping cart (all or nothing) as a User in the system.
    @Test
    public void testBuyingShoppingCartAsUser() {
        assertTrue(_bridge.testCheckBuyingShoppingCartUser("bob","0","Visa","Israel") ); // success - all products are available to buy them
        assertFalse(_bridge.testCheckBuyingShoppingCartUser("Tomer","1","Cal","Israel") ); // fail - one of the pruducts (or more) is not available
    }

    // Test if the user can logout from the system.
    @Test
    public void TestUserLogout() {
        assertTrue(_bridge.testLogoutToTheSystem("Bob") ); // success
        assertFalse(_bridge.testLogoutToTheSystem("notUsername")); // not a user in the system
    }
    
    // Test if the user logouts from the system - his shopping cart we saved in the system.
    @Test
    public void TestWhenUserLogoutThenHisCartSaved() {
        assertTrue(_bridge.TestWhenUserLogoutThenHisCartSaved("username") ); // success - his shopping cart saved
    }
    
    // Test if the user logouts from the system - he become a guest in the system.
    @Test
    public void TestWhenUserLogoutThenHeBecomeGuest() {
        assertTrue(_bridge.TestWhenUserLogoutThenHeBecomeGuest("Bob") ); // success - user logged out and become guest
        assertFalse(_bridge.TestWhenUserLogoutThenHeBecomeGuest("notUsername")); // not a user in the system
    }
    
    // Test that a user can open a shop and be the founder of the shop.
    @Test
    public void TestUserOpenAShop() {
        assertTrue(_bridge.TestUserOpenAShop("Bob","bobspassword", "shopName1", "Vias", "Israel") ); // success - user open a shop
        assertFalse(_bridge.TestUserOpenAShop("Tom","bobspassword", "shopName2", "MasterCard", "USA") ); // fail - user is a guest
    }
    
    // Test that a user can open write a review about the product he purchased.
    @Test
    public void TestUserWriteReviewOnPurchasedProduct() {
        assertTrue(_bridge.TestUserWriteReviewOnPurchasedProduct("bob","bobspassword", "0") ); // success - the user secceeded to write a review
        assertFalse(_bridge.TestUserWriteReviewOnPurchasedProduct("bob","bobspassword", "2") ); // fail - the user did not porchased this product
    }
    
    // Test that a user can rate a product he purchased.
    @Test
    public void TestUserRatingPurchasedProduct() {
        assertTrue(_bridge.TestUserRatingPurchasedProduct("bob","bobspassword", "0", "5") ); // success - the user secceeded to rate the product
        assertFalse(_bridge.TestUserRatingPurchasedProduct("bob","bobspassword", "0", "11") ); // fail - the score in invalid
    }
    
    // Test that a user can rate a shop he purchased from.
    @Test
    public void TestUserRatingShopHePurchasedFrom() {
        assertTrue(_bridge.TestUserRatingShopHePurchasedFrom("bob","bobspassword", "0", "4") ); // success - the user secceeded to rate the shop
        assertFalse(_bridge.TestUserRatingShopHePurchasedFrom("bob","bobspassword", "0", "30") ); // fail - the score in invalid
    }
    
    // Test that a user can send messages to the shop the purchased from about his orders.
    @Disabled("FOR VERSOIN 2 ~ This test is disabled cuase needs to implement in real bridge")
    // TODO: METAR - need to implement this test
    @Test
    public void TestUserMessagingShopHePurchasedFrom() {
        assertTrue(_bridge.TestUserMessagingShopHePurchasedFrom("bob","bobspassword", "shop1", "message1") ); // success - the user secceeded to send the message
        assertFalse(_bridge.TestUserMessagingShopHePurchasedFrom("bob","bobspassword", "shop1", "message1") ); // fail - the score in invalid
        assertFalse(_bridge.TestUserMessagingShopHePurchasedFrom("bob","bobspassword", "shop2", "message1") ); // fail - the user didnot purchased from this shop
    }
    
    // Test that a user can report the system manager in case of breaking the integrity rules.
    @Disabled("FOR VERSOIN 2 ~ This test is disabled cuase needs to implement in real bridge")
    // TODO: METAR - need to implement this test
    @Test
    public void TestUserReportSystemManagerOnBreakingIntegrityRules() {
        assertTrue(_bridge.TestUserReportSystemManagerOnBreakingIntegrityRules("bob","bobspassword", "message1") ); // success - the user secceeded to send the message
        assertFalse(_bridge.TestUserReportSystemManagerOnBreakingIntegrityRules("bob","bobspassword", "message1") ); // fail - the message in invalid
    }
    
    // Test that a user can see his own history shopping orders.
    @Test
    public void TestUserViewHistoryPurchaseList() {
        assertTrue(_bridge.TestUserViewHistoryPurchaseList("bob","bobspassword") ); // success - the user secceeded to see his history purchased list
        assertTrue(_bridge.TestUserViewHistoryPurchaseListWhenProductRemovedFromSystem("bob","bobspassword", "0") ); // success - the product exsist in the history purchased list
        assertTrue(_bridge.TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem("bob","bobspassword", "0") ); // success - the shop products exsist in the history purchased list
    }
    
    // Test that a user can see his own private details.
    @Test
    public void TestUserViewPrivateDetails() {
        assertTrue(_bridge.TestUserViewPrivateDetails("bob","bobspassword") ); // success - the user secceeded to see his private details
        //assertFalse(_bridge.TestUserViewPrivateDetails("dad","dadspassword") ); // fail - the user did not exsist in the system
    }
    
    
    // Test that a user can edit his own private details.
    @Test
    public void TestUserEditPrivateDetails() {
        assertTrue(_bridge.TestUserEditPrivateDetails("bob","bobspassword", "newemail@example.com") ); // success - the user secceeded to edit his email
        assertTrue(_bridge.TestUserEditPrivateDetails("bob","newPassword", "email@example.com") ); // success - the user secceeded to edit his password
        //assertFalse(_bridge.TestUserEditPrivateDetails("newName","bobspassword", "email@example.com") ); // fail - the user can not change his user name in the system (this user name is not exsist in the system)
    }
}
