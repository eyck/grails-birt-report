package com.itjw.grails.birt
import grails.util.Environment
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants

import java.util.logging.Level

import javax.servlet.ServletContext
import javax.servlet.ServletException

import org.apache.log4j.Logger
import org.eclipse.birt.core.data.DataTypeUtil
import org.eclipse.birt.core.exception.BirtException
import org.eclipse.birt.core.framework.PlatformFileContext
import org.eclipse.birt.data.engine.api.DataEngine
import org.eclipse.birt.report.engine.api.*
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils as RCU

class BirtReportService implements InitializingBean, ApplicationContextAware {

    static final Logger log = Logger.getLogger(BirtReportService)

    private final String REPORT_EXT = ".rptdesign"

    private static String supportedImageFormats = "PNG;GIF;JPG;BMP"

    private boolean svgEnabled = false

    static transactional = false

    ApplicationContext applicationContext

    def dataSource

    def useGrailsDatasource

    def grailsApplication
    // where are the reports located
    def reportHome
    // at which URL are the reports generated
    def baseURL
    // if the generated base url should be absolute
    def generateAbsoluteBaseURL = false
    // at which URL are the images accessible
    def baseImageURL
    // what is the image dir on disc
    def imageDir

    def defaultFormat = "inline"

    void afterPropertiesSet() {
        ServletContext sc = applicationContext?.servletContext
        if (!sc) {
            log.error "Could not derive servlet context, report generation disabled"
            return
        }
        reportHome = sc.getRealPath("/Reports")
        def birtConfig = grailsApplication.config.birt
        if (birtConfig.reportHome) {
            if (birtConfig.reportHome[0] == '/' || birtConfig.reportHome[1] == ':') {
                reportHome = birtConfig.reportHome
            } else {
                reportHome = sc.getRealPath(birtConfig.reportHome)
            }
        }
        log.info "reportHome is ${reportHome}"
        if(birtConfig.useGrailsDatasource)
            useGrailsDatasource = true
        log.info "${useGrailsDatasource?'':'not '}using grails data source"
        if (birtConfig.generateAbsoluteBaseURL)
            generateAbsoluteBaseURL = true
        if (birtConfig.baseUrl) {
            baseURL = birtConfig.baseUrl
            log.info "baseURL is ${baseURL}"
        } else {
            log.info "generated baseURL will ${generateAbsoluteBaseURL?'':'not '}be absolute"
        }
        baseImageURL = sc.contextPath + "/images/" + "rpt-img"
        imageDir = sc.getRealPath("/images/" + "rpt")
        if (birtConfig.imageUrl) {
            if (birtConfig.imageUrl[0] == '/'){
                baseImageURL = sc.contextPath + birtConfig.imageUrl
                imageDir = sc.getRealPath(birtConfig.imageUrl)
            } else {
                baseImageURL = sc.contextPath + "/" + birtConfig.imageUrl
                imageDir = sc.getRealPath("/" + birtConfig.imageUrl)
            }
        }
        def imgDir = new File(imageDir)
        if (!imgDir.exists() && !imgDir.mkdirs()) {
            log.error "Could not create report image directory, report generation disabled"
            return
        }
        log.info "baseImageUrl is ${baseImageURL} and points to ${imageDir}"

        System.setProperty( "RUN_UNDER_ECLIPSE", "false" )
        HTMLServerImageHandler imageHandler = new HTMLServerImageHandler()
        // for file based output
        // HTMLCompleteImageHandler imageHandler = new HTMLCompleteImageHandler()
        HTMLActionHandler actionHandler = new GrailsHTMLActionHandler(baseURL, defaultFormat)
        HTMLRenderOption renderOption = new HTMLRenderOption()
        renderOption.imageHandler = imageHandler
        renderOption.actionHandler = actionHandler
        // appContext[EngineContants.APPCONTEXT_CHART_RESOLUTION] = myvalue
        // Create the engineConfig for the report generator
        def engineConfig = new EngineConfig()
        engineConfig.appContext = [(DataEngine.MEMORY_BUFFER_SIZE): birtConfig.cacheSize ?: 100] // default Cache size 100MB
        engineConfig.engineHome = ""
        engineConfig.platformContext = new PlatformFileContext(engineConfig)
        engineConfig.setLogConfig(null, Environment.isDevelopmentMode()?Level.ALL:Level.SEVERE)
        engineConfig.setEmitterConfiguration(RenderOption.OUTPUT_FORMAT_HTML, renderOption)
        BirtEngineFactory.init(engineConfig)
    }

