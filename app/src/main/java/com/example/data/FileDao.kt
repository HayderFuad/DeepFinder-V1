package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(file: IndexedFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<IndexedFile>)

    @Query("DELETE FROM indexed_files WHERE filePath = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM indexed_files")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM indexed_files")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM indexed_files")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM indexed_files WHERE isContentIndexed = 1")
    suspend fun getContentIndexedCount(): Int

    @Query("SELECT * FROM indexed_files WHERE filePath = :path LIMIT 1")
    suspend fun getByPath(path: String): IndexedFile?

    @Query("""
        SELECT * FROM indexed_files 
        WHERE normalizedFileName LIKE '%' || :query || '%' 
           OR normalizedContent LIKE '%' || :query || '%'
        ORDER BY isDirectory DESC, lastModified DESC 
        LIMIT 200
    """)
    suspend fun searchAll(query: String): List<IndexedFile>

    @Query("""
        SELECT * FROM indexed_files 
        WHERE normalizedFileName LIKE '%' || :query || '%'
        ORDER BY isDirectory DESC, lastModified DESC 
        LIMIT 200
    """)
    suspend fun searchByNameOnly(query: String): List<IndexedFile>

    @Query("""
        SELECT * FROM indexed_files 
        WHERE normalizedContent LIKE '%' || :query || '%'
        ORDER BY lastModified DESC 
        LIMIT 200
    """)
    suspend fun searchByContentOnly(query: String): List<IndexedFile>

    @Query("""
        SELECT * FROM indexed_files 
        WHERE (normalizedFileName LIKE '%' || :query || '%' OR normalizedContent LIKE '%' || :query || '%')
          AND extension IN (:extensions)
        ORDER BY lastModified DESC 
        LIMIT 200
    """)
    suspend fun searchByExtensions(query: String, extensions: List<String>): List<IndexedFile>

    @Query("""
        SELECT * FROM indexed_files 
        WHERE isDirectory = 1 AND normalizedFileName LIKE '%' || :query || '%'
        ORDER BY lastModified DESC 
        LIMIT 200
    """)
    suspend fun searchDirectories(query: String): List<IndexedFile>

    @Query("SELECT * FROM indexed_files ORDER BY lastModified DESC LIMIT 50")
    suspend fun getRecentFiles(): List<IndexedFile>
}
