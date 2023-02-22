package de.l3s.thumbmaker;

import java.io.Serial;
import java.io.Serializable;

public class FilePreviewOptions extends ThumbOptions implements Serializable {
    @Serial
    private static final long serialVersionUID = 9108651028356549301L;

    private Integer width;
    private Integer height;
    private String format;
    private Boolean crop;
    private Boolean ignoreAspect;
    private Boolean oversize;
    private Boolean shrink;
    private Boolean enlarge;
    private Boolean thumbnail;
    private Integer quality;
    private Integer density;
    private String background;

    protected FilePreviewOptions() {
    }

    public FilePreviewOptions width(final int width) {
        this.width = width;
        return this;
    }

    public FilePreviewOptions height(final int height) {
        this.height = height;
        return this;
    }

    public FilePreviewOptions format(final String format) {
        this.format = format;
        return this;
    }

    public FilePreviewOptions crop() {
        this.crop = true;
        return this;
    }

    public FilePreviewOptions ignoreAspect() {
        this.ignoreAspect = true;
        return this;
    }

    public FilePreviewOptions oversize() {
        this.oversize = true;
        return this;
    }

    public FilePreviewOptions shrink() {
        this.shrink = true;
        return this;
    }

    public FilePreviewOptions enlarge() {
        this.enlarge = true;
        return this;
    }

    public FilePreviewOptions thumbnail() {
        this.thumbnail = true;
        return this;
    }

    public FilePreviewOptions quality(final int quality) {
        this.quality = quality;
        return this;
    }

    public FilePreviewOptions density(final int density) {
        this.density = density;
        return this;
    }

    public FilePreviewOptions background(final String background) {
        this.background = background;
        return this;
    }

    public ThumbOptions build() {
        return this;
    }
}
