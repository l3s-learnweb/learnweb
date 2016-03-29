package de.l3s.util;

//Copyright (c) 2003-2009, Jodd Team (jodd.org). All Rights Reserved.

import java.util.HashMap;

/**
 * Map file extensions to MIME types. Based on the Apache mime.types file.
 * http://www.iana.org/assignments/media-types/
 */
public class MimeTypes
{

    public static final String MIME_APPLICATION_ANDREW_INSET = "application/andrew-inset";
    public static final String MIME_APPLICATION_JSON = "application/json";
    public static final String MIME_APPLICATION_ZIP = "application/zip";
    public static final String MIME_APPLICATION_X_GZIP = "application/x-gzip";
    public static final String MIME_APPLICATION_TGZ = "application/tgz";
    public static final String MIME_APPLICATION_MSWORD = "application/msword";
    public static final String MIME_APPLICATION_POSTSCRIPT = "application/postscript";
    public static final String MIME_APPLICATION_PDF = "application/pdf";
    public static final String MIME_APPLICATION_JNLP = "application/jnlp";
    public static final String MIME_APPLICATION_MAC_BINHEX40 = "application/mac-binhex40";
    public static final String MIME_APPLICATION_MAC_COMPACTPRO = "application/mac-compactpro";
    public static final String MIME_APPLICATION_MATHML_XML = "application/mathml+xml";
    public static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String MIME_APPLICATION_ODA = "application/oda";
    public static final String MIME_APPLICATION_RDF_XML = "application/rdf+xml";
    public static final String MIME_APPLICATION_JAVA_ARCHIVE = "application/java-archive";
    public static final String MIME_APPLICATION_RDF_SMIL = "application/smil";
    public static final String MIME_APPLICATION_SRGS = "application/srgs";
    public static final String MIME_APPLICATION_SRGS_XML = "application/srgs+xml";
    public static final String MIME_APPLICATION_VND_MIF = "application/vnd.mif";
    public static final String MIME_APPLICATION_VND_MSEXCEL = "application/vnd.ms-excel";
    public static final String MIME_APPLICATION_VND_MSPOWERPOINT = "application/vnd.ms-powerpoint";
    public static final String MIME_APPLICATION_VND_RNREALMEDIA = "application/vnd.rn-realmedia";
    public static final String MIME_APPLICATION_X_BCPIO = "application/x-bcpio";
    public static final String MIME_APPLICATION_X_CDLINK = "application/x-cdlink";
    public static final String MIME_APPLICATION_X_CHESS_PGN = "application/x-chess-pgn";
    public static final String MIME_APPLICATION_X_CPIO = "application/x-cpio";
    public static final String MIME_APPLICATION_X_CSH = "application/x-csh";
    public static final String MIME_APPLICATION_X_DIRECTOR = "application/x-director";
    public static final String MIME_APPLICATION_X_DVI = "application/x-dvi";
    public static final String MIME_APPLICATION_X_FUTURESPLASH = "application/x-futuresplash";
    public static final String MIME_APPLICATION_X_GTAR = "application/x-gtar";
    public static final String MIME_APPLICATION_X_HDF = "application/x-hdf";
    public static final String MIME_APPLICATION_X_JAVASCRIPT = "application/x-javascript";
    public static final String MIME_APPLICATION_X_KOAN = "application/x-koan";
    public static final String MIME_APPLICATION_X_LATEX = "application/x-latex";
    public static final String MIME_APPLICATION_X_NETCDF = "application/x-netcdf";
    public static final String MIME_APPLICATION_X_OGG = "application/x-ogg";
    public static final String MIME_APPLICATION_X_SH = "application/x-sh";
    public static final String MIME_APPLICATION_X_SHAR = "application/x-shar";
    public static final String MIME_APPLICATION_X_SHOCKWAVE_FLASH = "application/x-shockwave-flash";
    public static final String MIME_APPLICATION_X_STUFFIT = "application/x-stuffit";
    public static final String MIME_APPLICATION_X_SV4CPIO = "application/x-sv4cpio";
    public static final String MIME_APPLICATION_X_SV4CRC = "application/x-sv4crc";
    public static final String MIME_APPLICATION_X_TAR = "application/x-tar";
    public static final String MIME_APPLICATION_X_RAR_COMPRESSED = "application/x-rar-compressed";
    public static final String MIME_APPLICATION_X_TCL = "application/x-tcl";
    public static final String MIME_APPLICATION_X_TEX = "application/x-tex";
    public static final String MIME_APPLICATION_X_TEXINFO = "application/x-texinfo";
    public static final String MIME_APPLICATION_X_TROFF = "application/x-troff";
    public static final String MIME_APPLICATION_X_TROFF_MAN = "application/x-troff-man";
    public static final String MIME_APPLICATION_X_TROFF_ME = "application/x-troff-me";
    public static final String MIME_APPLICATION_X_TROFF_MS = "application/x-troff-ms";
    public static final String MIME_APPLICATION_X_USTAR = "application/x-ustar";
    public static final String MIME_APPLICATION_X_WAIS_SOURCE = "application/x-wais-source";
    public static final String MIME_APPLICATION_VND_MOZZILLA_XUL_XML = "application/vnd.mozilla.xul+xml";
    public static final String MIME_APPLICATION_XHTML_XML = "application/xhtml+xml";
    public static final String MIME_APPLICATION_XSLT_XML = "application/xslt+xml";
    public static final String MIME_APPLICATION_XML = "application/xml";
    public static final String MIME_APPLICATION_XML_DTD = "application/xml-dtd";
    public static final String MIME_IMAGE_BMP = "image/bmp";
    public static final String MIME_IMAGE_CGM = "image/cgm";
    public static final String MIME_IMAGE_GIF = "image/gif";
    public static final String MIME_IMAGE_IEF = "image/ief";
    public static final String MIME_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_IMAGE_TIFF = "image/tiff";
    public static final String MIME_IMAGE_PNG = "image/png";
    public static final String MIME_IMAGE_SVG_XML = "image/svg+xml";
    public static final String MIME_IMAGE_VND_DJVU = "image/vnd.djvu";
    public static final String MIME_IMAGE_WAP_WBMP = "image/vnd.wap.wbmp";
    public static final String MIME_IMAGE_X_CMU_RASTER = "image/x-cmu-raster";
    public static final String MIME_IMAGE_X_ICON = "image/x-icon";
    public static final String MIME_IMAGE_X_PORTABLE_ANYMAP = "image/x-portable-anymap";
    public static final String MIME_IMAGE_X_PORTABLE_BITMAP = "image/x-portable-bitmap";
    public static final String MIME_IMAGE_X_PORTABLE_GRAYMAP = "image/x-portable-graymap";
    public static final String MIME_IMAGE_X_PORTABLE_PIXMAP = "image/x-portable-pixmap";
    public static final String MIME_IMAGE_X_RGB = "image/x-rgb";
    public static final String MIME_AUDIO_BASIC = "audio/basic";
    public static final String MIME_AUDIO_MIDI = "audio/midi";
    public static final String MIME_AUDIO_MPEG = "audio/mpeg";
    public static final String MIME_AUDIO_X_AIFF = "audio/x-aiff";
    public static final String MIME_AUDIO_X_MPEGURL = "audio/x-mpegurl";
    public static final String MIME_AUDIO_X_PN_REALAUDIO = "audio/x-pn-realaudio";
    public static final String MIME_AUDIO_X_WAV = "audio/x-wav";
    public static final String MIME_CHEMICAL_X_PDB = "chemical/x-pdb";
    public static final String MIME_CHEMICAL_X_XYZ = "chemical/x-xyz";
    public static final String MIME_MODEL_IGES = "model/iges";
    public static final String MIME_MODEL_MESH = "model/mesh";
    public static final String MIME_MODEL_VRLM = "model/vrml";
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String MIME_TEXT_RICHTEXT = "text/richtext";
    public static final String MIME_TEXT_RTF = "text/rtf";
    public static final String MIME_TEXT_HTML = "text/html";
    public static final String MIME_TEXT_CALENDAR = "text/calendar";
    public static final String MIME_TEXT_CSS = "text/css";
    public static final String MIME_TEXT_SGML = "text/sgml";
    public static final String MIME_TEXT_TAB_SEPARATED_VALUES = "text/tab-separated-values";
    public static final String MIME_TEXT_VND_WAP_XML = "text/vnd.wap.wml";
    public static final String MIME_TEXT_VND_WAP_WMLSCRIPT = "text/vnd.wap.wmlscript";
    public static final String MIME_TEXT_X_SETEXT = "text/x-setext";
    public static final String MIME_TEXT_X_COMPONENT = "text/x-component";
    public static final String MIME_VIDEO_QUICKTIME = "video/quicktime";
    public static final String MIME_VIDEO_MPEG = "video/mpeg";
    public static final String MIME_VIDEO_VND_MPEGURL = "video/vnd.mpegurl";
    public static final String MIME_VIDEO_X_MSVIDEO = "video/x-msvideo";
    public static final String MIME_VIDEO_X_MS_WMV = "video/x-ms-wmv";
    public static final String MIME_VIDEO_X_SGI_MOVIE = "video/x-sgi-movie";
    public static final String MIME_X_CONFERENCE_X_COOLTALK = "x-conference/x-cooltalk";

