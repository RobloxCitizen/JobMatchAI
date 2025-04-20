package com.jobmatch.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.jobmatch.model.MatchResult;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MainUI extends Application {

    private TextArea resultArea;
    private ComboBox<String> sourceComboBox;
    private RestTemplate restTemplate = new RestTemplate();
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.jobmatch/config.properties";
    private static final int FREE_REQUESTS_LIMIT = 5;
    private int requestCount = 0;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JobMatch AI");

        // Проверка ключа OpenAI
        String apiKey = loadApiKey();
        if (apiKey == null) {
            showApiKeyDialog(primaryStage);
            return;
        }

        // Проверка количества запросов
        requestCount = loadRequestCount();
        if (requestCount >= FREE_REQUESTS_LIMIT) {
            showPaymentDialog(primaryStage);
            return;
        }

        // Основной интерфейс
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");

        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: #3c3f41;");

        Button uploadButton = new Button("Загрузить резюме");
        uploadButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        uploadButton.setOnAction(e -> handleFileUpload());

        sourceComboBox = new ComboBox<>();
        sourceComboBox.getItems().addAll("hh.ru", "rabota.by", "Локальные данные");
        sourceComboBox.setValue("Локальные данные");
        sourceComboBox.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");

        Label titleLabel = new Label("JobMatch AI");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        topPanel.getChildren().addAll(titleLabel, uploadButton, sourceComboBox);

        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Arial", 18));
        resultArea.setStyle("-fx-control-inner-background: #3c3f41; -fx-text-fill: white;");
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(Double.MAX_VALUE);

        VBox adPanel = new VBox(10);
        adPanel.setPadding(new Insets(10));
        adPanel.setStyle("-fx-background-color: #3c3f41;");
        Label adLabel = new Label("Реклама: Подпишитесь на наш канал для карьерных советов!");
        adLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        adPanel.getChildren().add(adLabel);

        root.setTop(topPanel);
        root.setCenter(resultArea);
        root.setBottom(adPanel);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String loadApiKey() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            return props.getProperty("openai.api.key");
        } catch (IOException e) {
            return null;
        }
    }
    private int loadRequestCount() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            String countStr = props.getProperty("request.count", "0");
            return Integer.parseInt(countStr);
        } catch (IOException e) {
            return 0;
        }
    }

    private void saveApiKey(String apiKey) {
        Properties props = new Properties();
        props.setProperty("openai.api.key", apiKey);
        props.setProperty("request.count", String.valueOf(requestCount));
        File configFile = new File(CONFIG_FILE);
        configFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "JobMatch AI Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveRequestCount() {
        Properties props = new Properties();
        props.setProperty("openai.api.key", loadApiKey());
        props.setProperty("request.count", String.valueOf(requestCount));
        File configFile = new File(CONFIG_FILE);
        configFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "JobMatch AI Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showApiKeyDialog(Stage primaryStage) {
        Stage dialog = new Stage();
        dialog.setTitle("Введите ключ OpenAI");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        TextField apiKeyField = new TextField();
        apiKeyField.setPromptText("Введите ваш OpenAI API ключ");

        Button submitButton = new Button("Сохранить");
        submitButton.setOnAction(e -> {
            String apiKey = apiKeyField.getText();
            if (!apiKey.isEmpty()) {
                saveApiKey(apiKey);
                dialog.close();
                start(primaryStage);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ключ не может быть пустым!");
                alert.showAndWait();
            }
        });

        vbox.getChildren().addAll(new Label("OpenAI API ключ:"), apiKeyField, submitButton);
        Scene scene = new Scene(vbox, 300, 150);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showPaymentDialog(Stage primaryStage) {
        Stage dialog = new Stage();
        dialog.setTitle("Требуется подписка");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        Label message = new Label("Вы исчерпали лимит бесплатных запросов (" + FREE_REQUESTS_LIMIT + ").\n" +
                "Подпишитесь для продолжения использования.");
        Button subscribeButton = new Button("Подписаться");
        subscribeButton.setOnAction(e -> {
            // Заглушка для оплаты (например, интеграция с Stripe)
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Перейдите на наш сайт для оплаты подписки!");
            alert.showAndWait();
        });

        Button resetButton = new Button("Сбросить лимит (для теста)");
                resetButton.setOnAction(e -> {
                    requestCount = 0;
                    saveRequestCount();
                    dialog.close();
                    start(primaryStage);
                });

        vbox.getChildren().addAll(message, subscribeButton, resetButton);
        Scene scene = new Scene(vbox, 300, 200);
        dialog.setScene(scene);
        dialog.show();
    }

    private void handleFileUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите резюме");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"),
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            if (file.length() > 2 * 1024 * 1024) {
                resultArea.setText("Ошибка: Файл слишком большой (максимум 2 МБ)");
                return;
            }
            requestCount++;
            saveRequestCount();
            sendResumeToServer(file, sourceComboBox.getValue());
        }
    }

    private void sendResumeToServer(File resume, String source) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("resume", new FileSystemResource(resume));
            body.add("source", source);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<List<MatchResult>> response = restTemplate.exchange(
                    "http://localhost:8080/api/jobmatch/match", HttpMethod.POST, entity, new ParameterizedTypeReference<List<MatchResult>>() {}
            );
            List<MatchResult> matches = response.getBody();
            StringBuilder resultText = new StringBuilder();
            for (MatchResult match : matches) {
                resultText.append("Вакансия: ").append(match.getVacancyTitle())
                        .append("\nПричина: ").append(match.getReason()).append("\n\n");
            }
            resultArea.setText(resultText.toString());
        } catch (HttpServerErrorException e) {
            e.printStackTrace();
            resultArea.setText("Ошибка сервера: " + e.getMessage() + "\nResponse: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            resultArea.setText("Ошибка при обработке резюме: " + e.getMessage());
        }
    }

    private void displayResults(List<?> results) {
        StringBuilder sb = new StringBuilder();
        for (Object result : results) {
            Map<String, String> entry = (Map<String, String>) result;
            sb.append("Вакансия: ").append(entry.get("vacancyTitle")).append("\n");
            sb.append("Причина: ").append(entry.get("reason")).append("\n\n");
        }
        resultArea.setText(sb.toString());
    }
}