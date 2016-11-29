#!/usr/bin/env groovy

// This file is used in multi branch jenkins build, so this will build the snapshot.
// With multi branch builds we cannot set parameters.

// build/feature specific (most likely to change)
def branch='master'           // can we get this as a parameter?
def release=false             // by default false; true if parameter

// project specific settings 
def update="micro"            // needs to be set here in the source
def project="op-sdk-lib"      // needs to be set here in the source
def credid="200c2dab-036b-48a3-a824-1f4257be94ff" // jenkins id for deployer key for this project

// calculated settings
def giturl="git@github.com:digital-me/${project}.git"  // NB: this is the format ssh-agent understands
def isMultibranch = env.BRANCH_NAME != null;
if (isMultibranch) {
    println "branch: ${env.BRANCH_NAME}"
    branch=env.BRANCH_NAME;
} else {
    println "not a multibranch"
}


node {
    def newVersion=null
    
    withEnv(["PATH+MAVEN=${tool 'maven'}/bin", "JAVA_HOME=${tool 'jdk1.8.0_latest'}"]) {
        stage('Get clean source') {
            deleteDir()
            git url: giturl, branch: branch
        }

        stage ('Set new version') {
            newVersion = nextVersion(branch, update, release);
            echo "current version is ${currVersion}, new version will be ${newVersion}"
            currentBuild.displayName="#${env.BUILD_NUMBER}: ${newVersion}"
            sh "mvn versions:set -DnewVersion=$newVersion"
        }

        stage('Build & Deploy') {
            sh "mvn install"
            
        	def goals = 'deploy'; //release ? "install org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar org.owasp:dependency-check-maven:check" : 'install';
            def buildInfo = Artifactory.newBuildInfo()
            def server = Artifactory.server('qiy-artifactory@boxtel')
            def artifactoryMaven = Artifactory.newMavenBuild()
            artifactoryMaven.tool = 'maven' // Tool name from Jenkins configuration
            artifactoryMaven.opts = "-DskipTests=true -Djava.io.tmpdir=/opt/tmp"
            artifactoryMaven.deployer releaseRepo:'Qiy', snapshotRepo:'Qiy', server: server
            artifactoryMaven.resolver releaseRepo:'libs-releases', snapshotRepo:'libs-snapshots', server: server
            artifactoryMaven.run pom: 'pom.xml', goals: goals, buildInfo: buildInfo
            junit testResults: '**/target/surefire-reports/*.xml'
            step ([$class: 'DependencyCheckPublisher'])
        }

        stage('Tag release') {
            if (release) {
                sh "git tag -a '${branch}-${newVersion}' -m 'Release tag by Jenkins'"
                sshagent([credid]) {
                    sh "git -c core.askpass=true push origin '${branch}-${newVersion}'"
                }
            }
        }
    }
}

@NonCPS
def nextVersion(branch, update, release) {
    // - ask Git for the tags that start with the branch name
    // - keep everything after the first dash
    // - sort it as version numbers, reversed
    // - take the first entry
    // - or 0.0.12 if nothing was found

    println "calculating next version ${update} - ${release}"
    def currVersion=sh (script: "git tag -l  '${branch}-*' | cut -d'-' -f2- | sort -r -V | head -n1", returnStdout: true).trim()
    if (currVersion == "") {
        currVersion = sh (script: "git tag -l  'master-*' | cut -d'-' -f2- | sort -r -V | head -n1", returnStdout: true).trim()
    }
    if (currVersion == "") {
        currVersion = "0.0.12"
    }
    
    if (currVersion.length() < 5)  {
        throw new IllegalArgumentException("${currVersion} is too short")
    }
    def parts = currVersion.split('\\.')
    def major = parts[0].toInteger()
    def minor = parts[1].toInteger()
    def micro = parts[2].toInteger()

    switch (update) {
        case 'major':
            major = 1+major;
            minor = 0;
            micro = 0;
            break;
        case 'minor':
            minor = 1+minor;
            micro = 0;
            break;
        case 'micro':
            micro = 1+micro;
            break;
        default:
            throw new IllegalArgumentException(update + " is not a valid value for update")
    }
    String result = "${major}.${minor}.${micro}";
    //    println result
    return release ? result : "${result}-SNAPSHOT"
}

