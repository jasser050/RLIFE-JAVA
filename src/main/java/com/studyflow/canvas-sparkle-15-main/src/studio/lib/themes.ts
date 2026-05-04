import type { ThemePreset } from "./types";

export const THEMES: ThemePreset[] = [
  { id: "midnight", name: "Midnight",   bg: "linear-gradient(135deg,#0F172A 0%,#1E1B4B 100%)", accent: "#7F77DD", text: "#F0F6FC", font: "Inter", preview: ["#0F172A","#7F77DD"] },
  { id: "aurora",   name: "Aurora",     bg: "linear-gradient(135deg,#0F1B2D 0%,#1E3A8A 50%,#7C3AED 100%)", accent: "#22D3EE", text: "#F0F6FC", font: "Inter", preview: ["#0F1B2D","#22D3EE"] },
  { id: "sunset",   name: "Sunset",     bg: "linear-gradient(135deg,#7C2D12 0%,#EA580C 60%,#FBBF24 100%)", accent: "#FFFFFF", text: "#FFFFFF", font: "Poppins", preview: ["#EA580C","#FBBF24"] },
  { id: "minimal",  name: "Minimal",    bg: "#FAFAF9", accent: "#18181B", text: "#18181B", font: "Inter", preview: ["#FAFAF9","#18181B"] },
  { id: "paper",    name: "Paper",      bg: "#FBF8F1", accent: "#7C2D12", text: "#1C1917", font: "Playfair Display", preview: ["#FBF8F1","#7C2D12"] },
  { id: "bold",     name: "Bold",       bg: "#0A0A0A", accent: "#FACC15", text: "#FAFAFA", font: "Space Grotesk", preview: ["#0A0A0A","#FACC15"] },
  { id: "ocean",    name: "Ocean",      bg: "linear-gradient(135deg,#0C4A6E 0%,#0EA5E9 100%)", accent: "#FDE68A", text: "#FFFFFF", font: "Inter", preview: ["#0C4A6E","#0EA5E9"] },
  { id: "forest",   name: "Forest",     bg: "linear-gradient(135deg,#064E3B 0%,#10B981 100%)", accent: "#FDE68A", text: "#FFFFFF", font: "Inter", preview: ["#064E3B","#10B981"] },
  { id: "rose",     name: "Rose",       bg: "linear-gradient(135deg,#831843 0%,#EC4899 100%)", accent: "#FCE7F3", text: "#FFFFFF", font: "Poppins", preview: ["#831843","#EC4899"] },
  { id: "academic", name: "Academic",   bg: "#0F172A", accent: "#3B82F6", text: "#F0F6FC", font: "Merriweather", preview: ["#0F172A","#3B82F6"] },
  { id: "creative", name: "Creative",   bg: "linear-gradient(135deg,#170E2D 0%,#7C3AED 50%,#EC4899 100%)", accent: "#F0ABFC", text: "#FFFFFF", font: "Space Grotesk", preview: ["#170E2D","#EC4899"] },
  { id: "mono",     name: "Mono",       bg: "#FFFFFF", accent: "#000000", text: "#000000", font: "JetBrains Mono", preview: ["#FFFFFF","#000000"] },
];

export const FONT_FAMILIES = [
  "Inter","Poppins","Playfair Display","Space Grotesk","Merriweather",
  "Roboto","Montserrat","Lora","Bebas Neue","Oswald","Raleway",
  "JetBrains Mono","Source Code Pro","DM Sans","Quicksand","Nunito",
  "Work Sans","Archivo","Cormorant Garamond","Crimson Text",
];

export const FONT_SIZES = [10,12,14,16,18,20,24,28,32,36,42,48,56,64,72,84,96,120,144];

export const COLOR_SWATCHES = [
  "#000000","#FFFFFF","#F0F6FC","#1C2333","#0F172A",
  "#7F77DD","#A78BFA","#3B82F6","#06B6D4","#22D3EE",
  "#10B981","#22C55E","#FACC15","#F59E0B","#F97316",
  "#EF4444","#EC4899","#D946EF","#7C3AED","#8B5CF6",
  "#FCE7F3","#FEF3C7","#DBEAFE","#DCFCE7","#E0E7FF",
];

export const GRADIENT_PRESETS = [
  ["#7F77DD","#EC4899"],
  ["#22D3EE","#7C3AED"],
  ["#F59E0B","#EF4444"],
  ["#10B981","#22D3EE"],
  ["#0F172A","#7F77DD"],
  ["#EC4899","#FACC15"],
  ["#7C3AED","#06B6D4"],
  ["#1E3A8A","#7F77DD"],
];

