"use client";

const PENDING_GUEST_QUIZ_KEY = "quizai_pending_guest_quiz";

export function savePendingGuestQuiz(payload) {
  if (typeof window === "undefined") return;
  window.sessionStorage.setItem(PENDING_GUEST_QUIZ_KEY, JSON.stringify(payload));
}

export function readPendingGuestQuiz() {
  if (typeof window === "undefined") return null;

  const rawValue = window.sessionStorage.getItem(PENDING_GUEST_QUIZ_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue);
  } catch {
    window.sessionStorage.removeItem(PENDING_GUEST_QUIZ_KEY);
    return null;
  }
}

export function clearPendingGuestQuiz() {
  if (typeof window === "undefined") return;
  window.sessionStorage.removeItem(PENDING_GUEST_QUIZ_KEY);
}
