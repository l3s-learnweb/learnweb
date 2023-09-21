package de.l3s.thumbmaker;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;

@Disabled("To avoid live requests on CI")
class ThumbMakerTest {
    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final ThumbMaker thumbMaker = new ThumbMaker(learnwebExt.getLearnweb().getConfigProvider().getProperty("integration_thumbmaker_url"));

    @Test
    void makeScreenshot() throws IOException {
        InputStream stream = thumbMaker.makeScreenshot("https://www.google.com", ThumbOptions.screenshot().width(1024).fullPage(true));
        assertNotNull(stream);

        File targetFile = new File("target/screenshot.jpg");
        Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void makeFilePreview() throws IOException {
        InputStream stream = thumbMaker.makeFilePreview("https://www.africau.edu/images/default/sample.pdf", ThumbOptions.file().format("png"));
        assertNotNull(stream);

        File targetFile = new File("target/filepreview_new.png");
        Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
