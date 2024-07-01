package AcceptanceTests.Implementor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import Domain.*;
import Domain.Authenticators.*;
import Domain.ExternalServices.ExternalServiceHandler;
import Domain.Facades.*;
import Dtos.ExternalServiceDto;
import Dtos.ProductDto;
import Dtos.PurchaseCartDetailsDto;
import Dtos.ShopDto;
import Dtos.UserDto;
import Dtos.Rules.ShoppingBasketRuleDto;
import Exceptions.StockMarketException;
import ServiceLayer.*;
import enums.Category;

// A real conection to the system.
// The code is tested on the real information on te system.
@ExtendWith(SpringExtension.class)
@SuppressWarnings({"rawtypes" , "unchecked"})
@SpringBootTest
public class RealBridge implements BridgeInterface, ParameterResolver {

    // real services under test
    private ShopService _shopServiceUnderTest;
    private SystemService _systemServiceUnderTest;
    private UserService _userServiceUnderTest;

    // real facades to use in tests
    private ShopFacade _shopFacade = ShopFacade.getShopFacade();
    private ShoppingCartFacade _shoppingCartFacade;
    private UserFacade _userFacade;
    private PasswordEncoderUtil _passwordEncoder;
    private TokenService _tokenService;
    private ExternalServiceHandler _externalServiceHandler;

    // mocks
    @Mock
    private TokenService _tokenServiceMock;
    @Mock
    ShopFacade _shopFacadeMock;
    @Mock
    ShoppingBasket _shoppingBasketMock;
    @Mock
    ShoppingCartFacade _shoppingCartFacadeMock;
    @Mock
    PasswordEncoderUtil _passwordEncoderMock;

