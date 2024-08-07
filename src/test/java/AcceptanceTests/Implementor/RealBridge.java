package AcceptanceTests.Implementor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import Domain.Authenticators.*;
import Domain.Entities.Guest;
import Domain.Entities.Order;
import Domain.Entities.Shop;
import Domain.Entities.ShoppingBasket;
import Domain.Entities.ShoppingCart;
import Domain.Entities.User;
import Domain.Entities.Alerts.PurchaseFromShopUserAlert;
import Domain.Entities.enums.Category;
import Domain.ExternalServices.ExternalServiceHandler;
import Domain.ExternalServices.PaymentService.ProxyPayment;
import Domain.ExternalServices.SupplyService.ProxySupply;
import Domain.Facades.*;
import Domain.Repositories.*;
import Dtos.ExternalServiceDto;
import Dtos.PaymentInfoDto;
import Dtos.ProductDto;
import Dtos.PurchaseCartDetailsDto;
import Dtos.ShopDto;
import Dtos.SupplyInfoDto;
import Dtos.UserDto;
import Dtos.Rules.MinAgeRuleDto;
import Dtos.Rules.MinBasketPriceRuleDto;
import Dtos.Rules.MinProductAmountRuleDto;
import Dtos.Rules.ShoppingBasketRuleDto;
import Dtos.Rules.UserRuleDto;
import Exceptions.StockMarketException;
import Server.notifications.NotificationHandler;
import Server.notifications.WebSocketServer;
import ServiceLayer.*;
import kotlin.text._OneToManyTitlecaseMappingsKt;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

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
    private ShopFacade _shopFacade;
    private ShoppingCartFacade _shoppingCartFacade;
    private UserFacade _userFacade;

    // real classes to use in tests
    private PasswordEncoderUtil _passwordEncoder = new PasswordEncoderUtil();
    private TokenService _tokenService = new TokenService();
    private ExternalServiceHandler _externalServiceHandler = new ExternalServiceHandler();
    //private NotificationHandler _notificationHandler = new NotificationHandler();
    private EmailValidator _emailValidator = new EmailValidator();

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
    @Mock
    NotificationHandler _notificationHandlerMock;
    @Mock
    WebSocketServer webSocketServerMock;
    @Mock
    ProxyPayment _adapterPaymentMock;
    @Mock
    ProxySupply _adapterSupplyMock;

    @Mock
    DbGuestRepository _DbGuestRepositoryMock;
    @Mock
    DbOrderRepository _dbOrderRepositoryMock;
    @Mock
    DbProductRepository _dbProductRepositoryMock;
    @Mock
    DbRoleRepository _dbRoleRepositoryMock;
    @Mock
    DbShopOrderRepository _dbShopOrderRepositoryMock;
    @Mock
    DbShoppingBasketRepository _dbShoppingBasketRepositoryMock;
    @Mock
    DbShoppingCartRepository _dbShoppingCartRepositoryMock;
    @Mock
    DbShopRepository _dbShopRepositoryMock;
    @Mock
    DbUserRepository _dbUserRepositoryMock;
    @Mock
    DbDiscountRepository _dbDiscountRepositoryMock;
    @Mock
    DbPolicyRepository _dbPolicyRepositoryMock;

    // other private fields
    private static String token = "token";
    private Logger logger = Logger.getLogger(RealBridge.class.getName());
    private PaymentInfoDto paymentInfoDto;
    private SupplyInfoDto  supplyInfoDto;

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

        _userFacade = new UserFacade(new ArrayList<User>() {
                    {
                        add(new User("Bob", "bobspassword", "email@example.com", new Date()));
                    }
                }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);


        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(new User("Bob", "bobspassword", "email@example.com", new Date()));
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);
         
        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
        
        paymentInfoDto = new PaymentInfoDto("abc", "abc", "abc", "abc", "abc", "982", "abc");
        supplyInfoDto = new SupplyInfoDto("abc", "abc", "abc", "abc", "abc");
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
        
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("systemAdmin", _passwordEncoder.encodePassword("systemAdminPassword"), "email@example.com",
                        new Date()));
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(new User("systemAdmin", _passwordEncoder.encodePassword("systemAdminPassword"), "email@example.com", new Date()));
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        try {
            _userFacade.getUserByUsername("systemAdmin").setIsSystemAdmin(true);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.info("testOpenMarketSystem Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("manager", _passwordEncoder.encodePassword("managerPassword"), "email@gmail.com",
                        new Date()));
            }
        }, new ArrayList<>() {
            {
                add(new String("manager"));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(new User("manager", _passwordEncoder.encodePassword("managerPassword"), "email@gmail.com", new Date()));
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(new Guest("manager"));
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        try {
            _shoppingCartFacade.addCartForGuest("manager");
            _userFacade.getUserByUsername("manager").setIsSystemAdmin(true);
        } catch (Exception e) {
            logger.info("testAddExternalService Error message: " + e.getMessage());
            return false;
        }

        ExternalServiceDto externalServiceDto = new ExternalServiceDto(-1, "existSerivce", "name", "111");

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        try {
            _externalServiceHandler.addExternalService(externalServiceDto);
        } catch (Exception e) {
            // Handle the exception here
            logger.info("testAddExternalService Error message: " + e.getMessage());
            return false;
        }

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
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("manager", _passwordEncoder.encodePassword("managerPassword"), "email@gmail.com",
                        new Date()));
            }
        }, new ArrayList<>(){
            {
                add(new String("manager"));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(new User("manager", _passwordEncoder.encodePassword("managerPassword"), "email@gmail.com", new Date()));
            }
        }), new MemoryGuestRepository(new ArrayList<>(){
            {
                add(new Guest("manager"));
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _systemServiceUnderTest = new SystemService(_externalServiceHandler, _tokenServiceMock,
                _userFacade, _shoppingCartFacade);

        try {
            _shoppingCartFacade.addCartForGuest("manager");
        } catch (StockMarketException e) {
            logger.info("testChangeExternalService Error message: " + e.getMessage());
            return false;
        }

        try {
            _userFacade.getUserByUsername("manager").setIsSystemAdmin(true);
        } catch (Exception e) {
            logger.info("testChangeExternalService Error message: " + e.getMessage());
            return false;
        }
        ExternalServiceDto externalServiceDto = new ExternalServiceDto(0, "existSerivce", "name", "111");

        try {
            _externalServiceHandler.addExternalService(externalServiceDto);    
        } catch (Exception e) {
            logger.info("testChangeExternalService Error message: " + e.getMessage());
            return false;
        }

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

        _userFacade = new UserFacade(new ArrayList<User>(), new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>()), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
        
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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
        
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("Bobi", "encodePassword", "email@example.com",
                        new Date()));
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(new User("Bobi", "encodePassword", "email@example.com", new Date()));
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("Bob", _passwordEncoder.encodePassword("bobspassword"), "email@example.com", new Date()));
            }
        }, new ArrayList<>() {
            {
                add(new String("Bob"));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(new User("Bob", _passwordEncoder.encodePassword("bobspassword"), "email@example.com", new Date()));
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(new Guest("Bob"));
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());
        
        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
        
        try {
            _shoppingCartFacade.addCartForGuest(username);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testLoginToTheSystem Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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

    // // SYSTEM ADMIN TESTS
    // // --------------------------------------------------------------------------------------------------------------------------------------------------------------

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() { 
            {
                add(manager);
                add(guest);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(manager);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        try {
            _shopFacade.openNewShop(namanger, shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testSystemManagerViewHistoryPurcaseInShops Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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

    // // STORE MANAGER TESTS
    // // --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    public boolean testPermissionForShopManager(String username, Integer shopId, String permission) {
        // Arrange
        MockitoAnnotations.openMocks(this);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn("founder");
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(username)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn("founder")).thenReturn(true);

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(new User("shopManager", "shopManagerPassword", "email@email.com", new Date()));
                add(new User("founder", "founderPassword", "email@email.com", new Date()));
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(new User("shopManager", "shopManagerPassword", "email@email.com", new Date()));
                add(new User("founder", "founderPassword", "email@email.com", new Date()));
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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

    // // SHOP OWNER TESTS
    // // --------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    @Override
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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerAddProductToShop Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
                add(NotShopOwnerUserName);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerAddProductToShop Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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
            String productNameNew, String productPrice, String productPriceNew) {
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

        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email@email.com", new Date());
        User shopOwner = new User("shopOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com",
                new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        ProductDto productDto = new ProductDto(productName, Category.CLOTHING, Integer.parseInt(productPrice), 10);
        ProductDto productDtoNew = new ProductDto(productNameNew, Category.CLOTHING, Integer.parseInt(productPriceNew), 10 );
        ProductDto productDtoExist = new ProductDto("ExistProductName", Category.CLOTHING, Integer.parseInt(productPriceNew),10);

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerEditProductInShop Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act
        ResponseEntity<Response> res1 = _shopServiceUnderTest.addShopOwner(tokenShopFounder, Integer.parseInt(shopId),
                "shopOwner");
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

        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"), "email@email.com", new Date());
        User shopOwner = new User("shopOwner", _passwordEncoder.encodePassword("shopOwnerPassword"), "email@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopFounder);
                add(shopOwner);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        try {
            _shopFacade.openNewShop("Founder", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerChangeShopPolicies Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // List<MinBasketPriceRuleDto> policy1 = new ArrayList<>();
        // List<MinProductAmountRuleDto> policy2 = new ArrayList<>();
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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopFounder);
                add(existOwner);
                add(newOwner);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        try {
            _shopFacade.openNewShop("shopOwnerUserName", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerAppointAnotherShopOwner Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopFounder);
                add(existManager);
                add(newManager);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopOwner);
                add(shopManager);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopOwner);
                add(shopManager);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopOwner);
                add(userName);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopOwner);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopOwner);
                add(shopManager);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopOwner);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        try {
            _shopFacade.openNewShop("shopOwner", shopDto);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testShopOwnerViewHistoryPurcaseInShop Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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


    // // SHOPPING GUEST TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------


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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));


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
       _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));


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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));
        
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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));
        

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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));
        
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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));
        
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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));
        
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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));
        
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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn("guest");
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        Guest guest = new Guest("guest");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>() {
            {
                add(new String("guest"));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        // initiate _shopFacade

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));      

        // initiate _shoppingCartFacade

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        // create a shopingcart for the username
        try {
            _shoppingCartFacade.addCartForGuest("guest");

        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testAddProductToShoppingCartAsGuest Error message: " + e.getMessage());
            return false;
        }

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto(productId, Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken,
                Integer.parseInt(productId), 0, 1);

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

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn("guest");
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        String userToken = "userToken";
        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("user");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        Guest guest = new Guest("guest");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>() {
            {
                add(new String("guest"));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));      

        // initiate _shoppingCartFacade

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        // create a shopingcart for the username
        try {
            _shoppingCartFacade.addCartForGuest("guest");

        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testAddProductToShoppingCartAsGuest Error message: " + e.getMessage());
            return false;
        }

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken,
                0, 0, 1);
        
        // Act - this user checks the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.getShoppingCart(guestToken);

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
        if (res3.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartAsGuest Error message: " + res3.getBody().getErrorMessage());

        logger.info("testAddProductToShoppingCartAsGuest Error message: " + res4.getBody().getErrorMessage());
        if (status.equals("fail"))
            return res4.getBody().getErrorMessage() != null;
        return res4.getBody().getErrorMessage() == null;
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

        paymentInfoDto = new PaymentInfoDto("abc", "abc", "abc", "abc", "abc", "982", "abc");
        supplyInfoDto = new SupplyInfoDto("abc", "abc", "abc", "abc", "abc");
        
        // create a user in the system
        User user = new User("user", _passwordEncoder.encodePassword("password"), "email@email.com", new Date());
        Guest guest = new Guest(guestToken);

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>() {
            {
                add(new String(guestToken));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        // initiate _shopFacade

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));      

        // initiate _shoppingCartFacade

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        _adapterPaymentMock = ProxyPayment.getProxyAdapterPayment();
        _adapterSupplyMock = ProxySupply.getProxySupply();

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade, _adapterPaymentMock, _adapterSupplyMock);
        // create a shopingcart for the username
        try{
            _shoppingCartFacade.addCartForGuest(guestToken);
        }
        catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + e.getMessage());
            return false;
        }
        guest.setShoppingCart(shoppingCart);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
            //_shoppingCartFacade.addCartForGuest("guest");
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + e.getMessage());
            return false;
        }

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res1 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 0, 0, 1);
        ResponseEntity<Response> res2 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 1, 0, 1);
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 2, 1, 1);
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 3, 2, 1);

        paymentInfoDto = new PaymentInfoDto("1", "1", cardNumber, "1", "1", "982", "1");
        supplyInfoDto = new SupplyInfoDto("1", "1", "1", "1", "1");
        
        // Act
        ResponseEntity<Response> res5 = _userServiceUnderTest.purchaseCart(guestToken,
                new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

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
        return res5.getBody().getErrorMessage() == null;
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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopOwner);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));
        
        // initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade, _adapterPaymentMock, _adapterSupplyMock);
        // create a shopingcart for the username
        _shoppingCartFacade.addCartForGuestForTests(guestToken, shoppingCart);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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

        paymentInfoDto = new PaymentInfoDto("1", "1", cardNumber, "1", "1", "982", "1");
        supplyInfoDto = new SupplyInfoDto("1", "1", "1", "1", "1");

        // guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 0, 0, 1);
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 0, 0, 1);
        ResponseEntity<Response> res5 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 1, 0, 1);
        ResponseEntity<Response> res6 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 2, 1, 1);
        ResponseEntity<Response> res7 = _userServiceUnderTest.addProductToShoppingCart(userBuyerToken, 3, 2, 1);

        // Act
        ResponseEntity<Response> res8 = _userServiceUnderTest.purchaseCart(userBuyerToken,
            new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

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

        // Create a user in the system
        User user = new User("user", "password", "email@email.com", new Date());
        Guest guest = new Guest(guestToken);
        Guest guest2 = new Guest(guestToken2);
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>(){
            {
            add(new String(guestToken));
            add(new String(guestToken2));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
                add(guest2);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        // Initiate _shoppingCartFacade
        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        ShoppingCart shoppingCart = new ShoppingCart(_shopFacade, _adapterPaymentMock, _adapterSupplyMock);

        try {
            _shoppingCartFacade.addCartForGuest(guestToken);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAllOrNothingBuyingShoppingCartGuestThreading Error message: " + e.getMessage());
            return false;
        }

        ShoppingCart shoppingCart2 = new ShoppingCart(_shopFacade, _adapterPaymentMock, _adapterSupplyMock);

        try {
            _shoppingCartFacade.addCartForGuest(guestToken2);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckAllOrNothingBuyingShoppingCartGuestThreading Error message: " + e.getMessage());
            return false;
        }

        // Initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Initiate _userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

        // User "user" open 2 shops using ShopService
        ShopDto shopDto1 = new ShopDto("shopTestGuest1", "bankDetails1", "address1");

        // Shop owner adds a product1 to the shop using ShopService
        ProductDto productDto = new ProductDto("product1", Category.CLOTHING, 100, 1);

        // Shop owner adds a product2 to the shop using ShopService
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

        paymentInfoDto = new PaymentInfoDto("1", "1", cardNumber, "1", "1", "982", "1");
        supplyInfoDto = new SupplyInfoDto("1", "1", "1", "1", "1");

        PaymentInfoDto paymentInfoDto2 = new PaymentInfoDto("1", "1", cardNumber, "1", "1", "982", "1");
        SupplyInfoDto supplyInfoDto2 = new SupplyInfoDto("1", "1", "1", "1", "1");

        // Guest adds a product to the shopping cart using UserService
        ResponseEntity<Response> res1 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 0, 0, 1);
        ResponseEntity<Response> res2 = _userServiceUnderTest.addProductToShoppingCart(guestToken2, 0, 0, 1);
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken, 1, 0, 1);
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(guestToken2, 1, 0, 1);
        ResponseEntity<Response> res5 = _userServiceUnderTest.addProductToShoppingCart(guestToken2, 1, 0, 1);

        // Act
        ArrayList<Future<ResponseEntity<Response>>> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2); // Create a thread pool with 2 threads

        // Act
        // Task for first thread
        Callable<ResponseEntity<Response>> task1 = () -> _userServiceUnderTest.purchaseCart(guestToken, new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

        // Task for second thread
        Callable<ResponseEntity<Response>> task2 = () -> _userServiceUnderTest.purchaseCart(guestToken2, new PurchaseCartDetailsDto(paymentInfoDto2, supplyInfoDto2, basketsToBuy));

        // Submit tasks
        Future<ResponseEntity<Response>> future1 = executor.submit(task1);
        Future<ResponseEntity<Response>> future2 = executor.submit(task2);

        results.add(future1);
        results.add(future2);

        // Shutdown executor
        executor.shutdown();

        // Wait for tasks to complete
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Assert
        try {
            ResponseEntity<Response> result1 = future1.get();
            ResponseEntity<Response> result2 = future2.get();

            if (result1.getBody().getErrorMessage() != null) {
                logger.info("testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + result1.getBody().getErrorMessage());
                System.out.println("testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + result1.getBody().getErrorMessage());
                return false;
            }

            if (result2.getBody().getErrorMessage() != null) {
                logger.info("testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + result2.getBody().getErrorMessage());
                System.out.println("testCheckAllOrNothingBuyingShoppingCartGuest Error message: " + result2.getBody().getErrorMessage());
                return false;
            }

            boolean task1Result = (result1.getBody().getErrorMessage() == null);
            boolean task2Result = (result2.getBody().getErrorMessage() == null);

            return (task1Result && !task2Result) || (!task1Result && task2Result);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }


    // // SHOPPING USER TESTS --------------------------------------------------------------------------------------------------------------------------------------------------------------


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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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

        // create users in the system
        User owner = new User("owner", _passwordEncoder.encodePassword("password1"), "email1@email.com", new Date());
        User user = new User("user", _passwordEncoder.encodePassword("password2"), "email2@email.com", new Date());
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));


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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));


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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));


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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));


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
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(owner);
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));


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
         _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
 
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
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("UziNavon");
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest("UziNavon");
        User user = new User("UziNavon", _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String("UziNavon"));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest("UziNavon");
            _shoppingCartFacade.addCartForUser("UziNavon", user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testAddProductToShoppingCartAsUser Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, Integer.parseInt(shopId), productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                Integer.parseInt(productId), Integer.parseInt(shopId), 1);

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartAsUser Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartAsUser Error message: " + res2.getBody().getErrorMessage());

        logger.info("testAddProductToShoppingCartAsUser Error message: " + res3.getBody().getErrorMessage());
        return res3.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testCheckAndViewItemsInShoppingCartAsUser(String status) {

        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn("UziNavon");
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);

        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest("UziNavon");
        User user = new User("UziNavon", _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String("UziNavon"));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest("UziNavon");
            _shoppingCartFacade.addCartForUser("UziNavon", user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testAddProductToShoppingCartAsUser Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);

        // Act - this user checks the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.getShoppingCart(userToken);

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("testCheckAndViewItemsInShoppingCartAsUser Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("testCheckAndViewItemsInShoppingCartAsUser Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("testCheckAndViewItemsInShoppingCartAsUser Error message: " + res3.getBody().getErrorMessage());

        logger.info("testCheckAndViewItemsInShoppingCartAsUser Error message: " + res4.getBody().getErrorMessage());
        if (status.equals("fail"))
            return res4.getBody().getErrorMessage() != null;
        return res4.getBody().getErrorMessage() == null;
    }

    @Override
    public boolean testCheckBuyingShoppingCartUser(String username, String busketsToBuy, String cardNumber, String address) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto1 = new ProductDto("productName1", Category.CLOTHING, 5, 1);
        ProductDto productDto2 = new ProductDto("productName2", Category.CLOTHING, 5, 1);

        Guest guest = new Guest(username);
        User user = new User(username, _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest(username);
            _shoppingCartFacade.addCartForUser(username, user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testCheckBuyingShoppingCartUser Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto1);

        // Act - this user logs in using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);
          
        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res5 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto2);

        ResponseEntity<Response> res6;
        if (busketsToBuy.equals("0")) {
            res6 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                1, 0, 1);
        } else {
            res6 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                2, 0, 1);
        }


        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        paymentInfoDto = new PaymentInfoDto("abc", "abc", "abc", "abc", "abc", "982", "abc");
        supplyInfoDto = new SupplyInfoDto("abc", "abc", "abc", "abc", "abc");
        ResponseEntity<Response> res7 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null)
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res4.getBody().getErrorMessage());
        if (res6.getBody().getErrorMessage() != null) {
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if (res7.getBody().getErrorMessage() != null) {
            logger.info("testCheckBuyingShoppingCartUser Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        return true;

    }

    @Override
    public boolean testLogoutToTheSystem(String username) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        //String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest(username);
        User user = new User("UziNavon", _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);


        // Act - this user logs in using UserService
        ResponseEntity<Response> res1 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user logs out using UserService
        ResponseEntity<Response> res2 = _userServiceUnderTest.logOut(userToken);

        // Assert
        if (res1.getBody().getErrorMessage() != null) {
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res1.getBody().getErrorMessage());
            return false;
        }
        if (res2.getBody().getErrorMessage() != null) {
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res2.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestWhenUserLogoutThenHisCartSaved(String username) {

        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest(username);
        User user = new User(username, _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest(username);
            _shoppingCartFacade.addCartForUser(username, user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestWhenUserLogoutThenHisCartSaved Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto);

        // Act - this user logs in using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);

        // Act - this user logs out using UserService
        ResponseEntity<Response> res5 = _userServiceUnderTest.logOut(userToken);

        // Act - this user logs in again using UserService
        ResponseEntity<Response> res6 = _userServiceUnderTest.logIn("guestToken", username,"userPassword");

        // Act - this user logs out using UserService
        ResponseEntity<Response> res7 = _userServiceUnderTest.getShoppingCart(userToken);

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null) {
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if (res6.getBody().getErrorMessage() != null) {
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res6.getBody().getErrorMessage());
            return false;
        }
        if (res7.getBody().getErrorMessage() != null) {
            logger.info("TestWhenUserLogoutThenHisCartSaved Error message: " + res7.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestWhenUserLogoutThenHeBecomeGuest(String username) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest(username);
        User user = new User("UziNavon", _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest(username);
            _shoppingCartFacade.addCartForUser(username, user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestWhenUserLogoutThenHeBecomeGuest Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto);

        // Act - this user logs in using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);

        // Act - this user logs out using UserService
        ResponseEntity<Response> res5 = _userServiceUnderTest.logOut(userToken);

        // Act - get the guest's shopping cart - supposed to exist - the user's
        ResponseEntity<Response> res6 = _userServiceUnderTest.getShoppingCart("guestToken");


        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("TestWhenUserLogoutThenHeBecomeGuest Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("TestWhenUserLogoutThenHeBecomeGuest Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null) {}
            logger.info("TestWhenUserLogoutThenHeBecomeGuest Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("TestWhenUserLogoutThenHeBecomeGuest Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null) 
            logger.info("TestWhenUserLogoutThenHeBecomeGuest Error message: " + res5.getBody().getErrorMessage());
        if (res6.getBody().getErrorMessage() != null) {
            logger.info("TestWhenUserLogoutThenHeBecomeGuest Error message: " + res6.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestUserOpenAShop(String username, String password, String shopName, String bankDetails,
            String shopAddress) {

        MockitoAnnotations.openMocks(this);
        String tokenUserOrGuest;
        
        if (username == "Bob") {
            tokenUserOrGuest = "BobToken";
        } else {
            tokenUserOrGuest = "notUserToken";
        }

        when(_tokenServiceMock.validateToken("BobToken")).thenReturn(true);
        when(_tokenServiceMock.extractUsername("BobToken")).thenReturn("Bob");
        when(_tokenServiceMock.isUserAndLoggedIn("BobToken")).thenReturn(true);
        when(_tokenServiceMock.validateToken("notUserToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("notUserToken")).thenReturn(true);

        ShopDto shopDto = new ShopDto(shopName, bankDetails, shopAddress);

        User shopFounder = new User("Bob", _passwordEncoder.encodePassword(password),
                "email2@email.com", new Date());
        Guest guest = new Guest(username);

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenUserOrGuest, shopDto);

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
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest(username);
        User user = new User(username, _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest(username);
            _shoppingCartFacade.addCartForUser(username, user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestUserWriteReviewOnPurchasedProduct Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto);

        // Act - this user logs in using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        String cardNumber = "123456789";
        String address = "address";
        paymentInfoDto = new PaymentInfoDto("abc", "abc", "abc", "abc", "abc", "982", "abc");
        supplyInfoDto = new SupplyInfoDto("abc", "abc", "abc", "abc", "abc");
        ResponseEntity<Response> res5 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

        // Act
        ResponseEntity<Response> res6 = _userServiceUnderTest.writeReview(userToken, Integer.parseInt(productId), 0, "review");

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null) {
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if (res6.getBody().getErrorMessage() != null) {
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res6.getBody().getErrorMessage());
            return false;
        }
        return true;
    }


    @Override
    public boolean TestUserRatingPurchasedProduct(String username, String password, String productId, String score) {
        
        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest(username);
        User user = new User(username, _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest(username);
            _shoppingCartFacade.addCartForUser(username, user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestUserWriteReviewOnPurchasedProduct Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto);

        // Act - this user logs in using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        String cardNumber = "123456789";
        String address = "address";
        paymentInfoDto = new PaymentInfoDto("abc", "abc", "abc", "abc", "abc", "982", "abc");
        supplyInfoDto = new SupplyInfoDto("abc", "abc", "abc", "abc", "abc");
        ResponseEntity<Response> res5 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

        // Act
        ResponseEntity<Response> res6 = _shopServiceUnderTest.addProductRating(userToken, Integer.parseInt(productId), 0, Integer.parseInt(score));

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null) {
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if (res6.getBody().getErrorMessage() != null) {
            logger.info("TestUserWriteReviewOnPurchasedProduct Error message: " + res6.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestUserRatingShopHePurchasedFrom(String username, String password, String shopId, String score) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest(username);
        User user = new User(username, _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest(username);
            _shoppingCartFacade.addCartForUser(username, user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestUserRatingShopHePurchasedFrom Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto);

        // Act - this user logs in using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        String cardNumber = "123456789";
        String address = "address";
        paymentInfoDto = new PaymentInfoDto("abc", "abc", "abc", "abc", "abc", "982", "abc");
        supplyInfoDto = new SupplyInfoDto("abc", "abc", "abc", "abc", "abc");
        ResponseEntity<Response> res5 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

        // Act
        ResponseEntity<Response> res6 = _shopServiceUnderTest.addShopRating(userToken, Integer.parseInt(shopId), Integer.parseInt(score));

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("TestUserRatingShopHePurchasedFrom Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("TestUserRatingShopHePurchasedFrom Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("TestUserRatingShopHePurchasedFrom Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("TestUserRatingShopHePurchasedFrom Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null) {
            logger.info("TestUserRatingShopHePurchasedFrom Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if (res6.getBody().getErrorMessage() != null) {
            logger.info("TestUserRatingShopHePurchasedFrom Error message: " + res6.getBody().getErrorMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean TestUserMessagingShopHePurchasedFrom(String username, String password, String Id, String message) {
        // Arrange
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest(username);
        User user = new User(username, _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest(username);
            _shoppingCartFacade.addCartForUser(username, user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestUserMessagingShopHePurchasedFrom Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto);

        // Act - this user logs in using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        String cardNumber = "123456789";
        String address = "address";
        paymentInfoDto = new PaymentInfoDto("abc", "abc", "abc", "abc", "abc", "982", "abc");
        supplyInfoDto = new SupplyInfoDto("abc", "abc", "abc", "abc", "abc");
        ResponseEntity<Response> res5 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

        // Act
        ResponseEntity<Response> res6 = _shopServiceUnderTest.openComplaint(userToken, Integer.parseInt(Id), message);

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("TestUserMessagingShopHePurchasedFrom Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("TestUserMessagingShopHePurchasedFrom Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("TestUserMessagingShopHePurchasedFrom Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("TestUserMessagingShopHePurchasedFrom Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null) {
            logger.info("TestUserMessagingShopHePurchasedFrom Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if (res6.getBody().getErrorMessage() != null) {
            logger.info("TestUserMessagingShopHePurchasedFrom Error message: " + res6.getBody().getErrorMessage());
            return false;
        }

        try {
            // Assert - verify that the user got a message
            verify(_notificationHandlerMock, times(1)).sendMessage(eq(username), any(PurchaseFromShopUserAlert.class));
        } catch (StockMarketException ex) {
            ex.printStackTrace();
            logger.warning("TestUserMessagingShopHePurchasedFrom Error message: " + ex.getMessage());
            return false;
        }
         return true;
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
        MockitoAnnotations.openMocks(this);
        String userToken = "UziNavon";
        String tokenShopFounder = "ShopFounder";

        when(_tokenServiceMock.validateToken(userToken)).thenReturn(true);
        when(_tokenServiceMock.validateToken(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(userToken)).thenReturn(username);
        when(_tokenServiceMock.extractUsername(tokenShopFounder)).thenReturn("Founder");
        when(_tokenServiceMock.isUserAndLoggedIn(userToken)).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn(tokenShopFounder)).thenReturn(true);
        when(_tokenServiceMock.isGuest(userToken)).thenReturn(false);
        when(_tokenServiceMock.generateGuestToken()).thenReturn("guestToken");
        when(_tokenServiceMock.extractGuestId("guestToken")).thenReturn("guestId");
        when(_tokenServiceMock.validateToken("guestToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("guestToken")).thenReturn(true);


        ProductDto productDto = new ProductDto("productName", Category.CLOTHING, 5, 1);
        Guest guest = new Guest(username);
        User user = new User("Bob", _passwordEncoder.encodePassword("userPassword"), "email1@email.com",
                new Date());
        User shopFounder = new User("Founder", _passwordEncoder.encodePassword("shopFounderPassword"),
                "email2@email.com", new Date());

        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");

        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }, new ArrayList<>() {
            {
                add(new String(username));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
                add(shopFounder);
            }
        }), new MemoryGuestRepository(new ArrayList<>() {
            {
                add(guest);
            }
        }), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());
            
        try {
            _shoppingCartFacade.addCartForGuest(username);
            _shoppingCartFacade.addCartForUser(username, user);
        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("TestUserViewHistoryPurchaseList Error message: " + e.getMessage());
            return false;
        }

        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // Act

        // this user opens a shop using ShopSerivce
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(tokenShopFounder, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(tokenShopFounder, 0, productDto);

        // Act - this user logs in using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.logIn(userToken, username,"userPassword");

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(userToken,
                0, 0, 1);

        // user buys the product using UserService
        List<Integer> basketsToBuy = new ArrayList<>();
        basketsToBuy.add(0);
        String cardNumber = "123456789";
        String address = "address";
        paymentInfoDto = new PaymentInfoDto("abc", "abc", "abc", "abc", "abc", "982", "abc");
        supplyInfoDto = new SupplyInfoDto("abc", "abc", "abc", "abc", "abc");
        ResponseEntity<Response> res5 = _userServiceUnderTest.purchaseCart(userToken, new PurchaseCartDetailsDto(paymentInfoDto, supplyInfoDto, basketsToBuy));

        // Act
        ResponseEntity<Response> res6 = _userServiceUnderTest.getPersonalPurchaseHistory(userToken);

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res4.getBody().getErrorMessage());
        if (res5.getBody().getErrorMessage() != null) {
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res5.getBody().getErrorMessage());
            return false;
        }
        if (res6.getBody().getErrorMessage() != null) {
            logger.info("TestUserViewHistoryPurchaseList Error message: " + res6.getBody().getErrorMessage());
            return false;
        }

        // check if the purchased cart indeed returned
        List<Order> purchaseHistory = (List<Order>) res6.getBody().getReturnValue();
        if(purchaseHistory.size() == 0){
            logger.info("TestUserViewHistoryPurchaseList Error message: purchase history is empty");
            return false;
        }
        return true;
    }


    @SuppressWarnings("deprecation")
    @Override
    public boolean TestUserViewPrivateDetails(String username, String password) {

        MockitoAnnotations.openMocks(this);

        String tokenUserOrGuest;
        
        if (username == "Bob") {
            tokenUserOrGuest = "BobToken";
        } else {
            tokenUserOrGuest = "notUserToken";
        }

        // Arrange
        when(_tokenServiceMock.validateToken("BobToken")).thenReturn(true);
        when(_tokenServiceMock.extractUsername("BobToken")).thenReturn("Bob");
        when(_tokenServiceMock.isUserAndLoggedIn("BobToken")).thenReturn(true);
        when(_tokenServiceMock.validateToken("notUserToken")).thenReturn(true);
        when(_tokenServiceMock.isGuest("notUserToken")).thenReturn(true);
        when(_tokenServiceMock.isUserAndLoggedIn("notUserToken")).thenReturn(false);

        // initiate a user object
        User user = new User("Bob", password, "email@email.com", new Date(10, 10, 2021));
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

        // Act
        ResponseEntity<Response> res = _userServiceUnderTest.getUserDetails(tokenUserOrGuest);

        // Assert
        if(res.getBody().getErrorMessage() != null) {
            logger.info("TestUserViewPrivateDetails Error message: " + res.getBody().getErrorMessage());
            return false;
        }
    
        UserDto userDto = (UserDto) res.getBody().getReturnValue();
        if(userDto == null){
            logger.info("TestUserViewPrivateDetails Error message: userDto is null");
            return false;
        }
        return userDto.username.equals(username);
    }

    @Override
    public boolean TestUserEditPrivateDetails(String username, String newPassword, String newEmail) {
        // Arrange
        MockitoAnnotations.openMocks(this);

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
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

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

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(username);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.isGuest(token)).thenReturn(false);

        // create a user in the system
        Calendar calendar = Calendar.getInstance();
        calendar.set(1998, Calendar.NOVEMBER, 26);
        Date birthday = calendar.getTime();
        User user = new User(username, "password", "email@email.com", birthday);
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        // initiate _shoppingCartFacade

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        // // create a shopingcart for the username
        // try{
        //     _shoppingCartFacade.addCartForGuest(username);
        // }catch(Exception e){
        //     return false;
        // }
        _shoppingCartFacade.addCartForUser(username, user);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);


        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(token, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto(productId, Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(token, 0, productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(token,
                Integer.parseInt(productId), Integer.parseInt(shopId), 1);

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
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

        when(_DbGuestRepositoryMock.findByGuestId(guestname)).thenReturn(new Guest(guestname));

        // create a user in the system
        Calendar calendar = Calendar.getInstance();
        calendar.set(1998, Calendar.NOVEMBER, 26);
        Date birthday = calendar.getTime();
        User user = new User("user", "password", "email@email.com", birthday);


        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>() {
            {
                add(new String(guestname));
            }
        }, _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());
        
        _userFacade.addNewGuest(guestname);

        // initiate _shopFacade

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        // initiate _shoppingCartFacade

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), _DbGuestRepositoryMock, new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        // create a shoppingcart for the username
        try {
            _shoppingCartFacade.addCartForGuest(guestname);

        } catch (StockMarketException e) {
            e.printStackTrace();
            logger.warning("testAddProductToShoppingCartGuest Error message: " + e.getMessage());
            return false;
        }

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);

        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(userToken, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto(productId, Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(userToken, 0, productDto);

        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res3 = _userServiceUnderTest.addProductToShoppingCart(guestToken, Integer.parseInt(productId), Integer.parseInt(shopId), 1);
        
        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res2.getBody().getErrorMessage());

        logger.info("testAddProductToShoppingCartUser Error message: " + res3.getBody().getErrorMessage());
        return res3.getBody().getErrorMessage() == null;
    }


    @Override
    public boolean testAddProductToShoppingCartAsUserWithProductPolicy(String username, String productId, String shopId, String restrictedAge){
        
        // Arrange
        MockitoAnnotations.openMocks(this);

        String guestToken = "guestToken";
        when(_tokenServiceMock.validateToken(guestToken)).thenReturn(true);
        when(_tokenServiceMock.extractGuestId(guestToken)).thenReturn(username);
        when(_tokenServiceMock.isGuest(guestToken)).thenReturn(true);

        when(_tokenServiceMock.validateToken(token)).thenReturn(true);
        when(_tokenServiceMock.extractUsername(token)).thenReturn(username);
        when(_tokenServiceMock.isUserAndLoggedIn(token)).thenReturn(true);
        when(_tokenServiceMock.isGuest(token)).thenReturn(false);

        // create a user in the system
        Calendar calendar = Calendar.getInstance();
        calendar.set(1998, Calendar.NOVEMBER, 26);
        Date birthday = calendar.getTime();
        User user = new User(username, "password", "email@email.com", birthday);
        _userFacade = new UserFacade(new ArrayList<User>() {
            {
                add(user);
            }
        }, new ArrayList<>(), _passwordEncoder, _emailValidator, _dbUserRepositoryMock, _DbGuestRepositoryMock, _dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _notificationHandlerMock);
        _userFacade.setUserFacadeRepositories(new MemoryUserRepository(new ArrayList<User>() {
            {
                add(user);
            }
        }), new MemoryGuestRepository(new ArrayList<>()), new MemoryOrderRepository(), new MemoryShoppingCartRepository());

        _shopFacade = new ShopFacade(_dbShopRepositoryMock, _dbProductRepositoryMock, _dbRoleRepositoryMock, _userFacade, _notificationHandlerMock, _dbDiscountRepositoryMock, _dbPolicyRepositoryMock);
        _shopFacade.setShopFacadeRepositories(new MemoryShopRepository(new ArrayList<Shop>()), new MemoryProductRepository(new ArrayList<>()), new MemoryRoleRepository(new ArrayList<>()), new MemoryDiscountRepository(new ArrayList<>()), new MemoryPolicyRepository(new ArrayList<>()));

        // initiate _shoppingCartFacade

        _shoppingCartFacade = new ShoppingCartFacade(_dbShoppingCartRepositoryMock, _dbOrderRepositoryMock, _DbGuestRepositoryMock, _dbUserRepositoryMock, _dbShoppingBasketRepositoryMock, _dbShopOrderRepositoryMock, _userFacade, _shopFacade);
        _shoppingCartFacade.setShoppingCartFacadeRepositories(new MemoryShoppingCartRepository(), new MemoryOrderRepository(), new MemoryGuestRepository(new ArrayList<>()), new MemoryUserRepository(new ArrayList<>()), new MemoryShoppingBasketRepository(), new MemoryShopOrderRepository());

        // // create a shopingcart for the username
        // try{
        //     _shoppingCartFacade.addCartForGuest(username);
        // }catch(Exception e){
        //     return false;
        // }
        _shoppingCartFacade.addCartForUser(username, user);

        // initiate _shopServiceUnderTest
        _shopServiceUnderTest = new ShopService(_shopFacade, _tokenServiceMock, _userFacade);

        // initiate userServiceUnderTest
        _userServiceUnderTest = new UserService(_userFacade, _tokenServiceMock, _shoppingCartFacade, _notificationHandlerMock, webSocketServerMock);


        // this user opens a shop using ShopSerivce
        ShopDto shopDto = new ShopDto("shopName", "bankDetails", "address");
        ResponseEntity<Response> res1 = _shopServiceUnderTest.openNewShop(token, shopDto);

        // this user adds a product to the shop using ShopSerivce
        ProductDto productDto = new ProductDto(productId, Category.CLOTHING, 100, 1);
        ResponseEntity<Response> res2 = _shopServiceUnderTest.addProductToShop(token, 0, productDto);

        // this user adds a product policy to the shop using ShopService
        MinAgeRuleDto minAgeRuleDto = new MinAgeRuleDto(Integer.parseInt(restrictedAge));
        List<UserRuleDto> rules = new ArrayList<>();
        rules.add(minAgeRuleDto);
        ResponseEntity<Response> res3 = _shopServiceUnderTest.changeProductPolicy(token, 0, 0, rules);
        // Act - this user adds a product to the shopping cart using UserService
        ResponseEntity<Response> res4 = _userServiceUnderTest.addProductToShoppingCart(token,
                Integer.parseInt(productId), Integer.parseInt(shopId), 1);

        // Assert
        if (res1.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res1.getBody().getErrorMessage());
        if (res2.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res2.getBody().getErrorMessage());
        if (res3.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res3.getBody().getErrorMessage());
        if (res4.getBody().getErrorMessage() != null)
            logger.info("testAddProductToShoppingCartUser Error message: " + res4.getBody().getErrorMessage());
        
        return res4.getBody().getErrorMessage() == null;
    
    }

}
