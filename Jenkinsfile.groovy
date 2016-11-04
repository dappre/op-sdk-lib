#!/usr/bin/env groovy

def update='micro'
def branch='master'
def release=false
def project='op-sdk-lib'
def tagPrefix='rel-'

node {
    try {
        withEnv(["PATH+MAVEN=${tool 'maven'}/bin", "JAVA_HOME=${tool 'jdk1.8.0_latest'}"]) {
            stage('Clean') {
                sh "mvn clean"
            }
            
            stage ('Set new version') {
                def currVersion=sh script: 'tmp=\$(git tag -l  "rel-*" | cut -d\'-\' -f2- | sort -r -V | head -n1);echo \${tmp:-\'rel-0.0.12\'}', returnStdout: true
                def newVersion = newVersion(tagPrefix, update, currVersion);
                echo "current version is ${currVersion}, new version will be ${newVersion}"
                sh "mvn -DnewVersion=$newVersion versions:set"
            }
            
            stage('Build') {
                sh "mvn -Dmaven.test.failure.ignore install"
            }
            
            stage('Deploy') {
                sh "mvn deploy"
            }
        }
     } catch (e) {
         // TODO [FV 20161104] Send XMPP message here
         throw e;
     } finally {
         step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
     }
}

@NonCPS
def newVersion(tagPrefix, update, currVersion) {
    println "${tagPrefix} - ${update} - ${currVersion}"
    if (currVersion.length() < tagPrefix.length() + 5)  {
        throw new IllegalArgumentException("${currVersion} is too short for prefix ${tagPrefix}")
    }
    def parts = currVersion.substring(tagPrefix.length()).split('\\.')
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
    println result
    return result
}

