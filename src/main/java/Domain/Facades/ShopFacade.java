package Domain.Facades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Domain.Entities.Product;
import Domain.Entities.Role;
import Domain.Entities.Shop;
import Domain.Entities.ShopOrder;
import Domain.Entities.ShoppingBasket;
import Domain.Entities.User;
import Domain.Entities.Alerts.Alert;
import Domain.Entities.Alerts.AppointedManagerAlert;
import Domain.Entities.Alerts.AppointedOwnerAlert;
import Domain.Entities.Alerts.FireManagerAlert;
import Domain.Entities.Discounts.BaseDiscount;
import Domain.Entities.Discounts.CategoryFixedDiscount;
import Domain.Entities.Discounts.CategoryPercentageDiscount;
import Domain.Entities.Discounts.ConditionalDiscount;
import Domain.Entities.Discounts.Discount;
import Domain.Entities.Discounts.ProductFixedDiscount;
import Domain.Entities.Discounts.ProductPercentageDiscount;
import Domain.Entities.Discounts.ShopFixedDiscount;
import Domain.Entities.Discounts.ShopPercentageDiscount;
import Domain.Entities.Policies.ProductPolicy;
import Domain.Entities.Policies.ShopPolicy;
import Domain.Entities.Rules.AbstractRule;
import Domain.Entities.Rules.Rule;
import Domain.Entities.Rules.RuleFactory;
import Domain.Entities.enums.Category;
import Domain.Entities.enums.Permission;
import Domain.Repositories.DbDiscountRepository;
import Domain.Repositories.DbPolicyRepository;
import Domain.Repositories.DbProductRepository;
import Domain.Repositories.DbRoleRepository;
import Domain.Repositories.DbShopRepository;
import Domain.Repositories.InterfaceDiscountRepository;
import Domain.Repositories.InterfacePolicyRepository;
import Domain.Repositories.InterfaceProductRepository;
import Domain.Repositories.InterfaceRoleRepository;
import Domain.Repositories.InterfaceShopRepository;
import Dtos.BasicDiscountDto;
import Dtos.ConditionalDiscountDto;
import Dtos.ProductDto;
import Dtos.ProductGetterDto;
import Dtos.ShopDto;
import Dtos.ShopManagerDto;
import Dtos.ShopOrderDto;
import Dtos.Rules.ShoppingBasketRuleDto;
import Dtos.Rules.UserRuleDto;
import Dtos.ShopGetterDto;
import Exceptions.PermissionException;
import Exceptions.ProductDoesNotExistsException;
import Exceptions.StockMarketException;
import Server.notifications.NotificationHandler;

@Service
public class ShopFacade {
    private UserFacade _userFacade;
    private InterfaceShopRepository _shopRepository;
    private InterfaceProductRepository _productRepository;
    private InterfaceRoleRepository _roleRepository;
    private InterfaceDiscountRepository _discountRepository;
    private InterfacePolicyRepository _policyRepository;
    private NotificationHandler _notificationHandler;

    private static final Logger logger = Logger.getLogger(ShopFacade.class.getName());

    @Autowired
    public ShopFacade(DbShopRepository shopRepository, DbProductRepository productRepository,
            DbRoleRepository roleRepository, UserFacade userFacade, NotificationHandler notificationHandler,
            DbDiscountRepository discountRepository, DbPolicyRepository policyRepository) {
        _shopRepository = shopRepository;
        _productRepository = productRepository;
        _roleRepository = roleRepository;
        _userFacade = userFacade;
        _discountRepository = discountRepository;
        _notificationHandler = notificationHandler;
        _policyRepository = policyRepository;

        // For testing UI
        // try {
        // initUI();
        // }
        // catch (StockMarketException e) {
        // e.printStackTrace();
        // }
    }

    // set repositories to be used in test system
    public void setShopFacadeRepositories(InterfaceShopRepository shopRepository,
            InterfaceProductRepository productRepository, InterfaceRoleRepository roleRepository,
            InterfaceDiscountRepository discountRepository, InterfacePolicyRepository policyRepository) {
        _shopRepository = shopRepository;
        _productRepository = productRepository;
        _roleRepository = roleRepository;
        _discountRepository = discountRepository;
        _policyRepository = policyRepository;
    }

    public Shop getShopByShopId(Integer shopId) {
        Shop shop = _shopRepository.findByShopId(shopId);
        shop.setNotificationHandler(_notificationHandler);
        return shop;
    }

    // Checks if a shop ID exists.
    public Boolean isShopIdExist(Integer shopId) {
        return _shopRepository.findByShopId(shopId) == null ? false : true;
    }

    // Open a new shop only if the user is not a manager or owner of another shop.
    @Transactional
    public Integer openNewShop(String userName, ShopDto shopDto) throws StockMarketException {
        // check if the shop name already exists in the system, should be unique
        for (Shop shop : getAllShops()) {
            if (shop.getShopName().equals(shopDto.shopName)) {
                throw new StockMarketException(String.format("Shop name: %s already exists in the system.",
                        shopDto.shopName));
            }
        }

        // check and validate the shop details
        if (shopDto.shopName == null || shopDto.shopName.isEmpty()) {
            throw new StockMarketException("Shop name is null or empty.");
        }
        if (shopDto.bankDetails == null || shopDto.bankDetails.isEmpty()) {
            throw new StockMarketException("Bank details is null or empty.");
        }
        if (shopDto.shopAddress == null || shopDto.shopAddress.isEmpty()) {
            throw new StockMarketException("Shop address is null or empty.");
        }

        Shop shop = new Shop();
        shop.setShopName(shopDto.shopName);
        shop.setBankDetails(shopDto.bankDetails);
        shop.setShopAddress(shopDto.shopAddress);
        shop.setNotificationHandler(_notificationHandler);
        shop.setRoleRepository(_roleRepository);
        shop = _shopRepository.save(shop);
        shop.setShopFounder(userName);
        shop.notifyReOpenShop(userName);
        shop.setShopPolicy(_policyRepository.save(shop.getShopPolicy()));
        _shopRepository.flush();
        return shop.getShopId();
    }

