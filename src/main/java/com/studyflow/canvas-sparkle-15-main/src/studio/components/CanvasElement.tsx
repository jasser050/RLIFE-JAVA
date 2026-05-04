import { Rnd } from "react-rnd";
import * as LucideIcons from "lucide-react";
import type { CanvasEl } from "../lib/types";
import { useStudio } from "../lib/store";
import { useRef } from "react";

interface Props { el: CanvasEl; selected: boolean; }

export const CanvasElementView = ({ el, selected }: Props) => {
  const { updateElement, selectElement } = useStudio();
  const fileRef = useRef<HTMLInputElement>(null);

  const handleImagePick = (file?: File) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => updateElement(el.id, { imageUrl: reader.result as string });
    reader.readAsDataURL(file);
  };

  const commonStyle: React.CSSProperties = {
    opacity: el.opacity,
    transform: `rotate(${el.rotation}deg)`,
    transformOrigin: "center center",
    boxShadow: el.shadow ? `0 ${el.shadowOffsetY}px ${el.shadowBlur}px ${el.shadowColor}66` : undefined,
  };

  const fillCss = el.useGradient
    ? `linear-gradient(${el.gradientAngle}deg, ${el.gradientA}, ${el.gradientB})`
    : el.fill;

  const renderInner = () => {
    if (el.type === "text") {
      return (
        <div
          contentEditable={selected}
          suppressContentEditableWarning
          onBlur={(e) => updateElement(el.id, { text: e.currentTarget.innerText })}
          className="w-full h-full outline-none whitespace-pre-wrap"
          style={{
            fontFamily: `'${el.fontFamily}', sans-serif`,
            fontSize: el.fontSize,
            fontWeight: el.fontWeight,
            fontStyle: el.italic ? "italic" : "normal",
            textDecoration: [el.underline && "underline", el.strikethrough && "line-through"].filter(Boolean).join(" ") || "none",
            textAlign: el.textAlign,
            color: el.textColor,
            background: el.textBg !== "transparent" ? el.textBg : undefined,
            padding: el.textBg !== "transparent" ? "8px 12px" : 0,
            borderRadius: el.borderRadius,
            letterSpacing: el.letterSpacing,
            lineHeight: el.lineHeight,
            ...commonStyle,
          }}>
          {el.text}
        </div>
      );
    }
    if (el.type === "rect") {
      return <div className="w-full h-full" style={{
        background: fillCss,
        borderRadius: el.borderRadius,
        border: el.strokeWidth > 0 ? `${el.strokeWidth}px solid ${el.stroke}` : "none",
        ...commonStyle,
      }} />;
    }
    if (el.type === "circle") {
      return <div className="w-full h-full rounded-full" style={{
        background: fillCss,
        border: el.strokeWidth > 0 ? `${el.strokeWidth}px solid ${el.stroke}` : "none",
        ...commonStyle,
      }} />;
    }
    if (el.type === "line") {
      return <div className="w-full" style={{
        height: el.strokeWidth, background: el.stroke, marginTop: el.height / 2,
        borderRadius: 999, ...commonStyle,
      }} />;
    }
    if (el.type === "image") {
      return el.imageUrl ? (
        <img src={el.imageUrl} alt="" className="w-full h-full object-cover"
          style={{ borderRadius: el.borderRadius, ...commonStyle }} />
      ) : (
        <>
          <input ref={fileRef} type="file" accept="image/*" className="hidden"
            onChange={(e) => handleImagePick(e.target.files?.[0])} />
          <button type="button"
            onClick={(e) => { e.stopPropagation(); fileRef.current?.click(); }}
            className="w-full h-full flex flex-col items-center justify-center gap-2 cursor-pointer"
            style={{ background: el.fill, borderRadius: el.borderRadius,
              border: "2px dashed hsl(var(--studio-accent) / 0.4)", ...commonStyle }}>
            <LucideIcons.ImagePlus className="h-8 w-8" style={{ color: "hsl(var(--studio-accent))" }} />
            <span className="text-xs font-bold" style={{ color: "hsl(var(--studio-accent))" }}>Click to upload image</span>
          </button>
        </>
      );
    }
    if (el.type === "icon") {
      const Icon = (LucideIcons as any)[el.iconName] || LucideIcons.Star;
      return (
        <div className="w-full h-full flex items-center justify-center" style={commonStyle}>
          <Icon style={{ width: el.iconSize, height: el.iconSize, color: el.textColor,
            strokeWidth: 1.8 }} />
        </div>
      );
    }
    if (el.type === "sticker") {
      return (
        <div className="w-full h-full flex items-center justify-center select-none"
          style={{ fontSize: Math.min(el.width, el.height) * 0.85, lineHeight: 1,
            filter: el.shadow ? `drop-shadow(0 ${el.shadowOffsetY}px ${el.shadowBlur}px ${el.shadowColor}66)` : undefined,
            ...commonStyle }}>
          {el.sticker}
        </div>
      );
    }
    return null;
  };

  return (
    <Rnd
      size={{ width: el.width, height: el.height }}
      position={{ x: el.x, y: el.y }}
      disableDragging={el.locked}
      enableResizing={!el.locked}
      bounds="parent"
      onDragStop={(_, d) => updateElement(el.id, { x: d.x, y: d.y })}
      onResizeStop={(_, __, ref, ___, pos) => updateElement(el.id, {
        width: parseFloat(ref.style.width),
        height: parseFloat(ref.style.height),
        x: pos.x, y: pos.y,
      })}
      onMouseDown={(e: any) => { e.stopPropagation(); selectElement(el.id); }}
      style={{
        outline: selected ? "2px solid hsl(var(--studio-accent-hot))" : "none",
        outlineOffset: 2,
        cursor: el.locked ? "not-allowed" : "move",
      }}>
      {renderInner()}
    </Rnd>
  );
};