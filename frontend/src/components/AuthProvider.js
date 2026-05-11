"use client";

import { createContext, useContext, useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { fetchJson } from "../lib/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const pathname = usePathname();
  const initializedRef = useRef(false);
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshUser = async () => {
    try {
      const response = await fetchJson("/api/auth/me", {
        headers: {},
      });
      const nextUser = response?.data ?? null;
      setUser(nextUser);
      return nextUser;
    } catch {
      setUser(null);
      return null;
    } finally {
      setIsLoading(false);
    }
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
      setIsLoading(false);
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
