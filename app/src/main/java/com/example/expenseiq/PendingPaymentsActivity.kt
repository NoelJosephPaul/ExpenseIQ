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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.expenseiq.ui.theme.ExpenseIQTheme
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
                    title = {
                        Text(
                            "Pending Expenses",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
            ) {
                if (pendingPayments.isEmpty()) {
                    // Show "No Pending Payments" message
                    Text(
                        text = "No Pending Expenses",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(50.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center // Center align the text
                    )
                } else {
                    // Show the list of pending payments
                    LazyColumn(modifier = Modifier.padding(bottom = 20.dp)) {
                        items(pendingPayments) { payment ->
                            PaymentItem(payment, categories,
                                onCategorySelected = { selectedCategory, updatedAmount ->
                                    // Move payment from pending to expenses
                                    lifecycleScope.launch {
                                        db.pendingPaymentDao().deletePendingPayment(payment.id)
                                        db.transactionDao().insertTransaction(
                                            Transaction(amount = updatedAmount, categoryId = selectedCategory.id, date = payment.date)
                                        )
                                        // Refresh pending payments after deletion
                                        pendingPayments = db.pendingPaymentDao().getAllPendingPayments()
                                    }
                                },
                                onRemove = {
                                    // Remove payment from pending list
                                    lifecycleScope.launch {
                                        db.pendingPaymentDao().deletePendingPayment(payment.id)
                                        pendingPayments = db.pendingPaymentDao().getAllPendingPayments()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PaymentItem(
        payment: PendingPayment,
        categories: List<Category>,
        onCategorySelected: (Category, Float) -> Unit,
        onRemove: () -> Unit
    ) {
        var showCategoryDialog by remember { mutableStateOf(false) }

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
                    .padding(vertical = 2.dp, horizontal = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // Aligns all items vertically in the center
            ) {
                // Display the date on the left
                Text(
                    text = payment.date,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Display the amount in the center
                Text(
                    text = "₹ ${payment.amount}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Display the "+" button to open dialog and "X" button to remove payment on the right
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Select Category")
                    }

                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Payment")
                    }
                }
            }}

        // Show the category selection dialog if triggered
        if (showCategoryDialog) {
            CategorySelectionDialog(
                categories = categories,
                initialAmount = payment.amount,
                onSelectCategory = { selectedCategory, updatedAmount ->
                    showCategoryDialog = false
                    onCategorySelected(selectedCategory, updatedAmount)
                },
                onDismiss = { showCategoryDialog = false }
            )
        }
    }



    @Composable
    fun CategorySelectionDialog(
        categories: List<Category>,
        initialAmount: Float,
        onSelectCategory: (Category, Float) -> Unit,
        onDismiss: () -> Unit
    ) {
        var amount by remember { mutableStateOf(initialAmount.toString()) }
        var selectedCategory by remember { mutableStateOf<Category?>(null) } // Track selected category

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Amount and Select Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Edit Amount") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Select Category:")

                    // Make the list of categories scrollable
                    LazyColumn {
                        items(categories) { category ->
                            TextButton(
                                onClick = { selectedCategory = category }, // Set selected category
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        color = if (selectedCategory == category) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                            ) {
                                Text(
                                    text = category.name,
                                    color = Color.Black

                                    //if (selectedCategory == category) MaterialTheme.colorScheme.primary
                                    //else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        selectedCategory?.let { category ->
                            onSelectCategory(category, amount.toFloatOrNull() ?: initialAmount)
                            onDismiss() // Close dialog after selection
                        }
                    }) {
                        Text("Add")
                    }
                }
            }
        )
    }
}
