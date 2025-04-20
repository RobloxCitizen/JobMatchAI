package com.jobmatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.model.Vacancy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class VacancyFetcherService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${hh.api.key:}")
    private String hhApiKey;
    @Value("${hh.use.api:false}")
    private boolean useHhApi;
    @Value("${rabota.by.api.key:}")
    private String rabotaByApiKey;
    @Value("${rabota.by.use.api:false}")
    private boolean useRabotaByApi;

    /**
     * Получает список вакансий из указанного источника.
     * @param source Источник вакансий ("hh.ru", "rabota.by" или "Локальные данные")
     * @return Список объектов Vacancy
     */
    public List<Vacancy> fetchVacancies(String source) {
        switch (source) {
            case "hh.ru":
                return useHhApi && !hhApiKey.isEmpty() ? fetchFromHHApi() : fetchFromHHScraping();
            case "rabota.by":
                return useRabotaByApi && !rabotaByApiKey.isEmpty() ? fetchFromRabotaByApi() : fetchFromRabotaByScraping();
            default:
                return fetchLocalVacancies();
        }
    }

    /**
     * Получает вакансии с hh.ru через API (готово для использования с ключом).
     * @return Список вакансий
     */
    private List<Vacancy> fetchFromHHApi() {
        try {
            String url = "https://api.hh.ru/vacancies?text=java&area=1&per_page=10";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + hhApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // Парсим JSON-ответ от API
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("items");
            List<Vacancy> vacancies = new ArrayList<>();
            for (JsonNode item : items) {
                String title = item.path("name").asText();
                String description = item.path("snippet").path("responsibility").asText();
                vacancies.add(new Vacancy(title, description));
            }
            return vacancies;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Получает вакансии с hh.ru через веб-скрейпинг (резервный метод).
     * @return Список вакансий
     */
    private List<Vacancy> fetchFromHHScraping() {
        try {
            Document doc = Jsoup.connect("https://hh.ru/search/vacancy?text=java&area=1")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();
            // Извлекаем элементы вакансий по CSS-селектору
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

    /**
     * Заглушка для получения вакансий с rabota.by через API (для будущего использования).
     * @return Список вакансий
     */
    private List<Vacancy> fetchFromRabotaByApi() {
        System.out.println("API rabota.by не реализовано. Используется скрейпинг.");
        return fetchFromRabotaByScraping();
    }

    /**
     * Получает вакансии с rabota.by через веб-скрейпинг.
     * @return Список вакансий
     */
    private List<Vacancy> fetchFromRabotaByScraping() {
        try {
            Document doc = Jsoup.connect("https://rabota.by/search/vacancy?text=java")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();
            // Извлекаем элементы вакансий по CSS-селектору
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

    //Возвращает локальные тестовые вакансии для тестирования
    private List<Vacancy> fetchLocalVacancies() {
        return List.of(
                new Vacancy("Java Developer", "Работа с Spring Boot и REST API"),
                new Vacancy("Backend Engineer", "Фокус на Java и PostgreSQL"),
                new Vacancy("AI Engineer", "Использование API OpenAI с Java")
        );
    }
}