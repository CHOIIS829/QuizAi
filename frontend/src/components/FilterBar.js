"use client";

import { SOURCE_TYPE_OPTIONS, TOPIC_TAG_OPTIONS } from "../lib/constants";
import FilterSelect from "./FilterSelect";

export default function FilterBar({ sourceType, tag, onChange }) {
  // 소스 유형과 주제 태그 필터를 동일한 선택 UI로 제공한다.
  // 소스 유형 변경 값을 기존 필터 객체 형태로 전달한다.
  const handleSourceTypeChange = (nextSourceType) => {
    onChange({ sourceType: nextSourceType, tag });
  };

  // 주제 태그 변경 값을 기존 필터 객체 형태로 전달한다.
  const handleTagChange = (nextTag) => {
    onChange({ sourceType, tag: nextTag });
  };

  return (
    <div className="mb-8 grid gap-3 rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-2">
      <FilterSelect label="소스 유형" value={sourceType} options={SOURCE_TYPE_OPTIONS} onChange={handleSourceTypeChange} />
      <FilterSelect label="주제 태그" value={tag} options={TOPIC_TAG_OPTIONS} onChange={handleTagChange} />
    </div>
  );
}
