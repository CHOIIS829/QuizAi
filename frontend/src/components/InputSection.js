import { BookOpen, Play, BrainCircuit, AlertCircle } from "lucide-react";

export default function InputSection({ url, setUrl, quizCount, setQuizCount, isLoading, onGenerate }) {
    return (
        <div className="w-full max-w-[580px]">
            <div className="text-center space-y-6 mb-12">
                <div className="flex flex-col items-center gap-4 mb-8">
                    <div className="w-16 h-16 bg-blue-100 rounded-2xl flex items-center justify-center shadow-sm">
                        <BrainCircuit className="w-10 h-10 text-blue-600" />
                    </div>
                    <span className="text-3xl font-extrabold tracking-tight text-slate-900">Quiz AI</span>
                </div>
                <h1 className="text-4xl md:text-5xl font-bold tracking-tight text-slate-900">
                    어떤 걸 학습하고 계신가요?
                </h1>
                <p className="text-lg text-slate-500 max-w-xl mx-auto">
                    유튜브 영상이나 기술 블로그 URL만 넣으세요.
                    <br className="hidden md:block" />
                    AI가 핵심 내용을 분석해 모의고사를 만들어드립니다.
                </p>
            </div>

            <div className="bg-white rounded-3xl shadow-xl shadow-slate-200/60 p-8 border border-slate-100">
                <div className="space-y-2 mb-8">
                    <label className="text-sm font-semibold text-slate-700 ml-1">학습 자료 URL</label>
                    <div className="relative group">
                        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                            <BookOpen className="h-5 w-5 text-slate-400 group-focus-within:text-blue-500 transition-colors" />
                        </div>
                        <input
                            type="text"
                            className="block w-full rounded-xl border-0 py-4 pl-12 pr-4 text-slate-900 shadow-sm ring-1 ring-inset ring-slate-200 placeholder:text-slate-400 focus:ring-2 focus:ring-inset focus:ring-blue-500 sm:text-base bg-white transition-all outline-none"
                            placeholder="https://youtube.com/watch?v=... 또는 블로그 주소"
                            value={url}
                            onChange={(e) => setUrl(e.target.value)}
                        />
                    </div>
                </div>

                <div className="space-y-3 mb-8">
                    <label className="text-sm font-semibold text-slate-700 ml-1">문제 개수 설정</label>
                    <div className="grid grid-cols-4 gap-3">
                        {[3, 5, 10, 20].map((count) => (
                            <button
                                key={count}
                                onClick={() => setQuizCount(count)}
                                className={`py-3 rounded-xl text-sm font-medium transition-all duration-200 ${quizCount === count
                                    ? "bg-blue-600 text-white shadow-md shadow-blue-200 scale-105"
                                    : "bg-slate-50 text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                                    }`}
                            >
                                {count}문제
                            </button>
                        ))}
                    </div>
                </div>

                <button
                    onClick={onGenerate}
                    disabled={isLoading}
                    className="w-full bg-[#0F172A] hover:bg-[#1E293B] text-white rounded-xl py-4 font-semibold text-lg shadow-lg shadow-slate-900/20 transition-all active:scale-[0.98] flex items-center justify-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
                >
                    {isLoading ? (
                        <>
                            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                            <span>분석 중...</span>
                        </>
                    ) : (
                        <>
                            <Play className="w-5 h-5 fill-current" />
                            <span>문제 생성하기</span>
                        </>
                    )}
                </button>

                <div className="mt-6 p-4 bg-amber-50 rounded-2xl border border-amber-100 flex gap-3 text-left animate-in fade-in slide-in-from-bottom-2 duration-500">
                    <div className="shrink-0 mt-0.5">
                        <AlertCircle className="w-5 h-5 text-amber-500" />
                    </div>
                    <div className="text-sm text-amber-900 leading-relaxed">
                        <p className="font-bold mb-1">안내사항</p>
                        <p className="text-amber-700">
                            영상 길이에 따라 최대 2분까지 소요될 수 있습니다.
                            <br />
                            너무 긴 영상의 경우 분석이 실패할 수 있습니다.
                        </p>
                    </div>
                </div>

                <div className="mt-6 text-center">
                    <p className="text-xs font-medium text-slate-400 flex items-center justify-center gap-1.5">
                        <BrainCircuit className="w-3.5 h-3.5" />
                        Powered by Google Gemini 1.5 Pro
                    </p>
                </div>
            </div>
        </div>
    );
}
