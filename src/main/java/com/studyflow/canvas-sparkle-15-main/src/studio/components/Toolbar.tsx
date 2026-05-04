import { useState } from "react";
import {
  MousePointer2, Type, Square, Circle, Minus, Image as ImageIcon, Star,
  Heading1, Heading2, AlignLeft, List, Grid3x3, ZoomIn, ZoomOut, Layers,
  Palette, Smile, ChevronDown, Sticker, LayoutTemplate, Upload,
} from "lucide-react";
import { useStudio } from "../lib/store";
import { ICON_LIBRARY, STICKER_LIBRARY, PRESENTATION_TEMPLATES, THEMES } from "../lib/themes";
import * as LucideIcons from "lucide-react";
import { useRef } from "react";

export const Toolbar = () => {
  const { addElement, zoom, setZoom, showGrid, toggleGrid, applyTemplate } = useStudio();
  const [iconOpen, setIconOpen] = useState(false);
  const [stickerOpen, setStickerOpen] = useState(false);
  const [tplOpen, setTplOpen] = useState(false);
  const uploadRef = useRef<HTMLInputElement>(null);

  const handleUpload = (file?: File) => {
    if (!file) return;
    const r = new FileReader();
    r.onload = () => addElement("image", { imageUrl: r.result as string, width: 480, height: 320 });
    r.readAsDataURL(file);
  };

  return (
    <div className="h-12 flex items-center px-3 gap-1 border-b" style={{
      background: "hsl(var(--studio-surface))",
      borderColor: "hsl(var(--studio-border))",
    }}>
      <Section label="Tools">
        <Tool icon={MousePointer2} title="Select" onClick={() => useStudio.setState({ selectedId: null })} />
        <Tool icon={Type} title="Text" onClick={() => addElement("text")} />
      </Section>

      <Divider />

      <Section label="Shapes">
        <Tool icon={Square} title="Rectangle" onClick={() => addElement("rect")} />
        <Tool icon={Circle} title="Circle" onClick={() => addElement("circle")} />
        <Tool icon={Minus} title="Line" onClick={() => addElement("line")} />
      </Section>

      <Divider />

      <Section label="Media">
        <Tool icon={ImageIcon} title="Image placeholder" onClick={() => addElement("image")} />
        <input ref={uploadRef} type="file" accept="image/*" className="hidden"
          onChange={(e) => handleUpload(e.target.files?.[0])} />
        <Tool icon={Upload} title="Upload image" onClick={() => uploadRef.current?.click()} />
        <div className="relative">
          <Tool icon={Smile} title="Icon" onClick={() => setIconOpen(o => !o)} />
          {iconOpen && (
            <div className="absolute z-50 top-full mt-2 left-0 w-72 max-h-80 overflow-y-auto studio-scroll p-3 rounded-xl shadow-2xl"
              style={{ background: "hsl(var(--studio-card))", border: "1px solid hsl(var(--studio-border))" }}>
              <div className="text-[10px] uppercase font-bold mb-2 tracking-wider"
                style={{ color: "hsl(var(--studio-text-sec))" }}>Pick an icon</div>
              <div className="grid grid-cols-7 gap-1">
                {ICON_LIBRARY.map((name) => {
                  const Icon = (LucideIcons as any)[name];
                  if (!Icon) return null;
                  return (
                    <button key={name}
                      onClick={() => { addElement("icon", { iconName: name }); setIconOpen(false); }}
                      className="h-8 w-8 rounded-md flex items-center justify-center hover:bg-white/10 transition"
                      style={{ color: "hsl(var(--studio-text-pri))" }} title={name}>
                      <Icon className="h-4 w-4" />
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
        <div className="relative">
          <Tool icon={Sticker} title="Stickers / 3D" onClick={() => setStickerOpen(o => !o)} />
          {stickerOpen && (
            <div className="absolute z-50 top-full mt-2 left-0 w-80 max-h-80 overflow-y-auto studio-scroll p-3 rounded-xl shadow-2xl"
              style={{ background: "hsl(var(--studio-card))", border: "1px solid hsl(var(--studio-border))" }}>
              <div className="text-[10px] uppercase font-bold mb-2 tracking-wider"
                style={{ color: "hsl(var(--studio-text-sec))" }}>Stickers & 3D objects</div>
              <div className="grid grid-cols-8 gap-1">
                {STICKER_LIBRARY.map((s, i) => (
                  <button key={i} onClick={() => { addElement("sticker", { sticker: s }); setStickerOpen(false); }}
                    className="h-9 w-9 rounded-md flex items-center justify-center hover:bg-white/10 transition text-xl">
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </Section>

      <Divider />

      <Section label="Templates">
        <div className="relative">
          <button onClick={() => setTplOpen(o => !o)}
            className="h-8 px-2 rounded-md flex items-center gap-1.5 text-[11px] font-bold transition hover:bg-white/10"
            style={{ color: "hsl(var(--studio-text-pri))" }} title="Slide templates">
            <LayoutTemplate className="h-3.5 w-3.5" /> Templates <ChevronDown className="h-3 w-3 opacity-60" />
          </button>
          {tplOpen && (
            <div className="absolute z-50 top-full mt-2 left-0 w-[420px] max-h-[420px] overflow-y-auto studio-scroll p-3 rounded-xl shadow-2xl"
              style={{ background: "hsl(var(--studio-card))", border: "1px solid hsl(var(--studio-border))" }}>
              <div className="text-[10px] uppercase font-bold mb-2 tracking-wider"
                style={{ color: "hsl(var(--studio-text-sec))" }}>Apply a template to current slide</div>
              <div className="grid grid-cols-2 gap-2">
                {PRESENTATION_TEMPLATES.map(t => {
                  const theme = THEMES.find(x => x.id === t.themeId) ?? THEMES[0];
                  return (
                    <button key={t.id}
                      onClick={() => { applyTemplate(t.id); setTplOpen(false); }}
                      className="group relative aspect-video rounded-lg overflow-hidden transition hover:scale-[1.02]"
                      style={{ background: theme.bg, border: "1px solid hsl(var(--studio-border))" }}>
                      <div className="absolute inset-0 flex items-end p-2">
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded backdrop-blur-sm"
                          style={{ background: "rgba(0,0,0,0.55)", color: "#fff" }}>{t.name}</span>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </Section>

      <Divider />

      <Section label="Quick Insert">
        <Tool icon={Heading1} title="Title"
          onClick={() => addElement("text", { text: "Your Title", fontSize: 72, fontWeight: 800, width: 700, height: 100 })} />
        <Tool icon={Heading2} title="Subtitle"
          onClick={() => addElement("text", { text: "Subtitle goes here", fontSize: 36, fontWeight: 500, width: 600, height: 60 })} />
        <Tool icon={AlignLeft} title="Body"
          onClick={() => addElement("text", { text: "Lorem ipsum dolor sit amet, consectetur adipiscing.", fontSize: 18, fontWeight: 400, width: 480, height: 80 })} />
        <Tool icon={List} title="Bullets"
          onClick={() => addElement("text", { text: "• Point one\n• Point two\n• Point three", fontSize: 22, fontWeight: 400, width: 420, height: 140, lineHeight: 1.6 })} />
      </Section>

      <div className="ml-auto flex items-center gap-1">
        <button onClick={toggleGrid} className="h-8 px-2 rounded-md flex items-center gap-1.5 text-[11px] font-bold transition hover:bg-white/5"
          style={{ color: showGrid ? "hsl(var(--studio-accent-hot))" : "hsl(var(--studio-text-sec))" }}>
          <Grid3x3 className="h-3.5 w-3.5" /> Grid
        </button>
        <Divider />
        <button onClick={() => setZoom(zoom - 0.1)} className="h-8 w-8 rounded-md flex items-center justify-center hover:bg-white/5"
          style={{ color: "hsl(var(--studio-text-sec))" }}>
          <ZoomOut className="h-4 w-4" />
        </button>
        <span className="text-[11px] font-bold w-10 text-center" style={{ color: "hsl(var(--studio-text-pri))" }}>
          {Math.round(zoom * 100)}%
        </span>
        <button onClick={() => setZoom(zoom + 0.1)} className="h-8 w-8 rounded-md flex items-center justify-center hover:bg-white/5"
          style={{ color: "hsl(var(--studio-text-sec))" }}>
          <ZoomIn className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
};

const Section = ({ label, children }: { label: string; children: React.ReactNode }) => (
  <div className="flex items-center gap-1">
    <span className="text-[10px] uppercase font-bold tracking-wider mr-1.5" style={{ color: "hsl(var(--studio-text-mut))" }}>{label}</span>
    {children}
  </div>
);

const Divider = () => (
  <div className="h-6 w-px mx-2" style={{ background: "hsl(var(--studio-border))" }} />
);

const Tool = ({ icon: Icon, title, onClick }: { icon: any; title: string; onClick: () => void }) => (
  <button onClick={onClick} title={title}
    className="h-8 w-8 rounded-md flex items-center justify-center transition hover:bg-white/10"
    style={{ color: "hsl(var(--studio-text-pri))" }}>
    <Icon className="h-4 w-4" />
  </button>
);