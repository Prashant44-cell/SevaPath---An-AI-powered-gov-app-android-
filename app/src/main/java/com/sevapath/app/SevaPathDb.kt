package com.sevapath.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import androidx.compose.ui.graphics.Color
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SevaPathDb(context: Context) : SQLiteOpenHelper(context, "sevapath.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                email TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                salt TEXT NOT NULL,
                password_hash TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE requests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL,
                title TEXT NOT NULL,
                location TEXT NOT NULL,
                category TEXT NOT NULL,
                status TEXT NOT NULL,
                evidence TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE news (
                id TEXT PRIMARY KEY NOT NULL,
                email TEXT NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                type TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                likes INTEGER NOT NULL,
                liked_by_me INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema changes must use explicit migrations; user data must not be dropped.
    }

    fun userExists(email: String): Boolean = readableDatabase.query(
        "users", arrayOf("email"), "email = ?", arrayOf(email), null, null, null, "1"
    ).use { it.moveToFirst() }

    fun userName(email: String): String? = readableDatabase.query(
        "users", arrayOf("name"), "email = ?", arrayOf(email), null, null, null, "1"
    ).use { if (it.moveToFirst()) it.getString(0) else null }

    fun createUser(name: String, email: String, password: String): Boolean {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val values = ContentValues().apply {
            put("email", email)
            put("name", name)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("password_hash", Base64.encodeToString(hashPassword(password, salt), Base64.NO_WRAP))
        }
        return writableDatabase.insert("users", null, values) != -1L
    }

    fun authenticate(email: String, password: String): String? {
        return readableDatabase.query(
            "users", arrayOf("name", "salt", "password_hash"), "email = ?", arrayOf(email), null, null, null, "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val salt = Base64.decode(cursor.getString(1), Base64.NO_WRAP)
            val expected = Base64.decode(cursor.getString(2), Base64.NO_WRAP)
            val actual = hashPassword(password, salt)
            if (MessageDigest.isEqual(expected, actual)) cursor.getString(0) else null
        }
    }

    fun loadRequests(email: String): List<CitizenRequest> = readableDatabase.query(
        "requests", arrayOf("title", "location", "category", "status", "evidence"),
        "email = ?", arrayOf(email), null, null, "id DESC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val status = cursor.getString(3)
                add(CitizenRequest(cursor.getString(0), cursor.getString(1), cursor.getString(2), status, statusColor(status), cursor.getString(4)))
            }
        }
    }

    fun saveRequests(email: String, requests: List<CitizenRequest>) {
        writableDatabase.inTransaction {
            delete("requests", "email = ?", arrayOf(email))
            requests.forEach { request ->
                insert("requests", null, ContentValues().apply {
                    put("email", email)
                    put("title", request.title)
                    put("location", request.location)
                    put("category", request.category)
                    put("status", request.status)
                    put("evidence", request.evidence)
                })
            }
        }
    }

    fun loadNews(email: String): List<NewsItem> = readableDatabase.query(
        "news", arrayOf("id", "title", "body", "type", "timestamp", "likes", "liked_by_me"),
        "email = ?", arrayOf(email), null, null, "rowid DESC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(NewsItem(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getInt(6) == 1))
            }
        }
    }

    fun saveNews(email: String, news: List<NewsItem>) {
        writableDatabase.inTransaction {
            delete("news", "email = ?", arrayOf(email))
            news.forEach { item ->
                insert("news", null, ContentValues().apply {
                    put("id", item.id)
                    put("email", email)
                    put("title", item.title)
                    put("body", item.body)
                    put("type", item.type)
                    put("timestamp", item.timestamp)
                    put("likes", item.likes)
                    put("liked_by_me", if (item.likedByMe) 1 else 0)
                })
            }
        }
    }

    private fun SQLiteDatabase.inTransaction(action: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            action()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray = SecretKeyFactory
        .getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(password.toCharArray(), salt, 120_000, 256))
        .encoded

    private fun statusColor(status: String): Color = when (status) {
        "Under review" -> Color(0xFFFFC107)
        "Verified", "Received" -> Color(0xFF004D40)
        else -> Color(0xFFFF5722)
    }
}