    /**
     *  Returns a list of available reports in the reportHome directory. The list contains Maps of property
     *  name/value pairs. The properties contain the BIRT standart properties:<ul>
     *  <li>IReportRunnable.AUTHOR</li>
     *  <li>IReportRunnable.BASE_PROP</li>
     *  <li>IReportRunnable.COMMENTS</li>
     *  <li>IReportRunnable.CREATEDBY</li>
     *  <li>IReportRunnable.DESCRIPTION</li>
     *  <li>IReportRunnable.HELP_GUIDE</li>
     *  <li>IReportRunnable.REFRESH_RATE</li>
     *  <li>IReportRunnable.TITLE</li>
     *  <li>IReportRunnable.UNITS</li></ul>
     *  and 3 custom ones:<ul>
     *  <li>report name</li>
     *  <li>report design name</li>
     *  <li>absolute file name (including full path)</li></ul>
     *
     *  @return List<Map>
     */
    def listReports() {
        return listReports(null)
    }

    /**
     *  Returns a list of available reports in the reportHome directory. The list contains Maps of property
     *  name/value pairs. The properties contain the BIRT standart properties:<ul>
     *  <li>IReportRunnable.AUTHOR</li>
     *  <li>IReportRunnable.BASE_PROP</li>
     *  <li>IReportRunnable.COMMENTS</li>
     *  <li>IReportRunnable.CREATEDBY</li>
     *  <li>IReportRunnable.DESCRIPTION</li>
     *  <li>IReportRunnable.HELP_GUIDE</li>
     *  <li>IReportRunnable.REFRESH_RATE</li>
     *  <li>IReportRunnable.TITLE</li>
     *  <li>IReportRunnable.UNITS</li></ul>
     *  as well as 3 custom ones:<ul>
     *  <li>report name</li>
     *  <li>report design name</li>
     *  <li>absolute file name (including full path)</li></ul>
     *  and user properties which are specified by the userProps
     *
     *  @param userProps
     *  @return List<Map>
     */
    def listReports(userProps) {
        log.trace "Function: listReports()"
        def reports = []
        if (reportHome) {
            File reportDir = new File(reportHome,)
            def files = reportDir?.list().grep { it ==~ /.*\.rptdesign/ }
            files.each {
                def name = it.replace(REPORT_EXT, '')
                def prop = getReportProperties(name, userProps)
                prop["name"] = name
                prop["file"] = it
                prop["fullfile"] = reportDir.absolutePath + reportDir.separator + it
                reports << prop
            }
        }
        return reports
    }

    /**
     *  Returns a map containing the properties of a report design as
     *  name/value pairs. The properties contain the BIRT standart properties:<ul>
     *  <li>IReportRunnable.AUTHOR</li>
     *  <li>IReportRunnable.BASE_PROP</li>
     *  <li>IReportRunnable.COMMENTS</li>
     *  <li>IReportRunnable.CREATEDBY</li>
     *  <li>IReportRunnable.DESCRIPTION</li>
     *  <li>IReportRunnable.HELP_GUIDE</li>
     *  <li>IReportRunnable.REFRESH_RATE</li>
     *  <li>IReportRunnable.TITLE</li>
     *  <li>IReportRunnable.UNITS</li></ul>
     *  and 3 custom ones:<ul>
     *  <li>report name</li>
     *  <li>report design name</li>
     *  <li>absolute file name (including full path)</li></ul>
     *
     *  @param reportName
     *  @return List<Map>
     */
    def getReportProperties(reportName) {
        return getReportProperties(reportName, null)
    }

