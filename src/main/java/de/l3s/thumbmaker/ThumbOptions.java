package de.l3s.thumbmaker;

@SuppressWarnings({"ClassReferencesSubclass", "NonFinalUtilityClass"})
public class ThumbOptions {
    protected ThumbOptions() {
    }

    public static ScreenshotOptions screenshot() {
        return new ScreenshotOptions();
    }

    public static FilePreviewOptions file() {
        return new FilePreviewOptions();
    }
}
