package com.doruk.fitprogress.core.di

import com.doruk.fitprogress.core.ai.AiCoachingRepository
import com.doruk.fitprogress.core.ai.AiCoachingRepositoryImpl
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiCoachJson

/**
 * Factory interface that creates a [GenerativeModel] instance dynamically bound
 * to the user's runtime Gemini API Key from [com.doruk.fitprogress.core.datastore.AiSettingsDataStore]
 * while strictly enforcing `responseMimeType = "application/json"`.
 */
fun interface GenerativeModelFactory {
    fun create(apiKey: String, systemPrompt: String): GenerativeModel
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AiCoachBindsModule {
    @Binds
    @Singleton
    abstract fun bindAiCoachingRepository(
        impl: AiCoachingRepositoryImpl
    ): AiCoachingRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AiCoachModule {

    const val DEFAULT_GEMINI_MODEL = "gemini-2.0-flash"

    val FALLBACK_GEMINI_MODELS = listOf(
        DEFAULT_GEMINI_MODEL,
        "gemini-1.5-flash",
        "gemini-1.5-flash-latest",
        "gemini-2.0-flash-001",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite"
    )

    @Provides
    @Singleton
    @AiCoachJson
    fun provideAiCoachJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideGeminiGenerationConfig(): GenerationConfig = generationConfig {
        responseMimeType = "application/json"
        temperature = 0.25f
        topK = 32
        topP = 0.95f
    }

    @Provides
    @Singleton
    fun provideGenerativeModelFactory(
        generationConfig: GenerationConfig
    ): GenerativeModelFactory = GenerativeModelFactory { apiKey, systemPrompt ->
        GenerativeModel(
            modelName = DEFAULT_GEMINI_MODEL,
            apiKey = apiKey,
            generationConfig = generationConfig,
            systemInstruction = content {
                text(systemPrompt)
            }
        )
    }

    @Provides
    @Singleton
    fun provideKtorHttpClient(): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
    }
}
