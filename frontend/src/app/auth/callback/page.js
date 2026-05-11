"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "../../../components/AuthProvider";
import { clearPendingGuestQuiz, readPendingGuestQuiz } from "../../../lib/guest-quiz";
import { fetchJson } from "../../../lib/api";

export default function AuthCallbackPage() {
  return (
    <Suspense fallback={<CallbackFallback message="로그인 정보를 확인하고 있습니다..." />}>
      <AuthCallbackContent />
    </Suspense>
  );
}

function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshUser } = useAuth();
  const hasHandledRef = useRef(false);
  const [message, setMessage] = useState("로그인 정보를 확인하고 있습니다...");

  useEffect(() => {
    if (hasHandledRef.current) {
      return;
    }
    hasHandledRef.current = true;

    const finishLogin = async () => {
      const oauthError = searchParams.get("error");
      if (oauthError) {
        setMessage(resolveOAuthErrorMessage(oauthError, searchParams.get("reason")));
        return;
      }

      const currentUser = await refreshUser();
      if (!currentUser) {
        setMessage("로그인 상태를 확인할 수 없습니다.");
        return;
      }

      if (currentUser.needsNickname) {
        router.replace("/onboarding/nickname");
        return;
      }

      const pendingGuestQuiz = readPendingGuestQuiz();
      if (pendingGuestQuiz) {
        try {
          setMessage("게스트 퀴즈를 내 히스토리로 저장하는 중입니다...");
          const response = await fetchJson("/api/auth/guest-quizzes/import", {
            method: "POST",
            body: JSON.stringify(pendingGuestQuiz),
          });
          clearPendingGuestQuiz();
          router.replace(`/quizzes?id=${response.data.quizId}`);
          return;
        } catch (error) {
          setMessage(error.message || "게스트 퀴즈 저장에 실패했습니다.");
          return;
        }
      }

      router.replace("/mypage");
    };

    finishLogin();
  }, [router, searchParams, refreshUser]);

  return (
    <CallbackFallback message={message} />
  );
}

function resolveOAuthErrorMessage(errorCode, reason) {
  const normalizedCode = String(errorCode || "").toLowerCase();
  const normalizedReason = String(reason || "").toLowerCase();

  if (normalizedCode.includes("invalid_client") || normalizedReason.includes("invalid_client")) {
    return "Google OAuth 클라이언트 정보(client id/secret)가 올바르지 않습니다.";
  }

  if (normalizedCode.includes("invalid_token_response") || normalizedReason.includes("token response: 401")) {
    return "Google OAuth 토큰 교환에 실패했습니다. 현재 client secret 값이 Google Console의 활성 secret과 일치하는지 확인해주세요.";
  }

  if (normalizedCode.includes("redirect_uri_mismatch") || normalizedReason.includes("redirect_uri")) {
    return "Google OAuth 리디렉션 URI 설정이 일치하지 않습니다.";
  }

  if (normalizedCode.includes("access_denied")) {
    return "Google 계정 접근이 거부되었습니다. 테스트 사용자 등록 또는 게시 상태를 확인해주세요.";
  }

  if (normalizedCode.includes("oauth_email_missing")) {
    return "Google 계정에서 이메일 정보를 가져오지 못했습니다. 계정 권한을 확인해주세요.";
  }

  if (normalizedReason) {
    return `로그인에 실패했습니다. (${reason})`;
  }

  return "로그인에 실패했습니다. 다시 시도해주세요.";
}

function CallbackFallback({ message }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#F8FAFC] px-4 pt-24">
      <div className="w-full max-w-lg rounded-[2rem] border border-slate-200 bg-white p-10 text-center shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900">로그인 처리 중</h1>
        <p className="mt-4 leading-relaxed text-slate-500">{message}</p>
      </div>
    </main>
  );
}
