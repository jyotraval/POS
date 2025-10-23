package com.example.pos.data.dao

import androidx.room.*
import androidx.room.Transaction as RoomTransaction
import com.example.pos.data.entity.Transaction
import com.example.pos.data.entity.TransactionItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.util.Date

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE dateTime BETWEEN :startDate AND :endDate ORDER BY dateTime DESC")
    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE buyerName LIKE '%' || :query || '%' OR buyerPhone LIKE '%' || :query || '%' OR txnId LIKE '%' || :query || '%'")
    fun searchTransactions(query: String): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @RoomTransaction
    suspend fun insertTransactionWithItems(transaction: Transaction, items: List<TransactionItem>): Long {
        val transactionId = insert(transaction)
        val itemsWithTransactionId = items.map { it.copy(transactionId = transactionId) }
        insertTransactionItems(itemsWithTransactionId)
        return transactionId
    }

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getTransactionItems(transactionId: Long): Flow<List<TransactionItem>>

    @Query("""
        SELECT SUM(total) FROM transactions 
        WHERE dateTime BETWEEN :startDate AND :endDate
    """)
    fun getTotalSalesBetweenDates(startDate: Date, endDate: Date): Flow<Double?>

    @Query("""
        SELECT * FROM transactions 
        WHERE dateTime >= :startOfDay AND dateTime < :endOfDay
        ORDER BY dateTime DESC
    """)
    fun getTransactionsForDate(startOfDay: Date, endOfDay: Date): Flow<List<Transaction>>
}