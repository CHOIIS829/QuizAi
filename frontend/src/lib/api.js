"use client";

const API_BASE_URL = process.env.NODE_ENV === "development" ? "http://localhost:8080" : "";

export function buildApiUrl(path) {
  return `${API_BASE_URL}${path}`;
}

export function startOAuthLogin(provider) {
  window.location.href = buildApiUrl(`/oauth2/authorization/${provider}`);
}

export async function fetchJson(path, options = {}) {
  const { headers, ...rest } = options;
  const response = await fetch(buildApiUrl(path), {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    ...rest,
  });

  if (!response.ok) {
    throw await createErrorFromResponse(response);
  }

  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    return null;
  }

  return response.json();
}

export async function createErrorFromResponse(response) {
  const errorData = await response.json().catch(() => null);

  if (response.status === 401) {
    return new Error("로그인이 필요합니다.");
  }

  if (errorData?.errorCode) {
    switch (errorData.errorCode) {
      case "FAIL_CRAWL":
        return new Error("퀴즈 생성이 불가능한 URL입니다.");
      case "FAIL_DOWNLOAD":
        return new Error("영상 길이 또는 다운로드 상태 때문에 분석에 실패했습니다.");
      case "GEMINI_FAIL_ERROR":
        return new Error("현재 AI 요청이 많습니다. 잠시 후 다시 시도해주세요.");
      case "RATE_LIMIT_EXCEEDED":
        return new Error("요청이 많습니다. 잠시 후 다시 시도해주세요.");
      case "DUPLICATE_NICKNAME":
        return new Error("이미 사용 중인 닉네임입니다.");
      default:
        return new Error(errorData.message || "오류가 발생했습니다.");
    }
  }

  return new Error("서버와의 통신에 실패했습니다.");
}
