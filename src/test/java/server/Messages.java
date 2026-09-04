package server;

public final class Messages {
    public static final String INPUT_LOGIN = "Введите ваш логин:";
    public static final String HELLO_WORLD = "Hello, world!";
    public static final String HELLO = "Hello!";

    public static final String ERROR_LOGIN_EXISTS = "ERROR: Логин уже занят";
    public static final String MESSAGE_HISTORY_HEADER = "История сообщений";

    public static String userConnectedToChat(String senderType, String user, String roomType) {
        return senderType + ": " + user + " в комнате " + roomType + " подключился";
    }

    public static String currentUserMessage(String message) {
        return Users.CURRENT_USER + ": " + message;
    }

    public static String otherUserMessage(String user, String roomType, String message) {
        return user + " в комнате " + roomType + ": " + message;
    }

    public static String userLeftChat(String user, String roomType) {
        return Users.SERVER + ": " + user + " в комнате " + roomType + " покинул чат";
    }

    public static String systemMessageLogin(String user, String roomType) {
        return "LOGIN:" + user + " в комнате " + roomType;
    }
}
