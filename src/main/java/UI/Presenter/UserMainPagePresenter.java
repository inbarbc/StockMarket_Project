package UI.Presenter;
import java.time.ZoneId;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;

// import Dtos.UserDto;
import UI.Model.UserDto;
import UI.Model.Response;
import UI.Model.ShopDto;
import UI.View.UserMainPageView;

public class UserMainPagePresenter {

    private final UserMainPageView view;
    private static final String SERVER_PORT = "8080";


    public UserMainPagePresenter(UserMainPageView view) {
        this.view = view;
    }

    public void openNewShop(String shopName, String bankDetails, String shopAddress){
        RestTemplate restTemplate = new RestTemplate();
        ShopDto shopDto = new ShopDto(shopName, bankDetails, shopAddress);

        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        System.out.println("Token: " + token);

                        HttpHeaders headers = new HttpHeaders();
                        headers.add("Authorization", token);

                        HttpEntity<ShopDto> requestEntity = new HttpEntity<>(shopDto, headers);

                        ResponseEntity<String> response = restTemplate.exchange(
                                "http://localhost:" + view.getServerPort() + "/api/shop/openNewShop",
                                HttpMethod.POST,
                                requestEntity,
                                String.class);

                        if (response.getStatusCode().is2xxSuccessful()) {
                            view.showSuccessMessage("Shop opened successfully");
                            System.out.println(response.getBody());
                        } else {
                            view.showErrorMessage("Failed to open shop");
                        }
                    } else {
                        System.out.println("Token not found in local storage.");
                        view.showErrorMessage("Failed to open shop");
                    }
                });
    }


@SuppressWarnings("rawtypes")
public void getUserInfo() {
    RestTemplate restTemplate = new RestTemplate();

    UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
            .then(String.class, token -> {
                if (token != null && !token.isEmpty()) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Authorization", token);
                    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

                    try {
                        ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + SERVER_PORT + "/api/user/getUserDetails",
                                HttpMethod.GET,
                                requestEntity,
                                Response.class);

                        if (response.getStatusCode().is2xxSuccessful()) {
                            Response responseBody = response.getBody();

                            if (responseBody.getErrorMessage() == null) {
                                ObjectMapper objectMapper = new ObjectMapper();
                                UserDto userDto = objectMapper.convertValue(responseBody.getReturnValue(), UserDto.class);
                                view.usernameField.setValue(userDto.getUsername());
                                view.emailField.setValue(userDto.getEmail());
                                view.birthDateField.setValue(userDto.getBirthDate().toInstant()
                                      .atZone(ZoneId.systemDefault())
                                      .toLocalDate());
                                view.showSuccessMessage("Fetch user details succeed");
                            } else {
                                view.showErrorMessage("Fetch user details failed: " + responseBody.getErrorMessage());
                            }
                        } else {
                            view.showErrorMessage("Fetch user details failed: " + response.getStatusCode());
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

            
    @SuppressWarnings("rawtypes")
    public void updateUserInfo(UserDto userDto) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        UI.getCurrent().getPage().executeJs("return localStorage.getItem('authToken');")
                .then(String.class, token -> {
                    if (token != null && !token.isEmpty()) {
                        headers.set("Authorization", token);
                        
                        HttpEntity<UserDto> requestEntity = new HttpEntity<>(userDto, headers);
    
                        try {
                            ResponseEntity<Response> response = restTemplate.exchange(
                                "http://localhost:" + SERVER_PORT + "/api/user/setUserDetails",
                                HttpMethod.POST,
                                requestEntity,
                                Response.class);
    
                            Response responseBody = response.getBody();
                            if (response.getStatusCode().is2xxSuccessful() && responseBody.getErrorMessage() == null) {
                                view.showSuccessMessage("Set details succeed");
                            } else {
                                view.showErrorMessage("Set details failed: " + responseBody.getErrorMessage());
                            }
                        } catch (HttpClientErrorException e) {
                            ResponseHandler.handleResponse(e.getStatusCode());
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
}    
