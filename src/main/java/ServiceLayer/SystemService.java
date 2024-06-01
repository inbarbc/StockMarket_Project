package ServiceLayer;

import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import java.util.logging.Level;

import Domain.ExternalServices.ExternalServiceHandler;
import Domain.Facades.ShoppingCartFacade;
import Domain.Facades.UserFacade;

// Class that represents the system service and enables users (probably admins) to control the system.

@Service
public class SystemService {
    private UserService _userService;
    private ExternalServiceHandler _externalServiceHandler;
    private boolean _isOpen = false;
    private TokenService _tokenService;
    private UserFacade _userFacade;
    private ShoppingCartFacade _shoppingCartFacade;
    private static final Logger logger = Logger.getLogger(SystemService.class.getName());

    public SystemService(UserService userService, ExternalServiceHandler externalServiceHandler,
            TokenService tokenService, UserFacade userFacade, ShoppingCartFacade shoppingCartFacade) {
        _userService = userService;
        _externalServiceHandler = externalServiceHandler;
        _tokenService = tokenService;
        _userFacade = userFacade;
        _shoppingCartFacade = shoppingCartFacade;
        // TODO: create it as a singleton
        _externalServiceHandler = externalServiceHandler;
    }

    /**
     * Opens the system.
     * 
     * @param userId   the user ID
     * @param password the user password
     * @return a response indicating the success or failure of opening the system
     */
    public Response openSystem(String token) {
        Response response = new Response();
        try {
            String username = _tokenService.extractUsername(token);
            if (_tokenService.validateToken(token)) {
                // Check if the user is already logged in.
                if (!_tokenService.isUserAndLoggedIn(token)) {
                    response.setErrorMessage("User is not logged in");
                    logger.log(Level.SEVERE, "User is not logged in");
                    return response;
                }
                // Check if the user is an admin
                Response isAdminResponse = _userService.isSystemAdmin(username);
                if (isAdminResponse.getErrorMessage() != null) {
                    response.setErrorMessage("User is not an admin");
                    logger.log(Level.SEVERE, "User is not an admin");
                    return response;
                }

                // Check if the system is already open
                if (isSystemOpen()) {
                    response.setErrorMessage("System is already open");
                    logger.log(Level.SEVERE, "System is already open");
                    return response;
                }

                // Connect to external services
                if (!_externalServiceHandler.connectToServices()) {
                    response.setErrorMessage("Failed to connect to external services");
                    logger.log(Level.SEVERE, "Failed to connect to external services");
                    return response;
                }

                // Open the system
                setSystemOpen(true);
                _externalServiceHandler.addPaymentService("PaymentService", "Tal", "123456789");
                _externalServiceHandler.addSupplyService("SupplyService", "Tal", "123456789");
                logger.info("System opened by admin: " + username);
                response.setReturnValue("System Opened Successfully");
            } else {
                throw new Exception("Invalid session token.");
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to open system: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to open system: " + e.getMessage(), e);
        }
        return response;
    }

    // Set system to open
    public void setSystemOpen(boolean isOpen) {
        this._isOpen = isOpen;
    }

    // Check if system is open
    public boolean isSystemOpen() {
        return _isOpen;
    }

    /**
     * This is the first request that will occur when a new guest enters the system.
     * It generates a guest token and initializes a new cart with the guestID.
     *
     * @return Response object containing the generated guest token if successful,
     *         or an error message if there is a failure.
     */
    public Response requestToEnterSystem() {
        Response response = new Response();
        try {
            String token = _tokenService.generateGuestToken();
            String id = _tokenService.extractGuestId(token);
            logger.info("New guest entered into the system, ID:" + id);
            _userFacade.addNewGuest(id);
            _shoppingCartFacade.addCartForGuest(token);
            response.setReturnValue(token);
        } catch (Exception e) {
            response.setErrorMessage("Guest uuid failed: " + e.getMessage());
            logger.log(Level.SEVERE, "Guest uuid failed: " + e.getMessage(), e);
        }
        return response;
    }

    /**
     * This is the last request that will occur when a guest/user leave the system.
     * It removes the guest from the user system, and removes the guest's shopping
     * cart.
     *
     * @param token The session token for the guest.
     * @return Response object indicating the success or failure of the operation.
     */
    public Response leaveSystem(String token) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                String id = _tokenService.extractGuestId(token);
                if (id != null) {
                    logger.info("Guest with id: " + id + "left the system");
                    _userFacade.removeGuest(id);
                    _shoppingCartFacade.removeCartForGuest(token);
                    response.setReturnValue("Guest left system Successfully");
                }
            } else {
                throw new Exception("Invalid session token.");
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to leave the system: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to leave the system: " + e.getMessage(), e);
        }
        return response;
    }

    // add external service to the system
    public Response addExternalService(String token, String newSerivceName, String informationPersonName, String informationPersonPhone) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                String username = _tokenService.extractUsername(token);
                if (_tokenService.isUserAndLoggedIn(token) & _userFacade.isAdmin(username)) {
                    if (isSystemOpen()) {
                        if (_externalServiceHandler.addService(newSerivceName, informationPersonName, informationPersonPhone)) {
                            logger.info("External service: " + newSerivceName + " added by admin: " + username);
                            response.setReturnValue("External service added successfully");
                        } else {
                            response.setErrorMessage("Failed to add external service");
                            logger.log(Level.SEVERE, "Failed to add external service");
                        }
                    } else {
                        response.setErrorMessage("System is not open");
                        logger.log(Level.SEVERE, "System is not open");
                    }
                } else {
                    response.setErrorMessage("User is not logged in");
                    logger.log(Level.SEVERE, "User is not logged in");
                }
            } else {
                throw new Exception("Invalid session token.");
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to add external service: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to add external service: " + e.getMessage(), e);
        }
        return response;
    }
}