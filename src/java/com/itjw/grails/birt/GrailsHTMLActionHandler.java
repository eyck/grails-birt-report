package com.itjw.grails.birt;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.eclipse.birt.report.engine.api.HTMLActionHandler;
import org.eclipse.birt.report.engine.api.IAction;
import org.eclipse.birt.report.engine.api.IReportDocument;
import org.eclipse.birt.report.engine.api.script.IReportContext;

/**
 * HTML action handler for url generation.
 */
class GrailsHTMLActionHandler extends HTMLActionHandler {

    /**
     * URL parameter name that gives the format to display the report, html or
     * pdf.
     */
    public static final String PARAM_FORMAT = "format"; //$NON-NLS-1$

    /**
     * URL parameter name that gives the bookmark expression.
     */
    public static final String PARAM_BOOKMARK = "__bookmark"; //$NON-NLS-1$

    /**
     * URL parameter name that indicate the bookmark is TOC name.
     */
    public static final String PARAM_ISTOC = "__istoc"; //$NON-NLS-1$

    /**
     * UTF-8 encode constants.
     */
    public static final String UTF_8_ENCODE = "UTF-8"; //$NON-NLS-1$

    /**
     * Separator that connects the query parameter values.
     */
    public static final String PARAMETER_SEPARATOR = "&"; //$NON-NLS-1$

    /**
     * The character to start the query string in the url.
     */
    public static final String QUERY_CHAR = "?"; //$NON-NLS-1$

    /**
     * Equals operator.
     */
    public static final String EQUALS_OPERATOR = "="; //$NON-NLS-1$

    /**
     * Document instance.
     */
    protected IReportDocument document;

    /**
     * Locale of the requester.
     */

    protected Locale locale;

    /**
     * Page number of the action requester.
     */

    protected long page = -1;

    /**
     * if the page is embedded, the bookmark should always be a url to submit.
     */
    protected boolean isEmbeddable = false;

    /**
     * RTL option setting by the command line or URL parameter.
     */

    protected boolean isRtl = false;

    /**
     * if wanna use the master page, then set it to true.
     */

    protected boolean isMasterPageContent = true;

    /**
     * the preferred format of the host
     */
    protected String hostFormat;

    /**
     * the base URL for all formats
     */
    protected String baseUrl;

    /**
     * Constructor.
     */
    public GrailsHTMLActionHandler() {
    }

    /**
     * Constructor.
     */
    public GrailsHTMLActionHandler(String hostFormat) {
        this.hostFormat = hostFormat;
    }

    /**
     * Constructor.
     */
    public GrailsHTMLActionHandler(String baseUrl, String hostFormat) {
        this.baseUrl = baseUrl;
        this.hostFormat = hostFormat;
    }

    /**
     * Constructor. This is for renderTask.
     *
     * @param document
     * @param page
     * @param locale
     * @param isEmbeddable
     * @param isRtl
     * @param isMasterPageContent
     * @param format
     */

    public GrailsHTMLActionHandler(IReportDocument document, long page, Locale locale, boolean isEmbeddable,
                                   boolean isRtl, boolean isMasterPageContent, String format) {
        this.document = document;
        this.page = page;
        this.locale = locale;
        this.isEmbeddable = isEmbeddable;
        this.isRtl = isRtl;
        this.isMasterPageContent = isMasterPageContent;
        this.hostFormat = format;
    }

    /**
     * Constructor. This is for runAndRender task.
     *
     * @param locale
     * @param isRtl
     * @param isMasterPageContent
     * @param format
     */

    public GrailsHTMLActionHandler(Locale locale, boolean isRtl, boolean isMasterPageContent, String format) {
        this.locale = locale;
        this.isRtl = isRtl;
        this.isMasterPageContent = isMasterPageContent;
        this.hostFormat = format;
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * org.eclipse.birt.report.engine.api.HTMLActionHandler#getURL(org.eclipse
      * .birt.report.engine.api.IAction,
      * org.eclipse.birt.report.engine.api.script.IReportContext)
      */

    @Override
    public String getURL(IAction actionDefn, IReportContext context) {
        if (actionDefn == null) return null;
        String actionString = null;
        switch (actionDefn.getType()) {
            case IAction.ACTION_BOOKMARK: {
                actionString = buildBookmarkAction(actionDefn, context);
                break;
            }
            case IAction.ACTION_HYPERLINK: {
                actionString = actionDefn.getActionString();
                break;
            }
            case IAction.ACTION_DRILLTHROUGH: {
                actionString = buildDrillAction(actionDefn, context);
                break;
            }
        }
        if (baseUrl != null && actionString != null)
            return baseUrl + "/" + actionString;

        return actionString;
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * org.eclipse.birt.report.engine.api.HTMLActionHandler#getURL(org.eclipse
      * .birt.report.engine.api.IAction, java.lang.Object)
      */
    @Override
    public String getURL(IAction actionDefn, Object context) {
        if (actionDefn == null)
            return null;
        if (context instanceof IReportContext)
            return getURL(actionDefn, (IReportContext) context);

        throw new IllegalArgumentException("The context is of wrong type."); //$NON-NLS-1$
    }

