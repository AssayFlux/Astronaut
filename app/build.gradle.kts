plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.devtools.ksp")
}
android {
 namespace="org.offlineai.messenger"
 compileSdk=35
 defaultConfig {
  applicationId="org.offlineai.messenger"
  minSdk=26
  targetSdk=35
  versionCode=4
  versionName="0.4.0"
  testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner"
 }
 buildFeatures { compose=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
}
kotlin {
 compilerOptions {
  jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
 }
}
dependencies {
 val bom=platform("androidx.compose:compose-bom:2024.12.01")
 val roomVersion="2.8.5"
 implementation(bom)
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.documentfile:documentfile:1.0.1")
 implementation("androidx.room:room-runtime:$roomVersion")
 implementation("androidx.room:room-ktx:$roomVersion")
 ksp("androidx.room:room-compiler:$roomVersion")
 implementation("androidx.datastore:datastore-preferences:1.1.1")
 implementation("androidx.work:work-runtime-ktx:2.10.0")
 implementation("io.coil-kt:coil-compose:2.7.0")
 implementation("androidx.media3:media3-exoplayer:1.5.1")
 implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.1")
 testImplementation("junit:junit:4.13.2")
}
