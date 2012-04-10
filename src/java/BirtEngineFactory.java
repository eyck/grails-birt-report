import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.engine.api.*;
import org.apache.log4j.Logger;

import java.util.logging.Level;

public class BirtEngineFactory {

    private static Logger log = Logger.getLogger(BirtEngineFactory.class);

    private static IReportEngineFactory factory = null;
    private static EngineConfig engineConfig;
    private static IReportEngine birtEngine = null;

    public static synchronized void init(EngineConfig config) {
        if (birtEngine == null) {
            try {
                engineConfig = config;
                Platform.startup(config);
                factory = (IReportEngineFactory) Platform.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
                birtEngine = factory.createReportEngine(engineConfig);
            } catch (BirtException e) {
                log.error("BIRT Platform Startup threw an exception:" +
                        e.getMessage() +
                        " from " +
                        e.getPluginId() +
                        ", reporting will be disabled!");
            }
        }
    }

    public static synchronized IReportEngine getEngine() {
        return birtEngine;
    }

    public static synchronized void destroy() {
        if (birtEngine != null) birtEngine.destroy();
        Platform.shutdown();
        engineConfig=null;
        factory = null;
        birtEngine = null;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public IRenderOption createRenderOption() {
        return new RenderOption();
    }
}