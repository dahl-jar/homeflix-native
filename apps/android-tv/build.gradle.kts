plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}

val pitestTargets =
    mapOf(
        ":core:designsystem" to "app.homeflix.tv.core.designsystem.TvFocusStyle",
        ":core:network" to
            listOf(
                "app.homeflix.tv.core.network.JellyfinIdentity",
                "app.homeflix.tv.core.network.ServerCandidatesKt",
            ).joinToString(","),
        ":feature:auth" to
            listOf(
                "app.homeflix.tv.feature.auth.AuthContract",
                "app.homeflix.tv.feature.auth.ProfileSelection",
                "app.homeflix.tv.feature.auth.PinInputReducer",
            ).joinToString(","),
        ":core:session" to "app.homeflix.tv.core.session.SessionPayloadCodec",
        ":feature:detail" to "app.homeflix.tv.feature.detail.DetailFormatKt",
        ":feature:profile" to "app.homeflix.tv.feature.profile.ProfileDetailsKt",
        ":feature:home" to
            listOf(
                "app.homeflix.tv.feature.home.HomeContract",
                "app.homeflix.tv.feature.home.HomePolicy",
                "app.homeflix.tv.feature.home.HomeHeroMetadata",
            ).joinToString(","),
    )

subprojects {
    val targetClasses = pitestTargets[path] ?: return@subprojects
    pluginManager.withPlugin("com.android.library") {
        afterEvaluate {
            val pitest by configurations.creating
            val unitTest = tasks.named<Test>("testDebugUnitTest")
            val mainClasses = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
            val testClasses = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debugUnitTest/compileDebugUnitTestKotlin/classes")
            val reportDirectory = layout.buildDirectory.dir("reports/pitest/debug")
            val sourceDirectory = layout.projectDirectory.dir("src/main/kotlin")
            val mutationClasspath = files(mainClasses, testClasses, unitTest.map { it.classpath })

            dependencies.add(
                pitest.name,
                "org.pitest:pitest-command-line:${providers.gradleProperty("pitest.engine.version").get()}",
            )
            dependencies.add(pitest.name, "org.pitest:pitest-junit5-plugin:1.2.3")

            tasks.register<JavaExec>("pitestDebug") {
                group = "verification"
                description = "Runs scoped PIT mutation tests against the debug JVM unit tests."
                dependsOn(unitTest)
                classpath = pitest
                mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
                args(
                    "--reportDir",
                    reportDirectory.get().asFile.absolutePath,
                    "--targetClasses",
                    targetClasses,
                    "--targetTests",
                    "app.homeflix.*Test",
                    "--sourceDirs",
                    sourceDirectory.asFile.absolutePath,
                    "--includeLaunchClasspath",
                    "false",
                    "--testPlugin",
                    "junit5",
                    "--outputFormats",
                    "XML,HTML",
                    "--threads",
                    "4",
                    "--mutationThreshold",
                    "100",
                    "--coverageThreshold",
                    "100",
                    "--timestampedReports",
                    "false",
                    "--failWhenNoMutations",
                    "true",
                )
                argumentProviders.add {
                    listOf(
                        "--classPath",
                        mutationClasspath.files.joinToString(",") { it.absolutePath },
                        "--mutableCodePaths",
                        mainClasses.get().asFile.absolutePath,
                    )
                }
                inputs.dir(sourceDirectory)
                inputs.dir(testClasses)
                inputs.files(mutationClasspath).withNormalizer(ClasspathNormalizer::class.java)
                outputs.dir(reportDirectory)
            }
        }
    }
}