    // Close shop only if the user is the founder of the shop
    @Transactional
    public void closeShop(Integer shopId, String userName) throws StockMarketException {
        logger.info("Trying to close shop with id: " + shopId);
        try {
            if (!isShopIdExist(shopId))
                throw new StockMarketException(String.format("Shop ID: %d does not exist.", shopId));
            else {
                Shop shopToClose = getShopByShopId(shopId);
                shopToClose.setNotificationHandler(_notificationHandler);
                if (shopToClose.checkPermission(userName, Permission.FOUNDER) || _userFacade.isAdmin(userName)) {
                    shopToClose.closeShop();
                    getShopByShopId(shopId).notifyCloseShop(userName);
                } else {
                    throw new StockMarketException(String.format(
                            "User %s can't cloase the Shop: %d. Only the fonder has the permission", userName, shopId));
                }
            }
        } catch (StockMarketException e) {
            throw new StockMarketException(e.getMessage());
        }
        logger.info("Shop with id: " + shopId + " was closed successfully.");
    }

    // Reopen a shop only if the user is the founder of the shop
    @Transactional
    public void reOpenShop(Integer shopId, String userName) throws Exception {
        logger.info("Trying to reopen shop with id: " + shopId);
        try {
            if (!isShopIdExist(shopId))
                throw new Exception(String.format("Shop ID: %d does not exist.", shopId));
            else {
                Shop shopToReOpen = getShopByShopId(shopId);
                shopToReOpen.setNotificationHandler(_notificationHandler);
                if (shopToReOpen.checkPermission(userName, Permission.FOUNDER) || _userFacade.isAdmin(userName)) {
                    getShopByShopId(shopId).notifyReOpenShop(userName);
                    shopToReOpen.reopenShop();
                } else {
                    throw new Exception(String.format(
                            "User %s can't reopen the Shop: %d. Only the fonder has the permission", userName, shopId));
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        logger.info("Shop with id: " + shopId + " was reopened successfully.");
    }

    // Add a product to a shop by its ID.
    @Transactional
    public void addProductToShop(Integer shopId, ProductDto productDto, String userName) throws StockMarketException {
        // If the shop ID does not exist, raise an error
        if (!isShopIdExist(shopId))
            throw new StockMarketException(String.format("Shop ID: %d does not exist.", shopId));
        // If the product name exists in the shop, raise an error
        if (getShopByShopId(shopId).isProductNameExist(productDto.productName))
            throw new StockMarketException(String.format("Product name: %s already exists in shop: %d.",
                    productDto.productName, shopId));
        Shop shop = getShopByShopId(shopId);
        Product newProduct = new Product(productDto.productName, productDto.category, productDto.price, shop);
        newProduct = _productRepository.save(newProduct);
        newProduct.updateProductQuantity(productDto.productQuantity);
        _productRepository.flush();
        shop.addProductToShop(userName, newProduct);
        _shopRepository.flush();
    }

    // Remove a product from a shop by its ID.
    @Transactional
    public synchronized void removeProductFromShop(Integer shopId, ProductDto productDto, String userName)
            throws StockMarketException {
        // If the shop ID does not exist, raise an error
        if (!isShopIdExist(shopId))
            throw new StockMarketException(String.format("Shop ID: %d does not exist.", shopId));
        // If one of the inputs in productDto is null, raise an error
        if (productDto == null || productDto.productName == null || productDto.productName.isEmpty())
            throw new StockMarketException("Product name is null.");
        // If the product name does not exists in the shop, raise an error
        if (!getShopByShopId(shopId).isProductNameExist(productDto.productName))
            throw new StockMarketException(String.format("Product name: %s is not exists in shop: %d.",
                    productDto.productName, shopId));
        getShopByShopId(shopId).setNotificationHandler(_notificationHandler);
        getShopByShopId(shopId).removeProductFromShop(userName, productDto.productName, _productRepository);
        _shopRepository.flush();
    }

    @Transactional
    public void openComplaint(Integer shopId, String userName, String message) throws StockMarketException {
        try {
            if (!isShopIdExist(shopId))
                throw new StockMarketException(String.format("Shop ID: %d does not exist.", shopId));
            else {
                Shop shopToNotify = getShopByShopId(shopId);
                shopToNotify.openComplaint(userName, message);
            }
        } catch (StockMarketException e) {
            throw new StockMarketException(e.getMessage());
        }

    }

    // Edit a product in a shop by its ID.
    @Transactional
    public synchronized void editProductInShop(Integer shopId, ProductDto productDtoOld, ProductDto productDtoNew,
            String userName) throws StockMarketException {
        logger.info("Trying to edit product in shop with id: " + shopId);
        // If the shop ID does not exist, raise an error
        if (!isShopIdExist(shopId))
            throw new StockMarketException(String.format("Shop ID: %d does not exist.", shopId));

        // If one of the inputs in productDto is null, raise an error
        if (productDtoOld == null || productDtoOld.productName == null || productDtoOld.productName.isEmpty())
            throw new StockMarketException("Old product name is null.");
        if (productDtoNew == null || productDtoNew.productName == null || productDtoNew.productName.isEmpty())
            throw new StockMarketException("New product name is null.");
        if (productDtoOld == null || productDtoOld.category == null)
            throw new StockMarketException("Old product category is null.");
        if (productDtoNew == null || productDtoNew.category == null)
            throw new StockMarketException("New product category is null.");
        if (productDtoOld == null || productDtoOld.price == 0.0)
            throw new StockMarketException("Old product price can not be 0.");
        if (productDtoNew == null || productDtoNew.price == 0.0)
            throw new StockMarketException("New product price can not be 0..");

        // If the product name does not exists in the shop, raise an error
        if (!getShopByShopId(shopId).isProductNameExist(productDtoOld.productName))
            throw new StockMarketException(String.format("Product name: %s is not exists in shop: %d.",
                    productDtoOld.productName, shopId));
        logger.info("Product with name: " + productDtoOld.productName + " was found in shop with id: " + shopId);

        // Product product = _productRepository.findById(productDtoOld.productId).get();
        // getShopByShopId(shopId).editProductInShop(userName, product,
        // productDtoNew.productName,
        // productDtoNew.category, productDtoNew.price);

        getShopByShopId(shopId).editProductInShopByName(userName, productDtoOld.productName, productDtoNew.productName,
                productDtoNew.category, productDtoNew.price);

        _productRepository.flush();
        logger.info("Product with name: " + productDtoOld.productName + " was edited successfully.");
    }

    // Retrieves the purchase history for a shop by its ID.
    @Transactional
    public List<ShopOrder> getPurchaseHistory(Integer shopId) {
        List<ShopOrder> purchaseHistory = new ArrayList<>();
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            purchaseHistory = shop.getPurchaseHistory();
        }
        return purchaseHistory;
    }

    // Retrieves the purchase history for a shop by its ID.
    @Transactional
    public List<ShopOrderDto> getPurchaseHistoryDto(Integer shopId) throws StockMarketException {
        List<ShopOrder> purchaseHistory = getPurchaseHistory(shopId);
        List<ShopOrderDto> purchaseHistoryDto = new ArrayList<>();

        for (ShopOrder purchase : purchaseHistory) {
            purchaseHistoryDto.add(new ShopOrderDto(purchase));
        }
        return purchaseHistoryDto;
    }

    // Checks if a user is the owner of a shop.
    @Transactional
    public Boolean isShopOwner(Integer shopId, String userId) throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        try {
            if (shop != null) {
                return shop.isOwnerOrFounderOwner(userId);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // // Adds a basic discount to a shop. Can be Product, Shop or Category
    // discount.
    // @Transactional
    // public int addBasicDiscountToShop(int shopId, String username,
    // BasicDiscountDto discountDto)
    // throws StockMarketException {

    // Shop shop = getShopByShopId(shopId);
    // if (!shop.checkPermission(username, Permission.CHANGE_DISCOUNT_POLICY))
    // throw new PermissionException("User " + username + " has no permission to add
    // discount to shop " + shopId);
    // BaseDiscount discount;
    // if (discountDto.isPrecentage) {
    // if (discountDto.category != null)
    // discount = new CategoryPercentageDiscount(discountDto);
    // else if (discountDto.productId == -1)
    // discount = new ShopPercentageDiscount(discountDto);
    // else
    // discount = new ProductPercentageDiscount(discountDto);
    // } else {
    // if (discountDto.category != null)
    // discount = new CategoryFixedDiscount(discountDto);
    // else if (discountDto.productId == -1)
    // discount = new ShopFixedDiscount(discountDto);
    // else
    // discount = new ProductFixedDiscount(discountDto);
    // }

    // int id = shop.addDiscount(discount);
    // _discountRepository.save(discount);
    // return id;
    // }

    // Adds a conditional discount to a shop.
    @Transactional
    public int addConditionalDiscountToShop(int shopId, String username, ConditionalDiscountDto discountDto)
            throws StockMarketException {

        Shop shop = getShopByShopId(shopId);
        if (!shop.checkPermission(username, Permission.CHANGE_DISCOUNT_POLICY))
            throw new PermissionException("User " + username + " has no permission to add discount to shop " + shopId);

        ConditionalDiscount discount = new ConditionalDiscount(discountDto);
        return shop.addDiscount(discount);
    }

    // // Removes a discount from a shop.
    // @Transactional
    // public void removeDiscountFromShop(int shopId, int discountId, String
    // username) throws StockMarketException {
    // Shop shop = getShopByShopId(shopId);
    // if (!shop.checkPermission(username, Permission.CHANGE_DISCOUNT_POLICY))
    // throw new PermissionException(
    // "User " + username + " has no permission to remove discount from shop " +
    // shopId);
    // shop.removeDiscount(discountId);
    // }

    // this function is responsible searching a product in a shop by its name for
    // all type of users
    // by checking if all inputs are valid and then calling the function in shop
    @Transactional
    public Map<Integer, List<Product>> getProductInShopByName(Integer shopId, String productName)
            throws StockMarketException {
        Map<Integer, List<Product>> productsByShop = new HashMap<>();
        // If productName is null, raise an error
        if (productName == null) {
            throw new StockMarketException("Product name is null.");
        }
        // If shopId is null, search in all shops
        if (shopId == null) {
            for (Shop shop : getAllShops()) {
                List<Product> products = shop.getProductsByName(productName);
                if (!products.isEmpty()) {
                    productsByShop.put(shop.getShopId(), products);
                }
            }
        }
        // Search in a specific shop
        else {
            if (isShopIdExist(shopId)) {
                Shop shop = getShopByShopId(shopId);
                List<Product> products = shop.getProductsByName(productName);
                productsByShop.put(shop.getShopId(), products);
            } else {
                throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
            }
        }
        return productsByShop;
    }

    // this function is responsible return all the shops in the system
    @Transactional
    public List<Shop> getAllShops() {
        return _shopRepository.findAll();
    }

    // this function is responsible return all the shops in the system as DTO
    @Transactional
    public List<ShopDto> getAllShopsDto() {
        List<ShopDto> shops = new ArrayList<>();
        for (Shop shop : getAllShops()) {
            ShopDto shopDto = new ShopDto(shop);
            shops.add(shopDto);
        }
        return shops;
    }

    // this function is responsible getting all the products in a shop by its name
    @Transactional
    public Map<Integer, List<Product>> getProductInShopByCategory(Integer shopId, Category productCategory)
            throws StockMarketException {
        Map<Integer, List<Product>> productsByShop = new HashMap<>();
        // If category is null, raise an error
        if (productCategory == Category.DEFAULT_VAL) {
            throw new StockMarketException("Product category is null.");
        }
        // If shopId is null, search in all shops
        if (shopId == null) {
            for (Shop shop : getAllShops()) {
                List<Product> products = shop.getProductsByCategory(productCategory);
                if (!products.isEmpty()) {
                    productsByShop.put(shop.getShopId(), products);
                }
            }
        }
        // Search in a specific shop
        else {
            if (isShopIdExist(shopId)) {
                Shop shop = getShopByShopId(shopId);
                List<Product> products = shop.getProductsByCategory(productCategory);
                productsByShop.put(shop.getShopId(), products);
            } else {
                throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
            }
        }
        return productsByShop;
    }

    // this function is responsible getting all the products in a shop by there
    // keywords
    @Transactional
    public Map<Integer, List<Product>> getProductsInShopByKeywords(Integer shopId, List<String> keywords)
            throws StockMarketException {
        // If keywords is null, raise an error
        if (keywords == null || keywords.isEmpty()) {
            throw new StockMarketException("Product keywords is null or empty.");
        }
        Map<Integer, List<Product>> productsByShop = new HashMap<>();
        // If shopId is null, search in all shops
        if (shopId == null) {
            for (Shop shop : getAllShops()) {
                List<Product> products = shop.getProductsByKeywords(keywords);
                if (!products.isEmpty()) {
                    productsByShop.put(shop.getShopId(), products);
                }
            }
        }
        // Search in a specific shop
        else {
            if (isShopIdExist(shopId)) {
                Shop shop = getShopByShopId(shopId);
                List<Product> products = shop.getProductsByKeywords(keywords);
                productsByShop.put(shop.getShopId(), products);
            } else {
                throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
            }
        }
        return productsByShop;
    }

    // this function is responsible getting all the products in a shop by there
    // price range
    @Transactional
    public Map<Integer, List<Product>> getProductsInShopByPriceRange(Integer shopId, Double minPrice, Double maxPrice)
            throws StockMarketException {
        Map<Integer, List<Product>> productsByShop = new HashMap<>();
        // If shopId is null, search in all shops
        if (shopId == null) {
            for (Shop shop : getAllShops()) {
                List<Product> products = shop.getProductsByPriceRange(minPrice, maxPrice);
                if (!products.isEmpty()) {
                    productsByShop.put(shop.getShopId(), products);
                }
            }
        }
        // Search in a specific shop
        else {
            if (isShopIdExist(shopId)) {
                Shop shop = getShopByShopId(shopId);
                List<Product> products = shop.getProductsByPriceRange(minPrice, maxPrice);
                productsByShop.put(shop.getShopId(), products);
            } else {
                throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
            }
        }
        return productsByShop;
    }

    // this function is responsible update the quantity of a product in a shop
    @Transactional
    public void updateProductQuantity(String userName, Integer shopId, Integer productId, Integer productQuantity)
            throws StockMarketException {
        logger.info(
                userName + " trying to update product quantity: " + productQuantity + " in shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        if (shop == null)
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));

        Product product = shop.getProductById(productId);
        shop.updateProductQuantity(userName, product, productQuantity);
        _productRepository.flush();
        logger.info("Product quantity was updated successfully.");
    }

    // this function is responsible update the quantity of a product in a shop
    @Transactional
    public void updateProductName(String userName, Integer shopId, Integer productId, String productName)
            throws StockMarketException {
        logger.info(userName + " trying to update product name: " + productName + " in shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        if (shop == null)
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));

        Product product = shop.getProductById(productId);
        shop.updateProductName(userName, product, productName);
        _productRepository.flush();
        logger.info("Product name was updated successfully.");
    }

    // this function is responsible update the quantity of a product in a shop
    @Transactional
    public void updateProductPrice(String userName, Integer shopId, Integer productId, Double productPrice)
            throws StockMarketException {
        logger.info(userName + " trying to update product price: " + productPrice + " in shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        if (shop == null)
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));

        Product product = shop.getProductById(productId);
        shop.updateProductPrice(userName, product, productPrice);
        _productRepository.flush();
        logger.info("Product price was updated successfully.");
    }

    // this function is responsible update the quantity of a product in a shop
    @Transactional
    public void updateProductCategory(String userName, Integer shopId, Integer productId, Category productCategpory)
            throws StockMarketException {
        logger.info(
                userName + " trying to update product category: " + productCategpory + " in shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        if (shop == null)
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));

        Product product = shop.getProductById(productId);
        shop.updateProductCategory(userName, product, productCategpory);
        _productRepository.flush();
        logger.info("Product category was updated successfully.");
    }

