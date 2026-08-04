package com.quangthe.canca.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "fish_tickets")
data class FishTicket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketName: String,
    val tarePerBag: Double = 0.0, // Khối lượng bì mỗi mã (thùng/giỏ)
    val impurityPerTon: Int = 0,   // Sẽ không dùng trong cân cá
    val unitPrice: Int = 0,
    val deposit: Long = 0,      // Tiền cọc, ứng (kết hợp)
    val isDeleted: Boolean = false,
    val deductionType: Int = 0,   // 0: Phần trăm (%), 1: Số kg
    val deductionValue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fish_sheets",
    foreignKeys = [
        ForeignKey(
            entity = FishTicket::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ticketId")]
)
data class FishSheet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketId: Int,
    val sheetIndex: Int,        // 0, 1, 2...
    val numCols: Int,
    val numRows: Int,
    val colTitles: String,      // JSON array
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fish_cells",
    primaryKeys = ["sheetId", "rowIndex", "colIndex"],
    foreignKeys = [
        ForeignKey(
            entity = FishSheet::class,
            parentColumns = ["id"],
            childColumns = ["sheetId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FishCell(
    val sheetId: Int,
    val rowIndex: Int,
    val colIndex: Int,
    val value: Double
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val decimalPlaces: Int = 1,
    val maxIntegerDigits: Int = 2,
    val defaultNumCols: Int = 5,
    val defaultNumRows: Int = 5,
    val speakOnCellComplete: Boolean = true,
    val speakOnColumnComplete: Boolean = false,
    val vibrateOnColumnComplete: Boolean = true,
    val autoFocusNext: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f
)
