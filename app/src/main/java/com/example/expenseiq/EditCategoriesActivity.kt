package com.example.expenseiq

import android.graphics.Color.rgb
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.expenseiq.ui.theme.ExpenseIQTheme
import com.example.expenseiq.ui.theme.Purple40
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class EditCategoriesActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)

        setContent {
            ExpenseIQTheme {
                EditCategoriesScreen()
            }
        }
    }

    @Composable
    fun EditCategoriesScreen() {
        var categories by remember { mutableStateOf(listOf<Category>()) }
        var showAddCategoryDialog by remember { mutableStateOf(false) }
        var showEditCategoryDialog by remember { mutableStateOf(false) }
        var selectedCategory: Category? by remember { mutableStateOf(null) }
        var newCategoryName by remember { mutableStateOf("") }
        val isDarkMode = isSystemInDarkTheme()

        LaunchedEffect(Unit) { refreshCategories { categories = it } }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Edit Categories", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                if (categories.isEmpty()) {
                    Text(
                        "Click the '+' icon to add a new category.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)) {
                        items(categories) { category ->
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 3.dp, horizontal = 16.dp)
                                    .background(
                                        if(isDarkMode) Color(rgb(40, 40, 43)) else Color(rgb(242, 242, 242)),
                                        shape = MaterialTheme.shapes.small
                                    )
                            ){
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = category.name)
                                    Row {
                                        IconButton(onClick = {
                                            selectedCategory = category
                                            newCategoryName = category.name
                                            showEditCategoryDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Category")
                                        }
                                        IconButton(onClick = {
                                            deleteCategory(category.name) {
                                                refreshCategories { categories = it }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Category")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddCategoryDialog) {
                    AddCategoryDialog(
                        categoryName = newCategoryName,
                        onCategoryNameChange = { newCategoryName = it },
                        onDismiss = { showAddCategoryDialog = false },
                        onAddCategory = { categoryName ->
                            addCategory(categoryName) {
                                newCategoryName = ""
                                refreshCategories { categories = it }
                                showAddCategoryDialog = false
                            }
                        }
                    )
                }

                if (showEditCategoryDialog && selectedCategory != null) {
                    EditCategoryDialog(
                        category = selectedCategory!!,
                        categoryName = newCategoryName,
                        onCategoryNameChange = { newCategoryName = it },
                        onDismiss = { showEditCategoryDialog = false },
                        onEditCategory = { updatedCategoryName ->
                            updateCategoryName(selectedCategory!!.name, updatedCategoryName) {
                                newCategoryName = ""
                                refreshCategories { categories = it }
                                showEditCategoryDialog = false
                            }
                        }
                    )
                }
            }
        }
    }

    private fun refreshCategories(onResult: (List<Category>) -> Unit) {
        lifecycleScope.launch {
            try {
                val categories = db.categoryDao().getAllCategories()
                onResult(categories)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addCategory(categoryName: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                val newCategory = Category(name = categoryName)
                db.categoryDao().insertCategory(newCategory)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun deleteCategory(categoryName: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                val category = db.categoryDao().getCategoryByName(categoryName)
                if (category != null) {
                    db.transactionDao().deleteTransactionsByCategoryId(category.id)
                    db.categoryDao().deleteCategory(categoryName)
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateCategoryName(oldName: String, newName: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                val category = db.categoryDao().getCategoryByName(oldName)
                if (category != null) {
                    db.categoryDao().updateCategoryName(oldName, newName)
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Composable
    fun AddCategoryDialog(
        categoryName: String,
        onCategoryNameChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onAddCategory: (String) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = if (isSystemInDarkTheme()) Color(rgb(40, 40, 40)) else Color(249,249,249),
            title = { Text("Add New Category") },
            text = {
                TextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    label = { Text("Category Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (categoryName.isNotEmpty()) {
                        onAddCategory(categoryName)
                    }
                }) {
                    Text("Add",color = if(isSystemInDarkTheme()) Color.White else Purple40)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel",color = if(isSystemInDarkTheme()) Color.White else Purple40)
                }
            }
        )
    }

    @Composable
    fun EditCategoryDialog(
        category: Category,
        categoryName: String,
        onCategoryNameChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onEditCategory: (String) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = if (isSystemInDarkTheme()) Color(rgb(40, 40, 40)) else Color(249,249,249),
            title = { Text("Edit Category") },
            text = {
                TextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    label = { Text("New Category Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (categoryName.isNotEmpty()) {
                        onEditCategory(categoryName)
                    }
                }) {
                    Text("Save",color = if(isSystemInDarkTheme()) Color.White else Purple40)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel",color = if(isSystemInDarkTheme()) Color.White else Purple40)
                }
            }
        )
    }
}
