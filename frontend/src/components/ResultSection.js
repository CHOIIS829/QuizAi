
export default function ResultSection({ quizData, userAnswers, onRetry }) {
    if (!quizData || !quizData.questions) return null;

    const correctCount = quizData.questions.filter(
        q => userAnswers[q.id] === q.answer
    ).length;

    return (
        <div className="w-full max-w-2xl pb-20">

            <div className="bg-white rounded-3xl p-8 mb-8 text-center shadow-lg border border-slate-100">
                <h2 className="text-2xl font-bold text-slate-900 mb-2">학습 완료! 🎉</h2>
                <p className="text-slate-500">
                    총 {quizData.questions.length}문제 중 <span className="text-blue-600 font-bold text-xl">{correctCount}</span>문제를 맞추셨습니다.
                </p>
            </div>

            <div className="space-y-8">
                {quizData.questions.map((q, idx) => {
                    const isCorrect = userAnswers[q.id] === q.answer;
                    return (
                        <div key={q.id} className={`bg-white rounded-3xl p-8 shadow-sm border-2 ${isCorrect ? 'border-transparent' : 'border-red-100'}`}>
                            <div className="flex items-center gap-3 mb-4">
                                <span className={`flex items-center justify-center w-8 h-8 rounded-full font-bold text-sm ${isCorrect ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'
                                    }`}>
                                    {isCorrect ? 'O' : 'X'}
                                </span>
                                <span className="text-slate-400 font-medium">Question {idx + 1}</span>
                            </div>

                            <h3 className="text-lg font-bold text-slate-900 mb-4">{q.question}</h3>

                            {q.codeSnippet && (
                                <pre className="bg-slate-900 text-slate-50 p-4 rounded-xl mb-6 overflow-x-auto text-sm font-mono whitespace-pre-wrap">
                                    {q.codeSnippet}
                                </pre>
                            )}

                            <div className="space-y-2 mb-6">
                                {q.options.map((option, optIdx) => {
                                    const isSelected = userAnswers[q.id] === option;
                                    const isAnswer = q.answer === option;
                                    let style = "border-slate-100 text-slate-500";

                                    if (isAnswer) style = "border-green-500 bg-green-50 text-green-700 font-medium";
                                    else if (isSelected && !isCorrect) style = "border-red-500 bg-red-50 text-red-700";

                                    return (
                                        <div key={optIdx} className={`p-4 rounded-xl border ${style} flex justify-between items-center`}>
                                            <span>{option}</span>
                                            {isAnswer && <span className="text-xs bg-green-200 text-green-800 px-2 py-0.5 rounded">정답</span>}
                                            {isSelected && !isCorrect && <span className="text-xs bg-red-200 text-red-800 px-2 py-0.5 rounded">내 답</span>}
                                        </div>
                                    );
                                })}
                            </div>

                            <div className="bg-slate-50 p-5 rounded-xl text-sm leading-relaxed text-slate-700">
                                <span className="font-bold block mb-1">📝 해설</span>
                                {q.explanation}
                            </div>
                        </div>
                    );
                })}
            </div>

            <div className="fixed bottom-8 left-1/2 -translate-x-1/2">
                <button
                    onClick={onRetry}
                    className="px-8 py-4 bg-[#0F172A] text-white rounded-full font-bold shadow-2xl hover:bg-[#1E293B] transition-transform hover:scale-105 active:scale-95"
                >
                    다른 문제 만들기
                </button>
            </div>
        </div>
    );
}
