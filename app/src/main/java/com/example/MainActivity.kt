package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.Task
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TaskViewModel
import com.example.ui.viewmodel.SyncStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TasklyApp(viewModel = viewModel)
            }
        }
    }
}

enum class ScreenTab {
    Home, Search, Settings
}

@Composable
fun TasklyApp(viewModel: TaskViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (!isLoggedIn) {
        AuthScreen(
            onLogin = { email, pass, onResult ->
                val success = viewModel.loginUser(email, pass)
                onResult(success)
            },
            onSignUp = { email, name, pass, onResult ->
                val ok = viewModel.registerUser(email, name, pass)
                if (ok) {
                    viewModel.loginUser(email, pass)
                }
                onResult(ok)
            },
            onSocialLogin = { email, name, provider ->
                viewModel.registerAndLoginSocial(email, name, provider)
            }
        )
        return
    }

    var activeTab by remember { mutableStateOf(ScreenTab.Home) }
    
    // Bottom Sheet Overlay States
    var showAddSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToDeleteConfirm by remember { mutableStateOf<Task?>(null) }

    // Read states from viewmodel
    val tasks by viewModel.filteredTasks.collectAsState()
    val totalCount by viewModel.totalTasksCount.collectAsState()
    val completedCount by viewModel.completedTasksCount.collectAsState()
    val profileName by viewModel.profileName.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsState()

    // Determine current greeting depending on time of day
    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Dynamic Date formatter
    val dateText = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Screen 1 & 3 Faux Adaptive Navigation Bar for Mobile
            TasklyBottomBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Main views with animateContentSize or standard crossfade
            Crossfade(
                targetState = activeTab,
                animationSpec = tween(300),
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    ScreenTab.Home -> HomeScreen(
                        tasks = tasks,
                        totalCount = totalCount,
                        completedCount = completedCount,
                        profileName = profileName,
                        greetingText = greetingText,
                        dateText = dateText,
                        selectedCategory = selectedCategory,
                        selectedStatus = selectedStatus,
                        syncStatus = syncStatus,
                        lastSyncedTime = lastSyncedTime,
                        onTriggerSync = { viewModel.triggerSync() },
                        onCategorySelect = { viewModel.setCategoryFilter(it) },
                        onStatusSelect = { viewModel.setStatusFilter(it) },
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onTaskClick = { taskToEdit = it },
                        onAddClick = { showAddSheet = true },
                        onGoToSettings = { activeTab = ScreenTab.Settings }
                    )
                    ScreenTab.Search -> SearchScreen(
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        tasks = tasks,
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onTaskClick = { taskToEdit = it }
                    )
                    ScreenTab.Settings -> SettingsScreen(
                        profileName = profileName,
                        onProfileNameChange = { viewModel.updateProfileName(it) },
                        onResetDb = { viewModel.clearAllTasks() },
                        onSeedDb = { viewModel.clearAllTasks(); viewModel.addTask("Review project proposal", "Tomorrow", "High", "Work", "Review timeline and budget."); viewModel.addTask("Buy groceries", "Today at 11:30 AM", "Medium", "Personal", "Milk, veggies, fruit."); viewModel.addTask("Book doctor appointment", "June 10", "High", "Personal", "Routine checkup."); viewModel.addTask("Send anniversary card", "June 1", "Medium", "Personal", "Mail card to parents."); viewModel.addTask("Update portfolio", "June 15", "Low", "Work", "Case studies.") },
                        onLogout = { viewModel.logoutUser() }
                    )
                }
            }

            // Beautiful slide-up dialog bottom sheets (Responsive layouts)
            if (showAddSheet) {
                AddEditTaskSheet(
                    sheetTitle = "New Task",
                    onDismiss = { showAddSheet = false },
                    onConfirm = { name, dueDate, priority, cat, desc, dueTimestamp ->
                        viewModel.addTask(name, dueDate, priority, cat, desc, dueTimestamp)
                        showAddSheet = false
                    }
                )
            }

            taskToEdit?.let { task ->
                AddEditTaskSheet(
                    sheetTitle = "Edit Task",
                    initialTask = task,
                    onDismiss = { taskToEdit = null },
                    onConfirm = { name, dueDate, priority, cat, desc, dueTimestamp ->
                        viewModel.updateTask(task.copy(
                            name = name,
                            dueDate = dueDate,
                            priority = priority,
                            category = cat,
                            description = desc,
                            dueTimestamp = dueTimestamp
                        ))
                        taskToEdit = null
                    },
                    onDelete = {
                        taskToDeleteConfirm = task
                        taskToEdit = null
                    }
                )
            }

            // High priority danger confirmation overlay
            taskToDeleteConfirm?.let { task ->
                DeleteConfirmationDialog(
                    taskName = task.name,
                    onDismiss = { taskToDeleteConfirm = null },
                    onConfirm = {
                        viewModel.deleteTask(task)
                        taskToDeleteConfirm = null
                    }
                )
            }
        }
    }
}

