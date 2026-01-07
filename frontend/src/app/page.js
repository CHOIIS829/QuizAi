"use client";

import Image from "next/image";
import { useState } from "react";
import { BookOpen, Play, BrainCircuit } from "lucide-react";

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

  // --- Render Helpers ---

  const renderInputStep = () => (
    <div className="w-full max-w-[580px] bg-white rounded-3xl shadow-xl shadow-slate-200/60 p-8 border border-slate-100">
      <div className="space-y-2 mb-8">
        <label className="text-sm font-semibold text-slate-700 ml-1">학습 자료 URL</label>
        <div className="relative group">
          <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
            <BookOpen className="h-5 w-5 text-slate-400 group-focus-within:text-blue-500 transition-colors" />
          </div>
          <input
            type="text"
            className="block w-full rounded-xl border-0 py-4 pl-12 pr-4 text-slate-900 shadow-sm ring-1 ring-inset ring-slate-200 placeholder:text-slate-400 focus:ring-2 focus:ring-inset focus:ring-blue-500 sm:text-base bg-white transition-all outline-none"
            placeholder="https://youtube.com/watch?v=... 또는 블로그 주소"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
          />
        </div>
      </div>

      <div className="space-y-3 mb-8">
        <label className="text-sm font-semibold text-slate-700 ml-1">문제 개수 설정</label>
        <div className="grid grid-cols-4 gap-3">
          {[3, 5, 10, 20].map((count) => (
            <button
              key={count}
              onClick={() => setQuizCount(count)}
              className={`py-3 rounded-xl text-sm font-medium transition-all duration-200 ${quizCount === count
                ? "bg-blue-600 text-white shadow-md shadow-blue-200 scale-105"
                : "bg-slate-50 text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                }`}
            >
              {count}문제
            </button>
          ))}
        </div>
      </div>

      <button
        onClick={handleGenerate}
        disabled={isLoading}
        className="w-full bg-[#0F172A] hover:bg-[#1E293B] text-white rounded-xl py-4 font-semibold text-lg shadow-lg shadow-slate-900/20 transition-all active:scale-[0.98] flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
      >
        {isLoading ? (
          <>
            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            <span>분석 중...</span>
          </>
        ) : (
          <>
            <Play className="w-5 h-5 fill-current" />
            <span>문제 생성하기</span>
          </>
        )}
      </button>

      <div className="mt-6 text-center">
        <p className="text-xs font-medium text-slate-400 flex items-center justify-center gap-1.5">
          <BrainCircuit className="w-3.5 h-3.5" />
          Powered by Google Gemini 1.5 Pro
        </p>
      </div>
    </div>
  );

  const renderQuizStep = () => {
    if (!quizData || !quizData.questions || quizData.questions.length === 0) {
      return <div className="p-8 text-center text-red-500">데이터 오류: 문제를 불러올 수 없습니다.</div>;
    }

    const question = quizData.questions[currentQuestionIndex];
    if (!question) {
      return <div className="p-8 text-center text-red-500">문제 인덱스 오류입니다.</div>;
    }

    const progress = Math.round(((currentQuestionIndex + 1) / quizData.questions.length) * 100);

    return (
      <div className="w-full max-w-2xl">
        <div className="mb-6 flex items-center justify-between text-slate-500 text-sm font-medium">
          <span>진행률 {progress}%</span>
          <span>{currentQuestionIndex + 1} / {quizData.questions.length}</span>
        </div>

        {/* Progress Bar */}
        <div className="w-full h-2 bg-slate-100 rounded-full mb-8 overflow-hidden">
          <div
            className="h-full bg-blue-500 transition-all duration-500 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>

        <div className="bg-white rounded-3xl shadow-xl shadow-slate-200/60 p-8 border border-slate-100">
          <span className="inline-block px-3 py-1 bg-blue-50 text-blue-600 rounded-full text-xs font-bold mb-4">
            Q{currentQuestionIndex + 1}
          </span>
          <h2 className="text-xl font-bold text-slate-900 mb-6 leading-relaxed">
            {question.question}
          </h2>

          {question.codeSnippet && (
            <pre className="bg-slate-900 text-slate-50 p-6 rounded-xl mb-8 overflow-x-auto text-sm font-mono leading-relaxed whitespace-pre-wrap">
              {question.codeSnippet}
            </pre>
          )}

          <div className="space-y-3">
            {question.options.map((option, idx) => (
              <button
                key={idx}
                onClick={() => handleOptionSelect(question.id, option)}
                className={`w-full text-left p-4 rounded-xl border-2 transition-all duration-200 ${userAnswers[question.id] === option
                  ? "border-blue-500 bg-blue-50 text-blue-700"
                  : "border-slate-100 hover:border-blue-200 hover:bg-slate-50 text-slate-600"
                  }`}
              >
                <div className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full border flex items-center justify-center text-xs ${userAnswers[question.id] === option
                    ? "border-blue-500 bg-blue-500 text-white"
                    : "border-slate-300 text-slate-400"
                    }`}>
                    {String.fromCharCode(65 + idx)}
                  </div>
                  {option}
                </div>
              </button>
            ))}
          </div>

          <div className="mt-8 flex justify-end">
            <button
              onClick={handleNext}
              disabled={!userAnswers[question.id]}
              className="px-8 py-3 bg-[#0F172A] text-white rounded-xl font-semibold hover:bg-[#1E293B] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {currentQuestionIndex === quizData.questions.length - 1 ? '제출하기' : '다음 문제'}
            </button>
          </div>
        </div>
      </div>
    );
  };

  const renderResultStep = () => {
    if (!quizData || !quizData.questions) return null;

    const correctCount = quizData.questions.filter(
      q => userAnswers[q.id] === q.answer
    ).length;

    return (
      <div className="w-full max-w-2xl pb-20">
        <div className="bg-white rounded-3xl p-8 mb-8 text-center shadow-lg border border-slate-100">
          <h2 className="text-2xl font-bold text-slate-900 mb-2">학습 완료! 🎉</h2>
          <p className="text-slate-500">
            총 {quizData.questions.length}문제 중 <span className="text-blue-600 font-bold text-xl">{correctCount}</span>문제를 맞추셨습니다.
          </p>
        </div>

        <div className="space-y-8">
          {quizData.questions.map((q, idx) => {
            const isCorrect = userAnswers[q.id] === q.answer;
            return (
              <div key={q.id} className={`bg-white rounded-3xl p-8 shadow-sm border-2 ${isCorrect ? 'border-transparent' : 'border-red-100'}`}>
                <div className="flex items-center gap-3 mb-4">
                  <span className={`flex items-center justify-center w-8 h-8 rounded-full font-bold text-sm ${isCorrect ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'
                    }`}>
                    {isCorrect ? 'O' : 'X'}
                  </span>
                  <span className="text-slate-400 font-medium">Question {idx + 1}</span>
                </div>

                <h3 className="text-lg font-bold text-slate-900 mb-4">{q.question}</h3>

                {q.codeSnippet && (
                  <pre className="bg-slate-900 text-slate-50 p-4 rounded-xl mb-6 overflow-x-auto text-sm font-mono whitespace-pre-wrap">
                    {q.codeSnippet}
                  </pre>
                )}

                <div className="space-y-2 mb-6">
                  {q.options.map((option, optIdx) => {
                    const isSelected = userAnswers[q.id] === option;
                    const isAnswer = q.answer === option;
                    let style = "border-slate-100 text-slate-500";

                    if (isAnswer) style = "border-green-500 bg-green-50 text-green-700 font-medium";
                    else if (isSelected && !isCorrect) style = "border-red-500 bg-red-50 text-red-700";

                    return (
                      <div key={optIdx} className={`p-4 rounded-xl border ${style} flex justify-between items-center`}>
                        <span>{option}</span>
                        {isAnswer && <span className="text-xs bg-green-200 text-green-800 px-2 py-0.5 rounded">정답</span>}
                        {isSelected && !isCorrect && <span className="text-xs bg-red-200 text-red-800 px-2 py-0.5 rounded">내 답</span>}
                      </div>
                    );
                  })}
                </div>

                <div className="bg-slate-50 p-5 rounded-xl text-sm leading-relaxed text-slate-700">
                  <span className="font-bold block mb-1">📝 해설</span>
                  {q.explanation}
                </div>
              </div>
            );
          })}
        </div>

        <div className="fixed bottom-8 left-1/2 -translate-x-1/2">
          <button
            onClick={handleRetry}
            className="px-8 py-4 bg-[#0F172A] text-white rounded-full font-bold shadow-2xl hover:bg-[#1E293B] transition-transform hover:scale-105 active:scale-95"
          >
            다른 문제 만들기
          </button>
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col font-sans text-slate-900">
      <main className="flex-1 flex flex-col items-center justify-center px-4 py-12">
        {step === "INPUT" && (
          <div className="text-center space-y-6 mb-12">
            <div className="flex flex-col items-center gap-4 mb-8">
              <div className="w-16 h-16 bg-blue-100 rounded-2xl flex items-center justify-center shadow-sm">
                <BrainCircuit className="w-10 h-10 text-blue-600" />
              </div>
              <span className="text-3xl font-extrabold tracking-tight text-slate-900">Quiz AI</span>
            </div>
            <h1 className="text-4xl md:text-5xl font-bold tracking-tight text-slate-900">
              어떤 걸 학습하고 계신가요?
            </h1>
            <p className="text-lg text-slate-500 max-w-xl mx-auto">
              유튜브 영상이나 기술 블로그 URL만 넣으세요.
              <br className="hidden md:block" />
              AI가 핵심 내용을 분석해 모의고사를 만들어드립니다.
            </p>
          </div>
        )}

        {step === "INPUT" && renderInputStep()}
        {step === "QUIZ" && quizData && renderQuizStep()}
        {step === "RESULT" && quizData && renderResultStep()}
      </main>
    </div>
  );
}
