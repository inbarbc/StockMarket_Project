package Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Scanner;
import java.util.Set;

import org.springframework.stereotype.Service;

import Domain.User;
import Domain.ExternalServices.PaymentService.AdapterPayment;
import Domain.ExternalServices.SupplyService.AdapterSupply;
import Domain.Facades.ShopFacade;
import Domain.Facades.ShoppingCartFacade;
import Domain.Facades.UserFacade;
import Domain.Repositories.InterfaceShopRepository;
import Domain.Repositories.InterfaceShoppingCartRepository;
import Domain.Repositories.InterfaceUserRepository;
import Domain.Repositories.MemoryShopRepository;
import Domain.Repositories.MemoryShoppingCartRepository;
import Domain.Repositories.MemoryUserRepository;
import Dtos.ProductDto;
import Dtos.ShopDto;
import Dtos.UserDto;
import Exceptions.StockMarketException;
import Server.Controllers.UserController;
import Server.notifications.NotificationHandler;
import ServiceLayer.UserService;
import enums.Category;

@Service
public class MarketSystem {

    public final static String external_system_url = "https://damp-lynna-wsep-1984852e.koyeb.app/";
    public final static String tests_config_file_path = "..\\StockMarket_Project\\src\\main\\java\\Server\\Configuration\\tests_config.txt";
    public static String instructions_config_path = "..\\StockMarket_Project\\src\\main\\java\\Server\\Configuration\\instructions_config.txt";
    public final static String system_config_path = "..\\StockMarket_Project\\src\\main\\java\\Server\\Configuration\\system_config.txt";

    private AdapterPayment payment_adapter;
    private AdapterSupply supply_adapter;

    public static boolean test_flag = false;

    private static final Logger logger = Logger.getLogger(MarketSystem.class.getName());

    private ShopFacade shopFacade;
    private UserFacade userFacade;
    private ShoppingCartFacade shoppingCartFacade;

    public MarketSystem() throws StockMarketException {
        shopFacade = ShopFacade.getShopFacade();
        userFacade = UserFacade.getUserFacade();
        shoppingCartFacade = ShoppingCartFacade.getShoppingCartFacade();

        this.init_market(system_config_path);
    }

    // initiate the system using the args config files
    public void init_market(String config_file_path) throws StockMarketException{
        logger.info("Start Init Market");
        logger.info("Configuration File Path: "+config_file_path);
        String[] instructions;
        instructions = read_config_file(config_file_path);
        String external_services_instruction = instructions[0];
        set_external_services(external_services_instruction);
        connect_to_external_services();
        String database_instruction = instructions[1];
        set_database(database_instruction);
    }

    public AdapterPayment getPayment_adapter() {
        return payment_adapter;
    }
    public AdapterSupply getSupply_adapter() {
        return supply_adapter;
    }

