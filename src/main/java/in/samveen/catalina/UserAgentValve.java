/*
 * Apache License modified with one extra clause
 *
 * Caveat emptor: All potential profits and losses are YOURS.
 */
package in.samveen.catalina.valves;

import java.io.IOException;

import jakarta.servlet.ServletException;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.StringUtils;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.parser.Host;
import org.apache.catalina.valves.ValveBase;

/**
 * <p>
 * This Valve enables an attribute on a client if the request's header value matches an expected value.
 * </p>
 * <p>
 * If the request http header named <code>$userAgentHeader</code> (default value <code>User-Agent</code>)
 * matches the value given in <code>$triggersOn</code> configuration parameter (default value <code>null</code>),
 * mark the request with the attribute <code>$requestAttributeName</code> (default value
 *  <code>in.samveen.catalina.expectedUserAgent</code>) and value {@code Boolean.TRUE} to
 * indicate that this request has a header that matches an expected value.
 * </p>
 * <table border="1">
 * <caption>Configuration parameters</caption>
 * <tr>
 * <th>UserAgentValve property</th>
 * <th>Description</th>
 * <th>Format</th>
 * <th>Default Value</th>
 * </tr>
 * <tr>
 * <td>userAgentHeader</td>
 * <td>Name of the http header read by this valve that holds the flag that this request</td>
 * <td>Any http header name like <code>User-Agent</code>, <code>Host</code></td>
 * <td><code>User-Agent</code></td>
 * </tr>
 * <tr>
 * <td>triggersOn</td>
 * <td>Value of the <code>userAgentHeader</code> to compare against, to mark the request</td>
 * <td>Comma seperated String like <code>ELB-HealthChecker/2.0,libcurl/7.88.1</code></td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <td>requestAttributeName</td>
 * <td>The attribute used to mark the request when the <code>userAgentHeader</code> contains a matched value.
 * <td>String</td>
 * <td>in.samveen.catalina.expectedUserAgent</td>
 * </tr>
 * </table>
 * <p>
 * This Valve may be attached to any Container, depending on the granularity of the filtering you wish to perform.
 * </p>
 * <hr>
 * <p>
 * <strong>Sample</strong>
 * </p>
 * <p>
 * UserAgentValve configuration:
 * </p>
 * <code>
 * &lt;Valve
 *   className="in.samveen.catalina.valves.UserAgentValve"
 *   userAgentHeader="User-Agent"
 *   triggersOn="ELB-HealthChecker/2.0"
 *   requestAttributeName="ELB.DontLog"
 *   /&gt;</code>
 */
public class UserAgentValve extends ValveBase {
    /**
     * Logger
     */
    private static final Log log = LogFactory.getLog(UserAgentValve.class);

    /**
     * The request attribute set by the UserAgentFilter, identifying whther the value received in the target headera
     * matches the expected value or not. It is typically provided via the User-Agent HTTP header.
     */
    public static final String EXPECTED_UA_ATTRIBUTE = "in.samveen.catalina.expectedUserAgent";

    /**
     * Convert a given comma delimited String into an array of String
     *
     * @param commaDelimitedStrings The string to convert
     *
     * @return array of String (non <code>code</code>)
     */
    protected static String[] commaDelimitedListToStringArray(String commaDelimitedStrings) {
        if (commaDelimitedStrings == null || commaDelimitedStrings.length() == 0) {
            return new String[0];
        }

        String[] splits = commaDelimitedStrings.split(",");
        for (int i = 0; i < splits.length; ++i) {
            splits[i] = splits[i].trim();
        }
        return splits;
    }

    /**
     * @see #setRequestAttributeName(String)
     */
    private String requestAttributeName = EXPECTED_UA_ATTRIBUTE;

    /**
     * @see #setRequestAttributesEnabled(boolean)
     */
    private boolean requestAttributesEnabled = true;

    /**
     * @see #setTriggersOn(String)
     */
    private String triggersOn = null;

    /**
     * @see #setUserAgentHeader(String)
     */
    private String userAgentHeader = "User-Agent";


