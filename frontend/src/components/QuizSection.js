import { ArrowLeft, ArrowRight } from "lucide-react";
import { getQuizProgress, hasQuestionAnswer } from "../lib/quiz-navigation";

export default function QuizSection({
    quizData,
    currentQuestionIndex,
    userAnswers,
    onOptionSelect,
    onQuestionChange,
    onNext,
    onRetry
}) {
    // 답안을 유지하며 문제를 자유롭게 이동할 수 있는 공통 풀이 화면을 표시합니다.
    if (!quizData || !quizData.questions || quizData.questions.length === 0) {
        return <div className="p-8 text-center text-red-500">데이터 오류: 문제를 불러올 수 없습니다.</div>;
    }

    const question = quizData.questions[currentQuestionIndex];
    if (!question) {
        return <div className="p-8 text-center text-red-500">문제 인덱스 오류입니다.</div>;
    }

    const { answeredCount, progress, isComplete } = getQuizProgress(quizData.questions, userAnswers);
    const isLastQuestion = currentQuestionIndex === quizData.questions.length - 1;
    const remainingCount = quizData.questions.length - answeredCount;

    return (
        <div className="mx-auto w-full max-w-4xl">
            <div className="flex justify-end mb-4">
                <button
                    type="button"
                    onClick={onRetry}
                    className="flex items-center gap-2 text-slate-500 hover:text-slate-900 transition-colors font-medium px-3 py-2 rounded-lg hover:bg-slate-100"
                >
                    <ArrowLeft className="w-5 h-5" />
                    <span>돌아가기</span>
                </button>
            </div>

            <div className="mb-6 flex items-center justify-between text-slate-500 text-sm font-medium">
                <span>답변 완료 {answeredCount} / {quizData.questions.length} · {progress}%</span>
                <span>현재 문제 {currentQuestionIndex + 1} / {quizData.questions.length}</span>
            </div>

            {/* 답변 완료 수를 기준으로 진행률을 표시합니다. */}
            <div
                role="progressbar"
                aria-label="답변 완료율"
                aria-valuenow={progress}
                aria-valuemin={0}
                aria-valuemax={100}
                className="w-full h-2 bg-slate-100 rounded-full mb-6 overflow-hidden"
            >
                <div
                    className="h-full bg-blue-500 transition-all duration-500 ease-out"
                    style={{ width: `${progress}%` }}
                />
            </div>

            <nav aria-label="퀴즈 문제 이동" className="mb-6 rounded-3xl border border-slate-100 bg-white p-5 shadow-sm sm:p-6">
                <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                    <div>
                        <h2 className="font-bold text-slate-900">문제 이동</h2>
                        <p className="mt-1 text-sm leading-relaxed text-slate-500">
                            번호를 누르면 해당 문제로 이동합니다. 선택한 답은 유지됩니다.
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-3 text-xs text-slate-500">
                        <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded-full bg-blue-500" />현재 문제</span>
                        <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded-full border border-blue-200 bg-blue-100" />답변 완료</span>
                        <span className="flex items-center gap-1.5"><span className="h-3 w-3 rounded-full border border-slate-200 bg-white" />미응답</span>
                    </div>
                </div>
                <div className="grid grid-cols-[repeat(auto-fit,minmax(2.75rem,1fr))] gap-2">
                    {quizData.questions.map((item, index) => {
                        const isCurrent = index === currentQuestionIndex;
                        const isAnswered = hasQuestionAnswer(item, userAnswers);

                        return (
                            <button
                                key={item.id}
                                type="button"
                                onClick={() => onQuestionChange(index)}
                                aria-current={isCurrent ? "step" : undefined}
                                aria-label={`${index + 1}번 문제, ${isAnswered ? "답변 완료" : "미응답"}${isCurrent ? ", 현재 문제" : ""}`}
                                className={`min-h-11 rounded-xl border text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-500 ${isCurrent
                                    ? "border-blue-500 bg-blue-500 text-white"
                                    : isAnswered
                                        ? "border-blue-100 bg-blue-50 text-blue-600 hover:bg-blue-100"
                                        : "border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:bg-slate-50"
                                    }`}
                            >
                                {index + 1}
                            </button>
                        );
                    })}
                </div>
            </nav>

            <div className="bg-white rounded-3xl shadow-xl shadow-slate-200/60 p-5 sm:p-8 border border-slate-100">
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
                            type="button"
                            onClick={() => onOptionSelect(question.id, option)}
                            aria-pressed={userAnswers[question.id] === option}
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

                <div className="mt-8 flex flex-wrap items-center justify-between gap-3">
                    <button
                        type="button"
                        onClick={() => onQuestionChange(currentQuestionIndex - 1)}
                        disabled={currentQuestionIndex === 0}
                        className="flex min-h-11 items-center gap-2 rounded-xl border border-slate-300 px-4 py-3 font-semibold text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50 sm:px-6"
                    >
                        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
                        이전 문제
                    </button>
                    <p id="quiz-navigation-help" role="status" className="order-last w-full text-center text-sm leading-relaxed text-slate-500 lg:order-none lg:w-auto lg:flex-1">
                        {isLastQuestion && !isComplete
                            ? `아직 ${remainingCount}문제가 남았어요. 모든 문제에 답하면 제출할 수 있어요.`
                            : "이전 문제로 돌아가 답을 수정할 수 있어요."}
                    </p>
                    <button
                        type="button"
                        onClick={onNext}
                        disabled={isLastQuestion && !isComplete}
                        aria-describedby="quiz-navigation-help"
                        className="flex min-h-11 items-center gap-2 px-4 sm:px-6 py-3 bg-[#0F172A] text-white rounded-xl font-semibold hover:bg-[#1E293B] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {isLastQuestion ? '제출하기' : '다음 문제'}
                        {!isLastQuestion && <ArrowRight className="h-4 w-4" aria-hidden="true" />}
                    </button>
                </div>
            </div>
        </div>
    );
}
