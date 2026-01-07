import { ArrowLeft } from "lucide-react";

export default function QuizSection({
    quizData,
    currentQuestionIndex,
    userAnswers,
    onOptionSelect,
    onNext,
    onRetry
}) {
    if (!quizData || !quizData.questions || quizData.questions.length === 0) {
        return <div className="p-8 text-center text-red-500">데이터 오류: 문제를 불러올 수 없습니다.</div>;
    }

    const question = quizData.questions[currentQuestionIndex];
    if (!question) {
        return <div className="p-8 text-center text-red-500">문제 인덱스 오류입니다.</div>;
    }

    const progress = Math.round(((currentQuestionIndex + 1) / quizData.questions.length) * 100);

    return (
        <div className="w-full max-w-2xl">
            <div className="flex justify-end mb-4">
                <button
                    onClick={onRetry}
                    className="flex items-center gap-2 text-slate-500 hover:text-slate-900 transition-colors font-medium px-3 py-2 rounded-lg hover:bg-slate-100"
                >
                    <ArrowLeft className="w-5 h-5" />
                    <span>돌아가기</span>
                </button>
            </div>

            <div className="mb-6 flex items-center justify-between text-slate-500 text-sm font-medium">
                <span>진행률 {progress}%</span>
                <span>{currentQuestionIndex + 1} / {quizData.questions.length}</span>
            </div>

            {/* Progress Bar */}
            <div className="w-full h-2 bg-slate-100 rounded-full mb-8 overflow-hidden">
                <div
                    className="h-full bg-blue-500 transition-all duration-500 ease-out"
                    style={{ width: `${progress}%` }}
                />
            </div>

            <div className="bg-white rounded-3xl shadow-xl shadow-slate-200/60 p-8 border border-slate-100">
                <span className="inline-block px-3 py-1 bg-blue-50 text-blue-600 rounded-full text-xs font-bold mb-4">
                    Q{currentQuestionIndex + 1}
                </span>
                <h2 className="text-xl font-bold text-slate-900 mb-6 leading-relaxed">
                    {question.question}
                </h2>

                {question.codeSnippet && (
                    <pre className="bg-slate-900 text-slate-50 p-6 rounded-xl mb-8 overflow-x-auto text-sm font-mono leading-relaxed whitespace-pre-wrap">
                        {question.codeSnippet}
                    </pre>
                )}

                <div className="space-y-3">
                    {question.options.map((option, idx) => (
                        <button
                            key={idx}
                            onClick={() => onOptionSelect(question.id, option)}
                            className={`w-full text-left p-4 rounded-xl border-2 transition-all duration-200 ${userAnswers[question.id] === option
                                ? "border-blue-500 bg-blue-50 text-blue-700"
                                : "border-slate-100 hover:border-blue-200 hover:bg-slate-50 text-slate-600"
                                }`}
                        >
                            <div className="flex items-center gap-3">
                                <div className={`w-6 h-6 rounded-full border flex items-center justify-center text-xs ${userAnswers[question.id] === option
                                    ? "border-blue-500 bg-blue-500 text-white"
                                    : "border-slate-300 text-slate-400"
                                    }`}>
                                    {String.fromCharCode(65 + idx)}
                                </div>
                                {option}
                            </div>
                        </button>
                    ))}
                </div>

                <div className="mt-8 flex justify-end">
                    <button
                        onClick={onNext}
                        disabled={!userAnswers[question.id]}
                        className="px-8 py-3 bg-[#0F172A] text-white rounded-xl font-semibold hover:bg-[#1E293B] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {currentQuestionIndex === quizData.questions.length - 1 ? '제출하기' : '다음 문제'}
                    </button>
                </div>
            </div>
        </div>
    );
}