    // other private fields
    private static String token = "token";
    private Logger logger = Logger.getLogger(RealBridge.class.getName());

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == RealBridge.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return new RealBridge();
    }

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);

        _shopFacade = ShopFacade.getShopFacade();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("Bob", "bobspassword", "email@example.com", new Date()));
            }
        }, new ArrayList<>());
        _externalServiceHandler = new ExternalServiceHandler();
        _passwordEncoder = new PasswordEncoderUtil();
        _tokenService = TokenService.getTokenService();

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);
    }

    @AfterEach
    public void tearDown() {
    }

    // SYSTEM TESTS
    // --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    public boolean testOpenMarketSystem(String username) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        _passwordEncoder = new PasswordEncoderUtil();
        _externalServiceHandler = new ExternalServiceHandler();
        _shopFacade = ShopFacade.getShopFacade();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("systemAdmin", _passwordEncoder.encodePassword("systemAdminPassword"), "email@example.com",
                        new Date()));
            }
        }, new ArrayList<>());
        try {
            _userFacade.getUserByUsername("systemAdmin").setIsSystemAdmin(true);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.info("testOpenMarketSystem Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        _userServiceUnderTest.logIn(token, "systemAdmin", "systemAdminPassword");

        String token = username.equals("systemAdmin") ? "systemAdmin" : "guest";

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn("systemAdmin")).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn("guest")).thenReturn(false);
        when(_tokenServiceMock.isGuest("systemAdmin")).thenReturn(false);
        when(_tokenServiceMock.isGuest("guest")).thenReturn(true);

        // Act
        ResponseEntity<Response> res = _systemServiceUnderTest.openSystem(token);

        // Assert
        logger.info("testOpenMarketSystem Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testPayment(String senario) {
        // Dummy test
        return !senario.equals("error");
    }

    @Test
    public boolean testShipping(String senario) {
        // Dummy test
        return !senario.equals("error");
    }

    @Test
    public boolean testAddExternalService(String newSerivceName, String informationPersonName,
            String informationPersonPhone, Integer securityIdForService) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn("manager");
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(token)).thenReturn("manager");

        _externalServiceHandler = new ExternalServiceHandler();
        _passwordEncoder = new PasswordEncoderUtil();

        _shopFacade = new ShopFacade();
        _shoppingCartFacade = new ShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("manager", _passwordEncoder.encodePassword("managerPassword"), "email@gmail.com",
                        new Date()));
            }
        }, new ArrayList<>());

        _shoppingCartFacade.addCartForGuest("manager");
        try {
            _userFacade.getUserByUsername("manager").setIsSystemAdmin(true);
        } catch (Exception e) {
            logger.info("testAddExternalService Error message: " + e.getMessage());
            return false;
        }

        ExternalServiceDto externalServiceDto = new ExternalServiceDto(-1, "existSerivce", "name", "111");

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        _externalServiceHandler.addExternalService(externalServiceDto);

        ResponseEntity<Response> res1 = _userServiceUnderTest.logIn(token, "manager", "managerPassword");
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testAddExternalService Error message: " + res1.getBody().getErrorMessage());
            return false;
        }

        ResponseEntity<Response> res2 = _systemServiceUnderTest.openSystem(token);
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testAddExternalService Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        ExternalServiceDto externalServiceDto2 = new ExternalServiceDto(-1, newSerivceName, informationPersonName,
                informationPersonPhone);

        // Act
        ResponseEntity<Response> res = _systemServiceUnderTest.addExternalService(token, externalServiceDto2);

        // Assert
        logger.info("testAddExternalService Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testChangeExternalService(Integer oldServiceSystemId, String newSerivceName,
            String newInformationPersonName, String newInformationPersonPhone) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn("manager");
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(token)).thenReturn("manager");

        _externalServiceHandler = new ExternalServiceHandler();
        _passwordEncoder = new PasswordEncoderUtil();

        _shopFacade = new ShopFacade();
        _shoppingCartFacade = new ShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("manager", _passwordEncoder.encodePassword("managerPassword"), "email@gmail.com",
                        new Date()));
            }
        }, new ArrayList<>());

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        _shoppingCartFacade.addCartForGuest("manager");

        try {
            _userFacade.getUserByUsername("manager").setIsSystemAdmin(true);
        } catch (Exception e) {
            logger.info("testChangeExternalService Error message: " + e.getMessage());
            return false;
        }
        ExternalServiceDto externalServiceDto = new ExternalServiceDto(0, "existSerivce", "name", "111");

        _externalServiceHandler.addExternalService(externalServiceDto);

        ResponseEntity<Response> res1 = _userServiceUnderTest.logIn(token, "manager", "managerPassword");
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testChangeExternalService Error message: " + res1.getBody().getErrorMessage());
            return false;
        }

        ResponseEntity<Response> res2 = _systemServiceUnderTest.openSystem(token);
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testChangeExternalService Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        ExternalServiceDto externalServiceDto2 = new ExternalServiceDto(oldServiceSystemId, newSerivceName, "name",
                "111");

        // Act
        ResponseEntity<Response> res3 = _systemServiceUnderTest.changeExternalServiceName(token, externalServiceDto,
                newSerivceName);
        ResponseEntity<Response> res4 = _systemServiceUnderTest.changeExternalServiceInformationPersonName(token,
                externalServiceDto2, newInformationPersonName);
        ResponseEntity<Response> res5 = _systemServiceUnderTest.changeExternalServiceInformationPersonPhone(token,
                externalServiceDto2, newInformationPersonPhone);

        // Assert
        if (res3.getBody().getErrorMessage() != null)
            logger.info("changeExternalServiceName Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info(
                    "changeExternalServiceInformationPersonName Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null)
            logger.info(
                    "changeExternalServiceInformationPersonPhone Error message: " + res5.getBody().getErrorMessage());

        return res3.getBody().getErrorMessage() == null && res4.getBody().getErrorMessage() == null
                && res5.getBody().getErrorMessage() == null;
    }

    // GUEST TESTS
    // --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    public boolean TestGuestEnterTheSystem(String shouldSeccess) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        _shopFacade = ShopFacade.getShopFacade();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>(), new ArrayList<>());
        _externalServiceHandler = new ExternalServiceHandler();

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        _userFacade.addNewGuest("existGuest");

        String token = shouldSeccess.equals("newGuest") ? "newGuest" : "existGuest";

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId("newGuest")).thenReturn("newGuest");
        when(_tokenServiceMock.extractGuestId("existGuest")).thenReturn("existGuest");
        when(_tokenServiceMock.generateGuestToken()).thenReturn(token);

        // Act
        ResponseEntity<Response> res = _systemServiceUnderTest.requestToEnterSystem();

        // Assert
        logger.info("TestGuestEnterTheSystem Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean TestGuestRegisterToTheSystem(String username, String password, String email) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        _externalServiceHandler = new ExternalServiceHandler();
        _shopFacade = ShopFacade.getShopFacade();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("Bobi", "encodePassword", "email@example.com",
                        new Date()));
            }
        }, new ArrayList<>());

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);

        // Act
        UserDto userDto = new UserDto(username, password, email, new Date());
        ResponseEntity<Response> res = _userServiceUnderTest.register(token, userDto);

        // Assert
        logger.info("TestGuestRegisterToTheSystem Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testLoginToTheSystem(String username, String password) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        _externalServiceHandler = new ExternalServiceHandler();
        _passwordEncoder = new PasswordEncoderUtil();
        _shopFacade = ShopFacade.getShopFacade();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("Bob", _passwordEncoder.encodePassword("bobspassword"), "email@example.com", new Date()));
            }
        }, new ArrayList<>());

        _shoppingCartFacade.addCartForGuest(username);

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(token)).thenReturn(username);

        // Act
        ResponseEntity<Response> res = _userServiceUnderTest.logIn(token, username, password);

        // Assert
        logger.info("testLoginToTheSystem Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    // SYSTEM ADMIN TESTS
    // --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    public boolean testSystemManagerViewHistoryPurcaseInUsers(String managerName, String userName) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        User manager = new User(managerName, "managersPassword", "email@email.com",
                new Date());
        manager.setIsSystemAdmin(true);
        User guest = new User("guest", "guest", "email@email.com", new Date());
        User user = new User("userName", "userName", "email@email.com", new Date());

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(manager);
                add(guest);
                add(user);
            }
        }, new ArrayList<>());

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(managerName);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        // Act
        ResponseEntity<Response> res = _userServiceUnderTest.getUserPurchaseHistory(token, userName);

        // Assert
        logger.info("testSystemManagerViewHistoryPurcaseInUsers Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testSystemManagerViewHistoryPurcaseInShops(String namanger, Integer shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        User manager = new User("manager", "managersPassword", "email@email.com",
                new Date());
        manager.setIsSystemAdmin(true);

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(manager);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop(namanger, shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testSystemManagerViewHistoryPurcaseInShops Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(namanger);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        // Act
        ResponseEntity<Response> res = _shopServiceUnderTest.getShopPurchaseHistory(token, shopId);

        // Assert
        logger.info("testSystemManagerViewHistoryPurcaseInShops Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    // STORE MANAGER TESTS
    // --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    public boolean testPermissionForShopManager(String username, Integer shopId, String permission) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn("founder");
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(username)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn("founder")).thenReturn(true);

        _shopFacade = ShopFacade.getShopFacade();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("shopManager", "shopManagerPassword", "email@email.com", new Date()));
                add(new User("founder", "founderPassword", "email@email.com", new Date()));
            }
        }, new ArrayList<>());
        _externalServiceHandler = new ExternalServiceHandler();

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        _shopServiceUnderTest.openNewShop(token, shopDto);

        Set<String> permissions = new HashSet<>();
        if (permission.equals("possiblePermission")) {
            permissions.add("ADD_PRODUCT");
        }

        _shopServiceUnderTest.addShopManager(token, shopId, username, permissions);

        // Act
        ResponseEntity<Response> res = _shopServiceUnderTest.addProductToShop(token, shopId,
                new ProductDto("productName", Category.CLOTHING, 100, 1));

        // Assert
        logger.info("testPermissionForShopManager Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    // SHOP OWNER TESTS
    // --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    public boolean testShopOwnerAddProductToShop(String username, String shopId, String productName,
            String productAmount) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String tokenShopOwner = "shopOwner";
        String tokenShopFounder = "shopFounder";

        when(_tokenServiceMock.validateToken(tokenShopOwner)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopOwner)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopOwner)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());
        User shopOwner = new User("shopOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com",
                new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto(productName, Category.CLOTHING, Integer.parseInt(productAmount), 1);
        ProductDto productExistDto = new ProductDto("ExistProductName", Category.CLOTHING,
                Integer.parseInt(productAmount), 1);

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerAddProductToShop Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopOwner(tokenShopFounder, Integer.parseInt(shopId),
                "shopOwner");
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopOwner, Integer.parseInt(shopId),
                productExistDto);
        ResponseEntity<Response> res3 = _shopServiceUnderTest.addProductToShop(tokenShopOwner, Integer.parseInt(shopId),
                productDto);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerAddProductToShop Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerAddProductToShop Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if (res3.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerAddProductToShop Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        return res3.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerRemoveProductFromShop(String username, String shopId, String productName) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String tokenShopOwner = "shopOwner";
        String tokenShopFounder = "shopFounder";
        String tokenNotShopOwnerUserName = "NotShopOwnerUserName";

        when(_tokenServiceMock.validateToken(tokenShopOwner)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopOwner)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopOwner)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopOwner)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenNotShopOwnerUserName)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenNotShopOwnerUserName)).thenReturn("NotShopOwnerUserName");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenNotShopOwnerUserName)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());
        User shopOwner = new User("shopOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com",
                new Date());
        User NotShopOwnerUserName = new User("NotShopOwnerUserName",
                _passwordEncoder.encodePassword("NotShopOwnerUserNamePassword"), "email@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto(productName, Category.CLOTHING, 10, 1);

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
                add(NotShopOwnerUserName);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerAddProductToShop Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopOwner(tokenShopFounder, Integer.parseInt(shopId),
                "shopOwner");
        ResponseEntity<Response> res3 = _shopServiceUnderTest.addProductToShop(tokenShopOwner, Integer.parseInt(shopId),
                productDto);
        ResponseEntity<Response> res4 = _shopServiceUnderTest.removeProductFromShop(tokenShopOwner,
                Integer.parseInt(shopId), productDto);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerRemoveProductFromShop Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res3.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerRemoveProductFromShop Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        if (res4.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerRemoveProductFromShop Error message: " + res4.getBody().getErrorMessage());
            return false;
        }
        return res4.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerEditProductInShop(String username, String shopId, String productName,
            String productNameNew, String productAmount, String productAmountNew) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String tokenShopOwner = "shopOwner";
        String tokenShopFounder = "shopFounder";

        when(_tokenServiceMock.validateToken(tokenShopOwner)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopOwner)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopOwner)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());
        User shopOwner = new User("shopOwnerUserName", _passwordEncoder.encodePassword("shopOwnerPassword"),
                "email@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        ProductDto productDto = new ProductDto("ProductName", Category.CLOTHING, Integer.parseInt(productAmount), 1);
        ProductDto productDtoNew = new ProductDto(productNameNew, Category.CLOTHING, Integer.parseInt(productAmountNew),
                1);
        ProductDto productDtoExist = new ProductDto("ExistProductName", Category.CLOTHING,
                Integer.parseInt(productAmountNew), 1);

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerEditProductInShop Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopOwner(tokenShopFounder, Integer.parseInt(shopId),
                "shopOwnerUserName");
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopOwner, Integer.parseInt(shopId),
                productDto);
        ResponseEntity<Response> res3 = _shopServiceUnderTest.addProductToShop(tokenShopOwner, Integer.parseInt(shopId),
                productDtoExist);
        ResponseEntity<Response> res4 = _shopServiceUnderTest.editProductInShop(tokenShopOwner,
                Integer.parseInt(shopId), productDto, productDtoNew);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerEditProductInShop Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerEditProductInShop Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if (res3.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerEditProductInShop Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        if (res4.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerEditProductInShop Error message: " + res4.getBody().getErrorMessage());
            return false;
        }
        return res4.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerChangeShopPolicies(String username, String shopId, String newPolicy) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String tokenShopOwner = "shopOwner";
        String tokenShopFounder = "shopFounder";

        when(_tokenServiceMock.validateToken(tokenShopOwner)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopOwner)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopOwner)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"), "email@email.com", new Date());
        User shopOwner = new User("shopOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerChangeShopPolicies Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        List<ShoppingBasketRuleDto> policy = new ArrayList<>();

        if(newPolicy.equals("fail")) {
            return false;
        }

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopOwner(tokenShopFounder, Integer.parseInt(shopId), "shopOwner");
        ResponseEntity<Response> res2 = _shopServiceUnderTest.changeShopPolicy(tokenShopOwner, Integer.parseInt(shopId), policy);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerChangeShopPolicies Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        return res2.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerAppointAnotherShopOwner(String username, String shopId, String newOwnerUsername) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String tokenShopFounder = "shopOwnerUserName";

        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopFounder = new User("shopOwnerUserName", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());
        User existOwner = new User("existOwner", _passwordEncoder.encodePassword("existOwnerPassword"),
                "email@email.com", new Date());
        User newOwner = new User("newOwner", _passwordEncoder.encodePassword("newOwnerPassword"), "email@email.com",
                new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
                add(existOwner);
                add(newOwner);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("shopOwnerUserName", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerAppointAnotherShopOwner Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopOwner(tokenShopFounder, Integer.parseInt(shopId),
                "existOwner");
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addShopOwner(tokenShopFounder, Integer.parseInt(shopId),
                newOwnerUsername);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerAppointAnotherShopOwner Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerAppointAnotherShopOwner Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        return res2.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerAppointAnotherShopManager(String username, String shopId, String newManagerUsername) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String tokenShopFounder = "shopOwnerUserName";

        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopFounder = new User("shopOwnerUserName", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());
        User existManager = new User("existManager", _passwordEncoder.encodePassword("existManagerPassword"),
                "email@email.com", new Date());
        User newManager = new User("newManager", _passwordEncoder.encodePassword("newManagerPassword"),
                "email@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
                add(existManager);
                add(newManager);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("shopOwnerUserName", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerAppointAnotherShopManager Error message: " + e.getMessage());
            return false;
        }

        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        Set<String> permissions = new HashSet<>();
        permissions.add("ADD_PRODUCT");

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopManager(tokenShopFounder, Integer.parseInt(shopId),
                "existManager", permissions);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addShopManager(tokenShopFounder, Integer.parseInt(shopId),
                newManagerUsername, permissions);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerAppointAnotherShopManager Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerAppointAnotherShopManager Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        return res2.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerAddShopManagerPermission(String username, String shopId, String managerUsername,
            String permission) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopOwner = new User("shopOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com",
                new Date());
        User shopManager = new User("managerUserName", _passwordEncoder.encodePassword("shopManagerPassword"),
                "email@EMAIL.COM", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopOwner);
                add(shopManager);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("shopOwner", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerGetShopManagersPermissions Error message: " + e.getMessage());
            return false;
        }

        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        Set<String> permissions = new HashSet<>();
        permissions.add("ADD_PRODUCT");

        Set<String> permissionsToRemove = new HashSet<>();
        if (permission.equals("newPermission")) {
            permissionsToRemove.add("ADD_PRODUCT");
        }
        if (permission.equals("invalidPermission")) {
            permissionsToRemove.add("NON_EXIST_PERMISSION");
        }
        if (permission.equals("nonexistPermission")) {
            permissionsToRemove.add("EDIT_PRODUCT");
        }

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopManager(token, Integer.parseInt(shopId),
                "managerUserName", permissions);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.modifyManagerPermissions(token, Integer.parseInt(shopId),
                "managerUserName", permissionsToRemove);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerRemoveShopManagerPermission Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerRemoveShopManagerPermission Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        return res2.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerRemoveShopManagerPermission(String username, String shopId, String managerUsername,
            String permission) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopOwner = new User("shopOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com",
                new Date());
        User shopManager = new User("managerUserName", _passwordEncoder.encodePassword("shopManagerPassword"),
                "email@EMAIL.COM", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopOwner);
                add(shopManager);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("shopOwner", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerGetShopManagersPermissions Error message: " + e.getMessage());
            return false;
        }

        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        Set<String> permissions = new HashSet<>();
        permissions.add("ADD_PRODUCT");

        Set<String> permissionsToRemove = new HashSet<>();
        if (permission.equals("existPermission")) {
            permissionsToRemove.add("ADD_PRODUCT");
        }
        if (permission.equals("invalidPermission")) {
            permissionsToRemove.add("NON_EXIST_PERMISSION");
        }
        if (permission.equals("nonexistPermission")) {
            permissionsToRemove.add("EDIT_PRODUCT");
        }

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopManager(token, Integer.parseInt(shopId),
                "managerUserName", permissions);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.modifyManagerPermissions(token, Integer.parseInt(shopId),
                "managerUserName", permissionsToRemove);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerRemoveShopManagerPermission Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerRemoveShopManagerPermission Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        return res2.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerCloseShop(String username, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopOwner = new User("Founder", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com",
                new Date());
        User userName = new User("userName", _passwordEncoder.encodePassword("userNamePassword"), "email@email.com",
                new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopOwner);
                add(userName);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerCloseShop Error message: " + e.getMessage());
            return false;
        }

        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res = _shopServiceUnderTest.closeShop(token, Integer.parseInt(shopId));

        // Assert
        logger.info("testShopOwnerCloseShop Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerGetShopInfo(String username, String shopId) {
        MockitoAnnotations.openMocks(this);

        String tokenShopOwner = "shopOwner";

        when(_tokenServiceMock.validateToken(tokenShopOwner)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopOwner)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopOwner)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopOwner = new User("shopOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com",
                new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopOwner);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("shopOwner", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerGetShopInfo Error message: " + e.getMessage());
            return false;
        }

        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.displayShopGeneralInfo(tokenShopOwner,
                Integer.parseInt(shopId));

        // Assert
        logger.info("testShopOwnerGetShopInfo Error message: " + res1.getBody().getErrorMessage());
        return res1.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerGetShopManagersPermissions(String username, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        
        String tokenShopOwner = "userNameOwner";
        String tokenShopNotOwner = "userNameNotOwner";

        when(_tokenServiceMock.validateToken(tokenShopOwner)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopOwner)).thenReturn("userNameOwner");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopOwner)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenShopNotOwner)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopNotOwner)).thenReturn("userNameNotOwner");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopNotOwner)).thenReturn(true);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopOwner = new User("userNameOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com", new Date());
        User shopManager = new User("userNameManager", _passwordEncoder.encodePassword("shopManagerPassword"), "email@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopOwner);
                add(shopManager);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("userNameOwner", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerGetShopManagersPermissions Error message: " + e.getMessage());
            return false;
        }

        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        Set<String> permissions = new HashSet<>();
        permissions.add("ADD_PRODUCT");

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopManager(tokenShopOwner, 0, "userNameManager", permissions);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.getShopManagerPermissions(token, Integer.parseInt(shopId));

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerGetShopManagersPermissions Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        return res2.getBody().getErrorMessage() == null;
    }

    @Test
    public boolean testShopOwnerViewHistoryPurcaseInShop(String username, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        User shopOwner = new User("shopOwner", "shopOwnerPassword", "email@email.com",
                new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopOwner);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("shopOwner", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerViewHistoryPurcaseInShop Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        // Act
        ResponseEntity<Response> res = _shopServiceUnderTest.getShopPurchaseHistory(token, Integer.parseInt(shopId));

        // Assert
        logger.info("testShopOwnerViewHistoryPurcaseInShop Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }


    // SHOPPING GUEST TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    public boolean testSearchAndDisplayShopByIDAsGuest(String shopId, boolean shopContainsProducts) {
       // Arrange
       MockitoAnnotations.openMocks(this);

       String guestToken = "guestToken";
       when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
       when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
       when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

       String userToken = "userToken";
       when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
       when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
       when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
       when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
       _passwordEncoder = new PasswordEncoderUtil();

       // create a user in the system
       User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
       _userFacade = new UserFacade(new ArrayList<User>() {
           {
               add(user);
           }
       }, new ArrayList<>());

       _shopFacade = new ShopFacade();
       ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
       ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 100, 1);
       ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 50, 1);

       //this user opens a shop , and if required - adds a product to the shop using shopFacade
       try {
           _shopFacade.openNewShop("user", shopDto);
           if (shopContainsProducts) {
                _shopFacade.addProductToShop(0, productDto1, "user");
                _shopFacade.addProductToShop(0, productDto2, "user");
           }
       } 
       catch (StockMarketException e) {
           e.printStackTrace();
           logger.warning("testSearchAndDisplayShopByIDAsGuest Error message: " + e.getMessage());
           return false;
       }
       // initiate _shopServiceUnderTest
       _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

       // initiate userServiceUnderTest
       _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

       // Act - this user searches a shop by its ID using shopService
       ResponseEntity<Response> res1 = _shopServiceUnderTest.searchAndDisplayShopByID(guestToken, Integer.parseInt(shopId));

       // Assert
       if(res1.getBody().getErrorMessage() != null){
           logger.info("testSearchAndDisplayShopByIDAsGuest Error message: " + res1.getBody().getErrorMessage());
           System.out.println("testSearchAndDisplayShopByIDAsGuest Error message: " + res1.getBody().getErrorMessage());
           return false;
       }
        // check if search didnt find any shops
        List<ShopDto> result = (List<ShopDto>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testSearchAndDisplayShopByIDAsGuest message: search result is empty");
            System.out.println("testSearchAndDisplayShopByIDAsGuest message: search result is empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean testSearchAndDisplayShopByNameAsGuest(String shopName, boolean shopContainsProducts) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        _passwordEncoder = new PasswordEncoderUtil();

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName1", "bankDetails", "address");
        ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 100, 1);
        ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 50, 1);

        //this user opens a shop , and if required - adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("user", shopDto);
            if (shopContainsProducts) {
                    _shopFacade.addProductToShop(0, productDto1, "user");
                    _shopFacade.addProductToShop(0, productDto2, "user");
            }
        } 
        catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testSearchAndDisplayShopByNameAsGuest Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches a shop by its name using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchAndDisplayShopByName(guestToken, shopName);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testSearchAndDisplayShopByNameAsGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testSearchAndDisplayShopByNameAsGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any shops
        List<ShopDto> result = (List<ShopDto>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testSearchAndDisplayShopByNameAsGuest message: search result is empty");
            System.out.println("testSearchAndDisplayShopByNameAsGuest message: search result is empty");
            return false;
        }
        return true;    
    }
    
    @Override
    public boolean testGetShopInfoAsGuest(String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
       when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
       when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
       when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

       String userToken = "userToken";
       when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
       when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
       when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
       when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

       _passwordEncoder = new PasswordEncoderUtil();

        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        
        try {
            _shopFacade.openNewShop("user", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetShopInfoAsGuest Error message: " + e.getMessage());
            return false;
        }

        // Act
        ResponseEntity<Response> res = _shopServiceUnderTest.displayShopGeneralInfo(guestToken, Integer.parseInt(shopId));

        // Assert
        logger.info("testGetShopInfoAsGuest Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testGetProductInfoUsingProductNameAsGuest(String productName) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName1", Category.CLOTHING, 100, 1);

        //this user opens a shop adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("user", shopDto);
            _shopFacade.addProductToShop(0, productDto, "user");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingProductNameAsGuest Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches a product in all shops by its name using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductInShopByName(guestToken, null, productName);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingProductNameAsGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingProductNameAsGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in All shops
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testGetProductInfoUsingProductNameAsGuest message: search result is empty");
            System.out.println("testGetProductInfoUsingProductNameAsGuest message: search result is empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean testGetProductInfoUsingProductCategoryAsGuest(Category category) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto1 = new ShopDto("shopName1", "bankDetails", "address");
        ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 100, 1);
        ShopDto shopDto2 = new ShopDto("shopName2", "bankDetails", "address");
        ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 50, 1);

        //this user opens a shop adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("user", shopDto1);
            _shopFacade.openNewShop("user", shopDto2);
            _shopFacade.addProductToShop(0, productDto1, "user");
            _shopFacade.addProductToShop(1, productDto2, "user");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingProductCategoryAsGuest Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches product in all shops by their category using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductInShopByCategory(guestToken, null, category);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingProductCategoryAsGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingProductCategoryAsGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in All shops
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testGetProductInfoUsingProductCategoryAsGuest message: search result is empty");
            System.out.println("testGetProductInfoUsingProductCategoryAsGuest message: search result is empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean testGetProductInfoUsingKeywordsAsGuest(List<String> keywords) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto1 = new ShopDto("shopName1", "bankDetails", "address");
        ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 100, 1);
        ShopDto shopDto2 = new ShopDto("shopName2", "bankDetails", "address");
        ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 50, 1);
        List<String> keyword1 = new ArrayList<>();
        keyword1.add("keyword1");

        //this user opens a shop, adds a product to the shop, and adds keywords to the product using shopFacade
        try {
            _shopFacade.openNewShop("user", shopDto1);
            _shopFacade.openNewShop("user", shopDto2);
            _shopFacade.addProductToShop(0, productDto1, "user");
            _shopFacade.addProductToShop(1, productDto2, "user");
            _shopFacade.addKeywordsToProductInShop( "user", 0, 0, keyword1);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingKeywordsAsGuest Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches product in all shops by keywords using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductsInShopByKeywords(guestToken, null, keywords);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingKeywordsAsGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingKeywordsAsGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in All shops
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testGetProductInfoUsingKeywordsAsGuest message: search result is empty");
            System.out.println("testGetProductInfoUsingKeywordsAsGuest message: search result is empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean testGetProductInfoUsingProductNameInShopAsGuest(String productName, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        _passwordEncoder = new PasswordEncoderUtil();

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName1", Category.CLOTHING, 100, 1);

        //this user opens a shop adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("user", shopDto);
            _shopFacade.addProductToShop(0, productDto, "user");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingProductNameInShopAsGuest Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches a product in a specific shop by its name using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductInShopByName(guestToken, Integer.parseInt(shopId), productName);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingProductNameInShopAsGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingProductNameInShopAsGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in a specific shop
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        for (List<ProductDto> productsList : result.values()) {
            if (productsList.isEmpty()) {
                logger.info("testGetProductInfoUsingProductNameInShopAsGuest message: search result is empty");
                System.out.println("testGetProductInfoUsingProductNameInShopAsGuest message: search result is empty");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean testGetProductInfoUsingProductCategoryInShopAsGuest(Category category, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        _passwordEncoder = new PasswordEncoderUtil();

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName1", Category.CLOTHING, 100, 1);

        //this user opens a shop adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("user", shopDto);
            _shopFacade.addProductToShop(0, productDto, "user");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingProductCategoryInShopAsGuest Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches products in a specific shop by their category using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductInShopByCategory(guestToken, Integer.parseInt(shopId), category);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingProductCategoryInShopAsGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingProductCategoryInShopAsGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in a specific shop
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        for (List<ProductDto> productsList : result.values()) {
            if (productsList.isEmpty()) {
                logger.info("testGetProductInfoUsingProductCategoryInShopAsGuest message: search result is empty");
                System.out.println("testGetProductInfoUsingProductCategoryInShopAsGuest message: search result is empty");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean testGetProductInfoUsingKeywordsInShopAsGuest(List<String> keywords, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        _passwordEncoder = new PasswordEncoderUtil();

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName1", Category.CLOTHING, 100, 1);
        List<String> keyword1 = new ArrayList<>();
        keyword1.add("keyword1");

        //this user opens a shop, adds a product to the shop, and adds keywords to the product using shopFacade
        try {
            _shopFacade.openNewShop("user", shopDto);
            _shopFacade.addProductToShop(0, productDto, "user");
            _shopFacade.addKeywordsToProductInShop( "user", 0, 0, keyword1);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingKeywordsInShopAsGuest Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches products in a specific shop by keywords using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductsInShopByKeywords(guestToken, Integer.parseInt(shopId), keywords);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingKeywordsInShopAsGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingKeywordsInShopAsGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in a specific shop
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        for (List<ProductDto> productsList : result.values()) {
            if (productsList.isEmpty()) {
                logger.info("testGetProductInfoUsingKeywordsInShopAsGuest message: search result is empty");
                System.out.println("testGetProductInfoUsingKeywordsInShopAsGuest message: search result is empty");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean testAddProductToShoppingCartAsGuest(String productId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User("user", "password", "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuest(guestToken);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto(productId, Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken,
                Integer.parseInt(productId), 0);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testAddProductToShoppingCartAsGuest Error message: " + res1.getBody().getErrorMessage());
            System.out
                    .println("testAddProductToShoppingCartAsGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("testAddProductToShoppingCartAsGuest Error message: " + res2.getBody().getErrorMessage());
            System.out
                    .println("testAddProductToShoppingCartAsGuest Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        logger.info("testAddProductToShoppingCartAsGuest Error message: " + res3.getBody().getErrorMessage());
        System.out.println("testAddProductToShoppingCartAsGuest Error message: " + res3.getBody().getErrorMessage());
        return res3.getBody().getErrorMessage() == null;

    }

    @Override
    public boolean testCheckAndViewItemsInShoppingCartAsGuest(String status) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        String username = "username";
        String tokenCheck = "tokenCheck";
        _passwordEncoder = new PasswordEncoderUtil();

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.isGuest(token)).thenReturn(false);

        when(_tokenServiceMock.validateToken(tokenCheck)).thenReturn(true);
        when(_tokenServiceMock.isGuest(tokenCheck)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(tokenCheck)).thenReturn(tokenCheck);

        // create a user in the system
        User user = new User(username, _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        // initialize _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        // create a shopping cart for the user
        _shoppingCartFacade.addCartForGuest(tokenCheck);

        // user opens shop and adds product to it
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        _shopFacade = new ShopFacade();
        try {
            _shopFacade.openNewShop(username, shopDto);
            _shopFacade.addProductToShop(0, productDto, username);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAndViewItemsInShoppingCartAsGuest Error message: " + e.getMessage());
            return false;
        }

        // user adds product to shopping cart
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _userServiceUnderTest.addProductToShoppingCart(token, 0, 0);

        // Act
        ResponseEntity<Response> res = _userServiceUnderTest.getShoppingCart(tokenCheck);

        // Assert
        logger.info("testCheckAndViewItemsInShoppingCartAsGuest Error message: " + res.getBody().getErrorMessage());
        if (status.equals("fail"))
            return res.getBody().getErrorMessage() != null;
        return res.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testCheckAllOrNothingBuyingShoppingCartGuest(String test, List<Integer> basketsToBuy, String cardNumber, String address) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User("user", "password", "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        
        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade);
        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuestForTests(guestToken, shoppingCart);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // user "user" open 2 shops using ShopSerivce
        ShopDto shopDto1 = new ShopDto("shopTestGuest1", "bankDetails1", "address1");

        ShopDto shopDto2 = new ShopDto("shopTestGuest2", "bankDetails2", "address2");

        ShopDto shopDto3 = new ShopDto("shopTestGuest3", "bankDetails3", "address3");

        // shop owner adds a product1 to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("product1", Category.CLOTHING, 100, 5);

        // shop owner adds a product2 to the shop using ShopSerivce
        ProductDto productDto2 = new ProductDto("product2", Category.CLOTHING, 100, 5);

        ProductDto productDto3 = new ProductDto("product3", Category.CLOTHING, 100, 5);

        ProductDto productDto4 = new ProductDto("product4", Category.CLOTHING, 100, 5);
        if (test.equals("fail")) {
            productDto4 = new ProductDto("product4", Category.CLOTHING, 100, 0);
        }

        try {
            _shopFacade.openNewShop("user", shopDto1);
            _shopFacade.openNewShop("user", shopDto2);
            _shopFacade.openNewShop("user", shopDto3);
            _shopFacade.addProductToShop(0, productDto, "user");
            _shopFacade.addProductToShop(0, productDto2, "user");
            _shopFacade.addProductToShop(1, productDto3, "user");
            _shopFacade.addProductToShop(2, productDto4, "user");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + e.getMessage());
            return false;
        }

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res1 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 0, 0);
        ResponseEntity<Response> res2 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 0, 0);
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 1, 0);
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 2, 1);
        ResponseEntity<Response> res5 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 3, 2);

        // Act
        ResponseEntity<Response> res6 = _userServiceUnderTest.purchaseCart(guestToken,
                new PurchaseCartDetailsDto(basketsToBuy, cardNumber, address));

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res2.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if (res3.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res3.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        if (res4.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res4.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res4.getBody().getErrorMessage());
            return false;
        }
        if (res5.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res5.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        return res6.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testCheckAllOrNothingBuyingShoppingCartUser(List<Integer> basketsToBuy, String cardNumber,
            String address) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String userBuyerToken = "BuyerToken";
        when(_tokenServiceMock.validateToken(userBuyerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userBuyerToken)).thenReturn("buyer");
        when(_tokenServiceMock.isUserAndLoggedIn(userBuyerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userBuyerToken)).thenReturn(false);

        // create a user in the system
        User shopOwner = new User("shopOwner", "password", "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopOwner);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        
        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade);
        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuestForTests(guestToken, shoppingCart);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        UserDto userDto = new UserDto("buyer", "passBuyer", "buyer@email.com", new Date());
        ResponseEntity<Response> res1 = _userServiceUnderTest.register(guestToken, userDto);

        ResponseEntity<Response> res2 = _userServiceUnderTest.logIn(guestToken, "buyer", "passBuyer");

        // user "user" open 2 shops using ShopSerivce
        ShopDto shopDto1 = new ShopDto("shopTestUser1", "bankDetails1", "address1");

        ShopDto shopDto2 = new ShopDto("shopTestUser2", "bankDetails2", "address2");

        ShopDto shopDto3 = new ShopDto("shopTestUser3", "bankDetails3", "address3");

        // shop owner adds a product1 to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("product1", Category.CLOTHING, 100, 5);

        // shop owner adds a product2 to the shop using ShopSerivce
        ProductDto productDto2 = new ProductDto("product2", Category.CLOTHING, 100, 5);

        ProductDto productDto3 = new ProductDto("product3", Category.CLOTHING, 100, 5);

        ProductDto productDto4 = new ProductDto("product4", Category.CLOTHING, 100, 0);

        try {
            _shopFacade.openNewShop("user", shopDto1);
            _shopFacade.openNewShop("user", shopDto2);
            _shopFacade.openNewShop("user", shopDto3);
            _shopFacade.addProductToShop(0, productDto, "user");
            _shopFacade.addProductToShop(0, productDto2, "user");
            _shopFacade.addProductToShop(1, productDto3, "user");
            _shopFacade.addProductToShop(2, productDto4, "user");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAllOrNothingBuyingShoppingCartUser Error message: " + e.getMessage());
            return false;
        }

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 0, 0);
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 0, 0);
        ResponseEntity<Response> res5 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 1, 0);
        ResponseEntity<Response> res6 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 2, 1);
        ResponseEntity<Response> res7 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 3, 2);

        // Act
        ResponseEntity<Response> res8 = _userServiceUnderTest.purchaseCart(userBuyerToken,
                new PurchaseCartDetailsDto(basketsToBuy, cardNumber, address));

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res2.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if (res3.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res3.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        if (res4.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res4.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res4.getBody().getErrorMessage());
            return false;
        }
        if (res5.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res5.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if (res6.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res6.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res6.getBody().getErrorMessage());
            return false;
        }
        if (res7.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res7.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res7.getBody().getErrorMessage());
            return false;
        }
        return res8.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testCheckAllOrNothingBuyingShoppingCartGuestThreading(String test, List<Integer> basketsToBuy, String cardNumber, String address) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String guestToken2 = "guestToken2";
        when(_tokenServiceMock.validateToken(guestToken2)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken2)).thenReturn(guestToken2);
        when(_tokenServiceMock.isGuest(guestToken2)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User("user", "password", "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        
        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade);
        
        _shoppingCartFacade.addCartForGuestForTests(guestToken, shoppingCart);

        ShoppingCart shoppingCart2 = new ShoppingCart(_shopFacade);
        
        _shoppingCartFacade.addCartForGuestForTests(guestToken2, shoppingCart2);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // user "user" open 2 shops using ShopSerivce
        ShopDto shopDto1 = new ShopDto("shopTestGuest1", "bankDetails1", "address1");

        // shop owner adds a product1 to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("product1", Category.CLOTHING, 100, 1);

        // shop owner adds a product2 to the shop using ShopSerivce
        ProductDto productDto2 = new ProductDto("product2", Category.CLOTHING, 100, 5);

        if (test.equals("fail")) {
            productDto2 = new ProductDto("product4", Category.CLOTHING, 100, 0);
        }

        try {
            _shopFacade.openNewShop("user", shopDto1);
            _shopFacade.addProductToShop(0, productDto, "user");
            _shopFacade.addProductToShop(0, productDto2, "user");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAllOrNothingBuyingShoppingCartGuestThreading Error message: " + e.getMessage());
            return false;
        }

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res1 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 0, 0);
        ResponseEntity<Response> res2 = _userServiceUnderTest.addProductToShoppingCart(guestToken2, 0, 0);
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 1, 0);
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(guestToken2, 1, 0);
        ResponseEntity<Response> res5 = _userServiceUnderTest.addProductToShoppingCart(guestToken2, 1, 0);

        // Act
        ArrayList<ResponseEntity<Response>> results = new ArrayList<ResponseEntity<Response>>();
        ExecutorService executor = Executors.newFixedThreadPool(2); // create a thread pool with 2 threads

        // Act
        // Task for first thread
        Runnable task1 = () -> {
            results.add(_userServiceUnderTest.purchaseCart(guestToken, new PurchaseCartDetailsDto(basketsToBuy, cardNumber, address)));
        };

        // Task for second thread
        Runnable task2 = () -> {
            results.add(_userServiceUnderTest.purchaseCart(guestToken2, new PurchaseCartDetailsDto(basketsToBuy, cardNumber, address)));
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
        if (res1.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res1.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res2.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if (res3.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res3.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        if (res4.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res4.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res4.getBody().getErrorMessage());
            return false;
        }
        if (res5.getBody().getErrorMessage() != null) {
            logger.info(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res5.getBody().getErrorMessage());
            System.out.println(
                    "testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        boolean task1Result = (results.get(0).getBody().getErrorMessage() == null);
        boolean task2Result = (results.get(1).getBody().getErrorMessage() == null);

        return (task1Result && !task2Result) || (!task1Result && task2Result);
    }


    // SHOPPING USER TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    public boolean testSearchAndDisplayShopByIDAsUser(String shopId, boolean shopContainsProducts) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String shopOwnerToken = "shopOwnerToken";
        when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
        when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create users in the system
        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 100, 1);
       ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 50, 1);

       //this user opens a shop , and if required - adds a product to the shop using shopFacade
       try {
            _shopFacade.openNewShop("owner", shopDto);
            if (shopContainsProducts) {
                _shopFacade.addProductToShop(0, productDto1, "owner");
                _shopFacade.addProductToShop(0, productDto2, "owner");
            }
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testSearchAndDisplayShopByIDAsUser Error message: " + e.getMessage());
            return false;
        }

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches a shop by its ID using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchAndDisplayShopByID(userToken, Integer.parseInt(shopId));

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testSearchAndDisplayShopByIDAsUser Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testSearchAndDisplayShopByIDAsUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any shops
        List<ShopDto> result = (List<ShopDto>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testSearchAndDisplayShopByIDAsUser message: search result is empty");
            System.out.println("testSearchAndDisplayShopByIDAsUser message: search result is empty");
            return false;
        }
        return true;    
    }

    @Override
    public boolean testSearchAndDisplayShopByNameAsUser(String shopName, boolean shopContainsProducts) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String shopOwnerToken = "shopOwnerToken";
        when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
        when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create users in the system
        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName1", "bankDetails", "address");
        ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 100, 1);
       ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 50, 1);

       //this user opens a shop , and if required - adds a product to the shop using shopFacade
       try {
            _shopFacade.openNewShop("owner", shopDto);
            if (shopContainsProducts) {
                _shopFacade.addProductToShop(0, productDto1, "owner");
                _shopFacade.addProductToShop(0, productDto2, "owner");
            }
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testSearchAndDisplayShopByNameAsUser Error message: " + e.getMessage());
            return false;
        }

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches a shop by its name using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchAndDisplayShopByName(userToken, shopName);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testSearchAndDisplayShopByNameAsUser Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testSearchAndDisplayShopByNameAsUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any shops
        List<ShopDto> result = (List<ShopDto>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testSearchAndDisplayShopByIDAsUser message: search result is empty");
            System.out.println("testSearchAndDisplayShopByIDAsUser message: search result is empty");
            return false;
        }
        return true;  
    }
    
    @Override
    public boolean testGetShopInfoAsUser(String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String shopOwnerToken = "shopOwnerToken";
        when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
        when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        
        try {
            _shopFacade.openNewShop("owner", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetShopInfoAsUser Error message: " + e.getMessage());
            return false;
        }

        // Act
        ResponseEntity<Response> res = _shopServiceUnderTest.displayShopGeneralInfo(userToken, Integer.parseInt(shopId));

        // Assert
        logger.info("testGetShopInfoAsUser Error message: " + res.getBody().getErrorMessage());
        return res.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testGetProductInfoUsingProductNameAsUser(String productName) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String shopOwnerToken = "shopOwnerToken";
        when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
        when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create users in the system
        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName1", Category.CLOTHING, 100, 1);

        //this user opens a shop adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("owner", shopDto);
            _shopFacade.addProductToShop(0, productDto, "owner");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingProductNameAsUser Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches a product in all shops by its name using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductInShopByName(userToken, null, productName);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingProductNameAsUser Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingProductNameAsUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in All shops
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testGetProductInfoUsingProductNameAsUser message: search result is empty");
            System.out.println("testGetProductInfoUsingProductNameAsUser message: search result is empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean testGetProductInfoUsingProductCategoryAsUser(Category category) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String shopOwnerToken = "shopOwnerToken";
        when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
        when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create users in the system
        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto1 = new ShopDto("shopName1", "bankDetails", "address");
        ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 100, 1);
        ShopDto shopDto2 = new ShopDto("shopName2", "bankDetails", "address");
        ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 50, 1);

        //this user opens a shop adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("owner", shopDto1);
            _shopFacade.addProductToShop(0, productDto1, "owner");
            _shopFacade.openNewShop("owner", shopDto2);
            _shopFacade.addProductToShop(1, productDto2, "owner");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingProductCategoryAsUser Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches products in all shops by their category using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductInShopByCategory(userToken, null, category);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingProductCategoryAsUser Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingProductCategoryAsUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in All shops
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testGetProductInfoUsingProductCategoryAsUser message: search result is empty");
            System.out.println("testGetProductInfoUsingProductCategoryAsUser message: search result is empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean testGetProductInfoUsingKeywordsAsUser(List<String> keywords) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String shopOwnerToken = "shopOwnerToken";
        when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
        when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create users in the system
        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto1 = new ShopDto("shopName1", "bankDetails", "address");
        ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 100, 1);
        ShopDto shopDto2 = new ShopDto("shopName2", "bankDetails", "address");
        ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 50, 1);
        List<String> keyword1 = new ArrayList<>();
        keyword1.add("keyword1");

        //this user opens a shop, adds a product to the shop, and adds keywords to the product using shopFacade
        try {
            _shopFacade.openNewShop("owner", shopDto1);
            _shopFacade.addProductToShop(0, productDto1, "owner");
            _shopFacade.openNewShop("owner", shopDto2);
            _shopFacade.addProductToShop(1, productDto2, "owner");
            _shopFacade.addKeywordsToProductInShop( "owner", 0, 0, keyword1);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingKeywordsAsUser Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // // Act - this user searches product in all shops by keywords using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductsInShopByKeywords(userToken, null, keywords);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingKeywordsAsUser Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingKeywordsAsUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in All shops
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        if (result == null || result.isEmpty()) {
            logger.info("testGetProductInfoUsingKeywordsAsUser message: search result is empty");
            System.out.println("testGetProductInfoUsingKeywordsAsUser message: search result is empty");
            return false;
        }
        return true;
    }


    @Override
    public boolean testGetProductInfoUsingProductNameInShopAsUser(String productName, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String shopOwnerToken = "shopOwnerToken";
        when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
        when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create users in the system
        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName1", Category.CLOTHING, 100, 1);

        //this user opens a shop adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("owner", shopDto);
            _shopFacade.addProductToShop(0, productDto, "owner");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingProductNameInShopAsUser Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches a product in all shops by its name using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductInShopByName(userToken, Integer.parseInt(shopId), productName);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingProductNameInShopAsUser Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingProductNameInShopAsUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in a specific shop
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        for (List<ProductDto> productsList : result.values()) {
            if (productsList.isEmpty()) {
                logger.info("testGetProductInfoUsingProductNameInShopAsUser message: search result is empty");
                System.out.println("testGetProductInfoUsingProductNameInShopAsUser message: search result is empty");
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean testGetProductInfoUsingProductCategoryInShopAsUser(Category category, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        String shopOwnerToken = "shopOwnerToken";
        when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
        when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);

        _passwordEncoder = new PasswordEncoderUtil();

        // create users in the system
        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName1", Category.CLOTHING, 100, 1);

        //this user opens a shop adds a product to the shop using shopFacade
        try {
            _shopFacade.openNewShop("owner", shopDto);
            _shopFacade.addProductToShop(0, productDto, "owner");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testGetProductInfoUsingProductCategoryInShopAsUser Error message: " + e.getMessage());
            return false;
        }
        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act - this user searches products in a specific shop by their category using shopService
        ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductInShopByCategory(userToken, Integer.parseInt(shopId), category);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testGetProductInfoUsingProductCategoryInShopAsUser Error message: " + res1.getBody().getErrorMessage());
            System.out.println("testGetProductInfoUsingProductCategoryInShopAsUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        // check if search didnt find any products in a specific shop
        Map<Integer, List<ProductDto>> result = (Map<Integer, List<ProductDto>>) res1.getBody().getReturnValue();
        for (List<ProductDto> productsList : result.values()) {
            if (productsList.isEmpty()) {
                logger.info("testGetProductInfoUsingProductCategoryInShopAsUser message: search result is empty");
                System.out.println("testGetProductInfoUsingProductCategoryInShopAsUser message: search result is empty");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean testGetProductInfoUsingKeywordsInShopAsUser(List<String> keywords, String shopId) {
         // Arrange
         MockitoAnnotations.openMocks(this);

         String userToken = "userToken";
         when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
         when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
         when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
         when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
 
         String shopOwnerToken = "shopOwnerToken";
         when(_tokenServiceMock.validateToken(shopOwnerToken)).thenReturn(true);
         when(_tokenServiceMock.extractUsername(shopOwnerToken)).thenReturn("owner");
         when(_tokenServiceMock.isUserAndLoggedIn(shopOwnerToken)).thenReturn(true);
         when(_tokenServiceMock.isGuest(shopOwnerToken)).thenReturn(false);
 
         _passwordEncoder = new PasswordEncoderUtil();
 
         // create users in the system
         User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
         User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
         _userFacade = new UserFacade(new ArrayList<User>() {
             {
                 add(owner);
                 add(user);
             }
         }, new ArrayList<>());
 
         _shopFacade = new ShopFacade();
         ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
         ProductDto productDto = new ProductDto("productName1", Category.CLOTHING, 100, 1);
         List<String> keyword1 = new ArrayList<>();
         keyword1.add("keyword1");
 
         //this user opens a shop, adds a product to the shop, and adds keywords to the product using shopFacade
         try {
             _shopFacade.openNewShop("owner", shopDto);
             _shopFacade.addProductToShop(0, productDto, "owner");
             _shopFacade.addKeywordsToProductInShop( "owner", 0, 0, keyword1);
         } catch (StockMarketException e) {
             e.printStackTrace();
             logger.warning("testGetProductInfoUsingKeywordsInShopAsUser Error message: " + e.getMessage());
             return false;
         }
         // initiate _shopServiceUnderTest
         _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
 
         // initiate userServiceUnderTest
         _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
 
         // Act - this user searches products in a specific shop by keywords using shopService
         ResponseEntity<Response> res1 = _shopServiceUnderTest.searchProductsInShopByKeywords(userToken, Integer.parseInt(shopId), keywords);
 
         // Assert
         if(res1.getBody().getErrorMessage() != null){
             logger.info("testGetProductInfoUsingKeywordsInShopAsUser Error message: " + res1.getBody().getErrorMessage());
             System.out.println("testGetProductInfoUsingKeywordsInShopAsUser Error message: " + res1.getBody().getErrorMessage());
             return false;
         }
         // check if search didnt find any products in a specific shop
        Map<String, List<ProductDto>> result = (Map<String, List<ProductDto>>) res1.getBody().getReturnValue();
        for (List<ProductDto> productsList : result.values()) {
            if (productsList.isEmpty()) {
                logger.info("testGetProductInfoUsingKeywordsInShopAsUser message: search result is empty");
                System.out.println("testGetProductInfoUsingKeywordsInShopAsUser message: search result is empty");
                return false;
            }
        }
         return true;
    }

    @Override
    public boolean testAddProductToShoppingCartAsUser(String productId, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);

        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");

        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);

        User user = new User("UziNavon", _passwordEncoder.encodePassword("userPassword"), "email@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("Founder", shopDto);
            _shopFacade.addProductToShop(Integer.parseInt(shopId), productDto, "Founder");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testAddProductToShoppingCartAsUser Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _userServiceUnderTest.addProductToShoppingCart(userToken, 0, 0);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testAddProductToShoppingCartAsUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean testCheckAndViewItemsInShoppingCartAsUser(String status) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        String username = "username";
        _passwordEncoder = new PasswordEncoderUtil();

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.isGuest(token)).thenReturn(false);

        // create a user in the system
        User user = new User(username, _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        // initialize _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        // create a shopping cart for the user
        _shoppingCartFacade.addCartForGuest(username);
        _shoppingCartFacade.addCartForUser(username, user);

        // user opens shop and adds product to it
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        _shopFacade = new ShopFacade();
        try {
            _shopFacade.openNewShop(username, shopDto);
            _shopFacade.addProductToShop(0, productDto, username);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAndViewItemsInShoppingCartAsUser Error message: " + e.getMessage());
            return false;
        }

        // user adds product to shopping cart
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _userServiceUnderTest.addProductToShoppingCart(token, 0, 0);

        // Act
        ResponseEntity<Response> res = _userServiceUnderTest.getShoppingCart(token);

        // Assert
        logger.info("testCheckAndViewItemsInShoppingCartAsUser Error message: " + res.getBody().getErrorMessage());
        if (status.equals("fail"))
            return res.getBody().getErrorMessage() != null;
        return res.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testCheckBuyingShoppingCartUser(String username, String busketsToBuy, String cardNumber, String address) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User("user", "password", "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        
        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade);
        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuestForTests(guestToken, shoppingCart);
        _shoppingCartFacade.addCartForUser("guestToken", user);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // user "user" open shop using ShopSerivce
        ShopDto shopDto1 = new ShopDto("shopTestGuest1", "bankDetails1", "address1");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto1);

        // shop owner adds a product1 to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("product1", Category.CLOTHING, 100, 5);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(userToken, 0, 0);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(Integer.parseInt(busketsToBuy));

        // Act
        ResponseEntity<Response> res4 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(basketsToBuy, cardNumber, address));

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if(res2.getBody().getErrorMessage() != null){
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if(res3.getBody().getErrorMessage() != null){
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        if(res4.getBody().getErrorMessage() != null){
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res4.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean testLogoutToTheSystem(String username) {
        MockitoAnnotations.openMocks(this);
        _externalServiceHandler = new ExternalServiceHandler();
        _passwordEncoder = new PasswordEncoderUtil();
        _shopFacade = ShopFacade.getShopFacade();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>(), new ArrayList<>());

        _shoppingCartFacade.addCartForGuest(username);

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(token)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);

        UserDto userDto = new UserDto("Bob", "password", "email@email.com", new Date());

        // register the user
        ResponseEntity<Response> res1 = _userServiceUnderTest.register(token, userDto);

        // login the user
        ResponseEntity<Response> res2 = _userServiceUnderTest.logIn(token, username,"password");

        // Act
        ResponseEntity<Response> res3 = _userServiceUnderTest.logOut(token);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("testLogoutToTheSystem Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if(res2.getBody().getErrorMessage() != null){
            logger.info("testLogoutToTheSystem Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if(res3.getBody().getErrorMessage() != null){
            logger.info("testLogoutToTheSystem Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestWhenUserLogoutThenHisCartSaved(String username) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        _externalServiceHandler = new ExternalServiceHandler();
        _passwordEncoder = new PasswordEncoderUtil();
        _shopFacade = ShopFacade.getShopFacade();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
        _userFacade = new UserFacade(new ArrayList<User>(), new ArrayList<>());

        _shoppingCartFacade.addCartForGuest(username);

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(token)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);

        UserDto userDto = new UserDto(username, "password", "email@email.com", new Date());

        // register the user
        ResponseEntity<Response> res1 = _userServiceUnderTest.register(token, userDto);

        // login the user
        ResponseEntity<Response> res2 = _userServiceUnderTest.logIn(token, username,"password");

        // Act
        ResponseEntity<Response> res3 = _userServiceUnderTest.logOut(token);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if(res2.getBody().getErrorMessage() != null){
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if(res3.getBody().getErrorMessage() != null){
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestWhenUserLogoutThenHeBecomeGuest(String username) {
        return testLogoutToTheSystem(username);
    }

    @Override
    public boolean TestUserOpenAShop(String username, String password, String shopName, String bankDetails,
            String shopAddress) {

        ResponseEntity<Response> res1;
        MockitoAnnotations.openMocks(this);

        String tokenUserBob = "BobToken";
        String tokenUserTom = "TomToken";

        when(_tokenServiceMock.validateToken(tokenUserBob)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenUserBob)).thenReturn("Bob");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenUserBob)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenUserTom)).thenReturn(true);
        when(_tokenServiceMock.isGuest(tokenUserTom)).thenReturn(true);

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _shopFacade = new ShopFacade();
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        if (username == "Bob") {
        }
        res1 = _shopServiceUnderTest.openNewShop(tokenUserBob, shopDto);

        if (username == "Tom")
            res1 = _shopServiceUnderTest.openNewShop(tokenUserTom, shopDto);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("testShopOwnerAddProductToShop Error message: " + res1.getBody().getErrorMessage());
            return false;
        }

        assertEquals(1, _shopFacade.getAllShops().size());
        return true;

    }

    @Override
    public boolean TestUserWriteReviewOnPurchasedProduct(String username, String password, String productId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User("user", "password", "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        
        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade);
        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuestForTests(guestToken, shoppingCart);
        _shoppingCartFacade.addCartForUser("guestToken", user);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // user "user" open shop using ShopSerivce
        ShopDto shopDto1 = new ShopDto("shopTestGuest1", "bankDetails1", "address1");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto1);

        // shop owner adds a product1 to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("product1", Category.CLOTHING, 100, 5);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(userToken, 0, 0);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        String cardNumber = "123456789";
        String address = "address";
        ResponseEntity<Response> res4 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(basketsToBuy, cardNumber, address));

        // Act
        ResponseEntity<Response> res5 = _userServiceUnderTest.writeReview(userToken, Integer.parseInt(productId), 0, "review");

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res4.getBody().getErrorMessage());
        
        logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res5.getBody().getErrorMessage());
        return res5.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean TestUserRatingPurchasedProduct(String username, String password, String productId, String score) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String tokenShopFounder = "shopFounder";
        String tokenBob = "bobToken";

        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenBob)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenBob)).thenReturn("bob");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenBob)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("Founder", shopDto);
            _shopFacade.addProductToShop(0, productDto, "Founder");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestUserRatingPurchasedProduct Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addProductRating(tokenShopFounder, 0,
                Integer.parseInt(productId), Integer.parseInt(score));

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("TestUserRatingPurchasedProduct Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestUserRatingShopHePurchasedFrom(String username, String password, String shopId, String score) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String tokenShopFounder = "shopFounder";
        String tokenBob = "bobToken";

        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);

        when(_tokenServiceMock.validateToken(tokenBob)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(tokenBob)).thenReturn("bob");
        when(_tokenServiceMock.isUserAndLoggedIn(tokenBob)).thenReturn(true);

        _passwordEncoder = new PasswordEncoderUtil();

        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();

        try {
            _shopFacade.openNewShop("Founder", shopDto);
            // _shopFacade.addProductToShop(0, productDto, "Founder");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestUserRatingPurchasedProduct Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopRating(tokenShopFounder, Integer.parseInt(shopId),
                Integer.parseInt(score));

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("TestUserRatingPurchasedProduct Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestUserMessagingShopHePurchasedFrom(String username, String password, String Id, String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'TestUserMessagingShopHePurchasedFrom'");
    }

    @Override
    public boolean TestUserReportSystemManagerOnBreakingIntegrityRules(String username, String password,
            String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
                "Unimplemented method 'TestUserReportSystemManagerOnBreakingIntegrityRules'");
    }

    @Override
    public boolean TestUserViewHistoryPurchaseList(String username, String password) {
        // Arrange
        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        // initiate a user object
        User user = new User(username, password, "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuest(username);
        _shoppingCartFacade.addCartForUser(username, user);
        
        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(token, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("product", Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(token, 0, productDto);

        // this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(token, 0, 0);

        // this user buys the product using UserService
        List<Integer> shoppingBackets = new ArrayList<>();
        shoppingBackets.add(0);
        PurchaseCartDetailsDto purchaseCartDetailsDto = new PurchaseCartDetailsDto(shoppingBackets, "123456789", "address");
        ResponseEntity<Response> res4 = _userServiceUnderTest.purchaseCart(token, purchaseCartDetailsDto);

        // Act
        ResponseEntity<Response> res5 = _userServiceUnderTest.getPersonalPurchaseHistory(token);

        // Assert
        if(res1.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res1.getBody().getErrorMessage());
        if(res2.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res2.getBody().getErrorMessage());
        if(res3.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res3.getBody().getErrorMessage());
        if(res4.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res4.getBody().getErrorMessage());
        if(res5.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res5.getBody().getErrorMessage());

        // check if the purchased cart indeed returned
        List<Order> purchaseHistory = (List<Order>) res5.getBody().getReturnValue();
        if(purchaseHistory.size() == 0){
            logger.info("TestUserViewHistoryPurchaseList Error message: purchase history is empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem(String username, String password, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User(username, password, "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        
        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade);
        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuestForTests(guestToken, shoppingCart);
        _shoppingCartFacade.addCartForUser("guestToken", user);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // user "user" open shop using ShopSerivce
        ShopDto shopDto1 = new ShopDto("shopTestGuest1", "bankDetails1", "address1");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto1);

        // shop owner adds a product1 to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("product1", Category.CLOTHING, 100, 5);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(userToken, 0, 0);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        String cardNumber = "123456789";
        String address = "address";
        ResponseEntity<Response> res4 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(basketsToBuy, cardNumber, address));

        // user remove the shop
        ResponseEntity<Response> res5 = _shopServiceUnderTest.closeShop(userToken, 0);

        // Act
        ResponseEntity<Response> res6 = _userServiceUnderTest.getPersonalPurchaseHistory(userToken);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if(res2.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if(res3.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        if(res4.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res4.getBody().getErrorMessage());
            return false;
        }
        if(res5.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if(res6.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res6.getBody().getErrorMessage());
            return false;
        }

        // check if the purchased cart indeed returned
        List<Order> purchaseHistory = (List<Order>) res6.getBody().getReturnValue();
        if(purchaseHistory.size() == 0){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: purchase history is empty");
            return false;
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean TestUserViewPrivateDetails(String username, String password) {
        // Arrange
        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        // initiate a user object
        User user = new User(username, password, "email@email.com", new Date(10, 10, 2021));
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act
        ResponseEntity<Response> res = _userServiceUnderTest.getUserDetails(token);

        // Assert
        if(res.getBody().getErrorMessage() != null)
            logger.info("TestUserViewPrivateDetails Error message: " + res.getBody().getErrorMessage());
        
        UserDto userDto = (UserDto) res.getBody().getReturnValue();
        if(userDto == null){
            logger.info("TestUserViewPrivateDetails Error message: userDto is null");
            return false;
        }
        return userDto.username.equals(username);
    }

    @Override
    public boolean TestUserEditPrivateDetails(String username, String newPassword, String newEmail) {
        //Arrange
        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);

        // initiate a user object
        User user = new User("bob", "bobspassword", "email@email.com", new Date());
        UserDto userDto = new UserDto(user.getUserName(), newPassword, newEmail, user.getBirthDate());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // Act
        ResponseEntity<Response> res = _userServiceUnderTest.setUserDetails(token, userDto);

        // Assert
        if(res.getBody().getErrorMessage() != null)
            logger.info("TestUserEditPrivateDetails Error message: " + res.getBody().getErrorMessage());

        UserDto userDtoAfterEdit = (UserDto) res.getBody().getReturnValue();
        if(userDtoAfterEdit == null){
            logger.info("TestUserEditPrivateDetails Error message: userDtoAfterEdit is null");
            return false;
        }
        return userDtoAfterEdit.email.equals(newEmail) && userDtoAfterEdit.username.equals(username) && userDtoAfterEdit.birthDate.equals(user.getBirthDate());
    }

    // SHOPPING CART TESTS
    // --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean testAddProductToShoppingCartUser(String username, String productId, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.isGuest(token)).thenReturn(false);

        // create a user in the system
        User user = new User(username, "password", "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuest(username);
        _shoppingCartFacade.addCartForUser(username, user);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(token, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto(productId, Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(token, 0, productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(token,
                Integer.parseInt(productId), Integer.parseInt(shopId));

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res2.getBody().getErrorMessage());

        logger.info("testAddProductToShoppingCartUser Error message: " + res3.getBody().getErrorMessage());
        return res3.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testAddProductToShoppingCartGuest(String guestname, String productId, String shopId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestname);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User("user", "password", "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuest(guestname);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto(productId, Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken, Integer.parseInt(productId), Integer.parseInt(shopId));
        
        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res2.getBody().getErrorMessage());

        logger.info("testAddProductToShoppingCartUser Error message: " + res3.getBody().getErrorMessage());
        return res3.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean TestUserViewHistoryPurchaseListWhenProductRemovedFromSystem(String username, String password, String productId) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(guestToken);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User(username, password, "email@email.com", new Date());
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>());

        _shopFacade = new ShopFacade();
        
        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade();

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade);
        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuestForTests(guestToken, shoppingCart);
        _shoppingCartFacade.addCartForUser("guestToken", user);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade);

        // user "user" open shop using ShopSerivce
        ShopDto shopDto1 = new ShopDto("shopTestGuest1", "bankDetails1", "address1");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto1);

        // shop owner adds a product1 to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("product1", Category.CLOTHING, 100, 5);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(userToken, 0, 0);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        String cardNumber = "123456789";
        String address = "address";
        ResponseEntity<Response> res4 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(basketsToBuy, cardNumber, address));

        // user remove the product from the shop
        ResponseEntity<Response> res5 = _shopServiceUnderTest.removeProductFromShop(userToken, 0, productDto);

        // Act
        ResponseEntity<Response> res6 = _userServiceUnderTest.getPersonalPurchaseHistory(userToken);

        // Assert
        if(res1.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if(res2.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        if(res3.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res3.getBody().getErrorMessage());
            return false;
        }
        if(res4.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res4.getBody().getErrorMessage());
            return false;
        }
        if(res5.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if(res6.getBody().getErrorMessage() != null){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: " + res6.getBody().getErrorMessage());
            return false;
        }

        // check if the purchased cart indeed returned
        List<Order> purchaseHistory = (List<Order>) res6.getBody().getReturnValue();
        if(purchaseHistory.size() == 0){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: purchase history is empty");
            return false;
        }

        // check that the product is still in the purchase history
        if(purchaseHistory.get(0).getProductsByShoppingBasket().size() == 0){
            logger.info("TestUserViewHistoryPurchaseListWhenShopRemovedFromSystem Error message: product is not in the purchase history");
            return false;
        }
        
        return true;
    }
}
