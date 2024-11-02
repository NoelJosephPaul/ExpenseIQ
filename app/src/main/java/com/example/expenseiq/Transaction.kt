package com.example.expenseiq

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import org.w3c.dom.Text
import java.sql.Date

/*@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE // Adjust behavior on delete as necessary
        )
    ]
)*/

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Float,
    val categoryId: Long, // Foreign key referencing the Category
    val date: String // If you want to store the date as a timestamp
)
