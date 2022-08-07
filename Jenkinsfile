pipeline {
    triggers {
        githubPush()
    }

    options {
      skipDefaultCheckout true
      disableConcurrentBuilds abortPrevious: true
    }

    agent {
        docker {
            image "cimg/android:2022.07"
        }
    }

    stages {
        stage("checkout") {
            steps {
                checkout scm
            }
        }

        stage("build") {
            steps {
                sh "./gradlew build"
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.apk', fingerprint: true, followSymlinks: false, onlyIfSuccessful: true
                }
            }
        }
    }

    post {
      aborted {
        unstable 'Build got aborted'
      }
    }
}