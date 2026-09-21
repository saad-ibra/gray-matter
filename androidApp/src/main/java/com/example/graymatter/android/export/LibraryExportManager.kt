package com.example.graymatter.android.export

import android.content.Context
import android.net.Uri
import com.example.graymatter.domain.business.ExportService
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.ResourceEntryRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceEntryWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LibraryExportManager(
    private val topicRepository: TopicRepository,
    private val resourceRepository: ResourceRepository,
    private val resourceEntryRepository: ResourceEntryRepository,
    private val opinionRepository: OpinionRepository
) {

    suspend fun exportLibraryToZip(context: Context, outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val outputStream = contentResolver.openOutputStream(outputUri)
                ?: return@withContext Result.failure(Exception("Could not open output stream for URI"))

            ZipOutputStream(outputStream).use { zipOut ->
                
                // 1. Export Topics
                val topics = topicRepository.getAllTopics()
                for (topic in topics) {
                    val entries = resourceEntryRepository.getResourceEntriesByTopicId(topic.id).first()
                    val detailsList = entries.mapNotNull { entry ->
                        val res = resourceRepository.getResourceById(entry.resourceId) ?: return@mapNotNull null
                        val ops = opinionRepository.getOpinionsByItemId(entry.id).first()
                        ResourceEntryWithDetails(entry, res, ops)
                    }
                    
                    val markdown = ExportService.exportTopicSummary(topic, detailsList)
                    val safeTopicName = safeFileName(topic.name ?: "Untitled_Topic_${topic.id}")
                    
                    zipOut.putNextEntry(ZipEntry("Topics/$safeTopicName.md"))
                    zipOut.write(markdown.toByteArray())
                    zipOut.closeEntry()
                }

                // 2. Export Resources & Assets
                val resources = resourceRepository.resourcesStream.first()
                val imageAssetsToCopy = mutableSetOf<String>()

                for (resource in resources) {
                    val entry = resourceEntryRepository.getResourceEntryByResourceId(resource.id)
                    if (entry != null) {
                        val allOpinions = opinionRepository.getOpinionsByItemId(entry.id).first()
                        val details = ResourceEntryWithDetails(entry, resource, allOpinions)
                        var markdown = ExportService.exportResourceHistory(details)
                        
                        allOpinions.forEach { op ->
                            if (op.imagePath != null) {
                                val file = File(op.imagePath)
                                if (file.exists()) {
                                    val fileName = file.name
                                    imageAssetsToCopy.add(op.imagePath!!)
                                    markdown += "\n\n![[Assets/$fileName]]\n"
                                }
                            }
                        }

                        val safeResName = safeFileName(resource.title ?: "Untitled_Resource_${resource.id}")
                        zipOut.putNextEntry(ZipEntry("Resources/$safeResName.md"))
                        zipOut.write(markdown.toByteArray())
                        zipOut.closeEntry()
                    }
                }

                // 3. Copy Assets
                for (imagePath in imageAssetsToCopy) {
                    val file = File(imagePath)
                    if (file.exists()) {
                        zipOut.putNextEntry(ZipEntry("Assets/${file.name}"))
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun safeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}