    /**
     *  Returns a map containing the properties of a report design as
     *  name/value pairs. The properties contain the BIRT standart properties:<ul>
     *  <li>IReportRunnable.AUTHOR</li>
     *  <li>IReportRunnable.BASE_PROP</li>
     *  <li>IReportRunnable.COMMENTS</li>
     *  <li>IReportRunnable.CREATEDBY</li>
     *  <li>IReportRunnable.DESCRIPTION</li>
     *  <li>IReportRunnable.HELP_GUIDE</li>
     *  <li>IReportRunnable.REFRESH_RATE</li>
     *  <li>IReportRunnable.TITLE</li>
     *  <li>IReportRunnable.UNITS</li></ul>
     *  and 3 custom ones:<ul>
     *  <li>report name</li>
     *  <li>report design name</li>
     *  <li>absolute file name (including full path)</li></ul>
     *  and user properties which are specified by the userProps
     *
     *  @param reportName
     *  @param userProps
     *  @return List<Map>
     */
    def getReportProperties(reportName, userProps) {
        getReportProperties(reportName, null, userProps)
    }
    /**
     *  Returns a map containing the properties of a report design as
     *  name/value pairs. The properties contain the BIRT standart properties:<ul>
     *  <li>IReportRunnable.AUTHOR</li>
     *  <li>IReportRunnable.BASE_PROP</li>
     *  <li>IReportRunnable.COMMENTS</li>
     *  <li>IReportRunnable.CREATEDBY</li>
     *  <li>IReportRunnable.DESCRIPTION</li>
     *  <li>IReportRunnable.HELP_GUIDE</li>
     *  <li>IReportRunnable.REFRESH_RATE</li>
     *  <li>IReportRunnable.TITLE</li>
     *  <li>IReportRunnable.UNITS</li></ul>
     *  and 3 custom ones:<ul>
     *  <li>report name</li>
     *  <li>report design name</li>
     *  <li>absolute file name (including full path)</li></ul>
     *  and user properties which are specified by the userProps
     *
     *  @param reportName
     *  @param inputStream the input stream containing the report, optionally null
     *  @param userProps
     *  @return List<Map>
     */
    def getReportProperties(reportName, inputStream, userProps) {
        log.trace "Function: getReportProperties(${reportName}, ${inputStream}, ${userProps})"
        def props = [:]
        def reportFileName = reportHome + File.separator + reportName + REPORT_EXT
        def propnames = userProps ? userProps.keySet() : []
        propnames += [IReportRunnable.AUTHOR, IReportRunnable.BASE_PROP, IReportRunnable.COMMENTS, IReportRunnable.CREATEDBY, IReportRunnable.DESCRIPTION, IReportRunnable.HELP_GUIDE, IReportRunnable.REFRESH_RATE, IReportRunnable.TITLE, IReportRunnable.UNITS]
        try {
            //Open report design
            IReportRunnable design = inputStream?
                BirtEngineFactory.engine?.openReportDesign(reportName, inputStream):
                BirtEngineFactory.engine?.openReportDesign(reportFileName)
            if (!design) return props
            propnames.each {
                def prop = design.getProperty(it)
                if (prop) // property is defined by report
                    props[it] = prop
                else if (userProps && userProps.containsKey(it)) { // property value is default
                    props[it] = userProps[it]
                }
            }
            log.debug "Reportparams:${props}"
            return props
        } catch (Exception e) {
            log.error("Exception occured while getReportProperties: ${e.message}", e)
            throw new ServletException(e)
        }
    }

