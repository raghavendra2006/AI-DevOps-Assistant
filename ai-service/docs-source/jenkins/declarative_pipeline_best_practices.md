# Jenkins Declarative Pipeline Best Practices & Troubleshooting

## Structure of a Robust Declarative Pipeline
```groovy
pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21'
            args '-v /root/.m2:/root/.m2'
        }
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }
    environment {
        APP_NAME = 'ai-devops-backend'
        REGISTRY_CREDENTIALS = credentials('docker-registry-secret')
    }
    stages {
        stage('Compile & Test') {
            steps {
                sh 'mvn clean test'
            }
        }
        stage('Docker Build & Scan') {
            steps {
                sh 'docker build -t ${APP_NAME}:${BUILD_NUMBER} .'
            }
        }
    }
    post {
        failure {
            emailext subject: "Pipeline Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: "Check build console at ${env.BUILD_URL}",
                     to: "devops-alerts@company.com"
        }
        cleanup {
            cleanWs()
        }
    }
}
```

## Common Pipeline Failure Modes
1. **Agent Provisioning Timeout**: Jenkins fails to connect to dynamic Kubernetes or Docker agent cloud. Check agent network connectivity and Docker daemon socket permissions (`/var/run/docker.sock`).
2. **Missing Workspace Cleanup**: Out of disk space on Jenkins worker node (`No space left on device`). Always include `cleanWs()` in `post { cleanup { ... } }`.
3. **Groovy CPS Mismatches**: Non-serializable objects across pipeline steps. Wrap complex logic in `@NonCPS` methods or shared libraries.
