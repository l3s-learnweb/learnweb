package de.l3s.learnweb.resource.archive;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.Misc;
import de.l3s.util.URL;

public class WaybackUrlManager
{
    private static final Logger log = LogManager.getLogger(WaybackUrlManager.class);
    private static WaybackUrlManager instance;
    private final Learnweb learnweb;
    private LoadingCache<URL, UrlRecord> cache;
    private CDXClient cdxClient;

    // there should exist only one instance of this class because of the cache
    public static WaybackUrlManager getInstance(Learnweb learnweb)
    {
        if(instance == null)
        {
            instance = new WaybackUrlManager(learnweb);
        }
        return instance;
    }

    private WaybackUrlManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;

        cdxClient = new CDXClient();

        cache = CacheBuilder.newBuilder().maximumSize(3000000).build(new CacheLoader<>()
        {
            @Override
            public UrlRecord load(URL url) throws URISyntaxException, SQLException, RecordNotFoundException
            {
                UrlRecord record = getUrlRecordFromDatabase(url);

                if(null == record)
                    throw new RecordNotFoundException();

                return record;
            }
        });

        enableRelaxedSSLconnection();
    }

    private Connection getConnection() throws SQLException
    {
        return learnweb.getConnection();
    }

    /**
     *
     * @param url UTF8 or ASCII encoded URL
     * @return
     * @throws URISyntaxException
     * @throws SQLException
     * @throws ExecutionException
     */
    public UrlRecord getUrlRecord(String url) throws URISyntaxException, SQLException
    {
        URL asciiUrl = new URL(url);

        UrlRecord newRecord;
        try
        {
            newRecord = cache.get(asciiUrl);
        }
        catch(ExecutionException e)
        {
            if(e.getCause() instanceof RecordNotFoundException)
            {
                newRecord = new UrlRecord(asciiUrl); // create a new record for this url
            }
            else
            {
                log.error("fatal", e);
                throw new RuntimeException(e);
            }
        }

        return newRecord;
    }

    /**
     * This method will only return cached records or null if no cached record was found
     *
     * @param url
     * @return
     * @throws URISyntaxException
     */
    public UrlRecord getCachedUrlRecord(String url) throws URISyntaxException
    {
        URL asciiUrl = new URL(url);

        try
        {
            return cache.get(asciiUrl);
        }
        catch(Throwable e)
        {
            return null;
        }
    }

    public boolean hasToBeUpdated(UrlRecord urlRecord, Date minAcceptableCrawlTime, Date minAcceptableStatusTime) throws SQLException
    {
        if(minAcceptableCrawlTime != null && urlRecord.getCrawlDate().getTime() < minAcceptableCrawlTime.getTime())
            return true;
        if(minAcceptableStatusTime != null && urlRecord.getStatusCodeDate().getTime() < minAcceptableStatusTime.getTime())
            return true;
        return false;
    }

    public boolean updateRecord(UrlRecord record, Date minAcceptableCrawlTime, Date minAcceptableStatusTime) throws SQLException
    {
        boolean capturesUpdated = minAcceptableCrawlTime != null && updateRecordCaptures(record, minAcceptableCrawlTime.getTime());
        boolean statusUpdated = minAcceptableStatusTime != null && updateStatusCode(record, minAcceptableStatusTime.getTime());
        if(capturesUpdated || statusUpdated)
        {
            saveUrlRecord(record);
            return true;
        }
        return false;
    }

    private boolean updateStatusCode(UrlRecord urlRecord, long minAcceptableCrawlTime)
    {
        if(urlRecord.getStatusCodeDate().getTime() > minAcceptableCrawlTime)
        {
            return false; // already up-to-date
        }

        //int statusCode = -1;

        try
        {
            //statusCode = getStatusCode(urlRecord.getUrl().toString());
            getStatusCode(urlRecord);
        }
        catch(Throwable t)
        {
            log.error("can't check url: " + urlRecord.getUrl(), t);
        }

        // TODO insert content and status into wb_url_content

        //urlRecord.setStatusCode((short) statusCode);
        //urlRecord.setStatusCodeDate(new Date());
        return true;
    }

    private boolean updateRecordCaptures(UrlRecord urlRecord, long minAcceptableStatusTime)
    {
        if(urlRecord.getCrawlDate().getTime() > minAcceptableStatusTime)
        {
            return false; // already up-to-date
        }

        String url = urlRecord.getUrl().toString();
        url = url.substring(url.indexOf("//") + 2); // remove protocol (http(s)://)

        Date lastCapture, firstCapture;
        try
        {
            lastCapture = null;
            firstCapture = cdxClient.getFirstCaptureDate(url);

            if(firstCapture != null)
            {
                lastCapture = cdxClient.getLastCaptureDate(url);
            }

            if(lastCapture != null)
            {
                urlRecord.setFirstCapture(firstCapture);
                urlRecord.setLastCapture(lastCapture);
            }
            else
            {
                urlRecord.setFirstCapture(null);
                urlRecord.setLastCapture(null);
            }

            urlRecord.setCrawlDate(new Date());
            return true;

        }
        catch(ParseException | IOException e)
        {
            log.error("Wayback error for: " + urlRecord.getUrl(), e);
        }

        return false;
    }

    private static Date timestampToDate(Timestamp timestamp)
    {
        if(timestamp == null)
            return null;
        return new Date(timestamp.getTime());
    }

    private UrlRecord getUrlRecordFromDatabase(URL asciiUrl) throws SQLException
    {
        UrlRecord record = null;

        PreparedStatement urlRecordSelect = getConnection().prepareStatement("SELECT `url_id`, `first_capture`, `last_capture`, `all_captures_fetched`, `crawl_time`, status_code, status_code_date FROM learnweb_large.wb_url WHERE url = ?");
        urlRecordSelect.setString(1, asciiUrl.toString());
        ResultSet rs = urlRecordSelect.executeQuery();
        if(rs.next())
        {
            record = new UrlRecord(asciiUrl);
            record.setId(rs.getLong(1));
            record.setFirstCapture(timestampToDate(rs.getTimestamp(2)));
            record.setLastCapture(timestampToDate(rs.getTimestamp(3)));
            record.setAllCapturesFetched(rs.getInt(4) == 1);
            record.setCrawlDate(timestampToDate(rs.getTimestamp(5)));
            record.setStatusCode(rs.getShort(6));
            record.setStatusCodeDate(timestampToDate(rs.getTimestamp(7)));
        }
        urlRecordSelect.close();

        return record;
    }

    public UrlRecord saveUrlRecord(UrlRecord record) throws SQLException
    {
        //log.debug("save " + record.toString());

        if(record.getId() == -1L) // record is not stored yet => insert it
        {
            try(PreparedStatement urlRecordInsert = getConnection().prepareStatement("INSERT INTO learnweb_large.wb_url (`url`, `first_capture`, `last_capture`, `crawl_time`, all_captures_fetched, status_code, status_code_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS))
            {
                urlRecordInsert.setString(1, record.getUrl().toString());
                urlRecordInsert.setTimestamp(2, record.isArchived() ? new Timestamp(record.getFirstCapture().getTime()) : null);
                urlRecordInsert.setTimestamp(3, record.isArchived() ? new Timestamp(record.getLastCapture().getTime()) : null);
                urlRecordInsert.setTimestamp(4, new java.sql.Timestamp(record.getCrawlDate().getTime()));
                urlRecordInsert.setInt(5, record.isAllCapturesFetched() ? 1 : 0);
                urlRecordInsert.setInt(6, record.getStatusCode());
                urlRecordInsert.setTimestamp(7, new java.sql.Timestamp(record.getStatusCodeDate().getTime()));
                urlRecordInsert.executeUpdate();

                // get generated id
                ResultSet rs = urlRecordInsert.getGeneratedKeys();
                if(!rs.next())
                    throw new SQLException("database error: no id generated");
                record.setId(rs.getLong(1));
            }
            catch(SQLIntegrityConstraintViolationException e)
            {
                // if we catch a duplicate URL error we will try to merge the records
                if(e.getErrorCode() != 1062)
                {
                    log.error("error code " + e.getErrorCode());
                    throw e;
                }
                UrlRecord record2 = getUrlRecordFromDatabase(record.getUrl());

                if(record2 == null)
                {
                    Misc.sleep(900);
                    log.error("duplicated entry but record2 is null");
                    record2 = getUrlRecordFromDatabase(record.getUrl());
                }

                if(!record2.equals(record))
                {
                    log.error("Duplicate entry error; they don't match; 1:" + record + "; 2: " + record2);
                    throw e;
                }
            }
        }
        else
        {
            // record is already stored => updated it
            PreparedStatement urlRecordUpdate = getConnection().prepareStatement("UPDATE learnweb_large.wb_url SET `first_capture` = ?, `last_capture` = ?, `crawl_time` = ?, all_captures_fetched = ?, status_code = ?, status_code_date = ? WHERE `url_id` = ?");
            urlRecordUpdate.setTimestamp(1, record.isArchived() ? new Timestamp(record.getFirstCapture().getTime()) : null);
            urlRecordUpdate.setTimestamp(2, record.isArchived() ? new Timestamp(record.getLastCapture().getTime()) : null);
            urlRecordUpdate.setTimestamp(3, new java.sql.Timestamp(record.getCrawlDate().getTime()));
            urlRecordUpdate.setInt(4, record.isAllCapturesFetched() ? 1 : 0);
            urlRecordUpdate.setInt(5, record.getStatusCode());
            urlRecordUpdate.setTimestamp(6, new java.sql.Timestamp(record.getStatusCodeDate().getTime()));
            urlRecordUpdate.setLong(7, record.getId());
            urlRecordUpdate.executeUpdate();
            urlRecordUpdate.close();
        }

        cache.put(record.getUrl(), record);
        return record;
    }

    /**
     * @author Philipp
     *
     */
    public static class UrlRecord
    {
        private long id = -1L;
        private URL url;
        private Date firstCapture;
        private Date lastCapture;
        private Date crawlDate = new Date(3601000L);
        private boolean allCapturesFetched = false;

        private short statusCode = -3;
        private Date statusCodeDate = new Date(3601000L);
        private String content;

        public UrlRecord(URL asciiUrl)
        {
            this.url = asciiUrl;
        }

        public boolean isArchived()
        {
            return firstCapture != null && lastCapture != null;
        }

        public long getId()
        {
            return id;
        }

        private void setId(long id)
        {
            this.id = id;
        }

        public URL getUrl()
        {
            return url;
        }

        public void setUrl(URL url)
        {
            this.url = url;
        }

        public Date getFirstCapture()
        {
            return firstCapture;
        }

        private void setFirstCapture(Date firstCapture)
        {
            this.firstCapture = firstCapture;
        }

        public Date getLastCapture()
        {
            return lastCapture;
        }

        private void setLastCapture(Date lastCapture)
        {
            this.lastCapture = lastCapture;
        }

        public Date getCrawlDate()
        {
            return crawlDate;
        }

        private void setCrawlDate(Date lastUpdate)
        {
            this.crawlDate = lastUpdate;
        }

        public boolean isAllCapturesFetched()
        {
            return allCapturesFetched;
        }

        private void setAllCapturesFetched(boolean allCapturesFetched)
        {
            this.allCapturesFetched = allCapturesFetched;
        }

        public short getStatusCode()
        {
            return statusCode;
        }

        private void setStatusCode(short statusCode)
        {
            this.statusCode = statusCode;
        }

        public Date getStatusCodeDate()
        {
            return statusCodeDate;
        }

        private void setStatusCodeDate(Date statusCodeDate)
        {
            this.statusCodeDate = statusCodeDate;
        }

        public String getContent()
        {
            return content;
        }

        public void setContent(String content)
        {
            this.content = content;
        }

        @Override
        public boolean equals(final Object o)
        {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            final UrlRecord urlRecord = (UrlRecord) o;
            return allCapturesFetched == urlRecord.allCapturesFetched &&
                    statusCode == urlRecord.statusCode &&
                    Objects.equals(url, urlRecord.url) &&
                    Objects.equals(firstCapture, urlRecord.firstCapture) &&
                    Objects.equals(lastCapture, urlRecord.lastCapture);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(url, firstCapture, lastCapture, allCapturesFetched, statusCode);
        }

        public boolean isOffline()
        {
            return ((statusCode < 200 || statusCode >= 400) && statusCode != 403 && statusCode != 650 && statusCode != 999 && statusCode != 606 && statusCode != 603 && statusCode != 429 && statusCode != -1);
        }

        @Override
        public String toString()
        {
            return "UrlRecord [id=" + id + ", url=" + url + ", firstCapture=" + firstCapture + ", lastCapture=" + lastCapture + ", crawlDate=" + crawlDate + ", allCapturesFetched=" + allCapturesFetched + ", statusCode=" + statusCode + ", statusCodeDate=" + statusCodeDate + "]";
        }

    }

    public UrlRecord getStatusCode(UrlRecord urlRecord) throws IOException, URISyntaxException
    {

        int responseCode = -1;
        String urlStr = urlRecord.getUrl().toString();

        urlStr = urlStr.replaceAll(" ", "%20"); //few urls from Bing do not encode space correctly
        String originalUrl = urlStr; // in case we get redirect we need to compare the urls
        int maxRedirects = 20;
        String cookies = null;
        List<String> seenURLs = null;
        ///seenURLs.add(urlStr);

        while(maxRedirects > 0)
        {
            try
            {
                java.net.URL url = new java.net.URL(urlStr);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                connection.setRequestProperty("Accept-Language", "en,en-US;q=0.8,de;q=0.5,de-DE;q=0.3");
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                connection.setRequestProperty("Referer", "https://www.bing.com/");
                connection.setRequestProperty("Connection", "keep-alive");
                if(null != cookies)
                {
                    connection.setRequestProperty("Cookie", cookies);
                    //cookies = null;
                }

                responseCode = connection.getResponseCode();

                if(responseCode == -1)
                {
                    responseCode = 652;
                    break;
                }

                String server = connection.getHeaderField("Server");
                if(responseCode == 403 && server != null && server.equals("cloudflare-nginx"))
                {
                    responseCode = 606;
                    break;
                }

                List<String> cookiesHeader = connection.getHeaderFields().get("Set-Cookie");

                if(cookiesHeader != null)
                {
                    cookies = "";

                    for(String cookie : cookiesHeader)
                    {
                        try
                        {
                            cookies += HttpCookie.parse(cookie).get(0) + ";";
                        }
                        catch(IllegalArgumentException e)
                        {
                            //log.debug("Invalid cookie: " + cookie);
                        }
                    }
                }

                if(responseCode >= 300 && responseCode < 400)
                {
                    maxRedirects--;
                    String location = connection.getHeaderField("Location");

                    if(location == null)
                    {
                        responseCode = 607; //no location for redirect status code
                        break;
                    }

                    location = location.replaceAll(" ", "%20");

                    //log.debug("Redirect {}; Location {}", responseCode, location);

                    if(location.startsWith("/"))
                    {
                        int index = urlStr.indexOf('/', urlStr.indexOf("//") + 2);
                        String domain = index > 0 ? urlStr.substring(0, index) : urlStr;
                        urlStr = domain + location;
                    }
                    else if(!location.startsWith("http"))
                        urlStr = "http://" + location;
                    else
                        urlStr = location;

                    connection.disconnect();

                    if(null == seenURLs)
                        seenURLs = new LinkedList<>(); // init here to allow the two redirects to the initial url

                    if(seenURLs.contains(urlStr))
                    {
                        responseCode = 604;//too many redirects
                        break;
                    }
                    seenURLs.add(urlStr);
                }
                else
                {
                    if(responseCode >= 200 && responseCode < 300)
                    {
                        //download content if mime type does not start with "application/" (don't download PDFs)
                        String contentType = connection.getContentType();
                        if(contentType != null && !contentType.startsWith("application/"))
                        {
                            //To handle gzip and deflate encodings
                            String contentEncoding = connection.getContentEncoding();
                            InputStream inputStream;
                            if("gzip".equalsIgnoreCase(contentEncoding))
                            {
                                inputStream = new GZIPInputStream(connection.getInputStream());
                            }
                            else if("deflate".equalsIgnoreCase(contentEncoding))
                            {
                                Inflater inf = new Inflater(true);
                                inputStream = new InflaterInputStream(connection.getInputStream(), inf);
                            }
                            else
                                inputStream = connection.getInputStream();

                            //To check webpage character encoding if present in header field: Content-Type
                            String[] contentTypeSplit = contentType.split("charset=");
                            String charsetStr = null;
                            if(contentTypeSplit != null && contentTypeSplit.length == 2)
                                charsetStr = contentTypeSplit[1];

                            Charset charset = null;
                            try
                            {
                                if(charsetStr != null && !charsetStr.isEmpty())
                                    charset = Charset.forName(charsetStr);
                            }
                            catch(IllegalCharsetNameException e)
                            {
                            }

                            BufferedReader br;
                            if(charset != null)
                                br = new BufferedReader(new InputStreamReader(inputStream, charset));
                            else
                                br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                            StringBuilder content = new StringBuilder();
                            String inputLine;
                            while((inputLine = br.readLine()) != null)
                            {
                                content.append(inputLine);
                            }
                            urlRecord.setContent(content.toString().trim());
                            br.close();
                        }
                    }

                    connection.disconnect();
                    break;
                }
            }
            catch(UnknownHostException e)
            {
                //log.warn("UnknownHostException: {}", urlStr);
                responseCode = 600;
                break;
            }
            catch(ProtocolException e)
            {
                log.warn("ProtocolException: " + e.getMessage() + "; URL: {}" + urlStr);
                responseCode = 601;
                break;
            }
            catch(SocketException e)
            {
                log.warn("SocketException: " + e.getMessage() + "; URL: {}" + urlStr);
                responseCode = 602;
                logUrlInFile(urlStr);
                break;
            }
            catch(SocketTimeoutException e)
            {
                log.warn("SocketTimeoutException: " + e.getMessage() + "; URL: {}" + urlStr);
                responseCode = 603;
                break;
            }
            catch(SSLException e)
            {
                log.warn("SSLException: " + e.getMessage() + "; URL: {}" + urlStr);
                responseCode = getStatusCodeFromHttpClient(urlRecord);
                break;
            }
            catch(Exception e)
            {
                //this exception is thrown but not declared in the try block so we can't easily catch it
                if(GeneralSecurityException.class.isAssignableFrom(e.getClass()))
                {
                    log.warn("GeneralSecurityException: " + e.getMessage() + "; URL: {}" + urlStr);
                    responseCode = 651;
                    break;
                }
                else if(e.getCause() instanceof IllegalArgumentException)
                {
                    log.warn("Invalid redirect: " + e.getCause().getMessage() + "; URL: {}" + urlStr);
                    responseCode = 608; //redirect to invalid url
                    break;
                }
                else
                {
                    log.error("Can't check URL: " + urlStr, e);
                    responseCode = 653;
                    break;
                    //throw e;
                }
            }
        }

        if(maxRedirects == 0)
            responseCode = 604; // too many redirects

        // check if a URL was redirected to the base URL. This can usually be handled as some kind of error 404
        if(!Objects.equals(originalUrl, urlStr)) // got a redirect
        {
            String pathOld = new java.net.URL(originalUrl).getPath();
            String pathNew = new java.net.URL(urlStr).getPath();

            if(responseCode < 300 && pathOld != null && pathOld.length() > 4 && (pathNew == null || pathNew.length() < 4))
                responseCode = 605; // redirect to main page

            //log.debug("Redirect; status: {}; old {} ; new {}", responseCode, originalUrl, urlStr);
        }

        urlRecord.setStatusCode((short) responseCode);
        urlRecord.setStatusCodeDate(new Date());
        return urlRecord;
    }

    public UrlRecord getHtmlContent(String url) throws IOException, URISyntaxException
    {
        return getStatusCode(new UrlRecord(new URL(url)));
    }

    //This method is called only when there is a SSLHandshake failure from the previous method
    public int getStatusCodeFromHttpClient(UrlRecord urlRecord) throws URISyntaxException
    {
        try
        {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(urlRecord.getUrl().toString());
            request.addHeader("User-Agent", "Mozilla/5.0");
            HttpResponse response = client.execute(request);
            try(BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)))
            {
                StringBuilder result = new StringBuilder();
                String line;
                while((line = rd.readLine()) != null)
                {
                    result.append(line);
                }
                urlRecord.setContent(result.toString().trim());
                return 200;
            }
        }
        catch(IOException e)
        {
            log.warn("HttpClient method: SSLException: " + e.getMessage() + "; URL: {}" + urlRecord.getUrl());
            logUrlInFile(urlRecord.getUrl().toString());
            return 650;
        }
        catch(IllegalArgumentException e)
        {
            log.warn("HttpClient method: Invalid url: " + e.getMessage() + "; URL: {}" + urlRecord.getUrl());
            logUrlInFile(urlRecord.getUrl().toString());
            return 650;
        }

    }

    public void logUrlInFile(String url)
    {
        String filename = "/home/learnweb_user/searchlog_html_url_exceptions.txt";
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, StandardCharsets.UTF_8, true))))
        {
            out.println(url);
        }
        catch(IOException e)
        {
            log.error("Exception while writing url exception to file " + e.getMessage());
        }
    }

    public static void enableRelaxedSSLconnection()
    {
        /*
        System.setProperty("javax.net.debug", "all");
        System.setProperty("jsse.enableSNIExtension", "false");
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,SSLv3");
        */
        /* Start of Fix */
        TrustManager[] trustAllCerts = { new X509TrustManager()
        {
            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType)
            {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType)
            {
            }

        } };

        SSLContext sc;
        try
        {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        //HttpsURLConnection.setDefaultSSLSocketFactory(new SSLSocketFactoryWrapper((SSLSocketFactory) SSLSocketFactory.getDefault()));

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        /* End of the fix*/
    }

    private static class RecordNotFoundException extends Exception
    {
        private static final long serialVersionUID = -1754500933829531955L;
    }

    public static class SSLSocketFactoryWrapper extends SSLSocketFactory
    {

        private SSLSocketFactory factory;

        public SSLSocketFactoryWrapper(SSLSocketFactory factory)
        {
            this.factory = factory;
        }

        /*
        @Override
        public Socket createSocket() throws IOException {
        return  factory.createSocket();
        }
        */

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
        {
            return factory.createSocket(s, host, port, autoClose);
        }

        @Override
        public String[] getDefaultCipherSuites()
        {
            return factory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites()
        {
            return factory.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException
        {
            return factory.createSocket(host, port);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException
        {
            return factory.createSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException
        {
            return factory.createSocket(host, port, localHost, localPort);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
        {
            return factory.createSocket(address, port, localAddress, localPort);
        }

    }

    public static void main(String[] args) throws SQLException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, ClassNotFoundException
    {
        /*Calendar minCrawlTime = Calendar.getInstance();
        minCrawlTime.add(Calendar.DATE, -1);
        Date MIN_ACCEPTABLE_CRAWL_TIME = minCrawlTime.getTime();

        log.debug("start");
        WaybackUrlManager manager = Learnweb.createInstance().getWaybackUrlManager();
        UrlRecord record = manager.getUrlRecord("http://www.google.com/");
        log.debug(record);
        manager.updateRecord(record, null, MIN_ACCEPTABLE_CRAWL_TIME);
        log.debug(record);
        // check statusCodeDate in the output
        */

        WaybackUrlManager manager = Learnweb.createInstance().getWaybackUrlManager();
        String url = "http://www.cs.virginia.edu/~hw5x/Course/IR2017/_site/docs/Inverted Index.pptx";
        UrlRecord record = manager.getHtmlContent(url);
        log.debug(record.getStatusCode());
        log.debug(record.getContent());
        UrlRecord record2 = new UrlRecord(new URL(url));
        manager.getStatusCodeFromHttpClient(record2);
        log.debug(record2.getContent());
        System.exit(0);
    }
}
