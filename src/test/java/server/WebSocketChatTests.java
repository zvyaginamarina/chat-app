package server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.WebSocket;
import com.microsoft.playwright.assertions.LocatorAssertions;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WebSocketChatTests {
    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;
    private BrowserContext context2;
    private Page page2;

    private static ServerManager server;
    private ChatPage chat;
    private ChatPage chat2;
    private MessagesHistoryPage messagesHistoryWindow;

    @BeforeAll
    static void setupBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new LaunchOptions().setHeadless(false));
        server = new ServerManager();
        server.start();
    }

    @BeforeEach
    void setupContext() {
        context = browser.newContext();
        context.grantPermissions(List.of("notifications"));
        page = context.newPage();

        page.navigate(server.pageUrl());
        chat = new ChatPage(page);
    }

    @AfterEach
    void teardownContext() {
        if (context != null) {
            context.close();
        }
        if (context2 != null) {
            context2.close();
        }
    }

    @AfterAll
    static void tearDownBrowser() {
        try {
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception e) {
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    public void setupForSecondUser() {
        context2 = browser.newContext();
        context2.grantPermissions(List.of("notifications"));
        page2 = context2.newPage();

        page2.navigate(server.pageUrl());
        chat2 = new ChatPage(page2);
    }

    @Test
    @DisplayName("Connect to default chat")
    @Tag("Smoke")
    void connectToDefaultChat() {
        chat.inputName(Users.USER1);
        chat.connectToChat();

        assertThat(chat.messageField()).isVisible();
        assertThat(chat.sendMessageButton()).isEnabled();
        assertThat(chat.chatWindow()).isVisible();

        assertThat(chat.chatMessage(Messages.INPUT_LOGIN)).hasCount(1);
        assertThat(chat
                .chatMessage(Messages.userConnectedToChat(Users.CURRENT_USER, Users.USER1, Rooms.PUBLIC.roomType())))
                .isVisible();
    }

    @Test
    @DisplayName("Current user send message to public chat")
    @Tag("Smoke")
    void currentUserSendMessageToPublicChat() {
        chat.inputName(Users.USER1);
        chat.connectToChat();
        chat.inputMessage(Messages.HELLO_WORLD);
        chat.sendMessage();

        String currentUserMessage = Messages.currentUserMessage(Messages.HELLO_WORLD);

        assertThat(chat.chatMessage(currentUserMessage)).hasCount(1);
        assertThat(chat.chatMessage(currentUserMessage)).hasText(currentUserMessage);
    }

    @Test
    @DisplayName("User in public room sees other users messages")
    void sendMessagesFromDifferentUsersPublicRoom() {
        setupForSecondUser();

        chat.inputName(Users.USER1);
        chat.connectToChat();

        chat2.inputName(Users.USER2);
        chat2.connectToChat();

        chat.inputMessage(Messages.HELLO_WORLD);
        chat.sendMessage();

        assertThat(chat2
                .chatMessage(Messages.otherUserMessage(Users.USER1, Rooms.PUBLIC.roomType(), Messages.HELLO_WORLD)))
                .hasCount(1);
    }

    @Test
    @DisplayName("User in privte room sees other users messages")
    void sendMessagesFromDifferentUsersPrivateRoom() {
        setupForSecondUser();

        chat.inputName(Users.USER1);
        chat.selectRoom(Rooms.PRIVATE.roomName());
        chat.connectToChat();

        chat2.inputName(Users.USER2);
        chat2.selectRoom(Rooms.PRIVATE.roomName());
        chat2.connectToChat();

        chat.inputMessage(Messages.HELLO_WORLD);
        chat.sendMessage();

        assertThat(chat2
                .chatMessage(Messages.otherUserMessage(Users.USER1, Rooms.PRIVATE.roomType(), Messages.HELLO_WORLD)))
                .hasCount(1);
    }

    // @Disabled("Known defect: private messages appear in public room, regardles of
    // connection order")
    @Test
    @DisplayName("Message in private room isn't apear in public room")
    void privateMessageIsolation() {
        setupForSecondUser();

        chat.inputName(Users.USER1);
        chat.selectRoom(Rooms.PRIVATE.roomName());
        chat.connectToChat();

        chat2.inputName(Users.USER2);
        chat2.selectRoom(Rooms.PUBLIC.roomName());
        chat2.connectToChat();

        chat.inputMessage(Messages.HELLO_WORLD);
        chat.sendMessage();

        assertThat(chat.chatMessage(Messages.currentUserMessage(Messages.HELLO_WORLD)))
                .hasCount(0);

        assertThat(chat2
                .chatMessage(Messages.otherUserMessage(Users.USER1, Rooms.PRIVATE.roomType(), Messages.HELLO_WORLD)))
                .not().hasText(Messages.otherUserMessage(Users.USER1, Rooms.PRIVATE.roomType(), Messages.HELLO_WORLD));
    }

    @Test
    @DisplayName("Connect to chat with already taken username")
    void connectWithTakenUserName() {
        setupForSecondUser();

        chat.inputName(Users.USER1);
        chat.connectToChat();

        chat2.inputName(Users.USER1);
        chat2.connectToChat();

        assertThat(chat
                .chatMessage(Messages.userConnectedToChat(Users.CURRENT_USER, Users.USER1, Rooms.PUBLIC.roomType())))
                .hasCount(1);
        assertThat(chat2.chatMessage(Messages.ERROR_LOGIN_EXISTS)).hasText(Messages.ERROR_LOGIN_EXISTS);

        chat2.inputMessage(Messages.HELLO);
        chat2.sendMessage();

        assertThat(chat.chatMessage(Messages.otherUserMessage(Users.USER1, Rooms.PUBLIC.roomType(), Messages.HELLO)))
                .hasCount(0);
        assertThat(chat.chatMessage(Messages.currentUserMessage(Messages.HELLO))).hasCount(0);
    }

    @Disabled("Known defect: message history collects both messages from user and from system, should collect only from user")
    @Test
    @DisplayName("Messages history collect all messages in chat")
    void checkMessagesHistory() {
        chat.inputName(Users.USER1);
        chat.connectToChat();
        chat.inputMessage(Messages.HELLO_WORLD);
        chat.sendMessage();

        MessagesHistoryPage messagesHistory = chat.viewMessageHistory();

        assertThat(messagesHistory.pageHeader()).containsText(Messages.MESSAGE_HISTORY_HEADER);
        assertThat(messagesHistory.messageByPosition(0)).containsText(Messages.INPUT_LOGIN);
        assertThat(messagesHistory
                .messageByText(Messages.userConnectedToChat(Users.CURRENT_USER, Users.USER1, Rooms.PUBLIC.roomType())))
                .hasCount(1);
        assertThat(messagesHistory.messageByText(Messages.HELLO_WORLD)).hasCount(1);
    }

    @Test
    @DisplayName("User in room sees system message when another user left room (page close)")
    void userLeftRoomMessage() {
        setupForSecondUser();

        chat.inputName(Users.USER1);
        chat.connectToChat();

        chat2.inputName(Users.USER2);
        chat2.connectToChat();

        page2.close();

        assertThat(chat.chatMessage(Messages.userLeftChat(Users.USER2, Rooms.PUBLIC.roomType()))).hasCount(1);
    }

    @Test
    @DisplayName("User in room sees system message when another user left room (context close)")
    void userLeftRoomMessage2() {
        setupForSecondUser();

        chat.inputName(Users.USER1);
        chat.connectToChat();

        chat2.inputName(Users.USER2);
        chat2.connectToChat();

        context2.close();

        assertThat(chat.chatMessage(Messages.userLeftChat(Users.USER2, Rooms.PUBLIC.roomType()))).hasCount(1);
    }

    @Disabled("Ofline mode doesn't affect system behavior, need to wait until server timeout and disconnect user")
    @Test
    @DisplayName("User in room sees system message when another user left room (context offline)")
    void userLeftRoomMessage3() {
        setupForSecondUser();

        chat.inputName(Users.USER1);
        chat.connectToChat();

        chat2.inputName(Users.USER2);
        chat2.connectToChat();

        context2.setOffline(true);

        assertThat(chat.chatMessage(Messages.userLeftChat(Users.USER2, Rooms.PUBLIC.roomType())))
                .hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(30));
    }

    @Test
    @DisplayName("WebSocket connection and messages")
    void checkWSConnectionAndMessages() {

        AtomicReference<WebSocket> wsc = new AtomicReference<>();
        CopyOnWriteArrayList<String> received = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<String> sent = new CopyOnWriteArrayList<>();

        page.onWebSocket(ws -> {
            wsc.set(ws);

            ws.onFrameSent(frame -> {
                sent.add(frame.text());
            });

            ws.onFrameReceived(frame -> {
                received.add(frame.text());
            });

        });

        page.navigate(server.pageUrl());
        chat.inputName(Users.USER1);
        chat.connectToChat();

        assertThat(chat.chatMessage(Messages.INPUT_LOGIN)).hasCount(1);

        assertEquals("ws://localhost:1401/chat?room=public", wsc.get().url());
        assertEquals(Messages.INPUT_LOGIN, received.get(0));
        assertEquals(Messages.userConnectedToChat(Users.CURRENT_USER, Users.USER1, Rooms.PUBLIC.roomType()),
                received.get(1));
        assertEquals(Messages.systemMessageLogin(Users.USER1, Rooms.PUBLIC.roomType()), sent.get(0));

        chat.inputMessage(Messages.HELLO_WORLD);
        chat.sendMessage();

        assertThat(chat.chatMessage(Messages.HELLO_WORLD)).hasCount(1);
        assertEquals(Messages.HELLO_WORLD, sent.get(1));

    }

}
