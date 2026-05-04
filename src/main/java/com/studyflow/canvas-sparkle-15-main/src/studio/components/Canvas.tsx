import { useEffect, useRef, useState } from "react";
import { useStudio, useActiveSlide } from "../lib/store";
import { CanvasElementView } from "./CanvasElement";
import { ChevronLeft, ChevronRight } from "lucide-react";

const CANVAS_W = 1280;
const CANVAS_H = 720;

export const Canvas = () => {
  const slide = useActiveSlide();
  const { selectedId, selectElement, zoom, showGrid, slides, setActive } = useStudio();
  const hostRef = useRef<HTMLDivElement>(null);
  const [autoScale, setAutoScale] = useState(0.6);

  useEffect(() => {
    const el = hostRef.current;
    if (!el) return;
    const update = () => {
      const r = el.getBoundingClientRect();
      const pad = 64;
      const s = Math.min((r.width - pad) / CANVAS_W, (r.height - pad) / CANVAS_H);
      setAutoScale(Math.max(0.1, s));
    };
    update();
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const scale = autoScale * zoom;
  const idx = slides.findIndex(s => s.id === slide.id);

  return (
    <div ref={hostRef} className="flex-1 relative overflow-hidden flex items-center justify-center"
      style={{ background: "hsl(var(--studio-bg))" }}
      onClick={() => selectElement(null)}>
      <button onClick={() => idx > 0 && setActive(slides[idx-1].id)} disabled={idx === 0}
        className="absolute left-4 z-10 h-10 w-10 rounded-full flex items-center justify-center transition disabled:opacity-30"
        style={{ background: "hsl(var(--studio-card))", color: "hsl(var(--studio-text-pri))",
          border: "1px solid hsl(var(--studio-border))" }}>
        <ChevronLeft className="h-5 w-5" />
      </button>
      <button onClick={() => idx < slides.length - 1 && setActive(slides[idx+1].id)} disabled={idx === slides.length-1}
        className="absolute right-4 z-10 h-10 w-10 rounded-full flex items-center justify-center transition disabled:opacity-30"
        style={{ background: "hsl(var(--studio-card))", color: "hsl(var(--studio-text-pri))",
          border: "1px solid hsl(var(--studio-border))" }}>
        <ChevronRight className="h-5 w-5" />
      </button>

      <div
        onClick={(e) => e.stopPropagation()}
        className={`relative shadow-2xl ${showGrid ? "studio-canvas-grid" : ""}`}
        style={{
          width: CANVAS_W,
          height: CANVAS_H,
          background: slide.background,
          borderRadius: 14,
          transform: `scale(${scale})`,
          transformOrigin: "center center",
          boxShadow: "0 30px 80px -20px rgba(0,0,0,0.6)",
        }}>
        {slide.elements.map(el => (
          <CanvasElementView key={el.id} el={el} selected={el.id === selectedId} />
        ))}
      </div>

      <div className="absolute bottom-3 left-1/2 -translate-x-1/2 px-3 py-1.5 rounded-full text-[11px] font-bold flex items-center gap-2"
        style={{ background: "hsl(var(--studio-card))", color: "hsl(var(--studio-text-sec))",
          border: "1px solid hsl(var(--studio-border))" }}>
        <span style={{ color: "hsl(var(--studio-accent-hot))" }}>{idx + 1}</span>
        <span>/</span>
        <span>{slides.length}</span>
        <span className="opacity-50">·</span>
        <span>{Math.round(scale * 100)}%</span>
      </div>
    </div>
  );
};