    /**
     *  Extracts the report parameters of a report design. The returned list contains a map for each parameter
     *  containing:<ul>
     *  <li>name</li>
     *  <li>type</li>
     *  <li>controlType</li>
     *  <li>defaultVal</li>
     *  <li>helpText</li>
     *  <li>promptText</li>
     *  <li>allowBlank</li>
     *  <li>listEntries (a list conaining the possibe values for restricted types)</li></ul>
     *
     *  @param reportName
     *  @return List
     */
    def getReportParams(reportName) {
        getReportParams(reportName, null)
    }
    /**
     *  Extracts the report parameters of a report design. The returned list contains a map for each parameter
     *  containing:<ul>
     *  <li>name</li>
     *  <li>type</li>
     *  <li>controlType</li>
     *  <li>defaultVal</li>
     *  <li>helpText</li>
     *  <li>promptText</li>
     *  <li>allowBlank</li>
     *  <li>listEntries (a list conaining the possibe values for restricted types)</li></ul>
     *
     *  @param reportName
     *  @param inputStream the input stream containing the report, optionally null
     *  @return List
     */
    def getReportParams(reportName, inputStream) {
        log.trace "Function: getReportParams(${reportName})"
        def reportParams = []
        def reportFileName = reportHome + File.separator + reportName + REPORT_EXT
        if (!new File(reportFileName).exists()) return reportParams
        try {
            //Open report design
            // def engine = BirtEngineFactory.engine
            if (!BirtEngineFactory.engine) return reportParams
            IReportRunnable design = inputStream?
                BirtEngineFactory.engine?.openReportDesign(reportName, inputStream):
                BirtEngineFactory.engine?.openReportDesign(reportFileName)
            IGetParameterDefinitionTask task = BirtEngineFactory.engine.createGetParameterDefinitionTask(design)
            task.locale=getLocale()
            if(useGrailsDatasource) task.getAppContext().put("OdaJDBCDriverPassInConnection", dataSource.getConnection())
            // Iterate over all parameters, Don't report about groups
            task.getParameterDefns(false).each {param ->
                //Group section found
                if (!(param instanceof IParameterGroupDefn)) { //Groups are not supported
                    //Parameters are not in a group
                    def listentries = []
                    if (param.controlType == IScalarParameterDefn.LIST_BOX || param.controlType == IScalarParameterDefn.RADIO_BUTTON) {
                        //Parameter is a List Box
                        task.getSelectionList(param.name)?.each {
                            //Print out the selection choices
                            def selectionItem = (IParameterSelectionChoice) it
                            def value = selectionItem.value
                            def label = selectionItem.label
                            // log.debug label + "--" + value
                            listentries << ['label': label ?: value, 'value': value]
                        }
                    }
                    reportParams << ['name': param.name,
                            'type': param.dataType,
                            'paramType': param.scalarParameterType,
                            'controlType': param.controlType,
                            'defaultVal': task.getDefaultValue(param),
                            'helpText': param.helpText,
                            'promptText': param.promptText,
                            'allowBlank': param.allowBlank(),
                            'listEntries': listentries
                    ]
                }
            }
            task.close()
        } catch (Exception e) {
            log.error("Exception occured while getReportParams: ${e.message}", e)
            throw new ServletException(e)
        }
        return reportParams
    }

    /**
     *  Extracts the parameter names of a report design as a list of Strings
     *
     *  @param reportName
     *  @return List
     */
    def getReportParamNames(reportName) {
        log.trace "Function: getReportParamNames(${reportName})"
        def params = getReportParams(reportName)
        return params.name
    }

    /**
     *  Extracts the parameter names of a report design as a list of Strings
     *
     *  @param reportName
     *  @param inputStream the input stream containing the report, optionally null
     *  @return List
     */
    def getReportParamNames(reportName, inputStream) {
        log.trace "Function: getReportParamNames(${reportName})"
        def params = getReportParams(reportName, inputStream)
        return params.name
    }

    /**
     * Creates a renderOption for the given format. Supported values are 'inline' (html fragment),
     * 'html', 'pdf', 'xls', 'doc', 'ppt', 'odt', 'ods', 'odp'
     *
     * @param format
     * @return IRenderOption
     */
    def getRenderOption(format) {
        return getRenderOption(null, format)
    }

