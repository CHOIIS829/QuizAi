package com.ondongne.backend.domain.quiz.service;

import com.ondongne.backend.domain.gemini.service.GeminiService;
import com.ondongne.backend.domain.quiz.dto.QuizResponseDto;
import com.ondongne.backend.global.exception.FailCrawlException;
import com.ondongne.backend.global.exception.FailDownloadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    @Value("${app.file.temp-dir}")
    private String tempDir;

    @Value("${app.yt-dlp.path}")
    private String ytDlpPath;

    private final GeminiService geminiService;

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.|m\\.)?(youtube\\.com|youtu\\.be)/(watch\\?v=|shorts/|embed/|v/)?([a-zA-Z0-9_-]{11}).*$"
    );

    public QuizResponseDto processQuiz(String url, int quizCount) {
        // 전체 프로세스 시작 시간 측정
        long startTime = System.currentTimeMillis();

        QuizResponseDto response;

        if(!isYoutubeUrl(url)) {
            log.info(">>>>> 감지된 콘텐츠 타입 : BLOG / WEB POST");
            String text = crawlBlog(url);
            response = geminiService.generateQuizFromText(text, quizCount);
        } else {
            log.info(">>>>> 감지된 콘텐츠 타입 : YOUTUBE VIDEO");
            String filePath = downloadVideo(url);
            response = geminiService.generateQuizFromVideo(filePath, quizCount);
        }

        // 전체 프로세스 종료 및 시간 계산
        long endTime = System.currentTimeMillis();
        log.info(">>>>> Quiz 생성 전체 프로세스 완료! 소요 시간: {}ms", (endTime - startTime));

        return response;
    }


    private String downloadVideo(String url) {
        createTempDirectory();

        String fileName = UUID.randomUUID().toString() + ".mp4";
        String filePath = tempDir + File.separator + fileName;

        log.info(">>>>> 다운로드 시작... 저장 경로: {}", filePath);

        // 다운로드 시작 시간 측정
        long startTime = System.currentTimeMillis();

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    ytDlpPath,
                    "-f", "worst[ext=mp4]",
                    "-o", filePath,
                    url
            );

            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[yt-dlp] {}", line);
                }
            }

            int exitCode = process.waitFor();

            // 다운로드 종료 시간 측정 및 계산
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (exitCode != 0) {
                log.error(">>>>> 다운로드 실패 (소요 시간: {}ms)", duration);
                throw new FailDownloadException();
            }

            log.info(">>>>> 영상 다운로드 완료! 경로: {}, 소요 시간: {}ms", filePath, duration);

            return new File(filePath).getAbsolutePath();

        } catch (Exception e) {
            log.error(">>>>> 다운로드 중 오류 발생", e);
            throw new FailDownloadException();
        }
    }

    // createTempDirectory는 변경 사항 없음
    private void createTempDirectory() {
        File directory = new File(tempDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                log.error(">>>>> 임시 디렉토리 생성 실패: {}", tempDir);
                throw new RuntimeException("임시 디렉토리를 생성할 수 없습니다.");
            }
        }
    }

    private String crawlBlog(String url) {
        // 크롤링 시작 시간 측정
        long startTime = System.currentTimeMillis();

        try {
            log.info(">>>>> 크롤링 시작: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            doc.select("script, style, header, footer, nav, aside, iframe, .sidebar, .comment, .advertisement, .ads").remove();

            Element content = null;
            String[] selectors = {
                    "article", "main", ".post-content", ".entry-content",
                    ".markdown-body", "div[role='main']", "#content", ".content"
            };

            for (String selector : selectors) {
                content = doc.selectFirst(selector);
                if (content != null && content.text().length() > 50) {
                    log.info(">>>>> 본문 영역 감지됨: {}", selector);
                    break;
                }
            }

            if (content == null) {
                log.warn(">>>>> 명시적인 본문 영역을 찾지 못해 전체 body를 사용합니다.");
                content = doc.body();
            }

            String text = content.text().trim();

            if (text.isEmpty()) {
                throw new FailCrawlException();
            }

            // 크롤링 종료 시간 측정 및 계산
            long endTime = System.currentTimeMillis();
            log.info(">>>>> 크롤링 완료. 텍스트 길이: {}, 소요 시간: {}ms", text.length(), (endTime - startTime));

            return text;

        } catch (Exception e) {
            log.error(">>>>> 크롤링 중 오류 발생", e);
            throw new FailCrawlException();
        }
    }

    private boolean isYoutubeUrl(String url) {
        return url != null && YOUTUBE_PATTERN.matcher(url).matches();
    }
}