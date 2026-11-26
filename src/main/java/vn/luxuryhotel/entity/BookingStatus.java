package vn.luxuryhotel.entity;
public enum BookingStatus {
    PENDING("Chờ xác nhận"), CONFIRMED("Đã xác nhận"), CHECKED_IN("Đang lưu trú"),
    CANCELLED("Đã hủy"), REJECTED("Bị từ chối"), NO_SHOW("Không đến"),
    AWAITING_PAYMENT("Chờ thanh toán"), COMPLETED("Hoàn tất");
    private final String label;
    BookingStatus(String label) { this.label=label; }
    public String getLabel() { return label; }
    public boolean holdsRoom() { return this==PENDING || this==CONFIRMED || this==CHECKED_IN; }
}