    /**
     * Build URL for bookmark.
     *
     * @param action
     * @param context
     * @return the bookmark url
     */

    protected String buildBookmarkAction(IAction action, IReportContext context) {
        // if (action == null || context == null)
        return null;
        //
        // // Get Base URL
        // String baseURL = null;
        // Object renderContext = getRenderContext(context);
        // if (renderContext instanceof HTMLRenderOption) {
        // baseURL = ((HTMLRenderOption) renderContext).getBaseURL();
        // }
        // if (renderContext instanceof PDFRenderOption) {
        // baseURL = ((PDFRenderOption) renderContext).getBaseURL();
        // }
        //
        // if (baseURL == null)
        // return null;
        //
        // // Get bookmark
        // String bookmark = action.getBookmark();
        //
        // if (baseURL.lastIndexOf(IBirtConstants.SERVLET_PATH_FRAMESET) > 0) {
        // // In frameset mode, use javascript function to fire Ajax request to
        // // link to internal bookmark
        //            String func = "catchBookmark('" + htmlEncode(bookmark) + "');"; //$NON-NLS-1$ //$NON-NLS-2$
        //            return "javascript:try{" + func + "}catch(e){parent." + func + "};"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        // } else if (baseURL.lastIndexOf(IBirtConstants.SERVLET_PATH_RUN) > 0)
        // {
        // // In run mode, append bookmark at the end of URL
        //            String func = "catchBookmark('" + bookmark + "');"; //$NON-NLS-1$ //$NON-NLS-2$
        //            return "javascript:try{" + func + "}catch(e){parent." + func + "};"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        // }
        //
        // // Save the URL String
        // StringBuilder link = new StringBuilder();
        //
        // boolean realBookmark = false;
        //
        // if (this.document != null) {
        // long pageNumber = this.document.getPageNumber(action.getBookmark());
        // realBookmark = (pageNumber == this.page && !isEmbeddable);
        // }
        //
        // try {
        // bookmark = URLEncoder.encode(bookmark, UTF_8_ENCODE);
        // } catch (UnsupportedEncodingException e) {
        // // Does nothing
        // }
        //
        // link.append(baseURL);
        // link.append(QUERY_CHAR);
        //
        // // if the document is not null, then use it
        //
        // if (action.getReportName() != null && action.getReportName().length()
        // > 0) {
        // // link.append(PARAM_REPORT);
        // // link.append(EQUALS_OPERATOR);
        // String reportName = getReportName(context, action);
        // try {
        // reportName = URLEncoder.encode(reportName, UTF_8_ENCODE);
        // } catch (UnsupportedEncodingException e) {
        // // do nothing
        // }
        // link.append(reportName);
        // } else {
        // // its an iternal bookmark
        //            return "#" + action.getActionString(); //$NON-NLS-1$
        // }
        //
        // if (realBookmark) {
        //            link.append("#"); //$NON-NLS-1$
        // link.append(bookmark);
        // } else {
        // link.append(getQueryParameterString(PARAM_BOOKMARK, bookmark));
        //
        // // Bookmark is TOC name.
        // if (!action.isBookmark())
        //                link.append(getQueryParameterString(PARAM_ISTOC, "true")); //$NON-NLS-1$
        // }
        //
        // return link.toString();
    }

