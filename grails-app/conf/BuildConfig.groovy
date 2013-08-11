grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        mavenRepo "http://www.it-jw.com/maven"
    }

    dependencies {
        compile 'org.eclipse.birt.runtime:org.eclipse.birt.runtime:4.3.0a'
    }

    plugins {
        build ':release:2.1.0', {
            export = false
        }
    }
}
