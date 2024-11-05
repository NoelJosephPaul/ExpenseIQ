package com.example.expenseiq

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query



@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    @Query("SELECT * FROM categories WHERE name = :categoryName LIMIT 1")
    suspend fun getCategoryByName(categoryName: String): Category?

    @Query("DELETE FROM categories WHERE name = :category")
    suspend fun deleteCategory(category: String)
}


@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions")
    suspend fun getTotalExpenses(): Float? // Return nullable Float to handle cases with no transactions

    @Query("SELECT SUM(amount) FROM transactions WHERE SUBSTR(date, 4,7) = :currentMonthYear")
    suspend fun getCurrentMonthTotalExpenses(currentMonthYear: String): Float

    @Query("DELETE FROM transactions WHERE categoryId = :categoryId")
    suspend fun deleteTransactionsByCategoryId(categoryId: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND SUBSTR(date, 4,7) = :currentMonthYear")
    suspend fun getTotalExpenseByCategoryAndMonth(categoryId: Long, currentMonthYear: String): Float

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("UPDATE transactions SET amount = :amount WHERE id = :id")
    suspend fun updateTransactionAmount(id: Long, amount: Float)

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND SUBSTR(date, 4,7) = :currentMonthYear ORDER BY date DESC")
    suspend fun getTransactionsByCategoryAndMonth(categoryId: Long, currentMonthYear: String): List<Transaction>



    @Query("SELECT SUM(amount) FROM transactions WHERE SUBSTR(date, 4,7) = :monthYear")
    suspend fun getTotalExpensesForMonth(monthYear: String): Float

    @Query("SELECT COUNT(DISTINCT date) FROM transactions WHERE SUBSTR(date, 4,7) = :monthYear")
    suspend fun getDaysInMonth(monthYear: String): Int

    @Query("SELECT SUM(amount) AS total FROM transactions WHERE SUBSTR(date, 4, 7) = :monthYear GROUP BY date ORDER BY total DESC LIMIT 1")
    suspend fun getHighestExpenseAmount(monthYear: String): Float

    @Query("SELECT date FROM transactions WHERE SUBSTR(date, 4, 7) = :monthYear GROUP BY date ORDER BY SUM(amount) DESC LIMIT 1")
    suspend fun getHighestExpenseDate(monthYear: String): String

    @Query("SELECT SUM(amount) FROM transactions")
    suspend fun getEntireTotalExpenses(): Float

}



@Dao
interface PendingPaymentDao {
    @Insert
    suspend fun insertPendingPayment(pendingPayment: PendingPayment)

    @Query("SELECT * FROM pending_payments ORDER BY date DESC")
    suspend fun getAllPendingPayments(): List<PendingPayment>

    @Query("DELETE FROM pending_payments WHERE id = :paymentId")
    suspend fun deletePendingPayment(paymentId: Long)
}