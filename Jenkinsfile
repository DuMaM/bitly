pipeline {

    triggers {
        githubPush()
    }

    agent {
        docker {
            image "cimg/android:2022.06.1"
            args "--rm --interactive"
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
                sh "gradle build"
            }
        }
    }
}