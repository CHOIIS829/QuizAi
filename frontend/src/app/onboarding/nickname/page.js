"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../../../components/AuthProvider";
import { clearPendingGuestQuiz, readPendingGuestQuiz } from "../../../lib/guest-quiz";
import { fetchJson } from "../../../lib/api";

export default function NicknameOnboardingPage() {
  const router = useRouter();
  const { refreshUser } = useAuth();
  const [nickname, setNickname] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    setError("");

    try {
      await fetchJson("/api/users/me/nickname", {
        method: "POST",
        body: JSON.stringify({ nickname }),
      });

      await refreshUser();

      const pendingGuestQuiz = readPendingGuestQuiz();
      if (pendingGuestQuiz) {
        const response = await fetchJson("/api/auth/guest-quizzes/import", {
          method: "POST",
          body: JSON.stringify(pendingGuestQuiz),
        });
        clearPendingGuestQuiz();
        router.replace(`/quizzes?id=${response.data.quizId}`);
        return;
      }

      router.replace("/mypage");
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#F8FAFC] px-4 pt-24">
      <div className="w-full max-w-lg rounded-[2rem] border border-slate-200 bg-white p-10 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-blue-600">Onboarding</p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-900">서비스용 닉네임을 정해주세요</h1>
        <p className="mt-3 text-slate-500">
          게시판 작성자 이름과 마이페이지 표시에 사용됩니다. 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있어요.
        </p>

        <form onSubmit={handleSubmit} className="mt-8 space-y-4">
          <input
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            placeholder="예: ondongne_dev"
            className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-slate-900 outline-none transition focus:border-blue-400"
          />

          {error && <p className="text-sm text-red-500">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-full bg-slate-900 px-5 py-4 text-sm font-semibold text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "저장 중..." : "닉네임 저장하고 시작하기"}
          </button>
        </form>
      </div>
    </main>
  );
}
