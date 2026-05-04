import { useState } from "react";
import {
  Type, Palette, Layers, Settings2, Bold, Italic, Underline, Strikethrough,
  AlignLeft, AlignCenter, AlignRight, Trash2, Copy, Lock, Unlock,
  ChevronUp, ChevronDown, ChevronsUp, ChevronsDown, Eye, EyeOff,
  Sparkles, Image as ImageIcon, Sun, Layout,
} from "lucide-react";
import { useStudio, useActiveSlide, useSelected } from "../lib/store";
import { COLOR_SWATCHES, FONT_FAMILIES, FONT_SIZES, GRADIENT_PRESETS, THEMES } from "../lib/themes";

type Tab = "design" | "element" | "layers";

export const RightPanel = () => {
  const [tab, setTab] = useState<Tab>("design");
  const selected = useSelected();

  // auto-switch to element tab if you select something
  if (selected && tab === "design") {/* keep, manual switch */}

  return (
    <aside className="w-[300px] shrink-0 flex flex-col border-l" style={{
      background: "hsl(var(--studio-surface))",
      borderColor: "hsl(var(--studio-border))",
    }}>
      <div className="flex border-b" style={{ borderColor: "hsl(var(--studio-border))" }}>
        <TabBtn active={tab === "design"} onClick={() => setTab("design")} icon={Palette} label="Design" />
        <TabBtn active={tab === "element"} onClick={() => setTab("element")} icon={Settings2} label="Element" />
        <TabBtn active={tab === "layers"} onClick={() => setTab("layers")} icon={Layers} label="Layers" />
      </div>

      <div className="flex-1 overflow-y-auto studio-scroll">
        {tab === "design" && <DesignPanel />}
        {tab === "element" && (selected ? <ElementPanel /> : <EmptyHint />)}
        {tab === "layers" && <LayersPanel />}
      </div>
    </aside>
  );
};

// ─────────── Tabs ───────────
const TabBtn = ({ active, onClick, icon: Icon, label }: any) => (
  <button onClick={onClick}
    className="flex-1 h-11 flex items-center justify-center gap-1.5 text-[11px] font-bold transition"
    style={{
      color: active ? "hsl(var(--studio-accent-hot))" : "hsl(var(--studio-text-sec))",
      background: active ? "hsl(var(--studio-accent-dim))" : "transparent",
      borderBottom: `2px solid ${active ? "hsl(var(--studio-accent))" : "transparent"}`,
    }}>
    <Icon className="h-3.5 w-3.5" /> {label}
  </button>
);

const Section = ({ icon: Icon, title, children }: any) => (
  <div className="p-4 border-b" style={{ borderColor: "hsl(var(--studio-border))" }}>
    <div className="flex items-center gap-1.5 mb-3">
      <Icon className="h-3.5 w-3.5" style={{ color: "hsl(var(--studio-accent))" }} />
      <h4 className="text-[10px] font-bold uppercase tracking-wider"
        style={{ color: "hsl(var(--studio-text-sec))" }}>{title}</h4>
    </div>
    {children}
  </div>
);

const Field = ({ label, children }: any) => (
  <div className="mb-3 last:mb-0">
    <label className="block text-[10px] font-semibold mb-1.5"
      style={{ color: "hsl(var(--studio-text-mut))" }}>{label}</label>
    {children}
  </div>
);

const inputCls = "w-full h-8 px-2 rounded-md text-xs outline-none transition";
const inputStyle: React.CSSProperties = {
  background: "hsl(var(--studio-card))",
  color: "hsl(var(--studio-text-pri))",
  border: "1px solid hsl(var(--studio-border))",
};

