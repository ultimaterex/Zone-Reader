package com.serubii.zonereader.helpers


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.serubii.zonereader.SerubiiApp
import java.io.*

/**
 * This class provides boilerplate image processing methods for working with Firebase and CameraX
 *
 * @author Rex Serubii
 */
class FireXToolkit {

    private val TAG = this.javaClass.simpleName

    private lateinit var currentPhotoPath: String

    /** Internal Method that creates a temporary file with a randomized timestamp.
     *
     * this abuses the shared property currentPhotoPath
     *
     * @param context Context
     * @param name String
     * @param path String
     * @return File
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun createTemporaryImageFile(context: Context, name: String, path: String): File {
        // Create an image file name
        val storageDir: File = context.getExternalFilesDir(path)!!
        return File.createTempFile(
                name, /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
            Log.wtf(TAG, "Saved to $currentPhotoPath with $name")
        }


    }


    /** Method that saves a bitmap to a temporary [File] and return it's [Uri]
     *
     * @param bitmap Bitmap
     * @param name String
     * @param context Context
     * @param path String
     * @return Uri
     */
    fun saveBitmapToTemporaryFile(
            bitmap: Bitmap,
            name: String,
            context: Context = SerubiiApp.applicationContext(),
            path: String
    ): Uri {
        // Create the File where the photo should go
        val photoFile: File? = try {
            createTemporaryImageFile(context, name, path)
        } catch (ex: IOException) {
            // Error occurred while creating the File
            ex.printStackTrace()
            null
        }

        val os: OutputStream = BufferedOutputStream(FileOutputStream(photoFile))
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        os.flush()
        os.close()

        lateinit var photoURI: Uri
        // Continue only if the File was successfully created
        photoFile?.also {
            photoURI = FileProvider.getUriForFile(
                    context,
                    authority,
                    it
            )
        }
        return photoURI
    }

    /** Method that rotates a [Bitmap] to [angle] degrees
     *
     * @param source Bitmap
     * @param angle Int
     * @return Bitmap
     */
    fun rotateBitmap(source: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
        )
    }

    /** Method that recursively deletes all files inside of a folder
     *
     * @param context Context
     * @param path String
     */
    fun deleteEnv(context: Context, path: String) {
        val dir =
                File(context.getExternalFilesDir(path).toString())
        try {
            if (dir.isDirectory) {
                val children = dir.list()
                for (i in children.indices) {
                    File(dir, children[i]).delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Internal Method that handles bitmap flipping
     *
     * @receiver Bitmap
     * @return Bitmap
     */
    private fun Bitmap.flip(): Bitmap {
        val matrix = Matrix().apply { postScale(-1f, 1f, width / 2f, width / 2f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

}