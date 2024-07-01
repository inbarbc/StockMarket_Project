package Server.Controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Dtos.BasicDiscountDto;
import Dtos.ConditionalDiscountDto;
import Dtos.ProductDto;
import Dtos.ProductSearchDto;
import Dtos.ShopDto;
import ServiceLayer.Response;
import ServiceLayer.ShopService;

@RestController
@SuppressWarnings({"rawtypes" , "unchecked"})
@RequestMapping(path = "/api/shop")
public class ShopController {
    private final ShopService _shopService;

    @Autowired
    public ShopController(ShopService shopService) {
        this._shopService = shopService;
    }

    @PostMapping("/openNewShop")
    public ResponseEntity<Response> openNewShop( @RequestBody ShopDto shopDto,
            @RequestHeader(value = "Authorization") String token) {
        // example request:
        // http://localhost:8080/api/user/register?shopName=test&bankDetails=test&shopAddress=test
        System.out.println("Client: token is: " + token);
        ResponseEntity<Response> resp =_shopService.openNewShop(token, shopDto);
        return resp;
    }

    @GetMapping("/closeShop")
    public ResponseEntity<Response> closeShop(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.closeShop(token, shopId);
    }

    @PostMapping("/reopenShop")
    public ResponseEntity<Response> reopenShop(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.reOpenShop(token, shopId);
    }

    @PostMapping("/addProductToShop")
    public ResponseEntity<Response> addProductToShop(@RequestHeader("Authorization") String token, @RequestParam Integer shopId, @RequestBody ProductDto productDto) {
        ResponseEntity<Response> resp =_shopService.addProductToShop(token, shopId, productDto);
        return resp;
    }

    @PostMapping("/searchProductsInShopByName")
    public ResponseEntity<Response> searchProductInShopByName(@RequestHeader("Authorization") String token,
    @RequestBody ProductSearchDto productSearchDto) {
        return _shopService.searchProductInShopByName(token, productSearchDto.getShopId(),  productSearchDto.getProductName());
    }

    @PostMapping("/searchProductsInShopByCategory")
    public ResponseEntity<Response> searchByCategory(@RequestHeader("Authorization") String token,
    @RequestBody ProductSearchDto productSearchDto) {
    return _shopService.searchProductInShopByCategory(token, productSearchDto.getShopId(), productSearchDto.getCategory());
    }

    @PostMapping("/searchProductsInShopByKeywords")
    public ResponseEntity<Response> searchProducstInShopByKeywords(@RequestHeader("Authorization") String token,
    @RequestBody ProductSearchDto productSearchDto) {
        return _shopService.searchProductsInShopByKeywords(token, productSearchDto.getShopId(), productSearchDto.getKeywords());
    }

    @PostMapping("/getShopIdByName")
    public ResponseEntity<Response> getShopIdByName(@RequestHeader("Authorization") String token, @RequestBody String shopName) {
        return _shopService.getShopIdByName(token, shopName);
    }

    // @GetMapping("/searchProductsInShopByPriceRange")
    // public ResponseEntity<Response> searchProductsInShopByPriceRange(@RequestHeader("Authorization") String token,
    //         @RequestParam(required = false) Integer shopId,
    //         @RequestParam Double minPrice,
    //         @RequestParam Double maxPrice) {
    //     return _shopService.searchProductsInShopByPriceRange(token, shopId, minPrice, maxPrice);
    // }

    @GetMapping("/getShopPurchaseHistory")
    public ResponseEntity<Response> getShopPurchaseHistory(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.getShopPurchaseHistory(token, shopId);
    }

    @PostMapping("/addShopBasicDiscount")
    public ResponseEntity<Response>addShopBasicDiscount(@RequestHeader("Authorization") String token, @RequestParam Integer shopId,
            @RequestBody BasicDiscountDto discountDto) {
        return _shopService.addShopBasicDiscount(token, shopId, discountDto);
    }

    @PostMapping("/addShopConditionalDiscount")
    public ResponseEntity<Response>addShopConditionalDiscount(@RequestHeader("Authorization") String token,
            @RequestParam Integer shopId,
            @RequestBody ConditionalDiscountDto discountDto) {
        return _shopService.addShopConditionalDiscount(token, shopId, discountDto);
    }

    @PostMapping("/removeShopDiscount")
    public ResponseEntity<Response>removeShopDiscount(@RequestHeader("Authorization") String token, @RequestParam Integer shopId,
            @RequestParam Integer discountId) {
        return _shopService.removeDiscount(token, shopId, discountId);
    }

    @PostMapping("/updateProductQuantity")
    public ResponseEntity<Response>updateProductQuantity(@RequestHeader("Authorization") String token, @RequestParam Integer shopId,
            @RequestParam Integer productId, @RequestParam Integer quantity) {
        return _shopService.updateProductQuantity(token, shopId, productId, quantity);
    }

    @PostMapping("/addShopOwner")
    public ResponseEntity<Response>addShopOwner(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> request) {
        Integer shopId = (Integer) request.get("shopId");
        String newOwnerUsername = (String) request.get("newOwnerUsername");
        return _shopService.addShopOwner(token, shopId, newOwnerUsername);
    }

    @PostMapping("/addShopManager")
    public ResponseEntity<Response> addShopManager(@RequestHeader("Authorization") String token,
                                                @RequestBody Map<String, Object> request) {
        Integer shopId = (Integer) request.get("shopId");
        String newManagerUsername = (String) request.get("newManagerUsername");
        Set<String> permissions = new HashSet<>((List<String>) request.get("permissions"));
        return _shopService.addShopManager(token, shopId, newManagerUsername, permissions);
    }

