"use client";

import { SOURCE_TYPE_OPTIONS, TOPIC_TAG_OPTIONS } from "../lib/constants";

export default function FilterBar({ sourceType, tag, onChange }) {
  return (
    <div className="mb-8 grid gap-3 rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-2">
      <label className="flex flex-col gap-2 text-sm font-semibold text-slate-700">
        소스 유형
        <select
          value={sourceType}
          onChange={(event) => onChange({ sourceType: event.target.value, tag })}
          className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700 outline-none transition focus:border-blue-400"
        >
          {SOURCE_TYPE_OPTIONS.map((option) => (
            <option key={option.value || "all"} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>

      <label className="flex flex-col gap-2 text-sm font-semibold text-slate-700">
        주제 태그
        <select
          value={tag}
          onChange={(event) => onChange({ sourceType, tag: event.target.value })}
          className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700 outline-none transition focus:border-blue-400"
        >
          {TOPIC_TAG_OPTIONS.map((option) => (
            <option key={option.value || "all"} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
