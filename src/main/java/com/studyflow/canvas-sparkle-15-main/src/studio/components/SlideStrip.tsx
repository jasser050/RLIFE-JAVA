import { Plus, Copy, Trash2, GripVertical } from "lucide-react";
import { useStudio } from "../lib/store";

export const SlideStrip = () => {
  const { slides, activeSlideId, setActive, addSlide, duplicateSlide, deleteSlide, reorderSlide } = useStudio();

  return (
    <aside className="w-[200px] shrink-0 flex flex-col border-r" style={{
      background: "hsl(var(--studio-surface))",
      borderColor: "hsl(var(--studio-border))",
    }}>
      <div className="p-3 border-b" style={{ borderColor: "hsl(var(--studio-border))" }}>
        <button onClick={addSlide}
          className="w-full h-9 rounded-lg flex items-center justify-center gap-2 text-xs font-bold transition"
          style={{
            background: "hsl(var(--studio-accent-dim))",
            color: "hsl(var(--studio-accent-hot))",
            border: "1px dashed hsl(var(--studio-accent) / 0.4)",
          }}>
          <Plus className="h-4 w-4" /> Add Slide
        </button>
      </div>

      <div className="flex-1 overflow-y-auto studio-scroll p-3 space-y-3">
        {slides.map((sl, idx) => {
          const active = sl.id === activeSlideId;
          return (
            <div key={sl.id}
              draggable
              onDragStart={(e) => e.dataTransfer.setData("text/plain", String(idx))}
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                const from = Number(e.dataTransfer.getData("text/plain"));
                if (!Number.isNaN(from) && from !== idx) reorderSlide(from, idx);
              }}
              onClick={() => setActive(sl.id)}
              className="group rounded-xl p-2 cursor-pointer transition-all"
              style={{
                background: active ? "hsl(var(--studio-card))" : "transparent",
                border: `1.5px solid ${active ? "hsl(var(--studio-accent))" : "hsl(var(--studio-border))"}`,
                boxShadow: active ? "0 8px 24px -8px hsl(var(--studio-accent) / 0.4)" : "none",
              }}>
              <div className="flex items-center justify-between mb-1.5 px-0.5">
                <div className="flex items-center gap-1">
                  <GripVertical className="h-3 w-3 opacity-30" style={{ color: "hsl(var(--studio-text-sec))" }} />
                  <span className="text-[10px] font-bold" style={{
                    color: active ? "hsl(var(--studio-accent-hot))" : "hsl(var(--studio-text-mut))",
                  }}>{idx + 1}</span>
                </div>
                <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition">
                  <button onClick={(e) => { e.stopPropagation(); duplicateSlide(sl.id); }}
                    className="h-5 w-5 rounded flex items-center justify-center hover:bg-white/10"
                    style={{ color: "hsl(var(--studio-text-sec))" }}>
                    <Copy className="h-3 w-3" />
                  </button>
                  {slides.length > 1 && (
                    <button onClick={(e) => { e.stopPropagation(); deleteSlide(sl.id); }}
                      className="h-5 w-5 rounded flex items-center justify-center hover:bg-white/10"
                      style={{ color: "hsl(var(--studio-danger))" }}>
                      <Trash2 className="h-3 w-3" />
                    </button>
                  )}
                </div>
              </div>

              {/* Mini preview */}
              <div className="aspect-video rounded-md relative overflow-hidden" style={{ background: sl.background }}>
                {sl.elements.slice(0, 6).map(el => (
                  <div key={el.id} className="absolute" style={{
                    left: `${(el.x / 1280) * 100}%`,
                    top: `${(el.y / 720) * 100}%`,
                    width: `${(el.width / 1280) * 100}%`,
                    height: `${(el.height / 720) * 100}%`,
                    background: el.type === "text" ? "transparent" : el.useGradient
                      ? `linear-gradient(${el.gradientAngle}deg, ${el.gradientA}, ${el.gradientB})`
                      : el.fill,
                    borderRadius: el.type === "circle" ? "999px" : `${Math.max(2, el.borderRadius / 4)}px`,
                    opacity: el.opacity * 0.9,
                  }} />
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </aside>
  );
};