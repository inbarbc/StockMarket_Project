package Domain;

import java.util.ArrayList;
import java.util.List;

import Exceptions.ShopException;

public class ShopController {
    private static ShopController _shopController;
    private List<Shop> _shopsList;

    private ShopController() {
        this._shopsList = new ArrayList<>();
    }

    // Public method to provide access to the _shopController
    public static synchronized ShopController getShopController() {
        if (_shopController == null) {
            _shopController = new ShopController();
        }
        return _shopController;
    }

    public Shop getShopByShopId(Integer shopId){
        for (Shop shop : this._shopsList) {
            if (shop.getShopId().equals(shopId)) {
                return shop;
            }
        }
        return null;
    }

    public Boolean isShopIdExist(Integer shopId)
    {
        for (Shop shop : this._shopsList) {
            if (shop.getShopId().equals(shopId)) {
                return true;
            }
        }
        return false;
    }

    public void OpenNewShop(Integer shopId, String userName) throws Exception 
    {
        if(isShopIdExist(shopId))
            throw new Exception(String.format("Shop ID: %d is already exist.", shopId));
        else
            _shopsList.add(new Shop(shopId, userName));
    }

    public void addProductToShop(Integer shopId, Product product, String userName )
    {
        //TODO: need to implement  (check if shop exist)

    }

    //get purchase history for a shop by shop id
    public List<ShopOrder> getPurchaseHistory(Integer shopId)
    {
        List<ShopOrder> purchaseHistory = new ArrayList<>();
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            purchaseHistory = shop.getPurchaseHistory();
        }
        return purchaseHistory;
    }

    public Boolean isShopOwner(Integer shopId, String userId) throws ShopException {
        Shop shop = getShopByShopId(shopId);
        if (shop != null) {
            return shop.isOwnerOrFounderOwner(userId);
        }
        return false;
    }




}