// ─────────── Design (Slide) ───────────
const DesignPanel = () => {
  const slide = useActiveSlide();
  const { setSlideBg, setSlideTitle, setSlideNotes, themeId, setTheme } = useStudio();

  return (
    <>
      <Section icon={Sparkles} title="Theme">
        <div className="grid grid-cols-3 gap-2">
          {THEMES.map(t => (
            <button key={t.id} onClick={() => setTheme(t.id)}
              className="aspect-video rounded-lg overflow-hidden relative transition"
              style={{
                background: t.bg,
                border: `2px solid ${themeId === t.id ? "hsl(var(--studio-accent-hot))" : "transparent"}`,
              }}>
              <span className="absolute bottom-1 left-1 right-1 text-[8px] font-bold text-white truncate text-center"
                style={{ textShadow: "0 1px 2px rgba(0,0,0,0.6)" }}>{t.name}</span>
            </button>
          ))}
        </div>
      </Section>

      <Section icon={Sun} title="Background">
        <Field label="Solid color">
          <SwatchGrid value={slide.background} onChange={setSlideBg} />
        </Field>
        <Field label="Gradient presets">
          <div className="grid grid-cols-4 gap-1.5">
            {GRADIENT_PRESETS.map((g, i) => (
              <button key={i}
                onClick={() => setSlideBg(`linear-gradient(135deg, ${g[0]}, ${g[1]})`)}
                className="h-8 rounded-md transition hover:scale-105"
                style={{ background: `linear-gradient(135deg, ${g[0]}, ${g[1]})` }} />
            ))}
          </div>
        </Field>
      </Section>

      <Section icon={Layout} title="Slide info">
        <Field label="Title">
          <input className={inputCls} style={inputStyle} value={slide.title}
            onChange={(e) => setSlideTitle(e.target.value)} />
        </Field>
        <Field label="Speaker notes">
          <textarea rows={4} className={inputCls + " resize-none py-2 h-auto"} style={inputStyle}
            value={slide.notes} onChange={(e) => setSlideNotes(e.target.value)}
            placeholder="Notes for the speaker…" />
        </Field>
      </Section>
    </>
  );
};

const SwatchGrid = ({ value, onChange }: { value: string; onChange: (c: string) => void }) => (
  <div className="grid grid-cols-8 gap-1">
    {COLOR_SWATCHES.map(c => (
      <button key={c} onClick={() => onChange(c)}
        className="aspect-square rounded transition hover:scale-110"
        style={{
          background: c,
          border: value === c ? "2px solid hsl(var(--studio-accent-hot))" : "1px solid hsl(var(--studio-border))",
        }} />
    ))}
  </div>
);

// ─────────── Element ───────────
const EmptyHint = () => (
  <div className="p-8 text-center">
    <div className="h-12 w-12 mx-auto mb-3 rounded-full flex items-center justify-center"
      style={{ background: "hsl(var(--studio-accent-dim))" }}>
      <Settings2 className="h-5 w-5" style={{ color: "hsl(var(--studio-accent))" }} />
    </div>
    <p className="text-xs font-semibold mb-1" style={{ color: "hsl(var(--studio-text-pri))" }}>
      No element selected
    </p>
    <p className="text-[11px]" style={{ color: "hsl(var(--studio-text-sec))" }}>
      Pick something on the canvas to edit its style.
    </p>
  </div>
);

