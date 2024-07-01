package AcceptanceTests.Implementor;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

import enums.Category;

// Proxy is a structural design pattern that lets you provide a substitute or placeholder for another object.
// A proxy controls access to the original object, allowing you to perform something either before or after the request gets through to the original object.
public class ProxyBridge implements BridgeInterface{
    
    // IMPORTANT: all the functions will return false because the real implementation is in the RealBridge class

    
    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Override
    public boolean testOpenMarketSystem(String username) {
        throw new UnsupportedOperationException("Unimplemented method 'testOpenMarketSystem' in ProxyBridge class");
    }

    @Override
    public boolean testPayment(String senario) {
        throw new UnsupportedOperationException("Unimplemented method 'testPayment' in ProxyBridge class");
    }

    @Override
    public boolean testShipping(String senario) {
        throw new UnsupportedOperationException("Unimplemented method 'testShipping' in ProxyBridge class");
    }

    @Override
    public boolean TestGuestEnterTheSystem(String shouldSeccess) {
        throw new UnsupportedOperationException("Unimplemented method 'TestGuestEnterTheSystem' in ProxyBridge class"); 
    }

    @Override
    public boolean TestGuestRegisterToTheSystem(String username, String password, String email) {
        throw new UnsupportedOperationException("Unimplemented method 'TestGuestRegisterToTheSystem' in ProxyBridge class");
    }

    @Override
    public boolean testLoginToTheSystem(String username, String password) {
        throw new UnsupportedOperationException("Unimplemented method 'testLoginToTheSystem' in ProxyBridge class");
    }

    @Override
    public boolean testSearchAndDisplayShopByIDAsGuest(String shopId, boolean shopContainsProducts) {
        throw new UnsupportedOperationException("Unimplemented method 'testSearchAndDisplayShopByIDAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testSearchAndDisplayShopByNameAsGuest(String shopName, boolean shopContainsProducts) {
        throw new UnsupportedOperationException("Unimplemented method 'testSearchAndDisplayShopByNameAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testGetShopInfoAsGuest(String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetShopInfoAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingProductNameAsGuest(String productId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingProductNameAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingProductCategoryAsGuest(Category category) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingProductCategoryAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingKeywordsAsGuest(List<String> keyWords) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingKeywordsAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingProductNameInShopAsGuest(String productId, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingProductNameInShopAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingProductCategoryInShopAsGuest(Category category, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingProductCategoryInShopAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingKeywordsInShopAsGuest(List<String> keywords, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingKeywordsInShopAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testAddProductToShoppingCartAsGuest(String productId) {
        throw new UnsupportedOperationException("Unimplemented method 'testAddProductToShoppingCartAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testCheckAndViewItemsInShoppingCartAsGuest(String status) {
        throw new UnsupportedOperationException("Unimplemented method 'testCheckAndViewItemsInShoppingCartAsGuest' in ProxyBridge class");
    }

    @Override
    public boolean testCheckAllOrNothingBuyingShoppingCartGuest(String test, List<Integer> basketsToBuy, String cardNumber, String address) {
        throw new UnsupportedOperationException("Unimplemented method 'testCheckAllOrNothingBuyingShoppingCartGuest' in ProxyBridge class");
    }

    @Override
    public boolean testCheckAllOrNothingBuyingShoppingCartUser(List<Integer> basketsToBuy, String cardNumber, String address) {
        throw new UnsupportedOperationException("Unimplemented method 'testCheckAllOrNothingBuyingShoppingCartUser' in ProxyBridge class");
    }

    @Override
    public boolean testCheckAllOrNothingBuyingShoppingCartGuestThreading(String test, List<Integer> basketsToBuy, String cardNumber, String address) {
        throw new UnsupportedOperationException("Unimplemented method 'testCheckAllOrNothingBuyingShoppingCartGuestTherding' in ProxyBridge class");
    }

    @Override
    public boolean testSearchAndDisplayShopByIDAsUser(String shopId, boolean shopContainsProducts) {
        throw new UnsupportedOperationException("Unimplemented method 'testSearchAndDisplayShopByIDAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testSearchAndDisplayShopByNameAsUser(String shopName, boolean shopContainsProducts) {
        throw new UnsupportedOperationException("Unimplemented method 'testSearchAndDisplayShopByNameAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testGetShopInfoAsUser(String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetShopInfoAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingProductNameAsUser(String productId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingProductNameAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingProductCategoryAsUser(Category category) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingProductCategoryAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingKeywordsAsUser(List<String> keyWords) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingKeywordsAsUser' in ProxyBridge class");
    }


    @Override
    public boolean testGetProductInfoUsingProductNameInShopAsUser(String productId, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingProductNameInShopAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingProductCategoryInShopAsUser(Category category, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingProductCategoryInShopAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testGetProductInfoUsingKeywordsInShopAsUser(List<String> keywords, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testGetProductInfoUsingKeywordsInShopAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testAddProductToShoppingCartAsUser(String productId, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testAddProductToShoppingCartAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testCheckAndViewItemsInShoppingCartAsUser(String status) {
        throw new UnsupportedOperationException("Unimplemented method 'testCheckAndViewItemsInShoppingCartAsUser' in ProxyBridge class");
    }

    @Override
    public boolean testCheckBuyingShoppingCartUser(String username, String busketsToBuy, String cardNumber, String address) {
        throw new UnsupportedOperationException("Unimplemented method 'testCheckAllOrNothingBuyingShoppingCartUser' in ProxyBridge class");
    }

    @Override
    public boolean testLogoutToTheSystem(String username) {
        throw new UnsupportedOperationException("Unimplemented method 'testLogoutToTheSystem' in ProxyBridge class");
    }

    @Override
    public boolean TestWhenUserLogoutThenHisCartSaved(String username) {
        throw new UnsupportedOperationException("Unimplemented method 'TestWhenUserLogoutThenHisCartSaved' in ProxyBridge class");
    }

    @Override
    public boolean TestWhenUserLogoutThenHeBecomeGuest(String username) {
        throw new UnsupportedOperationException("Unimplemented method 'TestWhenUserLogoutThenHeBecomeGuest' in ProxyBridge class");
    }

    @Override
    public boolean TestUserOpenAShop(String username, String password, String shopName, String bankDetails, String shopAddress) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserOpenAShop' in ProxyBridge class");
    }

    @Override
    public boolean TestUserWriteReviewOnPurchasedProduct(String username, String password, String productId) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserWriteReviewOnPurchasedProduct' in ProxyBridge class");
    }

    @Override
    public boolean TestUserRatingPurchasedProduct(String username, String password, String productId, String score) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserRatingPurchasedProduct' in ProxyBridge class");
    }

    @Override
    public boolean TestUserRatingShopHePurchasedFrom(String username, String password, String shopId, String score) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserRatingShopHePurchasedFrom' in ProxyBridge class");
    }

    @Override
    public boolean TestUserMessagingShopHePurchasedFrom(String username, String password, String Id, String message) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserMessagingShopHePurchasedFrom' in ProxyBridge class");
    }

    @Override
    public boolean TestUserReportSystemManagerOnBreakingIntegrityRules(String username, String password,
            String message) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserReportSystemManagerOnBreakingIntegrityRules' in ProxyBridge class");
    }

    @Override
    public boolean TestUserViewHistoryPurchaseList(String username, String password) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserViewHistoryPurchaseList' in ProxyBridge class");
    }

    @Override
    public boolean TestUserViewHistoryPurchaseListWhenProductRemovedFromSystem(String username, String password,
            String productId) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserViewHistoryPurchaseListWhenProductRemovedFromSystem' in ProxyBridge class");
    }

