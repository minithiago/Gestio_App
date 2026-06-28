package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Subscription
import com.example.data.SubscriptionDatabase
import com.example.data.SubscriptionRepository
import com.example.receiver.SubscriptionNotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val application: Application,
    private val repository: SubscriptionRepository
) : AndroidViewModel(application) {

    // Filter, sort and search query states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Todos")
    
    // Sorting options: "vencimiento" (Due date), "monto_desc" (Price high to low), "monto_asc" (Price low to high), "alfa" (Alphabetical)
    val sortBy = MutableStateFlow("vencimiento")

    // Subscriptions flow from repository
    private val rawSubscriptions: Flow<List<Subscription>> = repository.allSubscriptions

    // Filtered and sorted subscriptions state
    val subscriptions: StateFlow<List<Subscription>> = combine(
        rawSubscriptions,
        searchQuery,
        selectedCategory,
        sortBy
    ) { subs, query, category, sort ->
        var filtered = subs

        // Apply Search
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Apply Category Filter
        if (category != "Todos") {
            filtered = filtered.filter { it.category == category }
        }

        // Apply Sorting
        when (sort) {
            "vencimiento" -> {
                // Sort by how many days are remaining (ascending, i.e., closest due dates first)
                filtered.sortedBy { SubscriptionNotificationHelper.getDaysRemaining(it.dueDateDay) }
            }
            "monto_desc" -> {
                filtered.sortedByDescending { it.amount }
            }
            "monto_asc" -> {
                filtered.sortedBy { it.amount }
            }
            "alfa" -> {
                filtered.sortedBy { it.name.lowercase() }
            }
            else -> filtered
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculated Statistics
    val totalMonthlySpending: StateFlow<Double> = rawSubscriptions.map { subs ->
        subs.sumOf { sub ->
            if (sub.billingCycle == "Anual") {
                sub.amount / 12.0
            } else {
                sub.amount
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeSubscriptionsCount: StateFlow<Int> = rawSubscriptions.map { subs ->
        subs.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val upcomingSevenDaysSpending: StateFlow<Double> = rawSubscriptions.map { subs ->
        subs.filter { sub ->
            val daysRemaining = SubscriptionNotificationHelper.getDaysRemaining(sub.dueDateDay)
            daysRemaining in 0..7
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Database Actions
    fun addSubscription(subscription: Subscription) {
        viewModelScope.launch {
            val id = repository.insert(subscription)
            val insertedSub = subscription.copy(id = id.toInt())
            // Schedule reminder for the inserted subscription
            SubscriptionNotificationHelper.scheduleReminder(application, insertedSub)
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.update(subscription)
            // Schedule or cancel reminder based on new state
            SubscriptionNotificationHelper.scheduleReminder(application, subscription)
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            SubscriptionNotificationHelper.cancelReminder(application, subscription.id)
            repository.delete(subscription)
        }
    }

    fun toggleNotification(subscription: Subscription) {
        viewModelScope.launch {
            val updated = subscription.copy(isNotificationEnabled = !subscription.isNotificationEnabled)
            repository.update(updated)
            SubscriptionNotificationHelper.scheduleReminder(application, updated)
        }
    }

    // Initialize all existing alarms (useful on app start or reboot)
    fun rescheduleAllAlarms() {
        viewModelScope.launch {
            rawSubscriptions.first().forEach { subscription ->
                SubscriptionNotificationHelper.scheduleReminder(application, subscription)
            }
        }
    }

    fun seedDefaultTemplates() {
        viewModelScope.launch {
            val defaults = listOf(
                Subscription(id = 0, name = "Netflix Premium", amount = 15.99, category = "Entretenimiento", billingCycle = "Mensual", dueDateDay = 2, paymentMethod = "Tarjeta", colorHex = "#E50914", isNotificationEnabled = true, notes = "Auto-pay activado"),
                Subscription(id = 0, name = "Spotify Family", amount = 10.99, category = "Música", billingCycle = "Mensual", dueDateDay = 8, paymentMethod = "PayPal", colorHex = "#1DB954", isNotificationEnabled = false, notes = "Compartido"),
                Subscription(id = 0, name = "Creative Cloud", amount = 54.99, category = "Servicios", billingCycle = "Mensual", dueDateDay = 14, paymentMethod = "Tarjeta", colorHex = "#FF3100", isNotificationEnabled = false, notes = "Adobe CC")
            )
            defaults.forEach { sub ->
                repository.insert(sub)
            }
            rescheduleAllAlarms()
        }
    }

    fun clearAllSubscriptions() {
        viewModelScope.launch {
            rawSubscriptions.first().forEach { subscription ->
                SubscriptionNotificationHelper.cancelReminder(application, subscription.id)
            }
            repository.deleteAll()
        }
    }

    // Static Template options for fast addition
    val subscriptionTemplates = listOf(
        SubscriptionTemplate("Netflix", 16.99, "Entretenimiento", "#E50914"),
        SubscriptionTemplate("Spotify", 10.99, "Música", "#1DB954"),
        SubscriptionTemplate("Disney+", 13.99, "Entretenimiento", "#0063E5"),
        SubscriptionTemplate("HBO Max / Max", 9.99, "Entretenimiento", "#021A52"),
        SubscriptionTemplate("YouTube Premium", 12.99, "Entretenimiento", "#FF0000"),
        SubscriptionTemplate("Amazon Prime", 4.99, "Entretenimiento", "#00A8E1"),
        SubscriptionTemplate("iCloud", 2.99, "Servicios", "#2997FF"),
        SubscriptionTemplate("Nintendo Switch Online", 3.99, "Entretenimiento", "#E60012"),
        SubscriptionTemplate("PlayStation Plus", 8.99, "Entretenimiento", "#003087"),
        SubscriptionTemplate("Xbox Game Pass", 14.99, "Entretenimiento", "#107C10"),
        SubscriptionTemplate("Gimnasio", 35.00, "Salud", "#FF5722"),
        SubscriptionTemplate("Seguro Médico", 50.00, "Salud", "#2196F3"),
        SubscriptionTemplate("Fibra / Internet", 29.99, "Servicios", "#9C27B0")
    )
}

data class SubscriptionTemplate(
    val name: String,
    val defaultAmount: Double,
    val category: String,
    val colorHex: String
)

class SubscriptionViewModelFactory(
    private val application: Application,
    private val repository: SubscriptionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
