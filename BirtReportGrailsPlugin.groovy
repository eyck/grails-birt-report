class BirtReportGrailsPlugin {
    def version = "3.7.0.1"
    def dependsOn = [:]

    // TODO Fill in these fields
    def author = "Eyck Jentzsch"
    def authorEmail = "eyck@jepemuc.de"
    def title = "Birt Reporting Plugin"
    def description = '''\
This plugin makes it easy to integrate the Birt reporting engine (runtime component) into your Grails application.
Currently BIRT 3.7.0 is used.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/birt-report"

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }
   
    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)		
    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }
	                                      
    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }
	
    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