@Composable
fun TasklyBottomBar(
    activeTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth()
            .height(72.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 4.dp
    ) {
        val items = listOf(
            Triple(ScreenTab.Home, Icons.Filled.Home, Icons.Outlined.Home),
            Triple(ScreenTab.Search, Icons.Filled.Search, Icons.Outlined.Search),
            Triple(ScreenTab.Settings, Icons.Filled.Settings, Icons.Outlined.Settings)
        )

        items.forEach { (tab, filledIcon, outlinedIcon) ->
            val isSelected = activeTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                        contentDescription = tab.name,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = tab.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.testTag("nav_btn_${tab.name.lowercase()}"),
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}

// ---------------- HOME SCREEN ----------------

@Composable
fun HomeScreen(
    tasks: List<Task>,
    totalCount: Int,
    completedCount: Int,
    profileName: String,
    greetingText: String,
    dateText: String,
    selectedCategory: String,
    selectedStatus: String,
    syncStatus: SyncStatus,
    lastSyncedTime: Long,
    onTriggerSync: () -> Unit,
    onCategorySelect: (String) -> Unit,
    onStatusSelect: (String) -> Unit,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onAddClick: () -> Unit,
    onGoToSettings: () -> Unit
) {
    var showSyncDetailsDialog by remember { mutableStateOf(false) }

    if (showSyncDetailsDialog) {
        Dialog(onDismissRequest = { showSyncDetailsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cloud Sync & Storage",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showSyncDetailsDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close dialog"
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Big Sync Status Icon & Pulse
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = when (syncStatus) {
                                    SyncStatus.SYNCED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    SyncStatus.SYNCING -> Color(0xFF2196F3).copy(alpha = 0.1f)
                                    SyncStatus.ERROR -> Color(0xFFF44336).copy(alpha = 0.1f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val rotationVal = if (syncStatus == SyncStatus.SYNCING) {
                            val transition = rememberInfiniteTransition(label = "SyncBigRotate")
                            val angle by transition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "syncRotation"
                            )
                            angle
                        } else 0f

                        Icon(
                            imageVector = when (syncStatus) {
                                SyncStatus.SYNCED -> Icons.Filled.CheckCircle
                                SyncStatus.SYNCING -> Icons.Filled.Refresh
                                SyncStatus.ERROR -> Icons.Filled.Warning
                            },
                            contentDescription = "Sync status icon",
                            tint = when (syncStatus) {
                                SyncStatus.SYNCED -> Color(0xFF4CAF50)
                                SyncStatus.SYNCING -> Color(0xFF2196F3)
                                SyncStatus.ERROR -> Color(0xFFF44336)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .rotate(rotationVal)
                        )
                    }

                    // Status text description
                    Text(
                        text = when (syncStatus) {
                            SyncStatus.SYNCED -> "Database Synced"
                            SyncStatus.SYNCING -> "Synchronizing database..."
                            SyncStatus.ERROR -> "Sync Connection Offline"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Database stats
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Storage System", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Room (SQLite)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cloud Mirror", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Active (AI Studio)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Synced", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val sdf = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
                                val formattedTime = remember(lastSyncedTime) { sdf.format(Date(lastSyncedTime)) }
                                Text(formattedTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons
                    Button(
                        onClick = onTriggerSync,
                        enabled = syncStatus != SyncStatus.SYNCING,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("force_sync_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (syncStatus == SyncStatus.SYNCING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Sync Room with Cloud Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp) // extra padding for bottom navigation & fab space
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "Logo icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Taskly",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Database Connection/Sync indicator
                    Row(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showSyncDetailsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("sync_indicator_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val indicatorColor = when (syncStatus) {
                            SyncStatus.SYNCED -> Color(0xFF4CAF50)
                            SyncStatus.SYNCING -> Color(0xFF2196F3)
                            SyncStatus.ERROR -> Color(0xFFF44336)
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(indicatorColor, CircleShape)
                        )

                        val rotationTransition = rememberInfiniteTransition(label = "SyncRotateTransition")
                        val syncRotationAngle by rotationTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "SyncRotation"
                        )

                        Icon(
                            imageVector = when (syncStatus) {
                                SyncStatus.SYNCED -> Icons.Filled.CheckCircle
                                SyncStatus.SYNCING -> Icons.Filled.Refresh
                                SyncStatus.ERROR -> Icons.Filled.Warning
                            },
                            contentDescription = "Sync details",
                            tint = indicatorColor,
                            modifier = Modifier
                                .size(14.dp)
                                .then(
                                    if (syncStatus == SyncStatus.SYNCING) {
                                        Modifier.rotate(syncRotationAngle)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }

                IconButton(
                    onClick = onGoToSettings,
                    modifier = Modifier.testTag("home_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings Icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Greeting Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                val waveEmoji = "👋"
                val finalGreeting = if (profileName.isNotBlank()) "$greetingText, $profileName" else "$greetingText $waveEmoji"
                Text(
                    text = finalGreeting,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bento Stats cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Main Highlight Card (Total Tasks)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Total list",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Total Tasks",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = totalCount.toString(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Card 2: Secondary Styled Bento Card (Completed)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed list",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Completed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = completedCount.toString(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Filters scrollable row (All, Active, Completed AND Category Toggles)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category tabs: All, Work, Personal, Urgent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("All", "Work", "Personal", "Urgent")
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategorySelect(cat) },
                            label = { Text(cat, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null,
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("cat_chip_$cat")
                        )
                    }
                }

                // Status tabs: All, Active, Completed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statuses = listOf("All", "Active", "Completed")
                    statuses.forEach { stat ->
                        FilterChip(
                            selected = selectedStatus == stat,
                            onClick = { onStatusSelect(stat) },
                            label = { Text(stat, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (selectedStatus == stat) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("status_chip_$stat")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task List View Section
            if (tasks.isEmpty()) {
                TasklyEmptyState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tasks.forEach { task ->
                        TaskItemCard(
                            task = task,
                            onToggle = { onToggleTask(task) },
                            onClick = { onTaskClick(task) }
                        )
                    }
                }
            }
        }

        // Beautiful floating action button
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(56.dp)
                .testTag("add_task_fab"),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Task Button",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun TasklyEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = "Inbox empty logo",
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Text(
            text = "No tasks yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Tap the + button to add your first task and start managing your day.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Checkbox Container
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (task.isCompleted) 0.dp else 2.dp,
                        color = if (task.isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable(onClick = onToggle)
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Check icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Task Text fields
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Due Date Info
                    if (task.dueDate.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Calendar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = task.dueDate,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Priority Tag Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (task.priority) {
                                    "High" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    "Medium" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = task.priority,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (task.priority) {
                                "High" -> MaterialTheme.colorScheme.error
                                "Medium" -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Category Tag Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------- SEARCH SCREEN ----------------

@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Search",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Search tasks by name or description details",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("What are you looking for?") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Live list
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search not found",
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No results found",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Try searching for a different keyword or check your spelling.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(tasks) { task ->
                    TaskItemCard(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}

// ---------------- SETTINGS SCREEN ----------------

@Composable
fun SettingsScreen(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    onResetDb: () -> Unit,
    onSeedDb: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Customize greets, lists, and database configurations",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Profile Editor Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "User Profile Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = profileName,
                    onValueChange = onProfileNameChange,
                    label = { Text("Display Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Session Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Authentication Session",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "You are currently logged in securely with local/social credentials. Sign out to return to the Bento Authentication Screen.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logout_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Session", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Local State Database configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Taskly Local Database Storage",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Room-persistence powers the tasks storage offline. Run tools below to seed screenshot mocks or reset database state.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                // Actions
                Button(
                    onClick = onSeedDb,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seed_database_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Seed Screenshot Mocks (5 Tasks)")
                }

                OutlinedButton(
                    onClick = onResetDb,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_database_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear All Tasks (Inspect Empty State)")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Taskly v1.0.0 — Modern M3 Design System Workspace",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.height(96.dp))
    }
}

// ---------------- RESPONSIVE BOTTOM SHEET DIALOG ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskSheet(
    sheetTitle: String,
    initialTask: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, dueDate: String, priority: String, category: String, description: String, dueTimestamp: Long?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var taskName by remember { mutableStateOf(initialTask?.name ?: "") }
    var taskDueDate by remember { mutableStateOf(initialTask?.dueDate ?: "Tomorrow at 9:00 AM") }
    var taskPriority by remember { mutableStateOf(initialTask?.priority ?: "Medium") }
    var taskCategory by remember { mutableStateOf(initialTask?.category ?: "Personal") }
    var taskDescription by remember { mutableStateOf(initialTask?.description ?: "") }
    var taskDueTimestamp by remember { mutableStateOf<Long?>(initialTask?.dueTimestamp) }

    // Dropdown selectors states
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDueDateMenu by remember { mutableStateOf(false) }

    val calendar = remember { java.util.Calendar.getInstance() }

    fun showDateTimePicker() {
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(java.util.Calendar.YEAR, year)
                calendar.set(java.util.Calendar.MONTH, month)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                
                val timePickerDialog = android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(java.util.Calendar.MINUTE, minute)
                        calendar.set(java.util.Calendar.SECOND, 0)
                        calendar.set(java.util.Calendar.MILLISECOND, 0)
                        
                        taskDueTimestamp = calendar.timeInMillis
                        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
                        taskDueDate = sdf.format(calendar.time)
                    },
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE),
                    false
                )
                timePickerDialog.show()
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Scrim Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Adaptive modal body
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {}) // block clicks inside card
                    // On wide screens limits width, on slim mobile is full screen bottom sheet
                    .widthIn(max = 480.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Mobile slide drag cue handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                            .size(width = 48.dp, height = 6.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(100.dp))
                    )

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sheetTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_sheet_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Sheet"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Form inputs
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Task name
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Task Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = taskName,
                                onValueChange = { if (it.length <= 120) taskName = it },
                                placeholder = { Text("What do you need to do?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("task_name_textfield"),
                                supportingText = { 
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text("${taskName.length} / 120")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                                singleLine = true
                            )
                        }

                        // Meta data inputs: Grid model
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Due Date Button Choice with dropdown triggers
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Due Date & Reminder",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showDueDateMenu = !showDueDateMenu },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("due_date_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(taskDueDate, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Date picker button",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showDueDateMenu,
                                        onDismissRequest = { showDueDateMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Today at 5:00 PM (Precise Reminder)") },
                                            onClick = {
                                                val cal = java.util.Calendar.getInstance()
                                                cal.set(java.util.Calendar.HOUR_OF_DAY, 17)
                                                cal.set(java.util.Calendar.MINUTE, 0)
                                                cal.set(java.util.Calendar.SECOND, 0)
                                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                                if (cal.timeInMillis < System.currentTimeMillis()) {
                                                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                                }
                                                taskDueTimestamp = cal.timeInMillis
                                                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
                                                taskDueDate = sdf.format(cal.time)
                                                showDueDateMenu = false
                                            },
                                            modifier = Modifier.testTag("due_menu_today")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Tomorrow at 9:00 AM (Precise Reminder)") },
                                            onClick = {
                                                val cal = java.util.Calendar.getInstance()
                                                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                                cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
                                                cal.set(java.util.Calendar.MINUTE, 0)
                                                cal.set(java.util.Calendar.SECOND, 0)
                                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                                taskDueTimestamp = cal.timeInMillis
                                                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
                                                taskDueDate = sdf.format(cal.time)
                                                showDueDateMenu = false
                                            },
                                            modifier = Modifier.testTag("due_menu_tomorrow")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Next Week at 9:00 AM (Precise Reminder)") },
                                            onClick = {
                                                val cal = java.util.Calendar.getInstance()
                                                cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                                                cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
                                                cal.set(java.util.Calendar.MINUTE, 0)
                                                cal.set(java.util.Calendar.SECOND, 0)
                                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                                taskDueTimestamp = cal.timeInMillis
                                                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
                                                taskDueDate = sdf.format(cal.time)
                                                showDueDateMenu = false
                                            },
                                            modifier = Modifier.testTag("due_menu_nextweek")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Choose Custom Date & Time...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                showDueDateMenu = false
                                                showDateTimePicker()
                                            },
                                            modifier = Modifier.testTag("due_menu_custom")
                                        )
                                    }
                                }
                            }

                            // Priority selector Button Selector dialog
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Priority",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showPriorityMenu = !showPriorityMenu },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("priority_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = "Flag",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(taskPriority, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Dropdown arrows",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showPriorityMenu,
                                        onDismissRequest = { showPriorityMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        listOf("High", "Medium", "Low").forEach { level ->
                                            DropdownMenuItem(
                                                text = { Text(level, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    taskPriority = level
                                                    showPriorityMenu = false
                                                },
                                                modifier = Modifier.testTag("priority_menu_$level")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Category field selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Category",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showCategoryMenu = !showCategoryMenu },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("category_select_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(taskCategory, fontSize = 14.sp)
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown arrows"
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showCategoryMenu,
                                    onDismissRequest = { showCategoryMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    listOf("Work", "Personal", "Urgent").forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                taskCategory = cat
                                                showCategoryMenu = false
                                            },
                                            modifier = Modifier.testTag("category_menu_$cat")
                                        )
                                    }
                                }
                            }
                        }

                        // Description TextField
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Description",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = taskDescription,
                                onValueChange = { taskDescription = it },
                                placeholder = { Text("Add details...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("task_description_textfield"),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Trigger buttons layout
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    if (taskName.isNotBlank()) {
                                        onConfirm(taskName, taskDueDate, taskPriority, taskCategory, taskDescription, taskDueTimestamp)
                                    }
                                },
                                enabled = taskName.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("confirm_task_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "Add symbol"
                                    )
                                    Text(
                                        text = if (initialTask == null) "Add Task" else "Save Changes",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Show delete option in Edit mode as beautiful high density custom Red trigger
                            if (initialTask != null && onDelete != null) {
                                OutlinedButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("delete_task_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Delete Task", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cancel_task_btn")
                            ) {
                                Text("Cancel", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- HIGH PRIORITY WARNING DELETE DIALOG CONFIRMATION ----------------

@Composable
fun DeleteConfirmationDialog(
    taskName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = "Danger Red warning symbol",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Delete Task?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "This action cannot be undone. Are you sure you want to permanently delete \"$taskName\"?",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_delete_confirm_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_delete_cancel_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        },
        modifier = Modifier
            .widthIn(max = 340.dp)
            .testTag("delete_confirmation_dialog"),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}

@Composable
fun AuthScreen(
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onSignUp: (String, String, String, (Boolean) -> Unit) -> Unit,
    onSocialLogin: (String, String, String) -> Unit
) {
    var isSignUpBlock by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showGooglePopup by remember { mutableStateOf(false) }
    var showFacebookPopup by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bento Glowing Header Logo Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Logo icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "TASKLY BENTO",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = if (isSignUpBlock) "Dynamic Local Registration" else "Modern Offline-First Workspace",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // Tab Selector Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isSignUpBlock) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { 
                            isSignUpBlock = false
                            errorMessage = null
                        }
                        .padding(vertical = 12.dp)
                        .testTag("tab_login"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Log In",
                        fontWeight = FontWeight.Bold,
                        color = if (!isSignUpBlock) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSignUpBlock) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { 
                            isSignUpBlock = true
                            errorMessage = null
                        }
                        .padding(vertical = 12.dp)
                        .testTag("tab_signup"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sign Up",
                        fontWeight = FontWeight.Bold,
                        color = if (isSignUpBlock) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main Credential Input Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isSignUpBlock) "Create Account credentials" else "Access your task panel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    errorMessage?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    contentDescription = "Error notification icon",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isSignUpBlock) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, "Name icon") },
                            modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, "Email icon") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, "Password lock icon") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = "Password visibility toggle"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )

                    if (isSignUpBlock) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, "Confirm Password icon") },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.CheckCircle else Icons.Default.Lock,
                                        contentDescription = "Confirm password visibility toggle"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            singleLine = true
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        } else {
                            Button(
                                onClick = {
                                    val trimmedEmail = email.trim()
                                    val trimmedName = name.trim()
                                    if (trimmedEmail.isEmpty() || password.isEmpty()) {
                                        errorMessage = "Email and password cannot be empty"
                                        return@Button
                                    }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                        errorMessage = "Please enter a valid email address"
                                        return@Button
                                    }
                                    if (password.length < 6) {
                                        errorMessage = "Password must be at least 6 characters"
                                        return@Button
                                    }
                                    
                                    isLoading = true
                                    errorMessage = null
                                    if (isSignUpBlock) {
                                        if (trimmedName.isEmpty()) {
                                            errorMessage = "Display name cannot be empty"
                                            isLoading = false
                                            return@Button
                                        }
                                        if (password != confirmPassword) {
                                            errorMessage = "Passwords do not match"
                                            isLoading = false
                                            return@Button
                                        }
                                        onSignUp(trimmedEmail, trimmedName, password) { success ->
                                            isLoading = false
                                            if (!success) {
                                                errorMessage = "User already registered or registration failed"
                                            }
                                        }
                                    } else {
                                        onLogin(trimmedEmail, password) { success ->
                                            isLoading = false
                                            if (!success) {
                                                errorMessage = "Incorrect email address or password"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    if (isSignUpBlock) "Register & Log In" else "Sign In to Taskly",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Divider card
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
                Text(
                    "OR CONTINUE WITH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    letterSpacing = 1.sp
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
            }

            // Social Integration Grid (Google / Facebook Cards side-by-side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google Block Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showGooglePopup = true }
                        .testTag("social_google_btn"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            tint = Color(0xFFEA4335), // Google Red
                            contentDescription = "Google Brand icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Facebook Block Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showFacebookPopup = true }
                        .testTag("social_facebook_btn"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            tint = Color(0xFF1877F2), // Facebook Blue
                            contentDescription = "Facebook Brand icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Facebook",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Google Sign-In Selector Popup Dialog
    if (showGooglePopup) {
        Dialog(onDismissRequest = { showGooglePopup = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("google_selector_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        tint = Color(0xFFEA4335),
                        contentDescription = "Google icon",
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Sign in with Google", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Select an account to authorize the Taskly Bento client application securely.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Real User Account Option derived dynamically from system metadata context
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSocialLogin("sohaibfarrukh363@gmail.com", "Sohaib Farrukh", "Google")
                                showGooglePopup = false
                            }
                            .testTag("google_option_sohaib"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("SF", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Sohaib Farrukh", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("sohaibfarrukh363@gmail.com", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Native Mock account mapping Alex Rivera
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSocialLogin("alex.rivera@gmail.com", "Alex Rivera", "Google")
                                showGooglePopup = false
                            }
                            .testTag("google_option_alex"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("AR", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Alex Rivera", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("alex.rivera@gmail.com", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    TextButton(onClick = { showGooglePopup = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Facebook SDK Authorization Popup Dialog
    if (showFacebookPopup) {
        Dialog(onDismissRequest = { showFacebookPopup = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("facebook_auth_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        tint = Color(0xFF1877F2),
                        contentDescription = "Facebook brand icon",
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Connect Facebook Account", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Taskly requests authorization to retrieve your profile picture and name.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            onSocialLogin("sohaibfarrukh363@facebook.com", "Sohaib Farrukh", "Facebook")
                            showFacebookPopup = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("facebook_confirm_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue as Sohaib Farrukh", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showFacebookPopup = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