    @PostMapping("/fireShopManager")
    public ResponseEntity<Response>fireShopManager(@RequestHeader("Authorization") String token, @RequestParam Integer shopId,
            @RequestParam String managerUsername) {
        return _shopService.fireShopManager(token, shopId, managerUsername);
    }

    @PostMapping("/resignFromRole")
    public ResponseEntity<Response>resignFromRole(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.resignFromRole(token, shopId);
    }

    @PostMapping("/modifyManagerPermissions")
    public ResponseEntity<Response>modifyManagerPermissions(@RequestHeader("Authorization") String token, @RequestParam Integer shopId,
            @RequestParam String managerUsername, @RequestBody Set<String> permissions) {
        return _shopService.modifyManagerPermissions(token, shopId, managerUsername, permissions);
    }

    @GetMapping("/displayShopPolicyInfo")
    public ResponseEntity<Response>displayShopPolicyInfo(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.displayShopPolicyInfo(token, shopId);
    }

    @GetMapping("/displayProductPolicyInfo")
    public ResponseEntity<Response>displayProductPolicyInfo(@RequestHeader("Authorization") String token, @RequestParam Integer shopId,
            @RequestParam Integer productId) {
        return _shopService.displayProductPolicyInfo(token, shopId, productId);
    }

    @GetMapping("/displayShopDiscountsInfo")
    public ResponseEntity<Response>displayShopDiscountsInfo(@RequestHeader("Authorization") String token,
            @RequestParam Integer shopId) {
        return _shopService.displayShopDiscountsInfo(token, shopId);
    }

    @GetMapping("/displayProductDiscountsInfo")
    public ResponseEntity<Response>displayProductDiscountsInfo(@RequestHeader("Authorization") String token,
            @RequestParam Integer shopId, @RequestParam Integer productId) {
        return _shopService.displayProductDiscountsInfo(token, shopId, productId);
    }

    @GetMapping("/displayShopGeneralInfo")
    public ResponseEntity<Response>displayShopGeneralInfo(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.displayShopGeneralInfo(token, shopId);
    }

    @GetMapping("/displayProductGeneralInfo")
    public ResponseEntity<Response>displayProductGeneralInfo(@RequestHeader("Authorization") String token,
            @RequestParam Integer shopId, @RequestParam Integer productId) {
        return _shopService.displayProductGeneralInfo(token, shopId, productId);
    }

    @GetMapping("/getUserShops")
    public ResponseEntity<Response>getUserShops(@RequestHeader("Authorization") String token) {
        return _shopService.getUserShopsIds(token);
    }

    @GetMapping("/getShopsEntities")
    public ResponseEntity<Response>getShopsEntities(@RequestHeader("Authorization") String token) {
        return _shopService.getShopsEntities(token);
    }
  
    @GetMapping("/getShopManagerPermissions")
    public ResponseEntity<Response> getShopManagerPermissions(@RequestHeader("Authorization") String token,
    @RequestParam Integer shopId) {
        return _shopService.getShopManagerPermissions(token, shopId);
    }

    @GetMapping("/getUserShopsNames")
    public ResponseEntity<Response>getUserShopsNames(@RequestHeader("Authorization") String token) {
        ResponseEntity<Response> resp = _shopService.getUserShopsNames(token);
        return resp;
    }

    @GetMapping("/searchAndDisplayShopByID")
    public ResponseEntity<Response> searchAndDisplayShopByID(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.searchAndDisplayShopByID(token, shopId);
    }

    @GetMapping("/searchAndDisplayShopByName")
    public ResponseEntity<Response> searchAndDisplayShopByName(@RequestHeader("Authorization") String token, @RequestParam String shopName) {
        return _shopService.searchAndDisplayShopByName(token, shopName);
    }

    @GetMapping(value = "/getAllShops", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response>getAllShops(@RequestHeader("Authorization") String token) {
        return _shopService.getAllShops(token);
    }

    @GetMapping("/getShopManagers")
    public ResponseEntity<Response>getShopManagers(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.getShopManagers(token, shopId);
    }

    @GetMapping(value = "/getAllProductInShop", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response>getAllProductInShop(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.getAllProductInShop(token, shopId);
    }
    

    @GetMapping("/getMySubordinates")
    public ResponseEntity<Response>getMySubordinates(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.getMySubordinates(token, shopId);
    }

    @GetMapping("/getShopDiscounts")
    public ResponseEntity<Response>getShopDiscounts(@RequestHeader("Authorization") String token, @RequestParam Integer shopId) {
        return _shopService.getShopDiscounts(token, shopId);
    }

    @PostMapping("/addShopDiscount")
    public ResponseEntity<Response> addShopDiscount(
            @RequestBody BasicDiscountDto discountDto,
            @RequestHeader(value = "Authorization") String token,
            @RequestParam Integer shopId) {
        return _shopService.addShopDiscount(token, discountDto, shopId);
    }

    @PostMapping("/deleteShopDiscount")
    public ResponseEntity<Response> deleteShopDiscount(
            @RequestBody BasicDiscountDto discountDto,
            @RequestHeader(value = "Authorization") String token,
            @RequestParam Integer shopId) {
        return _shopService.deleteShopDiscount(token, discountDto, shopId);
    }

    @PostMapping("/updatePermissions")
    public ResponseEntity<Response> updatePermissions(@RequestHeader("Authorization") String token,
                                                @RequestBody Map<String, Object> request) {
        Integer shopId = (Integer) request.get("shopId");
        String managerUsername = (String) request.get("managerUsername");
        Set<String> permissions = new HashSet<>((List<String>) request.get("permissions"));
        return _shopService.updatePermissions(token, shopId, managerUsername, permissions);
    }
    
    

}
