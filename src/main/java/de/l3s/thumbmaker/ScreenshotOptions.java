package de.l3s.thumbmaker;

import java.io.Serial;
import java.io.Serializable;

public class ScreenshotOptions extends ThumbOptions implements Serializable {
    @Serial
    private static final long serialVersionUID = 9108651028356549301L;

    private Integer width;
    private Integer height;
    private String format;
    private Integer quality;
    private Boolean fullPage;

    protected ScreenshotOptions() {
    }

    public ScreenshotOptions width(final int width) {
        this.width = width;
        return this;
    }

    public ScreenshotOptions height(final int height) {
        this.height = height;
        return this;
    }

    public ScreenshotOptions format(final String format) {
        this.format = format;
        return this;
    }

    public ScreenshotOptions quality(final int quality) {
        this.quality = quality;
        return this;
    }

    public ScreenshotOptions fullPage(final boolean fullPage) {
        this.fullPage = fullPage;
        return this;
    }

    public ThumbOptions build() {
        return this;
    }
}
