package com.rubixds.newscrawler.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rometools.fetcher.FetcherException;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
import com.rubixds.newscrawler.models.NewsData;
import com.rubixds.newscrawler.services.NewsCrawlerService;

@RestController
@RequestMapping("news/api/v1")
public class NewsController {


    @Autowired
    private NewsCrawlerService newsCrawlerService;


    @GetMapping("/collect")
    public ResponseEntity<?>getLatestNews()throws IOException,FeedException,FetcherException{
        List<SyndEntry>entries=newsCrawlerService.getLatestNews();
        return ResponseEntity.ok(entries);

    }

    @GetMapping("/scrap")
    public ResponseEntity<List<NewsData>>newsScrapper()throws IOException{
        List<NewsData>news=newsCrawlerService.GoogleNewsScrapper();

        return ResponseEntity.ok(news);

    }

}
