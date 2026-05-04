import { create } from "zustand";
import type { CanvasEl, ElementType, Slide } from "./types";
import { THEMES, PRESENTATION_TEMPLATES } from "./themes";

const uid = () => Math.random().toString(36).slice(2, 10);

export const defaultElement = (type: ElementType, x = 80, y = 80): CanvasEl => {
  const base: CanvasEl = {
    id: uid(), type, x, y, width: 240, height: 140, rotation: 0, opacity: 1,
    locked: false, fill: "#7F77DD", stroke: "transparent", strokeWidth: 0,
    borderRadius: 12, useGradient: false, gradientA: "#7F77DD", gradientB: "#EC4899",
    gradientAngle: 135, shadow: false, shadowBlur: 24, shadowColor: "#000000",
    shadowOffsetY: 8, text: "", fontFamily: "Inter", fontSize: 32, fontWeight: 600,
    italic: false, underline: false, strikethrough: false, textAlign: "left",
    letterSpacing: 0, lineHeight: 1.3, textColor: "#FFFFFF", textBg: "transparent",
    imageUrl: "", iconName: "Star", iconSize: 64, sticker: "✨",
  };
  switch (type) {
    case "text": return { ...base, text: "Double-click to edit", width: 360, height: 80, fill: "transparent", textColor: "#F0F6FC" };
    case "rect": return { ...base, width: 220, height: 140 };
    case "circle": return { ...base, width: 160, height: 160, borderRadius: 999 };
    case "line": return { ...base, height: 0, width: 240, stroke: "#7F77DD", strokeWidth: 4, fill: "transparent" };
    case "image": return { ...base, width: 320, height: 200, fill: "rgba(127,119,221,0.1)" };
    case "icon": return { ...base, width: 96, height: 96, fill: "transparent", textColor: "#7F77DD" };
    case "sticker": return { ...base, width: 140, height: 140, fill: "transparent" };
  }
};

const blankSlide = (n: number, theme = THEMES[0]): Slide => ({
  id: uid(), title: `Slide ${n}`, background: theme.bg, elements: [], notes: "",
});

interface State {
  slides: Slide[];
  activeSlideId: string;
  selectedId: string | null;
  themeId: string;
  zoom: number;
  showGrid: boolean;
  // actions
  setActive: (id: string) => void;
  addSlide: () => void;
  duplicateSlide: (id: string) => void;
  deleteSlide: (id: string) => void;
  reorderSlide: (from: number, to: number) => void;
  setTheme: (id: string) => void;
  setSlideBg: (bg: string) => void;
  setZoom: (z: number) => void;
  toggleGrid: () => void;
  // elements
  addElement: (type: ElementType, partial?: Partial<CanvasEl>) => void;
  updateElement: (id: string, patch: Partial<CanvasEl>) => void;
  deleteElement: (id: string) => void;
  duplicateElement: (id: string) => void;
  selectElement: (id: string | null) => void;
  bringForward: (id: string) => void;
  sendBackward: (id: string) => void;
  bringToFront: (id: string) => void;
  sendToBack: (id: string) => void;
  setSlideTitle: (t: string) => void;
  setSlideNotes: (n: string) => void;
  applyTemplate: (templateId: string) => void;
}

const first = blankSlide(1);

