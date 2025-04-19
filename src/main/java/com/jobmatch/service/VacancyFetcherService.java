package com.jobmatch.service;

import com.jobmatch.model.Vacancy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class VacancyFetcherService {

    public List<Vacancy> fetchVacancies(String source) {
        switch (source) {
            case "hh.ru":
                return fetchFromHH();
            case "rabota.by":
                return fetchFromRabotaBy();
            default:
                return fetchLocalVacancies();
        }
    }

    private List<Vacancy> fetchFromHH() {
        try {
            Document doc = Jsoup.connect("https://hh.ru/search/vacancy?text=java&area=1").get();
            Elements vacancies = doc.select(".vacancy-serp-item");
            List<Vacancy> result = new ArrayList<>();
            for (Element vacancy : vacancies) {
                String title = vacancy.select(".serp-item__title").text();
                String description = vacancy.select(".vacancy-serp-content").text();
                result.add(new Vacancy(title, description));
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private List<Vacancy> fetchFromRabotaBy() {
        try {
            Document doc = Jsoup.connect("https://rabota.by/search/vacancy?text=java").get();
            Elements vacancies = doc.select(".vacancy-serp-item");
            List<Vacancy> result = new ArrayList<>();
            for (Element vacancy : vacancies) {
                String title = vacancy.select(".vacancy-serp-item__title").text();
                String description = vacancy.select(".vacancy-serp-item__info").text();
                result.add(new Vacancy(title, description));
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private List<Vacancy> fetchLocalVacancies() {
        return List.of(
                new Vacancy("Java Developer", "Work with Spring Boot and REST APIs"),
                new Vacancy("Backend Engineer", "Focus on Java and PostgreSQL"),
                new Vacancy("AI Engineer", "Use OpenAI APIs with Java")
        );
    }
}