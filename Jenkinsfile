#!/usr/bin/env groovy

def config = updateConfig {
    update = 'micro';             // needs to be set here in the source
}

node {
    withEnv(["PATH+MAVEN=${tool 'maven'}/bin", "JAVA_HOME=${tool 'jdk1.8.0_latest'}"]) {
        stage ("Build") {
            getCleanSource(config);
            updateMvnVersionFromGitTag(config);
            // updateNlMvnDependencies(config);
            buildAndDeploy(config);
            // tagRelease(this, config);
        }
    }
}

def tagRelease(script, config) {
    script.stage('Tag release') {
        if (config['release']) {
            def ver = "${config['branch']}-${config['newVersion']}"
            script.sh "git tag -a '${ver}' -m 'Release tag by Jenkins'"
            script.sshagent([config['credid']]) {
                script.sh "git -c core.askpass=true push origin '${ver}'"
            }
        }
    }

}


