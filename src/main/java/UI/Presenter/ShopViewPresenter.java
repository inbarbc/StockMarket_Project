package UI.Presenter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vaadin.flow.component.UI;

import UI.Model.ProductDto;
import UI.Model.Response;
import UI.View.ShopView;

@SuppressWarnings({ "rawtypes" })
public class ShopViewPresenter {

    ShopView _view;

    public ShopViewPresenter(ShopView view) {
        this._view = view;
    }

    public void getShopProducts() {
        RestTemplate restTemplate = new RestTemplate();

        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        System.out.println("Token: " + token);

                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);

                        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
                        Integer shopId = (Integer) UI.getCurrent().getSession().getAttribute("shopId");
                        ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + _view.getServerPort() + "/api/shop/getAllProductInShop?shopId="
                                        + shopId,
                                HttpMethod.GET,
                                requestEntity,
                                Response.class);

                        if (response.getStatusCode().is2xxSuccessful()) {
                            Response responseBody = response.getBody();

                            if (responseBody.getErrorMessage() == null) {
                                ObjectMapper objectMapper = new ObjectMapper();
                                List<ProductDto> productDtoList = objectMapper.convertValue(
                                        responseBody.getReturnValue(),
                                        TypeFactory.defaultInstance().constructCollectionType(List.class,
                                                ProductDto.class));
                                _view.displayAllProducts(productDtoList);
                                _view.showSuccessMessage("products present successfully");
                            } else {
                                _view.showErrorMessage("Failed to parse JSON response");
                            }
                        } else {
                            _view.showErrorMessage("Failed to present products");
                        }
                    } else {
                        System.out.println("Token not found in local storage.");
                        _view.showErrorMessage("Failed to present products");
                    }
                });

    }


    public void openComplain(String message) {
        RestTemplate restTemplate = new RestTemplate();

        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        System.out.println("Token: " + token);

                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);

                        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
                        Integer shopId = (Integer) UI.getCurrent().getSession().getAttribute("shopId");
                        try {
                            String url = "http://localhost:" + _view.getServerPort() + "/api/shop/openComplaint?shopId=" + shopId + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8.toString());
                            ResponseEntity<Response> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                requestEntity,
                                Response.class);

                        if (response.getStatusCode().is2xxSuccessful()) {
                            Response responseBody = response.getBody();

                            if (responseBody.getErrorMessage() == null) {
                                _view.showSuccessMessage("complaint open successfully");
                            } else {
                                _view.showErrorMessage("Failed to parse JSON response");
                            }
                        } else {
                            _view.showErrorMessage("Failed to open complaint");
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    } else {
                        System.out.println("Token not found in local storage.");
                        _view.showErrorMessage("Failed to open complaint");
                    }
                });

    }

    public void addProductToCart(int shopId, int productId, int quantity) {
        RestTemplate restTemplate = new RestTemplate();

        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        System.out.println("Token: " + token);

                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);

                        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

                        try{
                            ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + _view.getServerPort() + "/api/user/addProductToShoppingCart?productID=" + productId +
                                 "&shopID=" + shopId + "&quantity=" + quantity,
                                HttpMethod.POST,
                                requestEntity,
                                Response.class);

                            if (response.getStatusCode().is2xxSuccessful()) {
                                Response responseBody = response.getBody();

                                if (responseBody.getErrorMessage() == null) {
                                    _view.showSuccessMessage("Product added to cart successfully");
                                }
                                else {
                                    _view.showErrorMessage("Failed to parse JSON response");
                                }                       
                            } else {
                                _view.showErrorMessage("Failed to add product to cart");
                            }
                        }
                        catch (HttpClientErrorException e) {
                            _view.showErrorMessage("HTTP error: " + e.getStatusCode());
                        }
                        catch (Exception e) {
                            _view.showErrorMessage("Failed to add product to cart: " + ErrorMessageGenerator.generateGenericErrorMessage(e.getMessage()));
                            e.printStackTrace();
                        }
                        
                    } else {
                        System.out.println("Token not found in local storage.");
                        _view.showErrorMessage("Failed to add product to cart");
                    }
                });
    }


}