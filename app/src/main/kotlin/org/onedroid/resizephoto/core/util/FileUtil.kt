package org.onedroid.resizephoto.core.util

import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.apply
import kotlin.jvm.Throws
import kotlin.text.lastIndexOf
import kotlin.text.padEnd
import kotlin.text.substring

object FileUtil {
    private const val EOF = -1
    private const val DEFAULT_BUFFER_SIZE = 1024 * 4

    @Throws(IOException::class)
    fun from(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = getFileName(context, uri)
        val splitName = splitFileName(fileName)

        var prefix = splitName[0]
        if (prefix.length < 3) {
            prefix = prefix.padEnd(3, '_')
        }
        var tempFile = File.createTempFile(prefix, splitName[1])
        tempFile = rename(tempFile, fileName)
        tempFile.deleteOnExit()
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(tempFile)
        } catch (_: FileNotFoundException) {
        }
        return if (out != null) {
            copy(inputStream, out)
            inputStream.close()
            out.close()
            tempFile
        } else {
            inputStream.close()
            null
        }
    }

    private fun splitFileName(fileName: String): Array<String> {
        var name = fileName
        var extension = ""
        val i = fileName.lastIndexOf(".")
        if (i != -1) {
            name = fileName.substring(0, i)
            extension = fileName.substring(i)
        }
        return arrayOf(name, extension)
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if ("content" == uri.scheme) {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } catch (_: Exception) {
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            val path = uri.path ?: return "tempfile"
            val cut = path.lastIndexOf(File.separatorChar)
            result = if (cut != -1) path.substring(cut + 1) else path
        }
        return result
    }

    private fun rename(file: File, newName: String): File {
        val newFile = File(file.parent, newName)
        if (!newFile.equals(file)) {
            if (newFile.exists()) {
                newFile.delete()
            }
            file.renameTo(newFile)
        }
        return newFile
    }

    @Throws(IOException::class)
    private fun copy(input: InputStream, output: OutputStream): Long {
        var count = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val n = input.read(buffer)
            if (n == EOF) break
            output.write(buffer, 0, n)
            count += n
        }
        return count
    }

    fun getImageResolution(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return Pair(options.outWidth, options.outHeight)
    }
}
