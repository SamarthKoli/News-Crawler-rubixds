package com.rubixds.newscrawler.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rubixds.newscrawler.models.NewsData;
import com.rubixds.newscrawler.services.NewsCrawlerService;

@RestController
@RequestMapping("news/api/v1")
public class NewsController {

    @Autowired
    private NewsCrawlerService newsCrawlerService;


    @GetMapping("/scrap")
    public ResponseEntity<List<NewsData>> newsScrapper() throws IOException {
        List<NewsData> news = newsCrawlerService.GoogleNewsScrapper();
        return ResponseEntity.ok(news);
    }
}