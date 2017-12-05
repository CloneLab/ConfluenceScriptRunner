//Page Create, Update and View logger to splunk


import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.user.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.UnsupportedEncodingException;
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.spaces.Space
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.confluence.event.events.content.page.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

def nullTrustManager = [
    checkClientTrusted: { chain, authType ->  },
    checkServerTrusted: { chain, authType ->  },
    getAcceptedIssuers: { null }
]

def nullHostnameVerifier = [
    verify: { hostname, session -> true }
]

SSLContext sc = SSLContext.getInstance("SSL")
sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
 
System.out.println("Start post2splunk.groovy")
 
def spaceManager = ComponentLocator.getComponent(SpaceManager);
def pageManager = ComponentLocator.getComponent(PageManager);
 
String userName="Anonymous";
def currentUser = AuthenticatedUserThreadLocal.get();
if (currentUser)
{
  userName=(String)currentUser.name;
}
 
def event = event as PageEvent;
String eventType=(String)event.toString();
eventType=eventType.replaceAll("com.atlassian.confluence.event.events.content.page.","");
eventType=eventType.substring(0, eventType.indexOf('@'));
eventType=eventType.replaceAll("Event","");
 
// keys to create unique nodes for counters
// https://docs.atlassian.com/confluence/5.9.7/com/atlassian/confluence/pages/Page.html
 
String spaceKey = event.page.getSpace().getKey();
String pageId = event.page.getIdAsString();
String pageName = event.page.getTitle();
 
def requestMethod = "POST";
def URLParam = [];
def baseURL = "https://server.com:8088/services/collector";
 
URL url = new URL(baseURL);
HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
conn.setDoOutput(true);
conn.setRequestMethod("POST");
conn.setRequestProperty("Content-Type", "application/json");
conn.setRequestProperty('Authorization', 'Splunk XXXXXXX-XXXX-XXXXX-865C-XXXXXXXXX');
 
String jSon= "{\"host\": \"server.com\",\"source\": \"webaccess\",\"sourcetype\": \"webaccess\",\"index\": \"confluence\",\"event\":{"
 
jSon = jSon + "\"event-type\":\"" + eventType  + "\","
jSon = jSon + "\"space-key\":\"" + spaceKey + "\","
jSon = jSon + "\"confluence-page-title\":\"" + pageName + "\","
jSon = jSon + "\"confluence-page-id\":\"" + pageId + "\","
jSon = jSon + "\"username\":\"" + userName + "\""
jSon = jSon + "}}"
 
def writer = new OutputStreamWriter(conn.outputStream)
writer.write(jSon)
writer.flush()
writer.close()
conn.connect();
try
{
  conn.getContent()
}
catch (all)
{
}
String Status=conn.getResponseCode()
String Message=conn.getResponseMessage()
