package ba.unsa.etf.rma.projekat.dataetc

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(foreignKeys = [ForeignKey(entity = Biljka::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("idBiljke"),
    onDelete = ForeignKey.CASCADE)]
)
data class BiljkaBitmap (
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    @ColumnInfo(name = "idBiljke") val idBiljke: Long,
    @ColumnInfo(name = "bitmap") val bitmap: Bitmap
)