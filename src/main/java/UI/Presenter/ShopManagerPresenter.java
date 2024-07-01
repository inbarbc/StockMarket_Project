package UI.Presenter;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vaadin.flow.component.UI;

import UI.Model.Permission;
import UI.Model.ProductDto;
import UI.Model.Response;
import UI.Model.ShopDiscountDto;
import UI.Model.ShopManagerDto;
import UI.View.ShopManagerView;
import UI.Model.Category;

@SuppressWarnings({"rawtypes" , "deprecation"})
public class ShopManagerPresenter {
    private final ShopManagerView view;

    public ShopManagerPresenter(ShopManagerView view) {
        this.view = view;
    }

    public void fetchManagerPermissions(String username){
        // Fetch the permissions of the manager
        RestTemplate restTemplate = new RestTemplate();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);

                        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<String> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/getShopManagerPermissions?shopId="+view.getShopId(),
                                HttpMethod.GET,
                                requestEntity,
                                String.class);

                        ObjectMapper objectMapper = new ObjectMapper();

                        try{
                            JsonNode responseJson = objectMapper.readTree(response.getBody());
                            if (response.getStatusCode().is2xxSuccessful()) {
                                view.showSuccessMessage("User permissions loaded successfully");
                                if (responseJson.get("errorMessage").isNull()) {
                                    List<String> permissions = objectMapper.convertValue(responseJson.get("returnValue"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                                    view.createPermissionButtons(permissions);
                                }else {
                                    view.showErrorMessage("User permissions loading failed");
                                    view.getUI().ifPresent(ui -> ui.navigate("user"));
                                }
                            }
                            else {
                                view.showErrorMessage("User permissions loading failed with status code: " + response.getStatusCodeValue());
                            }
                        }catch (HttpClientErrorException e) {
                            ResponseHandler.handleResponse(e.getStatusCode());
                        }catch (Exception e) {
                            view.showErrorMessage("Failed to parse response");
                            e.printStackTrace();
                            view.getUI().ifPresent(ui -> ui.navigate("user"));
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }

    public void viewProducts() {
        
    }

    public void addDiscounts() {

    }

    public void changeProductPolicy() {
        
    }

    public void appointManager(String newManagerUsername, Set<Permission> selectedPermissions) {
        RestTemplate restTemplate = new RestTemplate();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);
                        headers.setContentType(MediaType.APPLICATION_JSON); // Set content type

                        // Convert permissions to a set of strings
                        Set<String> permissionsList = selectedPermissions.stream()
                                .map(Enum::name)
                                .collect(Collectors.toSet());

                        // Create request body with permissions
                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("shopId", view.getShopId());
                        requestBody.put("newManagerUsername", newManagerUsername);
                        requestBody.put("permissions", permissionsList);

                        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

                        try {
                            ResponseEntity<String> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/addShopManager",
                                HttpMethod.POST,
                                requestEntity,
                                String.class
                            );

                            if (response.getStatusCode().is2xxSuccessful()) {
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode responseJson = objectMapper.readTree(response.getBody());

                                if (responseJson.get("errorMessage").isNull()) {
                                    view.showSuccessMessage("Manager appointed successfully");
                                } else {
                                    view.showErrorMessage("Failed to appoint manager: " + responseJson.get("errorMessage").asText());
                                }
                            } else {
                                view.showErrorMessage("Failed to appoint manager with status code: " + response.getStatusCodeValue());
                            }
                        } catch (HttpClientErrorException e) {
                            view.showErrorMessage("HTTP error: " + e.getStatusCode());
                        } catch (Exception e) {
                            int startIndex = e.getMessage().indexOf("\"errorMessage\":\"") + 16;
                            int endIndex = e.getMessage().indexOf("\",", startIndex);
                            view.showErrorMessage("Failed to appoint manager: " + e.getMessage().substring(startIndex, endIndex));
                            e.printStackTrace();
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }

    public void fetchShopManagers(Consumer<List<ShopManagerDto>> callback){
        RestTemplate restTemplate = new RestTemplate();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);

                        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/getShopManagers?shopId="+view.getShopId(),
                                HttpMethod.GET,
                                requestEntity,
                                Response.class);

                        try{
                            if (response.getStatusCode().is2xxSuccessful()) {
                                Response responseBody = response.getBody();
                                view.showSuccessMessage("Managers loaded successfully");
                                if (responseBody.getErrorMessage() == null) {
                                    ObjectMapper objectMapper = new ObjectMapper();
                                    List<ShopManagerDto> managers = objectMapper.convertValue(
                                        responseBody.getReturnValue(),
                                        TypeFactory.defaultInstance().constructCollectionType(List.class, ShopManagerDto.class));
                                    callback.accept(managers);
                                }else {
                                    view.showErrorMessage("Managers loading failed");
                                    //view.getUI().ifPresent(ui -> ui.navigate("user"));
                                }
                            }
                            else {
                                view.showErrorMessage("Managers loading failed with status code: " + response.getStatusCodeValue());
                            }
                        }catch (HttpClientErrorException e) {
                            ResponseHandler.handleResponse(e.getStatusCode());
                        }catch (Exception e) {
                            view.showErrorMessage("Failed to parse response");
                            e.printStackTrace();
                            //view.getUI().ifPresent(ui -> ui.navigate("user"));
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }


    public void fetchMySubordinates(Consumer<List<ShopManagerDto>> callback) {
        RestTemplate restTemplate = new RestTemplate();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);

                        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

                        ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/getMySubordinates?shopId="+view.getShopId(),
                                HttpMethod.GET,
                                requestEntity,
                                Response.class);

                        try{
                            if (response.getStatusCode().is2xxSuccessful()) {
                                Response responseBody = response.getBody();
                                view.showSuccessMessage("Subordinates loaded successfully");
                                if (responseBody.getErrorMessage() == null) {
                                    ObjectMapper objectMapper = new ObjectMapper();
                                    List<ShopManagerDto> managers = objectMapper.convertValue(
                                        responseBody.getReturnValue(),
                                        TypeFactory.defaultInstance().constructCollectionType(List.class, ShopManagerDto.class));
                                    callback.accept(managers);
                                }else {
                                    view.showErrorMessage("Subordinates loading failed");
                                }
                            }
                            else {
                                view.showErrorMessage("Subordinates loading failed with status code: " + response.getStatusCodeValue());
                            }
                        }catch (HttpClientErrorException e) {
                            ResponseHandler.handleResponse(e.getStatusCode());
                        }catch (Exception e) {
                            view.showErrorMessage("Failed to parse response");
                            e.printStackTrace();
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }

    public void viewShopRoles() {

    }

    public void viewPurchases() {

    }

    public void appointOwner(String newOwnerUsername){
        RestTemplate restTemplate = new RestTemplate();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);
                        headers.setContentType(MediaType.APPLICATION_JSON); // Set content type

                        // Create request body with permissions
                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("shopId", view.getShopId());
                        requestBody.put("newOwnerUsername", newOwnerUsername);

                        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

                        try {
                            ResponseEntity<String> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/addShopOwner",
                                HttpMethod.POST,
                                requestEntity,
                                String.class
                            );

                            if (response.getStatusCode().is2xxSuccessful()) {
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode responseJson = objectMapper.readTree(response.getBody());

                                if (responseJson.get("errorMessage").isNull()) {
                                    view.showSuccessMessage("Owner appointed successfully");
                                } else {
                                    view.showErrorMessage("Failed to appoint owner: " + responseJson.get("errorMessage").asText());
                                }
                            } else {
                                view.showErrorMessage("Failed to appoint owner with status code: " + response.getStatusCodeValue());
                            }
                        } catch (HttpClientErrorException e) {
                            view.showErrorMessage("HTTP error: " + e.getStatusCode());
                        } catch (Exception e) {
                            int startIndex = e.getMessage().indexOf("\"errorMessage\":\"") + 16;
                            int endIndex = e.getMessage().indexOf("\",", startIndex);
                            view.showErrorMessage("Failed to appoint owner: " + e.getMessage().substring(startIndex, endIndex));
                            e.printStackTrace();
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }

    public void addNewProduct(String productName, Category category, double price)
    {
        RestTemplate restTemplate = new RestTemplate();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        ProductDto productDto = new ProductDto(productName, category, price, 0);
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);
                        headers.setContentType(MediaType.APPLICATION_JSON); // Set content type

                        // Create the request object
                        // dtoWrapper request = new dtoWrapper(view.getShopId(), productDto);

                        // Use a strongly typed HttpEntity
                        HttpEntity<ProductDto> requestEntity = new HttpEntity<>(productDto, headers);

                        // Map<String, Object> requestBody = new HashMap<>();
                        // requestBody.put("shopId", view.getShopId());
                        // requestBody.put("productDto", productDto);
                            
                        // HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

                        try {
                            ResponseEntity<String> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/addProductToShop?shopId=" + view.getShopId(),
                                HttpMethod.POST,
                                requestEntity,
                                String.class
                            );

                            if (response.getStatusCode().is2xxSuccessful()) {
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode responseJson = objectMapper.readTree(response.getBody());

                                if (responseJson.get("errorMessage").isNull()) {
                                    view.showSuccessMessage("Product added to shop successfully");
                                } else {
                                    view.showErrorMessage("Failed to add product to shop: " + responseJson.get("errorMessage").asText());
                                }
                            } else {
                                view.showErrorMessage("Failed to add product to shop with status code: " + response.getStatusCodeValue());
                            }
                        } catch (HttpClientErrorException e) {
                            view.showErrorMessage("HTTP error: " + e.getStatusCode());
                        } catch (Exception e) {
                            int startIndex = e.getMessage().indexOf("\"errorMessage\":\"") + 16;
                            int endIndex = e.getMessage().indexOf("\",", startIndex);
                            view.showErrorMessage(e.getMessage().substring(startIndex, endIndex));
                            e.printStackTrace();
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }

    public void addNewProduct(String productName, String category, double price)
    {

    }


    public void fetchShopDiscounts(Consumer<List<ShopDiscountDto>> callback){
        RestTemplate restTemplate = new RestTemplate();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);

                        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

                    try{
                        ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/getShopDiscounts?shopId="+view.getShopId(),
                                HttpMethod.GET,
                                requestEntity,
                                Response.class);

                        
                            if (response.getStatusCode().is2xxSuccessful()) {
                                Response responseBody = response.getBody();
                                view.showSuccessMessage("Discounts loaded successfully");
                                if (responseBody.getErrorMessage() == null) {
                                    ObjectMapper objectMapper = new ObjectMapper();
                                    List<ShopDiscountDto> discounts = objectMapper.convertValue(
                                        responseBody.getReturnValue(),
                                        TypeFactory.defaultInstance().constructCollectionType(List.class, ShopDiscountDto.class));
                                    callback.accept(discounts);
                                }else {
                                    view.showErrorMessage("Discounts loading failed");
                                }
                            }
                            else {
                                view.showErrorMessage("Discounts loading failed with status code: " + response.getStatusCodeValue());
                            }
                        } catch (HttpClientErrorException e) {
                            view.showErrorMessage("HTTP error: " + e.getStatusCode());
                        } catch (Exception e) {
                            int startIndex = e.getMessage().indexOf("\"errorMessage\":\"") + 16;
                            int endIndex = e.getMessage().indexOf("\",", startIndex);
                            view.showErrorMessage("Failed to load discounts: " + e.getMessage().substring(startIndex, endIndex));
                            e.printStackTrace();
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }

    public void addDiscount(String discounType, boolean isPercentage, Double discountValue, Date expirationDate,
        Integer productId, Category category, Consumer<Boolean> callback){
        RestTemplate restTemplate = new RestTemplate();
        ShopDiscountDto discountDto = new ShopDiscountDto(productId, isPercentage, discountValue, expirationDate, category, -1); 
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);
                        HttpEntity<ShopDiscountDto> requestEntity = new HttpEntity<>(discountDto, headers);
                    try{
                        ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/addShopDiscount?shopId="+view.getShopId(),
                                HttpMethod.POST,
                                requestEntity,
                                Response.class);

                        
                            if (response.getStatusCode().is2xxSuccessful()) {
                                Response responseBody = response.getBody();
                                view.showSuccessMessage("Discount added loaded successfully");
                                if (responseBody.getErrorMessage() == null) {
                                    callback.accept(true);
                                }else {
                                    callback.accept(false);
                                    view.showErrorMessage("Discount adding failed");
                                }
                            }
                            else {
                                view.showErrorMessage("Discounts adding failed with status code: " + response.getStatusCodeValue());
                            }
                        } catch (HttpClientErrorException e) {
                            view.showErrorMessage("HTTP error: " + e.getStatusCode());
                        } catch (Exception e) {
                            int startIndex = e.getMessage().indexOf("\"errorMessage\":\"") + 16;
                            int endIndex = e.getMessage().indexOf("\",", startIndex);
                            view.showErrorMessage("Failed to add discount: " + e.getMessage().substring(startIndex, endIndex));
                            callback.accept(false);
                            e.printStackTrace();
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }

    public void deleteDiscount(ShopDiscountDto discountDto, Consumer<Boolean> callback){
        RestTemplate restTemplate = new RestTemplate(); 
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);
                        HttpEntity<ShopDiscountDto> requestEntity = new HttpEntity<>(discountDto, headers);
                    try{
                        ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/deleteShopDiscount?shopId="+view.getShopId(),
                                HttpMethod.POST,
                                requestEntity,
                                Response.class);

                        
                            if (response.getStatusCode().is2xxSuccessful()) {
                                Response responseBody = response.getBody();
                                view.showSuccessMessage("Discount deleted successfully");
                                if (responseBody.getErrorMessage() == null) {
                                    callback.accept(true);
                                }else {
                                    callback.accept(false);
                                    view.showErrorMessage("Discount deletion failed");
                                }
                            }
                            else {
                                view.showErrorMessage("Discount deletion failed with status code: " + response.getStatusCodeValue());
                            }
                        } catch (HttpClientErrorException e) {
                            view.showErrorMessage("HTTP error: " + e.getStatusCode());
                        } catch (Exception e) {
                            int startIndex = e.getMessage().indexOf("\"errorMessage\":\"") + 16;
                            int endIndex = e.getMessage().indexOf("\",", startIndex);
                            view.showErrorMessage("Failed to delete discount: " + e.getMessage().substring(startIndex, endIndex));
                            callback.accept(false);
                            e.printStackTrace();
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                    }
                });
    }

    public void updatePermissions(String managerUserName, Set<Permission> permissions, Consumer<Boolean> callback) {
        RestTemplate restTemplate = new RestTemplate();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);
                        headers.setContentType(MediaType.APPLICATION_JSON); // Set content type

                        // Convert permissions to a set of strings
                        Set<String> permissionsList = permissions.stream()
                                .map(Enum::name)
                                .collect(Collectors.toSet());

                        // Create request body with permissions
                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("shopId", view.getShopId());
                        requestBody.put("managerUsername", managerUserName);
                        requestBody.put("permissions", permissionsList);

                        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

                        try {
                            ResponseEntity<String> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/updatePermissions",
                                HttpMethod.POST,
                                requestEntity,
                                String.class
                            );

                            if (response.getStatusCode().is2xxSuccessful()) {
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode responseJson = objectMapper.readTree(response.getBody());

                                if (responseJson.get("errorMessage").isNull()) {
                                    view.showSuccessMessage("Manager permissions changed successfully");
                                    callback.accept(true);
                                } else {
                                    view.showErrorMessage("Failed to change manager permissions: " + responseJson.get("errorMessage").asText());
                                    callback.accept(false);
                                }
                            } else {
                                view.showErrorMessage("Failed to change manager permissions with status code: " + response.getStatusCodeValue());
                                callback.accept(false);
                            }
                        } catch (HttpClientErrorException e) {
                            view.showErrorMessage("HTTP error: " + e.getStatusCode());
                            callback.accept(false);
                        } catch (Exception e) {
                            int startIndex = e.getMessage().indexOf("\"errorMessage\":\"") + 16;
                            int endIndex = e.getMessage().indexOf("\",", startIndex);
                            view.showErrorMessage("Failed to change manager permissions: " + e.getMessage().substring(startIndex, endIndex));
                            callback.accept(false);
                            e.printStackTrace();
                        }
                    } else {
                        view.showErrorMessage("Authorization token not found. Please log in.");
                        callback.accept(false);
                    }
                });
    }
    
    
   
}