    /**
     * Creates a renderOption for the given HTTPServletRequest and format. Supported values for the format are
     * 'inline' (html fragment), 'html', 'pdf', 'xls', 'doc', 'ppt', 'odt', 'ods', 'odp'
     *  The request is used to derive the appropriate url used in hyperlinks within reports (e.g. drill-downs).
     *  This method allows to enable/disable the use of SVG on a per-request basis (the setting will persist until
     *  it is changed)
     *
     * @param request
     * @param format
     * @param genSVG
     * @return IRenderOption
     */
    def getRenderOption(request, format, genSVG) {
        svgEnabled = genSVG
        return getRenderOption(request, format)
    }

    /**
     * Creates a renderOption for the given HTTPServletRequest and format. Supported values for the format are
     * 'inline' (html fragment), 'html', 'pdf', 'xls', 'doc', 'ppt', 'odt', 'ods', 'odp'
     *  The request is used to derive the appropriate url used in hyperlinks within reports (e.g. drill-downs).
     *
     * @param request
     * @param format
     * @return IRenderOption
     */
    def getRenderOption(request, format) {
        log.trace "Function: getRenderOption(${request}, ${format})"
        IRenderOption options = new RenderOption()
        // set an absolute url to be used in output
        if (baseURL =~ /^\w+:\/\//) { // we have a complete URL
            options.baseURL = baseURL
        } else if(request != null) {
            if(generateAbsoluteBaseURL){
                // add the protocol/host/port part of the URL
                options.baseURL = "${request.getScheme()}://${request.getServerName()}:${request.getServerPort()}"
                // append application context path either as absolute path or as relative
                options.baseURL += baseURL[0] == "/" ? baseURL : request.getContextPath() + "/" + baseURL
            } else
                options.baseURL = request.contextPath
        } else {
            options.baseURL="/"
        }
        options.actionHandler = new GrailsHTMLActionHandler(options.baseURL, format?:defaultFormat)
        options.outputFormat = format?:"html"
        switch (options.outputFormat.toLowerCase()) {
            case "html":
                HTMLRenderOption htmlOptions = new HTMLRenderOption(options)
                htmlOptions.htmlPagination = false
                htmlOptions.embeddable = false
                htmlOptions.baseImageURL = baseImageURL
                htmlOptions.imageDirectory = imageDir
                htmlOptions.supportedImageFormats = supportedImageFormats + (svgEnabled ? ";SVG" : "")
                return htmlOptions
            case "pdf":
                PDFRenderOption pdfOptions = new PDFRenderOption(options)
                // pdfOptions.setOption(IPDFRenderOption.PAGE_OVERFLOW, IPDFRenderOption.FIT_TO_PAGE_SIZE)
                pdfOptions.setOption(IPDFRenderOption.PAGE_OVERFLOW, IPDFRenderOption.OUTPUT_TO_MULTIPLE_PAGES)
                pdfOptions.supportedImageFormats = supportedImageFormats + (svgEnabled ? ";SVG" : "")
                return pdfOptions
            default:
                return options
        }
    }

    private Object getParamValue(def dataType, def param) {
        switch (dataType) {
            case IScalarParameterDefn.TYPE_BOOLEAN:
                return  DataTypeUtil.toBoolean(param); break
            case IScalarParameterDefn.TYPE_DATE:
                return DataTypeUtil.toSqlDate(param); break
            case IScalarParameterDefn.TYPE_TIME:
                return DataTypeUtil.toSqlTime(param); break
            case IScalarParameterDefn.TYPE_DATE_TIME:
                return DataTypeUtil.toDate(param); break
            case IScalarParameterDefn.TYPE_DECIMAL:
                return DataTypeUtil.toBigDecimal(param); break
            case IScalarParameterDefn.TYPE_FLOAT:
                return DataTypeUtil.toDouble(param); break
            case IScalarParameterDefn.TYPE_STRING:
                return DataTypeUtil.toString(param); break
            case IScalarParameterDefn.TYPE_INTEGER:
                return DataTypeUtil.toInteger(param); break
            default: throw new RuntimeException("Unsupported dataType = " + dataType + ", param " + param)
        }
    }

