package ServiceLayer;

import java.util.logging.Logger;

import org.apache.catalina.servlets.DefaultServlet.SortManager.Order;
import org.springframework.stereotype.Service;

import java.lang.module.ModuleDescriptor.Opens;
import java.util.List;
import java.util.logging.Level;

import Domain.Product;
import Domain.ShopController;
import Domain.ShopOrder;
import Domain.ShoppingBasket;

@Service
public class ShopService {
    private ShopController _shopController;
    private static final Logger logger = Logger.getLogger(ShopController.class.getName());

    public ShopService(){
        _shopController = ShopController.getShopController();
    }

    /**
    * Opens a new shop with the specified shop ID and user name.
    * 
    * @param shopId    The ID of the new shop to be opened.
    * @param userName  The name of the user opening the shop (founder).
    * @return          A response indicating the success or failure of the operation.
    */
    public Response OpenNewShop(Integer shopId, String userName)
    {
        Response response = new Response();
        try
        {
            _shopController.OpenNewShop(shopId, userName);
            logger.info(String.format("New shop created by: %s with Shop ID: %d" ,userName, shopId));

        }
        catch (Exception e)
        {
            response.setErrorMessage(String.format("Failed to create shopID %d by user %s. Error: ", shopId, userName, e.getMessage()));
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return response;        
    }

    /**
    * Adds a product to the specified shop.
    * 
    * @param shopId    The ID of the shop to which the product will be added.
    * @param userName  The name of the user adding the product.
    * @param product   The product to be added to the shop.
    * @return          A response indicating the success or failure of the operation.
    */
    public Response addProductToShop(Integer shopId, String userName, Product product)
    {
        Response response = new Response();
        try
        {
            // TODO: verify if register and logged in and verify permissions in Shop
            _shopController.addProductToShop(shopId, product, userName);
            logger.info(String.format("New product %s :: %d added by: %s to Shop ID: %d" ,product.getProductName(), product.getProductId(), userName, shopId));

        }
        catch (Exception e)
        {
            response.setErrorMessage(String.format("Failed to add product %s :: %d to shopID %d by user %s. Error: ", product.getProductName(), 
            product.getProductId(), shopId, userName, e.getMessage()));
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        
        return response;        
    }

    //function to get a purchase from ShopController by shop ID
    public Response getPurchaseHistory(Integer shopId)
    {
        Response response = new Response();
        try
        {
            List<ShopOrder> purchasHistory = _shopController.getPurchaseHistory(shopId);
            response.setReturnValue(purchasHistory);
            logger.info(String.format("Purchase history retrieved for Shop ID: %d" ,shopId));

        }
        catch (Exception e)
        {
            response.setErrorMessage(String.format("Failed to retrieve purchase history for shopID %d. Error: ", shopId, e.getMessage()));
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        
        return response;        
    }

    //function to check if shop exists
    public Response isShopIdExist(Integer shopId)
    {
        Response response = new Response();
        try
        {
            Boolean isExist = _shopController.isShopIdExist(shopId);
            response.setReturnValue(isExist);
            logger.info(String.format("Shop ID: %d exists: %b" ,shopId, isExist));

        }
        catch (Exception e)
        {
            response.setErrorMessage(String.format("Failed to check if shopID %d exists. Error: ", shopId, e.getMessage()));
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        
        return response;        
    }

    public Response isShopOwner(Integer shopId, String userId) {
        Response response = new Response();
        try
        {
            Boolean isOwner = _shopController.isShopOwner(shopId, userId);
            response.setReturnValue(isOwner);
            logger.info(String.format("User %s is owner of Shop ID: %d: %b" ,userId, shopId, isOwner));

        }
        catch (Exception e)
        {
            response.setErrorMessage(String.format("Failed to check if user %s is owner of shopID %d. Error: ", userId, shopId, e.getMessage()));
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        
        return response;
    }
            
}
