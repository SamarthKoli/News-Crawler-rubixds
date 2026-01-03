package com.rubixds.newscrawler.services;

import com.rubixds.newscrawler.models.NewsData;
import com.rubixds.newscrawler.repository.NewsRepository;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsCrawlerService {

    @Autowired
    private NewsRepository newsRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    // NEW ENDPOINT: Triggers the Python background worker
    private final String PYTHON_TRIGGER_URL = "http://localhost:5000/trigger-analysis"; 

    public List<NewsData> GoogleNewsScrapper(String companyName, String durationInput) {
        List<NewsData> newsList = new ArrayList<>();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", java.util.Collections.singletonList("enable-automation"));
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            // 1. URL Construction
            String cleanDuration = durationInput.toLowerCase().replace(" ", "").replace("days", "d").replace("months", "m");
            if (!cleanDuration.endsWith("d") && !cleanDuration.endsWith("m")) cleanDuration += "d";
            String query = companyName + " when:" + cleanDuration;
            String searchUrl = "https://news.google.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&hl=en-US&gl=US&ceid=US:en";

            driver.get(searchUrl);
            String originalWindow = driver.getWindowHandle();
            
            // 2. Wait and Scroll
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(2000);
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 1000)");
            Thread.sleep(2000);

            List<WebElement> allLinks = driver.findElements(By.tagName("a"));
            int count = 0;
            int maxArticles = 10; // Can increase this now, as Java is faster!

            for (WebElement link : allLinks) {
                if (count >= maxArticles) break;

                try {
                    String href = link.getAttribute("href");
                    String title = link.getText().trim();
                    if (title.isEmpty()) title = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].textContent;", link);

                    if (href == null || title.length() < 15) continue;
                    
                    String lower = title.toLowerCase();
                    if (lower.contains("sign in") || lower.contains("google")) continue;

                    // 3. CHECK DB
                    NewsData existing = newsRepository.findByUrl(href);
                    if (existing != null) {
                        newsList.add(existing);
                        count++;
                        continue; 
                    }

                    NewsData dto = new NewsData();
                    dto.setTitle(title.replaceAll("\\s+", " ").trim());
                    dto.setUrl(href);
                    dto.setSource("Google News");
                    dto.setPublishedTime(durationInput);

                    // 4. SCRAPE FULL CONTENT (Best Effort)
                    // Even if this fails or returns empty, we STILL save it.
                    // Python will fix the missing text later.
                    System.out.println("Scraping metadata: " + dto.getTitle());
                    
                    driver.switchTo().newWindow(WindowType.TAB);
                    try {
                        driver.get(href);
                        new WebDriverWait(driver, Duration.ofSeconds(5)) // Short timeout for speed
                            .until(ExpectedConditions.presenceOfElementLocated(By.tagName("p")));
                        
                        List<WebElement> paragraphs = driver.findElements(By.tagName("p"));
                        String fullText = paragraphs.stream()
                                .map(WebElement::getText)
                                .filter(text -> text.length() > 20)
                                .collect(Collectors.joining("\n\n"));
                        
                        dto.setFullText(fullText);
                    } catch (Exception e) {
                        dto.setFullText(""); // It's okay! Python will handle empty text.
                    } finally {
                        driver.close();
                        driver.switchTo().window(originalWindow);
                    }

                    // 5. SAVE TO MONGODB
                    // We save immediately. 'Summary' will be null. 'Sentiment' will be 0.0.
                    dto = newsRepository.save(dto);

                    newsList.add(dto);
                    count++;

                } catch (Exception e) {
                    System.err.println("Skipping link: " + e.getMessage());
                    if (driver.getWindowHandles().size() > 1) driver.switchTo().window(originalWindow);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }

        // 6. TRIGGER PYTHON (Async)
        // This runs in the background. Java returns the list immediately.
        triggerPythonAnalysis();

        return newsList;
    }

    private void triggerPythonAnalysis() {
        try {
            System.out.println(">> Triggering Python Background Service...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PYTHON_TRIGGER_URL))
                    .POST(HttpRequest.BodyPublishers.noBody()) 
                    .build();

            // Use sendAsync so the user doesn't have to wait for analysis to finish
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> System.out.println(">> Python Response: " + response.body()));
                
        } catch (Exception e) {
            System.err.println("Failed to trigger python: " + e.getMessage());
        }
    }
}