import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firebase: يُفعَّل تلقائيًا فقط عند وضع google-services.json في مجلد app
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// ===== بيانات التوقيع: من متغيرات البيئة أو keystore.properties المحلي (غير مرفوع) =====
val localSigning = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: localSigning.getProperty(prop)

val ksPath = signingValue("KEYSTORE_PATH", "storeFile")
val ksPass = signingValue("KEYSTORE_PASSWORD", "storePassword")
val ksAlias = signingValue("KEY_ALIAS", "keyAlias")
val keyPass = signingValue("KEY_PASSWORD", "keyPassword")

val hasReleaseSigning = listOf(ksPath, ksPass, ksAlias, keyPass).all { !it.isNullOrBlank() } &&
        File(ksPath!!).exists()

android {
    namespace = "com.khuyutalarab.game"
    compileSdk = 35

    defaultConfig {
        // ثابت بعد الاعتماد — لا يتغير أبدًا
        applicationId = "com.khuyutalarab.game"
        minSdk = 26
        targetSdk = 35
        versionCode = 1          // يزداد مع كل إصدار منشور
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = File(ksPath!!)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
        // debug يبقى بمفتاح debug الافتراضي، منفصل عن النشر
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

// منع بناء Release بدون المفتاح الأصلي — لا مفتاح بديل ولا بناء غير موقّع.
gradle.taskGraph.whenReady {
    val wantsRelease = allTasks.any {
        it.name.contains("Release", ignoreCase = true) &&
            (it.name.startsWith("assemble") || it.name.startsWith("bundle"))
    }
    if (wantsRelease && !hasReleaseSigning) {
        throw GradleException(
            "بيانات توقيع الإصدار غير متوفرة. وفّر KEYSTORE_PATH وKEYSTORE_PASSWORD وKEY_ALIAS وKEY_PASSWORD " +
                "(أو keystore.properties محليًا). لن يُنشأ مفتاح بديل."
        )
    }
}
