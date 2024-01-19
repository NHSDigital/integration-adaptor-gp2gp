Boolean publishWiremockImage = true // true: To publish gp2gp wiremock image to AWS ECR gp2gp-wiremock
Boolean publishMhsMockImage  = true // true: to publish mhs mock image to AWS ECR gp2gp-mock-mhs
Boolean publishGpccMockImage  = true // true: to publish gpcc mock image to AWS ECR gp2gp-gpcc-mock
Boolean publishGpcApiMockImage  = true // true: to publish gpc api mock image to AWS ECR gp2gp-gpc-api-mock
Boolean publishSdsApiMockImage  = true // true: to publish sds api mock image to AWS ECR gp2gp-sds-api-mock

Boolean gpccDeploy    = true         // true: To deploy GPC-Consumer service inside gp2gp
String gpccBranch     = "main"      // Name of branch as a prefix to image name (GPC-Consumer) stored in ECR
String gpccEcrRepo    = "gpc-consumer" // ECR Repo Name of GPC-Consumer
String tfGpccImagePrefix  = "gpc-consumer" // Image name prefix of GPC-Consumer

pipeline {
    agent{
        label 'jenkins-workers'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: "10"))
        timeout(time: 1, unit: 'HOURS')
    }

    environment {
        BUILD_TAG = sh label: 'Generating build tag', returnStdout: true, script: 'python3 scripts/tag.py ${GIT_BRANCH} ${BUILD_NUMBER} ${GIT_COMMIT}'
        
        ECR_REPO_DIR = "gp2gp"
        WIREMOCK_ECR_REPO_DIR = "gp2gp-wiremock"
        MHS_MOCK_ECR_REPO_DIR = "gp2gp-mock-mhs"
        GPCC_MOCK_ECR_REPO_DIR = "gp2gp-gpcc-mock"
        GPC_API_MOCK_ECR_REPO_DIR = "gp2gp-gpc-api-mock"
        SDS_API_MOCK_ECR_REPO_DIR = "gp2gp-sds-api-mock"

        DOCKER_IMAGE = "${DOCKER_REGISTRY}/${ECR_REPO_DIR}:${BUILD_TAG}"
        WIREMOCK_DOCKER_IMAGE = "${DOCKER_REGISTRY}/${WIREMOCK_ECR_REPO_DIR}:${BUILD_TAG}"
        MHS_MOCK_DOCKER_IMAGE  = "${DOCKER_REGISTRY}/${MHS_MOCK_ECR_REPO_DIR}:${BUILD_TAG}"
        GPCC_MOCK_DOCKER_IMAGE  = "${DOCKER_REGISTRY}/${GPCC_MOCK_ECR_REPO_DIR}:${BUILD_TAG}"
        GPC_API_MOCK_DOCKER_IMAGE  = "${DOCKER_REGISTRY}/${GPC_API_MOCK_ECR_REPO_DIR}:${BUILD_TAG}"
        SDS_API_MOCK_DOCKER_IMAGE  = "${DOCKER_REGISTRY}/${SDS_API_MOCK_ECR_REPO_DIR}:${BUILD_TAG}"
    }

    stages {
        stage('Build') {
            stages {
                stage('Tests') {
                    steps {
                        script {
                            sh '''
                                source docker/vars.local.tests.sh
                                docker network create commonforgp2gp || true
                                docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml stop
                                docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml rm -f
                                docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml build
                                docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml up --exit-code-from gp2gp
                            '''
                        }
                    }
                    post {
                        always {
                            sh "docker cp tests:/home/gradle/service/build ."
                            archiveArtifacts artifacts: 'build/reports/**/*.*', fingerprint: true
                            junit '**/build/test-results/**/*.xml'
                            recordIssues(
                                enabledForFailure: true,
                                tools: [
                                    checkStyle(pattern: 'build/reports/checkstyle/*.xml'),
                                    spotBugs(pattern: 'build/reports/spotbugs/*.xml')
                                ]
                            )
                            // Disable JacocoPublisher for now, as our Jenkins doesn't support Java 17
                            // See NIAD-3022 for more details
                            // step([
                            //   $class : 'JacocoPublisher',
                            //   execPattern : '**/build/jacoco/*.exec',
                            //   classPattern : '**/build/classes/java',
                            //   sourcePattern : 'src/main/java',
                            //   exclusionPattern : '**/*Test.class'
                            // ])
                            sh "rm -rf build"
                            sh "docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml down"
                            sh "docker network rm commonforgp2gp"
                        }
                    }
                }

                stage('Build Docker Images') {
                    steps {
                        script {
                            if (sh(label: 'Running gp2gp docker build', script: 'docker build -f docker/service/Dockerfile -t ${DOCKER_IMAGE} .', returnStatus: true) != 0) {error("Failed to build gp2gp Docker image")}

                            if (publishWiremockImage) {
                                if (sh(label: "Running ${WIREMOCK_ECR_REPO_DIR} docker build", script: 'docker build -f docker/wiremock/Dockerfile -t ${WIREMOCK_DOCKER_IMAGE} docker/wiremock', returnStatus: true) != 0) {error("Failed to build ${WIREMOCK_ECR_REPO_DIR} Docker image")}
                            }
                            if (publishMhsMockImage) {
                                if (sh(label: "Running ${MHS_MOCK_ECR_REPO_DIR} docker build", script: 'docker build -f docker/mock-mhs-adaptor/Dockerfile -t ${MHS_MOCK_DOCKER_IMAGE} .', returnStatus: true) != 0) {error("Failed to build ${MHS_MOCK_ECR_REPO_DIR} Docker image")}
                            }
                            if (publishGpccMockImage) {
                                if (sh(label: "Running ${GPCC_MOCK_ECR_REPO_DIR} docker build", script: 'docker build -f docker/gpcc-mock/Dockerfile -t ${GPCC_MOCK_DOCKER_IMAGE} docker/gpcc-mock', returnStatus: true) != 0) {error("Failed to build ${GPCC_MOCK_ECR_REPO_DIR} Docker image")}
                            }
                            if (publishGpcApiMockImage) {
                                if (sh(label: "Running ${GPC_API_MOCK_ECR_REPO_DIR} docker build", script: 'docker build -f docker/gpc-api-mock/Dockerfile -t ${GPC_API_MOCK_DOCKER_IMAGE} docker/gpc-api-mock', returnStatus: true) != 0) {error("Failed to build ${GPC_API_MOCK_ECR_REPO_DIR} Docker image")}
                            }
                            if (publishSdsApiMockImage) {
                                if (sh(label: "Running ${SDS_API_MOCK_ECR_REPO_DIR} docker build", script: 'docker build -f docker/sds-api-mock/Dockerfile -t ${SDS_API_MOCK_DOCKER_IMAGE} docker/sds-api-mock', returnStatus: true) != 0) {error("Failed to build ${SDS_API_MOCK_ECR_REPO_DIR} Docker image")}
                            }
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
                            if (sh (label: "Pushing image", script: "docker push ${DOCKER_IMAGE}", returnStatus: true) !=0) { error("Docker push gp2gp image failed") }

                            if (publishWiremockImage) {
                                if (sh (label: "Pushing Wiremock image", script: "docker push ${WIREMOCK_DOCKER_IMAGE}", returnStatus: true) !=0) { error("Docker push ${WIREMOCK_ECR_REPO_DIR} image failed") }
                            }

                            if (publishMhsMockImage) {
                                if (sh(label: "Pushing MHS Mock image", script: "docker push ${MHS_MOCK_DOCKER_IMAGE}", returnStatus: true) != 0) {error("Docker push ${MHS_MOCK_ECR_REPO_DIR} image failed") }
                            }

                            if (publishGpccMockImage) {
                                if (sh(label: "Pushing GPCC Mock image", script: "docker push ${GPCC_MOCK_DOCKER_IMAGE}", returnStatus: true) != 0) {error("Docker push ${GPCC_MOCK_ECR_REPO_DIR} image failed") }
                            }

                            if (publishGpcApiMockImage) {
                                if (sh(label: "Pushing GPC API Mock image", script: "docker push ${GPC_API_MOCK_DOCKER_IMAGE}", returnStatus: true) != 0) {error("Docker push ${GPC_API_MOCK_ECR_REPO_DIR} image failed") }
                            }

                            if (publishSdsApiMockImage) {
                                if (sh(label: "Pushing SDS API Mock image", script: "docker push ${SDS_API_MOCK_DOCKER_IMAGE}", returnStatus: true) != 0) {error("Docker push ${SDS_API_MOCK_ECR_REPO_DIR} image failed") }
                            }
                        }
                    }
                }
                stage('Test') {
                    stages {

                        stage('E2E Tests') {
                            steps {
                                sh '''
                                    source docker/vars.local.e2e.sh
                                    docker network create commonforgp2gp || true
                                    docker-compose -f docker/docker-compose.yml -f docker/docker-compose-e2e-tests.yml build
                                    docker-compose -f docker/docker-compose.yml -f docker/docker-compose-e2e-tests.yml up --exit-code-from gp2gp-e2e-tests mongodb activemq gp2gp wiremock gpcc gp2gp-e2e-tests
                                '''

                            }
                            post {
                                always {
                                    sh "docker cp e2e-tests:/home/gradle/e2e-tests/build ."
                                    sh "mv build e2e-build"
                                    archiveArtifacts artifacts: 'e2e-build/reports/**/*.*', fingerprint: true
                                    junit '**/e2e-build/test-results/**/*.xml'
                                    sh "rm -rf e2e-build"
                                    sh "docker-compose -f docker/docker-compose.yml -f docker/docker-compose-e2e-tests.yml down"
                                    sh "docker network rm commonforgp2gp"
                                }
                            }
                        } //stage E2E Test
                    } //Stages
                }  // Stage Test
            }  // Stages Build
        } //Stage Build
    }  //Stages
    post {
        always {
            sh label: 'Remove images created by docker-compose', script: 'docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml down --rmi local'
            sh label: 'Remove exited containers', script: 'docker container prune --force'
            sh label: 'Remove images tagged with current BUILD_TAG', script: 'docker image rm -f $(docker images "*/*:*${BUILD_TAG}" -q) $(docker images "*/*/*:*${BUILD_TAG}" -q) || true'
        }
    }
}  // Pipeline

