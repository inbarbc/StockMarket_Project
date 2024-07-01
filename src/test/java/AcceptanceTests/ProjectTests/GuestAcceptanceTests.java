package AcceptanceTests.ProjectTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import AcceptanceTests.Implementor.BridgeInterface;
import AcceptanceTests.Implementor.RealBridge;

@ExtendWith(RealBridge.class)

public class GuestAcceptanceTests {

    // Fields.
    private BridgeInterface _bridge;

    // constructor.
    public GuestAcceptanceTests(RealBridge bridge) {
        _bridge = bridge;
    }
    
    // Test if the guest can enter the system.
    @Test
    public void TestGuestEnterTheSystem() {
        assertTrue(_bridge.TestGuestEnterTheSystem("newGuest") ); // success
        assertFalse(_bridge.TestGuestEnterTheSystem("existGuest") ); // fail - already exists
    }
    
    // Test if the user can register to the system.
    @Test
    public void TestGuestRegisterToTheSystem() {
        assertTrue(_bridge.TestGuestRegisterToTheSystem("Bob","bobspassword", "email@example.com") ); // success
        assertFalse(_bridge.TestGuestRegisterToTheSystem("Bobi","bobspassword", "email@example.com") ); // fail - already exists
        assertFalse(_bridge.TestGuestRegisterToTheSystem("","bobspassword", "email@example.com") ); // fail - empty username
        assertFalse(_bridge.TestGuestRegisterToTheSystem("Mom","", "email@example.com") ); // fail - empty pasword
        assertFalse(_bridge.TestGuestRegisterToTheSystem("Mom","momspassword", "")); // fail - empty email
    }
    
    // Test if the user can login to the system.
    @Test
    public void TestUserLogin() {
        assertTrue(_bridge.testLoginToTheSystem("Bob","bobspassword") ); // success
        assertFalse(_bridge.testLoginToTheSystem("","bobspassword") ); // fail - empty username
        assertFalse(_bridge.testLoginToTheSystem("Bob","") ); // fail - empty pasword
        assertFalse(_bridge.testLoginToTheSystem("Mom","momspassword")); // not a user in the system
    } 
}
