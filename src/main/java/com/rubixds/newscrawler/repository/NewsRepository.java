package com.rubixds.newscrawler.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.rubixds.newscrawler.models.NewsData;

@Repository
public interface NewsRepository extends MongoRepository<NewsData,String> {

    NewsData findByUrl(String url);
}
