package com.example.expenseiq


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color.rgb
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.expenseiq.ui.theme.ExpenseIQTheme
import java.text.SimpleDateFormat
import java.util.*






@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    private var totalExpense by mutableFloatStateOf(0f)
    private var categories by mutableStateOf(listOf<String>())
    private var categoryTotals by mutableStateOf(mapOf<String, Float>())
    private var transactionsMap by mutableStateOf(mapOf<String, List<Transaction>>())
    private var selectedMonthYear by mutableStateOf(SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date()))
    val formattedDate by derivedStateOf {
        // Format "MM/yyyy" to "Month, Year" for display
        SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(
            SimpleDateFormat("MM/yyyy", Locale.getDefault()).parse(selectedMonthYear) ?: Date()
        )
    }

    companion object {
        private const val REQUEST_SMS_PERMISSION = 1001 // Define the request code
        private const val REQUEST_CODE_PENDING_PAYMENTS = 1
        private const val REQUEST_CODE_EDIT_CATEGORIES = 2 // Define a request code constant
    }

    private fun requestSmsPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.RECEIVE_SMS), REQUEST_SMS_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
            }
        }
    }


    // State to control the visibility of the dialog
    private var showPickerDialog by mutableStateOf(false)





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)

        setContent {
            MaterialTheme {
                MainScreen(context = this)
            }
            requestSmsPermissions()
        }
    }



    private fun refreshData(monthYear: String = selectedMonthYear) {
        lifecycleScope.launch {
            try {
                categories = db.categoryDao().getAllCategories().map { it.name }
                totalExpense = db.transactionDao().getCurrentMonthTotalExpenses(monthYear)

                categoryTotals = db.categoryDao().getAllCategories().associate { category ->
                    category.name to db.transactionDao().getTotalExpenseByCategoryAndMonth(
                        category.id, monthYear
                    )
                }

                transactionsMap = db.categoryDao().getAllCategories().associate { category ->
                    category.name to db.transactionDao().getTransactionsByCategoryAndMonth(
                        category.id, monthYear
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    @Composable
    fun MainScreen(context: Context) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var showAddPaymentDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            refreshData()
        }

        val currentDate = remember { mutableStateOf(selectedMonthYear) }

        BackHandler(enabled = drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }


        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text(
                        "Menu",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 20.dp, bottom = 50.dp)
                            .padding(horizontal = 16.dp)
                    )

                    // Edit Categories Button
                    TextButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            startActivityForResult(
                                Intent(context, EditCategoriesActivity::class.java),
                                REQUEST_CODE_EDIT_CATEGORIES
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(25.dp)
                                .padding(horizontal = 5.dp)
                        )
                        Text(
                            "Edit Categories",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 5.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Insights Button
                    TextButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            startActivity(Intent(context, InsightsActivity::class.java))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart, // Use a pie chart icon
                            contentDescription = "Insights",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(25.dp)
                                .padding(horizontal = 5.dp)
                        )
                        Text(
                            "Insights",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 5.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // About Button
                    TextButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            startActivity(Intent(context, AboutActivity::class.java))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,  // You can use any suitable icon here
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(25.dp)
                                .padding(horizontal = 5.dp)
                        )
                        Text(
                            "About",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 5.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("ExpenseIQ", color = MaterialTheme.colorScheme.primary,style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))},
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showAddPaymentDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Payment")
                            }
                            IconButton(onClick = { navigateToPendingPayments() }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Pending Payments")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                    if (showPickerDialog) {
                        MonthYearPickerDialog(
                            initialMonthYear = selectedMonthYear,
                            onDismiss = { showPickerDialog = false },
                            onConfirm = { monthYear ->
                                selectedMonthYear = monthYear
                                refreshData(monthYear)
                            }
                        )
                    }

                    // Clickable Month-Year text
                    Box(
                        modifier = Modifier
                            .padding(vertical = 3.dp, horizontal = 16.dp)
                            .background(
                                Color(rgb(249, 249, 249)),
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 30.sp
                            ),
                            //color = MaterialTheme.colorScheme.onBackground,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(vertical = 8.dp, horizontal = 5.dp)
                                .clickable {
                                    showPickerDialog = true
                                } // Open month-year picker on click
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .background(Color(0xFFD3B7E7), shape = MaterialTheme.shapes.small)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = "Total Expenses This Month",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            //color = MaterialTheme.colorScheme.onBackground
                            color = Color.Black,
                        )
                        Text(
                            text = "\n ₹ $totalExpense",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 20.sp
                            ),
                            //color = MaterialTheme.colorScheme.onBackground
                            color = Color.Black,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black,
                                        Color.Black,
                                        Color.Transparent
                                    ),
                                    startX = 0f,
                                    endX = Float.POSITIVE_INFINITY
                                )
                            )
                            .align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = "Expense Breakdown",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp)
                    )

                    if (categoryTotals.isEmpty()) {
                        Text(
                            text = "\n\nNo Categories Found. \n Go to Menu >> Edit Categories to Add New Category ",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)) {
                            items(categoryTotals.toList()) { (category, total) ->
                                var isExpanded by remember { mutableStateOf(false) }

                                Column(modifier = Modifier.fillMaxWidth()) {

                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 3.dp, horizontal = 16.dp)
                                            .background(
                                                Color(rgb(242, 242, 242)),
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .padding(5.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isExpanded = !isExpanded }
                                                .padding(horizontal = 5.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = category,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 17.sp
                                                ),
                                                //color = MaterialTheme.colorScheme.onBackground
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "₹ $total",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 17.sp
                                                ),
                                                //color = MaterialTheme.colorScheme.onBackground
                                                color = Color.Black
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                                            transactionsMap[category]?.forEach { transaction ->
                                                var showMenu by remember { mutableStateOf(false) }
                                                var isEditing by remember { mutableStateOf(false) }
                                                var editAmount by remember {
                                                    mutableStateOf(
                                                        transaction.amount.toString()
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Date text on the left
                                                    Text(
                                                        text = transaction.date,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.align(Alignment.CenterVertically)
                                                    )

                                                    // Center Spacer for positioning
                                                    Spacer(modifier = Modifier.weight(1f))

                                                    // Amount in the center
                                                    Text(
                                                        text = "₹ ${transaction.amount}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.align(Alignment.CenterVertically),
                                                        textAlign = TextAlign.Center
                                                    )

                                                    // Right Spacer for positioning
                                                    Spacer(modifier = Modifier.weight(1f))

                                                    // 3-dot icon menu on the right
                                                    IconButton(
                                                        onClick = { showMenu = true },
                                                        modifier = Modifier.align(Alignment.CenterVertically)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.MoreVert,
                                                            contentDescription = "Options"
                                                        )
                                                    }

                                                    DropdownMenu(
                                                        expanded = showMenu,
                                                        onDismissRequest = { showMenu = false },
                                                        offset = DpOffset(
                                                            x = Dp.Infinity,
                                                            y = 0.dp
                                                        ) // Shifts menu to align with right side
                                                    ) {
                                                        DropdownMenuItem(
                                                            onClick = {
                                                                isEditing = true
                                                                showMenu = false
                                                            },
                                                            text = { Text("Edit") }
                                                        )
                                                        DropdownMenuItem(
                                                            onClick = {
                                                                lifecycleScope.launch {
                                                                    db.transactionDao()
                                                                        .deleteTransaction(
                                                                            transaction.id
                                                                        )
                                                                    refreshData()
                                                                }
                                                                showMenu = false
                                                            },
                                                            text = { Text("Delete") }
                                                        )
                                                    }
                                                }

                                                // Edit dialog
                                                if (isEditing) {
                                                    AlertDialog(
                                                        onDismissRequest = { isEditing = false },
                                                        title = { Text("Edit Transaction") },
                                                        text = {
                                                            TextField(
                                                                value = editAmount,
                                                                onValueChange = { editAmount = it },
                                                                label = { Text("Amount") },
                                                                keyboardOptions = KeyboardOptions(
                                                                    keyboardType = KeyboardType.Number
                                                                )
                                                            )
                                                        },
                                                        confirmButton = {
                                                            TextButton(onClick = {
                                                                val newAmount =
                                                                    editAmount.toFloatOrNull()
                                                                if (newAmount != null) {
                                                                    lifecycleScope.launch {
                                                                        db.transactionDao()
                                                                            .updateTransactionAmount(
                                                                                transaction.id,
                                                                                newAmount
                                                                            )
                                                                        refreshData()
                                                                    }
                                                                    isEditing = false
                                                                }
                                                            }) {
                                                                Text("Save")
                                                            }
                                                        },
                                                        dismissButton = {
                                                            TextButton(onClick = {
                                                                isEditing = false
                                                            }) {
                                                                Text("Cancel")
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }



                    if (showAddPaymentDialog) {
                        AddPaymentDialog(onDismiss = { showAddPaymentDialog = false }, categories, context, ::refreshData)
                    }
                }
            }
        }
    }

    @Composable
    fun MonthYearPickerDialog(
        initialMonthYear: String,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
    ) {
        val currentMonthYear = initialMonthYear.split("/")
        val initialYear = currentMonthYear[1].toInt()
        val initialMonth = currentMonthYear[0].toInt() - 1 // Convert to zero-based month

        var month by remember { mutableIntStateOf(initialMonth) }
        var year by remember { mutableIntStateOf(initialYear) } // Store year as Int

        // List of month names
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Month and Year") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Centered month picker with < and > buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                month = (month - 1 + monthNames.size) % monthNames.size // Wrap around to previous month
                            }
                        ) {
                            Text("<", style = MaterialTheme.typography.bodyLarge)
                        }

                        Text(
                            text = monthNames[month],
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = {
                                month = (month + 1) % monthNames.size // Wrap around to next month
                            }
                        ) {
                            Text(">", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    // Centered year picker with < and > buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        IconButton(
                            onClick = { year -= 1 } // Decrease year
                        ) {
                            Text("<", style = MaterialTheme.typography.bodyLarge)
                        }

                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = { year += 1 } // Increase year
                        ) {
                            Text(">", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val selectedMonth = month + 1 // Convert back to 1-based month
                    onConfirm(String.format("%02d/%d", selectedMonth, year))
                    onDismiss()
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun navigateToPendingPayments() {
        val intent = Intent(this, PendingPaymentsActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_PENDING_PAYMENTS)
    }

    /*companion object {
        private const val REQUEST_CODE_PENDING_PAYMENTS = 1
        private const val REQUEST_CODE_EDIT_CATEGORIES = 2 // Define a request code constant
    }*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PENDING_PAYMENTS || requestCode == REQUEST_CODE_EDIT_CATEGORIES) {
            refreshData() // Refresh data when returning from any of the activities
        }
    }

    @Composable
    fun AddPaymentDialog(
        onDismiss: () -> Unit,
        initialCategories: List<String>,
        context: Context,
        refreshData: () -> Unit // Add this parameter
    ) {
        var amount by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("") }
        var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) } // Initialize with current date
        var showCategoryDialog by remember { mutableStateOf(false) }
        var categories by remember { mutableStateOf(initialCategories) } // Use mutable state for categories


        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Expense") },
            text = {
                Column {
                    TextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                    )

                    // Button to open category selection dialog
                    Button(onClick = {
                        // Fetch categories when button is clicked
                        lifecycleScope.launch {
                            categories = db.categoryDao().getAllCategories().map { it.name }
                        }
                        showCategoryDialog = true // Show the dialog after fetching
                    }) {
                        Text(if (selectedCategory.isEmpty()) "Select Category" else selectedCategory)
                    }

                    // Button to open date picker dialog
                    Button(onClick = {
                        // Create a DatePickerDialog and show it
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                        }
                        val datePickerDialog = android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                // Set the selected date
                                val selectedTimeInMillis = Calendar.getInstance().apply {
                                    set(year, month, dayOfMonth)
                                }.timeInMillis
                                selectedDate = selectedTimeInMillis // Update selected date
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePickerDialog.show()
                    }) {
                        Text("Select Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (amount.isNotEmpty() && selectedCategory.isNotEmpty()) {
                        // Format the selected date to a string
                        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))

                        // Insert transaction into the database
                        lifecycleScope.launch {
                            val category = db.categoryDao().getCategoryByName(selectedCategory)
                            val categoryId = category?.id ?: return@launch // Exit if category is not found

                            val transaction = Transaction(
                                amount = amount.toFloat(),
                                categoryId = categoryId,
                                date = formattedDate // Use the formatted date as a String
                            )
                            db.transactionDao().insertTransaction(transaction)
                            refreshData() // Call refreshData here
                        }
                        onDismiss() // Close dialog after adding transaction
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

        // Category selection dialog
        if (showCategoryDialog) {
            CategorySelectionDialog(
                categories = categories,
                onSelectCategory = { category ->
                    selectedCategory = category
                    showCategoryDialog = false
                },
                onDismiss = { showCategoryDialog = false }
            )
        }
    }

    @Composable
    fun CategorySelectionDialog(
        categories: List<String>,
        onSelectCategory: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Category") },
            text = {
                LazyColumn {
                    items(categories) { category ->
                        TextButton(onClick = { onSelectCategory(category) }) {
                            Text(category)
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