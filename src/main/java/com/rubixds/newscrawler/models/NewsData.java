package com.rubixds.newscrawler.models;

import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
public class NewsData {


    private String Title;
    private String url;
    private String source;
    private String publishedTime;

    private String fullText;
    private String summary;
    private String sentimentLabel;
    private double sentimentScore;



}
