import { fetchJson } from "./api";

const DEFAULT_BOARD_FILTERS = Object.freeze({ sourceType: "", tag: "", page: 0, size: 12 });
const DEFAULT_BOARD_CACHE_TTL_MS = 5_000;

let defaultBoardRequest = null;
let defaultBoardRequestExpiresAt = 0;

export function buildBoardQuizzesPath(filters = DEFAULT_BOARD_FILTERS) {
  // 게시판 필터와 페이지 정보를 목록 API 요청 경로로 변환한다.
  const searchParams = new URLSearchParams();
  if (filters.sourceType) searchParams.set("sourceType", filters.sourceType);
  if (filters.tag) searchParams.set("tag", filters.tag);
  searchParams.set("page", String(filters.page ?? DEFAULT_BOARD_FILTERS.page));
  searchParams.set("size", String(filters.size ?? DEFAULT_BOARD_FILTERS.size));

  return `/api/board/quizzes?${searchParams.toString()}`;
}

function isDefaultBoardRequest(filters) {
  // 전달된 조회 조건이 게시판 최초 진입 시 사용하는 기본 조건인지 확인한다.
  return !filters.sourceType
    && !filters.tag
    && (filters.page ?? DEFAULT_BOARD_FILTERS.page) === DEFAULT_BOARD_FILTERS.page
    && (filters.size ?? DEFAULT_BOARD_FILTERS.size) === DEFAULT_BOARD_FILTERS.size;
}

export function fetchBoardQuizzes(filters = DEFAULT_BOARD_FILTERS) {
  // 기본 목록 요청은 짧게 재사용해 화면 전환과 API 호출을 병렬로 처리한다.
  const requestPath = buildBoardQuizzesPath(filters);
  if (!isDefaultBoardRequest(filters)) {
    return fetchJson(requestPath);
  }

  const now = Date.now();
  if (defaultBoardRequest && now < defaultBoardRequestExpiresAt) {
    return defaultBoardRequest;
  }

  const request = fetchJson(requestPath).catch((error) => {
    if (defaultBoardRequest === request) {
      defaultBoardRequest = null;
      defaultBoardRequestExpiresAt = 0;
    }
    throw error;
  });

  defaultBoardRequest = request;
  defaultBoardRequestExpiresAt = now + DEFAULT_BOARD_CACHE_TTL_MS;
  return request;
}

export function prefetchDefaultBoardQuizzes() {
  // 게시판 이동 전에 기본 목록 요청을 시작하고 실제 오류 처리는 게시판 화면에 맡긴다.
  void fetchBoardQuizzes().catch(() => undefined);
}
