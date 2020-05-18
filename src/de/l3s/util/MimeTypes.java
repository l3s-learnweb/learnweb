package de.l3s.util;

//Copyright (c) 2003-2009, Jodd Team (jodd.org). All Rights Reserved.

import java.util.Map;

/**
 * Map file extensions to MIME types. Based on the Apache mime.types file.
 * http://www.iana.org/assignments/media-types/
 */
public final class MimeTypes {
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

    private static final Map<String, String> MIME_TYPE_MAPPING = Map.ofEntries(
        Map.entry("xul", MIME_APPLICATION_VND_MOZZILLA_XUL_XML),
        Map.entry("json", MIME_APPLICATION_JSON),
        Map.entry("ice", MIME_X_CONFERENCE_X_COOLTALK),
        Map.entry("movie", MIME_VIDEO_X_SGI_MOVIE),
        Map.entry("avi", MIME_VIDEO_X_MSVIDEO),
        Map.entry("wmv", MIME_VIDEO_X_MS_WMV),
        Map.entry("m4u", MIME_VIDEO_VND_MPEGURL),
        Map.entry("mxu", MIME_VIDEO_VND_MPEGURL),
        Map.entry("htc", MIME_TEXT_X_COMPONENT),
        Map.entry("etx", MIME_TEXT_X_SETEXT),
        Map.entry("wmls", MIME_TEXT_VND_WAP_WMLSCRIPT),
        Map.entry("wml", MIME_TEXT_VND_WAP_XML),
        Map.entry("tsv", MIME_TEXT_TAB_SEPARATED_VALUES),
        Map.entry("sgm", MIME_TEXT_SGML),
        Map.entry("sgml", MIME_TEXT_SGML),
        Map.entry("css", MIME_TEXT_CSS),
        Map.entry("ifb", MIME_TEXT_CALENDAR),
        Map.entry("ics", MIME_TEXT_CALENDAR),
        Map.entry("wrl", MIME_MODEL_VRLM),
        Map.entry("vrlm", MIME_MODEL_VRLM),
        Map.entry("silo", MIME_MODEL_MESH),
        Map.entry("mesh", MIME_MODEL_MESH),
        Map.entry("msh", MIME_MODEL_MESH),
        Map.entry("iges", MIME_MODEL_IGES),
        Map.entry("igs", MIME_MODEL_IGES),
        Map.entry("rgb", MIME_IMAGE_X_RGB),
        Map.entry("ppm", MIME_IMAGE_X_PORTABLE_PIXMAP),
        Map.entry("pgm", MIME_IMAGE_X_PORTABLE_GRAYMAP),
        Map.entry("pbm", MIME_IMAGE_X_PORTABLE_BITMAP),
        Map.entry("pnm", MIME_IMAGE_X_PORTABLE_ANYMAP),
        Map.entry("ico", MIME_IMAGE_X_ICON),
        Map.entry("ras", MIME_IMAGE_X_CMU_RASTER),
        Map.entry("wbmp", MIME_IMAGE_WAP_WBMP),
        Map.entry("djv", MIME_IMAGE_VND_DJVU),
        Map.entry("djvu", MIME_IMAGE_VND_DJVU),
        Map.entry("svg", MIME_IMAGE_SVG_XML),
        Map.entry("ief", MIME_IMAGE_IEF),
        Map.entry("cgm", MIME_IMAGE_CGM),
        Map.entry("bmp", MIME_IMAGE_BMP),
        Map.entry("xyz", MIME_CHEMICAL_X_XYZ),
        Map.entry("pdb", MIME_CHEMICAL_X_PDB),
        Map.entry("ra", MIME_AUDIO_X_PN_REALAUDIO),
        Map.entry("ram", MIME_AUDIO_X_PN_REALAUDIO),
        Map.entry("m3u", MIME_AUDIO_X_MPEGURL),
        Map.entry("aifc", MIME_AUDIO_X_AIFF),
        Map.entry("aif", MIME_AUDIO_X_AIFF),
        Map.entry("aiff", MIME_AUDIO_X_AIFF),
        Map.entry("mp3", MIME_AUDIO_MPEG),
        Map.entry("mp2", MIME_AUDIO_MPEG),
        Map.entry("mp1", MIME_AUDIO_MPEG),
        Map.entry("mpga", MIME_AUDIO_MPEG),
        Map.entry("kar", MIME_AUDIO_MIDI),
        Map.entry("mid", MIME_AUDIO_MIDI),
        Map.entry("midi", MIME_AUDIO_MIDI),
        Map.entry("dtd", MIME_APPLICATION_XML_DTD),
        Map.entry("xsl", MIME_APPLICATION_XML),
        Map.entry("xml", MIME_APPLICATION_XML),
        Map.entry("xslt", MIME_APPLICATION_XSLT_XML),
        Map.entry("xht", MIME_APPLICATION_XHTML_XML),
        Map.entry("xhtml", MIME_APPLICATION_XHTML_XML),
        Map.entry("src", MIME_APPLICATION_X_WAIS_SOURCE),
        Map.entry("ustar", MIME_APPLICATION_X_USTAR),
        Map.entry("ms", MIME_APPLICATION_X_TROFF_MS),
        Map.entry("me", MIME_APPLICATION_X_TROFF_ME),
        Map.entry("man", MIME_APPLICATION_X_TROFF_MAN),
        Map.entry("roff", MIME_APPLICATION_X_TROFF),
        Map.entry("tr", MIME_APPLICATION_X_TROFF),
        Map.entry("t", MIME_APPLICATION_X_TROFF),
        Map.entry("texi", MIME_APPLICATION_X_TEXINFO),
        Map.entry("texinfo", MIME_APPLICATION_X_TEXINFO),
        Map.entry("tex", MIME_APPLICATION_X_TEX),
        Map.entry("tcl", MIME_APPLICATION_X_TCL),
        Map.entry("sv4crc", MIME_APPLICATION_X_SV4CRC),
        Map.entry("sv4cpio", MIME_APPLICATION_X_SV4CPIO),
        Map.entry("sit", MIME_APPLICATION_X_STUFFIT),
        Map.entry("swf", MIME_APPLICATION_X_SHOCKWAVE_FLASH),
        Map.entry("shar", MIME_APPLICATION_X_SHAR),
        Map.entry("sh", MIME_APPLICATION_X_SH),
        Map.entry("cdf", MIME_APPLICATION_X_NETCDF),
        Map.entry("nc", MIME_APPLICATION_X_NETCDF),
        Map.entry("latex", MIME_APPLICATION_X_LATEX),
        Map.entry("skm", MIME_APPLICATION_X_KOAN),
        Map.entry("skt", MIME_APPLICATION_X_KOAN),
        Map.entry("skd", MIME_APPLICATION_X_KOAN),
        Map.entry("skp", MIME_APPLICATION_X_KOAN),
        Map.entry("js", MIME_APPLICATION_X_JAVASCRIPT),
        Map.entry("hdf", MIME_APPLICATION_X_HDF),
        Map.entry("gtar", MIME_APPLICATION_X_GTAR),
        Map.entry("spl", MIME_APPLICATION_X_FUTURESPLASH),
        Map.entry("dvi", MIME_APPLICATION_X_DVI),
        Map.entry("dxr", MIME_APPLICATION_X_DIRECTOR),
        Map.entry("dir", MIME_APPLICATION_X_DIRECTOR),
        Map.entry("dcr", MIME_APPLICATION_X_DIRECTOR),
        Map.entry("csh", MIME_APPLICATION_X_CSH),
        Map.entry("cpio", MIME_APPLICATION_X_CPIO),
        Map.entry("pgn", MIME_APPLICATION_X_CHESS_PGN),
        Map.entry("vcd", MIME_APPLICATION_X_CDLINK),
        Map.entry("bcpio", MIME_APPLICATION_X_BCPIO),
        Map.entry("rm", MIME_APPLICATION_VND_RNREALMEDIA),
        Map.entry("ppt", MIME_APPLICATION_VND_MSPOWERPOINT),
        Map.entry("mif", MIME_APPLICATION_VND_MIF),
        Map.entry("grxml", MIME_APPLICATION_SRGS_XML),
        Map.entry("gram", MIME_APPLICATION_SRGS),
        Map.entry("smil", MIME_APPLICATION_RDF_SMIL),
        Map.entry("smi", MIME_APPLICATION_RDF_SMIL),
        Map.entry("rdf", MIME_APPLICATION_RDF_XML),
        Map.entry("ogg", MIME_APPLICATION_X_OGG),
        Map.entry("oda", MIME_APPLICATION_ODA),
        Map.entry("dmg", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("lzh", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("so", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("lha", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("dms", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("bin", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("mathml", MIME_APPLICATION_MATHML_XML),
        Map.entry("cpt", MIME_APPLICATION_MAC_COMPACTPRO),
        Map.entry("hqx", MIME_APPLICATION_MAC_BINHEX40),
        Map.entry("jnlp", MIME_APPLICATION_JNLP),
        Map.entry("ez", MIME_APPLICATION_ANDREW_INSET),
        Map.entry("txt", MIME_TEXT_PLAIN),
        Map.entry("ini", MIME_TEXT_PLAIN),
        Map.entry("c", MIME_TEXT_PLAIN),
        Map.entry("h", MIME_TEXT_PLAIN),
        Map.entry("cpp", MIME_TEXT_PLAIN),
        Map.entry("cxx", MIME_TEXT_PLAIN),
        Map.entry("cc", MIME_TEXT_PLAIN),
        Map.entry("chh", MIME_TEXT_PLAIN),
        Map.entry("java", MIME_TEXT_PLAIN),
        Map.entry("csv", MIME_TEXT_PLAIN),
        Map.entry("bat", MIME_TEXT_PLAIN),
        Map.entry("cmd", MIME_TEXT_PLAIN),
        Map.entry("asc", MIME_TEXT_PLAIN),
        Map.entry("rtf", MIME_TEXT_RTF),
        Map.entry("rtx", MIME_TEXT_RICHTEXT),
        Map.entry("html", MIME_TEXT_HTML),
        Map.entry("htm", MIME_TEXT_HTML),
        Map.entry("zip", MIME_APPLICATION_ZIP),
        Map.entry("rar", MIME_APPLICATION_X_RAR_COMPRESSED),
        Map.entry("gzip", MIME_APPLICATION_X_GZIP),
        Map.entry("gz", MIME_APPLICATION_X_GZIP),
        Map.entry("tgz", MIME_APPLICATION_TGZ),
        Map.entry("tar", MIME_APPLICATION_X_TAR),
        Map.entry("gif", MIME_IMAGE_GIF),
        Map.entry("jpeg", MIME_IMAGE_JPEG),
        Map.entry("jpg", MIME_IMAGE_JPEG),
        Map.entry("jpe", MIME_IMAGE_JPEG),
        Map.entry("tiff", MIME_IMAGE_TIFF),
        Map.entry("tif", MIME_IMAGE_TIFF),
        Map.entry("png", MIME_IMAGE_PNG),
        Map.entry("au", MIME_AUDIO_BASIC),
        Map.entry("snd", MIME_AUDIO_BASIC),
        Map.entry("wav", MIME_AUDIO_X_WAV),
        Map.entry("mov", MIME_VIDEO_QUICKTIME),
        Map.entry("qt", MIME_VIDEO_QUICKTIME),
        Map.entry("mpeg", MIME_VIDEO_MPEG),
        Map.entry("mpg", MIME_VIDEO_MPEG),
        Map.entry("mpe", MIME_VIDEO_MPEG),
        Map.entry("abs", MIME_VIDEO_MPEG),
        Map.entry("doc", MIME_APPLICATION_MSWORD),
        Map.entry("xls", MIME_APPLICATION_VND_MSEXCEL),
        Map.entry("eps", MIME_APPLICATION_POSTSCRIPT),
        Map.entry("ai", MIME_APPLICATION_POSTSCRIPT),
        Map.entry("ps", MIME_APPLICATION_POSTSCRIPT),
        Map.entry("pdf", MIME_APPLICATION_PDF),
        Map.entry("exe", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("dll", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("class", MIME_APPLICATION_OCTET_STREAM),
        Map.entry("jar", MIME_APPLICATION_JAVA_ARCHIVE),

        // MS Office
        // Map.entry("doc", "application/msword"),
        Map.entry("dot", "application/msword"),
        Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        Map.entry("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template"),
        Map.entry("docm", "application/vnd.ms-word.document.macroEnabled.12"),
        Map.entry("dotm", "application/vnd.ms-word.template.macroEnabled.12"),
        // Map.entry("xls", "application/vnd.ms-excel"),
        Map.entry("xlt", "application/vnd.ms-excel"),
        Map.entry("xla", "application/vnd.ms-excel"),
        Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        Map.entry("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template"),
        Map.entry("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12"),
        Map.entry("xltm", "application/vnd.ms-excel.template.macroEnabled.12"),
        Map.entry("xlam", "application/vnd.ms-excel.addin.macroEnabled.12"),
        Map.entry("xlsb", "application/vnd.ms-excel.sheet.binary.macroEnabled.12"),
        //Map.entry("ppt", "application/vnd.ms-powerpoint"),
        Map.entry("pot", "application/vnd.ms-powerpoint"),
        Map.entry("pps", "application/vnd.ms-powerpoint"),
        Map.entry("ppa", "application/vnd.ms-powerpoint"),
        Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        Map.entry("potx", "application/vnd.openxmlformats-officedocument.presentationml.template"),
        Map.entry("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow"),
        Map.entry("ppam", "application/vnd.ms-powerpoint.addin.macroEnabled.12"),
        Map.entry("pptm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12"),
        Map.entry("potm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12"),
        Map.entry("ppsm", "application/vnd.ms-powerpoint.slideshow.macroEnabled.12"),
        // Open Office
        Map.entry("odt", "application/vnd.oasis.opendocument.text"),
        Map.entry("ott", "application/vnd.oasis.opendocument.text-template"),
        Map.entry("oth", "application/vnd.oasis.opendocument.text-web"),
        Map.entry("odm", "application/vnd.oasis.opendocument.text-master"),
        Map.entry("odg", "application/vnd.oasis.opendocument.graphics"),
        Map.entry("otg", "application/vnd.oasis.opendocument.graphics-template"),
        Map.entry("odp", "application/vnd.oasis.opendocument.presentation"),
        Map.entry("otp", "application/vnd.oasis.opendocument.presentation-template"),
        Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
        Map.entry("ots", "application/vnd.oasis.opendocument.spreadsheet-template"),
        Map.entry("odc", "application/vnd.oasis.opendocument.chart"),
        Map.entry("odf", "application/vnd.oasis.opendocument.formula"),
        Map.entry("odb", "application/vnd.oasis.opendocument.database"),
        Map.entry("odi", "application/vnd.oasis.opendocument.image"),
        Map.entry("oxt", "application/vnd.openofficeorg.extension"),

        Map.entry("mp4", "video/mp4"),
        Map.entry("mkv", "video/x-matroska")
    );

    /**
     * Returns the corresponding MIME type to the given extension.
     * If no MIME type was found it returns 'application/octet-stream' type.
     */
    public static String getMimeType(String ext) {
        String mimeType = lookupMimeType(ext);
        if (mimeType == null) {
            mimeType = MIME_APPLICATION_OCTET_STREAM;
        }
        return mimeType;
    }

    /**
     * Simply returns MIME type or {@code null} if no type is found.
     */
    public static String lookupMimeType(String ext) {
        return MIME_TYPE_MAPPING.get(ext.toLowerCase());
    }
}
