package de.l3s.thumbmaker;

@SuppressWarnings("ClassReferencesSubclass")
public interface ThumbOptions {

    static ScreenshotOptions screenshot() {
        return new ScreenshotOptions();
    }

    static FilePreviewOptions file() {
        return new FilePreviewOptions();
    }
}
