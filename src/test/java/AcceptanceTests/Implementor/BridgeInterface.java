package AcceptanceTests.Implementor;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import enums.Category;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public interface BridgeInterface {
    
    @BeforeEach
    void init();

    // SYSTEM TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    boolean testOpenMarketSystem(String username);

    @Test
    boolean testPayment(String senario);

    @Test
    boolean testShipping(String senario);

    @Test
    boolean testAddExternalService(String newSerivceName, String informationPersonName, String informationPersonPhone, Integer securityIdForService);

    @Test
    boolean testChangeExternalService(Integer oldServiceSystemId, String newSerivceName, String newInformationPersonName, String newInformationPersonPhone);

    // GUEST TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------
    
    @Test
    boolean TestGuestEnterTheSystem(String shouldSeccess);
    
    @Test
    boolean TestGuestRegisterToTheSystem(String username, String password, String email);
    
    @Test
    boolean testLoginToTheSystem(String username, String password);

    // SHOPPING GUEST TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    boolean testSearchAndDisplayShopByIDAsGuest(String shopId, boolean shopContainsProducts);

    @Test
    boolean testSearchAndDisplayShopByNameAsGuest(String shopName, boolean shopContainsProducts); 

    @Test
    boolean testGetShopInfoAsGuest(String shopId);

    @Test
    boolean testGetProductInfoUsingProductNameAsGuest(String productName);
    
    @Test
    boolean testGetProductInfoUsingProductCategoryAsGuest(Category category);
    
    @Test
    boolean testGetProductInfoUsingKeywordsAsGuest(List<String> keyWords);
    
    @Test
    boolean testGetProductInfoUsingProductNameInShopAsGuest(String productName, String shopId);
    
    @Test
    boolean testGetProductInfoUsingProductCategoryInShopAsGuest(Category category, String shopId);
    
    @Test
    boolean testGetProductInfoUsingKeywordsInShopAsGuest(List<String> keywords, String shopId);

    @Test
    boolean testAddProductToShoppingCartAsGuest(String productId);
    
    @Test
    boolean testCheckAndViewItemsInShoppingCartAsGuest(String status);

    @Test
    boolean testCheckAllOrNothingBuyingShoppingCartGuest(String test, List<Integer> basketsToBuy, String cardNumber, String address);

    @Test
    boolean testCheckAllOrNothingBuyingShoppingCartUser(List<Integer> basketsToBuy, String cardNumber, String address);

    @Test
    boolean testCheckAllOrNothingBuyingShoppingCartGuestThreading(String test, List<Integer> basketsToBuy, String cardNumber, String address);
    
    // SHOPPING USER TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    boolean testSearchAndDisplayShopByIDAsUser(String shopId, boolean shopContainsProducts);

    @Test
    boolean testSearchAndDisplayShopByNameAsUser(String shopName, boolean shopContainsProducts);

    @Test
    boolean testGetShopInfoAsUser(String shopId);
    
    @Test
    boolean testGetProductInfoUsingProductNameAsUser(String productName);

    @Test
    boolean testGetProductInfoUsingProductCategoryAsUser(Category category);
    
    @Test
    boolean testGetProductInfoUsingKeywordsAsUser(List<String> keywords);
        
    @Test
    boolean testGetProductInfoUsingProductNameInShopAsUser(String productName, String shopId);
    
    @Test
    boolean testGetProductInfoUsingProductCategoryInShopAsUser(Category category, String shopId);
    
    @Test
    boolean testGetProductInfoUsingKeywordsInShopAsUser(List<String> keywords, String shopId);

    @Test
    boolean testAddProductToShoppingCartAsUser(String productId, String shopId);
    
    @Test
    boolean testCheckAndViewItemsInShoppingCartAsUser(String status);
    
    @Test
    boolean testCheckBuyingShoppingCartUser(String username, String busketsToBuy, String cardNumber, String address);

    @Test
    boolean testLogoutToTheSystem(String username);
    
    @Test
    boolean TestWhenUserLogoutThenHisCartSaved(String username);
    
    @Test
    boolean TestWhenUserLogoutThenHeBecomeGuest(String username);
    
    @Test
    boolean TestUserOpenAShop(String username, String password, String shopName, String bankDetails, String shopAddress);

    @Test
    boolean TestUserWriteReviewOnPurchasedProduct(String username, String password, String productId);

    @Test
    boolean TestUserRatingPurchasedProduct(String username, String password, String productId, String score);
    
    @Test
    boolean TestUserRatingShopHePurchasedFrom(String username, String password, String shopId, String score);
    
    @Test
    boolean TestUserMessagingShopHePurchasedFrom(String username, String password, String Id, String message);

    @Test
    boolean TestUserReportSystemManagerOnBreakingIntegrityRules(String username, String password, String message);
    
    @Test
    boolean TestUserViewHistoryPurchaseList(String username, String password);
    
    @Test
    boolean TestUserViewHistoryPurchaseListWhenProductRemovedFromSystem(String username, String password, String productId);

    @Test
    boolean TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem(String username, String password, String shopId);
    
    @Test
    boolean TestUserViewPrivateDetails(String username, String password);
    
    @Test
    boolean TestUserEditPrivateDetails(String username, String newPassword, String newEmail);

    // SHOP OWNER TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------
    
    @Test
    boolean testShopOwnerAddProductToShop(String username, String shopId, String productName, String productAmount);
    
    @Test
    boolean testShopOwnerRemoveProductFromShop(String username, String shopId, String productName);
    
    @Test
    boolean testShopOwnerEditProductInShop(String username, String shopId, String productName, String productNameNew, String productAmount, String productAmountNew);
    
    @Test
    boolean testShopOwnerChangeShopPolicies(String username, String shopId, String newPolicy);
    
    @Test
    boolean testShopOwnerAppointAnotherShopOwner(String username, String shopId, String newOwnerUsername);
    
    @Test
    boolean testShopOwnerAppointAnotherShopManager(String username, String shopId, String newManagerUsername);
    
    @Test
    boolean testShopOwnerAddShopManagerPermission(String username, String shopId, String managerUsername, String permission);
    
    @Test
    boolean testShopOwnerRemoveShopManagerPermission(String username, String shopId, String managerUsername, String permission);
    
    @Test
    boolean testShopOwnerCloseShop(String username, String shopId);
    
    @Test
    boolean testShopOwnerGetShopInfo(String username, String shopId);
    
    @Test
    boolean testShopOwnerGetShopManagersPermissions(String username, String shopId);
    
    @Test
    boolean testShopOwnerViewHistoryPurcaseInShop(String username, String shopId);
    
    // STORE MANAGER TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    boolean testPermissionForShopManager(String username, Integer shopId, String permission);

    // SYSTEM ADMIN TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    boolean testSystemManagerViewHistoryPurcaseInUsers(String managerName, String userName);

    @Test
    boolean testSystemManagerViewHistoryPurcaseInShops(String userName, Integer shopId);

    // SHOPPING CART TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    boolean testAddProductToShoppingCartUser(String username, String productId, String shopId);

    @Test
    boolean testAddProductToShoppingCartGuest(String username, String productId, String shopId);
}