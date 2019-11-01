timeout(time: 90, unit: 'MINUTES') {
    dir ("milvus") {
        checkout([$class: 'GitSCM', branches: [[name: "${SEMVER}"]], userRemoteConfigs: [[url: "https://github.com/milvus-io/milvus.git", name: 'origin', refspec: "+refs/heads/${SEMVER}:refs/remotes/origin/${SEMVER}"]]])
    }
    dir ("milvus/tests/milvus_python_test") {
        sh 'python3 -m pip install -r requirements.txt'
        sh "pytest . --alluredir=\"test_out/dev/single/sqlite\" --ip ${env.PIPELINE_NAME}-${env.BUILD_NUMBER}-single-gpu-milvus-gpu-engine.milvus.svc.cluster.local"
    }
    // mysql database backend test
    load "${env.WORKSPACE}/ci/jenkins/jenkinsfile/cleanupSingleDev.groovy"

    if (!fileExists('milvus-helm')) {
        dir ("milvus-helm") {
            checkout([$class: 'GitSCM', branches: [[name: "0.5.0"]], userRemoteConfigs: [[url: "https://github.com/milvus-io/milvus-helm.git", name: 'origin', refspec: "+refs/heads/0.5.0:refs/remotes/origin/0.5.0"]]])
        }
    }
    dir ("milvus-helm") {
        dir ("milvus-gpu") {
            sh "helm install --wait --timeout 300 --set engine.image.repository=registry.zilliz.com/milvus/engine-ee --set engine.image.tag=${DOCKER_VERSION} --set expose.type=clusterIP --name ${env.PIPELINE_NAME}-${env.BUILD_NUMBER}-single-gpu -f ci/db_backend/mysql_values.yaml --namespace milvus ."
        }
    }
    dir ("milvus/tests/milvus_python_test") {
        sh "pytest . --alluredir=\"test_out/dev/single/mysql\" --ip ${env.PIPELINE_NAME}-${env.BUILD_NUMBER}-single-gpu-milvus-gpu-engine.milvus.svc.cluster.local"
    }
}