int ecrLogin(String aws_region) {
    String ecrCommand = "aws ecr get-login --region ${aws_region}"
    String dockerLogin = sh (label: "Getting Docker login from ECR", script: ecrCommand, returnStdout: true).replace("-e none","") // some parameters that AWS provides and docker does not recognize
    return sh(label: "Logging in with Docker", script: dockerLogin, returnStatus: true)
}

String tfEnv(String tfEnvRepo="https://github.com/tfutils/tfenv.git", String tfEnvPath="~/.tfenv") {
  sh(label: "Get tfenv" ,  script: "git clone ${tfEnvRepo} ${tfEnvPath}", returnStatus: true)
  sh(label: "Install TF",  script: "${tfEnvPath}/bin/tfenv install"     , returnStatus: true)
  return "${tfEnvPath}/bin/terraform"
}

int terraformInit(String tfStateBucket, String project, String environment, String component, String region) {
  String terraformBinPath = tfEnv()
  println("Terraform Init for Environment: ${environment} Component: ${component} in region: ${region} using bucket: ${tfStateBucket}")
  String command = "${terraformBinPath} init -backend-config='bucket=${tfStateBucket}' -backend-config='region=${region}' -backend-config='key=${project}-${environment}-${component}.tfstate' -input=false -no-color"
  dir("components/${component}") {
    return( sh( label: "Terraform Init", script: command, returnStatus: true))
  } // dir
} // int TerraformInit

