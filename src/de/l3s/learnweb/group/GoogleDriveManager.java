package de.l3s.learnweb.group;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

public class GoogleDriveManager
{
    private final static Logger log = Logger.getLogger(GoogleDriveManager.class);

    /** Application name. */
    private static final String APPLICATION_NAME = "LearnWeb";

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the JSON factory. */
    private static JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /** Global instance of the scopes required by Google Drive. */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    /** Required for web access from LearnWeb account - https://drive.google.com/drive/u/0/folders/0B_Sy7ytn1dadbkxEZGFZTm5kZU0 */
    private static final String defaultParentFolder = "0B_Sy7ytn1dadbkxEZGFZTm5kZU0";

    private Drive drive = null;

    static
    {
        try
        {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        }
        catch(Throwable e)
        {
            log.error("Can not init HTTP transport");
            log.error("unhandled error", e);
        }
    }

    public File createEmptyDocument(String title, String type)
    {
        return createEmptyDocument(title, type, defaultParentFolder);
    }

    public File createEmptyDocument(String title, String type, String parent)
    {
        File fileMetadata = new File(), uploadedFile = null;
        fileMetadata.setTitle(title);

        if(type.equals("document"))
            fileMetadata.setMimeType("application/vnd.google-apps.document");
        else if(type.equals("presentation"))
            fileMetadata.setMimeType("application/vnd.google-apps.presentation");
        else if(type.equals("spreadsheet"))
            fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
        else if(type.equals("drawing"))
            fileMetadata.setMimeType("application/vnd.google-apps.drawing");
        else
            throw new IllegalArgumentException("type should be: document, presentation, spreadsheet or drawing");

        if(parent != null && !parent.isEmpty())
        {
            fileMetadata.setParents(Collections.singletonList(new ParentReference().setId(parent)));
        }

        Drive.Files.Insert insert;
        try
        {
            insert = getDrive().files().insert(fileMetadata);
            uploadedFile = insert.execute();
            log.debug("Created new Google Document: " + uploadedFile.getAlternateLink());
        }
        catch(IOException e)
        {
            log.error("Can not create document");
            log.error("unhandled error", e);
        }

        return uploadedFile;
    }

    public File getDocument(String docId)
    {
        File file = null;
        try
        {
            file = getDrive().files().get(docId).execute();
        }
        catch(IOException e)
        {
            log.error("Can not get file");
            log.error("unhandled error", e);
        }

        return file;
    }

    public String getDocumentThumbnail(String docId)
    {
        return getDocumentThumbnail(docId, null);
    }

    public String getDocumentThumbnail(String docId, Date modifiedAfter)
    {
        File file = getDocument(docId);
        if(modifiedAfter != null && (file.getModifiedDate().getValue() > modifiedAfter.getTime()))
        {
            return null;
        }

        return file.getThumbnailLink();
    }

    private Drive getDrive()
    {
        if(drive == null)
        {
            GoogleCredential credential = null;
            try
            {
                java.io.File p12 = new java.io.File(GoogleDriveManager.class.getClassLoader().getResource("Learnweb-55153726550a.p12").toURI());
                String accountId = "181029990744-vegd7p8ugh5amn1ilgunba03d1pai4sb@developer.gserviceaccount.com";

                credential = new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT).setJsonFactory(JSON_FACTORY).setServiceAccountId(accountId).setServiceAccountScopes(SCOPES).setServiceAccountPrivateKeyFromP12File(p12).build();
                credential.refreshToken();
            }
            catch(GeneralSecurityException e)
            {
                log.error("Can not access to Google API");
                log.error("unhandled error", e);
            }
            catch(URISyntaxException e)
            {
                log.error("Can not load P12File for Google API");
                log.error("unhandled error", e);
            }
            catch(IOException e)
            {
                log.error("Can not get GoogleDrive credential");
                log.error("unhandled error", e);
            }

            drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
        }

        return drive;
    }
}