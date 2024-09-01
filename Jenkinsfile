pipeline {
    agent any
    stages {
        stage('Build') {
            tools {
                jdk "jdk17"
            }
            steps {
                sh './gradlew publishToNyaaCatCILocal'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            cleanWs()
        }
    }
}