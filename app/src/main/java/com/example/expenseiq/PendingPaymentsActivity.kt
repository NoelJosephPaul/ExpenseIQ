package com.example.expenseiq

import com.example.expenseiq.AppDatabase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
class PendingPaymentsActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PendingPaymentsScreen()
            }
        }

        db = AppDatabase.getDatabase(this) // Initialize the database
    }

    @Composable
    fun PendingPaymentsScreen() {
        var pendingPayments by remember { mutableStateOf(listOf<PendingPayment>()) }
        var categories by remember { mutableStateOf(listOf<Category>()) }

        // Fetch pending payments and categories from the database
        LaunchedEffect(Unit) {
            pendingPayments = db.pendingPaymentDao().getAllPendingPayments()
            categories = db.categoryDao().getAllCategories()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pending Payments") }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Text("Pending Payments:", style = MaterialTheme.typography.titleLarge)

                pendingPayments.forEach { payment ->
                    PaymentItem(payment, categories) { selectedCategory ->
                        // Move payment from pending to expenses
                        lifecycleScope.launch {
                            db.pendingPaymentDao().deletePendingPayment(payment.id)
                            db.transactionDao().insertTransaction(
                                Transaction(amount = payment.amount, categoryId = selectedCategory.id, date =  payment.date) // Use categoryId instead of category
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PaymentItem(payment: PendingPayment, categories: List<Category>, onCategorySelected: (Category) -> Unit) {
        var selectedCategory by remember { mutableStateOf<Category?>(null) }
        var showCategoryDialog by remember { mutableStateOf(false) }

        Row(modifier = Modifier.padding(8.dp)) {
            Text("Amount: ${payment.amount}", modifier = Modifier.weight(1f))
            Button(onClick = { showCategoryDialog = true }) {
                Text(selectedCategory?.name ?: "Select Category")
            }
        }

        if (showCategoryDialog) {
            CategorySelectionDialog(
                categories = categories,
                onSelectCategory = { category ->
                    selectedCategory = category
                    showCategoryDialog = false
                    onCategorySelected(category)
                },
                onDismiss = { showCategoryDialog = false }
            )
        }
    }

    @Composable
    fun CategorySelectionDialog(categories: List<Category>, onSelectCategory: (Category) -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Category") },
            text = {
                Column {
                    categories.forEach { category ->
                        TextButton(onClick = { onSelectCategory(category) }) {
                            Text(category.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}
