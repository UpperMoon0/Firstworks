@Library('nstut-ci') _

// Java 21 + Gradle are native capabilities on every approved worker.
// Pilot on capacity-qualified Windows hosts while the Linux shared runtime is
// validated. Source is restricted to reviewed main; publishing stays on Actions.
nstutNativeGradle(
    agentLabel: 'ci-native-gradle-ready && ci-windows-native',
    targets: [
        [id: 'standalone', name: 'NeoForge 1.21.1 standalone tests, JAR and GameTests',
         java: 21, minutes: 45, tasks: ['clean', 'build', 'runGameTestServer']],
        [id: 'bucketlib', name: 'NeoForge 1.21.1 BucketLib tests, JAR and GameTests',
         java: 21, minutes: 45, tasks: ['-PbucketCompat', 'clean', 'build', 'runGameTestServer']]
    ]
)
