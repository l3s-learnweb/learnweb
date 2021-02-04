package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.office.converter.model.ConverterRequest;

class ConverterServiceTest {
    private static final Logger log = LogManager.getLogger(ConverterServiceTest.class);

    @Test
    void convert() throws SQLException, ClassNotFoundException, IOException {
        Learnweb learnweb = Learnweb.createInstance("https://learnweb.l3s.uni-hannover.de");

        ConverterRequest request = new ConverterRequest("ods", "png", "test",
            "https://learnweb.l3s.uni-hannover.de/download/732495/test.ods", "1");
        String thumbnail = ConverterService.convert(learnweb, request);

        log.info(thumbnail);
    }
}
