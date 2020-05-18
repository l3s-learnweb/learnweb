package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import de.l3s.learnweb.resource.office.history.model.CallbackData;

@Disabled
class ExampleTest {
    private static final Logger log = LogManager.getLogger(ExampleTest.class);

    @Test
    void gson() {
        String json = "{\"key\":\"1587932306000726871\",\"status\":2,\"url\":\"https://lw-office.l3s.uni-hannover.de/cache/files/1587932306000726871_8721/output.docx/output.docx?md5=gSB3WrZazSY42uG1p3vnhQ==&expires=1587982554&disposition=attachment&ooname=output.docx\",\"changesurl\":\"https://lw-office.l3s.uni-hannover.de/cache/files/1587932306000726871_8721/changes.zip/changes.zip?md5=y545GPyIoS-Rxhikiv8iFA==&expires=1587982554&disposition=attachment&ooname=output.zip\",\"history\":{\"serverVersion\":\"5.1.4\",\"changes\":[{\"created\":\"2020-04-27 10:00:35\",\"user\":{\"id\":\"9289\",\"name\":\"astappiev\"}}]},\"users\":[\"9289\"],\"actions\":[{\"type\":0,\"userid\":\"9289\"}],\"lastsave\":\"2020-04-27T10:00:43.252Z\",\"notmodified\":false}";

        Gson gson = new Gson();
        CallbackData callbackData = gson.fromJson(json, CallbackData.class);

        assertEquals("https://lw-office.l3s.uni-hannover.de/cache/files/1587932306000726871_8721/changes.zip/changes.zip?md5=y545GPyIoS-Rxhikiv8iFA==&expires=1587982554&disposition=attachment&ooname=output.zip", callbackData.getChangesUrl());
    }
}
