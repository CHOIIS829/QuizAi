"use client";

import Link from "next/link";
import { useState } from "react";
import InputSection from "../components/InputSection";
import QuizSection from "../components/QuizSection";
import ResultSection from "../components/ResultSection";
import LoginModal from "../components/LoginModal";
import { useAuth } from "../components/AuthProvider";
import { fetchJson, startOAuthLogin } from "../lib/api";
import { savePendingGuestQuiz } from "../lib/guest-quiz";

export default function Home() {
  const { user } = useAuth();
  const [step, setStep] = useState("INPUT");
  const [quizData, setQuizData] = useState(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [userAnswers, setUserAnswers] = useState({});
  const [url, setUrl] = useState("");
  const [quizCount, setQuizCount] = useState(5);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);

  const handleGenerate = async () => {
    if (!url) {
      alert("URL을 입력해주세요.");
      return;
    }

    setIsLoading(true);

    try {
      const startResult = await fetchJson("/api/quiz/generate", {
        method: "POST",
        body: JSON.stringify({
          url,
          quizCount,
        }),
      });

      await pollJobStatus(startResult.data.jobId);
    } catch (error) {
      alert(error.message || "문제 생성 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const pollJobStatus = async (jobId) => {
    const maxRetries = 300;
    let retryCount = 0;

    while (retryCount < maxRetries) {
      await new Promise((resolve) => setTimeout(resolve, 2000));

      const result = await fetchJson(`/api/quiz/status/${jobId}`);
      const { status, result: quizResult, message } = result.data;

      if (status === "COMPLETED" && quizResult) {
        setQuizData(quizResult);
        setStep("QUIZ");
        setCurrentQuestionIndex(0);
        setUserAnswers({});
        return;
      }

      if (status === "FAILED") {
        throw new Error(message || "퀴즈 생성에 실패했습니다.");
      }

      retryCount += 1;
    }

    throw new Error("작업 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.");
  };

  const handleOptionSelect = (questionId, option) => {
    setUserAnswers((prev) => ({
      ...prev,
      [questionId]: option,
    }));
  };

  const handleNext = () => {
    if (currentQuestionIndex < quizData.questions.length - 1) {
      setCurrentQuestionIndex((prev) => prev + 1);
      return;
    }
    setStep("RESULT");
  };

  const handleRetry = () => {
    setStep("INPUT");
    setQuizData(null);
    setCurrentQuestionIndex(0);
    setUserAnswers({});
    setUrl("");
  };

  const handleGuestSaveLogin = (provider) => {
    if (quizData) {
      savePendingGuestQuiz({
        sourceUrl: url,
        quizResult: quizData,
      });
    }

    startOAuthLogin(provider);
  };

  return (
    <>
      <main className={`min-h-screen bg-[#F8FAFC] px-4 py-12 text-slate-900 ${step !== "INPUT" ? "pt-28" : "pt-32"}`}>
        <div className="mx-auto flex max-w-5xl justify-center">
          {step === "INPUT" && (
            <InputSection
              url={url}
              setUrl={setUrl}
              quizCount={quizCount}
              setQuizCount={setQuizCount}
              isLoading={isLoading}
              onGenerate={handleGenerate}
              user={user}
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
              showGuestSavePrompt={!user}
              onGuestSave={() => setIsLoginModalOpen(true)}
            />
          )}
        </div>

        <div className="mx-auto mt-14 flex max-w-5xl justify-center border-t border-slate-200 pt-6">
          <Link href="/privacy" className="text-sm font-semibold text-slate-500 transition hover:text-slate-900">
            개인정보처리방침
          </Link>
        </div>
      </main>

      <LoginModal
        open={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onSelectProvider={handleGuestSaveLogin}
      />
    </>
  );
}
