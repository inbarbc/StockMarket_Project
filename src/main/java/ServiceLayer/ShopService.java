package ServiceLayer;

import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import java.util.logging.Level;

import Domain.Product;
import Domain.ShopController;

@Service
public class ShopService {
    private ShopController _shopController;
    private static final Logger logger = Logger.getLogger(ShopController.class.getName());

    public ShopService(){
        _shopController = ShopController.getShopController();
    }

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
}
