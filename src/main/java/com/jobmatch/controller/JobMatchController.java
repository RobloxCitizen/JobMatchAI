package com.jobmatch.controller;

import com.jobmatch.model.MatchResult;
import com.jobmatch.model.Vacancy;
import com.jobmatch.service.OpenAIService;
import com.jobmatch.service.ResumeParserService;
import com.jobmatch.service.VacancyFetcherService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/jobmatch")
public class JobMatchController {

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
    public ResponseEntity<List<MatchResult>> matchResume(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam("source") String source) {
        try {
            String resumeText = resumeParserService.extractText(resumeFile);
            List<Vacancy> vacancies = vacancyFetcherService.fetchVacancies(source);
            List<MatchResult> matches = openAIService.getBestMatches(resumeText, vacancies);
            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}