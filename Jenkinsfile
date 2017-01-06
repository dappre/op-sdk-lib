#!/usr/bin/env groovy

def config = updateConfig {
    update = 'micro';             // needs to be set here in the source
}

def nightly = config['nightly'];
def release = config['release'];

node {
    stage ("Build") {
        echo "Cleaning dir and getting source"
        getCleanGitSource(config);
        echo "Getting latest version from Git and updating the pom accordingly"
        updateMvnVersionFromGitTag(config);
        echo "Building maven project and deploying to Artifactory"
        buildMvnAndDeploy(config);
        echo "Done build stage"
    }
    
    if (release) {
        stage ("Tag") {
            echo "Starting tagging"
            tagGit(config);
            // TODO [FV 20170103] Deploy to public artifatory
        }
    }
}





