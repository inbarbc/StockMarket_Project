package Domain.Facades;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Domain.Alerts.Alert;
import Domain.Authenticators.EmailValidator;
import Domain.Authenticators.PasswordEncoderUtil;
import Domain.Order;
import Domain.Repositories.MemoryUserRepository;
import Domain.Repositories.InterfaceUserRepository;
import Domain.User;
import Dtos.OrderDto;
import Dtos.UserDto;
import Exceptions.StockMarketException;
import Exceptions.UserException;
import Server.notifications.NotificationHandler;

@Service
public class UserFacade {
    private static UserFacade _UserFacade;
    private InterfaceUserRepository _userRepository;
    private List<String> _guestIds;
    private EmailValidator _EmailValidator;
    private PasswordEncoderUtil _passwordEncoder;

    public UserFacade() {
        _userRepository = new MemoryUserRepository(new ArrayList<>());
        _guestIds = new ArrayList<>();
        _EmailValidator = new EmailValidator();
        _passwordEncoder = new PasswordEncoderUtil();

        // // //For testing UI
        // initUI();
    }

    public UserFacade(List<User> registeredUsers, List<String> guestIds) {
        _userRepository = new MemoryUserRepository(registeredUsers);
        _guestIds = guestIds;
        _EmailValidator = new EmailValidator();
        _passwordEncoder = new PasswordEncoderUtil();

        // // //For testing UI
        // initUI();
    }

    public UserFacade(InterfaceUserRepository userRepository) {
        _userRepository = userRepository;
        _guestIds = new ArrayList<>();
        _EmailValidator = new EmailValidator();
        _passwordEncoder = new PasswordEncoderUtil();

        // // //For testing UI
        // initUI();
    }

    // Public method to provide access to the _UserFacade
    public static synchronized UserFacade getUserFacade() {
        if (_UserFacade == null) {
            _UserFacade = new UserFacade(new ArrayList<>(), new ArrayList<>());
        }
        return _UserFacade;
    }

    // set the user repository to be used real time
    public void setUserRepository(InterfaceUserRepository userRepository) {
        _userRepository = userRepository;
    }

    // logIn function
    public void logIn(String userName, String password) throws StockMarketException{
        if (!AreCredentialsCorrect(userName, password)){
            throw new StockMarketException("User Name Is Not Registered Or Password Is Incorrect.");
        }
        
        User user = getUserByUsername(userName);
        if (user.isLoggedIn()){
                throw new StockMarketException("User is already logged in.");
        }
        
        user.logIn();
    }

    // logOut function
    public void logOut(String userName) throws StockMarketException{
        User user = getUserByUsername(userName);
        if (!user.isLoggedIn()){
                throw new StockMarketException("User is not logged in.");
        }
        
        user.logOut();
    }

    // function to check if a user exists in the system
    @Transactional
    public boolean doesUserExist(String username) {
        return _userRepository.doesUserExist(username);
    }

    // function to get a user by username
    @Transactional
    public User getUserByUsername(String username) throws StockMarketException {
        if (username == null)
            throw new UserException("Username is null.");
        if (!doesUserExist(username))
            throw new UserException(String.format("Username %s does not exist.", username));
        return _userRepository.getUserByUsername(username);
    }

    // function to check if the credentials are correct
    @Transactional
    public boolean AreCredentialsCorrect(String username, String raw_password) throws StockMarketException {
        User user = getUserByUsername(username);
        if (user != null) {
            return _passwordEncoder.matches(raw_password, user.getPassword());
        }
        return false;
    }

    // this function is used to register a new user to the system.
    @Transactional
    public void register(UserDto userDto) throws StockMarketException {
        if (userDto.username == null || userDto.username.isEmpty()) {
            throw new StockMarketException("UserName is empty.");
        }
        if (userDto.email == null || userDto.email.isEmpty()) {
            throw new StockMarketException("Email is empty.");
        }
        if (userDto.password == null || userDto.password.isEmpty() || userDto.password.length() < 5) {
            throw new StockMarketException("Password is empty, or too short.");
        }
        if (!_EmailValidator.isValidEmail(userDto.email)) {
            throw new StockMarketException("Email is not valid.");
        }
        String encodedPass = this._passwordEncoder.encodePassword(userDto.password);
        userDto.password = encodedPass;
        if (!doesUserExist(userDto.username)) {
            _userRepository.addUser(new User(userDto));
        } else {
            throw new StockMarketException("Username already exists.");
        }
    }

