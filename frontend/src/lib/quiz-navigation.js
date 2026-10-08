export function hasQuestionAnswer(question, userAnswers) {
  // 선택한 답이 해당 문제의 선택지에 포함되는지 확인합니다.
  return question.options.includes(userAnswers[question.id]);
}

export function getQuizProgress(questions, userAnswers) {
  // 답변한 문제 수를 기준으로 진행률과 제출 가능 여부를 계산합니다.
  const answeredCount = questions.filter((question) => hasQuestionAnswer(question, userAnswers)).length;

  return {
    answeredCount,
    progress: questions.length > 0 ? Math.round((answeredCount / questions.length) * 100) : 0,
    isComplete: questions.length > 0 && answeredCount === questions.length,
  };
}

export function isValidQuestionIndex(index, questionCount) {
  // 이동할 문제 번호가 실제 문제 범위 안에 있는 정수인지 확인합니다.
  return Number.isInteger(index) && index >= 0 && index < questionCount;
}