    private getReportBirtParams(Map params, IReportRunnable runnable) {
        log.trace "Function: getReportBirtParams(${params}, ${runnable})"
        try {
            //get parameter definitions
            // def engine = BirtEngineFactory.engine
            if (!BirtEngineFactory.engine) return null
            def task = BirtEngineFactory.engine.createGetParameterDefinitionTask(runnable)
            task.locale=getLocale()
            def paramDefs = task.getParameterDefns(false)
            //iterate over each parameter definition, updating as appropriate
            //from the supplied ReportAttributes object
            def paramMap = new HashMap()
            paramDefs.each {
                def paramName = it.name
                def paramVal
                if (params.containsKey(paramName)) {
                    def value = params[paramName];
                    if(value != null) {
                        if(it.scalarParameterType.equals(DesignChoiceConstants.SCALAR_PARAM_TYPE_MULTI_VALUE)) {
                            for(int i = 0; i < value.size(); i++) {
                                value[i] = getParamValue(it.dataType, value[i])
                            }
                            paramVal = value
                        } else {
                            paramVal = getParamValue(it.dataType, value)
                        }
                    }
                }
                if (paramVal != null) paramMap[paramName] = paramVal
            }
            return paramMap
        } catch (BirtException e) {
            // log.error("BIRT Exception occured while getReportBirtParams: ${e.message}", e)
            throw new Exception(e.message)
        }
    }

    /**
     * Get the locale of the request or as fallback of the host system
     * @return locale
     * */
    def getLocale() {
        Locale locale = null
        try {
            locale = RCU.getLocale(RequestContextHolder.currentRequestAttributes().getSession().request)
        }
        catch(Exception e){
            locale = Locale.getDefault()
        }
        log.debug "locale: ${locale}"
        return locale
    }

    /**
     * Runs and renders a report design into the format specified by renderOptions. Parameters are specified as
     * name/value pairs and will be parsed (by BIRT) into the appropriate format.
     *
     * @param reportName
     * @param parameters
     * @param renderOptions
     * @return ByteArrayOutputStream
     */
    def runAndRender(reportName, parameters, renderOptions, Locale locale = null) {
        runAndRender(reportName, null, parameters, renderOptions, locale)
    }
    /**
     * Runs and renders a report design into the format specified by renderOptions. Parameters are specified as
     * name/value pairs and will be parsed (by BIRT) into the appropriate format.
     *
     * @param reportName
     * @param inputStream the input stream containing the report, optionally null
     * @param parameters
     * @param renderOptions
     * @return ByteArrayOutputStream
     */
    def runAndRender(reportName, inputStream, parameters, renderOptions, Locale locale = null) {
        log.trace "Function: runAndRender(${reportName}, ${parameters}, ${renderOptions})"
        def reportFileName = reportHome + File.separator + reportName + REPORT_EXT
        log.debug "Parameters are ${parameters}"
        // def engine = BirtEngineFactory.engine
        if (!BirtEngineFactory.engine) return null
        //Open report design
        IReportRunnable design = inputStream?
            BirtEngineFactory.engine?.openReportDesign(reportName, inputStream):
            BirtEngineFactory.engine?.openReportDesign(reportFileName)
        //create task to run and render report
        IRunAndRenderTask task = BirtEngineFactory.engine.createRunAndRenderTask(design)
        task.locale=locale?:getLocale()
        def cacheSize = Integer.valueOf(grailsApplication.config.birt.cacheSize ?: 100)
        log.info "Setting memory buffer to ${cacheSize}MB"
        task.appContext.put(DataEngine.MEMORY_BUFFER_SIZE, cacheSize)
        // other options IN_MEMORY_CUBE_SIZE
        def taskParams = getReportBirtParams(parameters, design)
        log.debug "taskParams: ${taskParams}"
        task.setParameterValues(taskParams)
        task.validateParameters()
        ByteArrayOutputStream buf = new ByteArrayOutputStream()
        renderOptions.outputStream = buf
        task.renderOption = renderOptions
        if(useGrailsDatasource) task.getAppContext().put("OdaJDBCDriverPassInConnection", dataSource.getConnection())
        task.run()
        task.close()
        return buf
    }