int terraform(String action, String tfStateBucket, String project, String environment, String component, String region, Map<String, String> variables=[:], List<String> parameters=[]) {
    println("Running Terraform ${action} in region ${region} with: \n Project: ${project} \n Environment: ${environment} \n Component: ${component}")
    variablesMap = variables
    variablesMap.put('region',region)
    variablesMap.put('project', project)
    variablesMap.put('environment', environment)
    variablesMap.put('tf_state_bucket',tfStateBucket)
    parametersList = parameters
    parametersList.add("-no-color")
    //parametersList.add("-compact-warnings")  /TODO update terraform to have this working

    // Get the secret variables for global
    String secretsFile = "etc/secrets.tfvars"
    writeVariablesToFile(secretsFile,getAllSecretsForEnvironment(environment,"nia",region))
    String terraformBinPath = tfEnv()

    List<String> variableFilesList = [
      "-var-file=../../etc/global.tfvars",
      "-var-file=../../etc/${region}_${environment}.tfvars",
      "-var-file=../../${secretsFile}"
    ]
    if (action == "apply"|| action == "destroy") {parametersList.add("-auto-approve")}
    List<String> variablesList=variablesMap.collect { key, value -> "-var ${key}=${value}" }
    String command = "${terraformBinPath} ${action} ${variableFilesList.join(" ")} ${parametersList.join(" ")} ${variablesList.join(" ")} "
    dir("components/${component}") {
      return sh(label:"Terraform: "+action, script: command, returnStatus: true)
    } // dir
} // int Terraform

