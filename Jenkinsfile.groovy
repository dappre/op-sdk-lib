#!/usr/bin/env groovy

def update='minor'
def branch='master'
def release=false
def project='op-sdk-lib'

echo ${env.BUILD_URL}

node {
    try {

        withEnv(["PATH+MAVEN=${tool 'maven'}/bin", "JAVA_HOME=${tool 'jdk1.8.0_latest'}"]) {
            // Mark the code checkout 'stage'....
            stage 'Checkout'
            // Get some code from a GitHub repository
            git url: 'https://github.com/digital-me/${project}.git'
            
            stage 'Find new version'
            echo "${findVersion(project)}"
            echo "${env.GIT_URL}"
         
            // Mark the code build 'stage'....
            stage 'Build'
            // Run the maven build
            sh "mvn -Dmaven.test.failure.ignore clean package"
        }
     } catch (e) {
         // TODO [FV 20161104] Send XMPP message here
         throw e;
     } finally {
         step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
     }
}

def findVersion(project) {
    def url1 = "https://api.github.com/repos/digital-me/${project}/tags".toURL()
    def tagPrefix = "rel-"
    def min = new Tuple(0,0,12) // for bootstrapping
    def json = new groovy.json.JsonSlurper().parseText(url1.text);
    return json.collect {
        // println it
        def matcher = ( it.name =~ /${tagPrefix}([0-9]+).([0-9]+).([0-9]+)$/ )
        if (!matcher.find()) {
        // always have somthing in the result list,
        // if there are no items in the list, max will fail
        return min;
        }
        // else
        return new Tuple(
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        Integer.parseInt(matcher.group(3)))
    }
    .plus(min)
    .max {t1, t2 ->
        if (t1[0].compareTo(t2[0]) == 0){
            if (t1[1].compareTo(t2[1]) == 0) {
                return t1[2].compareTo(t2[2]);
            }
            return t1[1].compareTo(t2[1]);
        }
        return t[0].compareTo(t2[0]);
    }
}

