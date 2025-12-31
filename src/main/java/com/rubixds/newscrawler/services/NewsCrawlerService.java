package com.rubixds.newscrawler.services;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import com.rometools.fetcher.FeedFetcher;
import com.rometools.fetcher.FetcherException;
import com.rometools.fetcher.impl.HttpURLFeedFetcher;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rubixds.newscrawler.models.NewsData;

@Service
public class NewsCrawlerService {

    private static final String url = "https://news.google.com/search?q=Business+News&hl=en-US&gl=US&ceid=US:en";

    public List<SyndEntry> getLatestNews() throws IOException, FeedException, FetcherException {
        FeedFetcher feedFetcher = new HttpURLFeedFetcher();

        SyndFeed feed = feedFetcher.retrieveFeed(new URL(url));
        return feed.getEntries();

    }
public List<NewsData> GoogleNewsScrapper() {

    List<NewsData> newsList = new ArrayList<>();

    ChromeOptions options = new ChromeOptions();
    options.addArguments("C:\\Users\\samko\\AppData\\Local\\Google\\Chrome\\User Data\\Default");
options.addArguments("profile-directory=Default");
options.addArguments("--disable-blink-features=AutomationControlled");

    WebDriver driver = new ChromeDriver(options);

    try {
        driver.get(url);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        int attempts = 0;
        int linksCount = 0;

        while (attempts < 8 && linksCount == 0) {
            js.executeScript("window.scrollBy(0, 1200)");
            Thread.sleep(2000);

            linksCount = driver.findElements(
                By.xpath("//a[contains(@href,'/articles/')]")
            ).size();

            System.out.println("Links found: " + linksCount);
            attempts++;
        }

        List<WebElement> links =
            driver.findElements(By.xpath("//a[contains(@href,'/articles/')]"));

        for (WebElement link : links) {

            String title = link.getText().trim();
            if (title.isEmpty()) continue;

            String href = link.getAttribute("href");
            if (href == null) continue;

            NewsData dto = new NewsData();
            dto.setTitle(title);
            dto.setUrl(href);

            newsList.add(dto);
        }

    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        driver.quit();
    }

    return newsList;
}

}
