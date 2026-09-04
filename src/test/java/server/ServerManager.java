package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import lombok.Getter;

public class ServerManager {
    @Getter
    private final int port; // Номер порта, на котором будет запущен сервер.
    private WebSocketServer webSocketServer; // Экземпляр WebSocket-сервера.

    // Конструктор класса, автоматически ищет свободный порт.
    public ServerManager() {
        this.port = findFreePort(); // Инициализация порта при создании экземпляра.
    }

    // Метод для поиска свободного порта.
    private int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) { // Создаем сокет с портом 0, чтобы ОС выбрала свободный порт.
            return socket.getLocalPort(); // Возвращаем выбранный порт.
        } catch (IOException e) {
            throw new RuntimeException("Не удалось найти свободный порт", e); // Обработка исключения, если не удалось
                                                                              // найти порт.
        }
    }

    // Метод для запуска WebSocket-сервера.
    public void start() {
        try {
            webSocketServer = new WebSocketServer(port); // Создаем экземпляр WebSocket-сервера на найденном порту.
            new Thread(() -> { // Запускаем сервер в отдельном потоке.
                try {
                    webSocketServer.start(); // Запускаем сервер.
                } catch (Exception e) {
                    throw new RuntimeException("Server start failed", e); // Обработка исключения, если запуск сервера
                                                                          // не удался.
                }
            }).start();
            waitUntilReady(); // Ожидаем, пока сервер не станет доступным.
        } catch (Exception e) {
            throw new RuntimeException("Ошибка запуска сервера", e); // Обработка исключения при запуске сервера.
        }
    }

    // Метод для ожидания, пока сервер не станет доступным.
    private void waitUntilReady() throws InterruptedException {
        int timeout = 15000; // Максимальное время ожидания (15 секунд).
        int interval = 500; // Интервал проверки (500 мс).
        int elapsed = 0; // Время, прошедшее с начала ожидания.

        while (!isPortOpen()) { // Проверяем, открыт ли порт.
            if (elapsed >= timeout)
                throw new RuntimeException("Timeout"); // Если время ожидания истекло, выбрасываем исключение.
            Thread.sleep(interval); // Ждем перед следующей проверкой.
            elapsed += interval; // Увеличиваем счетчик времени.
        }
    }

    // Метод для проверки, открыт ли порт.
    private boolean isPortOpen() {
        try (Socket ignored = new Socket("localhost", port)) { // Пытаемся подключиться к локальному порту.
            return true; // Если подключение успешно, порт открыт.
        } catch (IOException e) {
            return false; // Если возникло исключение, порт закрыт.
        }
    }

    // Метод для остановки WebSocket-сервера.
    public void stop() {
        try {
            if (webSocketServer != null) { // Проверяем, инициализирован ли сервер.
                webSocketServer.stop(); // Останавливаем сервер.
            }
        } catch (Exception e) {
            System.err.println("Ошибка остановки сервера: " + e.getMessage()); // Обработка исключения при остановке
                                                                               // сервера.
        }
    }

    public String pageUrl() {
        String url = "http://localhost:" + port + "/index.html";
        return url;
    }

    public String wsUrl() {
        String url = "ws://localhost:" + port + "/chat?room=public";
        return url;
    }
}