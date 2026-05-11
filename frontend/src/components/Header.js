"use client";

import Link from "next/link";
import { useState } from "react";
import { BrainCircuit, LayoutGrid, LibraryBig, LogOut, UserCircle2 } from "lucide-react";
import { useAuth } from "./AuthProvider";
import LoginModal from "./LoginModal";
import { startOAuthLogin } from "../lib/api";

export default function Header() {
  const { user, logout } = useAuth();
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);

  return (
    <>
      <header className="fixed top-0 left-0 z-50 w-full border-b border-slate-200/70 bg-[#F8FAFC]/85 backdrop-blur">
        <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-5 py-4">
          <Link href="/" className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-blue-100 shadow-sm">
              <BrainCircuit className="h-6 w-6 text-blue-600" />
            </div>
            <div>
              <p className="text-lg font-extrabold tracking-tight text-slate-900">Quiz AI</p>
              <p className="text-xs text-slate-500">AI 튜터형 퀴즈 생성기</p>
            </div>
          </Link>

          <nav className="hidden items-center gap-2 md:flex">
            <Link href="/board" className="flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-white hover:text-slate-900">
              <LayoutGrid className="h-4 w-4" />
              게시판
            </Link>
            <Link href="/mypage" className="flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-white hover:text-slate-900">
              <LibraryBig className="h-4 w-4" />
              마이페이지
            </Link>
          </nav>

          <div className="flex items-center gap-3">
            {user ? (
              <>
                <div className="hidden items-center gap-2 rounded-full bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm sm:flex">
                  <UserCircle2 className="h-4 w-4 text-slate-400" />
                  {user.nickname || user.email}
                </div>
                <button
                  type="button"
                  onClick={logout}
                  className="flex items-center gap-2 rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-700"
                >
                  <LogOut className="h-4 w-4" />
                  로그아웃
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={() => setIsLoginModalOpen(true)}
                className="rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-700"
              >
                로그인
              </button>
            )}
          </div>
        </div>
      </header>

      <LoginModal
        open={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onSelectProvider={(provider) => startOAuthLogin(provider)}
      />
    </>
  );
}
