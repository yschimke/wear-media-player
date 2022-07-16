import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.VariantOutput
import com.android.build.api.variant.VariantOutputConfiguration
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class AppKind {
    Handset,
    Wear
}

fun Project.setVersionsForApp(kind: AppKind) = onMainOutput { variant, output ->
    output.versionName.set(provider { versionName(kind, variant.buildType) })
    output.versionCode.set(provider { versionCode(kind) })
}

private fun Project.versionCode(kind: AppKind): Int {
    val releaseBuildNumber = rootProject.file("releaseBuildNumber.txt").useLines {
        it.first()
    }.toInt().also { require(it > 0) }
    return releaseBuildNumber * 2 - when (kind) {
        AppKind.Handset -> 1
        AppKind.Wear -> 0
    }
}

private fun Project.versionName(
    kind: AppKind,
    buildType: String?
): String {
    val releaseName = rootProject.file("releaseName.txt").useLines {
        it.first()
    }.also { require(it.isNotBlank()) }
    val kindSuffix = when (kind) {
        AppKind.Handset -> ""
        AppKind.Wear -> "-w"
    }
    return when (buildType) {
        "debug" -> {
            val dateTime = Instant.now().atZone(ZoneId.of("Europe/Paris"))
            val date = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val time = dateTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
            "$releaseName$kindSuffix-$date[$time]"
        }
        else -> "$releaseName$kindSuffix"
    }
}

private fun Project.onMainOutput(block: (ApplicationVariant, VariantOutput) -> Unit) {
    the<ApplicationAndroidComponentsExtension>().onVariants { variant ->
        val mainOutput: VariantOutput = variant.outputs.single {
            it.outputType == VariantOutputConfiguration.OutputType.SINGLE
        }
        block(variant, mainOutput)
    }
}
