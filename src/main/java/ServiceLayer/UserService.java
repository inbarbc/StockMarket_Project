package ServiceLayer;

import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Level;

import Domain.Order;
import Domain.Facades.ShoppingCartFacade;
import Domain.Facades.UserFacade;
import Dtos.PurchaseCartDetailsDto;
import Dtos.UserDto;
import Server.notifications.NotificationHandler;
import Server.notifications.WebSocketServer;
import jakarta.transaction.Transactional;
import Domain.Alerts.*;

@SuppressWarnings({"rawtypes" , "unchecked"})
@Service
public class UserService {
    private UserFacade _userFacade;
    private TokenService _tokenService;

    private ShoppingCartFacade _shoppingCartFacade;
    private static final Logger logger = Logger.getLogger(UserService.class.getName());

    public UserService(UserFacade userFacade, TokenService tokenService,
            ShoppingCartFacade shoppingCartFacade) {
        _userFacade = userFacade;
        _tokenService = tokenService;
        _shoppingCartFacade = shoppingCartFacade;
    }

    public UserService() {
        _userFacade = UserFacade.getUserFacade();
        _tokenService = TokenService.getTokenService();
        _shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();
    }

    // this function is responsible for logging in a user to the system by checking
    // the credentials and generating a token for the user
    @Transactional
    public ResponseEntity<Response> logIn(String token, String userName, String password) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (userName == null || userName.isEmpty() || password == null || password.isEmpty()) {
                    response.setErrorMessage("Username or password is empty.");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
                _userFacade.logIn(userName, password);
                
                _shoppingCartFacade.addCartForUser(_tokenService.extractGuestId(token), _userFacade.getUserByUsername(userName));
                // update the new token for the user
                String newToken = _tokenService.generateUserToken(userName);
                response.setReturnValue(newToken);
             
                WebSocketServer.getInstance().replaceGuestTokenToUserToken(token, newToken, userName);
                // WebSocketServer.getInstance().sendMessage(userName, "You have been logged in");
                Alert alert  = new GeneralAlert("system Administrator", userName, " welcome to out website! Enjoy your first time in the system!");
                NotificationHandler.getInstance().sendMessage(userName, alert);

                logger.info("User " + userName + " Logged In Succesfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("LogIn Failed: " + e.getMessage());
            logger.log(Level.SEVERE, "LogIn Failed: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for logging out a user from the system
    @Transactional
    public ResponseEntity<Response> logOut(String token) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                String userName = _tokenService.extractUsername(token);
                _userFacade.logOut(userName);
                String newToken = _tokenService.generateGuestToken();
                String id = _tokenService.extractGuestId(newToken);
                _userFacade.addNewGuest(id);
                _shoppingCartFacade.addCartForGuest(id);
                logger.info("User successfully logged out: " + userName);
                response.setReturnValue(newToken);
                // close this session
                 WebSocketServer.getInstance().changeLoggedInSession(userName, newToken);
                Alert alert = new GeneralAlert("system Administrator", userName,
                "hello AGAIN LOGGED IN USER THIS MESSAGE HAVE BEEN WAITING FOR YOU!!!!!");
                NotificationHandler.getInstance().sendMessage(userName, alert);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Token is invalid: " + e.getMessage());
            logger.log(Level.SEVERE, "LogOut failed: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for registering a new user to the system
    @Transactional
    public ResponseEntity<Response> register(String token, UserDto userDto) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                _userFacade.register(userDto);
                logger.info("User registered: " + userDto.username);
                response.setReturnValue("Registration Succeeded");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Registration failed: " + e.getMessage());
            logger.log(Level.SEVERE, "Registration failed: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for purchasing the cart of a user or a guest
    // by checking the token and the user type and then calling the purchaseCart
    // function
    @Transactional
    public ResponseEntity<Response> purchaseCart(String token, PurchaseCartDetailsDto details) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (_tokenService.isGuest(token)) {
                    logger.log(Level.INFO, "Start purchasing cart for guest.");
                    _shoppingCartFacade.purchaseCartGuest(token, details);
                    response.setReturnValue("Guest bought card succeed");
                } else {
                    String userName = _tokenService.extractUsername(token);
                    logger.log(Level.INFO, "Start purchasing cart for user: " + userName);
                    _shoppingCartFacade.purchaseCartUser(userName, details);
                    response.setReturnValue("User bought card succeed");
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Cart bought has been failed: " + e.getMessage());
            logger.log(Level.SEVERE, "Cart bought has been failed: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for checking if a user is a system admin
    @Transactional
    public ResponseEntity<Response> isSystemAdmin(String userId) {
        Response response = new Response();
        try {
            if (_userFacade.isAdmin(userId)) {
                logger.info("User is an admin: " + userId);
                response.setReturnValue("User is an admin");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                logger.info("User is not an admin: " + userId);
                response.setReturnValue("User is not an admin");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to check if user is an admin: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to check if user is an admin: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves the purchase history of a specific user as an admin.
     *
     * @param token    The session token of the admin user.
     * @param username The ID of the user whose purchase history is to be retrieved.
     * @return A Response object containing the purchase history if successful, or
     *         an error message if not. () List<Order>
     */
    @Transactional
    public ResponseEntity<Response> getUserPurchaseHistory(String token, String username) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (!_tokenService.isUserAndLoggedIn(token)) {
                    response.setErrorMessage("User is not logged in");
                    logger.log(Level.SEVERE, "User is not logged in");
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }

                String adminUsername = _tokenService.extractUsername(token);
                boolean isAdmin = _userFacade.isAdmin(adminUsername);
                if (!isAdmin) {
                    response.setErrorMessage("User is not an admin");
                    logger.log(Level.SEVERE, "User is not an admin");
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }

                // get purchase history of a user
                response.setReturnValue(_userFacade.getPurchaseHistory(username));

                if (response.getErrorMessage() != null) {
                    response.setErrorMessage("Failed to get purchase history from user: " + username);
                    logger.log(Level.SEVERE, "Failed to get purchase history from user: " + username);
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

        } catch (Exception e) {
            response.setErrorMessage("Failed to get purchase history: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to get purchase history: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for getting the purchase history of a user.
    @Transactional
    public ResponseEntity<Response> getPersonalPurchaseHistory(String token) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (!_tokenService.isUserAndLoggedIn(token)) {
                    response.setErrorMessage("User is not logged in");
                    logger.log(Level.SEVERE, "User is not logged in");
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }
                String username = _tokenService.extractUsername(token);
                logger.info("Purchase history request for user: " + username);
                List<Order> purchaseHistory = _userFacade.getPurchaseHistory(username);
                logger.info("Purchase history retrieved for user: " + username);
                response.setReturnValue(purchaseHistory);

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to retrieve purchase history: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to retrieve purchase history: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for getting the purchase history of a shop.
    @Transactional
    public ResponseEntity<Response> addProductToShoppingCart(String token, int productID, int shopID) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (_tokenService.isGuest(token)) {
                    _shoppingCartFacade.addProductToGuestCart(_tokenService.extractGuestId(token), productID, shopID);
                } else if (_tokenService.isUserAndLoggedIn(token)) {
                    _shoppingCartFacade.addProductToUserCart(_tokenService.extractUsername(token), productID, shopID);
                } else {
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to add product: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to add product: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for removing a product from the shopping cart.
    @Transactional
    public ResponseEntity<Response> removeProductFromShoppingCart(String token, int productID, int shopID) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (_tokenService.isGuest(token)) {
                    _shoppingCartFacade.removeProductFromGuestCart(_tokenService.extractGuestId(token), productID,
                            shopID);
                } else if (_tokenService.isUserAndLoggedIn(token)) {
                    _shoppingCartFacade.removeProductFromUserCart(_tokenService.extractUsername(token), productID,
                            shopID);
                } else {
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to remove product: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to remove product: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for getting the shopping cart of a user: returns
    // a list of products in the cart.
    @Transactional
    public ResponseEntity<Response> getShoppingCart(String token) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (_tokenService.isGuest(token)) {
                    response.setReturnValue(_shoppingCartFacade.getGuestCart(_tokenService.extractGuestId(token)));
                } else if (_tokenService.isUserAndLoggedIn(token)) {
                    response.setReturnValue(_shoppingCartFacade.getUserCart(_tokenService.extractUsername(token)));
                } else {
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to get shopping cart: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to get shopping cart: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for changing the email of a user.
    @Transactional
    public ResponseEntity<Response> changeEmail(String username, String email) {
        Response response = new Response();
        try {
            _userFacade.changeEmail(username, email);
        } catch (Exception e) {
            response.setErrorMessage("Failed to change email for user: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to change email for user: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // this function is responsible for write a review for a product that bought by
    // the user (only after the purchase, and logged in user)
    @Transactional
    public ResponseEntity<Response> writeReview(String token, int productID, int shopID, String review) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (_tokenService.isUserAndLoggedIn(token)) {
                    String username = _tokenService.extractUsername(token);
                    List<Order> purchaseHistory = _userFacade.getPurchaseHistory(username);
                    _shoppingCartFacade.writeReview(username, purchaseHistory, productID, shopID, review);
                } else {
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to write review: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to write review: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for getting the user personal details.
    @Transactional
    public ResponseEntity<Response> getUserDetails(String token) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (_tokenService.isUserAndLoggedIn(token)) {
                    String username = _tokenService.extractUsername(token);
                    response.setReturnValue(_userFacade.getUserDetails(username));
                } else {
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to get user details: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to get user details: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for setting the user personal details.
    // @Transactional
    public ResponseEntity<Response> setUserDetails(String token, UserDto userDto) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (_tokenService.isUserAndLoggedIn(token)) {
                    String username = _tokenService.extractUsername(token);
                    response.setReturnValue(_userFacade.setUserDetails(username, userDto));
                } else {
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to set user details: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to set user details: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this function is responsible for getting the user shopping cart.
    @Transactional
    public ResponseEntity<Response> viewShoppingCart(String token, String username) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token)) {
                if (_tokenService.isUserAndLoggedIn(token)) {
                    response.setReturnValue(_shoppingCartFacade.viewShoppingCart(token, username));
                } else {
                    response.setReturnValue(
                            _shoppingCartFacade.viewShoppingCart(_tokenService.extractGuestId(token), null));
                }
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to view shopping cart: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to view shopping cart: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Response> viewOrderHistory(String token, String username) {
        Response response = new Response();
        try {
            if (_tokenService.validateToken(token) && _tokenService.isUserAndLoggedIn(token)) {
                response.setReturnValue(_userFacade.viewOrderHistory(username));
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            response.setErrorMessage("Failed to view order history: " + e.getMessage());
            logger.log(Level.SEVERE, "Failed to view order history: " + e.getMessage(), e);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
