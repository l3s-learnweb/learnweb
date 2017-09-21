package de.l3s.office;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.l3s.learnweb.Learnweb;

public class ConverterService
{
    private static int CONVERT_TIMEOUT = 120000;
    private static final MessageFormat CONVERT_PARAMS = new MessageFormat("?url={0}&outputtype={1}&filetype={2}&title={3}&key={4}");
    private static final int MAX_TRY = 3;
    private final static Logger logger = Logger.getLogger(ConverterService.class);
    private final Learnweb learnweb;

    public ConverterService(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public InputStream convert(String fileName, String fileUri)
    {
        String fileExt = fileName.substring(fileName.lastIndexOf("."));
        String fileType = FileUtility.getFileType(fileName);
        String internalFileExt = FileUtility.getInternalExtension(fileType);
        try
        {
            String key = FileUtility.generateRevisionId(fileUri);

            Pair<Integer, String> res = getConvertedUri(fileUri, fileExt, internalFileExt, key, false);

            int result = res.getKey();
            String newFileUri = res.getValue();

            if(result != 100)
            {
                logger.error("Could not convert file ");
            }

            URL url = new URL(newFileUri);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            InputStream stream = connection.getInputStream();

            if(stream == null)
            {
                throw new Exception("Stream is null");
            }

            return stream;

        }
        catch(Exception ex)
        {
            logger.error(ex);
        }
        return null;
    }

    public Pair<Integer, String> getConvertedUri(String documentUri, String fromExtension, String toExtension, String documentRevisionId, Boolean isAsync) throws Exception
    {
        String convertedDocumentUri = null;

        String xml = sendRequestToConvertService(documentUri, fromExtension, toExtension, documentRevisionId, isAsync);

        Document document = convertStringToXmlDocument(xml);

        Element responceFromConvertService = document.getDocumentElement();
        if(responceFromConvertService == null)
            throw new Exception("Invalid answer format");

        NodeList errorElement = responceFromConvertService.getElementsByTagName("Error");
        if(errorElement != null && errorElement.getLength() > 0)
            ProcessConvertServiceResponceError(Integer.parseInt(errorElement.item(0).getTextContent()));

        NodeList endConvertNode = responceFromConvertService.getElementsByTagName("EndConvert");
        if(endConvertNode == null || endConvertNode.getLength() == 0)
            throw new Exception("EndConvert node is null");

        Boolean isEndConvert = Boolean.parseBoolean(endConvertNode.item(0).getTextContent());

        NodeList percentNode = responceFromConvertService.getElementsByTagName("Percent");
        if(percentNode == null || percentNode.getLength() == 0)
            throw new Exception("Percent node is null");

        Integer percent = Integer.parseInt(percentNode.item(0).getTextContent());

        if(isEndConvert)
        {
            NodeList fileUrlNode = responceFromConvertService.getElementsByTagName("FileUrl");
            if(fileUrlNode == null || fileUrlNode.getLength() == 0)
                throw new Exception("FileUrl node is null");

            convertedDocumentUri = fileUrlNode.item(0).getTextContent();
            percent = 100;
        }
        else
        {
            percent = percent >= 100 ? 99 : percent;
        }

        return new MutablePair<>(percent, convertedDocumentUri);
    }

    private String sendRequestToConvertService(String documentUri, String fromExtension, String toExtension, String documentRevisionId, Boolean isAsync) throws Exception
    {
        fromExtension = fromExtension == null || fromExtension.isEmpty() ? FileUtility.getFileExtension(documentUri) : fromExtension;

        String title = FileUtility.getFileName(documentUri);
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        documentRevisionId = documentRevisionId == null || documentRevisionId.isEmpty() ? documentUri : documentRevisionId;

        documentRevisionId = FileUtility.generateRevisionId(documentRevisionId);

        Object[] args = { URLEncoder.encode(documentUri), toExtension.replace(".", ""), fromExtension.replace(".", ""), title, documentRevisionId };

        String urlToConverter = learnweb.getProperties().getProperty("FILES.DOCSERVICE.URL.CONVERTER") + CONVERT_PARAMS.format(args);

        if(isAsync)
            urlToConverter += "&async=true";

        URL url = new URL(urlToConverter);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONVERT_TIMEOUT);

        InputStream stream = null;
        int countTry = 0;

        while(countTry < MAX_TRY)
        {
            try
            {
                countTry++;
                stream = connection.getInputStream();
                break;
            }
            catch(Exception ex)
            {
                if(!(ex instanceof TimeoutException))
                    throw new Exception("Bad Request");
            }
        }
        if(countTry == MAX_TRY)
        {
            throw new Exception("Timeout");
        }

        if(stream == null)
            throw new Exception("Could not get an answer");

        String xml = convertStreamToString(stream);

        connection.disconnect();

        return xml;
    }

