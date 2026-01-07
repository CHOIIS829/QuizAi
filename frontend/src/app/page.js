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
        throw new Error("Failed to generate quiz");
      }

      const result = await response.json();
      setQuizData(result.data);
      setStep("QUIZ");
      setCurrentQuestionIndex(0);
      setUserAnswers({});
    } catch (error) {
      console.error("Error:", error);
      alert(`문제 생성 중 오류가 발생했습니다: ${error.message}`);
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
