@Library('nstut-ci') _

// Trusted-main-only mod validation. Keep GitHub Actions publishing unchanged.
// Isolate the optional BucketLib fixture from the standalone build and preserve
// test results and GameTest logs for BOTH runs, even after Gradle clean.
nstutGradle(
    targets: [
        [name: 'NeoForge 1.21.1 standalone: unit tests, JAR and GameTests',
         java: 21, minutes: 40,
         command: './gradlew --build-cache clean build runGameTestServer --no-daemon --max-workers=1 --stacktrace && mkdir -p ci-artifacts/standalone ci-reports/standalone && cp build/libs/*.jar ci-artifacts/standalone/ && cp -R build/test-results/test/. ci-reports/standalone/ && if [ -d build/gametest-run/logs ]; then cp -R build/gametest-run/logs ci-artifacts/standalone/; fi'],
        [name: 'NeoForge 1.21.1 BucketLib: unit tests, JAR and GameTests',
         java: 21, minutes: 40,
         command: './gradlew --build-cache -PbucketCompat clean build runGameTestServer --no-daemon --max-workers=1 --stacktrace && mkdir -p ci-artifacts/bucketlib ci-reports/bucketlib && cp build/libs/*.jar ci-artifacts/bucketlib/ && cp -R build/test-results/test/. ci-reports/bucketlib/ && if [ -d build/gametest-run/logs ]; then cp -R build/gametest-run/logs ci-artifacts/bucketlib/; fi']
    ],
    tests: 'ci-reports/**/*.xml',
    artifacts: 'ci-artifacts/**/*.jar',
    diagnostics: 'ci-artifacts/**/logs/**,build/reports/**,build/gametest-run/crash-reports/**'
)
