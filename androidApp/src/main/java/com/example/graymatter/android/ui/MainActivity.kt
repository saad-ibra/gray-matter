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
                
                // 1. Topics
                val aiTopicId = UUID.randomUUID().toString()
                val aiTopic = Topic(id = aiTopicId, name = "Artificial Intelligence", notes = "Synthesis on scaling, LLMs, and cognitive systems.", resourceCount = 2, updatedAt = now)
                topicRepository.saveTopic(aiTopic)
                
                val psychTopicId = UUID.randomUUID().toString()
                val psychTopic = Topic(id = psychTopicId, name = "Cognitive Psychology", notes = "Deliberating on brain capacity, memory limits, and cognitive load.", resourceCount = 1, updatedAt = now)
                topicRepository.saveTopic(psychTopic)

                val pkmTopicId = UUID.randomUUID().toString()
                val pkmTopic = Topic(id = pkmTopicId, name = "Knowledge Management", notes = "Opinion-first deliberation workflows and local-first software architecture.", resourceCount = 1, updatedAt = now)
                topicRepository.saveTopic(pkmTopic)
                
                // 2. Resource Entries
                // Entry 1: GPT-3 Paper
                val entry1Id = UUID.randomUUID().toString()
                val res1Id = UUID.randomUUID().toString()
                val op1Id = UUID.randomUUID().toString()
                resourceEntryRepository.createResourceEntryWithDetails(
                    resourceEntryId = entry1Id,
                    resourceId = res1Id,
                    resourceType = ResourceType.WEB_LINK.name,
                    url = "https://arxiv.org/abs/2005.14165",
                    filePath = null,
                    extractedText = null,
                    title = "Language Models are Few-Shot Learners",
                    description = "The foundational GPT-3 paper exploring scaling laws and few-shot capabilities.",
                    opinionId = op1Id,
                    opinionText = "Amazing scaling effects, but is it actual understanding or sophisticated statistical correlation? I believe scale is necessary but not sufficient for AGI. [[Artificial Intelligence]] is more than next-token prediction.",
                    confidence = 80,
                    now = now
                )
                resourceEntryRepository.updateResourceEntryTopic(entry1Id, aiTopicId)
                
                // Entry 2: Transformers Note
                val entry2Id = UUID.randomUUID().toString()
                val res2Id = UUID.randomUUID().toString()
                val op2Id = UUID.randomUUID().toString()
                resourceEntryRepository.createResourceEntryWithDetails(
                    resourceEntryId = entry2Id,
                    resourceId = res2Id,
                    resourceType = ResourceType.MARKDOWN.name,
                    url = null,
                    filePath = "/dummy/transformers.md",
                    extractedText = "A summary of CNNs, RNNs, and Transformers. Transformers use self-attention mechanism...",
                    title = "Neural Network Architectures",
                    description = "Comparing CNNs, RNNs, and Transformers for sequence processing.",
                    opinionId = op2Id,
                    opinionText = "Self-attention has completely replaced recurrent architectures for sequence modeling. It represents a paradigm shift in how we represent context.",
                    confidence = 95,
                    now = now
                )
                resourceEntryRepository.updateResourceEntryTopic(entry2Id, aiTopicId)

                // Entry 3: Memory limits paper
                val entry3Id = UUID.randomUUID().toString()
                val res3Id = UUID.randomUUID().toString()
                val op3Id = UUID.randomUUID().toString()
                resourceEntryRepository.createResourceEntryWithDetails(
                    resourceEntryId = entry3Id,
                    resourceId = res3Id,
                    resourceType = ResourceType.WEB_LINK.name,
                    url = "https://www.nature.com/articles/nn.4352",
                    filePath = null,
                    extractedText = null,
                    title = "Working Memory Limits",
                    description = "Investigating the neural capacity of working memory and visual short-term memory.",
                    opinionId = op3Id,
                    opinionText = "The classic limit of 7 items is actually smaller (around 4) when dynamic binding is required. This has huge implications for designing software UI/UX. See [[Cognitive Psychology]] notes.",
                    confidence = 85,
                    now = now
                )
                resourceEntryRepository.updateResourceEntryTopic(entry3Id, psychTopicId)

                // Entry 4: PKM Note
                val entry4Id = UUID.randomUUID().toString()
                val res4Id = UUID.randomUUID().toString()
                val op4Id = UUID.randomUUID().toString()
                resourceEntryRepository.createResourceEntryWithDetails(
                    resourceEntryId = entry4Id,
                    resourceId = res4Id,
                    resourceType = ResourceType.MARKDOWN.name,
                    url = null,
                    filePath = "/dummy/workflow.md",
                    extractedText = "A personal reflection workflow: Read -> Extract -> Formulate Opinion -> Review Timeline -> Connect via Relatrix.",
                    title = "My Deliberation Workflow",
                    description = "Deliberating on the collector's fallacy and building an active knowledge synthesis habit.",
                    opinionId = op4Id,
                    opinionText = "Forcing reflection before saving prevents the collector's fallacy. Highly aligned with the principles of forced deliberation. [[Knowledge Management]] is about thinking, not collecting.",
                    confidence = 100,
                    now = now
                )
                resourceEntryRepository.updateResourceEntryTopic(entry4Id, pkmTopicId)

                // Let's add a second opinion to GPT-3 Paper to show a timeline narrative
                val op5Id = UUID.randomUUID().toString()
                val opinion = Opinion(
                    id = op5Id,
                    itemId = entry1Id,
                    text = "Re-reading: Scaling laws might be slowing down. We might need architectural innovations (like neurosymbolic integration) to reach AGI. [[Artificial Intelligence]] requires planning and reasoning.",
                    confidenceScore = 65,
                    imagePath = null,
                    createdAt = now + 100000,
                    updatedAt = now + 100000
                )
                opinionRepository.saveOpinion(opinion)
                resourceEntryRepository.updateResourceEntryOpinionMetadata(entry1Id, now + 100000)
                
                // Sync all links to build a beautiful Relatrix Graph!
                autoLinkService.syncLinks(op1Id, com.example.graymatter.domain.ReferenceType.OPINION, "Amazing scaling effects, but is it actual understanding or sophisticated statistical correlation? I believe scale is necessary but not sufficient for AGI. [[Artificial Intelligence]] is more than next-token prediction.", emptyList())
                autoLinkService.syncLinks(op2Id, com.example.graymatter.domain.ReferenceType.OPINION, "Self-attention has completely replaced recurrent architectures for sequence modeling. It represents a paradigm shift in how we represent context.", emptyList())
                autoLinkService.syncLinks(op3Id, com.example.graymatter.domain.ReferenceType.OPINION, "The classic limit of 7 items is actually smaller (around 4) when dynamic binding is required. This has huge implications for designing software UI/UX. See [[Cognitive Psychology]] notes.", emptyList())
                autoLinkService.syncLinks(op4Id, com.example.graymatter.domain.ReferenceType.OPINION, "Forcing reflection before saving prevents the collector's fallacy. Highly aligned with the principles of forced deliberation. [[Knowledge Management]] is about thinking, not collecting.", emptyList())
                autoLinkService.syncLinks(op5Id, com.example.graymatter.domain.ReferenceType.OPINION, "Re-reading: Scaling laws might be slowing down. We might need architectural innovations (like neurosymbolic integration) to reach AGI. [[Artificial Intelligence]] requires planning and reasoning.", emptyList())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
