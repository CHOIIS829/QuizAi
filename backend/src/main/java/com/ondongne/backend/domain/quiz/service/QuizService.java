package com.ondongne.backend.domain.quiz.service;

import com.ondongne.backend.domain.gemini.service.GeminiService;
import com.ondongne.backend.domain.quiz.dto.QuizResponseDto;
import com.ondongne.backend.domain.quiz.repository.JobRedisRepository;
import com.ondongne.backend.global.exception.FailDownloadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    private final JobRedisRepository jobRedisRepository;

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.|m\\.)?(youtube\\.com|youtu\\.be)/(watch\\?v=|shorts/|embed/|v/)?([a-zA-Z0-9_-]{11}).*$"
    );

    public QuizResponseDto processQuiz(String url, int quizCount) {

        String jobId = UUID.randomUUID().toString();

        QuizResponseDto jobStatus = QuizResponseDto.builder()
                .jobId(jobId)
                .status(QuizResponseDto.JobStatus.PROCESSING)
                .message("퀴즈 생성이 진행 중입니다.")
                .build();

        jobRedisRepository.save(jobId, jobStatus);

        startAsyncJob(jobId, url, quizCount);

        return jobStatus;
    }

    private void startAsyncJob(String jobId, String url, int quizCount) {
        if(!isYoutubeUrl(url)) {
            log.info(">>>>> 감지된 콘텐츠 타입 : BLOG / WEB POST");

            crawlBlogAsync(url)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            text -> {
                                log.info(">>>>> [Job: {}] 크롤링 성공 (길이 : {}). 퀴즈 생성 시작...", jobId, text.length());
                                geminiService.generateQuizFromText(jobId, text, quizCount);
                            },
                            error -> {
                                log.error(">>>>> [Job: {}] 크롤링 실패: {}", jobId, error.getMessage());
                                jobRedisRepository.update(jobId, QuizResponseDto.JobStatus.FAILED, "크롤링에 실패했습니다.", null);
                            }
                    );

        } else {
            log.info(">>>>> 감지된 콘텐츠 타입 : YOUTUBE VIDEO");

            downloadVideoAsync(url)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            filePath -> {
                                log.info(">>>>> [Job: {}] 다운로드 성공 (경로: {}). 퀴즈 생성 시작...", jobId, filePath);
                                geminiService.generateQuizFromVideo(jobId, filePath, quizCount);
                            },
                            error -> {
                                log.error(">>>>> [Job: {}] 다운로드 실패: {}", jobId, error.getMessage());
                                jobRedisRepository.update(jobId, QuizResponseDto.JobStatus.FAILED, "동영상 다운로드에 실패했습니다.", null);
                            }
                    );
        }
    }

    private Mono<String> crawlBlogAsync(String url) {
        return Mono.fromCallable(() -> {
            try {
                log.info(">>>>> 크롤링 시작: {}", url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(10000)
                        .get();

                if (url.contains("blog.naver.com")) {
                    Element iframe = doc.select("iframe#mainFrame").first();
                    if (iframe != null) {
                        String realUrl = "https://blog.naver.com" + iframe.attr("src");
                        log.info(">>>>> 네이버 iframe 감지. 진짜 주소로 재접속: {}", realUrl);

                        doc = Jsoup.connect(realUrl)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) ...")
                                .timeout(10000)
                                .get();
                    }
                }

                doc.select("script, style, header, footer, nav, aside, iframe, .sidebar, .comment, .advertisement, .ads, .cookie-banner").remove();

                Element content = null;
                String[] selectors = {
                        ".se-main-container", // 네이버 스마트에디터 본문
                        "article", "main",
                        ".tt_article_useless_p_margin", // 티스토리 본문
                        ".post-content", ".entry-content",
                        ".markdown-body", "div[role='main']", "#content", ".content",
                        ".atom-one" // Velog 본문
                };

                for (String selector : selectors) {
                    content = doc.selectFirst(selector);
                    if (content != null && content.text().length() > 100) {
                        log.info(">>>>> 본문 영역 감지됨: {}", selector);
                        break;
                    }
                }

                if (content == null) {
                    log.warn(">>>>> 명시적인 본문 영역을 찾지 못했습니다.");
                    throw new FailCrawlException();
                }

                String text = content.text().trim();

                if (text.isEmpty()) {
                    throw new FailCrawlException();
                }

                return text;

            } catch (Exception e) {
                log.error(">>>>> 비동기 크롤링 중 오류 발생: {}", e.getMessage());
                throw new FailCrawlException(e);
            }
        });
    }


    private Mono<String> downloadVideoAsync(String url) {
        return Mono.fromCallable(() -> {
            createTempDirectory();

            String fileName = UUID.randomUUID().toString() + ".mp4"; // ex) uuid.mp4
            String filePath = tempDir + File.separator + fileName; // ex) /temp/video/uuid.mp4

            log.info(">>>>> 다운로드 시작... 저장 경로: {}", filePath);

            try {
                ProcessBuilder builder = new ProcessBuilder(
                        ytDlpPath,
                        "-f", "worst[ext=mp4]",
                        "--force-ipv4",
                        "--extractor-args", "youtube:player_client=android",
                        "-o", filePath,
                        url
                );

                builder.redirectErrorStream(true);
                Process process = builder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[yt-dlp] {}", line);
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    throw new FailDownloadException();
                }

                return new File(filePath).getAbsolutePath();

            } catch (Exception e) {
                log.error(">>>>> 다운로드 중 오류 발생", e);
                throw new FailDownloadException(e);
            }
        });
    }

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

    public QuizResponseDto getQuizStatus(String jobId) {
        return jobRedisRepository.findById(jobId);
    }

    private boolean isYoutubeUrl(String url) {
        return url != null && YOUTUBE_PATTERN.matcher(url).matches();
    }
}