"use client";

import { useState } from "react";
import Header from "../components/Header";
import InputSection from "../components/InputSection";
import QuizSection from "../components/QuizSection";
import ResultSection from "../components/ResultSection";

export default function Home() {
  const [step, setStep] = useState("INPUT"); // INPUT, QUIZ, RESULT
  const [quizData, setQuizData] = useState(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [userAnswers, setUserAnswers] = useState({});

  const [url, setUrl] = useState("");
  const [quizCount, setQuizCount] = useState(5);
  const [isLoading, setIsLoading] = useState(false);

  const handleGenerate = async () => {
    if (!url) {
      alert("URL을 입력해주세요.");
      return;
    }
    setIsLoading(true);

    try {
      const apiBaseUrl = process.env.NODE_ENV === 'development'
        ? "http://localhost:8080"
        : "";

      // 1. 퀴즈 생성 요청 (Job 시작)
      const startResponse = await fetch(`${apiBaseUrl}/api/quiz/generate`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          url: url,
          quizCount: quizCount,
        }),
      });

      if (!startResponse.ok) {
        throw await createErrorFromResponse(startResponse);
      }

      const startResult = await startResponse.json();
      const jobId = startResult.data.jobId;

      // 2. 폴링 시작 (Job 상태 확인)
      await pollJobStatus(apiBaseUrl, jobId);

    } catch (error) {
      console.error("Error:", error);
      alert(error.message || "문제 생성 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const createErrorFromResponse = async (response) => {
    const errorData = await response.json().catch(() => null);
    if (errorData && errorData.errorCode) {
      switch (errorData.errorCode) {
        case "FAIL_CRAWL":
          return new Error("퀴즈 생성이 불가능한 URL입니다.");
        case "FAIL_DOWNLOAD":
          return new Error("영상의 길이가 너무 깁니다. \n다른 영상으로 시도해주세요.");
        case "GEMINI_FAIL_ERROR":
          return new Error("현재 요청이 많아 잠시 후 다시 시도해주세요.");
        default:
          return new Error(errorData.message || "오류가 발생했습니다.");
      }
    }
    return new Error("서버와의 통신에 실패했습니다.");
  };

  const pollJobStatus = async (apiBaseUrl, jobId) => {
    const maxRetries = 60; // 최대 2분 대기 (2초 * 60)
    let retryCount = 0;

    while (retryCount < maxRetries) {
      await new Promise(resolve => setTimeout(resolve, 2000)); // 2초 대기

      try {
        const response = await fetch(`${apiBaseUrl}/api/quiz/status/${jobId}`);
        if (!response.ok) {
          throw await createErrorFromResponse(response);
        }

        const result = await response.json();
        const { status, result: quizResult, message } = result.data;

        if (status === "COMPLETED" && quizResult) {
          setQuizData(quizResult);
          setStep("QUIZ");
          setCurrentQuestionIndex(0);
          setUserAnswers({});
          return; // 성공 종료
        } else if (status === "FAILED") {
          throw new Error(message || "퀴즈 생성에 실패했습니다.");
        }
        // PROCESSING 상태면 계속 루프
      } catch (error) {
        throw error; // 에러 발생 시 상위 catch로 전달
      }
      retryCount++;
    }
    throw new Error("작업 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.");
  };

  const handleOptionSelect = (questionId, option) => {
    setUserAnswers(prev => ({
      ...prev,
      [questionId]: option
    }));
  };

  const handleNext = () => {
    if (currentQuestionIndex < quizData.questions.length - 1) {
      setCurrentQuestionIndex(prev => prev + 1);
    } else {
      setStep("RESULT");
    }
  };

  const handleRetry = () => {
    setStep("INPUT");
    setQuizData(null);
    setCurrentQuestionIndex(0);
    setUserAnswers({});
    setUrl("");
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col font-sans text-slate-900">
      {step !== "INPUT" && <Header />}

      <main className={`flex-1 flex flex-col items-center justify-center px-4 py-12 ${step !== "INPUT" ? "pt-24" : ""}`}>
        {step === "INPUT" && (
          <InputSection
            url={url}
            setUrl={setUrl}
            quizCount={quizCount}
            setQuizCount={setQuizCount}
            isLoading={isLoading}
            onGenerate={handleGenerate}
          />
        )}

        {step === "QUIZ" && quizData && (
          <QuizSection
            quizData={quizData}
            currentQuestionIndex={currentQuestionIndex}
            userAnswers={userAnswers}
            onOptionSelect={handleOptionSelect}
            onNext={handleNext}
            onRetry={handleRetry}
          />
        )}

        {step === "RESULT" && quizData && (
          <ResultSection
            quizData={quizData}
            userAnswers={userAnswers}
            onRetry={handleRetry}
          />
        )}
      </main>
    </div>
  );
}
