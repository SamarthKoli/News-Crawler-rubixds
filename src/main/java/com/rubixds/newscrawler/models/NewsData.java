package com.rubixds.newscrawler.models;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "articles") // Creates a collection named 'articles' in Mongo
public class NewsData {

    @Id
    private String id; // MongoDB needs a unique ID

    @JsonProperty("title")
    private String title;
    
    private String url;
    private String source;
    private String publishedTime;

    @JsonProperty("full_text")
    private String fullText;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("sentiment_label")
    private String sentimentLabel;

    @JsonProperty("sentiment_score")
    private double sentimentScore;

    private List<String> keywords;
}