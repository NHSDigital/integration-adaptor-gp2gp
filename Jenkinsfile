pipeline {
    agent{
        label 'jenkins-workers'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: "10"))
    }

    environment {
        BUILD_TAG = sh label: 'Generating build tag', returnStdout: true, script: 'python3 scripts/tag.py ${GIT_BRANCH} ${BUILD_NUMBER} ${GIT_COMMIT}'
        BUILD_TAG_LOWER = sh label: 'Lowercase build tag', returnStdout: true, script: "echo -n ${BUILD_TAG} | tr '[:upper:]' '[:lower:]'"
        ENVIRONMENT_ID = "build1"
        ECR_REPO_DIR = "gp2gp"
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/${ECR_REPO_DIR}:${BUILD_TAG}"
    }

    stages {
        stage('Build') {
            stages {
                stage('Test') {
                    steps {
                        script {
                            if (sh(label: 'Running gp2gp docker build', script: 'docker build -t ${DOCKER_IMAGE} -f docker/service/Dockerfile --target test .', returnStatus: true) != 0) {error("Failed to build gp2gp Docker image")}
                        }
                    }
                }
                stage('Build Docker Images') {
                    steps {
                        script {
                            if (sh(label: 'Running gp2gp docker build', script: 'docker build -t ${DOCKER_IMAGE} -f docker/service/Dockerfile .', returnStatus: true) != 0) {error("Failed to build gp2gp Docker image")}
                        }
                    }
                }
                stage('Push Image') {
                    when {
                        expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
                    }
                    steps {
                        script {
                            if (ecrLogin(TF_STATE_BUCKET_REGION) != 0 )  { error("Docker login to ECR failed") }
                            String dockerPushCommand = "docker push ${DOCKER_IMAGE}"
                            if (sh (label: "Pushing image", script: dockerPushCommand, returnStatus: true) !=0) { error("Docker push gp2gp image failed") }
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            sh label: 'Remove all unused images not just dangling ones', script:'docker system prune --force'
            sh 'docker image rm -f $(docker images "*/*:*${BUILD_TAG}" -q) $(docker images "*/*/*:*${BUILD_TAG}" -q) || true'
        }
    }
}

int ecrLogin(String aws_region) {
    String ecrCommand = "aws ecr get-login --region ${aws_region}"
    String dockerLogin = sh (label: "Getting Docker login from ECR", script: ecrCommand, returnStdout: true).replace("-e none","") // some parameters that AWS provides and docker does not recognize
    return sh(label: "Logging in with Docker", script: dockerLogin, returnStatus: true)
}
