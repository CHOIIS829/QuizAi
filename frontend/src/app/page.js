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
      const apiUrl = process.env.NODE_ENV === 'development'
        ? "http://localhost:8080/api/quiz/generate"
        : "/api/quiz/generate";

      const response = await fetch(apiUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          url: url,
          quizCount: quizCount,
        }),
      });

      if (!response.ok) {
        // 에러 응답 파싱
        const errorData = await response.json().catch(() => null);

        if (errorData && errorData.errorCode) {
          switch (errorData.errorCode) {
            case "FAIL_CRAWL":
              alert("퀴즈 생성이 불가능한 URL입니다.");
              break;
            case "FAIL_DOWNLOAD":
              alert("영상의 길이가 너무 깁니다. \n다른 영상으로 시도해주세요.");
              break;
            case "GEMINI_FAIL_ERROR":
              alert("현재 요청이 많아 잠시 후 다시 시도해주세요.");
              break;
            default:
              alert(`오류가 발생했습니다: ${errorData.message}`);
          }
          return; // 에러 처리 완료 후 종단
        }

        throw new Error("서버와의 통신에 실패했습니다.");
      }

      const result = await response.json();
      setQuizData(result.data);
      setStep("QUIZ");
      setCurrentQuestionIndex(0);
      setUserAnswers({});
    } catch (error) {
      console.error("Error:", error);
      // 위에서 처리되지 않은 일반적인 에러만 여기서 처리
      alert(`문제 생성 중 알 수 없는 오류가 발생했습니다: ${error.message}`);
    } finally {
      setIsLoading(false);
    }
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
