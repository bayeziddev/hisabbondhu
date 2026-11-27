import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// ---- Values injected by CI (GitHub Actions) or local gradle properties ----
fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

val ciVersionCode = env("VERSION_CODE")?.toIntOrNull()
val ciVersionName = env("VERSION_NAME")
val apiBaseUrl = (env("API_BASE_URL") ?: (findProperty("apiBaseUrl") as String?) ?: "https://api.hisabbondhu.com/")
    .let { if (it.endsWith("/")) it else "$it/" }
val websiteUrl = (env("WEBSITE_URL") ?: "https://bayeziddev.github.io/hisabbondhu/")
    .let { if (it.endsWith("/")) it else "$it/" }
val supportEmailValue = env("SUPPORT_EMAIL") ?: "support@sayadbayezid.com"
val supportWhatsApp = (env("SUPPORT_WHATSAPP") ?: "8801519601517").filter { it.isDigit() }

// Release signing (upload key). Provided only in CI from GitHub secrets.
val signingStoreFile = env("SIGNING_STORE_FILE")
val signingStorePassword = env("SIGNING_STORE_PASSWORD")
val signingKeyAlias = env("SIGNING_KEY_ALIAS")
val signingKeyPassword = env("SIGNING_KEY_PASSWORD") ?: signingStorePassword
val hasReleaseSigning = signingStoreFile != null && file(signingStoreFile).exists() &&
    signingStorePassword != null && signingKeyAlias != null

android {
    namespace = "com.hisabbondhu.app"
    compileSdk = 36

    defaultConfig {
        // Play Store package name (fixed forever once the Play Console app exists).
        // The Kotlin code package stays `com.hisabbondhu.app` (namespace above).
        applicationId = "com.hisabbondhu.myapp"
        minSdk = 24
        targetSdk = 36
        versionCode = ciVersionCode ?: 1
        versionName = ciVersionName ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "WEBSITE_URL", "\"$websiteUrl\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"$supportEmailValue\"")
        buildConfigField("String", "SUPPORT_WHATSAPP", "\"$supportWhatsApp\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Keep every language inside the AAB so the in-app language switch works offline.
    bundle {
        language { enableSplit = false }
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.android.installreferrer) // reads the referral code from a Play Store link

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Screenshot tour (android/tools/screenshot-tour.sh) — runs on emulators in CI
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.uiautomator)
}

// ---- Bundle the website's legal pages into the APK so they are readable offline ----
abstract class CopyLegalPagesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val websiteDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val supportEmail: Property<String>

    @TaskAction
    fun copyPages() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        val legal = File(out, "legal").apply { mkdirs() }
        val site = websiteDir.get().asFile
        listOf("privacy.html", "terms.html", "your-rights.html").forEach { name ->
            File(site, name).takeIf { it.exists() }?.copyTo(File(legal, name), overwrite = true)
        }
        File(site, "assets").takeIf { it.exists() }?.copyRecursively(File(legal, "assets"), overwrite = true)
        val email = supportEmail.get()
        legal.walkTopDown().filter { it.isFile && (it.extension == "html" || it.extension == "js") }.forEach { f ->
            f.writeText(f.readText().replace("support@sayadbayezid.com", email))
        }
    }
}

val copyLegalPages = tasks.register<CopyLegalPagesTask>("copyLegalPages") {
    websiteDir.set(rootProject.file("../website"))
    outputDir.set(layout.buildDirectory.dir("generated/legalAssets"))
    supportEmail.set(supportEmailValue)
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyLegalPages, CopyLegalPagesTask::outputDir)
    }
}
