package UI.Model;

import javax.validation.constraints.NotBlank;

public class ShopDto {
    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Bank details is required")
    private String bankDetails;

    @NotBlank(message = "Shop address is required")
    private String shopAddress;
    
    private Double shopRating;

    private Integer shopRatersCounter;

    private boolean isShopClosed;

    private Integer shopId;

    // Constructors, getters, and setters

    public ShopDto() {
    }

    public ShopDto(String shopName, String bankDetails, String shopAddress) {
        this.shopName = shopName;
        this.bankDetails = bankDetails;
        this.shopAddress = shopAddress;
        this.shopRating = -1.0;
        this.shopRatersCounter = 0;
        this.isShopClosed = false;
    }

    public ShopDto(String shopName, String bankDetails, String shopAddress, Double shopRating) {
        this.shopName = shopName;
        this.bankDetails = bankDetails;
        this.shopAddress = shopAddress;
        this.shopRating = shopRating;
        this.shopRatersCounter = 0;
        this.isShopClosed = false;
    }

    public ShopDto(Integer shopId, String shopName ,String bankDetails, String shopAddress, Double shopRating, Integer shopRatersCounter, boolean isShopClosed) {
        this.shopId = shopId;
        this.shopName = shopName;
        this.bankDetails = bankDetails;
        this.shopAddress = shopAddress;
        this.shopRating = shopRating;
        this.shopRatersCounter = shopRatersCounter;
        this.isShopClosed = isShopClosed;
    }

    // Getters and setters

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getBankDetails() {
        return bankDetails;
    }

    public void setBankDetails(String bankDetails) {
        this.bankDetails = bankDetails;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress;
    }

    public Double getShopRating() { return shopRating;}

    public void setShopRating(Double shopRating){
        this.shopRating = shopRating;
    }

    public Integer getShopRatersCounter() { return shopRatersCounter;}

    public void setShopRatersCounter(Integer shopRatersCounter){
        this.shopRatersCounter = shopRatersCounter;
    }

    public Integer getShopId() { return shopId;}

    public boolean getIsShopClosed() { return isShopClosed;}
}

