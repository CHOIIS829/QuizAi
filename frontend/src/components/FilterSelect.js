"use client";

import { Check, ChevronDown } from "lucide-react";
import { useEffect, useId, useRef, useState } from "react";

export default function FilterSelect({ label, value, options, onChange }) {
  // 브라우저와 화면 크기에 관계없이 같은 위치에 필터 선택 목록을 표시한다.
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef(null);
  const listboxId = useId();
  const selectedOption = options.find((option) => option.value === value) || options[0];

  useEffect(() => {
    if (!isOpen) return undefined;

    // 드롭다운 바깥을 클릭하면 열려 있는 선택 목록을 닫는다.
    const handlePointerDown = (event) => {
      if (!containerRef.current?.contains(event.target)) setIsOpen(false);
    };

    // ESC 키로 선택 목록을 닫을 수 있게 한다.
    const handleKeyDown = (event) => {
      if (event.key === "Escape") setIsOpen(false);
    };

    document.addEventListener("pointerdown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  // 선택한 값을 부모 필터에 전달한 뒤 선택 목록을 닫는다.
  const handleSelect = (optionValue) => {
    onChange(optionValue);
    setIsOpen(false);
  };

  return (
    <div ref={containerRef} className="relative">
      <p className="mb-2 text-sm font-semibold text-slate-700">{label}</p>
      <button
        type="button"
        onClick={() => setIsOpen((current) => !current)}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-controls={listboxId}
        aria-label={`${label}: ${selectedOption.label}`}
        className={`flex w-full items-center justify-between rounded-2xl border bg-slate-50 px-4 py-3 text-left text-sm font-medium text-slate-700 outline-none transition ${
          isOpen ? "border-blue-400 ring-2 ring-blue-100" : "border-slate-200 hover:border-slate-300"
        }`}
      >
        <span>{selectedOption.label}</span>
        <ChevronDown className={`h-4 w-4 transition-transform ${isOpen ? "rotate-180 text-blue-600" : "text-slate-500"}`} />
      </button>

      {isOpen && (
        <div
          id={listboxId}
          role="listbox"
          aria-label={label}
          className="absolute left-0 top-full z-30 mt-2 max-h-64 w-full overflow-y-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-xl"
        >
          {options.map((option) => {
            const isSelected = option.value === value;

            return (
              <button
                key={option.value || "all"}
                type="button"
                role="option"
                aria-selected={isSelected}
                onClick={() => handleSelect(option.value)}
                className={`flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-sm font-medium transition ${
                  isSelected ? "bg-blue-50 text-blue-700" : "text-slate-700 hover:bg-slate-50"
                }`}
              >
                <span>{option.label}</span>
                {isSelected && <Check className="h-4 w-4" />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
