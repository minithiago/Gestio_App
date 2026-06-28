package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.Subscription
import com.example.receiver.SubscriptionNotificationHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionApp(
    viewModel: SubscriptionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subscriptions by viewModel.subscriptions.collectAsState()
    val totalSpending by viewModel.totalMonthlySpending.collectAsState()
    val activeCount by viewModel.activeSubscriptionsCount.collectAsState()
    val upcomingSpending by viewModel.upcomingSevenDaysSpending.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    // Dialog state for adding/editing subscriptions
    var showUpsertDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }
    
    // Navigation tab state
    var currentTab by remember { mutableStateOf("Inicio") }

    // State for notification permission
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "¡Recordatorios de pago activados!", Toast.LENGTH_SHORT).show()
            viewModel.rescheduleAllAlarms()
        } else {
            Toast.makeText(context, "Los recordatorios del sistema no podrán mostrarse", Toast.LENGTH_LONG).show()
        }
    }

    // Trigger permission request or check on startup
    LaunchedEffect(Unit) {
        SubscriptionNotificationHelper.createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.rescheduleAllAlarms()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar: Circle, 40dp, bg primaryContainer, text onPrimaryContainer
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Text(
                            text = "Mis suscripciones",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Toast.makeText(context, "Los recordatorios ya están configurados", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("notification_status_button")
                        ) {
                            Icon(
                                imageVector = if (hasNotificationPermission) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "Estado de notificaciones",
                                tint = if (hasNotificationPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val tabs = listOf(
                        Triple("Inicio", Icons.Default.Home, "Inicio"),
                        Triple("Pagos", Icons.Default.Payments, "Pagos"),
                        Triple("Alertas", Icons.Default.Notifications, "Alertas"),
                        Triple("Ajustes", Icons.Default.Settings, "Ajustes")
                    )
                    
                    tabs.forEach { (tabName, icon, label) ->
                        val isSelected = currentTab == tabName
                        val tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTab = tabName }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(100.dp))
                                        .padding(horizontal = 20.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = fontWeight
                                ),
                                color = tint
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == "Inicio") {
                FloatingActionButton(
                    onClick = {
                        editingSubscription = null
                        showUpsertDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .testTag("add_subscription_fab")
                        .padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir Suscripción",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "Inicio" -> {
                    val nextUpcomingSub = remember(subscriptions) {
                        subscriptions.minByOrNull { SubscriptionNotificationHelper.getDaysRemaining(it.dueDateDay) }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Dashboard Banner (Sleek summary card)
                        item {
                            DashboardSection(
                                totalSpending = totalSpending,
                                activeCount = activeCount,
                                upcomingSpending = upcomingSpending,
                                hasNotificationPermission = hasNotificationPermission,
                                onRequestPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                nextUpcomingSub = nextUpcomingSub
                            )
                        }

                        // Category Share canvas visualization
                        if (subscriptions.isNotEmpty()) {
                            item {
                                CategoryExpenseChart(subscriptions = subscriptions)
                            }
                        }

                        // PRÓXIMOS VENCIMIENTOS header
                        item {
                            Text(
                                text = "PRÓXIMOS VENCIMIENTOS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        // Search and filters
                        item {
                            FilterSection(
                                searchQuery = searchQuery,
                                onSearchChanged = { viewModel.searchQuery.value = it },
                                selectedCategory = selectedCategory,
                                onCategorySelected = { viewModel.selectedCategory.value = it },
                                sortBy = sortBy,
                                onSortChanged = { viewModel.sortBy.value = it }
                            )
                        }

                        // Subscription List
                        if (subscriptions.isEmpty()) {
                            item {
                                EmptyStateSection(
                                    hasFilters = searchQuery.isNotBlank() || selectedCategory != "Todos",
                                    onClearFilters = {
                                        viewModel.searchQuery.value = ""
                                        viewModel.selectedCategory.value = "Todos"
                                    }
                                )
                            }
                        } else {
                            items(
                                items = subscriptions,
                                key = { it.id }
                            ) { subscription ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SubscriptionCard(
                                        subscription = subscription,
                                        onClick = {
                                            editingSubscription = subscription
                                            showUpsertDialog = true
                                        },
                                        onToggleNotification = {
                                            viewModel.toggleNotification(subscription)
                                        },
                                        onDelete = {
                                            viewModel.deleteSubscription(subscription)
                                            Toast.makeText(context, "Suscripción eliminada", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                "Pagos" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Distribución de Pagos",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        if (subscriptions.isNotEmpty()) {
                            CategoryExpenseChart(subscriptions = subscriptions)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Métodos de Pago Utilizados",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val methodTotals = remember(subscriptions) {
                                    subscriptions.groupBy { it.paymentMethod }
                                        .mapValues { (_, subs) -> subs.sumOf { it.amount } }
                                }
                                
                                if (methodTotals.isEmpty()) {
                                    Text("No hay métodos de pago registrados.", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    methodTotals.forEach { (method, total) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                val icon = when (method) {
                                                    "Tarjeta" -> Icons.Default.CreditCard
                                                    "PayPal" -> Icons.Default.Payments
                                                    "Banco" -> Icons.Default.AccountBalance
                                                    else -> Icons.Default.Money
                                                }
                                                Icon(imageVector = icon, contentDescription = method, tint = MaterialTheme.colorScheme.primary)
                                                Text(text = method, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            }
                                            Text(text = "$${String.format("%.2f", total)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Ciclos de Cobro",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val monthlySubs = subscriptions.filter { it.billingCycle == "Mensual" }
                                val annualSubs = subscriptions.filter { it.billingCycle == "Anual" }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Suscripciones Mensuales", style = MaterialTheme.typography.bodyMedium)
                                    Text("${monthlySubs.size}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Suscripciones Anuales", style = MaterialTheme.typography.bodyMedium)
                                    Text("${annualSubs.size}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
                "Alertas" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Alertas y Recordatorios",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasNotificationPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, if (hasNotificationPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasNotificationPermission) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (hasNotificationPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (hasNotificationPermission) "Alertas de Sistema Activas" else "Recordatorios Desactivados",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (hasNotificationPermission) "Recibirás alertas en tu teléfono un día antes de que expire cada suscripción." else "Necesitas otorgar permiso de notificaciones para recibir recordatorios.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (!hasNotificationPermission) {
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Activar")
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Estado de Alarma por Suscripción",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val activeAlarms = subscriptions.filter { it.isNotificationEnabled }

                                if (subscriptions.isEmpty()) {
                                    Text("Añade una suscripción para gestionar sus alarmas.", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Text(
                                        text = "${activeAlarms.size} de ${subscriptions.size} suscripciones tienen alertas activadas.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    subscriptions.forEach { sub ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(modifier = Modifier.size(10.dp).background(Color(android.graphics.Color.parseColor(sub.colorHex)), CircleShape))
                                                Text(text = sub.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            }
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = if (sub.isNotificationEnabled) "Activo" else "Apagado",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (sub.isNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                                Switch(
                                                    checked = sub.isNotificationEnabled,
                                                    onCheckedChange = { viewModel.toggleNotification(sub) },
                                                    modifier = Modifier.scale(0.8f)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
                "Ajustes" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Ajustes y Configuración",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Tema de la Aplicación",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                    Text(
                                        text = "Sleek Interface (Cyan y Azul Oscuro)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Se aplica automáticamente una paleta de color azul y cyan moderna y sofisticada con alto contraste y legibilidad.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Acciones de Datos",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.seedDefaultTemplates()
                                        Toast.makeText(context, "Plantillas por defecto cargadas", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cargar Plantillas Rápidas")
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearAllSubscriptions()
                                        Toast.makeText(context, "Todas las suscripciones han sido eliminadas", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Eliminar Todas las Suscripciones")
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Gestor de Suscripciones",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Versión 1.2.0 • Tema Sleek Interface",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Desarrollado por Ivan Naranjo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add / Edit Subscription Dialog
        if (showUpsertDialog) {
            UpsertSubscriptionDialog(
                subscription = editingSubscription,
                templates = viewModel.subscriptionTemplates,
                onDismiss = { showUpsertDialog = false },
                onSave = { sub ->
                    if (sub.id == 0) {
                        viewModel.addSubscription(sub)
                        Toast.makeText(context, "Suscripción añadida", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateSubscription(sub)
                        Toast.makeText(context, "Suscripción actualizada", Toast.LENGTH_SHORT).show()
                    }
                    showUpsertDialog = false
                }
            )
        }
    }
}

@Composable
fun DashboardSection(
    totalSpending: Double,
    activeCount: Int,
    upcomingSpending: Double,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    nextUpcomingSub: Subscription? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gasto Mensual Estimado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$${String.format("%.2f", totalSpending)}",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("total_spending_text")
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Activas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$activeCount",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    Text(
                        text = "Próximos 7 días",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$${String.format("%.2f", upcomingSpending)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (nextUpcomingSub != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val daysRemaining = SubscriptionNotificationHelper.getDaysRemaining(nextUpcomingSub.dueDateDay)
                val daysText = when {
                    daysRemaining == 0 -> "vence hoy"
                    daysRemaining == 1 -> "vence mañana"
                    daysRemaining < 0 -> "venció hace ${-daysRemaining} días"
                    else -> "vence en $daysRemaining días"
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(android.graphics.Color.parseColor(nextUpcomingSub.colorHex)), CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Próximo pago: ${nextUpcomingSub.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Día ${nextUpcomingSub.dueDateDay} ($daysText) • $${String.format("%.2f", nextUpcomingSub.amount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (!hasNotificationPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onRequestPermission
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notificaciones Desactivadas",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Actívalas aquí para no olvidar ningún vencimiento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Activar",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryExpenseChart(subscriptions: List<Subscription>) {
    val categorySpending = remember(subscriptions) {
        val map = subscriptions.groupBy { it.category }
            .mapValues { (_, subs) ->
                subs.sumOf { sub ->
                    if (sub.billingCycle == "Anual") sub.amount / 12.0 else sub.amount
                }
            }
        val total = map.values.sum()
        if (total > 0) {
            map.mapValues { it.value / total }
        } else {
            emptyMap()
        }
    }

    if (categorySpending.isEmpty()) return

    // Define color mapping for categories
    val categoryColors = mapOf(
        "Entretenimiento" to Color(0xFF0284C7), // Sky Blue
        "Música" to Color(0xFF06B6D4),         // Teal Cyan
        "Salud" to Color(0xFF00ADB5),          // Bright Cyan
        "Servicios" to Color(0xFF1E3A8A),      // Dark Navy Blue
        "Otros" to Color(0xFF64748B)           // Slate Grey Blue
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribución por Categorías",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Canvas to draw custom horizontal split progress bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                var currentStart = 0f
                val barWidth = size.width

                categorySpending.forEach { (cat, ratio) ->
                    val color = categoryColors[cat] ?: Color.Gray
                    val segmentWidth = barWidth * ratio.toFloat()
                    
                    if (segmentWidth > 0f) {
                        drawRect(
                            color = color,
                            topLeft = Offset(currentStart, 0f),
                            size = Size(segmentWidth, size.height)
                        )
                        currentStart += segmentWidth
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legends
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                categorySpending.forEach { (cat, ratio) ->
                    val color = categoryColors[cat] ?: Color.Gray
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$cat (${(ratio * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    sortBy: String,
    onSortChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Search and Sort button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text("Buscar suscripción...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("search_input")
            )

            // Sort Selector Box / Button
            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .size(52.dp)
                        .testTag("sort_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Ordenar por"
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Vencimiento más cercano") },
                        onClick = {
                            onSortChanged("vencimiento")
                            showSortMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Mayor precio") },
                        onClick = {
                            onSortChanged("monto_desc")
                            showSortMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Menor precio") },
                        onClick = {
                            onSortChanged("monto_asc")
                            showSortMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Nombre (A-Z)") },
                        onClick = {
                            onSortChanged("alfa")
                            showSortMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category filter chips
        val categories = listOf("Todos", "Entretenimiento", "Música", "Salud", "Servicios", "Otros")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null,
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }
    }
}

@Composable
fun SubscriptionCard(
    subscription: Subscription,
    onClick: () -> Unit,
    onToggleNotification: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val daysRemaining = SubscriptionNotificationHelper.getDaysRemaining(subscription.dueDateDay)
    val nextPaymentDate = SubscriptionNotificationHelper.getNextPaymentDate(subscription.dueDateDay)
    
    val badgeColor = when {
        daysRemaining == 0 -> MaterialTheme.colorScheme.error
        daysRemaining in 1..3 -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        daysRemaining in 4..7 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val daysText = when {
        daysRemaining == 0 -> "Vence hoy"
        daysRemaining == 1 -> "Vence mañana"
        else -> "Vence en $daysRemaining días"
    }

    val categoryIcon = when (subscription.category) {
        "Entretenimiento" -> Icons.Outlined.Tv
        "Música" -> Icons.Outlined.MusicNote
        "Salud" -> Icons.Outlined.FavoriteBorder
        "Servicios" -> Icons.Outlined.CloudQueue
        else -> Icons.Outlined.FolderOpen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_card_${subscription.name}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left color vertical line matching subscription theme
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(Color(android.graphics.Color.parseColor(subscription.colorHex)))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subscription Icon Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Color(android.graphics.Color.parseColor(subscription.colorHex)).copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = subscription.category,
                        tint = Color(android.graphics.Color.parseColor(subscription.colorHex))
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Mid: Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = subscription.paymentMethod,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (subscription.billingCycle == "Anual") "Anual" else "Mensual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date and warning badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = SubscriptionNotificationHelper.formatSpanishDate(nextPaymentDate),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = daysText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = badgeColor
                            )
                        }
                    }
                }

                // Right: Amount & Actions
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = "$${String.format("%.2f", subscription.amount)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Alert Bell Toggle
                        IconButton(
                            onClick = { onToggleNotification() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (subscription.isNotificationEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = "Alarma",
                                tint = if (subscription.isNotificationEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Delete button
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar suscripción?") },
            text = { Text("¿Estás seguro de que deseas eliminar la suscripción a ${subscription.name}? También se cancelará su alarma.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmptyStateSection(
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasFilters) Icons.Default.SearchOff else Icons.Default.AddCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (hasFilters) "Sin resultados" else "Organiza tus pagos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasFilters) {
                "No encontramos ninguna suscripción que coincida con tus filtros."
            } else {
                "Añade tus suscripciones mensuales (Netflix, Spotify, etc.) para visualizarlas en un solo lugar y recibir alertas."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (hasFilters) {
            Button(onClick = onClearFilters) {
                Text("Limpiar filtros")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpsertSubscriptionDialog(
    subscription: Subscription?,
    templates: List<SubscriptionTemplate>,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit
) {
    val isEditMode = subscription != null

    var name by remember { mutableStateOf(subscription?.name ?: "") }
    var amountString by remember { mutableStateOf(subscription?.amount?.let { String.format("%.2f", it) } ?: "") }
    var category by remember { mutableStateOf(subscription?.category ?: "Entretenimiento") }
    var billingCycle by remember { mutableStateOf(subscription?.billingCycle ?: "Mensual") }
    var dueDateDay by remember { mutableStateOf(subscription?.dueDateDay ?: 1) }
    var paymentMethod by remember { mutableStateOf(subscription?.paymentMethod ?: "Tarjeta") }
    var colorHex by remember { mutableStateOf(subscription?.colorHex ?: "#2196F3") }
    var isNotificationEnabled by remember { mutableStateOf(subscription?.isNotificationEnabled ?: true) }
    var notes by remember { mutableStateOf(subscription?.notes ?: "") }

    val presetColors = listOf(
        "#E50914", // Netflix Red
        "#1DB954", // Spotify Green
        "#0063E5", // Disney Blue
        "#E60012", // Nintendo Red
        "#00A8E1", // Prime Cyan
        "#9C27B0", // Purple
        "#FF5722", // Deep Orange
        "#FF9800", // Orange
        "#2196F3", // Blue
        "#4CAF50", // Green
        "#9E9E9E"  // Grey
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isEditMode) "Editar Suscripción" else "Nueva Suscripción",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    val amount = amountString.toDoubleOrNull() ?: 0.0
                                    if (name.isBlank() || amount <= 0.0) {
                                        return@Button
                                    }
                                    onSave(
                                        Subscription(
                                            id = subscription?.id ?: 0,
                                            name = name,
                                            amount = amount,
                                            category = category,
                                            billingCycle = billingCycle,
                                            dueDateDay = dueDateDay,
                                            paymentMethod = paymentMethod,
                                            colorHex = colorHex,
                                            isNotificationEnabled = isNotificationEnabled,
                                            notes = notes
                                        )
                                    )
                                },
                                enabled = name.isNotBlank() && (amountString.toDoubleOrNull() ?: 0.0) > 0.0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_subscription_button")
                            ) {
                                Text("Guardar")
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Template Pre-sets (Only for adding new subscriptions)
                    if (!isEditMode) {
                        Column {
                            Text(
                                text = "Plantillas Rápidas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                templates.forEach { template ->
                                    Card(
                                        onClick = {
                                            name = template.name
                                            amountString = String.format("%.2f", template.defaultAmount)
                                            category = template.category
                                            colorHex = template.colorHex
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(android.graphics.Color.parseColor(template.colorHex)).copy(alpha = 0.08f)
                                        ),
                                        border = if (name == template.name) {
                                            BorderStroke(2.dp, Color(android.graphics.Color.parseColor(template.colorHex)))
                                        } else {
                                            BorderStroke(1.dp, Color(android.graphics.Color.parseColor(template.colorHex)).copy(alpha = 0.2f))
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(android.graphics.Color.parseColor(template.colorHex)), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = template.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de la suscripción") },
                        placeholder = { Text("Ej. Netflix, Gimnasio") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subscription_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = amountString,
                            onValueChange = { amountString = it },
                            label = { Text("Monto ($)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("subscription_price_input")
                        )

                        // Billing cycle dropdown selector
                        var cycleExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = billingCycle,
                                onValueChange = {},
                                label = { Text("Ciclo") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { cycleExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { cycleExpanded = true }
                            )
                            DropdownMenu(
                                expanded = cycleExpanded,
                                onDismissRequest = { cycleExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mensual") },
                                    onClick = {
                                        billingCycle = "Mensual"
                                        cycleExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Anual") },
                                    onClick = {
                                        billingCycle = "Anual"
                                        cycleExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Due date day select slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Día de Cobro Mensual",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Día $dueDateDay",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = dueDateDay.toFloat(),
                            onValueChange = { dueDateDay = it.toInt() },
                            valueRange = 1f..31f,
                            steps = 30,
                            modifier = Modifier.testTag("due_day_slider")
                        )
                    }

                    // Category dropdown selector
                    var categoryExpanded by remember { mutableStateOf(false) }
                    val categories = listOf("Entretenimiento", "Música", "Salud", "Servicios", "Otros")
                    Box {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            label = { Text("Categoría") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { categoryExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryExpanded = true }
                        )
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Payment method selection dropdown
                    var paymentExpanded by remember { mutableStateOf(false) }
                    val paymentMethods = listOf("Tarjeta", "PayPal", "Banco", "Efectivo")
                    Box {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            label = { Text("Método de pago") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { paymentExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { paymentExpanded = true }
                        )
                        DropdownMenu(
                            expanded = paymentExpanded,
                            onDismissRequest = { paymentExpanded = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMethod = method
                                        paymentExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Color picker
                    Column {
                        Text(
                            text = "Color Visual",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            presetColors.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(color, CircleShape)
                                        .clickable { colorHex = hex }
                                        .border(
                                            width = if (colorHex == hex) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }

                    // Toggle notifications
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enviar Recordatorio",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Recibe una alerta 1 día antes del vencimiento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { isNotificationEnabled = it },
                            modifier = Modifier.testTag("notification_toggle")
                        )
                    }

                    // Notes (Opcional)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas o detalles (Opcional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
