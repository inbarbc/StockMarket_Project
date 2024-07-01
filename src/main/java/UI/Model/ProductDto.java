package UI.Model;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto {

    @NotBlank(message = "Product name is required")
    private String productName;
    
    @NotBlank(message = "Category is required")
    private Category category;

    @NotBlank(message = "Price is required")
    private double price;

    @NotBlank(message = "Product quantity is required")
    private int productQuantity;

    // Constructor
    public ProductDto(){}

    public ProductDto(String productName, Category category, double price, int productQuantity) {
        this.productName = productName;
        this.category = category;
        this.price = price;
        this.productQuantity = productQuantity;
    }

    // Getters and setters
    public String getProductName() {
        return productName;
    }

    public void setProductName(String newProductName) {
        this.productName = newProductName;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category newCategory) {
        this.category = newCategory;
    }

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double newPrice) {
        this.price = newPrice;
    }

    // public Integer getProductQuantity() { // Update getter name
    //     return this.productQuantity;
    // }

    // public void setProductQuantity(int newQuantity) { // Update setter name
    //     this.productQuantity = newQuantity;
    // }
}
