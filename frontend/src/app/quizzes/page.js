"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import QuizSection from "../../components/QuizSection";
import ResultSection from "../../components/ResultSection";
import { fetchJson } from "../../lib/api";
import { getQuizProgress, isValidQuestionIndex } from "../../lib/quiz-navigation";

export default function PersistedQuizPage() {
  return (
    <Suspense fallback={<QuizPageFallback message="퀴즈를 불러오는 중..." />}>
      <PersistedQuizContent />
    </Suspense>
  );
}

function PersistedQuizContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const quizId = searchParams.get("id");

  const [quizData, setQuizData] = useState(null);
  const [quizMeta, setQuizMeta] = useState(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [userAnswers, setUserAnswers] = useState({});
  const [step, setStep] = useState("QUIZ");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadQuiz = async () => {
      setIsLoading(true);
      setError("");

      try {
        const response = await fetchJson(`/api/quizzes/${quizId}`);
        setQuizMeta(response.data.quiz);
        setQuizData(response.data.quizResult);
        setStep("QUIZ");
        setCurrentQuestionIndex(0);
        setUserAnswers({});
      } catch (fetchError) {
        setError(fetchError.message);
      } finally {
        setIsLoading(false);
      }
    };

    if (!quizId) {
      setError("퀴즈 식별자가 없습니다.");
      setIsLoading(false);
      return;
    }

    loadQuiz();
  }, [quizId]);

  const handleOptionSelect = (questionId, option) => {
    setUserAnswers((prev) => ({
      ...prev,
      [questionId]: option,
    }));
  };

  const handleQuestionChange = (index) => {
    // 답안을 유지하면서 유효한 번호의 문제로 이동합니다.
    if (isValidQuestionIndex(index, quizData?.questions.length ?? 0)) {
      setCurrentQuestionIndex(index);
    }
  };

  const handleNext = () => {
    // 다음 문제로 이동하거나 모든 답변이 완료된 경우에만 결과를 표시합니다.
    if (currentQuestionIndex < quizData.questions.length - 1) {
      handleQuestionChange(currentQuestionIndex + 1);
      return;
    }
    if (getQuizProgress(quizData.questions, userAnswers).isComplete) {
      setStep("RESULT");
    }
  };

  const handleRetry = () => {
    setStep("QUIZ");
    setCurrentQuestionIndex(0);
    setUserAnswers({});
  };

  return (
    <main className="min-h-screen bg-[#F8FAFC] px-4 pb-24 pt-32">
      <div className="mx-auto max-w-5xl">
        {isLoading ? (
          <QuizPageFallback message="퀴즈를 불러오는 중..." />
        ) : error ? (
          <div className="rounded-[2rem] bg-white p-10 text-center shadow-sm">
            <p className="text-red-500">{error}</p>
            <button
              type="button"
              onClick={() => router.push("/board")}
              className="mt-4 rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-700"
            >
              게시판으로 이동
            </button>
          </div>
        ) : (
          <>
            <div className="mx-auto mb-6 w-full max-w-4xl rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
              <p className="text-sm font-semibold text-blue-600">{quizMeta?.sourceType}</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">{quizMeta?.title}</h1>
              <p className="mt-3 text-sm text-slate-500">
                작성자 {quizMeta?.authorNickname || "닉네임 미설정"} · {quizMeta?.sourceHost}
              </p>
              <div className="mt-4 flex flex-wrap gap-2">
                {quizMeta?.topicTags?.map((tag) => (
                  <span key={tag.slug} className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                    #{tag.displayName}
                  </span>
                ))}
              </div>
            </div>

            {step === "QUIZ" && quizData && (
              <QuizSection
                quizData={quizData}
                currentQuestionIndex={currentQuestionIndex}
                userAnswers={userAnswers}
                onOptionSelect={handleOptionSelect}
                onQuestionChange={handleQuestionChange}
                onNext={handleNext}
                onRetry={() => router.back()}
              />
            )}

            {step === "RESULT" && quizData && (
              <ResultSection
                quizData={quizData}
                userAnswers={userAnswers}
                onRetry={handleRetry}
              />
            )}
          </>
        )}
      </div>
    </main>
  );
}

function QuizPageFallback({ message }) {
  return (
    <div className="rounded-[2rem] bg-white p-10 text-center text-slate-500 shadow-sm">
      {message}
    </div>
  );
}
