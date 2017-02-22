package de.l3s.glossary;

import java.io.File;
import java.net.ConnectException;

import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

public class test
{
    public static void main(String args[]) throws ConnectException
    {
        File inputFile = new File("/Users/Rishita/Downloads/questioarioVenuti.odt");
        File outputFile = new File("/Users/Rishita/Downloads/questioarioVenuti.pdf");

        // connect to an OpenOffice.org instance running on port 8100
        OpenOfficeConnection connection = new SocketOpenOfficeConnection(8100);
        connection.connect();

        // convert
        DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
        converter.convert(inputFile, outputFile);

        // close the connection
        connection.disconnect();

    }
}
