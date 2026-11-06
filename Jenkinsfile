@Library('nstut-ci') _

// Trusted main only. GitHub Actions' release/CurseForge publishing stays intact
// until an independently verified Jenkins publishing replacement is available.
// Each fixture must start from a clean Gradle build and Minecraft test world.
nstutGradle(
    targets: [
        [name: 'NeoForge 1.21.1 standalone: unit tests, JAR and GameTests',
         java: 21, minutes: 40,
         command: './gradlew --build-cache clean build runGameTestServer --no-daemon --max-workers=1 --stacktrace && mkdir -p ci-artifacts/standalone && cp build/libs/*.jar ci-artifacts/standalone/'],
        [name: 'NeoForge 1.21.1 BucketLib: unit tests, JAR and GameTests',
         java: 21, minutes: 40,
         command: './gradlew --build-cache -PbucketCompat clean build runGameTestServer --no-daemon --max-workers=1 --stacktrace && mkdir -p ci-artifacts/bucketlib && cp build/libs/*.jar ci-artifacts/bucketlib/']
    ],
    tests: 'build/test-results/test/*.xml',
    artifacts: 'ci-artifacts/**/*.jar',
    diagnostics: 'build/reports/**,build/gametest-run/logs/**,build/gametest-run/crash-reports/**'
)
