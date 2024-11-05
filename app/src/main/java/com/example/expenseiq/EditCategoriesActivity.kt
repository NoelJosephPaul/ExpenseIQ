package com.example.expenseiq

import android.graphics.Color.rgb
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class EditCategoriesActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)

        setContent {
            MaterialTheme {
                EditCategoriesScreen()
            }
        }
    }

    @Composable
    fun EditCategoriesScreen() {
        var categories by remember { mutableStateOf(listOf<Category>()) }
        var showAddCategoryDialog by remember { mutableStateOf(false) }
        var newCategoryName by remember { mutableStateOf("") }

        // Fetch categories initially and whenever refreshed
        LaunchedEffect(Unit) { refreshCategories { categories = it } }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Categories", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
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
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                    )
                } else {
                    // Use LazyColumn for scrolling
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)) {
                        items(categories) { category ->
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 3.dp, horizontal = 16.dp)
                                    .background(
                                        Color(rgb(242, 242, 242)),
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
                                IconButton(onClick = {
                                    deleteCategory(category.name) {
                                        refreshCategories { categories = it }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Category")
                                }
                            }}
                        }
                    }
                }

                // Show add category dialog
                if (showAddCategoryDialog) {
                    AddCategoryDialog(
                        categoryName = newCategoryName,
                        onCategoryNameChange = { newCategoryName = it },
                        onDismiss = { showAddCategoryDialog = false },
                        onAddCategory = { categoryName ->
                            addCategory(categoryName) {
                                newCategoryName = "" // Reset the input field
                                refreshCategories { categories = it } // Refresh categories
                                showAddCategoryDialog = false // Close the dialog
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
                e.printStackTrace() // Log error or show error message to the user
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

    @Composable
    fun AddCategoryDialog(
        categoryName: String,
        onCategoryNameChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onAddCategory: (String) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
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
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
