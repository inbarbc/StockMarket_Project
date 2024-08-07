package Domain.Entities.Alerts;

import java.util.List;

public class CredentialRemovedAlert implements Alert {

    private String message;
    private String fromUser;
    private int shopId;
    private List<String> permissionsList;

    // constructor
    public CredentialRemovedAlert(String fromUser, List<String> permissionsList, int shopId) {
        this.fromUser = fromUser;
        this.permissionsList = permissionsList;
        this.shopId = shopId;
        setMessage();
    }

    public String getFromUser() {
        return fromUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage() {
        this.message = "New Notification: User " + fromUser + " removed you from the next credentials: " + permissionsList;
    }

    public boolean isEmpty() {
        return message.isEmpty();
    }

    public int getShopId() {
        return shopId;
    }

    public String getTargetUser() {
        return fromUser;
    }

}
