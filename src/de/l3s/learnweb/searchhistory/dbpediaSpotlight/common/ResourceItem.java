package de.l3s.learnweb.searchhistory.dbpediaSpotlight.common;

import static de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.Constants.COMMA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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