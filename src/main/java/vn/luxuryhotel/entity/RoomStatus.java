package vn.luxuryhotel.entity;
public enum RoomStatus {
    READY("Sẵn sàng"), OCCUPIED("Có khách"), DIRTY("Chờ dọn"), MAINTENANCE("Bảo trì"), INACTIVE("Ngừng kinh doanh");
    private final String label;
    RoomStatus(String label) { this.label=label; }
    public String getLabel() { return label; }
    public boolean bookable() { return this!=MAINTENANCE && this!=INACTIVE; }
}
