"use client";

import { useEffect, useState } from "react";
import FilterBar from "../../components/FilterBar";
import QuizCard from "../../components/QuizCard";
import LoginModal from "../../components/LoginModal";
import { useAuth } from "../../components/AuthProvider";
import { fetchJson, startOAuthLogin } from "../../lib/api";

export default function MyPage() {
  const { user, isLoading } = useAuth();
  const [filters, setFilters] = useState({ sourceType: "", tag: "" });
  const [quizzes, setQuizzes] = useState([]);
  const [isPageLoading, setIsPageLoading] = useState(true);
  const [error, setError] = useState("");
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);

  useEffect(() => {
    const loadQuizzes = async () => {
      setIsPageLoading(true);
      setError("");

      try {
        const searchParams = new URLSearchParams();
        if (filters.sourceType) searchParams.set("sourceType", filters.sourceType);
        if (filters.tag) searchParams.set("tag", filters.tag);
        searchParams.set("page", "0");
        searchParams.set("size", "12");

        const response = await fetchJson(`/api/my/quizzes?${searchParams.toString()}`);
        setQuizzes(response.data.content);
      } catch (fetchError) {
        setError(fetchError.message);
      } finally {
        setIsPageLoading(false);
      }
    };

    if (isLoading) return;
    if (!user) {
      setIsPageLoading(false);
      return;
    }

    loadQuizzes();
  }, [user, isLoading, filters.sourceType, filters.tag]);

  return (
    <>
      <main className="min-h-screen bg-[#F8FAFC] px-4 pb-24 pt-32">
        <div className="mx-auto max-w-6xl">
          <div className="mb-8">
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-blue-600">My Page</p>
            <h1 className="mt-3 text-4xl font-bold tracking-tight text-slate-900">내가 저장한 퀴즈</h1>
            <p className="mt-3 max-w-2xl text-base leading-relaxed text-slate-500">
              로그인한 상태에서 생성하거나, 게스트 결과 화면에서 저장한 퀴즈를 여기서 다시 풀 수 있어요.
            </p>
          </div>

          {!user && !isLoading ? (
            <div className="rounded-[2rem] border border-slate-200 bg-white p-10 text-center shadow-sm">
              <h2 className="text-2xl font-bold text-slate-900">로그인이 필요한 공간입니다</h2>
              <p className="mt-3 text-slate-500">Google 또는 Kakao로 로그인하면 내 퀴즈 히스토리를 저장할 수 있어요.</p>
              <button
                type="button"
                onClick={() => setIsLoginModalOpen(true)}
                className="mt-6 rounded-full bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-700"
              >
                로그인하기
              </button>
            </div>
          ) : (
            <>
              <FilterBar sourceType={filters.sourceType} tag={filters.tag} onChange={setFilters} />

              {isPageLoading ? (
                <div className="rounded-[2rem] bg-white p-10 text-center text-slate-500 shadow-sm">불러오는 중...</div>
              ) : error ? (
                <div className="rounded-[2rem] bg-white p-10 text-center text-red-500 shadow-sm">{error}</div>
              ) : quizzes.length === 0 ? (
                <div className="rounded-[2rem] bg-white p-10 text-center text-slate-500 shadow-sm">아직 저장된 퀴즈가 없습니다.</div>
              ) : (
                <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                  {quizzes.map((quiz) => (
                    <QuizCard key={quiz.id} quiz={quiz} />
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </main>

      <LoginModal
        open={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onSelectProvider={(provider) => startOAuthLogin(provider)}
      />
    </>
  );
}