    public String getShopFounderUsername(Integer shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return shop.getFounderName();
        }
        return null;
    }

    // Adds a new owner to a shop.
    @Transactional
    public void addShopOwner(String username, Integer shopId, String ownerUsername) throws StockMarketException {
        logger.info(username + " trying to add owner: " + ownerUsername + " to shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
        shop.AppointOwner(username, ownerUsername);
        _shopRepository.flush();
        logger.info("Owner was added successfully.");
        notifyAppointOwner(username, ownerUsername, shopId);
        logger.info(ownerUsername + " was notified that he was appointed as owner.");
    }

    // notify the owner that he was appointed
    @Transactional
    public void notifyAppointOwner(String username, String targetUser, int shopId) {
        Alert alert = new AppointedOwnerAlert(username, targetUser, shopId);
        _userFacade.notifyUser(targetUser, alert);
    }

    // Adds a new manager to a shop.
    @Transactional
    public void addShopManager(String username, Integer shopId, String managerUsername, Set<String> permissions)
            throws Exception {
        logger.info(username + " trying to add manager: " + managerUsername + " to shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            throw new Exception(String.format("Shop ID: %d doesn't exist.", shopId));
        }
        // Here we create a set of permissions from the strings.
        Set<Permission> permissionsSet = permissions.stream()
                .map(permissionString -> Permission.valueOf(permissionString.toUpperCase()))
                .collect(Collectors.toSet());
        shop.AppointManager(username, managerUsername, permissionsSet);
        _shopRepository.flush();
        logger.info("Manager was added successfully.");

        notifyAppointManager(username, managerUsername, permissions, shopId);
        logger.info(managerUsername + " was notified that he was appointed as manager.");
    }

    // notify the manager that he was appointed
    @Transactional
    public void notifyAppointManager(String username, String targetUser, Set<String> permissions, Integer shopId) {
        Alert alert = new AppointedManagerAlert(username, targetUser, permissions, shopId);
        _userFacade.notifyUser(targetUser, alert);
    }

    // Removes a manager from a shop.
    @Transactional
    public Set<String> fireShopManager(String username, Integer shopId, String managerUsername)
            throws StockMarketException {
        logger.info(username + " trying to fire manager: " + managerUsername + " from shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        Set<String> result = new HashSet<String>();
        if (shop == null) {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
        result = shop.fireRole(username, managerUsername);
        _shopRepository.flush();
        logger.info("Manager was fired successfully.");

        notifyFireUser(username, managerUsername, shopId);
        logger.info(managerUsername + " was notified that he was fired.");

        return result;
    }

    // notify the manager that he was fired
    @Transactional
    public void notifyFireUser(String targetUser, String manager, int shopId) {
        Alert alert = new FireManagerAlert(manager, targetUser, shopId);
        _userFacade.notifyUser(targetUser, alert);
    }

    // Resign a role from the shop.
    @Transactional
    public Set<String> resignFromRole(String username, Integer shopId) throws StockMarketException {
        logger.info(username + " trying to resign from shop with id: " + shopId);

        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            return null;
        }

        Set<String> ret = shop.resign(username);
        _shopRepository.flush();
        logger.info("User was resigned successfully.");

        return ret;
    }

    // Modify the permissions of a manager in a shop.
    @Transactional
    public void modifyManagerPermissions(String username, Integer shopId, String managerUsername,
            Set<String> permissions) throws StockMarketException {
        logger.info(username + " trying to modify permissions: " + permissions + " of manager: " + managerUsername
                + " in shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
        // Here we create a set of permissions from the strings.
        Set<Permission> permissionsSet = permissions.stream()
                .map(permissionString -> Permission.valueOf(permissionString.toUpperCase()))
                .collect(Collectors.toSet());
        shop.modifyPermissions(username, managerUsername, permissionsSet);
        _shopRepository.flush();
        logger.info("Permissions were modified successfully.");
    }

    // this function returns the shop policy
    @Transactional
    public String getShopPolicyInfo(Integer shopId) throws StockMarketException {
        if (isShopIdExist(shopId)) {
            Shop shop = getShopByShopId(shopId);
            return shop.getShopPolicyInfo();
        } else {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
    }

    // this function returns the shop policy
    @Transactional
    public List<ShoppingBasketRuleDto> getShopPolicy(Integer shopId) throws StockMarketException {
        if (isShopIdExist(shopId)) {
            Shop shop = getShopByShopId(shopId);
            List<AbstractRule<ShoppingBasket>> rules = shop.getShopPolicy().getRules();
            List<ShoppingBasketRuleDto> rulesDto = rules.stream()
                    .map(rule -> RuleFactory.createShoppingBasketRuleDto(rule)).toList();
            return rulesDto;
        } else {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
    }

    // this function returns the shop policy
    @Transactional
    public List<UserRuleDto> getProductPolicy(Integer shopId, Integer productId) throws StockMarketException {
        if (isShopIdExist(shopId)) {
            Shop shop = getShopByShopId(shopId);
            Product product = shop.getProductById(productId);
            List<AbstractRule<User>> rules = product.getProductPolicy().getRules();
            List<UserRuleDto> rulesDto = rules.stream().map(rule -> RuleFactory.createProductRule(rule)).toList();
            return rulesDto;
        } else {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
    }

    // this function returns the product policy
    @Transactional
    public String getProductPolicyInfo(Integer shopId, Integer productId) throws StockMarketException {
        if (isShopIdExist(shopId)) {
            Shop shop = getShopByShopId(shopId);
            return shop.getProductPolicyInfo(productId);
        } else {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
    }

    // this function returns the shop discounts
    @Transactional
    public String getShopDiscountsInfo(Integer shopId) throws StockMarketException {
        if (isShopIdExist(shopId)) {
            Shop shop = getShopByShopId(shopId);
            return shop.getShopDiscountsInfo();
        } else {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
    }

    // this function returns the product discounts
    @Transactional
    public String getProductDiscountsInfo(Integer shopId, Integer productId) throws StockMarketException {
        if (isShopIdExist(shopId)) {
            Shop shop = getShopByShopId(shopId);
            return shop.getProductDiscountsInfo(productId);
        } else {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
    }

    // this function returns the shop general info
    @Transactional
    public String getShopGeneralInfo(Integer shopId) throws StockMarketException {
        if (isShopIdExist(shopId)) {
            Shop shop = getShopByShopId(shopId);
            return shop.getShopGeneralInfo();
        } else {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
    }

    // this function returns the product general info
    @Transactional
    public String getProductGeneralInfo(Integer shopId, Integer productId) throws Exception {
        if (isShopIdExist(shopId)) {
            Shop shop = getShopByShopId(shopId);
            return shop.getProductGeneralInfo(productId);
        } else {
            throw new Exception(String.format("Shop ID: %d doesn't exist.", shopId));
        }
    }

    // this function adds a rating to a product
    @Transactional
    public void addProductRating(Integer shopId, Integer productId, Integer rating) throws StockMarketException {
        logger.info("Trying to add rating: " + rating + " to product with id: " + productId + " in shop with id: "
                + shopId);
        if (!isShopIdExist(shopId))
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));

        Shop shop = getShopByShopId(shopId);
        Product product = shop.getProductById(productId);
        shop.addProductRating(product, rating);
        _productRepository.flush();
        logger.info("Rating was added successfully.");
    }

    // this function adds a review to a product
    @Transactional
    public void addProductReview(String username, Integer shopId, Integer productId, String review)
            throws StockMarketException {
        if (!isShopIdExist(shopId)) {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
        Shop shop = getShopByShopId(shopId);
        shop.addReview(username, productId, review);
    }

    // this function adds a rating to a shop
    @Transactional
    public void addShopRating(Integer shopId, Integer rating) throws StockMarketException {
        if (!isShopIdExist(shopId))
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));

        Shop shop = getShopByShopId(shopId);
        shop.addShopRating(rating);
        _shopRepository.flush();
    }

    // Returns the shop name if exists, else returns null.
    @Transactional
    public String getShopName(Integer shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return shop.getShopName();
        }
        return null;
    }

    // Returns the shop bank details if exists, else returns null.
    @Transactional
    public String getShopBankDetails(Integer shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return shop.getBankDetails();
        }
        return null;
    }

    // Returns the shop address if exists, else returns null.
    @Transactional
    public String getShopAddress(Integer shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return shop.getShopAddress();
        }
        return null;
    }

    // Returns all the products in a shop by its ID.
    @Transactional
    public List<Product> getAllProductsInShopByID(Integer shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return shop.getAllProductsList();
        }
        return null;
    }

    // Returns all the products in a shop by its ID as DTO.
    @Transactional
    public List<ProductDto> getAllProductsDtoInShopByID(Integer shopId) {
        List<ProductDto> productDtos = new ArrayList<>();

        List<Product> products = getAllProductsInShopByID(shopId);
        for (Product product : products) {
            productDtos.add(new ProductDto(product));
        }
        return productDtos;
    }

    // Returns all shopIds of shops with the input name.
    @Transactional
    public List<Integer> getShopIdsByName(String shopName) {
        List<Integer> shopIds = new ArrayList<>();
        for (Shop shop : getAllShops()) {
            if (shop.getShopName().equals(shopName)) {
                shopIds.add(shop.getShopId());
            }
        }
        return shopIds;
    }

    // Returns all shopIds of shops that contain the input name.
    @Transactional
    public List<Integer> getShopIdsThatContainName(String shopName) {
        shopName = shopName.toLowerCase();
        List<Integer> shopIds = new ArrayList<>();
        for (Shop shop : getAllShops()) {
            if (shop.getShopName().toLowerCase().contains(shopName)) {
                shopIds.add(shop.getShopId());
            }
        }
        return shopIds;
    }

    // this function is responsible for changing the shop policy
    @Transactional
    public void changeShopPolicy(String username, int shopId, List<ShoppingBasketRuleDto> rules)
            throws StockMarketException {
        logger.info(username + " trying to change shop policy in shop with id: " + shopId + " to: " + rules);
        Shop shop = getShopByShopId(shopId);
        if (shop == null)
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        if (shop.isShopClosed())
            throw new StockMarketException(String.format("Shop ID: %d is closed.", shopId));
        // List<ShoppingBasketRuleDto> shopRules = new ArrayList<>();
        // shopRules.addAll(minBasketRules);
        // shopRules.addAll(minProductRules);
        // delete old policy
        ShopPolicy oldPolicy = shop.getShopPolicy();
        if (oldPolicy != shop.changeShopPolicy(username, rules)) {
            _policyRepository.delete(oldPolicy);
            shop.setShopPolicy(_policyRepository.save(shop.getShopPolicy()));
        }
        _policyRepository.flush();
        _shopRepository.flush();
        logger.info("Shop policy was changed successfully.");
    }

    // this function is responsible for changing the product policy
    @Transactional
    public void changeProductPolicy(String username, int shopId, int productId, List<UserRuleDto> productRules)
            throws StockMarketException {
        logger.info(username + " trying to change product policy in shop with id: " + shopId + " to: " + productRules);
        Shop shop = getShopByShopId(shopId);
        if (shop == null)
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        if (shop.isShopClosed())
            throw new StockMarketException(String.format("Shop ID: %d is closed.", shopId));
        ProductPolicy oldPolicy = shop.getProductById(productId).getProductPolicy();
        if (oldPolicy != shop.changeProductPolicy(username, productId, productRules)) {
            _policyRepository.delete(oldPolicy);
            shop.getProductById(productId)
                    .setProductPolicy(_policyRepository.save(shop.getProductById(productId).getProductPolicy()));
        }
        _policyRepository.flush();
        _shopRepository.flush();
        logger.info("Product policy was changed successfully.");
    }

    // This function is responsible for getting all the shops in the system
    @Transactional
    public List<ShopGetterDto> getShopsEntities() {
        List<Shop> shops = getAllShops();
        List<ShopGetterDto> shopsDto = new ArrayList<>();
        for (Shop shop : shops) {
            shopsDto.add(new ShopGetterDto(shop));
        }
        return shopsDto;
    }

    // This function is responsible for getting all the information about a shop
    public ShopDto getShopInfo(Integer shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return new ShopDto(shop);
        }
        return null;
    }

    // Get the permissions of a user in a shop
    @Transactional
    public List<String> getShopManagerPermissions(String username, int shopId) throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
        Set<Permission> permissions = shop.getRole(username).getPermissions();
        List<String> permissionsList = permissions.stream().map(permission -> permission.toString())
                .collect(Collectors.toList());
        return permissionsList;
    }

    // Get all the shops that the user has a role in.
    @Transactional
    public List<Integer> getUserShops(String username) throws StockMarketException {
        List<Integer> shops = new ArrayList<>();
        for (Shop shop : getAllShops()) {
            if (shop.checkIfHasRole(username)) {
                shops.add(shop.getShopId());
            }
        }
        return shops;
    }

    // Get all the shops names that the user has a role in.
    @Transactional
    public List<String> getUserShopsNames(String username) throws StockMarketException {
        List<String> shops = new ArrayList<>();
        for (Shop shop : getAllShops()) {
            if (shop.checkIfHasRole(username)) {
                shops.add(shop.getShopName());
            }
        }
        return shops;
    }

    // Adds keywords to a product in a shop
    @Transactional
    public void addKeywordsToProductInShop(String username, Integer shopId, Integer productId, List<String> keywords)
            throws StockMarketException {
        logger.info(username + " trying to add keywords: " + keywords + " to product with id: " + productId
                + " in shop with id: " + shopId);
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
        shop.addKeywordsToProduct(username, productId, keywords);

        _productRepository.save(shop.getProductById(productId));
        _productRepository.flush();

        logger.info("Keywords were added successfully.");
    }

    // function to initilaize data for UI testing
    public void initUI() throws StockMarketException {
        // Shop shop = new Shop(10, "shopUITest", "Tal", "bankUITest", "addressUITest");
        // _shopRepository.addShop(shop);
        // Product product = new Product(10, "productUITest", Category.ELECTRONICS,
        // 100.0);
        // product.updateProductQuantity(10);
        // shop.addProductToShop("Tal", product);

        // openNewShop("tal", new ShopDto("shopUITest1", "bankUITest",
        // "addressUITest1"));
        // openNewShop("tal", new ShopDto("shopUITest2", "bankUITest2",
        // "addressUITest2"));
        // addProductToShop(0, new ProductDto("productUITest1", Category.ELECTRONICS,
        // 40.0, 10), "tal");
        // addProductToShop(0, new ProductDto("productUITest2", Category.ELECTRONICS,
        // 30.0, 10), "tal");
        // addProductToShop(0, new ProductDto("productUITest3", Category.ELECTRONICS,
        // 10.0, 10), "tal");
        // addProductToShop(0, new ProductDto("productUITest4", Category.ELECTRONICS,
        // 20.0, 10), "tal");
        // addProductToShop(1, new ProductDto("productUITest5", Category.ELECTRONICS,
        // 10.5, 10), "tal");
        // addProductToShop(1, new ProductDto("productUITest6", Category.ELECTRONICS,
        // 50.0, 10), "tal");
        // addProductToShop(1, new ProductDto("productUITest7", Category.ELECTRONICS,
        // 30.0, 10), "tal");
        // addProductToShop(1, new ProductDto("productUITest8", Category.ELECTRONICS,
        // 50.0, 10), "tal");
        // addProductToShop(1, new ProductDto("productUITest9", Category.ELECTRONICS,
        // 20.0, 10), "tal");
    }

    // this function is responsible for getting all the shop managers
    @Transactional
    public List<ShopManagerDto> getShopManagers(String username, int shopId) throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            return null;
        }
        Map<String, Role> roles = shop.getUserToRoleMap(username, false);
        List<ShopManagerDto> managers = new ArrayList<>();
        for (Map.Entry<String, Role> entry : roles.entrySet()) {
            Set<Permission> permissions = entry.getValue().getPermissions();
            String role;
            if (permissions.contains(Permission.FOUNDER)) {
                role = "Founder";
            } else if (permissions.contains(Permission.OWNER)) {
                role = "Owner";
            } else {
                role = "Manager";
            }
            ShopManagerDto manager = new ShopManagerDto(entry.getKey(), role, permissions);
            managers.add(manager);
        }
        return managers;
    }

    // this function is responsible for getting all the subordinates of a manager
    @Transactional
    public List<ShopManagerDto> getMySubordinates(String username, int shopId) throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            return null;
        }
        Map<String, Role> roles = shop.getUserToRoleMap(username, true);
        Role manager = shop.getRole(username);
        Set<String> subordinates = manager.getAppointments();
        List<ShopManagerDto> managers = new ArrayList<>();
        for (Map.Entry<String, Role> entry : roles.entrySet()) {
            if (subordinates.contains(entry.getKey())) {
                Set<Permission> permissions = entry.getValue().getPermissions();
                String role;
                if (permissions.contains(Permission.FOUNDER)) {
                    role = "Founder";
                } else if (permissions.contains(Permission.OWNER)) {
                    role = "Owner";
                } else {
                    role = "Manager";
                }
                ShopManagerDto subordinate = new ShopManagerDto(entry.getKey(), role, permissions);
                managers.add(subordinate);
            }
        }
        return managers;
    }

    // returns shopID, name and Rating for response.
    // for example : " */Id/* 1 */Name/* shop1 */Rating/* 4.5"
    public String getShopStringForSearchById(int shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return shop.getShopStringForSearch();
        }
        return null;
    }

    public List<BasicDiscountDto> getShopDiscounts(String username, int shopId) throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            return null;
        }
        Map<Integer, Discount> discounts = shop.getDiscounts();
        List<BasicDiscountDto> discounts_list = new ArrayList<>();
        for (Map.Entry<Integer, Discount> entry : discounts.entrySet()) {
            Discount discount = entry.getValue();
            BasicDiscountDto discountDto = discount.getDto();
            discounts_list.add(discountDto);
        }
        return discounts_list;
    }

    /**
     * add a new discount to the shop
     * 
     * @param discountDto the discount to add
     * @param shopId      the shop
     * @throws StockMarketException
     */
    public void addShopDiscount(BasicDiscountDto discountDto, Integer shopId) throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            throw new StockMarketException("Shop " + shopId + " does not exist");
        }
        Discount discount;
        // create proper discount
        if (discountDto.isPrecentage) {
            if (discountDto.category != null) {
                discount = new CategoryPercentageDiscount(discountDto);
            } else {
                if (discountDto.productId == -1) {
                    discount = new ShopPercentageDiscount(discountDto);
                } else {
                    // check if product with this id exists.
                    if (!shop.isProductExist(discountDto.productId)) {
                        throw new StockMarketException(
                                "Prodcut with id " + discountDto.productId + " does not exist in shop " + shopId);
                    }
                    discount = new ProductPercentageDiscount(discountDto);
                }
            }
        } else {
            if (discountDto.category != null) {
                discount = new CategoryFixedDiscount(discountDto);
            } else {
                if (discountDto.productId == -1) {
                    discount = new ShopFixedDiscount(discountDto);
                } else {
                    // check if product with this id exists.
                    if (!shop.isProductExist(discountDto.productId)) {
                        throw new StockMarketException(
                                "Prodcut with id " + discountDto.productId + " does not exist in shop " + shopId);
                    }
                    discount = new ProductFixedDiscount(discountDto);
                }
            }
        }
        // Attach the Discount entity to the persistence context and save it
        discount = _discountRepository.save(discount);

        // Add the Discount to the Shop and save the Shop entity
        shop.addDiscount(discount);
        _shopRepository.flush();
    }

    /**
     * Delete a discount from the shop
     * 
     * @param discountDto the discount to delete
     * @param shopId      the shop
     * @throws StockMarketException
     */
    public void deleteShopDiscount(BasicDiscountDto discountDto, Integer shopId) throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            throw new StockMarketException("Shop " + shopId + " does not exist");
        }
        _discountRepository.deleteById(discountDto.id);
        shop.removeDiscount(discountDto.id);
        _shopRepository.flush();
    }

    public List<BasicDiscountDto> getProductDiscounts(int shopId, int productId) throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            return null;
        }
        Map<Integer, Discount> discounts = shop.getProductDiscounts(productId);
        List<BasicDiscountDto> discounts_list = new ArrayList<>();
        for (Map.Entry<Integer, Discount> entry : discounts.entrySet()) {
            Discount discount = entry.getValue();
            BasicDiscountDto discountDto = discount.getDto();
            discounts_list.add(discountDto);
        }
        return discounts_list;
    }

    // Update the permissins of manager in shop.
    @Transactional
    public void updatePermissions(String username, Integer shopId, String managerUsername, Set<String> permissions)
            throws StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop == null) {
            throw new StockMarketException(String.format("Shop ID: %d doesn't exist.", shopId));
        }
        // Here we create a set of permissions from the strings.
        Set<Permission> permissionsSet = permissions.stream()
                .map(permissionString -> Permission.valueOf(permissionString.toUpperCase()))
                .collect(Collectors.toSet());
        shop.setNotificationHandler(_notificationHandler);
        shop.modifyPermissions(username, managerUsername, permissionsSet);
        _shopRepository.flush();
    }

    // this function returns the shop id by its name and founder
    public int getShopIdByShopNameAndFounder(String founder, String shopName) {
        for (Shop shop : getAllShops()) {
            if (shop.getShopName().equals(shopName) && shop.getFounderName().equals(founder)) {
                return shop.getShopId();
            }
        }
        return -1;
    }

    // shop names are unique, so we can get the shop id by its name
    public int getShopIdByShopName(String string) {
        for (Shop shop : getAllShops()) {
            if (shop.getShopName().equals(string)) {
                return shop.getShopId();
            }
        }
        return -1;
    }

    // this function returns the product id by its name and shop id
    public int getProductIdByProductNameAndShopId(String product_name, int shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            for (Product product : shop.getAllProductsList()) {
                if (product.getProductName().equals(product_name)) {
                    return product.getProductId();
                }
            }
        }
        return -1;
    }

    // get shopDto including rating by shopId
    public ShopDto getShopDtoById(int shopId) {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return new ShopDto(shopId, shop.getShopName(), shop.getBankDetails(), shop.getShopAddress(),
                    shop.getShopRating());
        }
        return null;
    }

    // returns productGetterDto - a very detailed object including discounts
    public ProductGetterDto getProductDetaildDtoById(int shopId, int productId)
            throws ProductDoesNotExistsException, StockMarketException {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            Product product = shop.getProductById(productId);
            if (product != null) {
                ProductGetterDto productGetterDto = new ProductGetterDto(product);
                productGetterDto.setProductDiscounts(getProductDiscounts(shopId, productId));
                return productGetterDto;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public Product getProductById(int productID) {
        return _productRepository.getById(productID);
    }
}
