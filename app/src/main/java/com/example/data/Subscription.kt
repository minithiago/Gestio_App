package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val category: String, // Entretenimiento, Música, Salud, Servicios, Otros
    val billingCycle: String, // Mensual, Anual
    val dueDateDay: Int, // Day of the month (1-31)
    val paymentMethod: String, // Tarjeta, PayPal, Banco, Efectivo
    val colorHex: String, // Hex color code
    val isNotificationEnabled: Boolean = true,
    val notes: String = ""
)
