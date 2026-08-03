# Kế hoạch chuyển đổi ứng dụng Cân Lúa sang Cân Cá

Dự án hiện tại đang sử dụng thuật ngữ "Rice" (Lúa) trong code và "Lúa" trong UI. Kế hoạch này sẽ đổi tất cả sang "Fish" (Cá) để phù hợp với tên ứng dụng là "CÂN CÁ".

## Những thay đổi chính

### 1. Dữ liệu (Data Layer)
- Đổi tên các Entity: `RiceTicket` -> `FishTicket`, `RiceSheet` -> `FishSheet`, `RiceCell` -> `FishCell`.
- Cập nhật tên bảng trong Database: `rice_tickets` -> `fish_tickets`, v.v.
- Đổi tên DAO: `RiceDao` -> `FishDao`.
- Đổi tên Repository: `RiceRepository` -> `FishRepository`.
- Đổi tên Database class: `RiceDatabase` -> `FishDatabase`.

### 2. ViewModel Layer
- Đổi tên `RiceViewModel` -> `FishViewModel`.
- Cập nhật các biến và hàm bên trong ViewModel.

### 3. Giao diện (UI Layer)
- Cập nhật tất cả các Text hiển thị từ "lúa" sang "cá".
- Ví dụ: "TỔNG TIỀN LÚA" -> "TỔNG TIỀN CÁ", "Cân lúa by Quang Thế" -> "Cân cá by Quang Thế".
- Cập nhật các tham chiếu class trong các Screen.

### 4. Tiện ích (Utils)
- Cập nhật `ExportUtils.kt` để đổi tên class và nhãn trong file Excel xuất ra.

## Danh sách file cần thay đổi

### [MODIFY] [Entities.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/data/Entities.kt)
### [MODIFY] [RiceDao.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/data/RiceDao.kt) -> `FishDao.kt`
### [MODIFY] [RiceDatabase.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/data/RiceDatabase.kt) -> `FishDatabase.kt`
### [MODIFY] [RiceRepository.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/data/RiceRepository.kt) -> `FishRepository.kt`
### [MODIFY] [RiceViewModel.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/viewmodel/RiceViewModel.kt) -> `FishViewModel.kt`
### [MODIFY] [MainScreen.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/ui/screens/MainScreen.kt)
### [MODIFY] [SettingsScreen.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/ui/screens/SettingsScreen.kt)
### [MODIFY] [TicketDetailScreen.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/ui/screens/TicketDetailScreen.kt)
### [MODIFY] [TrashScreen.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/ui/screens/TrashScreen.kt)
### [MODIFY] [ExportUtils.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/utils/ExportUtils.kt)
### [MODIFY] [MainActivity.kt](file:///D:/AndroidStudioProjects/CANCA/app/src/main/java/com/quangthe/canca/MainActivity.kt)

## Kế hoạch kiểm tra
1. Xây dựng lại dự án (Rebuild project).
2. Kiểm tra xem ứng dụng có khởi chạy thành công không.
3. Kiểm tra các màn hình xem chữ "lúa" đã được thay bằng "cá" chưa.
4. Kiểm tra chức năng lưu trữ và hiển thị dữ liệu (Database migration nếu cần, nhưng ở đây có thể xóa data cũ để làm mới).
