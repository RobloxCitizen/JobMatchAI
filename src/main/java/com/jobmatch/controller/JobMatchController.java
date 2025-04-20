package com.jobmatch.controller;

import com.jobmatch.model.MatchResult;
import com.jobmatch.model.Vacancy;
import com.jobmatch.service.OpenAIService;
import com.jobmatch.service.ResumeParserService;
import com.jobmatch.service.VacancyFetcherService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/jobmatch")
public class JobMatchController {

    private static final Logger log = LoggerFactory.getLogger(JobMatchController.class);

    private final ResumeParserService resumeParserService;
    private final VacancyFetcherService vacancyFetcherService;
    private final OpenAIService openAIService;

    public JobMatchController(
            ResumeParserService resumeParserService,
            VacancyFetcherService vacancyFetcherService,
            OpenAIService openAIService
    ) {
        this.resumeParserService = resumeParserService;
        this.vacancyFetcherService = vacancyFetcherService;
        this.openAIService = openAIService;
    }

    @PostMapping("/match")
    public ResponseEntity<?> matchResume(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam("source") String source) {
        try {
            // Валидация файла
            if (resumeFile.isEmpty()) {
                log.error("Файл резюме пуст");
                return ResponseEntity.badRequest().body("Файл резюме не может быть пустым");
            }
            String fileName = resumeFile.getOriginalFilename();
            if (fileName == null || !(fileName.endsWith(".pdf") || fileName.endsWith(".txt"))) {
                log.error("Неподдерживаемый тип файла: {}", fileName);
                return ResponseEntity.badRequest().body("Поддерживаются только файлы .pdf и .txt");
            }

            // Валидация источника
            if (!List.of("hh.ru", "rabota.by", "Локальные данные").contains(source)) {
                log.error("Недопустимый источник: {}", source);
                return ResponseEntity.badRequest().body("Недопустимый источник: " + source);
            }

            // Парсинг резюме
            String resumeText = resumeParserService.extractText(resumeFile);
            if (resumeText == null || resumeText.trim().isEmpty()) {
                log.error("Не удалось извлечь текст из резюме");
                return ResponseEntity.badRequest().body("Не удалось извлечь текст из резюме");
            }

            // Получение вакансий
            List<Vacancy> vacancies = vacancyFetcherService.fetchVacancies(source);
            if (vacancies.isEmpty()) {
                log.warn("Вакансии не найдены для источника: {}", source);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Вакансии не найдены");
            }

            // Получение рекомендаций от OpenAI
            List<MatchResult> matches = openAIService.getBestMatches(resumeText, vacancies);
            if (matches.isEmpty()) {
                log.warn("Подходящие вакансии не найдены");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Подходящие вакансии не найдены");
            }

            return ResponseEntity.ok(matches);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Внутренняя ошибка при обработке резюме: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Внутренняя ошибка: " + e.getMessage());
        }
    }
}