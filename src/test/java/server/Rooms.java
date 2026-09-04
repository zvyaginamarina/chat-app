package server;

public enum Rooms {
    PUBLIC("public", "Публичная комната"),
    PRIVATE("private", "Приватная комната");

    private final String roomType;
    private final String roomName;

    Rooms(String roomType, String roomName) {
        this.roomType = roomType;
        this.roomName = roomName;
    }

    public String roomType() {
        return roomType;
    }

    public String roomName() {
        return roomName;
    }

}