Map<String,String> collectTfOutputs(String component) {
  Map<String,String> returnMap = [:]
  dir("components/${component}") {
    String terraformBinPath = tfEnv()
    List<String> outputsList = sh (label: "Listing TF outputs", script: "${terraformBinPath} output", returnStdout: true).split("\n")
    outputsList.each {
      returnMap.put(it.split("=")[0].trim(),it.split("=")[1].trim())
    }
  } // dir
  return returnMap
}

// Retrieving Secrets from AWS Secrets
String getSecretValue(String secretName, String region) {
  String awsCommand = "aws secretsmanager get-secret-value --region ${region} --secret-id ${secretName} --query SecretString --output text"
  return sh(script: awsCommand, returnStdout: true).trim()
}

Map<String,Object> decodeSecretKeyValue(String rawSecret) {
  List<String> secretsSplitted = rawSecret.replace("{","").replace("}","").split(",")
  Map<String,Object> secretsDecoded = [:]
  secretsSplitted.each {
    String key = it.split(":")[0].trim().replace("\"","")
    Object value = it.split(":")[1]
    secretsDecoded.put(key,value)
  }
  return secretsDecoded
}

List<String> getSecretsByPrefix(String prefix, String region) {
  String awsCommand = "aws secretsmanager list-secrets --region ${region} --query SecretList[].Name --output text"
  List<String> awsReturnValue = sh(script: awsCommand, returnStdout: true).split()
  return awsReturnValue.findAll { it.startsWith(prefix) }
}

Map<String,Object> getAllSecretsForEnvironment(String environment, String secretsPrefix, String region) {
  List<String> globalSecrets = getSecretsByPrefix("${secretsPrefix}-global",region)
  println "global secrets:" + globalSecrets
  List<String> environmentSecrets = getSecretsByPrefix("${secretsPrefix}-${environment}",region)
  println "env secrets:" + environmentSecrets
  Map<String,Object> secretsMerged = [:]
  globalSecrets.each {
    String rawSecret = getSecretValue(it,region)
    if (it.contains("-kvp")) {
      secretsMerged << decodeSecretKeyValue(rawSecret)
    } else {
      secretsMerged.put(it.replace("${secretsPrefix}-global-",""),rawSecret)
    }
  }
  environmentSecrets.each {
    String rawSecret = getSecretValue(it,region)
    if (it.contains("-kvp")) {
      secretsMerged << decodeSecretKeyValue(rawSecret)
    } else {
      secretsMerged.put(it.replace("${secretsPrefix}-${environment}-",""),rawSecret)
    }
  }
  return secretsMerged
}

void writeVariablesToFile(String filename, Map<String,Object> variablesMap) {
  List<String> variablesList=variablesMap.collect { key, value -> "${key} = ${value}" }
  sh (script: "touch ${filename} && echo '\n' > ${filename}")
  variablesList.each {
    sh (script: "echo '${it}' >> ${filename}")
  }
}

// Retrieving Docker images from ECR

String getLatestImageTag(String prefix, String ecrRepo, String region) {
  List<String> imageTags = getAllImageTagsByPrefix(prefix, ecrRepo, region)
  Map<Integer, String> buildNumberTag = [:]
  Integer maxBuild = 0
  imageTags.each {
    Integer currBuild = it.replace("${prefix}-","").split("-")[0]
    buildNumberTag.put(currBuild, it)
    if (currBuild  > maxBuild) { maxBuild = currBuild}
  }
  return buildNumberTag[maxBuild]
}

List<String> getAllImageTagsByPrefix(String prefix, String ecrRepo, String region) {
  String awsCommand = "aws ecr list-images --repository-name ${ecrRepo} --region ${region} --query imageIds[].imageTag --output text"
  List<String> allImageTags = sh (script: awsCommand, returnStdout: true).split()
  return allImageTags.findAll{it.startsWith(prefix)}
}
