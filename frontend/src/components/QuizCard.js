"use client";

import Link from "next/link";
import { ArrowRight, BookCopy, Link2 } from "lucide-react";

export default function QuizCard({ quiz }) {
  return (
    <Link
      href={`/quizzes?id=${quiz.id}`}
      className="group block rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg"
    >
      <div className="mb-4 flex items-center justify-between gap-3">
        <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-blue-600">
          {quiz.sourceType}
        </span>
        <span className="text-xs font-medium text-slate-400">{quiz.questionCount}문제</span>
      </div>

      <h3 className="line-clamp-2 text-xl font-bold leading-snug text-slate-900">{quiz.title}</h3>
      <p className="mt-3 flex items-center gap-2 text-sm text-slate-500">
        <BookCopy className="h-4 w-4" />
        작성자 {quiz.authorNickname || "닉네임 미설정"}
      </p>
      <p className="mt-2 flex items-center gap-2 text-sm text-slate-500">
        <Link2 className="h-4 w-4" />
        {quiz.sourceHost}
      </p>

      <div className="mt-5 flex flex-wrap gap-2">
        {quiz.topicTags.map((tag) => (
          <span key={tag.slug} className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
            #{tag.displayName}
          </span>
        ))}
      </div>

      <div className="mt-6 flex items-center justify-end text-sm font-semibold text-slate-900">
        다시 풀기
        <ArrowRight className="ml-1 h-4 w-4 transition group-hover:translate-x-0.5" />
      </div>
    </Link>
  );
}