const ElementPanel = () => {
  const el = useSelected()!;
  const { updateElement, deleteElement, duplicateElement,
    bringForward, sendBackward, bringToFront, sendToBack } = useStudio();
  const set = (p: any) => updateElement(el.id, p);

  return (
    <>
      {/* Quick actions */}
      <div className="flex items-center gap-1 p-2 border-b" style={{ borderColor: "hsl(var(--studio-border))" }}>
        <ActionBtn icon={Copy} title="Duplicate" onClick={() => duplicateElement(el.id)} />
        <ActionBtn icon={el.locked ? Lock : Unlock} title={el.locked ? "Unlock" : "Lock"}
          onClick={() => set({ locked: !el.locked })} />
        <ActionBtn icon={ChevronsUp} title="To front" onClick={() => bringToFront(el.id)} />
        <ActionBtn icon={ChevronUp} title="Forward" onClick={() => bringForward(el.id)} />
        <ActionBtn icon={ChevronDown} title="Backward" onClick={() => sendBackward(el.id)} />
        <ActionBtn icon={ChevronsDown} title="To back" onClick={() => sendToBack(el.id)} />
        <div className="ml-auto" />
        <ActionBtn icon={Trash2} title="Delete" danger onClick={() => deleteElement(el.id)} />
      </div>

      {/* Position & size */}
      <Section icon={Settings2} title="Position & Size">
        <div className="grid grid-cols-2 gap-2">
          <NumField label="X" value={el.x} onChange={(v) => set({ x: v })} />
          <NumField label="Y" value={el.y} onChange={(v) => set({ y: v })} />
          <NumField label="W" value={el.width} onChange={(v) => set({ width: v })} />
          <NumField label="H" value={el.height} onChange={(v) => set({ height: v })} />
        </div>
        <Field label={`Rotation: ${el.rotation}°`}>
          <input type="range" min={-180} max={180} value={el.rotation}
            onChange={(e) => set({ rotation: +e.target.value })} className="w-full accent-current"
            style={{ accentColor: "hsl(var(--studio-accent))" }} />
        </Field>
        <Field label={`Opacity: ${Math.round(el.opacity * 100)}%`}>
          <input type="range" min={0} max={1} step={0.01} value={el.opacity}
            onChange={(e) => set({ opacity: +e.target.value })} className="w-full"
            style={{ accentColor: "hsl(var(--studio-accent))" }} />
        </Field>
      </Section>

      {/* Text */}
      {el.type === "text" && (
        <Section icon={Type} title="Typography">
          <Field label="Text">
            <textarea rows={3} value={el.text}
              onChange={(e) => set({ text: e.target.value })}
              className={inputCls + " resize-none py-2 h-auto"} style={inputStyle} />
          </Field>
          <div className="grid grid-cols-2 gap-2 mb-3">
            <div>
              <label className="block text-[10px] font-semibold mb-1.5" style={{ color: "hsl(var(--studio-text-mut))" }}>Font</label>
              <select value={el.fontFamily} onChange={(e) => set({ fontFamily: e.target.value })}
                className={inputCls} style={inputStyle}>
                {FONT_FAMILIES.map(f => <option key={f}>{f}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-[10px] font-semibold mb-1.5" style={{ color: "hsl(var(--studio-text-mut))" }}>Size</label>
              <select value={el.fontSize} onChange={(e) => set({ fontSize: +e.target.value })}
                className={inputCls} style={inputStyle}>
                {FONT_SIZES.map(s => <option key={s}>{s}</option>)}
              </select>
            </div>
          </div>
          <Field label={`Weight: ${el.fontWeight}`}>
            <input type="range" min={100} max={900} step={100} value={el.fontWeight}
              onChange={(e) => set({ fontWeight: +e.target.value })}
              className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
          </Field>
          <div className="flex items-center gap-1 mb-3">
            <ToggleBtn active={el.fontWeight >= 700} icon={Bold} onClick={() => set({ fontWeight: el.fontWeight >= 700 ? 400 : 700 })} />
            <ToggleBtn active={el.italic} icon={Italic} onClick={() => set({ italic: !el.italic })} />
            <ToggleBtn active={el.underline} icon={Underline} onClick={() => set({ underline: !el.underline })} />
            <ToggleBtn active={el.strikethrough} icon={Strikethrough} onClick={() => set({ strikethrough: !el.strikethrough })} />
            <div className="w-px h-5 mx-1" style={{ background: "hsl(var(--studio-border))" }} />
            <ToggleBtn active={el.textAlign === "left"} icon={AlignLeft} onClick={() => set({ textAlign: "left" })} />
            <ToggleBtn active={el.textAlign === "center"} icon={AlignCenter} onClick={() => set({ textAlign: "center" })} />
            <ToggleBtn active={el.textAlign === "right"} icon={AlignRight} onClick={() => set({ textAlign: "right" })} />
          </div>
          <Field label="Text color">
            <SwatchGrid value={el.textColor} onChange={(c) => set({ textColor: c })} />
          </Field>
          <Field label={`Letter spacing: ${el.letterSpacing}px`}>
            <input type="range" min={-2} max={20} step={0.5} value={el.letterSpacing}
              onChange={(e) => set({ letterSpacing: +e.target.value })}
              className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
          </Field>
          <Field label={`Line height: ${el.lineHeight}`}>
            <input type="range" min={0.8} max={3} step={0.05} value={el.lineHeight}
              onChange={(e) => set({ lineHeight: +e.target.value })}
              className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
          </Field>
        </Section>
      )}

      {/* Fill */}
      {(el.type === "rect" || el.type === "circle") && (
        <Section icon={Palette} title="Fill">
          <div className="flex gap-1 mb-3 p-1 rounded-md" style={{ background: "hsl(var(--studio-card))" }}>
            <ChipBtn active={!el.useGradient} onClick={() => set({ useGradient: false })}>Solid</ChipBtn>
            <ChipBtn active={el.useGradient} onClick={() => set({ useGradient: true })}>Gradient</ChipBtn>
          </div>
          {!el.useGradient ? (
            <SwatchGrid value={el.fill} onChange={(c) => set({ fill: c })} />
          ) : (
            <>
              <Field label="Color A">
                <SwatchGrid value={el.gradientA} onChange={(c) => set({ gradientA: c })} />
              </Field>
              <Field label="Color B">
                <SwatchGrid value={el.gradientB} onChange={(c) => set({ gradientB: c })} />
              </Field>
              <Field label={`Angle: ${el.gradientAngle}°`}>
                <input type="range" min={0} max={360} value={el.gradientAngle}
                  onChange={(e) => set({ gradientAngle: +e.target.value })}
                  className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
              </Field>
              <Field label="Presets">
                <div className="grid grid-cols-4 gap-1.5">
                  {GRADIENT_PRESETS.map((g, i) => (
                    <button key={i} onClick={() => set({ gradientA: g[0], gradientB: g[1] })}
                      className="h-8 rounded-md transition hover:scale-105"
                      style={{ background: `linear-gradient(135deg, ${g[0]}, ${g[1]})` }} />
                  ))}
                </div>
              </Field>
            </>
          )}
        </Section>
      )}

      {/* Stroke / radius */}
      {(el.type === "rect" || el.type === "circle" || el.type === "line") && (
        <Section icon={Settings2} title="Border">
          <Field label="Stroke color">
            <SwatchGrid value={el.stroke} onChange={(c) => set({ stroke: c })} />
          </Field>
          <Field label={`Stroke width: ${el.strokeWidth}px`}>
            <input type="range" min={0} max={20} value={el.strokeWidth}
              onChange={(e) => set({ strokeWidth: +e.target.value })}
              className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
          </Field>
          {el.type === "rect" && (
            <Field label={`Border radius: ${el.borderRadius}px`}>
              <input type="range" min={0} max={120} value={el.borderRadius}
                onChange={(e) => set({ borderRadius: +e.target.value })}
                className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
            </Field>
          )}
        </Section>
      )}

      {/* Image */}
      {el.type === "image" && (
        <Section icon={ImageIcon} title="Image">
          <Field label="Image URL">
            <input className={inputCls} style={inputStyle} value={el.imageUrl}
              onChange={(e) => set({ imageUrl: e.target.value })}
              placeholder="https://…" />
          </Field>
          <Field label={`Border radius: ${el.borderRadius}px`}>
            <input type="range" min={0} max={200} value={el.borderRadius}
              onChange={(e) => set({ borderRadius: +e.target.value })}
              className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
          </Field>
        </Section>
      )}

      {/* Icon */}
      {el.type === "icon" && (
        <Section icon={Sparkles} title="Icon">
          <Field label={`Size: ${el.iconSize}px`}>
            <input type="range" min={16} max={300} value={el.iconSize}
              onChange={(e) => set({ iconSize: +e.target.value })}
              className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
          </Field>
          <Field label="Color">
            <SwatchGrid value={el.textColor} onChange={(c) => set({ textColor: c })} />
          </Field>
        </Section>
      )}

      {/* Shadow */}
      <Section icon={Sun} title="Shadow">
        <label className="flex items-center gap-2 cursor-pointer mb-3">
          <input type="checkbox" checked={el.shadow}
            onChange={(e) => set({ shadow: e.target.checked })} />
          <span className="text-xs font-semibold" style={{ color: "hsl(var(--studio-text-pri))" }}>
            Enable drop shadow
          </span>
        </label>
        {el.shadow && (
          <>
            <Field label={`Blur: ${el.shadowBlur}px`}>
              <input type="range" min={0} max={80} value={el.shadowBlur}
                onChange={(e) => set({ shadowBlur: +e.target.value })}
                className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
            </Field>
            <Field label={`Offset Y: ${el.shadowOffsetY}px`}>
              <input type="range" min={-40} max={40} value={el.shadowOffsetY}
                onChange={(e) => set({ shadowOffsetY: +e.target.value })}
                className="w-full" style={{ accentColor: "hsl(var(--studio-accent))" }} />
            </Field>
            <Field label="Color">
              <SwatchGrid value={el.shadowColor} onChange={(c) => set({ shadowColor: c })} />
            </Field>
          </>
        )}
      </Section>
    </>
  );
};

// ─────────── Layers ───────────
const LayersPanel = () => {
  const slide = useActiveSlide();
  const { selectedId, selectElement, updateElement, deleteElement } = useStudio();
  const reversed = [...slide.elements].reverse();

  if (reversed.length === 0) {
    return (
      <div className="p-8 text-center text-xs" style={{ color: "hsl(var(--studio-text-sec))" }}>
        No layers yet. Add elements from the toolbar.
      </div>
    );
  }

  return (
    <div className="p-2">
      {reversed.map((el) => {
        const active = el.id === selectedId;
        const name = el.type === "text" ? (el.text.slice(0, 20) || "Text") :
          el.type === "icon" ? `Icon: ${el.iconName}` :
          el.type[0].toUpperCase() + el.type.slice(1);
        return (
          <div key={el.id}
            onClick={() => selectElement(el.id)}
            className="group flex items-center gap-2 px-2 py-1.5 rounded-md cursor-pointer transition mb-0.5"
            style={{
              background: active ? "hsl(var(--studio-accent-dim))" : "transparent",
              border: `1px solid ${active ? "hsl(var(--studio-accent))" : "transparent"}`,
            }}>
            <span className="text-[10px] font-bold w-4 text-center"
              style={{ color: "hsl(var(--studio-accent-hot))" }}>{layerIcon(el.type)}</span>
            <span className="text-[11px] font-semibold flex-1 truncate"
              style={{ color: "hsl(var(--studio-text-pri))" }}>{name}</span>
            <button onClick={(e) => { e.stopPropagation(); updateElement(el.id, { locked: !el.locked }); }}
              className="h-5 w-5 rounded flex items-center justify-center opacity-0 group-hover:opacity-100"
              style={{ color: "hsl(var(--studio-text-sec))" }}>
              {el.locked ? <Lock className="h-3 w-3" /> : <Unlock className="h-3 w-3" />}
            </button>
            <button onClick={(e) => { e.stopPropagation(); deleteElement(el.id); }}
              className="h-5 w-5 rounded flex items-center justify-center opacity-0 group-hover:opacity-100"
              style={{ color: "hsl(var(--studio-danger))" }}>
              <Trash2 className="h-3 w-3" />
            </button>
          </div>
        );
      })}
    </div>
  );
};

const layerIcon = (t: string) => ({
  text: "T", rect: "▢", circle: "●", line: "—", image: "▣", icon: "★",
}[t] ?? "•");

// ─────────── helpers ───────────
const ActionBtn = ({ icon: Icon, title, onClick, danger }: any) => (
  <button onClick={onClick} title={title}
    className="h-7 w-7 rounded-md flex items-center justify-center transition hover:bg-white/10"
    style={{ color: danger ? "hsl(var(--studio-danger))" : "hsl(var(--studio-text-sec))" }}>
    <Icon className="h-3.5 w-3.5" />
  </button>
);

const ToggleBtn = ({ active, icon: Icon, onClick }: any) => (
  <button onClick={onClick}
    className="h-7 w-7 rounded-md flex items-center justify-center transition"
    style={{
      background: active ? "hsl(var(--studio-accent-dim))" : "transparent",
      color: active ? "hsl(var(--studio-accent-hot))" : "hsl(var(--studio-text-sec))",
      border: `1px solid ${active ? "hsl(var(--studio-accent))" : "transparent"}`,
    }}>
    <Icon className="h-3.5 w-3.5" />
  </button>
);

const ChipBtn = ({ active, onClick, children }: any) => (
  <button onClick={onClick}
    className="flex-1 h-7 rounded text-[11px] font-bold transition"
    style={{
      background: active ? "hsl(var(--studio-accent))" : "transparent",
      color: active ? "white" : "hsl(var(--studio-text-sec))",
    }}>{children}</button>
);

const NumField = ({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) => (
  <div>
    <label className="block text-[10px] font-semibold mb-1.5" style={{ color: "hsl(var(--studio-text-mut))" }}>{label}</label>
    <input type="number" value={Math.round(value)} onChange={(e) => onChange(+e.target.value)}
      className={inputCls} style={inputStyle} />
  </div>
);