package gearhub.website.gearhub.model;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    DELIVERED;

    public static OrderStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        return OrderStatus.valueOf(raw.trim().toUpperCase());
    }
}
