import { BrainCircuit } from "lucide-react";

export default function Header() {
    return (
        <header className="fixed top-0 left-0 w-full p-6 flex justify-between items-center z-50 bg-[#F8FAFC]/80 backdrop-blur-sm">
            <div className="flex items-center gap-2">
                <div className="w-10 h-10 bg-blue-100 rounded-xl flex items-center justify-center shadow-sm">
                    <BrainCircuit className="w-6 h-6 text-blue-600" />
                </div>
                <span className="text-xl font-extrabold tracking-tight text-slate-900">Quiz AI</span>
            </div>
        </header>
    );
}
