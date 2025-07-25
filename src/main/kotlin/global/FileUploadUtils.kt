package com.marlow.global

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import java.io.File
import java.util.UUID

object FileUploadUtils {
    fun saveFile(part: PartData.FileItem, folder: String = "image_uploads"): String {
        val fileName   = part.originalFileName ?: UUID.randomUUID().toString()
        val uploadPath = File(folder)

        if (!uploadPath.exists()) {
            uploadPath.mkdirs()
        }

        val savedFile = File(uploadPath, fileName)
        part.streamProvider().use { input ->
            savedFile.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }

        return "/$folder/$fileName"
    }
}