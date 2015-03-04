package de.l3s.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/*
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.semanticdesktop.aperture.extractor.Extractor;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.extractor.ExtractorFactory;
import org.semanticdesktop.aperture.extractor.impl.DefaultExtractorRegistry;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerImpl;
import org.semanticdesktop.aperture.util.IOUtil;
import org.semanticdesktop.aperture.vocabulary.NIE;
*/
/**
 * The source code of this class shows how to use a MimeTypeIdentifier and a collection of Extractors to get
 * the full-text and metadata of a specified file.
 */
public class FileInspector
{

    public class FileInfo
    {
	private String mimeType;
	private String title;
	private String textContent;
	private String fileName;
	private String author;

	public String getFileName()
	{
	    return fileName;
	}

	public String getMimeType()
	{
	    return mimeType;
	}

	public String getTitle()
	{
	    return title == null ? fileName : title;
	}

	public String getTextContent()
	{
	    return textContent;
	}

	public String getAuthor()
	{
	    return author;
	}

	@Override
	public String toString()
	{
	    return "FileInfo [mimeType=" + mimeType + ", title=" + title + ", textContent=" + textContent + ", fileName=" + fileName + "]";
	}

    }

    /*
    private MagicMimeTypeIdentifier identifier;
    private DefaultExtractorRegistry extractorRegistry;
    */
    public FileInspector()
    {/*
     // create a MimeTypeIdentifier
     identifier = new MagicMimeTypeIdentifier();

     // create an ExtractorRegistry containing all Extractors
     extractorRegistry = new DefaultExtractorRegistry();
     */
    }

    public static void main(String[] args) throws Exception
    {
	File file = new File("d:\\LearnWeb-2.0.pdf");
	FileInputStream stream = new FileInputStream(file);

	URL url = new URL("http://www.flickr.com/photos/kudo88/5825246809/");
	InputStream stream2 = url.openStream();

	FileInspector inspector = new FileInspector();
	FileInfo info = inspector.inspect(stream, "LearnWeb-2.0.pdf");

	System.out.println(info);

	info = inspector.inspect(stream2, "dfgdfgfdg");
	System.out.println(info);

    }

    public FileInfo inspect(InputStream inputStream, String fileName) throws IOException
    {
	FileInfo info = new FileInfo();
	info.fileName = fileName;

	int i = fileName.lastIndexOf('.');
	if(i > 0)
	{
	    String extension = fileName.substring(i + 1);
	    info.mimeType = MimeTypes.getMimeType(extension);
	    info.title = fileName.substring(0, i);
	}
	else
	{
	    info.mimeType = "application/octet-stream";
	    info.title = fileName;
	}

	return info;

	/*
	// read as many bytes of the file as desired by the MIME type identifier
	int minimumArrayLength = identifier.getMinArrayLength();
	int bufferSize = Math.max(minimumArrayLength, 8192);
	BufferedInputStream buffer = new BufferedInputStream(inputStream, bufferSize); 
	buffer.mark(minimumArrayLength + 10); // add some for safety
	byte[] bytes = IOUtil.readBytes(buffer, minimumArrayLength);
	
	FileInfo info = new FileInfo();
	info.fileName = fileName;
	info.mimeType = identifier.identify(bytes, fileName, null);   // let the MimeTypeIdentifier determine the MIME type of this file     
	
	// skip the extraction phase when the MIME type could not be determined
	if (info.mimeType == null) 
	{
	System.err.println("WARNING: MIME type could not be established.");
	info.mimeType = "application/octet-stream";
	return info;
	}
	
	URI uri = new URIImpl("http://learnweb.l3s.uni-hannover.de/nix");
	Model model = RDF2Go.getModelFactory().createModel();
	model.open();
	RDFContainer container = new RDFContainerImpl(model, uri);
	// determine and apply an Extractor that can handle this MIME type
	Set<ExtractorFactory> factories = extractorRegistry.getExtractorFactories(info.mimeType);
	if (factories != null && !factories.isEmpty()) 
	{
		// just fetch the first available Extractor
		ExtractorFactory factory = (ExtractorFactory) factories.iterator().next();
		Extractor extractor = factory.get();

		buffer.reset();
		try {
			extractor.extract(uri, buffer, null, info.mimeType, container);
		}
		catch (ExtractorException e) {
			e.printStackTrace();
			container.dispose(); 
	        buffer.close();
	        inputStream.close();
	        
			return info;
		}
		catch(java.lang.NoSuchFieldError a)
		{
			System.err.println("file can't be processed NoSuchFieldError");
			container.dispose(); 
	        buffer.close();
	        inputStream.close();
	        
			return info;
		}

		info.title = container.getString(NIE.title);
		info.textContent = container.getString(NIE.plainTextContent);
		if (null != info.textContent) { // remove multiple linebreaks
			Pattern pattern = Pattern.compile("\n\\s+");
			Matcher action = pattern.matcher(info.textContent.trim());
			info.textContent = action.replaceAll("\n\n");
		}
	}              
	
	container.dispose(); 
	buffer.close();
	inputStream.close();
	
	return info;
	*/
    }
}
