package server;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Locator.FilterOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class MessagesHistoryPage {
    private Page page;
    private Locator header;
    private Locator message;

    public MessagesHistoryPage(Page page) {
        this.page = page;
        header = page.getByRole(AriaRole.HEADING);
        message = page.getByRole(AriaRole.LISTITEM);
    }

    public Locator pageHeader() {
        return header;
    }

    public Locator messageByPosition(int messageNumber) {
        return message.nth(messageNumber);
    }

    public Locator messageByText(String messageText) {
        return message.filter(new FilterOptions().setHasText(messageText));
    }

}