// Lucide icon names available in the icon picker
export const ICON_LIBRARY = [
  "Star","Heart","Zap","Flame","Crown","Trophy","Award","Sparkles","Sun","Moon",
  "Cloud","CloudRain","Snowflake","Umbrella","Leaf","Trees","Flower","Mountain",
  "Globe","MapPin","Compass","Plane","Car","Bike","Train","Ship","Rocket",
  "Smartphone","Laptop","Monitor","Camera","Music","Headphones","Mic","Video",
  "Mail","MessageCircle","Bell","Phone","Send","Share2","Link","Bookmark",
  "Lightbulb","Brain","Target","TrendingUp","BarChart3","PieChart","LineChart",
  "DollarSign","Euro","CreditCard","Wallet","ShoppingBag","Gift","Tag",
  "Lock","Unlock","Shield","Key","Eye","EyeOff","CheckCircle","XCircle",
  "AlertCircle","Info","HelpCircle","Plus","Minus","X","Check","ArrowRight",
  "ArrowLeft","ArrowUp","ArrowDown","ChevronRight","Play","Pause","Settings",
  "User","Users","UserCheck","Code","Database","Server","Cpu","Wifi",
  "Book","BookOpen","GraduationCap","Pencil","FileText","Folder","Image",
  "Calendar","Clock","Timer","Hourglass","Activity","Atom","Beaker","FlaskConical",
];

export const SLIDE_TEMPLATES = [
  { id: "blank", name: "Blank" },
  { id: "title", name: "Title" },
  { id: "title-content", name: "Title + Content" },
  { id: "two-column", name: "Two Columns" },
  { id: "image-text", name: "Image + Text" },
  { id: "quote", name: "Quote" },
  { id: "stat", name: "Big Stat" },
  { id: "section", name: "Section Break" },
];

// Emoji + 3D-style stickers (rendered huge as text)
export const STICKER_LIBRARY = [
  "✨","⭐","🔥","💡","🚀","🎯","🏆","💎","❤️","💯",
  "🎉","🎊","🎁","🌈","☀️","🌙","⚡","🌟","💫","🪐",
  "🧠","👁️","👋","👍","👏","🙌","💪","🫶","🤝","🤖",
  "📱","💻","🖥️","⌨️","🎧","📷","🎬","🎮","🕹️","🎨",
  "📚","📖","✏️","📝","📊","📈","📉","📌","📎","🔗",
  "💰","💳","💵","🪙","🛍️","🛒","🎓","🏅","🥇","🥈",
  "🌍","🌎","🌏","🗺️","✈️","🚗","🚀","🛸","⛵","🚢",
  "🍎","🍊","🍋","🍇","🍓","🍕","🍔","☕","🍵","🧋",
  "🐶","🐱","🦊","🐼","🦁","🐯","🦄","🐲","🦋","🌸",
  "🌺","🌻","🌷","🌹","🍀","🌿","🌳","🌴","🪴","🍃",
];

// Built-in presentation templates (Canva-style starters)
export interface SlideTemplate {
  id: string;
  name: string;
  themeId: string;
  build: (uid: () => string) => any[]; // returns CanvasEl[]
}

const el = (uid: () => string, patch: any) => ({
  id: uid(),
  type: "text",
  x: 80, y: 80, width: 400, height: 80, rotation: 0, opacity: 1, locked: false,
  fill: "transparent", stroke: "transparent", strokeWidth: 0, borderRadius: 12,
  useGradient: false, gradientA: "#7F77DD", gradientB: "#EC4899", gradientAngle: 135,
  shadow: false, shadowBlur: 24, shadowColor: "#000000", shadowOffsetY: 8,
  text: "", fontFamily: "Inter", fontSize: 32, fontWeight: 600,
  italic: false, underline: false, strikethrough: false, textAlign: "left",
  letterSpacing: 0, lineHeight: 1.3, textColor: "#FFFFFF", textBg: "transparent",
  imageUrl: "", iconName: "Star", iconSize: 64, sticker: "✨",
  ...patch,
});