    // function to add an order to a user
    @Transactional
    public void addOrderToUser(String username, Order order) throws StockMarketException {
        User user = getUserByUsername(username);
        user.addOrder(order);
    }

    // function that check if a given user is an admin
    @Transactional
    public boolean isAdmin(String userName) throws StockMarketException {
        User user = getUserByUsername(userName);
        return user.isAdmin();
    }

    // function to check if a given user is a guest
    @Transactional
    private boolean isGuestExists(String id) {
        return _guestIds.contains(id);
    }

    // function to add a new guest to the system
    @Transactional
    public void addNewGuest(String id) {
        if (isGuestExists(id)) {
            throw new IllegalArgumentException("Guest with ID " + id + " already exists.");
        }
        _guestIds.add(id);
    }

    // function to remove a guest from the system
    @Transactional
    public void removeGuest(String id) {
        if (!isGuestExists(id)) {
            throw new IllegalArgumentException("Guest with ID " + id + " does not exist.");
        }
        _guestIds.remove(String.valueOf(id));
    }

    // function to get all the registered users
    @Transactional
    public List<User> get_registeredUsers() {
        return _userRepository.getAllUsers();
    }

    // function to return the purchase history for the user
    @Transactional
    public List<Order> getPurchaseHistory(String username) throws StockMarketException {
        User user = getUserByUsername(username);
        if (user != null) {
            return user.getPurchaseHistory();
        } else {
            throw new UserException("User not found.");
        }
    }

    // change email for a user
    @Transactional
    public void changeEmail(String username, String email) throws StockMarketException {
        User user = getUserByUsername(username);
        if (email == null || email.isEmpty()) {
            throw new UserException("Email is empty.");
        }
        if (!_EmailValidator.isValidEmail(email)) {
            throw new UserException("Email is not valid.");
        }
        user.setEmail(email);
    }

    // getting the user personal details
    @Transactional
    public UserDto getUserDetails(String username) {
        User user = _userRepository.getUserByUsername(username);
        return new UserDto(user.getUserName(), user.getPassword(), user.getEmail(), user.getBirthDate());
    }

    // set the user personal new details
    @Transactional
    public UserDto setUserDetails(String username, UserDto userDto) throws StockMarketException {
        if (userDto.username == null || userDto.username.isEmpty()) {
            throw new StockMarketException("new UserName is empty.");
        }
        if (userDto.email == null || userDto.email.isEmpty()) {
            throw new StockMarketException("new Email is empty.");
        }
        if (!_EmailValidator.isValidEmail(userDto.email)) {
            throw new StockMarketException("new Email is not valid.");
        }
        User user = getUserByUsername(username);

        if (user == null) {
            throw new StockMarketException("Username already exists.");
        } else {
            user.setEmail(userDto.email);
            user.setBirthDate(userDto.birthDate);
        }
        return new UserDto(user.getUserName(), user.getPassword(), user.getEmail(), user.getBirthDate());
    }

    // function to notify a user when an alert is triggered
    @Transactional
    public boolean notifyUser(String targetUser, Alert alert) {
        try {
            NotificationHandler.getInstance().sendMessage(targetUser, alert);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<OrderDto> viewOrderHistory(String username) throws StockMarketException {
        User user = getUserByUsername(username);
        List<Order> orders = user.getPurchaseHistory();
        List<OrderDto> orderDtos = new ArrayList<>();
        for (Order order : orders) {
            orderDtos.add(new OrderDto(order));
        }
        return orderDtos;
    }

    // // // function to initilaize data for UI testing
    // public void initUI() {
    //     _userRepository.addUser(new User("tal", 
    //             this._passwordEncoder.encodePassword("taltul"), "tal@gmail.com", new Date()));
    //     _userRepository.addUser(new User("vladik", 
    //             this._passwordEncoder.encodePassword("123456"), "vladik@gmail.com", new Date()));
    //     _userRepository.addUser(new User("v", 
    //             this._passwordEncoder.encodePassword("123456"), "v@gmail.com", new Date()));
    //     _userRepository.addUser(new User("test", 
    //             this._passwordEncoder.encodePassword("123456"), "v@gmail.com", new Date()));
    //     _userRepository.addUser(new User("metar", 
    //             this._passwordEncoder.encodePassword("123456"), "v@gmail.com", new Date()));
    // }

}
