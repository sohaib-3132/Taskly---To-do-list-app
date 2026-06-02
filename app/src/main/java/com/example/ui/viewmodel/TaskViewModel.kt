package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.receiver.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    
    // Auth and Session state persistence
    private val sharedPrefs = application.getSharedPreferences("taskly_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUserEmail = MutableStateFlow<String?>(sharedPrefs.getString("current_user_email", null))
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _currentUserName = MutableStateFlow<String?>(sharedPrefs.getString("current_user_name", null))
    val currentUserName: StateFlow<String?> = _currentUserName.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = _currentUserEmail.map { !it.isNullOrEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = !sharedPrefs.getString("current_user_email", null).isNullOrEmpty()
        )

    // Base flows
    val allTasks: StateFlow<List<Task>>
    
    // UI Filters
    private val _selectedCategory = MutableStateFlow("All") // All, Work, Personal, Urgent
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedStatus = MutableStateFlow("All") // All, Active, Completed
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Greeting settings
    private val _profileName = MutableStateFlow(sharedPrefs.getString("current_user_name", "Alex") ?: "Alex")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    // Cloud synchronization states
    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    fun triggerSync() {
        viewModelScope.launch {
            if (_syncStatus.value == SyncStatus.SYNCING) return@launch
            _syncStatus.value = SyncStatus.SYNCING
            kotlinx.coroutines.delay(1800) // simulated network synchronization delay
            _lastSyncedTime.value = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.SYNCED
        }
    }

    private fun triggerSyncOnWrite() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            kotlinx.coroutines.delay(1200) // quick automatic sync broadcast on local write
            _lastSyncedTime.value = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.SYNCED
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        val taskDao = database.taskDao()
        repository = TaskRepository(taskDao)
        
        allTasks = repository.allTasks
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed default tasks if database is empty on first boot
        viewModelScope.launch {
            allTasks.first { true } // wait for initial fetch
            if (allTasks.value.isEmpty()) {
                seedMockTasks()
            }
        }
    }

    private suspend fun seedMockTasks() {
        // High fidelity mock tasks matching the screenshot EXACTLY
        val mocks = listOf(
            Task(
                name = "Review project proposal",
                dueDate = "Tomorrow",
                priority = "High",
                category = "Work",
                description = "Review the timeline, scope, and resource plan for the Taskly Android launch.",
                isCompleted = false,
                createdTimestamp = System.currentTimeMillis() - 86400000 // Yesterday
            ),
            Task(
                name = "Buy groceries",
                dueDate = "Today at 11:30 AM",
                priority = "Medium",
                category = "Personal",
                description = "Need to pick up milk, bread, spinach, and apples.",
                isCompleted = true,
                createdTimestamp = System.currentTimeMillis() - 86400000 + 5400000
            ),
            Task(
                name = "Book doctor appointment",
                dueDate = "June 10 at 1:15 PM",
                priority = "High",
                category = "Personal",
                description = "Routine annual physical checkup.",
                isCompleted = false,
                createdTimestamp = System.currentTimeMillis() - 86400000 + 11700000
            ),
            Task(
                name = "Send anniversary card",
                dueDate = "June 1 at 9:00 AM",
                priority = "Medium",
                category = "Personal",
                description = "Purchase and mail card for parents' wedding anniversary.",
                isCompleted = true,
                createdTimestamp = System.currentTimeMillis() - 172800000
            ),
            Task(
                name = "Update portfolio",
                dueDate = "June 15 at 4:45 PM",
                priority = "Low",
                category = "Work",
                description = "Upload the latest Jetpack Compose case studies to my Behance and portfolio site.",
                isCompleted = false,
                createdTimestamp = System.currentTimeMillis() - 172800000 + 27900000
            )
        )
        for (mock in mocks) {
            repository.insertTask(mock)
        }
    }

    // Filtered task lists for home display or search
    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks,
        _selectedCategory,
        _selectedStatus,
        _searchQuery
    ) { tasks, category, status, query ->
        tasks.filter { task ->
            // Category Match
            val matchesCategory = if (category == "All") {
                true
            } else {
                task.category.equals(category, ignoreCase = true)
            }

            // Status Match
            val matchesStatus = when (status) {
                "Active" -> !task.isCompleted
                "Completed" -> task.isCompleted
                else -> true
            }

            // Search query match
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                task.name.contains(query, ignoreCase = true) ||
                        task.description.contains(query, ignoreCase = true)
            }

            matchesCategory && matchesStatus && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics derived state flows
    val totalTasksCount: StateFlow<Int> = allTasks
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedTasksCount: StateFlow<Int> = allTasks
        .map { list -> list.count { it.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Setter Actions
    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(status: String) {
        _selectedStatus.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateProfileName(name: String) {
        val currentEmail = _currentUserEmail.value
        if (!currentEmail.isNullOrEmpty()) {
            sharedPrefs.edit()
                .putString("user_name_${currentEmail.trim().lowercase()}", name)
                .putString("current_user_name", name)
                .apply()
            _currentUserName.value = name
        }
        _profileName.value = name
    }

    // Auth Actions
    fun registerUser(email: String, name: String, pass: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || name.trim().isEmpty() || pass.isEmpty()) return false
        if (sharedPrefs.contains("user_pwd_$cleanEmail")) {
            return false // user already registered
        }
        sharedPrefs.edit()
            .putString("user_pwd_$cleanEmail", pass)
            .putString("user_name_$cleanEmail", name.trim())
            .apply()
        return true
    }

    fun loginUser(email: String, pass: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val savedPass = sharedPrefs.getString("user_pwd_$cleanEmail", null)
        if (savedPass == pass) {
            val name = sharedPrefs.getString("user_name_$cleanEmail", "Alex") ?: "Alex"
            sharedPrefs.edit()
                .putString("current_user_email", cleanEmail)
                .putString("current_user_name", name)
                .apply()
            _currentUserEmail.value = cleanEmail
            _currentUserName.value = name
            _profileName.value = name
            return true
        }
        return false
    }

    fun registerAndLoginSocial(email: String, name: String, provider: String) {
        val cleanEmail = email.trim().lowercase()
        sharedPrefs.edit()
            .putString("user_pwd_$cleanEmail", "social_auth_$provider")
            .putString("user_name_$cleanEmail", name)
            .putString("current_user_email", cleanEmail)
            .putString("current_user_name", name)
            .apply()
        _currentUserEmail.value = cleanEmail
        _currentUserName.value = name
        _profileName.value = name
    }

    fun logoutUser() {
        sharedPrefs.edit()
            .remove("current_user_email")
            .remove("current_user_name")
            .apply()
        _currentUserEmail.value = null
        _currentUserName.value = null
    }

    // Database Writes
    fun addTask(
        name: String,
        dueDate: String,
        priority: String,
        category: String,
        description: String,
        dueTimestamp: Long? = null
    ) {
        viewModelScope.launch {
            val task = Task(
                name = name,
                dueDate = dueDate,
                priority = priority,
                category = category,
                description = description,
                isCompleted = false,
                dueTimestamp = dueTimestamp
            )
            val insertedId = repository.insertTask(task)
            if (dueTimestamp != null && dueTimestamp > System.currentTimeMillis()) {
                ReminderScheduler.schedule(getApplication(), task.copy(id = insertedId.toInt()))
            }
            triggerSyncOnWrite()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), task)
            repository.updateTask(task)
            if (task.dueTimestamp != null && task.dueTimestamp > System.currentTimeMillis() && !task.isCompleted) {
                ReminderScheduler.schedule(getApplication(), task)
            }
            triggerSyncOnWrite()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            ReminderScheduler.cancel(getApplication(), updated)
            repository.updateTask(updated)
            if (!updated.isCompleted && updated.dueTimestamp != null && updated.dueTimestamp > System.currentTimeMillis()) {
                ReminderScheduler.schedule(getApplication(), updated)
            }
            triggerSyncOnWrite()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), task)
            repository.deleteTask(task)
            triggerSyncOnWrite()
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAllTasks()
            triggerSyncOnWrite()
        }
    }
}

enum class SyncStatus {
    SYNCED,
    SYNCING,
    ERROR
}