export const useStudio = create<State>((set, get) => ({
  slides: [first],
  activeSlideId: first.id,
  selectedId: null,
  themeId: "midnight",
  zoom: 1,
  showGrid: true,

  setActive: (id) => set({ activeSlideId: id, selectedId: null }),
  addSlide: () => set((s) => {
    const t = THEMES.find(t => t.id === s.themeId) ?? THEMES[0];
    const n = blankSlide(s.slides.length + 1, t);
    return { slides: [...s.slides, n], activeSlideId: n.id, selectedId: null };
  }),
  duplicateSlide: (id) => set((s) => {
    const idx = s.slides.findIndex(x => x.id === id);
    if (idx < 0) return s;
    const src = s.slides[idx];
    const dup: Slide = { ...src, id: uid(), title: src.title + " copy",
      elements: src.elements.map(e => ({ ...e, id: uid() })) };
    const next = [...s.slides];
    next.splice(idx + 1, 0, dup);
    return { slides: next, activeSlideId: dup.id, selectedId: null };
  }),
  deleteSlide: (id) => set((s) => {
    if (s.slides.length === 1) return s;
    const next = s.slides.filter(x => x.id !== id);
    const active = s.activeSlideId === id ? next[0].id : s.activeSlideId;
    return { slides: next, activeSlideId: active, selectedId: null };
  }),
  reorderSlide: (from, to) => set((s) => {
    const next = [...s.slides];
    const [m] = next.splice(from, 1);
    next.splice(to, 0, m);
    return { slides: next };
  }),

  setTheme: (id) => set((s) => {
    const t = THEMES.find(x => x.id === id) ?? THEMES[0];
    return {
      themeId: id,
      slides: s.slides.map(sl => ({ ...sl, background: t.bg })),
    };
  }),
  setSlideBg: (bg) => set((s) => ({
    slides: s.slides.map(sl => sl.id === s.activeSlideId ? { ...sl, background: bg } : sl),
  })),
  setZoom: (z) => set({ zoom: Math.max(0.25, Math.min(2, z)) }),
  toggleGrid: () => set((s) => ({ showGrid: !s.showGrid })),

  addElement: (type, partial) => {
    const el = { ...defaultElement(type), ...partial };
    set((s) => ({
      slides: s.slides.map(sl => sl.id === s.activeSlideId
        ? { ...sl, elements: [...sl.elements, el] } : sl),
      selectedId: el.id,
    }));
  },
  updateElement: (id, patch) => set((s) => ({
    slides: s.slides.map(sl => sl.id === s.activeSlideId
      ? { ...sl, elements: sl.elements.map(e => e.id === id ? { ...e, ...patch } : e) }
      : sl),
  })),
  deleteElement: (id) => set((s) => ({
    slides: s.slides.map(sl => sl.id === s.activeSlideId
      ? { ...sl, elements: sl.elements.filter(e => e.id !== id) } : sl),
    selectedId: s.selectedId === id ? null : s.selectedId,
  })),
  duplicateElement: (id) => set((s) => {
    const sl = s.slides.find(x => x.id === s.activeSlideId);
    if (!sl) return s;
    const src = sl.elements.find(e => e.id === id);
    if (!src) return s;
    const dup = { ...src, id: uid(), x: src.x + 24, y: src.y + 24 };
    return {
      slides: s.slides.map(x => x.id === sl.id
        ? { ...x, elements: [...x.elements, dup] } : x),
      selectedId: dup.id,
    };
  }),
  selectElement: (id) => set({ selectedId: id }),

  bringForward: (id) => set((s) => mutateOrder(s, id, "forward")),
  sendBackward: (id) => set((s) => mutateOrder(s, id, "backward")),
  bringToFront: (id) => set((s) => mutateOrder(s, id, "front")),
  sendToBack: (id) => set((s) => mutateOrder(s, id, "back")),

  setSlideTitle: (t) => set((s) => ({
    slides: s.slides.map(sl => sl.id === s.activeSlideId ? { ...sl, title: t } : sl),
  })),
  setSlideNotes: (n) => set((s) => ({
    slides: s.slides.map(sl => sl.id === s.activeSlideId ? { ...sl, notes: n } : sl),
  })),

  applyTemplate: (templateId) => set((s) => {
    const tpl = PRESENTATION_TEMPLATES.find(t => t.id === templateId);
    if (!tpl) return s;
    const theme = THEMES.find(t => t.id === tpl.themeId) ?? THEMES[0];
    const elements = tpl.build(uid);
    return {
      themeId: tpl.themeId,
      slides: s.slides.map(sl => sl.id === s.activeSlideId
        ? { ...sl, elements, background: theme.bg } : sl),
      selectedId: null,
    };
  }),
}));

function mutateOrder(s: State, id: string, op: "forward"|"backward"|"front"|"back"): Partial<State> {
  return {
    slides: s.slides.map(sl => {
      if (sl.id !== s.activeSlideId) return sl;
      const i = sl.elements.findIndex(e => e.id === id);
      if (i < 0) return sl;
      const arr = [...sl.elements];
      const [e] = arr.splice(i, 1);
      if (op === "front") arr.push(e);
      else if (op === "back") arr.unshift(e);
      else if (op === "forward") arr.splice(Math.min(i + 1, arr.length), 0, e);
      else arr.splice(Math.max(i - 1, 0), 0, e);
      return { ...sl, elements: arr };
    }),
  };
}

export const useActiveSlide = () => {
  return useStudio((s) => s.slides.find(sl => sl.id === s.activeSlideId) ?? s.slides[0]);
};
export const useSelected = () => {
  return useStudio((s) => {
    const sl = s.slides.find(x => x.id === s.activeSlideId);
    return sl?.elements.find(e => e.id === s.selectedId) ?? null;
  });
};