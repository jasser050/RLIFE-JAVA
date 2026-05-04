export type ElementType = "text" | "rect" | "circle" | "line" | "image" | "icon" | "sticker";

export interface CanvasEl {
  id: string;
  type: ElementType;
  x: number;
  y: number;
  width: number;
  height: number;
  rotation: number;
  opacity: number;
  locked: boolean;
  // fill / stroke
  fill: string;
  stroke: string;
  strokeWidth: number;
  borderRadius: number;
  // gradient
  useGradient: boolean;
  gradientA: string;
  gradientB: string;
  gradientAngle: number;
  // shadow
  shadow: boolean;
  shadowBlur: number;
  shadowColor: string;
  shadowOffsetY: number;
  // text
  text: string;
  fontFamily: string;
  fontSize: number;
  fontWeight: number;
  italic: boolean;
  underline: boolean;
  strikethrough: boolean;
  textAlign: "left" | "center" | "right";
  letterSpacing: number;
  lineHeight: number;
  textColor: string;
  textBg: string;
  // image
  imageUrl: string;
  // icon
  iconName: string;
  iconSize: number;
  // sticker (emoji or 3d object)
  sticker: string;
}

export interface Slide {
  id: string;
  title: string;
  background: string;        // hex/gradient css
  elements: CanvasEl[];
  notes: string;
}

export interface ThemePreset {
  id: string;
  name: string;
  bg: string;        // background css value
  accent: string;    // hex
  text: string;      // hex
  font: string;
  preview: [string, string];
}