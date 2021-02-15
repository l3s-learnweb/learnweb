package de.l3s.learnweb.resource.ted;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class TedManagerTest {
    private static final Logger log = LogManager.getLogger(TedManagerTest.class);
    private static final String VIDEO_ID = "7TXEZ4tP06c";
    private static final String VIDEO_CC_LANG = "es";

    @Test
    void fetchTedXTranscriptsTest() {
        String respXml = TedManager.getTedxData("https://www.youtube.com/api/timedtext?type=list&v=" + VIDEO_ID);

        Document doc = Jsoup.parse(respXml, "", Parser.xmlParser());
        Elements tracks = doc.select("transcript_list track");
        if (!tracks.isEmpty()) {
            for (Element track : tracks) {
                String langCode = track.attr("lang_code");
                String langName = track.attr("lang_translated");

                log.info("{} - {}", langCode, langName);
            }
        }
    }

    @Test
    void insertTedXTranscriptsTest() {
        String respXml = TedManager.getTedxData("https://www.youtube.com/api/timedtext?lang=" + VIDEO_CC_LANG + "&v=" + VIDEO_ID);

        Document doc = Jsoup.parse(respXml, "", Parser.xmlParser());
        Elements texts = doc.select("transcript text");
        if (!texts.isEmpty()) {
            for (Element text : texts) {
                double start = Double.parseDouble(text.attr("start"));
                double duration = Double.parseDouble(text.attr("dur"));
                String paragraph = text.text().replace("\n", " ");

                int startTimeInt = (int) (start * 1000);
                log.info("{}[{}]: {}", startTimeInt, duration, paragraph);
            }
        }
    }
}
