plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.metro)
}

repositories {
  mavenCentral()
  google()
}

ktfmt { googleStyle() }

kotlin {
  macosArm64("native") { binaries { executable { entryPoint = "dev.ralf.screen.main" } } }

  sourceSets {
    nativeMain {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.molecule.runtime)
      }
    }
    nativeTest {
      dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.assertk)
        implementation(libs.turbine)
        implementation(libs.molecule.runtime)
      }
    }
  }
}

tasks.register<Copy>("assembleApp") {
  dependsOn("linkReleaseExecutableNative")

  from("build/bin/native/releaseExecutable/screen-on-app.kexe") {
    into("MacOS")
    rename { "ScreenOn" }
    filePermissions { unix("755") }
  }
  from("Info.plist")
  into(layout.buildDirectory.dir("ScreenOn.app/Contents"))
}

tasks.register<Exec>("packageApp") {
  dependsOn("assembleApp")

  val appPath = layout.buildDirectory.file("ScreenOn.app").get().asFile.absolutePath
  commandLine("codesign", "--force", "--deep", "--sign", "-", appPath)

  doLast { println("Created and signed $appPath") }
}
