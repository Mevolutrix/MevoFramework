package SVCInterface;
import java.util.Map;

/**
 * Common SVC customize handler interface
 * Used to IOC the user defined SVC handler logic module into Http Service handing for "SVC" request
 */
public interface ISvcHandler {
    public String getRequest(String funcName,Map<String,String> params,ISvcHelper serviceHelper);
    public String postRequest(String funcName,Map<String,String> params,String jsonBody,ISvcHelper serviceHelper);
}