package com.example.expenseiq

import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.expenseiq.ui.theme.ExpenseIQTheme
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import java.util.Calendar
import com.github.mikephil.charting.formatter.ValueFormatter


@OptIn(ExperimentalMaterial3Api::class)
class InsightsActivity : ComponentActivity() {

    private lateinit var db: AppDatabase // Reference to the database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)

        setContent {
            MaterialTheme {
                InsightsScreen()
            }
        }
    }

    @Composable
    fun InsightsScreen() {
        var pieData by remember { mutableStateOf<List<PieEntry>>(emptyList()) }
        val months = listOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear downTo currentYear - 10).map { it.toString() } // Last 10 years
        var selectedMonth by remember { mutableStateOf(months[Calendar.getInstance().get(Calendar.MONTH)]) }
        var selectedYear by remember { mutableStateOf(currentYear.toString()) }
        val scope = rememberCoroutineScope()

        var averageDailyExpense by remember { mutableStateOf(0f) }
        var highestExpenseDay by remember { mutableStateOf<Pair<String, Float>?>(null) }
        var entireTotalExpense by remember { mutableFloatStateOf(0f) }

        // Fetch data whenever month or year is updated
        LaunchedEffect(selectedMonth, selectedYear) {
            scope.launch {
                pieData = fetchCategoryWiseExpenses("$selectedMonth/$selectedYear")
                averageDailyExpense = calculateAverageDailyExpense("$selectedMonth/$selectedYear")
                highestExpenseDay = fetchHighestExpenseDay("$selectedMonth/$selectedYear")
                entireTotalExpense =calculateEntireTotalExpense()

            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Insights",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            content = { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Month and Year Picker
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Month Dropdown
                        DropdownSelector(
                            label = "Month",
                            options = months,
                            selectedOption = selectedMonth,
                            onOptionSelected = { selectedMonth = it }
                        )

                        // Year Dropdown
                        DropdownSelector(
                            label = "Year",
                            options = years,
                            selectedOption = selectedYear,
                            onOptionSelected = { selectedYear = it }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {


                        item {

                            Text(
                                "PieChart Breakdown",
                                color = Color.Black,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            if (pieData.isEmpty()) {
                                Text(
                                    "No expenses data available for $selectedMonth/$selectedYear",
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                ExpensePieChart(pieData = pieData)
                            }

                            // Display Average Daily Expense
                            Text(
                                text = "Average Daily Expense",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(top = 40.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                                    .background(Color(0xFFD3B7E7), shape = MaterialTheme.shapes.small)
                                    .padding(5.dp)
                            ){
                            Text(
                                text = "₹${"%.2f".format(averageDailyExpense)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(vertical = 0.dp)
                            )}

                            // Display Highest Expense Day
                            highestExpenseDay?.let { (day, amount) ->
                                Text(
                                    text = "Highest Expense Day of the Month",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(top=10.dp, bottom = 1.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp, horizontal = 16.dp)
                                        .background(Color(0xFFD3B7E7), shape = MaterialTheme.shapes.small)
                                        .padding(5.dp)
                                ){
                                Text(
                                    text = "₹${"%.2f".format(amount)} ( $day )",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(top = 0.dp)
                                )}
                            } ?: Text(
                                text = "No expenses recorded for this month.",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Display Average Daily Expense
                            Text(
                                text = "All Time Expense",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                                    .background(Color(0xFFD3B7E7), shape = MaterialTheme.shapes.small)
                                    .padding(top=5.dp)
                            ){
                                Text(
                                    text = "₹${"%.2f".format(entireTotalExpense)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(top = 0.dp)
                            )}
                            Spacer(Modifier.padding(bottom=30.dp))
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun DropdownSelector(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("$label: $selectedOption")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun ExpensePieChart(pieData: List<PieEntry>) {
        AndroidView(
            factory = { context ->
                PieChart(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    setUsePercentValues(false) // Show actual values
                    setDrawEntryLabels(false) // Disable direct labels on slices to avoid clutter
                    description.isEnabled = false // Disable the description label
                    setDrawHoleEnabled(true) // Enable a hole in the center for styling

                    // Set extra offsets to add space around the chart
                    setExtraOffsets(0f, 0f, 0f, 10f) // Adds 30 dp space at the bottom

                    // Legend Configuration
                    legend.apply {
                        isEnabled = true
                        verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                        horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                        orientation = Legend.LegendOrientation.VERTICAL
                        setDrawInside(false)
                        textSize = 12f
                        form = Legend.LegendForm.CIRCLE // Color indicators are circular
                        formSize = 10f // Adjust the size of color indicators
                        xEntrySpace = 10f // Space between legend entries
                        yEntrySpace = 10f
                    }
                }
            },
            update = { pieChart ->
                // Configure the data set for the pie chart with colors
                val dataSet = PieDataSet(pieData, "").apply {
                    colors = listOf(
                        Color(0xFFE57373).toArgb(),
                        Color(0xFF81C784).toArgb(),
                        Color(0xFF64B5F6).toArgb(),
                        Color(0xFFFFD54F).toArgb(),
                        Color(0xFFBA68C8).toArgb()
                    )
                    valueTextColor = Color.Transparent.toArgb() // Hide values from slices
                    sliceSpace = 2f // Space between slices for visual clarity
                }

                // Format PieData to display values as category name + total in the legend
                val pieDataFormatted = PieData(dataSet).apply {
                    setValueFormatter(object : ValueFormatter() {
                        override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                            return "" // Hide slice labels
                        }

                        override fun getFormattedValue(value: Float): String {
                            return "" // Keep the formatted value empty to hide slice labels
                        }
                    })
                }

                // Set the formatted data to the pie chart
                pieChart.data = pieDataFormatted

                // Create custom legend entries with formatted values
                val legendEntries = pieData.map { entry ->
                    LegendEntry("${entry.label}: ${"%.2f".format(entry.value)}", Legend.LegendForm.CIRCLE, 10f, 2f, null, dataSet.colors[pieData.indexOf(entry)])
                }

                // Set the custom legend entries using setCustom()
                pieChart.legend.setCustom(legendEntries)

                pieChart.invalidate() // Refresh chart with new data
                pieChart.animateY(800)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
        )
    }


    private suspend fun fetchCategoryWiseExpenses(currentMonthYear: String): List<PieEntry> {
        val categoryData = db.categoryDao().getAllCategories()
        val pieEntries = categoryData.mapNotNull { category ->
            val totalByCategory = db.transactionDao().getTotalExpenseByCategoryAndMonth(
                categoryId = category.id,
                currentMonthYear = currentMonthYear
            )
            if (totalByCategory > 0) {
                PieEntry(totalByCategory, category.name) // Create a PieEntry for each category with total
            } else {
                null
            }
        }
        return pieEntries // Return the list of PieEntry
    }

    private suspend fun calculateAverageDailyExpense(monthYear: String): Float {
        val totalExpenses = db.transactionDao().getTotalExpensesForMonth(monthYear)
        val daysInMonth = db.transactionDao().getDaysInMonth(monthYear) // A new function to get days in month
        return if (daysInMonth > 0) totalExpenses / daysInMonth else 0f
    }

    private suspend fun fetchHighestExpenseDay(monthYear: String): Pair<String, Float>? {
        val highestAmount = db.transactionDao().getHighestExpenseAmount(monthYear)
        val highestDate = db.transactionDao().getHighestExpenseDate(monthYear)
        return Pair(highestDate, highestAmount)    }

    private suspend fun calculateEntireTotalExpense(): Float {
        val entireTotalExpenses = db.transactionDao().getEntireTotalExpenses()
        return entireTotalExpenses
    }
}