    private void ProcessConvertServiceResponceError(int errorCode) throws Exception
    {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConvertService: ";

        switch(errorCode)
        {
        case -8:
            errorMessage = errorMessageTemplate + "Error document VKey";
            break;
        case -7:
            errorMessage = errorMessageTemplate + "Error document request";
            break;
        case -6:
            errorMessage = errorMessageTemplate + "Error database";
            break;
        case -5:
            errorMessage = errorMessageTemplate + "Error unexpected guid";
            break;
        case -4:
            errorMessage = errorMessageTemplate + "Error download error";
            break;
        case -3:
            errorMessage = errorMessageTemplate + "Error convertation error";
            break;
        case -2:
            errorMessage = errorMessageTemplate + "Error convertation timeout";
            break;
        case -1:
            errorMessage = errorMessageTemplate + "Error convertation unknown";
            break;
        case 0:
            break;
        default:
            errorMessage = "ErrorCode = " + errorCode;
            break;
        }

        throw new Exception(errorMessage);
    }

    private Pair<Integer, String> getResponseUri(String xml) throws Exception
    {
        Document document = convertStringToXmlDocument(xml);

        Element responceFromConvertService = document.getDocumentElement();
        if(responceFromConvertService == null)
            throw new Exception("Invalid answer format");

        NodeList errorElement = responceFromConvertService.getElementsByTagName("Error");
        if(errorElement != null && errorElement.getLength() > 0)
            ProcessConvertServiceResponceError(Integer.parseInt(errorElement.item(0).getTextContent()));

        NodeList endConvert = responceFromConvertService.getElementsByTagName("EndConvert");
        if(endConvert == null || endConvert.getLength() == 0)
            throw new Exception("Invalid answer format");

        Boolean isEndConvert = Boolean.parseBoolean(endConvert.item(0).getTextContent());

        int resultPercent = 0;
        String responseUri = null;

        if(isEndConvert)
        {
            NodeList fileUrl = responceFromConvertService.getElementsByTagName("FileUrl");
            if(fileUrl == null || endConvert.getLength() == 0)
                throw new Exception("Invalid answer format");

            resultPercent = 100;
            responseUri = fileUrl.item(0).getTextContent();
        }
        else
        {
            NodeList percent = responceFromConvertService.getElementsByTagName("Percent");
            if(percent != null && percent.getLength() > 0)
                resultPercent = Integer.parseInt(percent.item(0).getTextContent());

            resultPercent = resultPercent >= 100 ? 99 : resultPercent;
        }

        return new MutablePair<>(resultPercent, responseUri);
    }

    private static String convertStreamToString(InputStream stream) throws IOException
    {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();

        while(line != null)
        {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }

        String result = stringBuilder.toString();

        return result;
    }

    private static Document convertStringToXmlDocument(String xml) throws IOException, ParserConfigurationException, SAXException
    {
        DocumentBuilderFactory documentBuildFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder doccumentBuilder = documentBuildFactory.newDocumentBuilder();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes("utf-8"));
        InputSource inputSource = new InputSource(inputStream);
        Document document = doccumentBuilder.parse(inputSource);
        return document;
    }
}
