package com.example.expenseiq


import com.example.expenseiq.AppDatabase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch



class ExpenseViewModel(private val database: AppDatabase) : ViewModel() {

    fun addCategory(category: Category) {
        viewModelScope.launch {
            database.categoryDao().insertCategory(category)
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            database.transactionDao().insertTransaction(transaction)
        }
    }

    suspend fun getAllCategories(): List<Category> {
        return database.categoryDao().getAllCategories()
    }

    suspend fun getAllTransactions(): List<Transaction> {
        return database.transactionDao().getAllTransactions()
    }
}