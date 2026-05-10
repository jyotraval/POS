package com.example.pos.data.dao

import androidx.room.*
import com.example.pos.data.entity.Item
import kotlinx.coroutines.flow.Flow
import com.example.pos.data.model.TopSellingItem


@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY isPinned DESC, name ASC")
    fun getItemsByCategory(categoryId: Long): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY isPinned DESC, name ASC")
    fun getAllItems(): Flow<List<Item>>

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    @Query("""
    SELECT 
        i.name AS itemName,
        SUM(ti.quantity) AS quantitySold,
        SUM(ti.quantity * i.price) AS revenue
    FROM items i
    INNER JOIN transaction_items ti ON i.id = ti.itemId
    GROUP BY i.id
    ORDER BY quantitySold DESC
    LIMIT :limit
""")
    fun getTopSellingItems(limit: Int): Flow<List<TopSellingItem>>




}