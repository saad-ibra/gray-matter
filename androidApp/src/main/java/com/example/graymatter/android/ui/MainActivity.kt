package com.example.graymatter.android.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.example.graymatter.android.GrayMatterApplication
import com.example.graymatter.android.preferences.AppPreferences
import com.example.graymatter.android.preferences.AppTheme
import com.example.graymatter.android.security.BiometricAuthManager
import com.example.graymatter.android.ui.screens.BiometricLockScreen
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import org.koin.android.ext.android.inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.graymatter.data.TopicRepository
import com.example.graymatter.data.ResourceEntryRepository
import com.example.graymatter.data.ResourceRepository
import com.example.graymatter.data.OpinionRepository
import com.example.graymatter.domain.ResourceType
import com.example.graymatter.domain.Topic
import com.example.graymatter.domain.Resource
import com.example.graymatter.domain.ResourceEntry
import com.example.graymatter.domain.Opinion
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * Main Activity for Gray Matter app.
 *
 * Security measures applied here:
 * - FLAG_SECURE: prevents screenshots, screen recordings, and recent-apps previews
 * - Biometric gate: requires hardware-verified authentication before showing content
 */
class MainActivity : FragmentActivity() {

    private val biometricAuthManager = BiometricAuthManager()
    private lateinit var securityPreferences: com.example.graymatter.android.security.SecurityPreferences
    private lateinit var appPreferences: AppPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)
        
        securityPreferences = com.example.graymatter.android.security.SecurityPreferences(this)
        appPreferences = AppPreferences.getInstance(this)
        seedDummyData()

        // Prevent screenshots, screen recordings, and recent-apps preview if enabled.
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        
        // Check biometric availability (auto-unlocks if not available)
        biometricAuthManager.checkAvailability(this)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            // Observe theme preference reactively
            val themeChoice by appPreferences.themeState.collectAsState()
            val darkTheme = when (themeChoice) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            
            // Observe keep-screen-awake preference reactively
            val keepAwake by appPreferences.keepScreenAwakeState.collectAsState()
            if (keepAwake) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            GrayMatterTheme(darkTheme = darkTheme) {
                GrayMatterApp()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Optional: re-lock when app goes to background for extended time
        // Uncomment the line below for stricter security (re-lock on every pause)
        // biometricAuthManager.lock()
    }

    private fun seedDummyData() {
        val topicRepository: TopicRepository by inject()
        val resourceEntryRepository: ResourceEntryRepository by inject()
        val opinionRepository: OpinionRepository by inject()
        val autoLinkService: com.example.graymatter.domain.business.AutoLinkService by inject()
        
        lifecycleScope.launch {
            try {
                // Only seed if empty
                val existingTopics = topicRepository.topicsStream.first()
                if (existingTopics.isNotEmpty()) return@launch
                
                val now = Clock.System.now().toEpochMilliseconds()
                
                // Helper to create topic
                suspend fun createTopic(name: String, notes: String): String {
                    val id = UUID.randomUUID().toString()
                    topicRepository.saveTopic(Topic(id = id, name = name, notes = notes, resourceCount = 0, updatedAt = now))
                    return id
                }
                
                // Helper to create resource entry with opinion
                suspend fun createEntry(
                    topicId: String,
                    title: String,
                    description: String,
                    resourceType: String,
                    url: String?,
                    filePath: String?,
                    extractedText: String?,
                    opinionText: String,
                    confidence: Int
                ) {
                    val entryId = UUID.randomUUID().toString()
                    val resId = UUID.randomUUID().toString()
                    val opId = UUID.randomUUID().toString()
                    resourceEntryRepository.createResourceEntryWithDetails(
                        resourceEntryId = entryId,
                        resourceId = resId,
                        resourceType = resourceType,
                        url = url,
                        filePath = filePath,
                        extractedText = extractedText,
                        title = title,
                        description = description,
                        opinionId = opId,
                        opinionText = opinionText,
                        confidence = confidence,
                        now = now
                    )
                    resourceEntryRepository.updateResourceEntryTopic(entryId, topicId)
                    topicRepository.incrementResourceCount(topicId)
                    // Create opinion record
                    opinionRepository.saveOpinion(
                        Opinion(
                            id = opId,
                            itemId = entryId,
                            text = opinionText,
                            confidenceScore = confidence,
                            imagePath = null,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    // Sync backlinks
                    autoLinkService.syncLinks(opId, com.example.graymatter.domain.ReferenceType.OPINION, opinionText, emptyList())
                }
                
                // Topics
                val aiTopicId = createTopic("Artificial Intelligence", "Exploring large models, scaling laws, and emergent capabilities.")
                val psychTopicId = createTopic("Cognitive Psychology", "Memory limits, attention, and mental models.")
                val pkmTopicId = createTopic("Knowledge Management", "Deliberate reflection, visual notes, and relational databases.")
                val devTopicId = createTopic("Software Development", "Modern mobile, backend, and devops practices.")
                
                // Entries – expand to many diverse items
                createEntry(
                    topicId = aiTopicId,
                    title = "Language Models are Few-Shot Learners",
                    description = "Foundational GPT‑3 paper on scaling and few‑shot prompting.",
                    resourceType = ResourceType.WEB_LINK.name,
                    url = "https://arxiv.org/abs/2005.14165",
                    filePath = null,
                    extractedText = null,
                    opinionText = "Scale delivers performance, but true understanding likely needs symbolic reasoning. [[Artificial Intelligence]]",
                    confidence = 80
                )
                createEntry(
                    topicId = aiTopicId,
                    title = "Transformers: Attention Is All You Need",
                    description = "The original transformer architecture that revolutionized NLP.",
                    resourceType = ResourceType.WEB_LINK.name,
                    url = "https://arxiv.org/abs/1706.03762",
                    filePath = null,
                    extractedText = null,
                    opinionText = "Self‑attention replaced recurrence and opened doors to massive models. Critical for modern AI. [[Artificial Intelligence]]",
                    confidence = 95
                )
                createEntry(
                    topicId = psychTopicId,
                    title = "Working Memory Limits",
                    description = "Nature review on capacity of visual working memory.",
                    resourceType = ResourceType.WEB_LINK.name,
                    url = "https://www.nature.com/articles/nn.4352",
                    filePath = null,
                    extractedText = null,
                    opinionText = "The classic 7±2 rule is overstated; real limits are ~4 items. UI should respect this. [[Cognitive Psychology]]",
                    confidence = 85
                )
                createEntry(
                    topicId = pkmTopicId,
                    title = "Deliberate Knowledge Capture Workflow",
                    description = "Steps: Read → Highlight → Vision → Opinion → Link.",
                    resourceType = ResourceType.MARKDOWN.name,
                    url = null,
                    filePath = "/dummy/deliberate_workflow.md",
                    extractedText = "A concise guide to forced reflection before saving.",
                    opinionText = "Forcing a visual note (Vision) before entry prevents collector's fallacy. Essential for deep learning. [[Knowledge Management]]",
                    confidence = 100
                )
                createEntry(
                    topicId = devTopicId,
                    title = "Jetpack Compose Basics",
                    description = "Introductory guide to modern Android UI toolkit.",
                    resourceType = ResourceType.WEB_LINK.name,
                    url = "https://developer.android.com/jetpack/compose",
                    filePath = null,
                    extractedText = null,
                    opinionText = "Compose simplifies UI creation and integrates well with MVVM. Good fit for Relatrix UI.",
                    confidence = 90
                )
                // Add a batch of synthetic entries to inflate data size (10 more)
                repeat(10) { idx ->
                    val topicId = listOf(aiTopicId, psychTopicId, pkmTopicId, devTopicId)[idx % 4]
                    createEntry(
                        topicId = topicId,
                        title = "Synthetic Entry ${idx + 1}",
                        description = "Automatically generated placeholder for demo purposes.",
                        resourceType = ResourceType.WEB_LINK.name,
                        url = "https://example.com/synthetic${idx + 1}",
                        filePath = null,
                        extractedText = null,
                        opinionText = "Placeholder opinion #${idx + 1}. Demonstrates scaling of entries.",
                        confidence = 70
                    )
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