    /**
     * reading the data from the configuration file.
     * @param config_path the path of the configuration file.
     * @return the 2 config instructions, 1) external services 2) database
     * @throws StockMarketException if the format file is unmatched.
     */
    public String[] read_config_file(String config_path) throws StockMarketException {
        String[] to_return = new String[2];
        int counter = 0;
        try {
            File file = new File(config_path);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String instruction = scanner.nextLine();
                if (!instruction.equals("")) {
                    if (counter > 1){
                        throw new StockMarketException("Config File - Illegal Format.");
                    }
                    to_return[counter]  = instruction;
                    counter++;
                }
            }
        }
        catch (FileNotFoundException e) {throw new StockMarketException("Config File - File Not Found");}
        if (counter != 2) {throw new StockMarketException("Config File - Format File Unmatched.");}
        return to_return;
    }

    /** Connect the system to the external services after set the services according the configuration file.
     * @throws StockMarketException if the handshake fail.
     */
    private void connect_to_external_services() throws StockMarketException {
        logger.info("System Start Connect To External Services");
        //boolean connect_to_external_systems = payment_adapter.handshake() && supply_adapter.handshake();
        boolean connect_to_external_systems = payment_adapter.ConnectToService() && supply_adapter.ConnectToService();
        if (!connect_to_external_systems) // have to exit
        {
            throw new StockMarketException("Cant Connect To The External Systems");
        }
    }

    /**
     * Requirement 1.3 & 1.4
     *
     * this method crate adapters to the external services.
     * @param config - "external_services:demo" or "external_services:real"
     * @throws StockMarketException if the input is illegal.
     */
    public void set_external_services(String config) throws StockMarketException {
        if (config.equals("external_services:tests")){
            logger.info("Set Tests External Services");
            payment_adapter = AdapterPayment.getAdapterPayment();
            supply_adapter = AdapterSupply.getAdapterSupply();
            // this.payment_adapter = new PaymentAdapterTests();
            // this.supply_adapter = new SupplyAdapterTests();
            // NotificationHandler.setTestsHandler();
        }
        else if (config.equals("external_services:fail_tests")){
            logger.info("Set Denied Tests External Services");
            payment_adapter = AdapterPayment.getAdapterPayment();
            supply_adapter = AdapterSupply.getAdapterSupply();
            // this.payment_adapter = new PaymentAdapter() {
            //     @Override
            //     public boolean handshake() {
            //         return false;
            //     }

            //     @Override
            //     public int payment(PaymentInfo paymentInfo, double price) {
            //         return -1;
            //     }

            //     @Override
            //     public int cancel_pay(int transaction_id) {
            //         return -1;
            //     }
            // };
            // this.supply_adapter = new SupplyAdapterTests();
            // NotificationHandler.setTestsHandler();
        }
        else if (config.equals("external_services:real")){
            logger.info("Set Real External Services");
            payment_adapter = AdapterPayment.getAdapterPayment();
            supply_adapter = AdapterSupply.getAdapterSupply();
            // this.payment_adapter = new PaymentAdapterImpl();
            // this.supply_adapter = new SupplyAdapterImpl();
        }
        else {
            throw new StockMarketException("System Config File - Illegal External Services Data.");
        }
    }

    /**
     * this method init system database,
     * if the demo option on, the system will init data from the data config file,
     *      the init can failed and system keep running without data.
     * if the real option on, the method will try to connect the real database.
     * @param config - configuration instruction - "database:demo" or "database:real".
     * @throws StockMarketException if the connection to DB fail OR wrong format of the config instruction.
     */
    private void set_database(String config) throws StockMarketException{
        // database:real/demo
        if (config.equals("database:tests")){
            // no db
            logger.info("Init Data For Tests: No Database");
            // NotificationHandler.setTestsHandler();
            test_flag = true;
            // HibernateUtils.set_tests_mode();
            InterfaceShoppingCartRepository shoppingCartRepository = new MemoryShoppingCartRepository();
            InterfaceShopRepository shopRepository = new MemoryShopRepository(new ArrayList<>());
            InterfaceUserRepository userRepository = new MemoryUserRepository(new ArrayList<>());
        }
        else if (config.equals("database:tests_load_and_drop")){
            // load from test-db
                logger.info("Init & Drop Data For Tests From Exist Database");
                // HibernateUtils.set_load_tests_mode();
        }
        else if (config.equals("database:tests_init")){
            // for demo tests in configuration tests.
            logger.info("Init Data For Tests From Empty Database");
            // HibernateUtils.set_init_test_config();
            InterfaceShoppingCartRepository shoppingCartRepository = new MemoryShoppingCartRepository();
            InterfaceShopRepository shopRepository = new MemoryShopRepository(new ArrayList<>());
            InterfaceUserRepository userRepository = new MemoryUserRepository(new ArrayList<>());
        }
        else if (config.equals("database:tests_load")){
            logger.info("Init Data For Tests From Exist Database");
            // HibernateUtils.set_tests_load_config();
        }
        else if (config.equals("database:real_load")){
            try
            {
                // HibernateUtils.set_normal_use();
                // SystemLogger.getInstance().add_log("Init Data From Database");
                // StoreController.get_instance().load(true);
                // UserController.get_instance().load(true);
                // QuestionController.getInstance().load();
            }
            catch (Exception e){
                throw new StockMarketException("Cant Connect To Database.");
            }
        }
        else if (config.equals(("database:real_init"))){
            // HibernateUtils.set_demo_use();
            // TODO: change to real time system repository
            InterfaceShoppingCartRepository shoppingCartRepository = new MemoryShoppingCartRepository();
            shoppingCartFacade.setShoppingCartRepository(shoppingCartRepository);
            InterfaceShopRepository shopRepository = new MemoryShopRepository(new ArrayList<>());
            shopFacade.setShopRepository(shopRepository);
            InterfaceUserRepository userRepository = new MemoryUserRepository(new ArrayList<>());
            userFacade.setUserRepository(userRepository);
            
            logger.info("Init Data From Instructions File, Data File Path: " + instructions_config_path);
            init_data_to_market(instructions_config_path);
        }
        else {
            throw new StockMarketException("System Config File - Illegal Database Data.");
        }
    }

    /**
     * init date from the instructions configuration file.
     * this method should keep the logic order of system instructions.
     * "" is legal input -> the method wouldn't do anything and keep going.
     * @param instructions_config_path - location of the instruction config file.
     * @return true if the system load data successfully.
     *  false if was illegal instructions order OR illegal format instruction.
     */
    public void init_data_to_market(String instructions_config_path){
        // HashMap<String, MarketFacade> facades = new HashMap<>();
        try{
            File file = new File(instructions_config_path);
            Scanner scanner = new Scanner(file);
            // HibernateUtils.beginTransaction();
            // HibernateUtils.setBegin_transaction(false);
            while (scanner.hasNextLine()){
                String instruction = scanner.nextLine();
                if (!instruction.equals("")){
                    String[] instruction_params = instruction.split("#");
                    run_instruction(instruction_params);
                }
            }
            // HibernateUtils.setBegin_transaction(true);
            // HibernateUtils.commit();
        } catch (Exception e) {
            // HibernateUtils.setBegin_transaction(true);
            // HibernateUtils.rollback();
            logger.info("Init Data Demo Fail, The System Run With No Data :" + e.getMessage());
            // have to reset all the data of the market and stop the method.
            // for (MarketFacade marketFacade : facades.values()){
            //     marketFacade.clear();
            // }
            // facades.clear();
        }
    }

    /**
     * execute instructions from the init data config file.
     * @param instruction_params - instruction to execute.
     * @param facades - the data structure who managed the initialization of data.
     * @throws Exception in bad format or bad logical order of instructions.
     */
    private void run_instruction(String[] instruction_params) throws Exception {
        String instruction = instruction_params[0];

        // handle instructions :
        if (instruction.equals("logIn")){
            //logIn#user_name#password
            userFacade.logIn(instruction_params[1], instruction_params[2]);
        }
        
        else if (instruction.equals("register")){
            //register#user_name#password#email#birthdate
            LocalDate localdate = LocalDate.parse(instruction_params[4], DateTimeFormatter.ISO_LOCAL_DATE);
            @SuppressWarnings("deprecation")
            Date birthdate = new Date(localdate.getYear(), localdate.getMonthValue(), localdate.getDayOfMonth());
            UserDto userDto = new UserDto(instruction_params[1], instruction_params[2], instruction_params[3], birthdate);
            userFacade.register(userDto);
        }

        else if (instruction.equals("add_admin")){
            //add_admin#user_name#password#email#birthdate
            LocalDate localdate = LocalDate.parse(instruction_params[4], DateTimeFormatter.ISO_LOCAL_DATE);
            @SuppressWarnings("deprecation")
            Date birthdate = new Date(localdate.getYear(), localdate.getMonthValue(), localdate.getDayOfMonth());
            UserDto userDto = new UserDto(instruction_params[1], instruction_params[2], instruction_params[3], birthdate);
            userFacade.register(userDto);
            User user = userFacade.getUserByUsername(instruction_params[1]);
            user.setIsSystemAdmin(true);
        }

        else if (instruction.equals("logOut")){
            //logOut#user_name
            userFacade.logOut(instruction_params[1]);
        }

        else if (instruction.equals("add_product_to_cart")){
            //add_product_to_cart#user_name#product_name#shop_name
            int shopId = shopFacade.getShopIdByShopName(instruction_params[3]);
            int productId = shopFacade.getProductIdByProductNameAndShopId(instruction_params[2], shopId);
            shoppingCartFacade.addProductToUserCart(instruction_params[1], productId, shopId);
        }

        else if (instruction.equals("buy_cart")){
        }

        else if (instruction.equals("open_shop")){
            //open_shop#user_name#shop_name#bank_details#shop_address
            ShopDto shopDto = new ShopDto(instruction_params[2], instruction_params[3], instruction_params[4]);
            shopFacade.openNewShop(instruction_params[1], shopDto);
        }

        else if (instruction.equals("rate_product")){
        }

        else if (instruction.equals("rate_shop")){
            //rate_shop#user_name#shop_name#rating
            int shopId = shopFacade.getShopIdByShopName(instruction_params[2]);
            shopFacade.addShopRating(shopId, Integer.parseInt(instruction_params[3]));
        }
        
        else if (instruction.equals("add_product_to_shop")){
            //add_product_to_shop#user_name#shop_name#category#product_name#price#quantity
            ProductDto productDto = new ProductDto(instruction_params[3], Category.valueOf(instruction_params[4]), Integer.parseInt(instruction_params[5]), Integer.parseInt(instruction_params[6]));
            int shopId = shopFacade.getShopIdByShopNameAndFounder(instruction_params[1], instruction_params[2]);
            shopFacade.addProductToShop(shopId, productDto, instruction_params[1]);
        }
        
        else if (instruction.equals("appoint_shop_owner")){
            //appoint_shop_owner#founder_user_name#shop_name#owner_user_name
            int shopId = shopFacade.getShopIdByShopNameAndFounder(instruction_params[1], instruction_params[2]);
            shopFacade.addShopOwner(instruction_params[1], shopId, instruction_params[3]);

        }

        else if (instruction.equals("appoint_shop_manager")){
            //appoint_shop_manager#founder_user_name#shop_name#manager_user_name#permission1#permission2#...
            int shopId = shopFacade.getShopIdByShopNameAndFounder(instruction_params[1], instruction_params[2]);
            Set<String> permissions = new HashSet<>();
            for (int i = 4; i < instruction_params.length; i++){
                permissions.add(instruction_params[i]);
            }
            shopFacade.addShopManager(instruction_params[1], shopId, instruction_params[3], permissions);
        }

        else if (instruction.equals("close_shop")){
            //close_shop#user_name#shop_name
            int shopId = shopFacade.getShopIdByShopNameAndFounder(instruction_params[1], instruction_params[2]);
            shopFacade.closeShop(shopId, instruction_params[2]);
        }

        else if (instruction.equals("reopen_shop")){
            //reopen_shop#user_name#shop_name
            int shopId = shopFacade.getShopIdByShopNameAndFounder(instruction_params[1], instruction_params[2]);
            shopFacade.reOpenShop(shopId, instruction_params[2]);
        }
        
        else{
            throw new Exception("Illegal Instruction Input");
        }
    }
}
