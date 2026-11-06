package validation

import com.badlogic.gdx.graphics.glutils.FileTextureData

object ReflectionFieldContract {
    @JvmStatic
    fun main(args: Array<String>) {
        val data = FileTextureData(null, null, null, false)
        val pixmap = FileTextureData::class.java.getDeclaredField("pixmap")
        check(pixmap.name == "pixmap")
        pixmap.isAccessible = true
        check(pixmap.get(data) == null)
        pixmap.set(data, null)
        check(pixmap.get(data) == null)
        println("Unciv FileTextureData.pixmap reflection contract passed")
    }
}