    /**
     * builds URL for drillthrough action
     *
     * @param action  instance of the IAction instance
     * @param context the context for building the action string
     * @return a URL
     */
    protected String buildDrillAction(IAction action, IReportContext context) {
        if (action == null || context == null)
            return null;

        StringBuilder link = new StringBuilder();
        String reportName = getReportShortName(context, action);

        if (reportName != null && !reportName.equals("")) //$NON-NLS-1$
        {
            try {
                link.append(URLEncoder.encode(reportName, UTF_8_ENCODE) + QUERY_CHAR);
            } catch (UnsupportedEncodingException e1) {
                // It should not happen. Does nothing
            }

            // Adds the parameters
            if (action.getParameterBindings() != null) {
                boolean hasSeparator = false;
                Iterator paramsIte = action.getParameterBindings().entrySet().iterator();
                while (paramsIte.hasNext()) {
                    Map.Entry entry = (Map.Entry) paramsIte.next();
                    try {
                        String key = (String) entry.getKey();
                        Object valueObj = entry.getValue();
                        if (valueObj != null) {
                            // TODO: here need the get the format from the
                            // parameter.
                            String value = DataUtil.getDisplayValue(valueObj);
                            link.append(getQueryParameterString(URLEncoder.encode(key, UTF_8_ENCODE), URLEncoder
                                    .encode(value, UTF_8_ENCODE), hasSeparator));
                            hasSeparator = true;
                        }
                    } catch (UnsupportedEncodingException e) {
                        // Does nothing
                    }
                }

            }

            // add format support
            String format = action.getFormat();
            if (format == null || format.length() == 0)
                format = hostFormat;
//            if (format == null || format.length() == 0)
//                format = HTMLRenderOption.OUTPUT_FORMAT_HTML;
            if (format != null && format.length() > 0) {
                link.append(getQueryParameterString(PARAM_FORMAT, format, true));
            }

            // // add bookmark
            // String bookmark = action.getBookmark();
            // if (bookmark != null) {
            // try {
            // // In pdf format, don't support bookmark as
            // // parameter
            // if (IBirtConstants.PDF_RENDER_FORMAT.equalsIgnoreCase(format)) {
            //                        link.append("#"); //$NON-NLS-1$
            // // use TOC to find bookmark, only link to document file
            //                        if (!action.isBookmark() && reportName.toLowerCase().endsWith(".rptdocument")) //$NON-NLS-1$
            // {
            // InputOptions options = new InputOptions();
            // options.setOption(InputOptions.OPT_LOCALE, locale);
            // // bookmark = BirtReportServiceFactory
            // // .getReportService().findTocByName(
            // // reportName, bookmark, options);
            // }
            // link.append(URLEncoder.encode(bookmark, UTF_8_ENCODE));
            // } else {
            // bookmark = URLEncoder.encode(bookmark, UTF_8_ENCODE);
            // link.append(getQueryParameterString(PARAM_BOOKMARK, bookmark));
            //
            // // Bookmark is TOC name.
            // if (!action.isBookmark())
            //                            link.append(ParameterAccessor.getQueryParameterString(PARAM_ISTOC, "true")); //$NON-NLS-1$
            // }
            //
            // } catch (UnsupportedEncodingException e) {
            // // Does nothing
            // }
            // }
        }

        return link.toString();
    }

    /**
     * Gets the effective report path.
     *
     * @param context
     * @param action
     * @return the effective report path
     */

    private String getReportName(IReportContext context, IAction action) {
        assert context != null;
        assert action != null;
        String reportName = action.getReportName();
        return reportName;
    }

    /**
     * Gets the report file name without suffix.
     *
     * @param context
     * @param action
     * @return the effective report path
     */

    private String getReportShortName(IReportContext context, IAction action) {
        String reportName = getReportName(context, action);
        File f = new File(reportName);
        return f.getName().replaceAll("\\.rptdesign", "");
    }

    public String getQueryParameterString(String paramName, String value) {
        return getQueryParameterString(paramName, value, true);
    }

    public String getQueryParameterString(String paramName, String value, boolean separator) {
        StringBuilder b = new StringBuilder();
        if (separator)
            b.append(PARAMETER_SEPARATOR);
        b.append(paramName);
        b.append(EQUALS_OPERATOR);
        b.append(value);
        return b.toString();
    }

    /**
     * This function is used to encode an ordinary string that may contain
     * characters or more than one consecutive spaces for appropriate HTML
     * display.
     *
     * @param s
     * @return String
     */
    public final String htmlEncode(String s) {
        String sHtmlEncoded = ""; //$NON-NLS-1$

        if (s == null) {
            return null;
        }

        StringBuilder sbHtmlEncoded = new StringBuilder();
        final char chrarry[] = s.toCharArray();

        for (int i = 0; i < chrarry.length; i++) {
            char c = chrarry[i];

            switch (c) {
                case '\t':
                    sbHtmlEncoded.append("&#09;"); //$NON-NLS-1$
                    break;
                case '\n':
                    sbHtmlEncoded.append("<br>"); //$NON-NLS-1$
                    break;
                case '\r':
                    sbHtmlEncoded.append("&#13;"); //$NON-NLS-1$
                    break;
                case ' ':
                    sbHtmlEncoded.append("&#32;"); //$NON-NLS-1$
                    break;
                case '"':
                    sbHtmlEncoded.append("&#34;"); //$NON-NLS-1$
                    break;
                case '\'':
                    sbHtmlEncoded.append("&#39;"); //$NON-NLS-1$
                    break;
                case '<':
                    sbHtmlEncoded.append("&#60;"); //$NON-NLS-1$
                    break;
                case '>':
                    sbHtmlEncoded.append("&#62;"); //$NON-NLS-1$
                    break;
                case '`':
                    sbHtmlEncoded.append("&#96;"); //$NON-NLS-1$
                    break;
                case '&':
                    sbHtmlEncoded.append("&#38;"); //$NON-NLS-1$
                    break;
                case '\\':
                    sbHtmlEncoded.append("&#92;"); //$NON-NLS-1$
                    break;
                case '/':
                    sbHtmlEncoded.append("&#47;"); //$NON-NLS-1$
                break;
            default:
                sbHtmlEncoded.append(c);
            }
        }

        sHtmlEncoded = sbHtmlEncoded.toString();
        return sHtmlEncoded;
    }
}