    @Override
    public boolean TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem(String username, String password,
            String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem' in ProxyBridge class");
    }

    @Override
    public boolean TestUserViewPrivateDetails(String username, String password) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserViewPrivateDetails' in ProxyBridge class");
    }

    @Override
    public boolean TestUserEditPrivateDetails(String username, String newPassword, String newEmail) {
        throw new UnsupportedOperationException("Unimplemented method 'TestUserEditEmail' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerAddProductToShop(String username, String shopId, String productName,
            String productAmount) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerAddProductToShop' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerRemoveProductFromShop(String username, String shopId, String productName) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerRemoveProductFromShop' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerEditProductInShop(String username, String shopId, String productName,
            String productNameNew, String productAmount, String productAmountNew) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerEditProductInShop' in ProxyBridge class");
    }

    @Override
    public boolean testPermissionForShopManager(String username, Integer shopId, String permission) {
        throw new UnsupportedOperationException("Unimplemented method 'testPermissionForShopManager' in ProxyBridge class");
    }

    @Override
    public boolean testSystemManagerViewHistoryPurcaseInUsers(String namanger, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testSystemManagerViewHistoryPurcaseInUsers' in ProxyBridge class");
    }

    @Override
    public boolean testSystemManagerViewHistoryPurcaseInShops(String namanger, Integer shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testSystemManagerViewHistoryPurcaseInShops' in ProxyBridge class");
    }

    @Override
    public boolean testAddProductToShoppingCartUser(String username, String productId, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testAddProductToShoppingCartUser' in ProxyBridge class");
    }

    @Override
    public boolean testAddProductToShoppingCartGuest(String username, String productId, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testAddProductToShoppingCartGuest' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerChangeShopPolicies(String username, String shopId, String newPolicy) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerChangeShopPolicies' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerAppointAnotherShopOwner(String username, String shopId, String newOwnerUsername) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerAppointAnotherShopOwner' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerAddShopManagerPermission(String username, String shopId, String managerUsername,
            String permission) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerAddShopManagerPermission' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerRemoveShopManagerPermission(String username, String shopId, String managerUsername,
            String permission) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerRemoveShopManagerPermission' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerCloseShop(String username, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerCloseShop' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerGetShopInfo(String username, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerGetShopInfo' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerGetShopManagersPermissions(String username, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerGetShopManagersPermissions' in ProxyBridge class");
    }

    @Override
    public boolean testShopOwnerViewHistoryPurcaseInShop(String username, String shopId) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerViewHistoryPurcaseInShop' in ProxyBridge class");
    }

    @Override
    public boolean testAddExternalService(String newSerivceName, String informationPersonName, String informationPersonPhone, Integer securityIdForService) {
        throw new UnsupportedOperationException("Unimplemented method 'testAddExternalService'");
    }

    @Override
    public boolean testChangeExternalService(Integer oldServiceSystemId, String newSerivceName, String newInformationPersonName, String newInformationPersonPhone) {
        throw new UnsupportedOperationException("Unimplemented method 'testChangeExternalService'");
    }

    @Override
    public boolean testShopOwnerAppointAnotherShopManager(String username, String shopId, String newManagerUsername) {
        throw new UnsupportedOperationException("Unimplemented method 'testShopOwnerAppointAnotherShopManager'");
    }
}