    private static HashMap<String, String> mimeTypeMapping;

    static
    {
	mimeTypeMapping = new HashMap<String, String>(200)
	{
	    /**
	     * 
	     */
	    private static final long serialVersionUID = 504794795433631388L;

	    {
		put("xul", MIME_APPLICATION_VND_MOZZILLA_XUL_XML);
		put("json", MIME_APPLICATION_JSON);
		put("ice", MIME_X_CONFERENCE_X_COOLTALK);
		put("movie", MIME_VIDEO_X_SGI_MOVIE);
		put("avi", MIME_VIDEO_X_MSVIDEO);
		put("wmv", MIME_VIDEO_X_MS_WMV);
		put("m4u", MIME_VIDEO_VND_MPEGURL);
		put("mxu", MIME_VIDEO_VND_MPEGURL);
		put("htc", MIME_TEXT_X_COMPONENT);
		put("etx", MIME_TEXT_X_SETEXT);
		put("wmls", MIME_TEXT_VND_WAP_WMLSCRIPT);
		put("wml", MIME_TEXT_VND_WAP_XML);
		put("tsv", MIME_TEXT_TAB_SEPARATED_VALUES);
		put("sgm", MIME_TEXT_SGML);
		put("sgml", MIME_TEXT_SGML);
		put("css", MIME_TEXT_CSS);
		put("ifb", MIME_TEXT_CALENDAR);
		put("ics", MIME_TEXT_CALENDAR);
		put("wrl", MIME_MODEL_VRLM);
		put("vrlm", MIME_MODEL_VRLM);
		put("silo", MIME_MODEL_MESH);
		put("mesh", MIME_MODEL_MESH);
		put("msh", MIME_MODEL_MESH);
		put("iges", MIME_MODEL_IGES);
		put("igs", MIME_MODEL_IGES);
		put("rgb", MIME_IMAGE_X_RGB);
		put("ppm", MIME_IMAGE_X_PORTABLE_PIXMAP);
		put("pgm", MIME_IMAGE_X_PORTABLE_GRAYMAP);
		put("pbm", MIME_IMAGE_X_PORTABLE_BITMAP);
		put("pnm", MIME_IMAGE_X_PORTABLE_ANYMAP);
		put("ico", MIME_IMAGE_X_ICON);
		put("ras", MIME_IMAGE_X_CMU_RASTER);
		put("wbmp", MIME_IMAGE_WAP_WBMP);
		put("djv", MIME_IMAGE_VND_DJVU);
		put("djvu", MIME_IMAGE_VND_DJVU);
		put("svg", MIME_IMAGE_SVG_XML);
		put("ief", MIME_IMAGE_IEF);
		put("cgm", MIME_IMAGE_CGM);
		put("bmp", MIME_IMAGE_BMP);
		put("xyz", MIME_CHEMICAL_X_XYZ);
		put("pdb", MIME_CHEMICAL_X_PDB);
		put("ra", MIME_AUDIO_X_PN_REALAUDIO);
		put("ram", MIME_AUDIO_X_PN_REALAUDIO);
		put("m3u", MIME_AUDIO_X_MPEGURL);
		put("aifc", MIME_AUDIO_X_AIFF);
		put("aif", MIME_AUDIO_X_AIFF);
		put("aiff", MIME_AUDIO_X_AIFF);
		put("mp3", MIME_AUDIO_MPEG);
		put("mp2", MIME_AUDIO_MPEG);
		put("mp1", MIME_AUDIO_MPEG);
		put("mpga", MIME_AUDIO_MPEG);
		put("kar", MIME_AUDIO_MIDI);
		put("mid", MIME_AUDIO_MIDI);
		put("midi", MIME_AUDIO_MIDI);
		put("dtd", MIME_APPLICATION_XML_DTD);
		put("xsl", MIME_APPLICATION_XML);
		put("xml", MIME_APPLICATION_XML);
		put("xslt", MIME_APPLICATION_XSLT_XML);
		put("xht", MIME_APPLICATION_XHTML_XML);
		put("xhtml", MIME_APPLICATION_XHTML_XML);
		put("src", MIME_APPLICATION_X_WAIS_SOURCE);
		put("ustar", MIME_APPLICATION_X_USTAR);
		put("ms", MIME_APPLICATION_X_TROFF_MS);
		put("me", MIME_APPLICATION_X_TROFF_ME);
		put("man", MIME_APPLICATION_X_TROFF_MAN);
		put("roff", MIME_APPLICATION_X_TROFF);
		put("tr", MIME_APPLICATION_X_TROFF);
		put("t", MIME_APPLICATION_X_TROFF);
		put("texi", MIME_APPLICATION_X_TEXINFO);
		put("texinfo", MIME_APPLICATION_X_TEXINFO);
		put("tex", MIME_APPLICATION_X_TEX);
		put("tcl", MIME_APPLICATION_X_TCL);
		put("sv4crc", MIME_APPLICATION_X_SV4CRC);
		put("sv4cpio", MIME_APPLICATION_X_SV4CPIO);
		put("sit", MIME_APPLICATION_X_STUFFIT);
		put("swf", MIME_APPLICATION_X_SHOCKWAVE_FLASH);
		put("shar", MIME_APPLICATION_X_SHAR);
		put("sh", MIME_APPLICATION_X_SH);
		put("cdf", MIME_APPLICATION_X_NETCDF);
		put("nc", MIME_APPLICATION_X_NETCDF);
		put("latex", MIME_APPLICATION_X_LATEX);
		put("skm", MIME_APPLICATION_X_KOAN);
		put("skt", MIME_APPLICATION_X_KOAN);
		put("skd", MIME_APPLICATION_X_KOAN);
		put("skp", MIME_APPLICATION_X_KOAN);
		put("js", MIME_APPLICATION_X_JAVASCRIPT);
		put("hdf", MIME_APPLICATION_X_HDF);
		put("gtar", MIME_APPLICATION_X_GTAR);
		put("spl", MIME_APPLICATION_X_FUTURESPLASH);
		put("dvi", MIME_APPLICATION_X_DVI);
		put("dxr", MIME_APPLICATION_X_DIRECTOR);
		put("dir", MIME_APPLICATION_X_DIRECTOR);
		put("dcr", MIME_APPLICATION_X_DIRECTOR);
		put("csh", MIME_APPLICATION_X_CSH);
		put("cpio", MIME_APPLICATION_X_CPIO);
		put("pgn", MIME_APPLICATION_X_CHESS_PGN);
		put("vcd", MIME_APPLICATION_X_CDLINK);
		put("bcpio", MIME_APPLICATION_X_BCPIO);
		put("rm", MIME_APPLICATION_VND_RNREALMEDIA);
		put("ppt", MIME_APPLICATION_VND_MSPOWERPOINT);
		put("mif", MIME_APPLICATION_VND_MIF);
		put("grxml", MIME_APPLICATION_SRGS_XML);
		put("gram", MIME_APPLICATION_SRGS);
		put("smil", MIME_APPLICATION_RDF_SMIL);
		put("smi", MIME_APPLICATION_RDF_SMIL);
		put("rdf", MIME_APPLICATION_RDF_XML);
		put("ogg", MIME_APPLICATION_X_OGG);
		put("oda", MIME_APPLICATION_ODA);
		put("dmg", MIME_APPLICATION_OCTET_STREAM);
		put("lzh", MIME_APPLICATION_OCTET_STREAM);
		put("so", MIME_APPLICATION_OCTET_STREAM);
		put("lha", MIME_APPLICATION_OCTET_STREAM);
		put("dms", MIME_APPLICATION_OCTET_STREAM);
		put("bin", MIME_APPLICATION_OCTET_STREAM);
		put("mathml", MIME_APPLICATION_MATHML_XML);
		put("cpt", MIME_APPLICATION_MAC_COMPACTPRO);
		put("hqx", MIME_APPLICATION_MAC_BINHEX40);
		put("jnlp", MIME_APPLICATION_JNLP);
		put("ez", MIME_APPLICATION_ANDREW_INSET);
		put("txt", MIME_TEXT_PLAIN);
		put("ini", MIME_TEXT_PLAIN);
		put("c", MIME_TEXT_PLAIN);
		put("h", MIME_TEXT_PLAIN);
		put("cpp", MIME_TEXT_PLAIN);
		put("cxx", MIME_TEXT_PLAIN);
		put("cc", MIME_TEXT_PLAIN);
		put("chh", MIME_TEXT_PLAIN);
		put("java", MIME_TEXT_PLAIN);
		put("csv", MIME_TEXT_PLAIN);
		put("bat", MIME_TEXT_PLAIN);
		put("cmd", MIME_TEXT_PLAIN);
		put("asc", MIME_TEXT_PLAIN);
		put("rtf", MIME_TEXT_RTF);
		put("rtx", MIME_TEXT_RICHTEXT);
		put("html", MIME_TEXT_HTML);
		put("htm", MIME_TEXT_HTML);
		put("zip", MIME_APPLICATION_ZIP);
		put("rar", MIME_APPLICATION_X_RAR_COMPRESSED);
		put("gzip", MIME_APPLICATION_X_GZIP);
		put("gz", MIME_APPLICATION_X_GZIP);
		put("tgz", MIME_APPLICATION_TGZ);
		put("tar", MIME_APPLICATION_X_TAR);
		put("gif", MIME_IMAGE_GIF);
		put("jpeg", MIME_IMAGE_JPEG);
		put("jpg", MIME_IMAGE_JPEG);
		put("jpe", MIME_IMAGE_JPEG);
		put("tiff", MIME_IMAGE_TIFF);
		put("tif", MIME_IMAGE_TIFF);
		put("png", MIME_IMAGE_PNG);
		put("au", MIME_AUDIO_BASIC);
		put("snd", MIME_AUDIO_BASIC);
		put("wav", MIME_AUDIO_X_WAV);
		put("mov", MIME_VIDEO_QUICKTIME);
		put("qt", MIME_VIDEO_QUICKTIME);
		put("mpeg", MIME_VIDEO_MPEG);
		put("mpg", MIME_VIDEO_MPEG);
		put("mpe", MIME_VIDEO_MPEG);
		put("abs", MIME_VIDEO_MPEG);
		put("doc", MIME_APPLICATION_MSWORD);
		put("xls", MIME_APPLICATION_VND_MSEXCEL);
		put("eps", MIME_APPLICATION_POSTSCRIPT);
		put("ai", MIME_APPLICATION_POSTSCRIPT);
		put("ps", MIME_APPLICATION_POSTSCRIPT);
		put("pdf", MIME_APPLICATION_PDF);
		put("exe", MIME_APPLICATION_OCTET_STREAM);
		put("dll", MIME_APPLICATION_OCTET_STREAM);
		put("class", MIME_APPLICATION_OCTET_STREAM);
		put("jar", MIME_APPLICATION_JAVA_ARCHIVE);

		// MS Office
		// put("doc", "application/msword");
		put("dot", "application/msword");
		put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
		put("docm", "application/vnd.ms-word.document.macroEnabled.12");
		put("dotm", "application/vnd.ms-word.template.macroEnabled.12");
		// put("xls", "application/vnd.ms-excel");
		put("xlt", "application/vnd.ms-excel");
		put("xla", "application/vnd.ms-excel");
		put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
		put("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12");
		put("xltm", "application/vnd.ms-excel.template.macroEnabled.12");
		put("xlam", "application/vnd.ms-excel.addin.macroEnabled.12");
		put("xlsb", "application/vnd.ms-excel.sheet.binary.macroEnabled.12");
		//put("ppt", "application/vnd.ms-powerpoint");
		put("pot", "application/vnd.ms-powerpoint");
		put("pps", "application/vnd.ms-powerpoint");
		put("ppa", "application/vnd.ms-powerpoint");
		put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		put("potx", "application/vnd.openxmlformats-officedocument.presentationml.template");
		put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
		put("ppam", "application/vnd.ms-powerpoint.addin.macroEnabled.12");
		put("pptm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12");
		put("potm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12");
		put("ppsm", "application/vnd.ms-powerpoint.slideshow.macroEnabled.12");
		// Open Office
		put("odt", "application/vnd.oasis.opendocument.text");
		put("ott", "application/vnd.oasis.opendocument.text-template");
		put("oth", "application/vnd.oasis.opendocument.text-web");
		put("odm", "application/vnd.oasis.opendocument.text-master");
		put("odg", "application/vnd.oasis.opendocument.graphics");
		put("otg", "application/vnd.oasis.opendocument.graphics-template");
		put("odp", "application/vnd.oasis.opendocument.presentation");
		put("otp", "application/vnd.oasis.opendocument.presentation-template");
		put("ods", "application/vnd.oasis.opendocument.spreadsheet");
		put("ots", "application/vnd.oasis.opendocument.spreadsheet-template");
		put("odc", "application/vnd.oasis.opendocument.chart");
		put("odf", "application/vnd.oasis.opendocument.formula");
		put("odb", "application/vnd.oasis.opendocument.database");
		put("odi", "application/vnd.oasis.opendocument.image");
		put("oxt", "application/vnd.openofficeorg.extension");

		put("mp4", "video/mp4");
		put("mkv", "video/x-matroska");

	    }
	};
    }

    /**
     * Registers MIME type for provided extension. Existing extension type will be overriden.
     */
    public static void registerMimeType(String ext, String mimeType)
    {
	mimeTypeMapping.put(ext, mimeType);
    }

    /**
     * Returns the corresponding MIME type to the given extension.
     * If no MIME type was found it returns 'application/octet-stream' type.
     */
    public static String getMimeType(String ext)
    {
	String mimeType = lookupMimeType(ext);
	if(mimeType == null)
	{
	    mimeType = MIME_APPLICATION_OCTET_STREAM;
	}
	return mimeType;
    }

    /**
     * Simply returns MIME type or <code>null</code> if no type is found.
     */
    public static String lookupMimeType(String ext)
    {
	return mimeTypeMapping.get(ext.toLowerCase());
    }
}
