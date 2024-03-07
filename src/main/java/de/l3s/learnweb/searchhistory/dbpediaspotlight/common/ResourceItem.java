package de.l3s.learnweb.searchhistory.dbpediaspotlight.common;

import static de.l3s.learnweb.searchhistory.dbpediaspotlight.common.Constants.COMMA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ResourceItem {
    @SerializedName("@URI")
    private String uri;

    @SerializedName("@support")
    private String support;

    @SerializedName("@types")
    private String types;

    @SerializedName("@surfaceForm")
    private String surfaceForm;

    @SerializedName("@offset")
    private String offSet;

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(final String support) {
        this.support = support;
    }

    public String getTypes() {
        return types;
    }

    public void setTypes(final String types) {
        this.types = types;
    }

    public String getSurfaceForm() {
        return surfaceForm;
    }

    public void setSurfaceForm(final String surfaceForm) {
        this.surfaceForm = surfaceForm;
    }

    public String getOffSet() {
        return offSet;
    }

    public void setOffSet(final String offSet) {
        this.offSet = offSet;
    }

    public String getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(final String similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getPercentageOfSecondRank() {
        return percentageOfSecondRank;
    }

    public void setPercentageOfSecondRank(final String percentageOfSecondRank) {
        this.percentageOfSecondRank = percentageOfSecondRank;
    }

    @SerializedName("@similarityScore")
    private String similarityScore;

    @SerializedName("@percentageOfSecondRank")
    private String percentageOfSecondRank;

    public Integer beginIndex() {
        try {
            return Integer.valueOf(offSet);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Integer endIndex() {
        if (surfaceForm != null) {
            return beginIndex() + surfaceForm.length();
        }

        return 0;
    }

    public List<String> typesList() {
        if (types != null && !types.isEmpty()) {
            return Arrays.asList(types.split(COMMA));
        }

        return new ArrayList<>();
    }

    public Double score() {
        try {
            return Double.valueOf(similarityScore);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }
}
