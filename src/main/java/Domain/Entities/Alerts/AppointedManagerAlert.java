package Domain.Entities.Alerts;

import java.util.Set;

public class AppointedManagerAlert implements Alert {

    private String fromUser;
    private String targetUser;
    private Set<String> permissions;
    private int shopId;
    private String message;

    public AppointedManagerAlert(String appointingManager, String username, Set<String> permissions, int shopId) {
        this.fromUser = appointingManager;
        this.targetUser = username;
        this.permissions = permissions;
        this.shopId = shopId;
        this.message = "";
        setMessage();
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public void setMessage() {

        StringBuilder permissionsText = new StringBuilder();
        for (String permission : this.permissions) {
            if (permissionsText.length() > 0) {
                permissionsText.append(", ");
            }
            permissionsText.append(permission.toLowerCase().replace("_", " "));
        }

        this.message = "New Notification: " + targetUser + "\nUser : " + this.fromUser
                + " has appointed you as a manager in shop " + this.shopId + " with the following permissions: "
                + permissionsText.toString();
    }

    @Override
    public String getFromUser() {
        return this.fromUser;
    }

    @Override
    public boolean isEmpty() {
        return this.message.isEmpty();
    }

    @Override
    public int getShopId() {
        return this.shopId;
    }

    @Override
    public String getTargetUser() {
        return this.targetUser;
    }

}