export const PRESENTATION_TEMPLATES: SlideTemplate[] = [
  {
    id: "tpl-cover-bold", name: "Bold Cover", themeId: "midnight",
    build: (uid) => [
      el(uid, { type: "rect", x: 60, y: 60, width: 8, height: 600, fill: "#7F77DD", borderRadius: 4 }),
      el(uid, { type: "text", x: 100, y: 200, width: 900, height: 80, text: "INTRODUCING", fontSize: 24, fontWeight: 700, letterSpacing: 8, textColor: "#7F77DD" }),
      el(uid, { type: "text", x: 100, y: 260, width: 1100, height: 200, text: "Your Big Idea\nStarts Here", fontSize: 110, fontWeight: 800, textColor: "#FFFFFF", lineHeight: 1.05 }),
      el(uid, { type: "text", x: 100, y: 540, width: 800, height: 60, text: "A presentation by your name • 2026", fontSize: 22, textColor: "#A8B0C2" }),
    ],
  },
  {
    id: "tpl-title-content", name: "Title + Content", themeId: "midnight",
    build: (uid) => [
      el(uid, { type: "text", x: 80, y: 80, width: 1100, height: 90, text: "Section Title", fontSize: 64, fontWeight: 800, textColor: "#FFFFFF" }),
      el(uid, { type: "rect", x: 80, y: 180, width: 80, height: 6, fill: "#7F77DD", borderRadius: 4 }),
      el(uid, { type: "text", x: 80, y: 220, width: 1100, height: 400, text: "• First key point of your slide\n• Second important takeaway\n• Third supporting detail\n• Closing thought", fontSize: 30, fontWeight: 400, textColor: "#E6E9F2", lineHeight: 1.7 }),
    ],
  },
  {
    id: "tpl-two-col", name: "Two Columns", themeId: "minimal",
    build: (uid) => [
      el(uid, { type: "text", x: 80, y: 60, width: 1100, height: 80, text: "Compare & Contrast", fontSize: 56, fontWeight: 800, textColor: "#18181B" }),
      el(uid, { type: "rect", x: 80, y: 180, width: 540, height: 460, fill: "#F4F4F5", borderRadius: 24 }),
      el(uid, { type: "text", x: 110, y: 210, width: 480, height: 50, text: "OPTION A", fontSize: 18, fontWeight: 700, letterSpacing: 4, textColor: "#7F77DD" }),
      el(uid, { type: "text", x: 110, y: 260, width: 480, height: 360, text: "Describe the first option here. Keep it short and punchy.", fontSize: 24, textColor: "#27272A", lineHeight: 1.5 }),
      el(uid, { type: "rect", x: 660, y: 180, width: 540, height: 460, fill: "#18181B", borderRadius: 24 }),
      el(uid, { type: "text", x: 690, y: 210, width: 480, height: 50, text: "OPTION B", fontSize: 18, fontWeight: 700, letterSpacing: 4, textColor: "#FACC15" }),
      el(uid, { type: "text", x: 690, y: 260, width: 480, height: 360, text: "Describe the second option here.", fontSize: 24, textColor: "#FAFAFA", lineHeight: 1.5 }),
    ],
  },
  {
    id: "tpl-stat", name: "Big Stat", themeId: "bold",
    build: (uid) => [
      el(uid, { type: "text", x: 80, y: 160, width: 1100, height: 60, text: "RESULTS", fontSize: 28, fontWeight: 700, letterSpacing: 8, textColor: "#FACC15" }),
      el(uid, { type: "text", x: 80, y: 220, width: 1100, height: 280, text: "98%", fontSize: 280, fontWeight: 900, textColor: "#FAFAFA", lineHeight: 1 }),
      el(uid, { type: "text", x: 80, y: 520, width: 1100, height: 80, text: "of customers recommend our product", fontSize: 32, textColor: "#A1A1AA" }),
    ],
  },
  {
    id: "tpl-quote", name: "Quote", themeId: "paper",
    build: (uid) => [
      el(uid, { type: "text", x: 80, y: 80, width: 200, height: 200, text: "“", fontSize: 240, fontWeight: 800, textColor: "#7C2D12", lineHeight: 1 }),
      el(uid, { type: "text", x: 200, y: 220, width: 1000, height: 280, text: "Design is not just what it looks like. Design is how it works.", fontSize: 56, fontWeight: 600, textColor: "#1C1917", lineHeight: 1.3, italic: true }),
      el(uid, { type: "text", x: 200, y: 540, width: 800, height: 50, text: "— STEVE JOBS", fontSize: 22, fontWeight: 700, letterSpacing: 6, textColor: "#7C2D12" }),
    ],
  },
  {
    id: "tpl-image-text", name: "Image + Text", themeId: "aurora",
    build: (uid) => [
      el(uid, { type: "image", x: 60, y: 60, width: 560, height: 600, borderRadius: 24 }),
      el(uid, { type: "text", x: 660, y: 140, width: 560, height: 60, text: "FEATURE", fontSize: 22, fontWeight: 700, letterSpacing: 8, textColor: "#22D3EE" }),
      el(uid, { type: "text", x: 660, y: 200, width: 560, height: 200, text: "Tell your story", fontSize: 72, fontWeight: 800, textColor: "#FFFFFF", lineHeight: 1.05 }),
      el(uid, { type: "text", x: 660, y: 420, width: 540, height: 200, text: "Add a short, compelling description that supports your headline and engages the audience.", fontSize: 22, textColor: "#CBD5E1", lineHeight: 1.6 }),
    ],
  },
  {
    id: "tpl-section", name: "Section Break", themeId: "creative",
    build: (uid) => [
      el(uid, { type: "text", x: 80, y: 280, width: 1100, height: 80, text: "CHAPTER 02", fontSize: 26, fontWeight: 700, letterSpacing: 12, textColor: "#F0ABFC" }),
      el(uid, { type: "text", x: 80, y: 340, width: 1100, height: 160, text: "The Next Step", fontSize: 120, fontWeight: 800, textColor: "#FFFFFF" }),
      el(uid, { type: "rect", x: 80, y: 510, width: 120, height: 8, fill: "#F0ABFC", borderRadius: 4 }),
    ],
  },
  {
    id: "tpl-thanks", name: "Thank You", themeId: "rose",
    build: (uid) => [
      el(uid, { type: "text", x: 80, y: 220, width: 1120, height: 200, text: "Thank You.", fontSize: 160, fontWeight: 800, textColor: "#FFFFFF", textAlign: "center" }),
      el(uid, { type: "text", x: 80, y: 440, width: 1120, height: 60, text: "Questions? Let's talk.", fontSize: 32, textColor: "#FCE7F3", textAlign: "center" }),
    ],
  },
];