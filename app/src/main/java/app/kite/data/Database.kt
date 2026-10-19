package app.kite.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import app.kite.domain.Source
import app.kite.domain.Where
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One tug on the string: when, where the mind was, and whether the string was reeled all the way in.
 * A pull starts unreeled and is marked [reeled] when the kite touches the grass.
 */
@Entity(tableName = "pulls", indices = [Index("at")])
data class Pull(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long = System.currentTimeMillis(),
    val where: String = Where.HERE.name,
    val source: String = Source.TUG.name,
    val reeled: Boolean = false,
) {
    val whereOf: Where get() = Where.of(where)
    val sourceOf: Source get() = Source.of(source)
    fun day(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    /** Minutes after midnight, for placing the kite across the day's sky. */
    fun minute(zone: ZoneId = ZoneId.systemDefault()): Int = Instant.ofEpochMilli(at).atZone(zone).toLocalTime().let { it.hour * 60 + it.minute }
}

@Dao
interface PullDao {
    @Query("SELECT * FROM pulls ORDER BY at")
    fun observe(): Flow<List<Pull>>

    @Query("SELECT * FROM pulls ORDER BY at")
    suspend fun all(): List<Pull>

    @Query("SELECT * FROM pulls ORDER BY at DESC LIMIT 1")
    suspend fun last(): Pull?

    @Insert
    suspend fun insert(p: Pull): Long

    @Insert
    suspend fun insertAll(p: List<Pull>)

    @Query("UPDATE pulls SET reeled = 1 WHERE id = :id")
    suspend fun reeled(id: Long)

    @Query("DELETE FROM pulls")
    suspend fun clear()
}

@Database(entities = [Pull::class], version = 1, exportSchema = true)
abstract class KiteDatabase : RoomDatabase() {
    abstract fun dao(): PullDao

    companion object {
        fun create(context: Context): KiteDatabase =
            Room.databaseBuilder(context.applicationContext, KiteDatabase::class.java, "kite.db").build()
    }
}
