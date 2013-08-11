grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

    inherits 'global'
    log 'info'//'warn'

    repositories {
	// remoced as http://repo.grails.org/grails/core/org/eclipse/birt/runtime/org.apache.poi/3.9.0.v201303080712/org.apache.poi-3.9.0.v201303080712.jar is not valid
        // grailsCentral()
	mavenCentral()
        mavenRepo "http://www.it-jw.com/maven"
    }

    dependencies {
        compile 'org.eclipse.birt.runtime:org.eclipse.birt.runtime:4.3.0a'
    }

    plugins {
        build ':release:2.2.1', {
            export = false
        }
    }
}
