package com.rubixds.newscrawler.services;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;


import com.rubixds.newscrawler.models.NewsData;

@Service
public class NewsCrawlerService {

public List<NewsData> GoogleNewsScrapper(String companyName, String durationInput) {
        List<NewsData> newsList = new ArrayList<>();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", java.util.Collections.singletonList("enable-automation"));
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            // 1. CONSTRUCT DYNAMIC URL
            // Format duration to Google style (e.g., "90 days" -> "90d")
            String cleanDuration = durationInput.toLowerCase().replace(" ", "").replace("days", "d").replace("months", "m");
            if (!cleanDuration.endsWith("d") && !cleanDuration.endsWith("m") && !cleanDuration.endsWith("y")) {
                cleanDuration += "d"; // Default to days if format is unclear
            }

            // Build Query: "Tesla when:90d"
            String query = companyName + " when:" + cleanDuration;
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://news.google.com/search?q=" + encodedQuery + "&hl=en-US&gl=US&ceid=US:en";

            System.out.println("Scraping URL: " + searchUrl);
            driver.get(searchUrl);

            // ... (Rest of your Selenium logic remains the same) ...
            
            String originalWindow = driver.getWindowHandle();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(2000);

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollBy(0, 1000)");
            Thread.sleep(2000);

            List<WebElement> allLinks = driver.findElements(By.tagName("a"));
            System.out.println("Links found for " + companyName + ": " + allLinks.size());

            int count = 0;
            int maxArticles = 5; 

            for (WebElement link : allLinks) {
                if (count >= maxArticles) break;

                try {
                    String href = link.getAttribute("href");
                    String title = link.getText().trim();

                    if (title.isEmpty()) title = link.getAttribute("aria-label");
                    if (title == null || title.isEmpty()) {
                        title = (String) js.executeScript("return arguments[0].textContent;", link);
                    }

                    if (href == null) continue;

                    boolean isContent = (title != null && title.length() > 15);
                    if (isContent) {
                        String lower = title.toLowerCase();
                        if (lower.contains("sign in") || lower.contains("google")) isContent = false;
                    }

                    if (isContent) {
                        NewsData dto = new NewsData();
                        dto.setTitle(title.replaceAll("\\s+", " ").trim());
                        dto.setUrl(href);
                        dto.setSource("Google News");
                        dto.setPublishedTime(durationInput); // Storing the requested duration or "Recent"

                        boolean exists = newsList.stream().anyMatch(n -> n.getUrl().equals(dto.getUrl()));
                        if (!exists) {
                            
                            // === FULL CONTENT SCRAPING ===
                            System.out.println("Scraping content for: " + dto.getTitle());
                            driver.switchTo().newWindow(WindowType.TAB);
                            
                            try {
                                driver.get(href); 
                                WebDriverWait articleWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                                articleWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("p")));

                                List<WebElement> paragraphs = driver.findElements(By.tagName("p"));
                                String fullText = paragraphs.stream()
                                        .map(WebElement::getText)
                                        .filter(text -> text.length() > 20) 
                                        .collect(Collectors.joining("\n\n"));
                                
                                dto.setFullText(fullText);
                                
                            } catch (Exception e) {
                                System.err.println("Failed to scrape text: " + e.getMessage());
                                dto.setFullText("Content extraction failed.");
                            } finally {
                                driver.close();
                                driver.switchTo().window(originalWindow);
                            }
                            // ============================

                            newsList.add(dto);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    if (driver.getWindowHandles().size() > 1) driver.switchTo().window(originalWindow);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }

        return newsList;
    }
}