package Domain;

import java.util.List;

import Domain.Exceptions.PaymentFailedException;
import Domain.Exceptions.ShippingFailedException;
import Domain.ExternalServices.PaymentService.PaymentMethod;
import Domain.ExternalServices.SupplyService.SupplyMethod;

public class User {
    private String _encoded_password;
    private String _username;
    private boolean _loggedIn;
    private String _email;
    private ShoppingCart _shoppingCart;

    public User(String username, String encoded_password, String email) {
        _username = username;
        _encoded_password = encoded_password;
        _loggedIn = false;
        _email = email;
        _shoppingCart = new ShoppingCart();
    }

    public boolean isCurrUser(String username, String encoded_password) {
        if(_username == username & _encoded_password == encoded_password){
            return true;
        }
        return false;
    }

    public String getUserName(){
        return _username;
    }

    public String getEncodedPassword(){
        return _encoded_password;
    }

    public void logIn(){
        _loggedIn = true;
    }

    public void logOut(){
        _loggedIn = false;
    }

    public String getEmail() {
        return _email;
    }

    public void setEmail(String email) {
        _email = email;
    }

    public void purchaseCart(List<Integer> busketsToBuy, PaymentMethod paymentMethod, SupplyMethod shippingMethod) {
        _shoppingCart.purchaseCart(this, busketsToBuy);
        try {
            paymentMethod.pay();
            shippingMethod.deliver();
        } catch (PaymentFailedException e) {
            _shoppingCart.cancelPurchase(this, busketsToBuy);
            throw new PaymentFailedException("Payment failed");
        } catch (ShippingFailedException e) {
            _shoppingCart.cancelPurchase(this, busketsToBuy);
            throw new ShippingFailedException("Shipping failed");
        }
        
    }

}
