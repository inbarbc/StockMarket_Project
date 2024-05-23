package ServiceLayer;

import java.util.logging.Logger;

import org.springframework.security.core.userdetails.User;

import java.util.logging.Level;

import Domain.ShoppingCartFacade;
import Domain.Shop;
import Domain.ShoppingCart;
import Domain.UserController;
import Domain.ExternalServices.ExternalServiceHandler;

// Class that represents the system service and enables users (probably admins) to control the system.

public class SystemService {
    private UserService _userService;
    private ExternalServiceHandler _externalServiceHandler;
    private boolean _isOpen = false;
    private TokenService _tokenService;
    private UserController _userController;
    private ShoppingCartFacade _shoppingCartFacade;
    private static final Logger logger = Logger.getLogger(SystemService.class.getName());

    public SystemService(UserService userService, ExternalServiceHandler externalServiceHandler) {
        _userService = userService;
        _externalServiceHandler = externalServiceHandler;
        _tokenService = new TokenService();
        _userController = new UserController();
        _shoppingCartFacade = new ShoppingCartFacade();
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
        String userId = tokenService.extractUsername(token);
        try {
            if (tokenService.validateToken(token)) {
                // Check if the user is already logged in.
                if (!tokenService.isLoggedIn(token)) {
                    response.setErrorMessage("User is not logged in");
                    logger.log(Level.SEVERE, "User is not logged in");
                    return response;
                }
                // Check if the user is an admin
                Response isAdminResponse = userService.isAdmin(userId);
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
                logger.info("System opened by admin: " + userId);
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
    private void setSystemOpen(boolean isOpen) {
        this._isOpen = isOpen;
    }

    // Check if system is open
    public boolean isSystemOpen() {
        return _isOpen;
    }

    // TODO: change doc and name- its a request to open the system
    public Response enterSystem(){
        Response response = new Response();
        try {
            String token = _tokenService.generateGuestToken();
            String id = _tokenService.extractGuestId(token);
            logger.info("New guest entered into the system, ID:" + id);
            _userController.addNewGuest(id);
            _shoppingCartFacade.addCartForGuest(token);
            response.setReturnValue(token);
        } catch (Exception e) {
            response.setErrorMessage("Guest uuid failed: " + e.getMessage());
            logger.log(Level.SEVERE, "Guest uuid failed: " + e.getMessage(), e);
        }
        return response;
    }

    public Response leaveSystem(String token) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                String id = _tokenService.extractGuestId(token);
                if(id != null){
                    logger.info("Guest with id: " + id + "left the system");
                    _userController.removeGuest(id);
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

}