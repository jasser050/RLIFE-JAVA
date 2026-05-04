import { useEffect } from "react";
import { Topbar } from "./components/Topbar";
import { SlideStrip } from "./components/SlideStrip";
import { Toolbar } from "./components/Toolbar";
import { Canvas } from "./components/Canvas";
import { RightPanel } from "./components/RightPanel";
import { useStudio } from "./lib/store";

export default function PresentationStudio() {
  const { selectedId, deleteElement, duplicateElement, selectElement } = useStudio();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA" || (e.target as HTMLElement)?.isContentEditable) return;
      if (!selectedId) return;
      if (e.key === "Delete" || e.key === "Backspace") deleteElement(selectedId);
      if ((e.metaKey || e.ctrlKey) && e.key === "d") { e.preventDefault(); duplicateElement(selectedId); }
      if (e.key === "Escape") selectElement(null);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [selectedId, deleteElement, duplicateElement, selectElement]);

  return (
    <div className="flex flex-col h-screen overflow-hidden" style={{ background: "hsl(var(--studio-bg))" }}>
      <Topbar />
      <div className="flex flex-1 overflow-hidden">
        <SlideStrip />
        <div className="flex-1 flex flex-col overflow-hidden">
          <Toolbar />
          <Canvas />
        </div>
        <RightPanel />
      </div>
    </div>
  );
}