import { Sparkles, Download, Play, Undo2, Redo2, Hexagon, Share2 } from "lucide-react";
import { useStudio } from "../lib/store";
import { THEMES } from "../lib/themes";

export const Topbar = () => {
  const { themeId, setTheme } = useStudio();
  return (
    <div className="h-14 flex items-center px-4 gap-3 border-b" style={{
      background: "hsl(var(--studio-surface))",
      borderColor: "hsl(var(--studio-border))",
    }}>
      <div className="flex items-center gap-2 pr-3 border-r" style={{ borderColor: "hsl(var(--studio-border))" }}>
        <Hexagon className="h-6 w-6" style={{ color: "hsl(var(--studio-accent))" }} fill="currentColor" />
        <span className="font-bold text-sm tracking-tight" style={{ color: "hsl(var(--studio-text-pri))" }}>Studio</span>
      </div>

      <div className="flex items-center gap-1">
        <IconBtn title="Undo"><Undo2 className="h-4 w-4" /></IconBtn>
        <IconBtn title="Redo"><Redo2 className="h-4 w-4" /></IconBtn>
      </div>

      <div className="h-6 w-px" style={{ background: "hsl(var(--studio-border))" }} />

      <div className="flex items-center gap-1.5 overflow-x-auto studio-scroll">
        {THEMES.map(t => (
          <button key={t.id} onClick={() => setTheme(t.id)}
            className="group flex items-center gap-2 px-2 py-1 rounded-md transition-all"
            style={{
              background: themeId === t.id ? "hsl(var(--studio-accent-dim))" : "transparent",
              border: `1px solid ${themeId === t.id ? "hsl(var(--studio-accent))" : "transparent"}`,
            }}
            title={t.name}
          >
            <span className="h-5 w-5 rounded shadow-inner border" style={{
              background: `linear-gradient(135deg, ${t.preview[0]}, ${t.preview[1]})`,
              borderColor: "rgba(255,255,255,0.1)",
            }} />
            <span className="text-xs font-semibold whitespace-nowrap" style={{
              color: themeId === t.id ? "hsl(var(--studio-text-pri))" : "hsl(var(--studio-text-sec))",
            }}>{t.name}</span>
          </button>
        ))}
      </div>

      <div className="ml-auto flex items-center gap-2">
        <IconBtn title="Share"><Share2 className="h-4 w-4" /></IconBtn>
        <button className="flex items-center gap-2 px-3 h-9 rounded-lg text-xs font-bold transition"
          style={{ background: "hsl(var(--studio-card))", color: "hsl(var(--studio-text-pri))",
            border: "1px solid hsl(var(--studio-border))" }}>
          <Play className="h-3.5 w-3.5" /> Present
        </button>
        <button className="flex items-center gap-2 px-3 h-9 rounded-lg text-xs font-bold transition"
          style={{ background: "hsl(var(--studio-card))", color: "hsl(var(--studio-text-pri))",
            border: "1px solid hsl(var(--studio-border))" }}>
          <Download className="h-3.5 w-3.5" /> Export
        </button>
        <button className="flex items-center gap-2 px-3.5 h-9 rounded-lg text-xs font-bold text-white transition shadow-lg"
          style={{ background: "linear-gradient(135deg, hsl(var(--studio-accent)), hsl(var(--studio-accent-hot)))" }}>
          <Sparkles className="h-3.5 w-3.5" /> Generate AI
        </button>
      </div>
    </div>
  );
};

const IconBtn = ({ children, title }: { children: React.ReactNode; title: string }) => (
  <button title={title} className="h-8 w-8 rounded-md flex items-center justify-center transition hover:bg-white/5"
    style={{ color: "hsl(var(--studio-text-sec))" }}>
    {children}
  </button>
);