    /**
     * Runs a report design and generates a reportDocument with the given name. Parameters are specified as
     * name/value pairs and will be parsed (by BIRT) into the appropriate format.
     *
     * @param reportName
     * @param parameters
     * @param reportDocumentName
     */
    def run(reportName, parameters, reportDocumentName, Locale locale = null) {
        run(reportName, parameters, reportDocumentName, locale)
    }

    /**
     * Runs a report design and generates a reportDocument with the given name. Parameters are specified as
     * name/value pairs and will be parsed (by BIRT) into the appropriate format.
     *
     * @param reportName
     * @param inputStream the input stream containing the report, optionally null
     * @param parameters
     * @param reportDocumentName
     */
    def run(reportName, inputStream, parameters, reportDocumentName, Locale locale = null) {
        log.trace "Function: run(${reportName}, ${parameters}, ${reportDocumentName})"
        def reportFileName = reportHome + File.separator + reportName + REPORT_EXT
        log.debug "Parameters are ${parameters}"
        // def engine = BirtEngineFactory.engine
        if (!BirtEngineFactory.engine) return null
        //Open report design
        IReportRunnable design = inputStream?
            BirtEngineFactory.engine?.openReportDesign(reportName, inputStream):
            BirtEngineFactory.engine?.openReportDesign(reportFileName)
        //create task to run and render report
        IRunTask task = BirtEngineFactory.engine.createRunTask(design)
        task.locale=locale?:getLocale()
        def cacheSize = Integer.valueOf(grailsApplication.config.birt.cacheSize ?: 100)
        log.info "Setting memory buffer to ${cacheSize}MB"
        task.appContext.put(DataEngine.MEMORY_BUFFER_SIZE, cacheSize)
        // other options IN_MEMORY_CUBE_SIZE
        def taskParams = getReportBirtParams(parameters, design)
        log.debug "taskParams: ${taskParams}"
        task.parameterValues = taskParams
        if(useGrailsDatasource) task.getAppContext().put("OdaJDBCDriverPassInConnection", dataSource.getConnection())
        task.validateParameters()
        task.run(reportDocumentName)
        task.close()
    }

    /**
     * Renders a report document into the format specified by renderOptions. Parameters are specified as
     * name/value pairs and will be parsed (by BIRT) into the appropriate format.
     *
     * @param reportDocumentName
     * @param parameters
     * @param reportDocumentName
     */
    def render(reportDocumentName, parameters, renderOptions, Locale locale = null) {
        render(reportDocumentName, null, parameters, renderOptions,  locale)
    }
    /**
     * Renders a report document into the format specified by renderOptions. Parameters are specified as
     * name/value pairs and will be parsed (by BIRT) into the appropriate format.
     *
     * @param reportDocumentName
     * @param parameters
     * @param reportDocumentName
     */
    def render(reportDocumentName, inputStream, parameters, renderOptions, Locale locale = null) {
        log.trace "Function: render(${reportDocumentName}, ${renderOptions})"
        if (!BirtEngineFactory.engine) return null
        //Open report design
        IReportDocument design = BirtEngineFactory.engine.openReportDocument(reportDocumentName)
        //create task to run and render report
        IRenderTask task = BirtEngineFactory.engine.createRenderTask(design)
        task.locale=locale?:getLocale()
        if (parameters) task.parameterValues = parameters
        ByteArrayOutputStream buf = new ByteArrayOutputStream()
        renderOptions.outputStream = buf
        task.renderOption = renderOptions
        if(useGrailsDatasource) task.getAppContext().put("OdaJDBCDriverPassInConnection", dataSource.getConnection())
        task.render()
        task.close()
        return buf
    }
}
