package com.entech.inventory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class InventoryItem(
    val id: Long,
    val name: String,
    val partNumber: String,
    val category: String,
    val description: String,
    val quantity: Int,
    val minimum: Int,
    val location: String,
    val supplier: String
)

data class StockMovement(
    val itemName: String,
    val type: String,
    val quantity: Int,
    val date: String
)

class InventoryDatabase(context: Context) :
    SQLiteOpenHelper(context, "entech_inventory.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                part_number TEXT NOT NULL UNIQUE,
                category TEXT NOT NULL,
                description TEXT,
                quantity INTEGER NOT NULL CHECK(quantity >= 0),
                minimum INTEGER NOT NULL CHECK(minimum >= 0),
                location TEXT,
                supplier TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id INTEGER NOT NULL,
                item_name TEXT NOT NULL,
                type TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                date TEXT NOT NULL
            )
        """.trimIndent())

        val samples = listOf(
            arrayOf("Screw","egh7003","Hardware","Standard steel screw","200","0","Bin A-1","The Shop"),
            arrayOf("Bolt","egh7004","Hardware","Heavy duty bolt","300","0","Bin A-2","The Shop")
        )
        samples.forEach { row ->
            val values = ContentValues().apply {
                put("name",row[0]); put("part_number",row[1]); put("category",row[2])
                put("description",row[3]); put("quantity",row[4].toInt()); put("minimum",row[5].toInt())
                put("location",row[6]); put("supplier",row[7])
            }
            db.insert("items",null,values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Database version 1. Add migration logic here if the schema changes.
    }

    fun getItems(search: String = ""): List<InventoryItem> {
        val result = mutableListOf<InventoryItem>()
        val db = readableDatabase
        val cursor = if (search.isBlank()) {
            db.rawQuery("SELECT * FROM items ORDER BY name COLLATE NOCASE", null)
        } else {
            db.rawQuery(
                "SELECT * FROM items WHERE name LIKE ? OR part_number LIKE ? OR supplier LIKE ? ORDER BY name COLLATE NOCASE",
                arrayOf("%$search%","%$search%","%$search%")
            )
        }
        cursor.use {
            while (it.moveToNext()) {
                result.add(InventoryItem(
                    it.getLong(it.getColumnIndexOrThrow("id")),
                    it.getString(it.getColumnIndexOrThrow("name")),
                    it.getString(it.getColumnIndexOrThrow("part_number")),
                    it.getString(it.getColumnIndexOrThrow("category")),
                    it.getString(it.getColumnIndexOrThrow("description")) ?: "",
                    it.getInt(it.getColumnIndexOrThrow("quantity")),
                    it.getInt(it.getColumnIndexOrThrow("minimum")),
                    it.getString(it.getColumnIndexOrThrow("location")) ?: "",
                    it.getString(it.getColumnIndexOrThrow("supplier")) ?: ""
                ))
            }
        }
        return result
    }

    fun saveItem(item: InventoryItem): Boolean {
        val values = ContentValues().apply {
            put("name",item.name); put("part_number",item.partNumber); put("category",item.category)
            put("description",item.description); put("quantity",item.quantity); put("minimum",item.minimum)
            put("location",item.location); put("supplier",item.supplier)
        }
        return try {
            if (item.id == 0L) writableDatabase.insertOrThrow("items",null,values) != -1L
            else writableDatabase.update("items",values,"id=?",arrayOf(item.id.toString())) > 0
        } catch (_: Exception) { false }
    }

    fun deleteItem(id: Long) {
        writableDatabase.delete("items","id=?",arrayOf(id.toString()))
    }

    fun recordMovement(item: InventoryItem, type: String, amount: Int): Boolean {
        if (amount <= 0) return false
        val newQuantity = if (type == "received") item.quantity + amount else item.quantity - amount
        if (newQuantity < 0) return false
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val update = ContentValues().apply { put("quantity",newQuantity) }
            db.update("items",update,"id=?",arrayOf(item.id.toString()))
            val movement = ContentValues().apply {
                put("item_id",item.id); put("item_name",item.name); put("type",type)
                put("quantity",amount); put("date",System.currentTimeMillis().toString())
            }
            db.insertOrThrow("movements",null,movement)
            db.setTransactionSuccessful()
            true
        } catch (_: Exception) { false }
        finally { db.endTransaction() }
    }

    fun getMovements(type: String? = null): List<StockMovement> {
        val result = mutableListOf<StockMovement>()
        val cursor = if (type == null) {
            readableDatabase.rawQuery("SELECT * FROM movements ORDER BY id DESC LIMIT 30",null)
        } else {
            readableDatabase.rawQuery("SELECT * FROM movements WHERE type=? ORDER BY id DESC LIMIT 30",arrayOf(type))
        }
        cursor.use {
            while(it.moveToNext()) {
                val millis = it.getString(it.getColumnIndexOrThrow("date")).toLongOrNull() ?: 0L
                val date = java.text.SimpleDateFormat("MMM d, yyyy",java.util.Locale.getDefault()).format(java.util.Date(millis))
                result.add(StockMovement(
                    it.getString(it.getColumnIndexOrThrow("item_name")),
                    it.getString(it.getColumnIndexOrThrow("type")),
                    it.getInt(it.getColumnIndexOrThrow("quantity")),
                    date
                ))
            }
        }
        return result
    }
}
