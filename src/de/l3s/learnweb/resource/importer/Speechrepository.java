package de.l3s.learnweb.resource.importer;

import de.l3s.learnweb.Learnweb;

public class Speechrepository
{

    public static void main(String[] args) throws Exception
    {
        new Speechrepository().importVideos();
    }

    private void importVideos() throws Exception
    {
        Learnweb learnweb = Learnweb.createInstance(null);

        learnweb.getConnection();

        /*
        Resource tedResource = new Resource();
        tedResource.setTitle(title);
        tedResource.setDescription(description);
        tedResource.setUrl("https://www.ted.com/talks/" + slug);
        tedResource.setSource(SERVICE.ted);
        tedResource.setType(Resource.ResourceType.video);
        tedResource.setDuration(duration);
        tedResource.setMaxImageUrl(maxImageUrl);
        tedResource.setCreationDate(publishedAt);
        tedResource.setIdAtService(tedId);
        tedResource.setTranscript("");
        
        try
        {
            rpm.processImage(tedResource, FileInspector.openStream(tedResource.getMaxImageUrl()));
            tedResource.setGroup(tedGroup);
            admin.addResource(tedResource);
            //tedResource.save();
        
            //save new TED resource ID in order to use it later for saving transcripts
            resourceId = tedResource.getId();
            */
    }

}
