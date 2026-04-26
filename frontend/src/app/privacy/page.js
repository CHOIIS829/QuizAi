import Link from "next/link";

export const metadata = {
  title: "개인정보처리방침 | Quiz AI",
  description: "Quiz AI 서비스의 개인정보처리방침입니다.",
};

const EFFECTIVE_DATE = "2026년 4월 26일";
const CONTACT_EMAIL = "quizai829@gmail.com";

export default function PrivacyPage() {
  return (
    <main className="min-h-screen bg-[#F8FAFC] px-4 pb-20 pt-32 text-slate-900">
      <div className="mx-auto w-full max-w-4xl">
        <div className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm sm:p-12">
          <p className="text-sm font-semibold tracking-wide text-blue-600">Privacy Policy</p>
          <h1 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">개인정보처리방침</h1>
          <p className="mt-4 text-sm leading-relaxed text-slate-500">
            Quiz AI(이하 &quot;서비스&quot;)는 이용자의 개인정보를 중요하게 생각하며, 관련 법령을 준수합니다.
            본 방침은 서비스 이용 과정에서 처리되는 개인정보에 대해 안내합니다.
          </p>

          <div className="mt-8 space-y-8 text-sm leading-7 text-slate-700">
            <section>
              <h2 className="text-lg font-bold text-slate-900">1. 총칙</h2>
              <p className="mt-2">
                서비스는 최소한의 개인정보를 수집하며, 수집 목적 범위 내에서만 이용합니다. 이용자의 권리 보호를 위해
                안전한 관리 체계를 유지합니다.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-bold text-slate-900">2. 수집 항목</h2>
              <p className="mt-2">
                서비스는 Google OAuth 로그인 과정에서 다음 정보를 수집할 수 있습니다: 이메일 주소, 프로필 이미지,
                서비스 닉네임(이용자 설정), 로그인 이력(접속 시각).
              </p>
            </section>

            <section>
              <h2 className="text-lg font-bold text-slate-900">3. 수집 목적</h2>
              <p className="mt-2">
                수집한 개인정보는 회원 식별, 로그인 유지, 퀴즈 히스토리 제공, 고객 문의 대응, 서비스 개선 및 보안
                대응을 위해 사용합니다.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-bold text-slate-900">4. 보관 및 파기</h2>
              <p className="mt-2">
                개인정보는 수집 목적 달성 시 또는 이용자의 삭제 요청 시 지체 없이 파기합니다. 법령에 따라 일정 기간
                보관이 필요한 경우 해당 기간 동안 별도 보관 후 파기합니다.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-bold text-slate-900">5. 제3자 제공</h2>
              <p className="mt-2">
                서비스는 이용자의 동의 또는 법령상 근거가 있는 경우를 제외하고 개인정보를 외부에 제공하지 않습니다.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-bold text-slate-900">6. 처리 위탁</h2>
              <p className="mt-2">
                원활한 서비스 운영을 위해 클라우드 인프라, 인증(OAuth), 로그/모니터링 등 일부 업무를 외부 서비스에
                위탁할 수 있으며, 관련 법령에 따라 안전하게 관리합니다.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-bold text-slate-900">7. 이용자 권리</h2>
              <p className="mt-2">
                이용자는 언제든 본인의 개인정보 조회, 수정, 삭제를 요청할 수 있으며, 관련 문의는 아래 연락처로 접수할
                수 있습니다.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-bold text-slate-900">8. 문의처</h2>
              <p className="mt-2">
                개인정보 관련 문의:{" "}
                <a className="font-semibold text-blue-600 hover:text-blue-700" href={`mailto:${CONTACT_EMAIL}`}>
                  {CONTACT_EMAIL}
                </a>
              </p>
            </section>

            <section>
              <h2 className="text-lg font-bold text-slate-900">9. 시행일</h2>
              <p className="mt-2">본 개인정보처리방침은 {EFFECTIVE_DATE}부터 적용됩니다.</p>
            </section>
          </div>

          <div className="mt-10 border-t border-slate-200 pt-6">
            <Link href="/" className="text-sm font-semibold text-slate-600 hover:text-slate-900">
              홈으로 돌아가기
            </Link>
          </div>
        </div>
      </div>
    </main>
  );
}
