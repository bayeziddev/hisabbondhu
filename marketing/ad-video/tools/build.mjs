// Generates the HyperFrames compositions for the HisabBondhu ad (1080x1920, Bangla).
// Usage: node tools/build.mjs   → writes index.html, compositions/*.html
// Edit the SCENES table below (timings are seconds, local to each scene), then re-run.
import { writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
mkdirSync(join(ROOT, "compositions"), { recursive: true });

const W = 1080, H = 1920;
const C = { green: "#087F5B", deep: "#053D2E", amber: "#FFD43B", red: "#E03131", orange: "#F08C00", blue: "#1971C2" };
// Phone: screen 405x720 at (302,440); screenshots are 1080x1920 → factor 0.375.
const PHONE = { left: 290, top: 412, w: 429, h: 744, sx: 12, sy: 12, sw: 405, sh: 720 };

const FONT_FACE = ["regular:400", "semibold:600", "bold:700"]
  .map((s) => { const [f, w] = s.split(":"); return `@font-face{font-family:'Hind Siliguri';src:url('assets/fonts/hind_siliguri_${f}.ttf') format('truetype');font-weight:${w};font-style:normal}`; })
  .join("\n");

const BASE_CSS = `
${FONT_FACE}
#root{position:absolute;inset:0;font-family:'Hind Siliguri',sans-serif;color:#fff;overflow:visible}
#root .hdr{position:absolute;left:120px;width:768px;top:292px;height:114px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;text-align:center}
#root .chip{font-size:28px;font-weight:700;color:${C.deep};background:${C.amber};border-radius:999px;padding:0 22px;line-height:40px}
#root .ttl{font-size:60px;font-weight:700;line-height:1.15;text-shadow:0 4px 18px rgba(0,0,0,.25)}
#root .ttl .w{display:inline-block;margin:0 .14em}
#root .phone{position:absolute;left:${PHONE.left}px;top:${PHONE.top}px;width:${PHONE.w}px;height:${PHONE.h}px;border-radius:58px;background:#0b0f0e;box-shadow:0 34px 90px rgba(0,0,0,.45),inset 0 0 0 2px rgba(255,255,255,.10)}
#root .screen{position:absolute;left:${PHONE.sx}px;top:${PHONE.sy}px;width:${PHONE.sw}px;height:${PHONE.sh}px;border-radius:46px;overflow:hidden;background:#fff}
#root .enter,#root .zoom{position:absolute;left:0;top:0;width:${PHONE.sw}px;height:${PHONE.sh}px}
#root img.scr{position:absolute;left:0;top:0;width:${PHONE.sw}px;height:${PHONE.sh}px}
#root .tap{position:absolute;left:0;top:0;width:54px;height:54px;border-radius:50%;background:rgba(255,255,255,.55);border:4px solid ${C.amber};box-shadow:0 4px 14px rgba(0,0,0,.35);opacity:0}
#root .press{position:absolute;background:rgba(8,127,91,.30);opacity:0}
#root .mark{position:absolute;border:4px solid ${C.amber};border-radius:16px;opacity:0;box-shadow:0 0 0 999px rgba(5,61,46,.0)}
`;

// ---------- small code builders (emit GSAP lines) ----------
function scene(id) {
  const lines = [], sfx = [], extraHtml = [];
  let pressN = 0, pressEnd = 0;
  const p = (s) => `#${id}-${s}`;
  const api = {
    id, lines, sfx, extraHtml, p,
    js: (s) => lines.push(s),
    sound: (t, file, vol = 0.7) => sfx.push({ t, file, vol }),
    // header: chip + title words blur in, exit before the scene ends
    header(dur, at = 0.05) {
      lines.push(`tl.fromTo('${p("chip")}',{opacity:0,filter:'blur(6px)'},{opacity:1,filter:'blur(0px)',duration:0.16,ease:'power4.out'},${at});`);
      lines.push(`gsap.utils.toArray('${p("ttl")} .w').forEach((w,i)=>tl.fromTo(w,{opacity:0,filter:'blur(6px)'},{opacity:1,filter:'blur(0px)',duration:0.13,ease:'power4.out'},${at + 0.06}+i*0.067));`);
      lines.push(`tl.to('${p("hdr")}',{opacity:0,filter:'blur(6px)',duration:0.16,ease:'power2.in'},${(dur - 0.18).toFixed(2)});`);
    },
    enter(kind = "slide") {
      if (kind === "slide") lines.push(`tl.fromTo('${p("enter")}',{x:70,opacity:0},{x:0,opacity:1,duration:0.32,ease:'power3.out'},0);`);
      else if (kind === "settle") lines.push(`tl.fromTo('${p("enter")}',{scale:1.06,opacity:0.2,transformOrigin:'50% 50%'},{scale:1,opacity:1,duration:0.35,ease:'expo.out'},0);`);
      else if (kind === "fade") lines.push(`tl.fromTo('${p("enter")}',{opacity:0},{opacity:1,duration:0.25,ease:'power2.out'},0);`);
    },
    appear(t, x, y) { lines.push(`tl.fromTo('${p("tap")}',{x:${x - 27},y:${y - 27},opacity:0,scale:0.6},{opacity:1,scale:1,duration:0.2,ease:'power3.out'},${t});`); },
    move(t, x, y, d = 0.42) { lines.push(`tl.to('${p("tap")}',{x:${x - 27},y:${y - 27},duration:${d},ease:'power2.inOut'},${t});`); },
    press(t, rect, file = "click.mp3") {
      lines.push(`tl.to('${p("tap")}',{scale:0.78,duration:0.08,ease:'power2.in'},${t});`);
      lines.push(`tl.to('${p("tap")}',{scale:1,duration:0.2,ease:'power2.out'},${(t + 0.08).toFixed(2)});`);
      pressEnd = t + 0.29;
      if (rect) {
        const n = ++pressN, [x, y, w, h, r = 14] = rect;
        extraHtml.push(`<div id="${id}-pr${n}" class="press" style="left:${x}px;top:${y}px;width:${w}px;height:${h}px;border-radius:${r}px"></div>`);
        lines.push(`tl.fromTo('${p("pr" + n)}',{opacity:0},{opacity:1,duration:0.06},${t});`);
        lines.push(`tl.to('${p("pr" + n)}',{opacity:0,duration:0.3,ease:'power2.out'},${(t + 0.16).toFixed(2)});`);
      }
      sfx.push({ t: t + 0.03, file, vol: 0.55 });
    },
    hideTap(t) { t = Math.max(t, +(pressEnd + 0.01).toFixed(2)); lines.push(`tl.to('${p("tap")}',{opacity:0,scale:0.7,duration:0.15,ease:'power2.in'},${t});`); },
    // zoom the screen content so (px,py) sits at the screen centre, clamped to the edges
    zoom(t, px, py, s, d = 0.6) {
      const cx = PHONE.sw / 2, cy = PHONE.sh / 2;
      let x = cx - px * s, y = cy - py * s;
      x = Math.min(0, Math.max(PHONE.sw - PHONE.sw * s, x));
      y = Math.min(0, Math.max(PHONE.sh - PHONE.sh * s, y));
      lines.push(`tl.to('${p("zoom")}',{scale:${s},x:${x.toFixed(1)},y:${y.toFixed(1)},transformOrigin:'0 0',duration:${d},ease:'power2.inOut'},${t});`);
    },
    unzoom(t, d = 0.5) { lines.push(`tl.to('${p("zoom")}',{scale:1,x:0,y:0,transformOrigin:'0 0',duration:${d},ease:'power2.inOut'},${t});`); },
    // amber outline that marks a detail (screen coordinates)
    mark(t, rect, hold = 1.4) {
      const n = extraHtml.length + 1, [x, y, w, h] = rect;
      extraHtml.push(`<div id="${id}-mk${n}" class="mark" style="left:${x}px;top:${y}px;width:${w}px;height:${h}px"></div>`);
      lines.push(`tl.fromTo('${p("mk" + n)}',{opacity:0,scale:1.12,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.28,ease:'expo.out'},${t});`);
      lines.push(`tl.to('${p("mk" + n)}',{opacity:0,duration:0.18,ease:'power2.in'},${(t + hold).toFixed(2)});`);
    },
  };
  return api;
}

const words = (s) => s.split(" ").map((w) => `<span class="w">${w}</span>`).join("");

function phoneHtml(id, screenImg, inner = "") {
  return `<div id="${id}-phone" class="phone"><div id="${id}-screen" class="screen"><div id="${id}-enter" class="enter" data-layout-allow-overflow><div id="${id}-zoom" class="zoom" data-layout-allow-overflow>
  ${screenImg ? `<img id="${id}-img" class="scr" src="assets/screens/${screenImg}.png" alt="">` : ""}
  ${inner}
  <div id="${id}-tap" class="tap"></div>
</div></div></div></div>`;
}

function headerHtml(id, chip, title) {
  return `<div id="${id}-hdr" class="hdr">${chip ? `<div id="${id}-chip" class="chip">${chip}</div>` : `<div id="${id}-chip"></div>`}<div id="${id}-ttl" class="ttl">${words(title)}</div></div>`;
}

// ---------- scenes ----------
// Each: { id, dur, captions:[[start,end,text]], build(s) → html }
const SCENES = [];

// 0. HOOK — frame 0 already carries the question.
SCENES.push({
  id: "hook", dur: 4.2, captions: [],
  build(s) {
    s.js(`tl.fromTo('#hook-q',{scale:1.06,transformOrigin:'50% 50%'},{scale:1,duration:0.9,ease:'expo.out'},0);`);
    s.js(`tl.fromTo('#hook-strike',{scaleX:0,transformOrigin:'0% 50%'},{scaleX:1,duration:0.3,ease:'power3.out'},1.15);`);
    s.sound(1.17, "whoosh-short.mp3", 0.6);
    s.js(`tl.to('#hook-q',{opacity:0,y:-40,filter:'blur(10px)',duration:0.22,ease:'power3.in'},1.62);`);
    s.js(`tl.fromTo('#hook-phone',{y:1560},{y:0,duration:0.75,ease:'expo.out'},1.72);`);
    s.sound(1.74, "whoosh.mp3", 0.6);
    s.header(4.2, 1.8);
    s.zoom(2.75, 165, 186, 1.5, 0.6);
    return `${headerHtml("hook", "", "এখন মোবাইলেই সব হিসাব")}
<div id="hook-q" style="position:absolute;left:120px;width:768px;top:640px;text-align:center">
  <div style="font-size:92px;font-weight:700;line-height:1.15">কাগজের খাতায়</div>
  <div style="font-size:92px;font-weight:700;line-height:1.15;color:${C.amber}">বাকির হিসাব?</div>
  <div id="hook-strike" style="position:absolute;left:40px;right:40px;top:118px;height:14px;border-radius:7px;background:${C.red};transform-origin:0 50%"></div>
  <div style="font-size:40px;font-weight:600;line-height:1.3;margin-top:26px;opacity:.9">হারায়, ভেজে, ভুল হয়</div>
</div>
<div id="hook-lift" style="position:absolute;inset:0">${phoneHtml("hook", "home").replace('id="hook-phone" class="phone"', 'id="hook-phone" class="phone"')}</div>`;
  },
});

// 1. BRAND — name + quick montage of real screens
SCENES.push({
  id: "brand", dur: 3.2,
  captions: [[0.2, 1.75, "নেট ছাড়াই দোকানের হিসাব"], [1.85, 3.15, "দেখুন, কীভাবে শুরু করবেন"]],
  build(s) {
    s.js(`tl.fromTo('#brand-logo',{opacity:0,scale:0.7,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.4,ease:'back.out(1.4)'},0.05);`);
    s.js(`gsap.utils.toArray('#brand-ttl .w').forEach((w,i)=>tl.fromTo(w,{opacity:0,filter:'blur(6px)'},{opacity:1,filter:'blur(0px)',duration:0.13,ease:'power4.out'},0.15+i*0.067));`);
    s.js(`tl.to('#brand-hdr',{opacity:0,filter:'blur(6px)',duration:0.16,ease:'power2.in'},3.02);`);
    // montage inside the phone: home → customers → stock → reports
    s.js(`tl.set('#brand-m1',{opacity:1},0.7); tl.set('#brand-m2',{opacity:1},1.3); tl.set('#brand-m3',{opacity:1},1.9);`);
    s.sound(0.05, "pop.mp3", 0.5);
    return `<div id="brand-hdr" class="hdr" style="flex-direction:row;gap:22px">
  <img id="brand-logo" src="assets/icon.png" alt="" style="width:96px;height:96px;border-radius:24px;box-shadow:0 8px 24px rgba(0,0,0,.3)">
  <div id="brand-ttl" class="ttl" style="font-size:76px">${words("হিসাববন্ধু")}</div>
</div>
${phoneHtml("brand", "home", `
  <img id="brand-m1" class="scr" src="assets/screens/customers.png" alt="" style="opacity:0">
  <img id="brand-m2" class="scr" src="assets/screens/stock.png" alt="" style="opacity:0">
  <img id="brand-m3" class="scr" src="assets/screens/reports.png" alt="" style="opacity:0">`)}`;
  },
});

// 2. INSTALL — store search → Install → Open (drawn: we have no store capture)
SCENES.push({
  id: "install", dur: 6.0,
  captions: [[0.2, 1.25, "Play Store-এ খুঁজুন"], [1.3, 2.5, "লিখুন: HisabBondhu"], [2.55, 4.35, "Install চাপুন"], [4.4, 5.95, "তারপর Open চাপুন"]],
  build(s) {
    s.header(6.0);
    s.enter("fade");
    const q = "HisabBondhu";
    s.js(`gsap.utils.toArray('#install-q .c').forEach((c,i)=>tl.set(c,{opacity:1},0.45+i*0.07));`);
    s.sound(0.45, "typing.mp3", 0.45);
    s.js(`tl.fromTo('#install-card',{opacity:0,y:24},{opacity:1,y:0,duration:0.35,ease:'expo.out'},1.4);`);
    s.js(`tl.fromTo('#install-shots',{opacity:0,y:30},{opacity:1,y:0,duration:0.4,ease:'expo.out'},1.55);`);
    s.sound(1.42, "pop.mp3", 0.45);
    s.appear(2.2, 202, 252);
    s.press(2.75, null, "click.mp3");
    s.js(`tl.set('#install-btn-t',{opacity:0},2.85); tl.set('#install-prog',{opacity:1},2.85);`);
    s.js(`tl.fromTo('#install-fill',{scaleX:0,transformOrigin:'0% 50%'},{scaleX:1,duration:1.2,ease:'power1.inOut'},2.9);`);
    s.js(`tl.set('#install-prog',{opacity:0},4.12); tl.set('#install-open',{opacity:1},4.12);`);
    s.js(`tl.fromTo('#install-open',{scale:0.92,transformOrigin:'50% 50%'},{scale:1,duration:0.3,ease:'back.out(1.6)'},4.12);`);
    s.sound(4.14, "pop.mp3", 0.5);
    s.press(4.75, null, "click-soft.mp3");
    s.hideTap(4.95);
    // app opens: green splash grows from the button
    s.js(`tl.fromTo('#install-splash',{opacity:0,scale:0.2,transformOrigin:'50% 35%'},{opacity:1,scale:1,duration:0.45,ease:'power3.in'},5.0);`);
    s.js(`tl.fromTo('#install-splashlogo',{opacity:0,scale:0.6,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.3,ease:'expo.out'},5.35);`);
    s.sound(5.0, "whoosh.mp3", 0.45);
    const chars = [...q].map((c) => `<span class="c" style="opacity:0">${c}</span>`).join("");
    return `${headerHtml("install", "ধাপ ১", "অ্যাপ ইনস্টল করুন")}
${phoneHtml("install", null, `
 <div style="position:absolute;inset:0;background:#fff;font-family:'Hind Siliguri',sans-serif;color:#1f2a27">
  <div data-layout-ignore style="position:absolute;left:28px;top:14px;font-size:15px;color:#555">9:30</div>
  <div style="position:absolute;left:16px;top:44px;width:373px;height:52px;border-radius:26px;background:#EEF2F1;display:flex;align-items:center;padding-left:18px;gap:12px">
    <svg width="22" height="22" viewBox="0 0 24 24"><circle cx="10" cy="10" r="7" stroke="#5f6b67" stroke-width="2.5" fill="none"/><path d="M15 15l6 6" stroke="#5f6b67" stroke-width="2.5" stroke-linecap="round"/></svg>
    <div id="install-q" style="font-size:22px;font-weight:600;letter-spacing:.2px">${chars}</div>
  </div>
  <div id="install-card" style="position:absolute;left:20px;top:116px;width:365px;opacity:0">
    <div style="display:flex;gap:16px;align-items:center">
      <img src="assets/icon.png" alt="" style="width:84px;height:84px;border-radius:20px;box-shadow:0 2px 8px rgba(0,0,0,.18)">
      <div><div style="font-size:23px;font-weight:700;line-height:1.2">HisabBondhu: Shop Ledger Khata</div>
      <div data-layout-ignore style="font-size:16px;color:#087F5B;font-weight:600;margin-top:2px">HisabBondhu · Business · Free</div></div>
    </div>
    <div style="position:absolute;left:0;top:112px;width:365px;height:50px;border-radius:25px;background:#087F5B;overflow:hidden">
      <div id="install-btn-t" style="position:absolute;inset:0;color:#fff;font-size:21px;font-weight:700;display:flex;align-items:center;justify-content:center">Install</div>
      <div id="install-prog" style="position:absolute;inset:0;background:#DDEFE8;opacity:0">
        <div id="install-fill" style="position:absolute;inset:0;background:#7CC7AE"></div>
        <div style="position:absolute;inset:0;color:#064e3b;font-size:21px;font-weight:700;display:flex;align-items:center;justify-content:center">ইনস্টল হচ্ছে…</div>
      </div>
      <div id="install-open" style="position:absolute;inset:0;color:#fff;font-size:21px;font-weight:700;display:flex;align-items:center;justify-content:center;opacity:0;background:#087F5B">Open</div>
    </div>
  </div>
  <div id="install-shots" style="position:absolute;left:20px;top:300px;display:flex;gap:10px;opacity:0">
    <img src="assets/screens/home.png" alt="" style="width:115px;height:204px;border-radius:12px;border:1px solid #dde3e1">
    <img src="assets/screens/customer_ledger.png" alt="" style="width:115px;height:204px;border-radius:12px;border:1px solid #dde3e1">
    <img src="assets/screens/reports.png" alt="" style="width:115px;height:204px;border-radius:12px;border:1px solid #dde3e1">
  </div>
  <div data-layout-ignore style="position:absolute;left:20px;top:524px;width:365px;font-size:17px;line-height:1.45;color:#3c4744">ইন্টারনেট ছাড়াই দোকানের বিক্রি, বাকি, খরচ ও স্টকের সহজ বাংলা হিসাব খাতা।</div>
  <div id="install-splash" style="position:absolute;inset:0;background:#087F5B;opacity:0;display:flex;align-items:center;justify-content:center">
    <div id="install-splashlogo" style="width:120px;height:120px;border-radius:30px;opacity:0;background:url('assets/icon.png') center/cover"></div>
  </div>
 </div>`)}`;
  },
});

// 3. LANGUAGE
SCENES.push({
  id: "lang", dur: 4.0,
  captions: [[0.2, 1.9, "ভাষা বাছুন: বাংলা"], [1.95, 3.95, "শুরু করুন চাপুন"]],
  build(s) {
    s.header(4.0);
    s.js(`tl.fromTo('#lang-cover',{opacity:1},{opacity:0,duration:0.35,ease:'power2.out'},0.05);`);
    s.appear(0.6, 201, 526);
    s.press(0.95, [22, 491, 356, 69, 16]);
    s.move(1.6, 203, 690);
    s.press(2.3, [18, 676, 369, 40, 20], "click-soft.mp3");
    s.hideTap(2.6);
    return `${headerHtml("lang", "ধাপ ২", "ভাষা বাছুন")}
${phoneHtml("lang", "language", `<div id="lang-cover" style="position:absolute;inset:0;background:#087F5B"></div>`)}`;
  },
});

// 4. SIGN-IN — email → code (no password)
SCENES.push({
  id: "signin", dur: 6.0,
  captions: [[0.2, 1.8, "আপনার ইমেইল দিন"], [1.85, 3.4, "কোড পাঠান চাপুন"], [3.45, 5.95, "ইমেইলের ৬ সংখ্যার কোড দিন"]],
  build(s) {
    s.header(6.0);
    s.enter("slide");
    s.appear(0.5, 203, 283);
    s.press(0.8, [27, 255, 353, 57, 12]);
    s.move(1.55, 203, 360);
    s.press(2.1, [27, 334, 353, 52, 26], "click-soft.mp3");
    s.hideTap(2.35);
    s.js(`tl.fromTo('#signin-note',{y:-110,opacity:0},{y:0,opacity:1,duration:0.45,ease:'expo.out'},2.5);`);
    s.sound(2.52, "pop.mp3", 0.5);
    s.js(`tl.to('#signin-note',{y:-110,opacity:0,duration:0.3,ease:'power3.in'},3.55);`);
    s.js(`tl.fromTo('#signin-sheet',{y:280,opacity:0},{y:0,opacity:1,duration:0.45,ease:'expo.out'},3.5);`);
    s.js(`gsap.utils.toArray('#signin-sheet .d').forEach((d,i)=>tl.fromTo(d,{opacity:0,y:8},{opacity:1,y:0,duration:0.12,ease:'power3.out'},3.85+i*0.1));`);
    s.sound(3.85, "typing.mp3", 0.4);
    s.js(`tl.fromTo('#signin-ok',{opacity:0,scale:0.85,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.3,ease:'back.out(1.6)'},4.45);`);
    s.sound(4.47, "sparkle.mp3", 0.45);
    const digits = "482916".split("").map((d) => `<div style="width:46px;height:58px;border-radius:12px;border:2px solid #087F5B;display:flex;align-items:center;justify-content:center;font-size:28px;font-weight:700;color:#053D2E"><span class="d" style="opacity:0">${d}</span></div>`).join("");
    return `${headerHtml("signin", "ধাপ ৩", "ইমেইল দিয়ে প্রবেশ")}
${phoneHtml("signin", "signin", `
  <div id="signin-note" style="position:absolute;left:12px;top:10px;width:381px;height:90px;border-radius:20px;background:#fff;box-shadow:0 6px 22px rgba(0,0,0,.22);display:flex;align-items:center;gap:12px;padding:0 14px;opacity:0;color:#1f2a27">
    <svg width="36" height="36" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="3" fill="#E7F5EF" stroke="#087F5B" stroke-width="1.6"/><path d="M3 7l9 6 9-6" stroke="#087F5B" stroke-width="1.6" fill="none"/></svg>
    <div><div style="font-size:17px;color:#5f6b67">নতুন ইমেইল · HisabBondhu</div><div style="font-size:23px;font-weight:700">প্রবেশ কোড: 482916</div></div>
  </div>
  <div id="signin-sheet" style="position:absolute;left:0;top:440px;width:405px;height:280px;background:#fff;border-radius:26px 26px 0 0;box-shadow:0 -10px 30px rgba(0,0,0,.18);color:#1f2a27;opacity:0">
    <div style="text-align:center;font-size:25px;font-weight:700;margin-top:22px">ইমেইলে আসা কোডটি দিন</div>
    <div style="display:flex;gap:9px;justify-content:center;margin-top:18px">${digits}</div>
    <div id="signin-ok" style="margin:22px auto 0;width:320px;height:52px;border-radius:26px;background:#E7F5EF;color:#087F5B;font-size:24px;font-weight:700;display:flex;align-items:center;justify-content:center;gap:8px;opacity:0">
      <svg width="22" height="22" viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5" stroke="#087F5B" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>পাসওয়ার্ড লাগে না</div>
  </div>`)}`;
  },
});

// 5. PROFILE
SCENES.push({
  id: "profile", dur: 3.8,
  captions: [[0.2, 2.0, "আপনার নাম ও মোবাইল নম্বর"], [2.05, 3.75, "এগিয়ে যান চাপুন"]],
  build(s) {
    s.header(3.8);
    s.enter("slide");
    s.appear(0.45, 203, 276);
    s.press(0.7, [27, 249, 353, 55, 12]);
    s.move(1.1, 203, 351, 0.36);
    s.press(1.55, [27, 323, 353, 56, 12], "click-soft.mp3");
    s.move(2.15, 203, 654);
    s.press(2.75, [17, 629, 372, 51, 26]);
    s.hideTap(3.0);
    return `${headerHtml("profile", "ধাপ ৪", "আপনার পরিচয়")}
${phoneHtml("profile", "profile")}`;
  },
});

// 6. SHOP SETUP
SCENES.push({
  id: "setup", dur: 4.2,
  captions: [[0.2, 2.2, "দোকানের নাম ও ধরন দিন"], [2.25, 4.15, "শুরু করুন — ব্যস!"]],
  build(s) {
    s.header(4.2);
    s.enter("slide");
    s.appear(0.45, 203, 177);
    s.press(0.7, [18, 149, 371, 56, 12]);
    s.move(1.2, 77, 425);
    s.press(1.72, [22, 410, 110, 30, 15], "click-soft.mp3");
    s.move(2.3, 204, 607);
    s.press(2.9, [18, 583, 372, 49, 25]);
    s.hideTap(3.15);
    s.js(`tl.fromTo('#setup-done',{opacity:0,scale:0.8,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.35,ease:'back.out(1.6)'},3.25);`);
    s.sound(3.27, "sparkle.mp3", 0.4);
    return `${headerHtml("setup", "ধাপ ৫", "দোকানের তথ্য")}
${phoneHtml("setup", "setup", `
  <div id="setup-done" style="position:absolute;left:62px;top:300px;width:280px;height:120px;border-radius:24px;background:rgba(8,127,91,.96);color:#fff;display:flex;flex-direction:column;align-items:center;justify-content:center;opacity:0;box-shadow:0 12px 30px rgba(0,0,0,.3)">
    <svg width="40" height="40" viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
    <div style="font-size:30px;font-weight:700">দোকান তৈরি!</div></div>`)}`;
  },
});

// 7. HOME — dashboard at a glance
SCENES.push({
  id: "home", dur: 5.6,
  captions: [[0.2, 2.4, "হাতে কত টাকা, পাবো, দেবো"], [2.45, 4.7, "আজকের বিক্রি — এক নজরে"], [4.75, 5.55, "এবার বিক্রি লিখি"]],
  build(s) {
    s.header(5.6);
    s.enter("settle");
    s.zoom(0.55, 165, 186, 1.5, 0.6);
    s.zoom(2.45, 110, 545, 1.5, 0.65);
    s.unzoom(4.15, 0.5);
    s.appear(4.75, 61, 328);
    s.press(5.12, [21, 291, 79, 73, 18]);
    return `${headerHtml("home", "হোম", "এক নজরে সব হিসাব")}
${phoneHtml("home", "home")}`;
  },
});

// 8. SALE ENTRY
SCENES.push({
  id: "sale", dur: 6.4,
  captions: [[0.2, 1.5, "টাকার পরিমাণ লিখুন"], [1.55, 3.3, "নগদ, বিকাশ না বাকি — বাছুন"], [3.35, 6.35, "সংরক্ষণ — মাত্র ১০ সেকেন্ডে"]],
  build(s) {
    s.header(6.4);
    s.enter("settle");
    s.appear(0.55, 204, 369);
    s.press(0.9, [18, 338, 371, 63, 12]);
    s.move(1.6, 152, 473, 0.4);
    s.press(2.2, [115, 459, 73, 27, 13], "click-soft.mp3");
    s.move(3.15, 204, 654, 0.45);
    s.press(3.85, [18, 628, 371, 52, 26]);
    s.hideTap(4.05);
    s.js(`tl.fromTo('#sale-toast',{opacity:0,y:30},{opacity:1,y:0,duration:0.4,ease:'expo.out'},4.15);`);
    s.sound(4.17, "sparkle.mp3", 0.45);
    return `${headerHtml("sale", "", "বিক্রি লিখুন")}
${phoneHtml("sale", "sale_entry", `
  <div id="sale-toast" style="position:absolute;left:24px;top:556px;width:357px;height:62px;border-radius:31px;background:#1f2a27;color:#fff;display:flex;align-items:center;justify-content:center;gap:10px;font-size:24px;font-weight:700;opacity:0;box-shadow:0 10px 24px rgba(0,0,0,.3)">
    <svg width="24" height="24" viewBox="0 0 24 24"><circle cx="12" cy="12" r="11" fill="#2FB380"/><path d="M6.5 12.5l3.5 3.5 7.5-8" stroke="#fff" stroke-width="2.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>বিক্রি সংরক্ষিত হয়েছে</div>`)}`;
  },
});

// 9. CUSTOMERS (baki khata)
SCENES.push({
  id: "customers", dur: 4.8,
  captions: [[0.2, 2.3, "সব কাস্টমারের বাকি এক খাতায়"], [2.35, 3.6, "মোট পাবো ৳৩,৭২৬"], [3.65, 4.75, "কাস্টমারে চাপুন"]],
  build(s) {
    s.header(4.8);
    s.enter("slide");
    s.zoom(0.45, 110, 185, 1.9, 0.6);
    s.mark(1.1, [18, 148, 183, 73], 1.1);
    s.unzoom(2.4, 0.45);
    s.appear(3.55, 204, 391);
    s.press(3.95, [0, 361, 405, 60, 0]);
    return `${headerHtml("customers", "", "বাকির খাতা")}
${phoneHtml("customers", "customers")}`;
  },
});

// 10. CUSTOMER LEDGER → WhatsApp reminder (app's real message text)
SCENES.push({
  id: "ledger", dur: 7.2,
  captions: [[0.2, 2.0, "কার কাছে কত বাকি — দেখুন"], [2.05, 4.6, "এক চাপে WhatsApp-এ পাঠান"], [4.65, 7.15, "ভদ্র বাংলায় তাগাদা মেসেজ"]],
  build(s) {
    s.header(7.2);
    s.enter("settle");
    s.zoom(0.4, 203, 160, 1.6, 0.6);
    s.unzoom(1.55, 0.45);
    s.appear(1.95, 87, 351);
    s.press(2.35, [57, 336, 61, 55, 12]);
    s.hideTap(2.6);
    s.js(`tl.fromTo('#ledger-dim',{opacity:0},{opacity:1,duration:0.25},2.6);`);
    s.js(`tl.fromTo('#ledger-chat',{y:320,opacity:0},{y:0,opacity:1,duration:0.5,ease:'expo.out'},2.65);`);
    s.sound(2.67, "whoosh-short.mp3", 0.45);
    s.js(`tl.fromTo('#ledger-send',{scale:1,transformOrigin:'50% 50%'},{scale:0.82,duration:0.08,ease:'power2.in'},5.4); tl.to('#ledger-send',{scale:1,duration:0.2},5.48);`);
    s.sound(5.43, "click-soft.mp3", 0.5);
    s.js(`tl.fromTo('#ledger-ticks',{opacity:0},{opacity:1,duration:0.15},5.65);`);
    s.sound(5.66, "pop.mp3", 0.45);
    return `${headerHtml("ledger", "", "বাকির তাগাদা")}
${phoneHtml("ledger", "customer_ledger", `
  <div id="ledger-dim" style="position:absolute;inset:0;background:rgba(0,0,0,.45);opacity:0"></div>
  <div id="ledger-chat" style="position:absolute;left:0;top:250px;width:405px;height:470px;background:#ECE5DD;border-radius:24px 24px 0 0;opacity:0;color:#1f2a27">
    <div style="height:62px;background:#075E54;border-radius:24px 24px 0 0;display:flex;align-items:center;gap:12px;padding:0 16px;color:#fff">
      <div style="width:38px;height:38px;border-radius:50%;background:#FFE8CC;color:#D9480F;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:20px">স</div>
      <div style="font-size:20px;font-weight:700">সালমা আপা</div></div>
    <div style="position:absolute;left:46px;right:14px;top:80px;background:#DCF8C6;border-radius:16px 4px 16px 16px;padding:14px 16px 26px;font-size:21px;line-height:1.5;box-shadow:0 1px 2px rgba(0,0,0,.12)">
      প্রিয় সালমা আপা, ২০ নভেম্বর ২০২৬ পর্যন্ত আপনার মোট বাকি ৳২,২০০। সুবিধামতো সময়ে পরিশোধ করার অনুরোধ রইলো। ধন্যবাদ।
      <div id="ledger-ticks" style="position:absolute;right:12px;bottom:6px;font-size:16px;font-weight:700;color:#1864AB;opacity:0">✓✓</div></div>
    <div style="position:absolute;left:12px;right:12px;bottom:18px;height:54px;display:flex;gap:10px;align-items:center">
      <div style="flex:1;height:50px;border-radius:25px;background:#fff"></div>
      <div id="ledger-send" style="width:50px;height:50px;border-radius:50%;background:#128C7E;display:flex;align-items:center;justify-content:center">
        <svg width="24" height="24" viewBox="0 0 24 24"><path d="M3 20l18-8L3 4v6l12 2-12 2z" fill="#fff"/></svg></div>
    </div>
  </div>`)}`;
  },
});

// 11. STOCK
SCENES.push({
  id: "stock", dur: 4.4,
  captions: [[0.2, 2.3, "বিক্রি করলেই স্টক কমে"], [2.35, 4.35, "কম স্টকে সতর্কতা আসে"]],
  build(s) {
    s.header(4.4);
    s.enter("slide");
    s.zoom(0.45, 150, 150, 1.6, 0.55);
    s.zoom(1.75, 230, 340, 1.2, 0.6);
    s.mark(2.45, [8, 316, 389, 50], 1.2);
    s.sound(2.47, "pop.mp3", 0.45);
    s.unzoom(3.75, 0.45);
    return `${headerHtml("stock", "", "স্টকের হিসাব")}
${phoneHtml("stock", "stock")}`;
  },
});

// 12. REPORTS
SCENES.push({
  id: "reports", dur: 4.6,
  captions: [[0.2, 1.9, "দিন, সপ্তাহ, মাসের রিপোর্ট"], [1.95, 4.55, "এই মাসের লাভ — এক চাপে"]],
  build(s) {
    s.header(4.6);
    s.enter("slide");
    s.appear(0.45, 301, 125);
    s.press(0.8, [266, 112, 69, 27, 13], "click-soft.mp3");
    s.hideTap(1.05);
    s.zoom(1.45, 300, 280, 1.9, 0.6);
    s.mark(2.1, [208, 250, 183, 72], 1.6);
    s.sound(2.12, "pop.mp3", 0.45);
    s.unzoom(3.95, 0.45);
    return `${headerHtml("reports", "", "রিপোর্ট ও লাভ")}
${phoneHtml("reports", "reports")}`;
  },
});

// 13. OFFLINE → SYNC (app's real offline banner text)
SCENES.push({
  id: "offline", dur: 5.6,
  captions: [[0.2, 3.35, "ইন্টারনেট নেই? সমস্যা নেই"], [3.4, 5.55, "নেট এলে নিজেই ব্যাকআপ"]],
  build(s) {
    s.header(5.6);
    s.enter("fade");
    s.js(`tl.fromTo('#offline-banner',{y:-96},{y:0,duration:0.4,ease:'expo.out'},0.2);`);
    s.js(`tl.fromTo('#offline-badge',{opacity:0,scale:0.6,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.35,ease:'back.out(1.6)'},0.35);`);
    s.sound(0.37, "pop.mp3", 0.45);
    s.js(`tl.to('#offline-banner',{y:-96,duration:0.3,ease:'power3.in'},3.4);`);
    s.js(`tl.to('#offline-chip',{opacity:0,duration:0.2},3.55);`);
    s.js(`tl.to('#offline-off',{opacity:0,duration:0.15},3.45); tl.fromTo('#offline-on',{opacity:0,scale:0.7,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.3,ease:'back.out(1.6)'},3.47);`);
    s.sound(3.49, "sparkle.mp3", 0.45);
    s.zoom(3.75, 344, 56, 2.2, 0.6);
    s.mark(4.35, [301, 42, 86, 28], 1.0);
    return `${headerHtml("offline", "", "নেট ছাড়াই চলে")}
${phoneHtml("offline", "home", `
  <div id="offline-chip" style="position:absolute;left:292px;top:41px;width:108px;height:30px;border-radius:15px;background:#FFF4E6;color:#D9480F;font-size:13px;font-weight:700;display:flex;align-items:center;justify-content:center">অফলাইন · সংরক্ষিত</div>
  <div id="offline-banner" style="position:absolute;left:0;top:0;width:405px;height:96px;background:${C.orange};color:#fff;font-size:18px;font-weight:600;line-height:1.35;padding:28px 16px 0;box-sizing:border-box">ইন্টারনেট নেই — সব হিসাব ফোনে নিরাপদে সংরক্ষিত হচ্ছে, নেট এলে ব্যাকআপ হবে।</div>`)}
<div id="offline-badge" style="position:absolute;left:150px;top:560px;width:112px;height:112px;border-radius:50%;background:#fff;box-shadow:0 10px 30px rgba(0,0,0,.3)">
  <svg id="offline-off" style="position:absolute;left:24px;top:24px" width="64" height="64" viewBox="0 0 24 24"><path d="M2 8.5a15 15 0 0 1 20 0M5 12a10.5 10.5 0 0 1 14 0M8.5 15.5a5.5 5.5 0 0 1 7 0" stroke="${C.orange}" stroke-width="2" fill="none" stroke-linecap="round"/><circle cx="12" cy="19" r="1.6" fill="${C.orange}"/><path d="M3 3l18 18" stroke="${C.red}" stroke-width="2.4" stroke-linecap="round"/></svg>
  <svg id="offline-on" style="position:absolute;left:24px;top:24px;opacity:0" width="64" height="64" viewBox="0 0 24 24"><path d="M7 18a4.5 4.5 0 0 1-.6-9 6 6 0 0 1 11.4 1.5A4 4 0 0 1 17 18z" fill="#E7F5EF" stroke="${C.green}" stroke-width="1.8"/><path d="M9 13.5l2.2 2.2 4-4" stroke="${C.green}" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
</div>`;
  },
});

// 14. CTA — the phone becomes the icon
SCENES.push({
  id: "cta", dur: 5.6,
  captions: [[0.4, 5.5, "আজই ফ্রি ডাউনলোড করুন"]],
  build(s) {
    s.js(`tl.fromTo('#cta-phone',{scale:1,opacity:1,y:0,filter:'blur(0px)',transformOrigin:'50% 30%'},{scale:0.3,opacity:0,y:-120,filter:'blur(8px)',duration:0.5,ease:'power3.in'},0);`);
    s.js(`tl.fromTo('#cta-logo',{opacity:0,scale:0.5,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.6,ease:'back.out(1.5)'},0.3);`);
    s.sound(0.32, "impact-bass-1.mp3", 0.5);
    s.js(`gsap.utils.toArray('#cta-name .w').forEach((w,i)=>tl.fromTo(w,{opacity:0,filter:'blur(8px)'},{opacity:1,filter:'blur(0px)',duration:0.18,ease:'power4.out'},0.7+i*0.08));`);
    s.js(`tl.fromTo('#cta-tag',{opacity:0,filter:'blur(6px)'},{opacity:1,filter:'blur(0px)',duration:0.2,ease:'power4.out'},1.05);`);
    s.js(`tl.fromTo('#cta-btn',{opacity:0,y:30},{opacity:1,y:0,duration:0.45,ease:'expo.out'},1.45);`);
    s.sound(1.47, "pop.mp3", 0.5);
    s.js(`tl.fromTo('#cta-feat',{opacity:0},{opacity:1,duration:0.3},1.85);`);
    return `<div id="cta-wrap" style="position:absolute;inset:0">${phoneHtml("cta", "home")}</div>
<div style="position:absolute;left:120px;width:768px;top:400px;text-align:center">
  <img id="cta-logo" src="assets/icon.png" alt="" style="width:220px;height:220px;border-radius:54px;box-shadow:0 18px 50px rgba(0,0,0,.35)">
  <div id="cta-name" style="font-size:112px;font-weight:700;line-height:1.1;margin-top:26px">${words("হিসাববন্ধু")}</div>
  <div id="cta-tag" style="font-size:42px;font-weight:600;line-height:1.3;margin-top:8px;opacity:.95">দোকানের হিসাব,<br>সবসময় হাতের কাছে</div>
  <div id="cta-btn" style="margin:44px auto 0;width:640px;height:96px;border-radius:48px;background:#fff;color:${C.green};font-size:40px;font-weight:700;display:flex;align-items:center;justify-content:center;gap:16px;box-shadow:0 14px 40px rgba(0,0,0,.3)">
    <svg width="40" height="40" viewBox="0 0 24 24"><path d="M12 3v12m0 0l-5-5m5 5l5-5M5 20h14" stroke="${C.green}" stroke-width="2.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>Play Store-এ ফ্রি ডাউনলোড</div>
  <div id="cta-feat" style="font-size:32px;font-weight:600;margin-top:22px;color:${C.amber}">বাংলায় · নেট ছাড়াই · একদম ফ্রি</div>
</div>`;
  },
});

// ---------- assemble ----------
let t = 0;
const placed = SCENES.map((sc) => { const start = +t.toFixed(2); t += sc.dur; return { ...sc, start }; });
const TOTAL = +t.toFixed(2);
const allSfx = [], allCaps = [];

for (const sc of placed) {
  const s = scene(sc.id);
  const html = sc.build(s);
  const extra = s.extraHtml.join("\n");
  // press highlights and marks live inside the zoom layer of the phone
  const body = extra ? html.replace(`<div id="${sc.id}-tap" class="tap">`, `${extra}\n  <div id="${sc.id}-tap" class="tap">`) : html;
  const file = `<template>
<style>${BASE_CSS}</style>
<div id="root" data-composition-id="${sc.id}" data-width="${W}" data-height="${H}">
${body}
</div>
<script>
  const tl = gsap.timeline({ paused: true });
  ${s.lines.join("\n  ")}
  window.__timelines["${sc.id}"] = tl;
</script>
</template>
`;
  writeFileSync(join(ROOT, "compositions", `${sc.id}.html`), file);
  for (const e of s.sfx) allSfx.push({ ...e, t: +(sc.start + e.t).toFixed(2) });
  for (const [a, b, text] of sc.captions) allCaps.push({ start: +(sc.start + a).toFixed(2), end: +(sc.start + b).toFixed(2), text });
}
// riser into the CTA impact (the only one in the film)
const cta = placed.find((p) => p.id === "cta");
allSfx.push({ t: +(cta.start - 2.2).toFixed(2), file: "riser.mp3", vol: 0.35, mediaStart: 2.6, dur: 2.4 });
allSfx.sort((a, b) => a.t - b.t);

// ---------- captions composition (kinetic: words blur in, the current word turns amber) ----------
const capGroups = allCaps.map((c, i) => {
  const ws = c.text.split(" ").map((w, j) => `<span id="cap${i}-w${j}" class="cw">${w}</span>`).join(" ");
  return `<div id="cap${i}" class="cg">${ws}</div>`;
}).join("\n");
const capJs = allCaps.map((c, i) => {
  const n = c.text.split(" ").length;
  const step = Math.max(0.18, (c.end - c.start - 0.35) / n);
  const out = [`tl.set('#cap${i}',{opacity:1,visibility:'visible'},${c.start});`];
  for (let j = 0; j < n; j++) {
    const ta = +(c.start + j * 0.08).toFixed(2);
    out.push(`tl.fromTo('#cap${i}-w${j}',{opacity:0,y:14,filter:'blur(6px)'},{opacity:1,y:0,filter:'blur(0px)',duration:0.16,ease:'power4.out'},${ta});`);
    const th = +(c.start + 0.12 + j * step).toFixed(2);
    out.push(`tl.to('#cap${i}-w${j}',{color:'${C.amber}',scale:1.08,duration:0.12,ease:'power2.out'},${th});`);
    if (j < n - 1) out.push(`tl.to('#cap${i}-w${j}',{color:'#ffffff',scale:1,duration:0.15,ease:'power2.out'},${+(th + step).toFixed(2)});`);
  }
  out.push(`tl.to('#cap${i}',{opacity:0,duration:0.12,ease:'power2.in'},${+(c.end - 0.12).toFixed(2)});`);
  out.push(`tl.set('#cap${i}',{opacity:0,visibility:'hidden'},${c.end});`);
  return out.join("\n  ");
}).join("\n  ");

writeFileSync(join(ROOT, "compositions", "captions.html"), `<template>
<style>
${FONT_FACE}
#root{position:absolute;inset:0;font-family:'Hind Siliguri',sans-serif}
#root .cg{position:absolute;left:120px;width:660px;top:1164px;height:72px;display:flex;align-items:center;justify-content:center;flex-wrap:nowrap;white-space:nowrap;column-gap:14px;text-align:center;opacity:0;visibility:hidden;overflow:visible}
#root .cw{display:inline-block;color:#fff;font-size:48px;font-weight:700;line-height:1.2;text-shadow:0 3px 14px rgba(0,0,0,.45)}
</style>
<div id="root" data-composition-id="captions" data-width="${W}" data-height="${H}">
${capGroups}
</div>
<script>
  const tl = gsap.timeline({ paused: true });
  ${capJs}
  window.__timelines["captions"] = tl;
</script>
</template>
`);

// ---------- footer: a quiet brand line under the captions ----------
{
  const fdur = +(cta.start - placed[1].start).toFixed(2);
  writeFileSync(join(ROOT, "compositions", "footer.html"), `<template>
<style>
${FONT_FACE}
#root{position:absolute;inset:0;font-family:'Hind Siliguri',sans-serif}
#root .ft{position:absolute;left:0;width:${W}px;top:1318px;height:80px;display:flex;align-items:center;justify-content:center;gap:16px;color:#fff}
</style>
<div id="root" data-composition-id="footer" data-width="${W}" data-height="${H}">
  <div id="footer-row" class="ft" data-layout-allow-safe-zone>
    <img id="footer-icon" src="assets/icon.png" alt="" style="width:60px;height:60px;border-radius:15px;box-shadow:0 6px 16px rgba(0,0,0,.3)">
    <div style="font-size:40px;font-weight:700">হিসাববন্ধু</div>
    <div style="font-size:30px;font-weight:600;color:${C.amber}">· Play Store-এ ফ্রি</div>
  </div>
</div>
<script>
  const tl = gsap.timeline({ paused: true });
  tl.fromTo('#footer-row',{opacity:0,y:16},{opacity:0.9,y:0,duration:0.4,ease:'expo.out'},0.1);
  tl.to('#footer-row',{opacity:0,duration:0.2,ease:'power2.in'},${(fdur - 0.22).toFixed(2)});
  window.__timelines["footer"] = tl;
</script>
</template>
`);
}

// ---------- root ----------
const hosts = placed.map((sc) => `      <div id="${sc.id}" data-composition-id="${sc.id}" data-composition-src="compositions/${sc.id}.html" data-start="${sc.start}" data-duration="${sc.dur}" data-track-index="1" data-track-kind="graphics" data-width="${W}" data-height="${H}" style="z-index:2"></div>`).join("\n");
// sound effects: one lane, a second only where two overlap
const lanes = [];
const audio = allSfx.map((e, i) => {
  const dur = e.dur ?? { "click.mp3": 0.36, "click-soft.mp3": 0.36, "whoosh.mp3": 0.57, "whoosh-short.mp3": 0.57, "pop.mp3": 0.72, "typing.mp3": 1.54, "sparkle.mp3": 1.8, "impact-bass-1.mp3": 2.1, "riser.mp3": 2.4 }[e.file];
  let lane = lanes.findIndex((end) => end <= e.t);
  if (lane === -1) { lanes.push(0); lane = lanes.length - 1; }
  lanes[lane] = e.t + dur;
  return `      <audio id="sfx${i + 1}" class="clip" src="assets/sfx/${e.file}" data-start="${e.t}" data-duration="${dur}"${e.mediaStart ? ` data-media-start="${e.mediaStart}" data-fade-in="0.4"` : ""} data-volume="${e.vol}" data-track-index="${4 + lane}"></audio>`;
}).join("\n");

writeFileSync(join(ROOT, "index.html"), `<!doctype html>
<html lang="bn">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=${W}, height=${H}" />
    <script src="assets/vendor/gsap.min.js"></script>
    <style>
${FONT_FACE}
      * { margin: 0; padding: 0; box-sizing: border-box; }
      html, body { margin: 0; width: ${W}px; height: ${H}px; overflow: hidden; background: ${C.deep}; }
      #root { position: relative; width: ${W}px; height: ${H}px; font-family: 'Hind Siliguri', sans-serif; }
      #bg { position: absolute; inset: 0; z-index: 0;
        background:
          radial-gradient(rgba(255,255,255,.10) 1.4px, transparent 1.7px) 0 0 / 36px 36px,
          radial-gradient(900px 700px at 85% 12%, rgba(25,113,194,.45), rgba(25,113,194,0) 70%),
          radial-gradient(800px 800px at 10% 92%, rgba(255,212,59,.16), rgba(255,212,59,0) 70%),
          linear-gradient(180deg, #0A6E50 0%, ${C.green} 38%, ${C.deep} 100%); }
    </style>
  </head>
  <body>
    <div id="root" data-composition-id="main" data-start="0" data-duration="${TOTAL}" data-width="${W}" data-height="${H}">
      <div id="bg" class="clip" data-start="0" data-duration="${TOTAL}" data-track-index="0"></div>
${hosts}
      <div id="footer" data-composition-id="footer" data-composition-src="compositions/footer.html" data-start="${placed[1].start}" data-duration="${+(cta.start - placed[1].start).toFixed(2)}" data-track-index="3" data-track-kind="graphics" data-width="${W}" data-height="${H}" style="z-index:4"></div>
      <div id="captions" data-composition-id="captions" data-composition-src="compositions/captions.html" data-start="0" data-duration="${TOTAL}" data-track-index="2" data-track-kind="captions" data-width="${W}" data-height="${H}" style="z-index:5"></div>
${audio}
    </div>
    <script>
      window.__timelines = window.__timelines || {};
      window.__timelines["main"] = gsap.timeline({ paused: true });
    </script>
  </body>
</html>
`);

console.log(`scenes: ${placed.length}, length: ${TOTAL}s, captions: ${allCaps.length}, sfx: ${allSfx.length}`);
for (const p of placed) console.log(`  ${p.start.toFixed(2).padStart(6)}  ${p.id}`);
