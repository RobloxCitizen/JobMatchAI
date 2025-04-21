package com.jobmatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.model.MatchResult;
import com.jobmatch.model.Vacancy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
    }

    public List<MatchResult> getBestMatches(String resumeText, List<Vacancy> vacancies) {
        // Заглушка для тестирования
        if (apiKey == null || apiKey.isEmpty()) {
            return getMockMatches(resumeText, vacancies);
        }

        String prompt = buildPrompt(resumeText, vacancies);
        String response = callOpenAI(prompt);
        return parseResponse(response);
    }

    private String buildPrompt(String resumeText, List<Vacancy> vacancies) {
        StringBuilder sb = new StringBuilder();
        sb.append("Резюме пользователя:\n").append(resumeText).append("\n\n");
        sb.append("Вот список вакансий:\n");
        for (int i = 0; i < vacancies.size(); i++) {
            Vacancy v = vacancies.get(i);
            sb.append(i + 1).append(". ").append(v.getTitle()).append(": ")
                    .append(v.getDescription()).append("\n");
        }
        sb.append("\nВыбери 10 наиболее подходящих вакансий и объясни почему. Верни ответ в JSON-формате, содержащем массив объектов с полями 'vacancyTitle' и 'reason':\n");
        sb.append("[\n");
        sb.append("  {\"vacancyTitle\": \"Название вакансии\", \"reason\": \"Причина соответствия\"},\n");
        sb.append("  ...\n");
        sb.append("]");
        return sb.toString();
    }

    private String callOpenAI(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String requestBody = "{\"model\": \"gpt-4\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return response.getBody();
    }

    private List<MatchResult> parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();

            System.out.println("OpenAI response:\n" + content);

            JsonNode jsonContent = objectMapper.readTree(content);
            if (!jsonContent.isArray()) {
                throw new RuntimeException("Ответ OpenAI не является JSON-массивом");
            }

            return objectMapper.readValue(
                    content,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MatchResult.class)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private List<MatchResult> getMockMatches(String resumeText, List<Vacancy> vacancies) {
        // Заглушка: возвращаем фиктивные результаты
        List<MatchResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(3, vacancies.size()); i++) {
            MatchResult result = new MatchResult();
            result.setVacancyTitle(vacancies.get(i).getTitle());
            result.setReason("Эта вакансия подходит, так как резюме содержит релевантные навыки.");
            results.add(result);
        }
        return results;
    }
}