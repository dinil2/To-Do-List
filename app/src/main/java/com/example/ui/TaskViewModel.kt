package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TaskStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val activeCount: Int = 0,
    val completionPercentage: Float = 0f
)

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // Filter states
    val selectedCategory = MutableStateFlow("All")
    val selectedPriority = MutableStateFlow("All")
    val selectedStatus = MutableStateFlow("All") // "All", "Active", "Completed"
    val searchQuery = MutableStateFlow("")

    // Raw categories and priorities for selectors
    val categories = listOf("Personal", "Work", "Urgent", "Shopping", "Health")
    val priorities = listOf("Low", "Medium", "High")

    // Filtered tasks StateFlow
    val filteredTasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        selectedCategory,
        selectedPriority,
        selectedStatus,
        searchQuery
    ) { tasks, category, priority, status, query ->
        tasks.filter { task ->
            val matchesCategory = category == "All" || task.category.equals(category, ignoreCase = true)
            val matchesPriority = priority == "All" || task.priority.equals(priority, ignoreCase = true)
            val matchesStatus = when (status) {
                "Active" -> !task.isCompleted
                "Completed" -> task.isCompleted
                else -> true
            }
            val matchesSearch = query.isEmpty() || 
                    task.title.contains(query, ignoreCase = true) || 
                    task.description.contains(query, ignoreCase = true)

            matchesCategory && matchesPriority && matchesStatus && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactive task stats
    val taskStats: StateFlow<TaskStats> = repository.allTasks.map { tasks ->
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val active = total - completed
        val percent = if (total > 0) (completed.toFloat() / total.toFloat()) else 0f
        TaskStats(total, completed, active, percent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskStats()
    )

    // User Operations
    fun addTask(
        title: String,
        description: String = "",
        category: String = "Personal",
        priority: String = "Medium",
        dueDate: String? = null
    ) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                val newTask = Task(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    dueDate = dueDate
                )
                repository.insert(newTask)
            }
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun editTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    fun deleteTaskById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    // Setters for filters
    fun setCategoryFilter(category: String) {
        selectedCategory.value = category
    }

    fun setPriorityFilter(priority: String) {
        selectedPriority.value = priority
    }

    fun setStatusFilter(status: String) {
        selectedStatus.value = status
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    // Provider Factory for instant creation inside setContent context
    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
