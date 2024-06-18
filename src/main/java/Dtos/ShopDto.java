package Dtos;

import Domain.Shop;

public class ShopDto {
    public String shopName;
    public String bankDetails;
    public String shopAddress;

    public ShopDto(String shopName, String bankDetails, String shopAddress) {
        this.shopName = shopName;
        this.bankDetails = bankDetails;
        this.shopAddress = shopAddress;
    }

    public ShopDto (Shop shop) {
        this.shopName = shop.getShopName();
        this.bankDetails = shop.getBankDetails();
        this.shopAddress = shop.getShopAddress();
    }
}
