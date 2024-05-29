package ba.unsa.etf.rma.projekat.web

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ba.unsa.etf.rma.projekat.R
import ba.unsa.etf.rma.projekat.dataetc.Biljka
import ba.unsa.etf.rma.projekat.dataetc.KlimatskiTip
import ba.unsa.etf.rma.projekat.dataetc.Zemljiste
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class TrefleDAO(
    private val context: Context
) {
    suspend fun getImage(plant: Biljka): Bitmap {
        val defaultBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.picture1)

        val result = withContext(Dispatchers.IO) {
            PlantRepository.getPlantImage(plant.getLatinName())
        }

        return when(result){
            is GetImageResponse -> {
                if(result.data.isNotEmpty()){
                    result.data[0].link?.let {
                        getImageBitmap(it)
                    } ?: defaultBitmap
                } else defaultBitmap
            }

            else -> defaultBitmap
        }
    }

    private suspend fun getImageBitmap(urlText: String): Bitmap{
        return withContext(Dispatchers.IO){
            try {
                val url = URL(urlText)
                BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } catch (e: IOException) {
                println(e)
                BitmapFactory.decodeResource(context.resources, R.drawable.picture1)
            }
        }
    }

    private suspend fun getPlantId(latin: String): Int{
        val result = withContext(Dispatchers.IO){
            PlantRepository.getPlantId(latin)
        }

        val returnVal = when(result){
            is GetPlantIdResponse -> {
                if(result.data.isNotEmpty())
                    result.data[0].id
                else -1
            }

            else -> -1
        }

        return returnVal
    }

    suspend fun fixData(plant: Biljka): Biljka{
        val result = withContext(Dispatchers.IO){
            PlantRepository.getPlantData(getPlantId(plant.getLatinName()))
        }

        return when(result){
            is GetPlantDataResponse -> onDataSuccess(result.data, plant)
            else -> plant
        }
    }

    private fun onDataSuccess(plantData: PlantData, plant: Biljka): Biljka{
        var medicinskoUpozorenje = plant.medicinskoUpozorenje
        if(!plantData.edible && !plant.medicinskoUpozorenje.contains("NIJE JESTIVO"))
            medicinskoUpozorenje += " NIJE JESTIVO"
        if(plantData.specifications.toxicity != null && plantData.specifications.toxicity != "none" && !plant.medicinskoUpozorenje.contains("TOKSIČNO"))
            medicinskoUpozorenje += " TOKSIČNO"

        var zemljisniTipovi = plant.zemljisniTipovi
        if(plantData.growth.soilTexture != null){
            zemljisniTipovi =
                when(plantData.growth.soilTexture){
                    9 -> listOf(Zemljiste.SLJUNOVITO)
                    10 -> listOf(Zemljiste.KRECNJACKO)
                    1,2 -> listOf(Zemljiste.GLINENO)
                    3,4 -> listOf(Zemljiste.PJESKOVITO)
                    5,6 -> listOf(Zemljiste.ILOVACA)
                    7,8 -> listOf(Zemljiste.CRNICA)
                    else -> plant.zemljisniTipovi
                }
        }

        var klimatskiTipovi = plant.klimatskiTipovi
        val light = plantData.growth.light
        val atmosphericHumidity = plantData.growth.atmosphericHumidity
        if(light != null && atmosphericHumidity != null){
            klimatskiTipovi = when {
                light in 6..9 && atmosphericHumidity in 1..5 -> listOf(KlimatskiTip.SREDOZEMNA)
                light in 8..10 && atmosphericHumidity in 7..10 -> listOf(KlimatskiTip.TROPSKA)
                light in 6..9 && atmosphericHumidity in 5..8 -> listOf(KlimatskiTip.SUBTROPSKA)
                light in 4..7 && atmosphericHumidity in 3..7 -> listOf(KlimatskiTip.UMJERENA)
                light in 7..9 && atmosphericHumidity in 1..2 -> listOf(KlimatskiTip.SUHA)
                light in 0..5 && atmosphericHumidity in 3..7 -> listOf(KlimatskiTip.PLANINSKA)
                else -> plant.klimatskiTipovi
            }
        }

        return Biljka(
            naziv = plant.naziv,
            porodica = plantData.family ?: plant.porodica,
            medicinskoUpozorenje = medicinskoUpozorenje,
            medicinskeKoristi = plant.medicinskeKoristi,
            profilOkusa = plant.profilOkusa,
            jela = if(!plantData.edible) listOf() else plant.jela,
            klimatskiTipovi = klimatskiTipovi,
            zemljisniTipovi = zemljisniTipovi
            )
    }

    suspend fun getPlantsWithFlowerColor(flowerColor:String, substr:String): List<Biljka>{
        val result = withContext(Dispatchers.IO){
            PlantRepository.getPlantsByFlowerColor(flowerColor, substr)
        }

        return when(result){
            is GetByFlowerColorResponse -> withContext(Dispatchers.IO){
                onSearchSuccess(result.data, substr)
            }
            else -> listOf()
        }
    }

    private suspend fun onSearchSuccess(plantData: List<ShortPlantData>, substr: String): List<Biljka>{
        val newPlantData = plantData.filter { it.scientificName.contains(substr, ignoreCase = true) }

        val returnList = newPlantData.map { plant -> fixData(Biljka(
            naziv = (plant.name ?: "") + " (" + plant.scientificName + ")",
            porodica = "",
            medicinskoUpozorenje = "",
            medicinskeKoristi = listOf(),
            profilOkusa = null,
            jela = listOf(),
            klimatskiTipovi = listOf(),
            zemljisniTipovi = listOf()
        )) }.toMutableList()

        return returnList
    }
}