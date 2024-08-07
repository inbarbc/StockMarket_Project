package UI.View;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinSession;

import UI.Presenter.UserShopsPagePresenter;

@PageTitle("User Shops Page")
public class UserShopsPageView extends BaseView {

    private UserShopsPagePresenter presenter;
    
    
    private String _username;

    public UserShopsPageView(){
        // Retrieve the username from the session
        _username = (String) VaadinSession.getCurrent().getAttribute("username");

        // Initialize presenter
        presenter = new UserShopsPagePresenter(this);
        presenter.fetchShops(_username);

        H1 title = new H1("My Shops");
        add(title);
    }
    

    public void createShopButtons(List<Integer> shops, List<String> shopNames) {

        if(shops.isEmpty()){
            add(new Paragraph("No shops found"));
            return;
        }

        // Create a vertical layout for the grid
        VerticalLayout gridLayout = new VerticalLayout();
        gridLayout.setAlignItems(Alignment.START);

        // Set a maximum of 3 buttons per row
        int maxButtonsPerRow = 3;
        HorizontalLayout rowLayout = new HorizontalLayout();

        for (int i = 0; i < shops.size(); i++) {
            Integer shopId = shops.get(i);
            Button shopButton = new Button("" + shopNames.get(i), e -> navigateToManageShop(shopId));
            shopButton.addClassName("same-size-button");
            rowLayout.add(shopButton);

            if ((i + 1) % maxButtonsPerRow == 0 || i == shops.size() - 1) {
                gridLayout.add(rowLayout);
                rowLayout = new HorizontalLayout();
            }
        }

        // Add the grid layout to the main layout
        add(gridLayout);
    }

    public void navigateToManageShop(Integer shopId) {
        //UI.getCurrent().navigate("user_shops/" + shopId);
        getUI().ifPresent(ui -> ui.navigate("user_shops/" + shopId));
    }

}
