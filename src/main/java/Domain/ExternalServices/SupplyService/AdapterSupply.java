package Domain.ExternalServices.SupplyService;

import java.util.logging.Logger;

import Domain.ExternalServices.ExternalService;
import Exceptions.ShippingFailedException;

public class AdapterSupply extends ExternalService {

    // private fields
    private static AdapterSupply _AdapterSupply;
    private ProxySupply _supplyService;
    private static final Logger logger = Logger.getLogger(AdapterSupply.class.getName());

    private AdapterSupply(int id, String newSerivceName, String informationPersonName, String informationPersonPhone) {
        super(id, newSerivceName, informationPersonName, informationPersonPhone);
        _AdapterSupply = this;
        _supplyService = new ProxySupply();
    }

    public static AdapterSupply getAdapterSupply() {
        if(_AdapterSupply == null)
        _AdapterSupply = new AdapterSupply(-1, "SupplyService", "Tal", "123456789");
        return _AdapterSupply;
    }

    @Override
    public boolean ConnectToService() {
        // Connect to the supply service
        // TODO : forward the request to the Supply service
        logger.info("Connecting to the Supply service");

        return true;
    }

    public void checkIfDeliverOk(String address) throws ShippingFailedException {
        logger.info("Checking if the delivery is valid");
        if (!_supplyService.checkIfDeliverOk(address))
            throw new ShippingFailedException("Shipping failed");
    }

    public void deliver(String clientAddress, String shopAddress) throws ShippingFailedException{
        logger.info("Delivering the cart");
        if (!_supplyService.deliver(clientAddress, shopAddress)) 
            throw new ShippingFailedException("Shipping failed");
    }
}
