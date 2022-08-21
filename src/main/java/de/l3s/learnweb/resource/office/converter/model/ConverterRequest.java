package de.l3s.learnweb.resource.office.converter.model;

public class ConverterRequest {
    private final String fileType;
    private final String outputType;
    private final String title;
    private final String url;
    private final String key;
    private final OfficeThumbnailParams thumbnail;

    public ConverterRequest(String fileType, String outputType, String title, String url, String key) {
        this.fileType = fileType.replace(".", "");
        this.outputType = outputType.replace(".", "");
        this.title = title;
        this.url = url;
        this.key = key;
        this.thumbnail = new OfficeThumbnailParams();
    }

    public String getFileType() {
        return fileType;
    }

    public String getOutputType() {
        return outputType;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    public OfficeThumbnailParams getThumbnail() {
        return thumbnail;
    }

    @Override
    public String toString() {
        return "ConverterRequest [fileType=" + fileType + ", outputType=" + outputType + ", title=" + title +
            ", url=" + url + ", key=" + key + ", thumbnail=" + thumbnail + "]";
    }

    public static class OfficeThumbnailParams {
        private boolean first = true;
        private int aspect;
        private int height = 1024; // the thumbnail height in pixels
        private int width = 1280; // the thumbnail width in pixels

        public boolean isFirst() {
            return first;
        }

        public void setFirst(boolean first) {
            this.first = first;
        }

        public int getAspect() {
            return aspect;
        }

        public void setAspect(int aspect) {
            this.aspect = aspect;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        @Override
        public String toString() {
            return "OfficeThumbnailParams [first=" + first + ", aspect=" + aspect + ", height=" + height + ", width=" + width + "]";
        }
    }
}
