package com.example.expensetracker

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val category: String,
    val date: Long, // LocalDate.toEpochDay()
    val note: String = ""
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun all(): Flow<List<Expense>>
    @Insert suspend fun insert(e: Expense)
    @Update suspend fun update(e: Expense)
    @Delete suspend fun delete(e: Expense)
}

@Database(entities = [Expense::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): ExpenseDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "expenses.db")
                .build().also { inst = it }
        }
    }
}
