package AcceptanceTests.ProjectTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import AcceptanceTests.Implementor.BridgeInterface;
import AcceptanceTests.Implementor.RealBridge;

@ExtendWith(RealBridge.class)

public class ShopOwnerAcceptanceTests {

    // Fields.
    private BridgeInterface _bridge;

    // constructor.
    public ShopOwnerAcceptanceTests(RealBridge bridge) {
        _bridge = bridge;
    }
    
    // Test that shop owner can add products to the shop.
    @Test
    public void testShopOwnerAddProductToShop() {
        assertTrue(_bridge.testShopOwnerAddProductToShop("shopOwner", "0", "ProductName", "1") ); // success
        // assertFalse(_bridge.testShopOwnerAddProductToShop("Nirvana", "0", "ProductName", "-1") ); // fail - invalid amount
        assertFalse(_bridge.testShopOwnerAddProductToShop("whoAmI", "0", "ExistProductName", "1") ); // fail - the user is not the shop onwer
        assertFalse(_bridge.testShopOwnerAddProductToShop("shopOwner", "0", "ExistProductName", "1") ); // fail - already exist product name
    }
    
    // Test that shop owner can remove products from the shop.
    @Test
    public void testShopOwnerRemoveProductFromShop() {
        assertTrue(_bridge.testShopOwnerRemoveProductFromShop("shopOwner", "0", "ProductName") ); // success
        assertFalse(_bridge.testShopOwnerRemoveProductFromShop("shopOwner", "0", "") ); // fail - invalid productname
        assertFalse(_bridge.testShopOwnerRemoveProductFromShop("NotShopOwnerUserName", "0", "ExistProductName") ); // fail - the user is not the shop onwer
    }
    
    // Test that shop owner can edit products details in the shop.
    @Test
    public void testShopOwnerEditProductInShop() {
        assertTrue(_bridge.testShopOwnerEditProductInShop("shopOwnerUserName", "0", "ProductName", "newProductName", "1", "2") ); // success
        assertFalse(_bridge.testShopOwnerEditProductInShop("shopOwnerUserName", "0", "ProductName", "newProductName", "1", "0") ); // fail - invalid amount
        assertFalse(_bridge.testShopOwnerEditProductInShop("shopOwnerUserName", "0", "ProductName", "ExistProductName", "1", "2") ); // fail - already exist product name
        assertFalse(_bridge.testShopOwnerEditProductInShop("user", "0", "ProductName", "newProductName", "1", "2") ); // fail - the user is not the shop onwer
    }
    
    // Test that shop owner can edit the shop policies.
    @Test
    public void testShopOwnerChangeShopPolicies() {
        assertTrue(_bridge.testShopOwnerChangeShopPolicies("shopOwner", "0", "new shop policy") ); // success
        assertFalse(_bridge.testShopOwnerChangeShopPolicies("shopOwner", "0", "fail") ); // fail - invalid new shop policy
        assertFalse(_bridge.testShopOwnerChangeShopPolicies("shopOwnerUserName", "0", "new shop policy") ); // fail - the user is not the shop onwer
    }
    
    // Test that shop owner can add another shop owner to his shop.
    @Test
    public void testShopOwnerAppointAnotherShopOwner() {
        assertTrue(_bridge.testShopOwnerAppointAnotherShopOwner("shopOwnerUserName", "0", "newOwner") ); // success
        assertFalse(_bridge.testShopOwnerAppointAnotherShopOwner("shopOwnerUserName", "0", "newOwnerInvalidUserName") ); // fail - invalid new owner name
        assertFalse(_bridge.testShopOwnerAppointAnotherShopOwner("shopOwnerUserName", "0", "existOwner") ); // fail - the new user is already a shop onwer
    }
    
    // Test that shop owner can add another shop manager to his shop.
    @Test
    public void testShopOwnerAppointAnotherShopManager() {
        assertTrue(_bridge.testShopOwnerAppointAnotherShopManager("shopOwnerUserName", "0", "newManager") ); // success
        assertFalse(_bridge.testShopOwnerAppointAnotherShopManager("shopOwnerUserName", "0", "newManagerInvalidUserName") ); // fail - invalid new manager name
        assertFalse(_bridge.testShopOwnerAppointAnotherShopManager("shopOwnerUserName", "0", "existManager") ); // fail - the new user is already a shop manager
    }

    // Test that the shop owner can add a permission of a shop manager.
    @Test
    public void testShopOwnerAddShopManagerPermission(){
        assertTrue(_bridge.testShopOwnerAddShopManagerPermission("shopOwner", "0", "managerUserName", "newPermission")); // success
        assertFalse(_bridge.testShopOwnerAddShopManagerPermission("shopOwner", "0", "managerUserName", "invalidPermission")); // fail - invalid permission
    }
    
    // Test that the shop owner can remove a permission of a shop manager.
    @Test
    public void testShopOwnerRemoveShopManagerPermission(){
        assertTrue(_bridge.testShopOwnerRemoveShopManagerPermission("shopOwner", "0", "managerUserName", "existPermission")); // success
        assertFalse(_bridge.testShopOwnerRemoveShopManagerPermission("shopOwner", "0", "managerUserName", "invalidPermission")); // fail - invalid permission
    }
    
    // Test that the shop owner can close his shop in the system.
    @Test
    public void testShopOwnerCloseShop(){
        assertTrue(_bridge.testShopOwnerCloseShop("Founder", "0")); // success
        assertFalse(_bridge.testShopOwnerCloseShop("userName", "0")); // fail - exist shop but he is not the owner
        assertFalse(_bridge.testShopOwnerCloseShop("userName", "-1")); // fail - non exist shop id
    }
    
    // Test that the shop owner can get the information about the shop.
    @Test
    public void testShopOwnerGetShopInfo(){
        assertTrue(_bridge.testShopOwnerGetShopInfo("shopOwner", "0")); // success
        assertFalse(_bridge.testShopOwnerGetShopInfo("shopOwner", "-1")); // fail - non exist shop id
    }
    
    // Test that the shop owner can get the permissions of the shop managers.
    @Test
    public void testShopOwnerGetShopManagersPermissions(){
        assertTrue(_bridge.testShopOwnerGetShopManagersPermissions("userNameManager", "0")); // success
        assertFalse(_bridge.testShopOwnerGetShopManagersPermissions("userNameNotOwner", "0")); // fail - exist shop but he is not a manager
        assertFalse(_bridge.testShopOwnerGetShopManagersPermissions("userNameManager", "1")); // fail - non exist shop id
    }
    
    // Test that the shop owner can see the history purchases of the shop.
    @Test
    public void testShopOwnerViewHistoryPurcaseInShop() {
        assertTrue(_bridge.testShopOwnerViewHistoryPurcaseInShop("shopOwner", "0") ); // success
        assertFalse(_bridge.testShopOwnerViewHistoryPurcaseInShop("userName", "0")); // fail - exist shop but he is not the owner
        assertFalse(_bridge.testShopOwnerViewHistoryPurcaseInShop("userName", "-1")); // fail - non exist shop id
    }
}
