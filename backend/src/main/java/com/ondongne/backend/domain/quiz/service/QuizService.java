package com.ondongne.backend.domain.quiz.service;

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

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.|m\\.)?(youtube\\.com|youtu\\.be)/(watch\\?v=|shorts/|embed/|v/)?([a-zA-Z0-9_-]{11}).*$"
    );

    public String processQuiz(String url, int quizCount) {
        if(!isYoutubeUrl(url)) {
            log.info(">>>>> 감지된 콘텐즈 타입 : BLOG / WEB POST");
            return crawlBlog(url);
        } else {
            log.info(">>>>> 감지된 콘텐즈 타입 : YOUTUBE VIDEO");
            return downloadVideo(url);
        }
    }


    private String downloadVideo(String url) {

        createTempDirectory();

        String fileName = UUID.randomUUID().toString() + ".mp4";
        String filePath = tempDir + File.separator + fileName;

        log.info(">>>>> 다운로드 시작... 저장 경로: {}", filePath);

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    ytDlpPath,
                    "-f", "best[height<=360]", // 용량 절약을 위해 360p 이하 최적 화질 선택
                    "-o", filePath,            // 저장될 파일 경로 지정
                    url                        // 다운로드할 URL
            );

            // 프로세스의 에러 출력을 표준 출력과 합쳐서 로그로 보기 위함
            builder.redirectErrorStream(true);

            // 프로세스 시작
            Process process = builder.start();

            // yt-dlp의 진행 상황 로그 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[yt-dlp] {}", line);
                }
            }

            // 다운로드가 끝날 때까지 대기 (Blocking)
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new FailDownloadException();
            }

            log.info(">>>>> 영상 다운로드 완료! 경로: {}", filePath);

            // 6. 다운로드된 파일의 절대 경로 반환
            return new File(filePath).getAbsolutePath();

        } catch (Exception e) {
            log.error(">>>>> 다운로드 중 오류 발생", e);
            throw new FailDownloadException();
        }
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

    private String crawlBlog(String url) {
        try {
            log.info(">>>>> 크롤링 시작: {}", url);

            // 1. Jsoup으로 HTML 문서 가져오기 (User-Agent 설정으로 차단 방지)
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000) // 10초 타임아웃
                    .get();

            // 2. 불필요한 요소(노이즈) 제거
            // 스크립트, 스타일, 헤더, 푸터, 네비게이션, 사이드바 등 제거
            doc.select("script, style, header, footer, nav, aside, iframe, .sidebar, .comment, .advertisement, .ads").remove();

            // 3. 본문 영역 찾기 (우선순위 기반 탐색)
            Element content = null;

            // 일반적인 블로그 및 기술 문서 사이트들의 본문 선택자 후보군
            String[] selectors = {
                    "article",                  // 시멘틱 태그 (가장 정확)
                    "main",                     // 메인 영역
                    ".post-content",            // 워드프레스/티스토리 등
                    ".entry-content",
                    ".markdown-body",           // 깃허브/벨로그(Velog) 등
                    "div[role='main']",
                    "#content",
                    ".content"
            };

            for (String selector : selectors) {
                content = doc.selectFirst(selector);
                // 내용을 찾았고, 너무 짧지 않다면 본문으로 간주
                if (content != null && content.text().length() > 50) {
                    log.info(">>>>> 본문 영역 감지됨: {}", selector);
                    break;
                }
            }

            // 못 찾았으면 body 전체 사용 (최후의 수단)
            if (content == null) {
                log.warn(">>>>> 명시적인 본문 영역을 찾지 못해 전체 body를 사용합니다.");
                content = doc.body();
            }

            // 4. 텍스트 추출 및 공백 정리
            String text = content.text().trim();

            if (text.isEmpty()) {
                throw new FailCrawlException();
            }

            log.info(">>>>> 크롤링 완료. 텍스트 길이: {}", text.length());
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
