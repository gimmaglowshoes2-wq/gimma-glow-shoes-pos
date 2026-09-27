package com.example.data.model

enum class OrderStatus(val label: String, val stepIndex: Int) {
    QUEUED("Antrian", 0),
    WASHING("Sedang Dicuci", 1),
    DRYING("Pengeringan", 2),
    READY("Siap Diambil", 3),
    COMPLETED("Sudah Diambil", 4),
    CANCELLED("Dibatalkan", -1);

    companion object {
        fun fromLabel(label: String): OrderStatus {
            return entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) } ?: QUEUED
        }
    }
}

enum class PaymentMethod(val label: String) {
    CASH("Tunai"),
    QRIS("QRIS"),
    TRANSFER("Transfer"),
    LATER("Bayar Saat Ambil");

    companion object {
        fun fromLabel(label: String): PaymentMethod {
            return entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) } ?: CASH
        }
    }
}
