class BirtReportGrailsPlugin {
	def version = "4.3.0.2"
	def grailsVersion = "2.0 > *"
	def author = "Eyck Jentzsch"
	def authorEmail = "eyck@jepemuc.de"
	def title = "Birt Reporting Plugin"
	def description = 'Makes it easy to integrate the Birt reporting engine (runtime component) into your Grails application. Currently BIRT 4.3.0 is used.'
	def documentation = "http://grails.org/plugin/birt-report"

	def license = 'APACHE'
	def issueManagement = [system: 'JIRA', url: 'https://github.com/eyck/grails-birt-report/issues']
	def scm = [url: 'https://github.com/eyck/grails-birt-report']

	def pluginExcludes = [
		'lib/**',
		'web-app/**'
	]
}
