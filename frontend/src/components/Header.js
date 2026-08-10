"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { BrainCircuit, LayoutGrid, LibraryBig, LogIn, LogOut, Menu, PlusCircle, UserCircle2, X } from "lucide-react";
import { useAuth } from "./AuthProvider";
import LoginModal from "./LoginModal";
import { startOAuthLogin } from "../lib/api";
import { prefetchDefaultBoardQuizzes } from "../lib/boardApi";

export default function Header() {
  const { user, logout } = useAuth();
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    if (!isMobileMenuOpen) return undefined;

    const originalOverflow = document.body.style.overflow;
    const handleEscape = (event) => {
      if (event.key === "Escape") setIsMobileMenuOpen(false);
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleEscape);

    return () => {
      document.body.style.overflow = originalOverflow;
      window.removeEventListener("keydown", handleEscape);
    };
  }, [isMobileMenuOpen]);

  const closeMobileMenu = () => setIsMobileMenuOpen(false);
  const openLoginModal = () => {
    closeMobileMenu();
    setIsLoginModalOpen(true);
  };

  const handleMobileBoardClick = () => {
    // 모바일 게시판 이동과 목록 요청을 동시에 시작하고 메뉴를 닫는다.
    prefetchDefaultBoardQuizzes();
    closeMobileMenu();
  };

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
            <Link href="/" className="flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-white hover:text-slate-900">
              <PlusCircle className="h-4 w-4" />
              퀴즈 만들기
            </Link>
            <Link
              href="/board"
              onMouseEnter={prefetchDefaultBoardQuizzes}
              onFocus={prefetchDefaultBoardQuizzes}
              onClick={prefetchDefaultBoardQuizzes}
              className="flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-white hover:text-slate-900"
            >
              <LayoutGrid className="h-4 w-4" />
              게시판
            </Link>
            {user && (
              <Link href="/mypage" className="flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-white hover:text-slate-900">
                <LibraryBig className="h-4 w-4" />
                마이페이지
              </Link>
            )}
          </nav>

          <div className="hidden items-center gap-3 md:flex">
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

          <button
            type="button"
            onClick={() => setIsMobileMenuOpen(true)}
            aria-label="메뉴 열기"
            aria-expanded={isMobileMenuOpen}
            className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-900 text-white transition hover:bg-slate-700 md:hidden"
          >
            <Menu className="h-5 w-5" />
          </button>
        </div>
      </header>

      <div
        className={`fixed inset-0 z-[60] bg-slate-950/35 transition-opacity duration-200 md:hidden ${isMobileMenuOpen ? "opacity-100" : "pointer-events-none opacity-0"}`}
        aria-hidden={!isMobileMenuOpen}
        onClick={closeMobileMenu}
      >
        <aside
          className={`ml-auto flex h-full w-[min(88%,22rem)] flex-col bg-[#F8FAFC] p-5 shadow-2xl transition-transform duration-200 ${isMobileMenuOpen ? "translate-x-0" : "translate-x-full"}`}
          aria-label="모바일 메뉴"
          onClick={(event) => event.stopPropagation()}
        >
          <div className="flex items-center justify-between">
            <p className="text-lg font-extrabold text-slate-900">메뉴</p>
            <button
              type="button"
              onClick={closeMobileMenu}
              aria-label="메뉴 닫기"
              className="flex h-11 w-11 items-center justify-center rounded-2xl text-slate-600 transition hover:bg-white hover:text-slate-900"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <nav className="mt-8 space-y-2">
            <Link href="/" onClick={closeMobileMenu} className="flex items-center gap-3 rounded-2xl px-4 py-4 text-base font-semibold text-slate-800 transition hover:bg-white">
              <PlusCircle className="h-5 w-5 text-blue-600" />
              퀴즈 만들기
            </Link>
            <Link
              href="/board"
              onPointerDown={prefetchDefaultBoardQuizzes}
              onFocus={prefetchDefaultBoardQuizzes}
              onClick={handleMobileBoardClick}
              className="flex items-center gap-3 rounded-2xl px-4 py-4 text-base font-semibold text-slate-800 transition hover:bg-white"
            >
              <LayoutGrid className="h-5 w-5 text-blue-600" />
              게시판
            </Link>
            {user && (
              <Link href="/mypage" onClick={closeMobileMenu} className="flex items-center gap-3 rounded-2xl px-4 py-4 text-base font-semibold text-slate-800 transition hover:bg-white">
                <LibraryBig className="h-5 w-5 text-blue-600" />
                마이페이지
              </Link>
            )}
          </nav>

          <div className="mt-auto border-t border-slate-200 pt-5">
            {user ? (
              <>
                <div className="mb-3 flex items-center gap-2 px-1 text-sm font-medium text-slate-600">
                  <UserCircle2 className="h-5 w-5 text-slate-400" />
                  <span className="truncate">{user.nickname || user.email}</span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    logout();
                    closeMobileMenu();
                  }}
                  className="flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-3.5 text-base font-semibold text-white transition hover:bg-slate-700"
                >
                  <LogOut className="h-5 w-5" />
                  로그아웃
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={openLoginModal}
                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-3.5 text-base font-semibold text-white transition hover:bg-slate-700"
              >
                <LogIn className="h-5 w-5" />
                로그인
              </button>
            )}
          </div>
        </aside>
      </div>

      <LoginModal
        open={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onSelectProvider={(provider) => startOAuthLogin(provider)}
      />
    </>
  );
}
