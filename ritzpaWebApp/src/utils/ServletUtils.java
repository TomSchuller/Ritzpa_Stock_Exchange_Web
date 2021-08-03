package utils;

import engine.Engine;
import engine.EngineImpl;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static constants.Constants.INT_PARAMETER_ERROR;

public class ServletUtils {
    private static final String RITZPA_ENGINE_ATTRIBUTE_NAME = "ritzpaEngine";

    private static final Object ritzpaLock = new Object();

    public static EngineImpl getEngine (ServletContext servletContext) {
        // singelton
        synchronized (ritzpaLock) {
            if (servletContext.getAttribute(RITZPA_ENGINE_ATTRIBUTE_NAME) == null) {
                servletContext.setAttribute(RITZPA_ENGINE_ATTRIBUTE_NAME, new EngineImpl());
            }
        }
        //return ritzpa.getUserNames();
        //maybe return Engine
        return (EngineImpl) servletContext.getAttribute(RITZPA_ENGINE_ATTRIBUTE_NAME);
    }

    public static int getIntParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException numberFormatException) {
            }
        }
        return INT_PARAMETER_ERROR;
    }


}
