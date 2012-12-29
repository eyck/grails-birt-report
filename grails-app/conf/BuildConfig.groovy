grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn"
    repositories {
		  grailsRepo "http://grails.org/plugins"
		  mavenRepo "https://oss.sonatype.org/content/repositories/releases"
    }
    dependencies {
      // specify dependencies here under either 'build', 'compile', 
      // 'runtime', 'test' or 'provided' scopes eg.
   }
}
