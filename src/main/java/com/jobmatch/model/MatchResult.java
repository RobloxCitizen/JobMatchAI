package com.jobmatch.model;

public class MatchResult {
    private String vacancyTitle;
    private String reason;

    public MatchResult() {}

    public MatchResult(String vacancyTitle, String reason) {
        this.vacancyTitle = vacancyTitle;
        this.reason = reason;
    }

    public String getVacancyTitle() {
        return vacancyTitle;
    }

    public void setVacancyTitle(String vacancyTitle) {
        this.vacancyTitle = vacancyTitle;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}