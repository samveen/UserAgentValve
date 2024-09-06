# UserAgentValve

This [repository][1] implements a custom valve for tomcat that sets a request attribute on matching the user agent string.

# Source

The bare bones for this project are inspired by [Tomcat-Valve-Example/README.md][2]. 

The meat came from [Tomcat-11.0.x implementation of RemoteIpValve][3]

The humors came from Google Search(TM).

# Requirements 

For building
- OpenJDK 17 (`sudo apt install openjdk-17-jdk-headless` on `bookworm`) or equivalent (Corretto, Temurin, Semeru, SapMachine, Java SE)
- [Maven][4]

For Running:
- OpenJDK 17
- [Tomcat 11.0.0-M24][5]

# Build

- Install the pre-requisites

- Clone the project.

- `mvn clean install`

# Install

- Install the built jar (in `target/`) into `${CATALINA_HOME}/lib` or `${CATALINA_BASE}/lib`.

- Add Valve filter config into server.xml, BEFORE the `*AccessLogValve` sections.

  ```xml
    <Valve className="in.samveen.catalina.valves.UserAgentValve"
           userAgentHeader="User-Agent"
           triggersOn="ELB-HealthChecker/2.0" />
  ```

- Add `conditionUnless` attribute to `*AccessLogValve` disable logging of marked requests.
  ```xml
    <Valve className="org.apache.catalina.valves.JsonAccessLogValve" directory="logs"
           prefix="access_log.json" pattern="combined"
           requestAttributesEnabled="true"
           conditionUnless="in.samveen.catalina.expectedUserAgent" />
  ```
- Optionally, add an alternate log path (using `conditionIf`) for the marked requests.
  ```xml
    <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
           prefix="healthcheck-access_log.txt" pattern="common"
           requestAttributesEnabled="true"
           conditionIf="in.samveen.catalina.expectedUserAgent" />
  ```

- Restart Tomcat service

# Profit
 
- Save a ton of headache, heartache, neckache with logging.

- Save money on cloud event storage.

- Send me a fraction of that Money


  [1]: https://github.com/samveen/UserAgentValve
  [2]: https://github.com/Keetmalin/Tomcat-Valve-Example/blob/master/README.md
  [3]: https://github.com/apache/tomcat/blob/11.0.x/java/org/apache/catalina/valves/RemoteIpValve.java
  [4]: https://maven.apache.org/download.cgi
  [5]: https://tomcat.apache.org/download-11.cgi
