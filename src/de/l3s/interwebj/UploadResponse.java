package de.l3s.interwebj;

import java.io.InputStream;
import java.io.Serializable;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import de.l3s.learnweb.Resource;

public class UploadResponse implements Serializable
{
    private static final long serialVersionUID = -3975292604112903040L;
    private Resource result;

    public UploadResponse(InputStream inputStream) throws IllegalResponseException
    {
        parse(inputStream);
    }

    private void parse(InputStream inputStream) throws IllegalResponseException
    {
        try
        {
            Element root = new SAXReader().read(inputStream).getRootElement();

            if(null == root.attributeValue("stat") || !root.attributeValue("stat").equals("ok"))
            {
                throw new IllegalResponseException(root.asXML());
            }

            Element resultElement = root.element("result");

            result = new Resource();
            result.setType(resultElement.elementText("type"));
            result.setTitle(resultElement.elementText("title"));
            result.setDescription(resultElement.elementText("description"));
            result.setLocation(resultElement.elementText("service"));
            result.setSource(resultElement.elementText("service"));
            //currentResult.tags = resultElement.elementText("tags");
            result.setUrl(resultElement.elementText("url"));

            result.setMaxImageUrl((resultElement.elementText("max_image_url")));

            if(!result.getType().equals(Resource.ResourceType.image))
            {
                result.setEmbeddedRaw(resultElement.elementText("embedded_size4"));
                if(null == result.getEmbeddedRaw())
                    result.setEmbeddedRaw(resultElement.elementText("embedded_size3"));
            }
        }
        catch(DocumentException e1)
        {
            throw new IllegalResponseException(e1);
        }
    }

    public Resource getResult()
    {
        return result;
    }
}
