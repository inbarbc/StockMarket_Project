package UI.View;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import UI.Presenter.SystemAdminPresenter;

@Route("admin")
public class SystemAdminPageView extends BaseView {

    private SystemAdminPresenter presenter;

    public SystemAdminPageView() {
        presenter = new SystemAdminPresenter(this);

        // Create the image buttons
        Button closeShopButton = createImageButton("https://github.com/inbarbc/StockMarket_Project/blob/main/closeShop.png?raw=true", "Close Shop", this::onCloseShopClick);
        Button unsubscribeUserButton = createImageButton("https://github.com/inbarbc/StockMarket_Project/blob/main/UnsubscribeUser.png?raw=true", "Unsubscribe User", this::onUnsubscribeUserClick);
        Button respondingComplaintsButton = createImageButton("https://github.com/inbarbc/StockMarket_Project/blob/main/RespondingComplaints.png?raw=true", "Responding to Complaints", this::onRespondingComplaintsClick);
        Button sendingMessagesButton = createImageButton("https://github.com/inbarbc/StockMarket_Project/blob/main/SendingMessages.png?raw=true", "Sending Messages", this::onSendingMessagesClick);
        Button purchaseHistoryButton = createImageButton("https://github.com/inbarbc/StockMarket_Project/blob/main/PurchaseHistory.png?raw=true", "Purchase History", this::onPurchaseHistoryClick);
        Button systemInformationButton = createImageButton("https://github.com/inbarbc/StockMarket_Project/blob/main/SystemInformation.png?raw=true", "System Information", this::onSystemInformationClick);
        
        // Create layouts for buttons
        HorizontalLayout firstRow = new HorizontalLayout(closeShopButton, unsubscribeUserButton, respondingComplaintsButton);
        firstRow.setSpacing(true); // Add spacing between buttons
        firstRow.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER); // Center buttons in the row
        firstRow.getStyle().set("margin-bottom", "150px"); 

        HorizontalLayout secondRow = new HorizontalLayout(sendingMessagesButton, purchaseHistoryButton, systemInformationButton);
        secondRow.setSpacing(true); // Add spacing between buttons
        secondRow.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER); // Center buttons in the row
        secondRow.getStyle().set("margin-bottom", "150px");

        VerticalLayout layout = new VerticalLayout(firstRow, secondRow);
        layout.setSpacing(true); // Add spacing between rows
        layout.setPadding(true); // Add padding around the layout
        layout.setDefaultHorizontalComponentAlignment(VerticalLayout.Alignment.CENTER); // Center rows in the layout
        layout.setSizeFull(); // Make the layout take the full size of the container
        layout.setJustifyContentMode(VerticalLayout.JustifyContentMode.CENTER); // Center the layout vertically

        add(layout);
    }

    private Button createImageButton(String imagePath, String tooltip, Runnable clickListener) {
        // Create an image component
        Image image = new Image(imagePath, tooltip);
        image.setWidth("200px");
        image.setHeight("200px");
    
        // Create a button and set the image as its icon
        Button button = new Button();
        button.setIcon(image);
        button.addClickListener(event -> clickListener.run());
        button.addClassName("pointer-cursor");
        button.getElement().setAttribute("title", tooltip); // Set tooltip

        button.getStyle().set("margin-right", "150px");

        return button;
    }    

    private void onCloseShopClick() {
         // Create and configure the dialog
        Dialog dialog = new Dialog();
        
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        H4 headline = new H4("Choose the shop ID you want to close");
        dialog.add(headline);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER); 
        dialogLayout.setAlignItems(Alignment.CENTER); 


        // Create components for the dialog
        TextField shopIdField = new TextField("Enter Shop ID");
        Button submitButton = new Button("Submit", event -> {
            String shopId = shopIdField.getValue();
            presenter.closeShop(shopId); // Call presenter method with shop ID
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", event -> dialog.close());

        // Add components to the dialog layout
        dialogLayout.add(shopIdField, new HorizontalLayout(submitButton, cancelButton));

        dialog.add(dialogLayout);
        dialog.open();

    }

    private void onUnsubscribeUserClick() {
        Notification.show("Unsubscribe User clicked");
        // Handle the unsubscribe user action
    }

    private void onRespondingComplaintsClick() {
        Notification.show("Responding to Complaints clicked");
        // Handle the responding to complaints action
    }

    private void onSendingMessagesClick() {
        Notification.show("Sending Messages clicked");
        // Handle the sending messages action
    }

    private void onPurchaseHistoryClick() {
        Notification.show("Purchase History clicked");
        // Handle the purchase history action
    }

    private void onSystemInformationClick() {
        Notification.show("System Information clicked");
        // Handle the system information action
    }
}