    /**
     * Default constructor that ensures {@link ValveBase#ValveBase(boolean)} is called with <code>true</code>.
     */
    public UserAgentValve() {
        // Async requests are supported with this valve
        super(true);

        if (log.isTraceEnabled()) {
            log.trace("UserAgentValve: Initialization complete");
        }
    }

    /**
     * @see #setTriggersOn(String)
     *
     * @return the trigger values (e.g. "ELB-HealthChecker/2.0,libcurl/7.88.1")
     */
    public String getTriggersOn() {
        return triggersOn;
    }

    /**
     * @see #setRequestAttributeName(boolean)
     *
     * @return <code>true</code> if the attributes will be logged, otherwise <code>false</code>
     */
    public String getRequestAttributeName() {
        return requestAttributeName;
    }

    /**
     * @see #setRequestAttributesEnabled(boolean)
     *
     * @return <code>true</code> if the attributes will be logged, otherwise <code>false</code>
     */
    public boolean getRequestAttributesEnabled() {
        return requestAttributesEnabled;
    }

    /**
     * @see #setUserAgentHeader(String)
     *
     * @return the protocol header (e.g. "User-Agent")
     */
    public String getUserAgentHeader() {
        return userAgentHeader;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (triggersOn == null) {
            throw new IllegalArgumentException("Valve has not been configured");
        }

        final String userAgentHeaderValue = request.getHeader(userAgentHeader);
        final String[] expectedValues=commaDelimitedListToStringArray(triggersOn);

        boolean isExpected = false;

        if (log.isTraceEnabled()) {
            log.trace("Incoming request with ID " + request.getRequestId() + " for URI " +
                                request.getRequestURI() + " with HEADER[" + userAgentHeader + "] = [" + 
                                userAgentHeaderValue + "] processing against [" + triggersOn +"].");
        }

        // Check the header value
        if (userAgentHeaderValue != null && userAgentHeaderValue != "") {
            for(String val: expectedValues) {
                if (userAgentHeaderValue.equals(val)) {
                    isExpected = true;
                    break;
                }
            }

        }

        // Set the request attribute
        if (requestAttributesEnabled && isExpected) {
            request.setAttribute(requestAttributeName, isExpected);
            if (log.isTraceEnabled()) {
                log.trace("Request " + request.getRequestId() + " matched. Attribute " + requestAttributeName +
                            " set on request");
            }
        } else
            if (log.isTraceEnabled())
                log.trace("Request " + request.getRequestId() + " not  matched.");

        getNext().invoke(request, response);
    }

    /**
     * <p>
     * The values to match against those coming in the <code>User-Agent</code> header.
     * </p>
     * <p>
     * Default value : <code>null</code>
     * </p>
     *
     * @param triggersOn The header value to match
     */
    public void setTriggersOn(String triggersOn) {
        this.triggersOn = triggersOn;
    }

    /**
     * The attribute used to mark the request when the <code>userAgentHeader</code> contains a matched value.
     * Default is <code>in.samveen.catalina.expectedUserAgent</code>.
     *
     * @param requestAttributeName The attribute name
     */
    public void setRequestAttributeName(String requestAttributeName) {
        this.requestAttributeName = requestAttributeName;
    }

    /**
     * Should this valve set request attributes for matching header value for the request? This is typically
     * used in conjunction with the {@link AccessLog} for conditional logging.
     * Default is <code>true</code>. The attributes set are taken from the value of requestAttributeName.
     *
     * @param requestAttributesEnabled <code>true</code> causes the attributes to be set, <code>false</code> disables
     *                                     the setting of the attributes.
     */
    public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
        this.requestAttributesEnabled = requestAttributesEnabled;
    }

    /**
     * <p>
     * Header that holds the incoming value to test, usually named <code>User-Agent</code>.
     * </p>
     * <p>
     * Default value : <code>User-Agent</code>
     * </p>
     *
     * @param userAgentHeader The header name
     */
    public void setUserAgentHeader(String userAgentHeader) {
        this.userAgentHeader = userAgentHeader;
    }
}
