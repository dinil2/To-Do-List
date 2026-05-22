package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Task
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val stats by viewModel.taskStats.collectAsStateWithLifecycle()
    
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.selectedPriority.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ElectricNeonViolet,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = SlateDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Heading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Task Space",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Complete your orbits today",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SlateDarkSurfaceVariant)
                        .border(1.dp, ElectricNeonViolet.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Logo Orbit",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Banner
            StatsBanner(stats = stats)

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Flow Filters
            FilterSection(
                selectedCategory = selectedCategory,
                selectedPriority = selectedPriority,
                selectedStatus = selectedStatus,
                categories = viewModel.categories,
                priorities = viewModel.priorities,
                onCategorySelect = { viewModel.setCategoryFilter(it) },
                onPrioritySelect = { viewModel.setPriorityFilter(it) },
                onStatusSelect = { viewModel.setStatusFilter(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tasks List
            if (tasks.isEmpty()) {
                EmptyStateView(
                    isSearch = searchQuery.isNotEmpty() || selectedCategory != "All" || selectedPriority != "All" || selectedStatus != "All"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = tasks,
                        key = { it.id }
                    ) { task ->
                        TaskItemRow(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskComplete(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onEdit = { taskToEdit = task },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddDialog) {
        TaskDialog(
            categories = viewModel.categories,
            priorities = viewModel.priorities,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, cat, prio, due ->
                viewModel.addTask(title, desc, cat, prio, due)
                showAddDialog = false
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { task ->
        TaskDialog(
            task = task,
            categories = viewModel.categories,
            priorities = viewModel.priorities,
            onDismiss = { taskToEdit = null },
            onSave = { title, desc, cat, prio, due ->
                viewModel.editTask(
                    task.copy(
                        title = title,
                        description = desc,
                        category = cat,
                        priority = prio,
                        dueDate = due
                    )
                )
                taskToEdit = null
            }
        )
    }
}

@Composable
fun StatsBanner(stats: TaskStats) {
    val progressPercent = (stats.completionPercentage * 100).toInt()
    val incentiveText = when {
        stats.totalCount == 0 -> "Let's list some goals and launch!"
        progressPercent == 0 -> "Ready to dock? Check items to advance!"
        progressPercent < 50 -> "Orbit calculations look good. Keeping pace!"
        progressPercent < 100 -> "More than halfway home! Full thruster power!"
        else -> "Perfect orbit established! Outstanding work!"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ElectricNeonViolet.copy(alpha = 0.85f),
                        Color(0xFF5D33D6).copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("stats_banner")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orbit Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${stats.completedCount} of ${stats.totalCount} Done",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic progress bar
            LinearProgressIndicator(
                progress = { stats.completionPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CyanAccent,
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Hint",
                    tint = CyanAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = incentiveText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search cosmic missions...", color = TextMutedDark) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondaryDark
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Search",
                        tint = TextSecondaryDark
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricNeonViolet,
            unfocusedBorderColor = SlateDarkSurfaceVariant,
            focusedContainerColor = SlateDarkSurface,
            unfocusedContainerColor = SlateDarkSurface,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_input")
    )
}

@Composable
fun FilterSection(
    selectedCategory: String,
    selectedPriority: String,
    selectedStatus: String,
    categories: List<String>,
    priorities: List<String>,
    onCategorySelect: (String) -> Unit,
    onPrioritySelect: (String) -> Unit,
    onStatusSelect: (String) -> Unit
) {
    val scrollStateCategory = rememberScrollState()
    val scrollStatePriority = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status Row (All, Active, Completed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statusOptions = listOf("All", "Active", "Completed")
            statusOptions.forEach { status ->
                val isSelected = selectedStatus == status
                FilterChip(
                    selected = isSelected,
                    onClick = { onStatusSelect(status) },
                    label = { Text(status) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricNeonViolet.copy(alpha = 0.25f),
                        selectedLabelColor = Color.White,
                        containerColor = SlateDarkSurface,
                        labelColor = TextSecondaryDark
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("status_filter_chip_$status")
                )
            }
        }

        // Horizontal Category Tabs
        Box(modifier = Modifier.fillMaxWidth()) {
            val categoryOptions = listOf("All") + categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_filter_row")
                    .horizontalScroll(scrollStateCategory),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categoryOptions.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Card(
                        modifier = Modifier
                            .clickable { onCategorySelect(cat) }
                            .testTag("cat_pill_$cat"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ElectricNeonViolet else SlateDarkSurface
                        ),
                        border = if (!isSelected) BorderStroke(1.dp, SlateDarkSurfaceVariant) else null
                    ) {
                        Text(
                            text = cat,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else TextSecondaryDark
                        )
                    }
                }
            }
        }

        // Horizontal Priority Toggles
        Box(modifier = Modifier.fillMaxWidth()) {
            val priorityOptions = listOf("All") + priorities
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("priority_filter_row")
                    .horizontalScroll(scrollStatePriority),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                priorityOptions.forEach { prio ->
                    val isSelected = selectedPriority == prio
                    val badgeColor = when (prio) {
                        "High" -> CoralAccent
                        "Medium" -> AmberGold
                        "Low" -> EmeraldGreen
                        else -> CyanAccent
                    }
                    Card(
                        modifier = Modifier
                            .clickable { onPrioritySelect(prio) }
                            .testTag("prio_pill_$prio"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) badgeColor.copy(alpha = 0.2f) else SlateDarkSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) badgeColor else SlateDarkSurfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (prio != "All") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = prio,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else TextSecondaryDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemRow(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val priorityColor = when (task.priority) {
        "High" -> CoralAccent
        "Medium" -> AmberGold
        else -> EmeraldGreen
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}")
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) SlateDarkSurface.copy(alpha = 0.5f) else SlateDarkSurface
        ),
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) SlateDarkSurfaceVariant.copy(alpha = 0.5f) else SlateDarkSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Checkbox Orbit
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("task_checkbox_click_${task.id}")
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Toggle Complete",
                            tint = ElectricNeonViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(2.dp, TextSecondaryDark, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) TextMutedDark else TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricNeonViolet.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ElectricNeonViolet
                            )
                        }

                        // Priority indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(priorityColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.priority,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = priorityColor
                            )
                        }

                        // Due Date
                        if (!task.dueDate.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Due Date",
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = task.dueDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }

                // Expand Chevron
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand description",
                        tint = TextSecondaryDark
                    )
                }
            }

            // Description and editing block
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Divider(color = SlateDarkSurfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (task.description.isNotBlank()) task.description else "No supplementary cargo records (description).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (task.description.isNotBlank()) TextSecondaryDark else TextMutedDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onEdit,
                            colors = ButtonDefaults.textButtonColors(contentColor = CyanAccent),
                            modifier = Modifier.testTag("task_edit_button_${task.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = CoralAccent),
                            modifier = Modifier.testTag("task_delete_button_${task.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Obliterate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isSearch: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else Icons.Default.CheckCircle,
                contentDescription = "Empty State Icon",
                tint = TextMutedDark,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isSearch) "Search Orbit Failed" else "Empty Flight Log",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSearch) "No tasks matching filter vectors." else "Create a task using the + button to launch orbits.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedDark,
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun TaskDialog(
    task: Task? = null,
    categories: List<String>,
    priorities: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, category: String, priority: String, dueDate: String?) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(task?.category ?: categories.first()) }
    var selectedPriority by remember { mutableStateOf(task?.priority ?: "Medium") }
    var dueDateText by remember { mutableStateOf(task?.dueDate ?: "") }

    var titleError by remember { mutableStateOf(false) }
    val scrollStateCategories = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDarkSurface),
            border = BorderStroke(1.dp, SlateDarkSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .testTag("task_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (task == null) "Log New Mission" else "Re-program Mission",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Divider(color = SlateDarkSurfaceVariant)

                // Title Input
                Column {
                    Text("Mission Title *", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = false
                        },
                        isError = titleError,
                        placeholder = { Text("Enter title...", color = TextMutedDark) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricNeonViolet,
                            unfocusedBorderColor = SlateDarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_title_input")
                    )
                    if (titleError) {
                        Text(
                            text = "Title cannot be blank",
                            color = CoralAccent,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Description Input
                Column {
                    Text("Telemetry Log (Description)", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Log notes / parameters...", color = TextMutedDark) },
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricNeonViolet,
                            unfocusedBorderColor = SlateDarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_desc_input")
                    )
                }

                // Due Date input
                Column {
                    Text("Docking Target (Due Date)", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = dueDateText,
                        onValueChange = { dueDateText = it },
                        placeholder = { Text("e.g. Today, Tomorrow, or May 25", color = TextMutedDark) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricNeonViolet,
                            unfocusedBorderColor = SlateDarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_due_input")
                    )

                    // Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf("Today", "Tomorrow", "Next Week")
                        presets.forEach { preset ->
                            Card(
                                modifier = Modifier.clickable { dueDateText = preset },
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateDarkSurfaceVariant)
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Category Selector
                Column {
                    Text("Task Sector (Category)", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollStateCategories),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Card(
                                modifier = Modifier.clickable { selectedCategory = cat },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ElectricNeonViolet else SlateDarkSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else TextSecondaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Priority Selector
                Column {
                    Text("Thrust Vector (Priority)", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorities.forEach { prio ->
                            val isSelected = selectedPriority == prio
                            val color = when (prio) {
                                "High" -> CoralAccent
                                "Medium" -> AmberGold
                                else -> EmeraldGreen
                            }
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPriority = prio },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) color.copy(alpha = 0.25f) else SlateDarkSurfaceVariant
                                ),
                                border = if (isSelected) BorderStroke(1.dp, color) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = prio,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondaryDark),
                        modifier = Modifier.testTag("dialog_cancel_button")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                            } else {
                                onSave(title, description, selectedCategory, selectedPriority, dueDateText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricNeonViolet),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("dialog_save_button")
                    ) {
                        Text("Launch Mission", color = Color.White)
                    }
                }
            }
        }
    }
}
