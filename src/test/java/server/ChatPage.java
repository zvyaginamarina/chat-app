package server;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Locator.FilterOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.GetByRoleOptions;
import com.microsoft.playwright.options.AriaRole;

public class ChatPage {
    private Page page;
    private Locator nameField;
    private Locator roomsSelect;
    private Locator connectButton;
    private Locator messageHistoryButton;
    private Locator messageField;
    private Locator sendMessageButton;
    private Locator chatWindow;
    private Locator chatMessage;

    public ChatPage(Page page) {
        this.page = page;
        nameField = page.getByPlaceholder("Ваше имя");
        roomsSelect = page.getByRole(AriaRole.COMBOBOX);
        connectButton = page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Подключиться"));
        messageHistoryButton = page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("История сообщений"));
        messageField = page.getByPlaceholder("Сообщение");
        sendMessageButton = page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("Отправить"));
        chatWindow = page.locator("#messages");
        chatMessage = page.locator(".message");
    }

    public void inputName(String name) {
        nameField.fill(name);
    }

    public void selectRoom(String roomType) {
        roomsSelect.selectOption(roomType);
    }

    public void connectToChat() {
        connectButton.click();
    }

    public MessagesHistoryPage viewMessageHistory() {
        Page messageHistoryPage = page.waitForPopup(() -> {
            messageHistoryButton.click();
        });
        return new MessagesHistoryPage(messageHistoryPage);
    }

    public Locator messageField() {
        return messageField;
    }

    public void inputMessage(String message) {
        messageField.fill(message);
    }

    public Locator sendMessageButton() {
        return sendMessageButton;
    }

    public void sendMessage() {
        sendMessageButton.click();
    }

    public Locator chatWindow() {
        return chatWindow;
    }

    public Locator chatMessage(String message) {
        return chatMessage.filter(new FilterOptions().setHasText(message));
    }

    public String getChatMessage(String message) {
        return chatMessage(message).textContent();
    }

    public void scrollChatWindow() {
        chatWindow.scrollIntoViewIfNeeded();
    }

}
