pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        BASE_URL = 'http://localhost:8080'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Prepare') {
            steps {
                sh 'cp .env.example .env'
            }
        }

        stage('Start Backend') {
            steps {
                sh '''
                    docker compose up -d postgres-auth postgres-files postgres-prefs redis rabbitmq
                    sleep 20
                    docker compose up -d auth-service file-service preferences-service gateway
                    for i in $(seq 1 36); do
                        if curl -sf http://localhost:8080/actuator/health > /dev/null; then
                            echo "gateway up"
                            exit 0
                        fi
                        sleep 5
                    done
                    echo "gateway did not become healthy" >&2
                    docker compose logs --tail 100
                    exit 1
                '''
            }
        }

        stage('API Tests') {
            steps {
                dir('testing-framework') {
                    sh 'mvn test -DsuiteXmlFile=src/test/resources/testng-parallel-classes.xml'
                }
            }
            post {
                always {
                    junit testResults: 'testing-framework/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
                }
            }
        }
    }

    post {
        always {
            sh 'docker compose down -v || true'
        }
        success { echo 'All tests passed' }
        failure { echo 'Tests failed' }
    }
}
