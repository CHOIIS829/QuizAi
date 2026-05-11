"use client";

import { useEffect, useState } from "react";
import FilterBar from "../../components/FilterBar";
import QuizCard from "../../components/QuizCard";
import { fetchJson } from "../../lib/api";

export default function BoardPage() {
  const [filters, setFilters] = useState({ sourceType: "", tag: "" });
  const [quizzes, setQuizzes] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadBoard = async () => {
      setIsLoading(true);
      setError("");

      try {
        const searchParams = new URLSearchParams();
        if (filters.sourceType) searchParams.set("sourceType", filters.sourceType);
        if (filters.tag) searchParams.set("tag", filters.tag);
        searchParams.set("page", "0");
        searchParams.set("size", "12");

        const response = await fetchJson(`/api/board/quizzes?${searchParams.toString()}`);
        setQuizzes(response.data.content);
      } catch (fetchError) {
        setError(fetchError.message);
      } finally {
        setIsLoading(false);
      }
    };

    loadBoard();
  }, [filters.sourceType, filters.tag]);

  return (
    <main className="min-h-screen bg-[#F8FAFC] px-4 pb-24 pt-32">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8">
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-blue-600">Board</p>
          <h1 className="mt-3 text-4xl font-bold tracking-tight text-slate-900">사람들이 만든 공개 퀴즈</h1>
          <p className="mt-3 max-w-2xl text-base leading-relaxed text-slate-500">
            URL을 직접 찾지 않아도, 공개된 문제를 태그와 소스 유형별로 찾아 바로 풀어볼 수 있어요.
          </p>
        </div>

        <FilterBar sourceType={filters.sourceType} tag={filters.tag} onChange={setFilters} />

        {isLoading ? (
          <div className="rounded-[2rem] bg-white p-10 text-center text-slate-500 shadow-sm">불러오는 중...</div>
        ) : error ? (
          <div className="rounded-[2rem] bg-white p-10 text-center text-red-500 shadow-sm">{error}</div>
        ) : quizzes.length === 0 ? (
          <div className="rounded-[2rem] bg-white p-10 text-center text-slate-500 shadow-sm">아직 공개된 퀴즈가 없습니다.</div>
        ) : (
          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            {quizzes.map((quiz) => (
              <QuizCard key={quiz.id} quiz={quiz} />
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
