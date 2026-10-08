"use client";

import { createContext, useContext, useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { fetchJson } from "../lib/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // 인증 정보를 공유하고 로그인 콜백의 로딩 상태는 최초 렌더링에서 결정합니다.
  const pathname = usePathname();
  const initializedRef = useRef(false);
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(pathname !== "/auth/callback");

  const refreshUser = () => {
    // 인증 조회가 끝난 뒤 사용자와 로딩 상태를 함께 갱신합니다.
    return fetchJson("/api/auth/me", {
      headers: {},
    })
      .then((response) => response?.data ?? null)
      .catch(() => null)
      .then((nextUser) => {
        setUser(nextUser);
        setIsLoading(false);
        return nextUser;
      });
  };

  const logout = async () => {
    await fetchJson("/api/auth/logout", {
      method: "POST",
      body: JSON.stringify({}),
    }).catch(() => null);
    setUser(null);
  };

  useEffect(() => {
    if (initializedRef.current) {
      return;
    }
    initializedRef.current = true;

    if (pathname === "/auth/callback") {
      return;
    }

    refreshUser();
  }, [pathname]);

  return (
    <AuthContext.Provider value={{ user, isLoading, refreshUser, logout, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
