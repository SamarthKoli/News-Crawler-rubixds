package com.rubixds.newscrawler.services;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;


import com.rubixds.newscrawler.models.NewsData;

@Service
public class NewsCrawlerService {

   

public List<NewsData> GoogleNewsScrapper() {
    List<NewsData> newsList = new ArrayList<>();

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--start-maximized");
    options.addArguments("--disable-blink-features=AutomationControlled");
    options.setExperimentalOption("excludeSwitches", java.util.Collections.singletonList("enable-automation"));
    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

    WebDriver driver = new ChromeDriver(options);
    try {
        driver.get("https://news.google.com/search?q=Business+News&hl=en-US&gl=US&ceid=US:en");
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        // Give it time to settle
        Thread.sleep(3000); 

        // Scroll to load more items
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0, 1000)");
        Thread.sleep(2000);

        List<WebElement> allLinks = driver.findElements(By.tagName("a"));


        for (WebElement link : allLinks) {
            try {
                String href = link.getAttribute("href");
                String title = link.getText().trim();
                
                //  If visible text is empty, check aria-label (Common in Google News)
                if (title.isEmpty()) {
                    title = link.getAttribute("aria-label");
                }
                // Javascript text content
                if (title == null || title.isEmpty()) {
                    title = (String) js.executeScript("return arguments[0].textContent;", link);
                }

                if (href == null) continue;


             
                // Instead of looking for specific URLs like "/articles/", we look for CONTENT.
                // If a link has a title longer than 15 chars, it's likely a headline.
                boolean isContent = (title != null && title.length() > 15);
                
                // Exclude common non-news links
                if (isContent) {
                    String lowerTitle = title.toLowerCase();
                    if (lowerTitle.contains("sign in") || lowerTitle.contains("google") || 
                        lowerTitle.contains("feedback") || lowerTitle.contains("privacy")) {
                        isContent = false;
                    }
                }

                if (isContent) {
                    NewsData dto = new NewsData();
                    dto.setTitle(title.replaceAll("\\s+", " ").trim());
                    dto.setUrl(href);
                    dto.setSource("Google News");
                    dto.setPublishedTime("Recent");

                    
                    boolean exists = newsList.stream().anyMatch(n -> n.getUrl().equals(dto.getUrl()));
                    if (!exists) {
                        newsList.add(dto);
                    }
                }

            } catch (Exception e) {
                
            }
        }
        
       

    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (driver != null) {
            driver.quit();
        }
    }

    return newsList;
}     
}