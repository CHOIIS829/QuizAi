"use client";

import { X, Chrome, MessageCircleMore } from "lucide-react";

export default function LoginModal({ open, onClose, onSelectProvider }) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/35 px-4">
      <div className="w-full max-w-md rounded-[2rem] bg-white p-8 shadow-2xl shadow-slate-900/20">
        <div className="mb-6 flex items-start justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">로그인하고 이어서 사용하기</h2>
            <p className="mt-2 text-sm leading-relaxed text-slate-500">
              로그인하면 생성한 퀴즈를 마이페이지에 저장하고, 나중에 다시 풀어볼 수 있어요.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-3">
          <button
            type="button"
            onClick={() => onSelectProvider("google")}
            className="flex w-full items-center justify-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-4 font-semibold text-slate-900 transition hover:border-slate-300 hover:bg-slate-50"
          >
            <Chrome className="h-5 w-5" />
            Google로 계속하기
          </button>
          <button
            type="button"
            onClick={() => onSelectProvider("kakao")}
            className="flex w-full items-center justify-center gap-3 rounded-2xl border border-[#F7E14B] bg-[#FEE500] px-4 py-4 font-semibold text-[#191919] transition hover:brightness-95"
          >
            <MessageCircleMore className="h-5 w-5" />
            Kakao로 계속하기
          </button>
        </div>
      </div>
    </div>
  );
}
