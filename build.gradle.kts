import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.metro)
}

ktfmt { googleStyle() }

kotlin {
  macosArm64("native") { binaries { executable { entryPoint = "dev.ralf.restless.main" } } }

  sourceSets {
    nativeMain {
      dependencies {
        implementation(libs.compose.runtime)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.molecule.runtime)
      }
    }
    nativeTest {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.assertk)
        implementation(libs.turbine)
      }
    }
  }
}

tasks.register<Copy>("assembleApp") {
  val releaseLink = tasks.named<KotlinNativeLink>("linkReleaseExecutableNative")
  from(releaseLink.map { it.outputFile.get() }) {
    into("MacOS")
    rename { "Restless" }
    filePermissions { unix("755") }
  }
  from("Info.plist")
  into(layout.buildDirectory.dir("Restless.app/Contents"))
}

tasks.register<Exec>("packageApp") {
  dependsOn("assembleApp")

  val appPath = layout.buildDirectory.file("Restless.app").get().asFile.absolutePath
  commandLine("codesign", "--force", "--deep", "--sign", "-", appPath)

  doLast { println("Created and signed $appPath") }
}
