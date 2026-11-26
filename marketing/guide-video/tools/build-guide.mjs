// HisabBondhu user guide (16:9, Bangla, narrated by the ElevenLabs "Rahat" voice).
// Usage: node tools/build-guide.mjs  → index.html, compositions/*.html, subtitles.srt, tools/narration.json
//
// Each scene has a narration line. When assets/vo/<id>.mp3 exists its real length times the scene;
// otherwise the length is estimated from the text, so the film can be previewed before the voice exists.
// Every on-screen action is keyed to a word of the narration and lands when the voice says it.
import { writeFileSync, existsSync, mkdirSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
mkdirSync(join(ROOT, "compositions"), { recursive: true });

const W = 1920, H = 1080;
const C = { green: "#087F5B", deep: "#053D2E", amber: "#FFD43B", red: "#E03131", orange: "#F08C00" };
const SW = 504, SH = 896, F = SW / 1080;          // phone screen; screenshots are 1080x1920
const PHONE = { left: 210, top: 72 };
const LEAD = 0.45, TAIL = 0.75, CPS = 13.5;      // voice starts LEAD s into a scene; est. chars per second
const PANEL = { left: 860, width: 860 };
const BN = "০১২৩৪৫৬৭৮৯";
const bnNum = (n) => String(n).replace(/\d/g, (d) => BN[d]);

const FONT_FACE = ["regular:400", "semibold:600", "bold:700"].map((s) => {
  const [f, w] = s.split(":");
  return `@font-face{font-family:'Hind Siliguri';src:url('assets/fonts/hind_siliguri_${f}.ttf') format('truetype');font-weight:${w};font-style:normal}`;
}).join("\n");

const plain = (t) => t.replace(/\[[^\]]*\]\s*/g, "").trim();

// ------------------------------------------------------------------------------------------
// SCENES — narration (vo), screen, panel text and the actions keyed to narration words.
// Coordinates are pixels of the 1080x1920 screenshot.
// ------------------------------------------------------------------------------------------
const SCENES = [
  {
    id: "s01", screen: "home", chip: "শুরু", title: "হিসাববন্ধু ব্যবহারের পূর্ণ গাইড",
    vo: "[warmly] আসসালামু আলাইকুম। হিসাববন্ধু অ্যাপে আপনাকে স্বাগতম। এই ভিডিওতে দেখবো, কীভাবে দোকানের নগদ বিক্রি, বাকি, টাকা আদায়, খরচ আর স্টকের হিসাব রাখবেন — একদম শুরু থেকে, ধাপে ধাপে।",
    steps: [["নগদ ও বাকি বিক্রি", "নগদ বিক্রি"], ["বাকির খাতা ও আদায়", "টাকা আদায়"], ["খরচ ও মাল কেনা", "খরচ আর"], ["স্টক ও রিপোর্ট", "স্টকের"]],
    intro: true,
  },
  {
    id: "s02", screen: null, chip: "ধাপ ১", title: "অ্যাপ ইনস্টল করুন",
    vo: "প্রথমে Play Store খুলুন। সার্চে লিখুন HisabBondhu, তারপর Install চাপুন। অ্যাপটি সম্পূর্ণ ফ্রি। ইনস্টল হলে Open চাপুন।",
    steps: [["Play Store খুলুন", "Play Store"], ["HisabBondhu লিখে খুঁজুন", "সার্চে"], ["Install চাপুন", "Install চাপুন"], ["Open চাপুন", "Open"]],
    overlay: "store",
  },
  {
    id: "s03", screen: "language", chip: "ধাপ ২", title: "ভাষা বাছুন",
    vo: "অ্যাপ খুললে প্রথমে ভাষা বাছাই করুন — বাংলা অথবা English। তারপর নিচের শুরু করুন বাটনে চাপুন।",
    steps: [["বাংলা বা English বাছুন", "ভাষা বাছাই"], ["শুরু করুন চাপুন", "শুরু করুন"]],
    acts: [{ k: "tap", at: "বাংলা অথবা", x: 535, y: 1402, rect: [60, 1310, 950, 185] }, { k: "tap", at: "শুরু করুন", x: 540, y: 1850, rect: [45, 1800, 990, 110] }],
  },
  {
    id: "s04", screen: "signin", chip: "ধাপ ৩", title: "ইমেইল দিয়ে প্রবেশ",
    vo: "এবার আপনার ইমেইল ঠিকানা লিখুন, আর কোড পাঠান চাপুন। ইমেইলে একটি ছয় সংখ্যার কোড আসবে। কোডটি দিলেই প্রবেশ — কোনো পাসওয়ার্ড মনে রাখতে হবে না।",
    steps: [["ইমেইল লিখুন", "ইমেইল ঠিকানা"], ["কোড পাঠান চাপুন", "কোড পাঠান"], ["ইমেইলের ৬ সংখ্যার কোড দিন", "ছয় সংখ্যার"], ["পাসওয়ার্ড লাগে না", "পাসওয়ার্ড"]],
    acts: [{ k: "tap", at: "ইমেইল ঠিকানা", x: 541, y: 754, rect: [71, 680, 941, 152] }, { k: "tap", at: "কোড পাঠান", x: 541, y: 960, rect: [71, 890, 941, 140] }],
    overlay: "code",
  },
  {
    id: "s05", screen: "profile", chip: "ধাপ ৪", title: "আপনার পরিচয়",
    vo: "তারপর আপনার নাম আর মোবাইল নম্বর দিন। নতুন সুবিধা আর অফারের খবর পেতে চাইলে নিচের ঘরে টিক দিন। এরপর এগিয়ে যান চাপুন।",
    steps: [["আপনার নাম", "আপনার নাম"], ["মোবাইল নম্বর", "মোবাইল নম্বর"], ["খবর পেতে চাইলে টিক (ঐচ্ছিক)", "টিক"], ["এগিয়ে যান চাপুন", "এগিয়ে যান"]],
    acts: [
      { k: "tap", at: "আপনার নাম", x: 541, y: 737, rect: [71, 664, 941, 146] },
      { k: "tap", at: "মোবাইল নম্বর", x: 541, y: 936, rect: [71, 861, 941, 149] },
      { k: "tap", at: "টিক", x: 115, y: 1172, rect: [70, 1126, 92, 92] },
      { k: "tap", at: "এগিয়ে যান", x: 541, y: 1745, rect: [45, 1677, 991, 136] },
    ],
  },
  {
    id: "s06", screen: "setup", chip: "ধাপ ৫", title: "দোকানের তথ্য",
    vo: "এখানে দোকানের নাম, মালিকের নাম, মোবাইল নম্বর আর ব্যবসার ধরন দিন। এই ধাপ চাইলে পরেও করা যায়। শুরু করুন চাপলেই আপনার ডিজিটাল খাতা তৈরি।",
    steps: [["দোকানের নাম", "দোকানের নাম"], ["মালিকের নাম ও মোবাইল", "মালিকের নাম"], ["ব্যবসার ধরন", "ব্যবসার ধরন"], ["শুরু করুন — খাতা তৈরি", "শুরু করুন চাপলেই"]],
    acts: [
      { k: "tap", at: "দোকানের নাম", x: 541, y: 472, rect: [49, 398, 991, 149] },
      { k: "tap", at: "মালিকের নাম", x: 541, y: 672, rect: [49, 598, 991, 149] },
      { k: "tap", at: "মোবাইল নম্বর", x: 541, y: 876, rect: [49, 803, 991, 147] },
      { k: "tap", at: "ব্যবসার ধরন", x: 207, y: 1133, rect: [58, 1094, 292, 81] },
      { k: "mark", at: "পরেও", rect: [380, 1700, 330, 90], hold: 1.4 },
      { k: "tap", at: "শুরু করুন চাপলেই", x: 541, y: 1619, rect: [49, 1554, 991, 130] },
    ],
  },
  {
    id: "s07", screen: "home", chip: "হোম", title: "এক নজরে সব হিসাব",
    vo: "এটাই হোম পেজ। সবার উপরে হাতে থাকা টাকা। পাবো মানে কাস্টমারদের কাছে আপনার মোট বাকি, আর দেবো মানে সাপ্লায়ারদের কাছে আপনার দেনা। নিচে আজকের হিসাবে আজকের বিক্রি, নগদ আদায়, বাকি বিক্রি, খরচ আর আনুমানিক লাভ এক নজরে।",
    steps: [["হাতে থাকা টাকা", "হাতে থাকা"], ["পাবো = কাস্টমারের বাকি", "পাবো মানে"], ["দেবো = সাপ্লায়ারের দেনা", "দেবো মানে"], ["আজকের বিক্রি ও লাভ", "আজকের হিসাবে"]],
    acts: [
      { k: "zoom", at: "হাতে থাকা", px: 540, py: 560, s: 1.15 },
      { k: "mark", at: "হাতে থাকা", rect: [80, 300, 480, 190], hold: 1.6 },
      { k: "mark", at: "পাবো মানে", rect: [84, 551, 444, 113], hold: 2.0 },
      { k: "mark", at: "দেবো মানে", rect: [560, 551, 450, 113], hold: 2.0 },
      { k: "zoom", at: "আজকের হিসাবে", px: 540, py: 1450, s: 1.35 },
      { k: "mark", at: "আজকের বিক্রি", rect: [49, 1214, 991, 470], hold: 3.0 },
    ],
  },
  {
    id: "s08", screen: "home", chip: "হোম", title: "হিসাব লেখার বাটন",
    vo: "সব হিসাব লেখা শুরু হয় এই বাটনগুলো থেকে — বিক্রি, টাকা পেলাম, খরচ আর কেনা। পাশের সারিতে আছে টাকা দিলাম, বিক্রি ফেরত, কেনা ফেরত, স্টক সমন্বয়, আর ক্যাশ যোগ বা তোলা।",
    steps: [["বিক্রি", "বিক্রি, টাকা"], ["টাকা পেলাম", "টাকা পেলাম"], ["খরচ ও কেনা", "খরচ আর"], ["ফেরত, স্টক, ক্যাশ", "বিক্রি ফেরত"]],
    acts: [
      { k: "zoom", at: 0.1, px: 540, py: 960, s: 1.3 },
      { k: "mark", at: "বিক্রি, টাকা", rect: [55, 777, 211, 194], hold: 0.9 },
      { k: "mark", at: "টাকা পেলাম", rect: [308, 777, 211, 194], hold: 0.9 },
      { k: "mark", at: "খরচ আর", rect: [568, 777, 210, 194], hold: 0.8 },
      { k: "mark", at: "আর কেনা", rect: [827, 777, 211, 194], hold: 0.9 },
      { k: "mark", at: "টাকা দিলাম", rect: [49, 1080, 301, 61], hold: 0.9 },
      { k: "mark", at: "বিক্রি ফেরত", rect: [373, 1080, 308, 61], hold: 0.9 },
      { k: "mark", at: "কেনা ফেরত", rect: [707, 1080, 298, 61], hold: 1.2 },
    ],
  },
  {
    id: "s09", screen: "sale_entry", chip: "বিক্রি", title: "নগদ বিক্রি লিখুন",
    vo: "নগদ বিক্রি লিখতে বিক্রি বাটনে চাপুন। উপরে নগদ বাছাই করা থাকে। বিক্রির টাকা লিখুন, তারপর পেমেন্ট মাধ্যম বাছুন — নগদ, বিকাশ, নগদ মোবাইল, রকেট, ব্যাংক বা কার্ড। শেষে সংরক্ষণ করুন চাপুন। ব্যস, বিক্রি লেখা হয়ে গেল।",
    steps: [["নগদ বাছাই থাকে", "উপরে নগদ"], ["টাকার পরিমাণ লিখুন", "বিক্রির টাকা"], ["পেমেন্ট মাধ্যম বাছুন", "পেমেন্ট মাধ্যম"], ["সংরক্ষণ করুন", "শেষে সংরক্ষণ"]],
    acts: [
      { k: "tap", at: "উপরে নগদ", x: 295, y: 334, rect: [49, 285, 492, 98] },
      { k: "tap", at: "বিক্রির টাকা", x: 541, y: 981, rect: [49, 900, 988, 169] },
      { k: "mark", at: "পেমেন্ট মাধ্যম", rect: [40, 1215, 1000, 185], hold: 1.6 },
      { k: "tap", at: "বিকাশ", x: 405, y: 1260, rect: [308, 1224, 195, 72] },
      { k: "tap", at: "শেষে সংরক্ষণ", x: 541, y: 1745, rect: [49, 1674, 988, 139] },
    ],
    overlay: "toast",
  },
  {
    id: "s10", screen: "sale_entry", chip: "বিক্রি", title: "পণ্যসহ বিক্রি",
    vo: "চাইলে পণ্য বাছাই করুন চেপে পণ্য আর পরিমাণ দিন। তাহলে দাম নিজে থেকেই বসে যাবে, আর বিক্রির সাথে সাথে স্টক থেকে কমে যাবে।",
    steps: [["পণ্য বাছাই করুন", "পণ্য বাছাই"], ["পরিমাণ দিন", "পরিমাণ দিন"], ["দাম নিজে বসে যায়", "দাম নিজে"], ["স্টক নিজে কমে", "স্টক থেকে"]],
    acts: [
      { k: "zoom", at: 0.1, px: 540, py: 600, s: 1.4 },
      { k: "tap", at: "পণ্য বাছাই", x: 908, y: 745, rect: [49, 687, 988, 107] },
      { k: "mark", at: "পণ্য বাছাই", rect: [49, 687, 988, 107], hold: 2.4 },
      { k: "unzoom", at: "স্টক থেকে" },
    ],
  },
  {
    id: "s11", screen: "sale_entry", chip: "বিক্রি", title: "বাকিতে বিক্রি",
    vo: "এবার বাকিতে বিক্রি। উপরের বাকি অপশনে চাপুন। তারপর কাস্টমার বাছাই করুন — তালিকা থেকে নাম বাছুন, অথবা সেখানেই নতুন কাস্টমার যোগ করুন। মোট টাকা লিখুন। কাস্টমার এখন কিছু টাকা দিলে এখন জমা পেলাম ঘরে লিখুন। বাকি টাকাটা নিজে থেকেই তার খাতায় উঠে যাবে।",
    steps: [["বাকি বাছুন", "বাকি অপশনে"], ["কাস্টমার বাছাই করুন", "কাস্টমার বাছাই"], ["মোট টাকা লিখুন", "মোট টাকা"], ["এখন যা পেলেন তা লিখুন", "এখন জমা"], ["বাকি খাতায় উঠে যায়", "খাতায়"]],
    acts: [
      { k: "tap", at: "বাকি অপশনে", x: 794, y: 334, rect: [551, 285, 486, 98] },
      { k: "tap", at: "কাস্টমার বাছাই", x: 908, y: 518, rect: [49, 420, 988, 190] },
      { k: "tap", at: "মোট টাকা", x: 541, y: 981, rect: [49, 900, 988, 169] },
    ],
    overlay: "paid",
  },
  {
    id: "s12", screen: "customers", chip: "খাতা", title: "বাকির খাতা",
    vo: "নিচের খাতা ট্যাবে সব কাস্টমারের তালিকা। উপরে মোট পাবো, আর প্রত্যেকের নামের পাশে লাল রঙে লেখা তার বাকি। নাম বা মোবাইল নম্বর দিয়ে খুঁজুন, বেশি বাকি চাপলে যার বাকি বেশি সে উপরে আসবে। নতুন কাস্টমার যোগ করতে নিচের নতুন কাস্টমার বাটন চাপুন। পাশের ট্যাবে সাপ্লায়ারদের তালিকা।",
    steps: [["মোট পাবো", "মোট পাবো"], ["কার কত বাকি", "লাল রঙে"], ["খুঁজুন ও সাজান", "খুঁজুন"], ["নতুন কাস্টমার যোগ", "নতুন কাস্টমার যোগ"], ["সাপ্লায়ারদের তালিকা", "সাপ্লায়ারদের"]],
    acts: [
      { k: "mark", at: "মোট পাবো", rect: [49, 395, 486, 195], hold: 1.8 },
      { k: "mark", at: "লাল রঙে", rect: [880, 975, 180, 545], hold: 2.0 },
      { k: "mark", at: "খুঁজুন", rect: [40, 650, 1000, 140], hold: 1.2 },
      { k: "tap", at: "বেশি বাকি", x: 162, y: 872, rect: [49, 840, 227, 65] },
      { k: "tap", at: "নতুন কাস্টমার বাটন", x: 859, y: 1530, rect: [650, 1460, 420, 140] },
      { k: "tap", at: "সাপ্লায়ারদের", x: 811, y: 292, rect: [540, 240, 540, 100] },
    ],
  },
  {
    id: "s13", screen: "customer_ledger", chip: "খাতা", title: "কাস্টমারের খাতা",
    vo: "কাস্টমারের নামে চাপলে তার পুরো খাতা খুলে যায়। উপরে তার মোট বাকি। কাস্টমার টাকা দিলে টাকা পেলাম চাপুন, টাকা লিখে সংরক্ষণ করুন — বাকি নিজে থেকেই কমে যাবে। আবার বাকিতে দিলে বাকি বিক্রি চাপুন। নিচে তারিখসহ সব লেনদেনের তালিকা।",
    steps: [["মোট বাকি", "মোট বাকি"], ["টাকা পেলাম = বাকি আদায়", "টাকা পেলাম"], ["আবার বাকি দিলে: বাকি বিক্রি", "বাকি বিক্রি চাপুন"], ["সব লেনদেন তারিখসহ", "লেনদেনের তালিকা"]],
    acts: [
      { k: "zoom", at: "মোট বাকি", px: 360, py: 430, s: 1.5 },
      { k: "unzoom", at: "কাস্টমার টাকা" },
      { k: "tap", at: "টাকা পেলাম", x: 305, y: 774, rect: [49, 730, 443, 130] },
      { k: "tap", at: "বাকি বিক্রি চাপুন", x: 775, y: 774, rect: [560, 730, 470, 130] },
      { k: "mark", at: "লেনদেনের তালিকা", rect: [30, 1150, 1020, 430], hold: 2.0 },
    ],
  },
  {
    id: "s14", screen: "customer_ledger", chip: "খাতা", title: "বাকির তাগাদা",
    vo: "বাকি তুলতে টাকা তোলার রিমাইন্ডার দিন চেপে একটি তারিখ ঠিক করুন — সেদিন ফোনে নোটিফিকেশন আসবে। WhatsApp বা এসএমএস চাপলে ভদ্র বাংলা মেসেজে বাকির হিসাব পাঠানো যায়, আর PDF চাপলে পুরো হিসাব বিবরণী শেয়ার করা যায়।",
    steps: [["রিমাইন্ডারের তারিখ দিন", "রিমাইন্ডার"], ["সেদিন নোটিফিকেশন", "নোটিফিকেশন"], ["WhatsApp / SMS মেসেজ", "WhatsApp"], ["PDF হিসাব বিবরণী", "PDF"]],
    acts: [
      { k: "tap", at: "রিমাইন্ডার", x: 379, y: 628, rect: [60, 590, 640, 80] },
      { k: "tap", at: "WhatsApp", x: 233, y: 936, rect: [152, 897, 162, 146] },
      { k: "mark", at: "এসএমএস", rect: [380, 897, 140, 146], hold: 1.0 },
      { k: "tap", at: "PDF চাপলে", x: 856, y: 936, rect: [780, 897, 160, 146] },
    ],
    overlay: "chat",
  },
  {
    id: "s15", screen: "home", chip: "খরচ ও কেনা", title: "খরচ ও মাল কেনা",
    vo: "দোকানের খরচ লিখতে হোম থেকে খরচ বাটনে চাপুন। খাত বাছুন — যেমন ভাড়া, বেতন বা বিদ্যুৎ বিল — টাকা লিখে সংরক্ষণ করুন। মাল কিনলে কেনা বাটনে চাপুন। নগদে কিনলে নগদে কেনা, আর বাকিতে কিনলে বাকিতে কেনা বেছে সাপ্লায়ার বাছাই করুন। সাপ্লায়ারকে টাকা দিলে টাকা দিলাম চাপুন।",
    steps: [["খরচ: খাত বাছুন, টাকা লিখুন", "খরচ বাটনে"], ["কেনা: নগদে বা বাকিতে", "কেনা বাটনে"], ["সাপ্লায়ার বাছাই করুন", "সাপ্লায়ার বাছাই"], ["টাকা দিলাম = দেনা শোধ", "টাকা দিলাম"]],
    acts: [
      { k: "zoom", at: 0.1, px: 540, py: 960, s: 1.3 },
      { k: "tap", at: "খরচ বাটনে", x: 673, y: 874, rect: [568, 777, 210, 194] },
      { k: "tap", at: "কেনা বাটনে", x: 932, y: 874, rect: [827, 777, 211, 194] },
      { k: "tap", at: "টাকা দিলাম", x: 200, y: 1110, rect: [49, 1080, 301, 61] },
    ],
  },
  {
    id: "s16", screen: "stock", chip: "পণ্য", title: "স্টকের হিসাব",
    vo: "পণ্য ট্যাবে সব পণ্য আর তাদের স্টক। উপরে স্টকের মোট মূল্য। বিক্রি করলে স্টক নিজে থেকে কমে, আর কিনলে বাড়ে। কোনো পণ্য কমে গেলে কম স্টক সতর্কতা দেখায়। নতুন পণ্য যোগ করতে নতুন পণ্য চাপুন — নাম, একক, বিক্রয় মূল্য, ক্রয় মূল্য আর বর্তমান স্টক দিন।",
    steps: [["স্টকের মোট মূল্য", "স্টকের মোট"], ["বিক্রিতে কমে, কেনায় বাড়ে", "কিনলে বাড়ে"], ["কম স্টক সতর্কতা", "কম স্টক"], ["নতুন পণ্য যোগ", "নতুন পণ্য চাপুন"]],
    acts: [
      { k: "mark", at: "স্টকের মোট", rect: [45, 298, 292, 130], hold: 1.8 },
      { k: "mark", at: "কম স্টক", rect: [0, 840, 1080, 130], hold: 2.0 },
      { k: "tap", at: "নতুন পণ্য চাপুন", x: 872, y: 1530, rect: [700, 1470, 370, 120] },
    ],
  },
  {
    id: "s17", screen: "reports", chip: "রিপোর্ট", title: "রিপোর্ট ও লাভ",
    vo: "রিপোর্ট ট্যাবে আজ, গতকাল, গত ৭ দিন, এই মাস বা গত মাসের হিসাব দেখুন। মোট বিক্রি, মোট কেনা, খরচ আর আনুমানিক লাভ — সব এক জায়গায়। নিচে টাকার আসা-যাওয়া আর পেমেন্ট মাধ্যম অনুযায়ী হিসাব। চাইলে Excel ফাইল বা সম্পূর্ণ ব্যাকআপ ফাইলও বানাতে পারবেন।",
    steps: [["সময় বাছুন: আজ থেকে মাস", "এই মাস"], ["বিক্রি, কেনা, খরচ, লাভ", "মোট বিক্রি"], ["টাকার আসা-যাওয়া", "আসা-যাওয়া"], ["Excel ও ব্যাকআপ ফাইল", "Excel"]],
    acts: [
      { k: "tap", at: "এই মাস", x: 804, y: 330, rect: [710, 298, 185, 71] },
      { k: "mark", at: "মোট বিক্রি", rect: [36, 460, 486, 130], hold: 1.2 },
      { k: "mark", at: "আনুমানিক লাভ", rect: [555, 680, 486, 136], hold: 1.6 },
      { k: "zoom", at: "আসা-যাওয়া", px: 540, py: 1280, s: 1.25 },
      { k: "mark", at: "আসা-যাওয়া", rect: [36, 930, 1010, 680], hold: 2.4 },
    ],
  },
  {
    id: "s18", screen: "customer_ledger", chip: "সংশোধন", title: "ভুল হলে কী করবেন",
    vo: "ভুল হলে? হিসাব কখনো মুছে ফেলা হয় না। লেনদেনটি খুলে হিসাব বাতিল চাপুন। একটি উল্টো হিসাব যোগ হয়ে বাকি, স্টক আর ক্যাশ ঠিক হয়ে যায়, আর ইতিহাস সবসময় পরিষ্কার থাকে।",
    steps: [["লেনদেনটি খুলুন", "লেনদেনটি খুলে"], ["হিসাব বাতিল চাপুন", "হিসাব বাতিল"], ["উল্টো হিসাবে সব ঠিক", "উল্টো হিসাব"]],
    acts: [{ k: "tap", at: "লেনদেনটি খুলে", x: 500, y: 1270, rect: [30, 1160, 1020, 220] }],
    overlay: "cancel",
  },
  {
    id: "s19", screen: "home", chip: "ব্যাকআপ", title: "নেট ছাড়াই চলে",
    vo: "সবচেয়ে বড় সুবিধা — ইন্টারনেট ছাড়াই সব হিসাব লেখা যায়। হিসাব আগে আপনার ফোনে সংরক্ষিত হয়, নেট এলেই নিজে থেকে আপনার অ্যাকাউন্টে ব্যাকআপ হয়ে যায় — অ্যাপ বন্ধ থাকলেও। ফোন বদলালেও একই ইমেইলে প্রবেশ করলে সব হিসাব ফিরে আসবে।",
    steps: [["নেট ছাড়াই লেখা যায়", "ইন্টারনেট ছাড়াই"], ["আগে ফোনে সংরক্ষিত", "ফোনে সংরক্ষিত"], ["নেট এলে নিজে ব্যাকআপ", "নেট এলেই"], ["নতুন ফোনে সব ফিরে আসে", "ফোন বদলালেও"]],
    acts: [
      { k: "zoom", at: "নেট এলেই", px: 918, py: 150, s: 2.0, delay: 0.5 },
      { k: "mark", at: "ব্যাকআপ হয়ে", rect: [804, 113, 227, 75], hold: 2.2 },
    ],
    overlay: "offline",
  },
  {
    id: "s20", screen: "more", chip: "আরও", title: "সেটিংস ও সাহায্য",
    vo: "আরও ট্যাবে ভাষা বদল, বাকি আদায়ের রিমাইন্ডার আর সাহায্যের সব কিছু। কোনো প্রশ্ন বা সমস্যা হলে যোগাযোগ করুন থেকে WhatsApp বা ইমেইলে আমাদের জানান।",
    steps: [["ভাষা বদল", "ভাষা বদল"], ["বাকি আদায়ের রিমাইন্ডার", "রিমাইন্ডার"], ["যোগাযোগ: WhatsApp ও ইমেইল", "যোগাযোগ করুন"]],
    acts: [
      { k: "tap", at: "ভাষা বদল", x: 259, y: 1100, rect: [40, 1027, 1000, 146] },
      { k: "mark", at: "রিমাইন্ডার", rect: [40, 1213, 1000, 147], hold: 1.4 },
      { k: "img", at: "যোগাযোগ করুন", src: "contact" },
      { k: "mark", at: "WhatsApp", rect: [48, 400, 987, 160], hold: 1.2 },
      { k: "mark", at: "ইমেইলে", rect: [48, 587, 987, 160], hold: 1.4 },
    ],
  },
  {
    id: "s21", screen: "home", chip: "", title: "",
    vo: "[warmly] তাহলে আর দেরি কেন? আজই হিসাববন্ধু দিয়ে আপনার দোকানের হিসাব শুরু করুন — বাংলায়, ইন্টারনেট ছাড়াই, একদম ফ্রি। ধন্যবাদ।",
    steps: [], outro: true,
  },
];

// ------------------------------------------------------------------------------------------
const probe = (f) => +(+execFileSync("ffprobe", ["-v", "error", "-select_streams", "a:0", "-show_entries", "stream=duration", "-of", "csv=p=0", f]).toString().trim()).toFixed(2);

let clock = 0;
const placed = SCENES.map((sc) => {
  const text = plain(sc.vo);
  const voFile = join(ROOT, "assets", "vo", `${sc.id}.mp3`);
  const hasVo = existsSync(voFile);
  const speech = hasVo ? probe(voFile) : +(text.length / CPS).toFixed(2);
  const dur = +(LEAD + speech + TAIL + (sc.outro ? 2.2 : 0)).toFixed(2);
  const start = +clock.toFixed(2);
  clock += dur;
  // time (scene-local) when the voice says `kw`
  const at = (kw) => {
    if (typeof kw === "number") return kw;
    const i = text.indexOf(kw);
    if (i < 0) { console.warn(`  ! ${sc.id}: "${kw}" not in narration`); return LEAD; }
    return +(LEAD + (i / text.length) * speech).toFixed(2);
  };
  return { ...sc, text, hasVo, speech, dur, start, at };
});
const TOTAL = +clock.toFixed(2);

// ------------------------------------------------------------------------------------------
const BASE_CSS = `
${FONT_FACE}
#root{position:absolute;inset:0;font-family:'Hind Siliguri',sans-serif;color:#fff;overflow:visible}
#root .brand{position:absolute;left:${PANEL.left}px;top:112px;height:52px;display:flex;align-items:center;gap:14px;opacity:.85}
#root .brand img{width:48px;height:48px;border-radius:12px}
#root .brand div{font-size:28px;font-weight:600}
#root .chip{position:absolute;left:${PANEL.left}px;top:194px;font-size:28px;font-weight:700;color:${C.deep};background:${C.amber};border-radius:999px;padding:0 24px;line-height:46px}
#root .ttl{position:absolute;left:${PANEL.left}px;top:250px;width:${PANEL.width}px;font-size:66px;font-weight:700;line-height:1.15;text-shadow:0 4px 18px rgba(0,0,0,.25)}
#root .ttl .w{display:inline-block;margin-right:.22em}
#root .step{position:absolute;left:${PANEL.left}px;width:${PANEL.width}px;height:68px;display:flex;align-items:center;gap:22px;opacity:0}
#root .step .n{width:56px;height:56px;border-radius:50%;background:rgba(255,255,255,.14);border:3px solid rgba(255,255,255,.55);display:flex;align-items:center;justify-content:center;font-size:30px;font-weight:700;flex:none}
#root .step .t{font-size:40px;font-weight:600;line-height:1.2}
#root .phone{position:absolute;left:${PHONE.left}px;top:${PHONE.top}px;width:${SW + 24}px;height:${SH + 24}px;border-radius:66px;background:#0b0f0e;box-shadow:0 34px 90px rgba(0,0,0,.45),inset 0 0 0 2px rgba(255,255,255,.10)}
#root .screen{position:absolute;left:12px;top:12px;width:${SW}px;height:${SH}px;border-radius:54px;overflow:hidden;background:#fff}
#root .enter,#root .zoom{position:absolute;left:0;top:0;width:${SW}px;height:${SH}px}
#root img.scr{position:absolute;left:0;top:0;width:${SW}px;height:${SH}px}
#root .tap{position:absolute;left:0;top:0;width:60px;height:60px;border-radius:50%;background:rgba(255,255,255,.55);border:4px solid ${C.amber};box-shadow:0 4px 14px rgba(0,0,0,.35);opacity:0}
#root .press{position:absolute;background:rgba(8,127,91,.30);opacity:0}
#root .mark{position:absolute;border:4px solid ${C.amber};border-radius:16px;opacity:0}
`;

const px = (v) => +(v * F).toFixed(1);
const words = (s) => s.split(" ").map((w) => `<span class="w">${w}</span>`).join("");

function overlayHtml(sc) {
  const id = sc.id;
  switch (sc.overlay) {
    case "store": {
      const chars = [..."HisabBondhu"].map((c) => `<span class="c" style="opacity:0">${c}</span>`).join("");
      return `<div style="position:absolute;inset:0;background:#fff;color:#1f2a27">
  <div style="position:absolute;left:20px;top:58px;width:464px;height:64px;border-radius:32px;background:#EEF2F1;display:flex;align-items:center;padding-left:22px;gap:14px">
    <svg width="28" height="28" viewBox="0 0 24 24"><circle cx="10" cy="10" r="7" stroke="#5f6b67" stroke-width="2.5" fill="none"/><path d="M15 15l6 6" stroke="#5f6b67" stroke-width="2.5" stroke-linecap="round"/></svg>
    <div id="${id}-q" style="font-size:28px;font-weight:600">${chars}</div></div>
  <div id="${id}-card" style="position:absolute;left:24px;top:150px;width:456px;opacity:0">
    <div style="display:flex;gap:20px;align-items:center">
      <img src="assets/icon.png" alt="" style="width:104px;height:104px;border-radius:24px;box-shadow:0 2px 8px rgba(0,0,0,.18)">
      <div><div style="font-size:28px;font-weight:700;line-height:1.2">HisabBondhu: Shop Ledger Khata</div>
      <div data-layout-ignore style="font-size:20px;color:#087F5B;font-weight:600;margin-top:4px">Business · Free</div></div></div>
    <div style="position:absolute;left:0;top:140px;width:456px;height:62px;border-radius:31px;background:#087F5B;overflow:hidden">
      <div id="${id}-btn" style="position:absolute;inset:0;color:#fff;font-size:28px;font-weight:700;display:flex;align-items:center;justify-content:center">Install</div>
      <div id="${id}-prog" style="position:absolute;inset:0;background:#DDEFE8;opacity:0"><div id="${id}-fill" style="position:absolute;inset:0;background:#7CC7AE"></div>
        <div style="position:absolute;inset:0;color:#064e3b;font-size:28px;font-weight:700;display:flex;align-items:center;justify-content:center">ইনস্টল হচ্ছে…</div></div>
      <div id="${id}-open" style="position:absolute;inset:0;color:#fff;font-size:28px;font-weight:700;display:flex;align-items:center;justify-content:center;opacity:0;background:#087F5B">Open</div></div></div>
  <div id="${id}-shots" style="position:absolute;left:24px;top:380px;display:flex;gap:12px;opacity:0">
    <img src="assets/screens/home.png" alt="" style="width:144px;height:256px;border-radius:14px;border:1px solid #dde3e1">
    <img src="assets/screens/customer_ledger.png" alt="" style="width:144px;height:256px;border-radius:14px;border:1px solid #dde3e1">
    <img src="assets/screens/reports.png" alt="" style="width:144px;height:256px;border-radius:14px;border:1px solid #dde3e1"></div>
</div>`;
    }
    case "code": {
      const d = "482916".split("").map((c) => `<div style="width:58px;height:72px;border-radius:14px;border:2px solid #087F5B;display:flex;align-items:center;justify-content:center;font-size:34px;font-weight:700;color:#053D2E"><span class="d" style="opacity:0">${c}</span></div>`).join("");
      return `<div id="${id}-sheet" style="position:absolute;left:0;top:560px;width:${SW}px;height:336px;background:#fff;border-radius:30px 30px 0 0;box-shadow:0 -10px 30px rgba(0,0,0,.18);color:#1f2a27;opacity:0">
  <div style="text-align:center;font-size:30px;font-weight:700;margin-top:28px">ইমেইলে আসা কোডটি দিন</div>
  <div style="display:flex;gap:12px;justify-content:center;margin-top:22px">${d}</div>
  <div id="${id}-ok" style="margin:26px auto 0;width:380px;height:60px;border-radius:30px;background:#E7F5EF;color:#087F5B;font-size:28px;font-weight:700;display:flex;align-items:center;justify-content:center;opacity:0">পাসওয়ার্ড লাগে না</div></div>`;
    }
    case "toast":
      return `<div id="${id}-toast" style="position:absolute;left:30px;top:700px;width:444px;height:72px;border-radius:36px;background:#1f2a27;color:#fff;display:flex;align-items:center;justify-content:center;font-size:28px;font-weight:700;opacity:0;box-shadow:0 10px 24px rgba(0,0,0,.3)">✓ বিক্রি সংরক্ষিত হয়েছে</div>`;
    case "paid":
      return `<div id="${id}-paid" style="position:absolute;left:22px;top:${px(1090)}px;width:460px;background:#fff;border:3px solid #087F5B;border-radius:16px;padding:12px 16px;color:#1f2a27;opacity:0;box-shadow:0 10px 24px rgba(0,0,0,.25)">
  <div style="font-size:28px;color:#087F5B;font-weight:600;line-height:1.3">এখন জমা পেলাম</div>
  <div style="font-size:36px;font-weight:700;line-height:1.3">৳ ৫০০</div>
  <div id="${id}-due" style="margin-top:4px;font-size:28px;font-weight:700;color:${C.red};opacity:0">৳৭৫০ বাকি থাকবে</div></div>`;
    case "chat":
      return `<div id="${id}-chat" style="position:absolute;left:0;top:500px;width:${SW}px;height:396px;background:#ECE5DD;border-radius:28px 28px 0 0;color:#1f2a27;opacity:0">
  <div style="height:76px;background:#075E54;border-radius:28px 28px 0 0;display:flex;align-items:center;gap:14px;padding:0 20px;color:#fff">
    <div style="width:46px;height:46px;border-radius:50%;background:#FFE8CC;color:#D9480F;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:28px">স</div>
    <div style="font-size:24px;font-weight:700">সালমা আপা</div></div>
  <div style="position:absolute;left:30px;right:14px;top:92px;background:#DCF8C6;border-radius:18px 4px 18px 18px;padding:12px 16px;font-size:28px;line-height:1.4;box-shadow:0 1px 2px rgba(0,0,0,.12)">প্রিয় সালমা আপা, ২০ নভেম্বর ২০২৬ পর্যন্ত আপনার মোট বাকি ৳২,২০০। সুবিধামতো সময়ে পরিশোধ করার অনুরোধ রইলো। ধন্যবাদ।</div></div>`;
    case "cancel":
      return `<div id="${id}-dim" style="position:absolute;inset:0;background:rgba(0,0,0,.45);opacity:0"></div>
<div id="${id}-dlg" style="position:absolute;left:28px;top:250px;width:448px;background:#fff;border-radius:28px;padding:26px 26px 18px;color:#1f2a27;opacity:0;box-shadow:0 16px 40px rgba(0,0,0,.35)">
  <div style="font-size:30px;font-weight:700;line-height:1.3">এই হিসাবটি বাতিল করবেন?</div>
  <div data-layout-ignore style="font-size:24px;line-height:1.4;margin-top:10px;color:#3c4744">একটি উল্টো হিসাব যোগ হবে, যাতে বাকি, স্টক ও ক্যাশ ঠিক হয়ে যায়। নিরাপত্তার জন্য মূল হিসাবটি ইতিহাসে থাকবে।</div>
  <div style="display:flex;justify-content:flex-end;gap:26px;margin-top:18px;font-size:28px;font-weight:700"><span style="color:#087F5B">বাতিল</span><span id="${id}-cbtn" style="color:${C.red};padding:2px 12px;border-radius:10px">হিসাব বাতিল</span></div></div>`;
    case "offline":
      return `<div id="${id}-chip2" data-layout-ignore style="position:absolute;left:${px(800)}px;top:${px(112)}px;width:${px(240)}px;height:${px(78)}px;border-radius:20px;background:#FFF4E6;color:#D9480F;font-size:22px;font-weight:700;display:flex;align-items:center;justify-content:center">অফলাইন</div>
<div id="${id}-banner" style="position:absolute;left:14px;top:640px;width:${SW - 28}px;height:150px;border-radius:22px;background:${C.orange};color:#fff;font-size:28px;font-weight:600;line-height:1.35;padding:14px 20px;box-sizing:border-box;box-shadow:0 10px 24px rgba(0,0,0,.3)">ইন্টারনেট নেই — সব হিসাব ফোনে নিরাপদে সংরক্ষিত হচ্ছে</div>`;
    default: return "";
  }
}

function overlayJs(sc) {
  const id = sc.id, A = sc.at, L = [];
  switch (sc.overlay) {
    case "store": {
      const t0 = A("সার্চে");
      L.push(`gsap.utils.toArray('#${id}-q .c').forEach((c,i)=>tl.set(c,{opacity:1},${t0}+i*0.07));`);
      L.push(`tl.fromTo('#${id}-card',{opacity:0,y:24},{opacity:1,y:0,duration:0.35,ease:'expo.out'},${(t0 + 0.9).toFixed(2)});`);
      L.push(`tl.fromTo('#${id}-shots',{opacity:0,y:30},{opacity:1,y:0,duration:0.4,ease:'expo.out'},${(t0 + 1.05).toFixed(2)});`);
      const ti = A("Install চাপুন") + 0.1;
      L.push(`tl.set('#${id}-btn',{opacity:0},${(ti + 0.12).toFixed(2)}); tl.set('#${id}-prog',{opacity:1},${(ti + 0.12).toFixed(2)});`);
      const to = A("Open");
      L.push(`tl.fromTo('#${id}-fill',{scaleX:0,transformOrigin:'0% 50%'},{scaleX:1,duration:${Math.max(0.8, to - ti - 0.6).toFixed(2)},ease:'power1.inOut'},${(ti + 0.15).toFixed(2)});`);
      L.push(`tl.set('#${id}-prog',{opacity:0},${(to - 0.35).toFixed(2)}); tl.set('#${id}-open',{opacity:1},${(to - 0.35).toFixed(2)});`);
      return { L, taps: [{ t: ti, x: 252 / F, y: 321 / F, rect: null }, { t: to + 0.3, x: 252 / F, y: 321 / F, rect: null }] };
    }
    case "code": {
      const t = A("ছয় সংখ্যার");
      L.push(`tl.fromTo('#${id}-sheet',{y:340,opacity:0},{y:0,opacity:1,duration:0.45,ease:'expo.out'},${t});`);
      L.push(`gsap.utils.toArray('#${id}-sheet .d').forEach((d,i)=>tl.fromTo(d,{opacity:0,y:8},{opacity:1,y:0,duration:0.12,ease:'power3.out'},${(t + 0.5).toFixed(2)}+i*0.12));`);
      L.push(`tl.fromTo('#${id}-ok',{opacity:0,scale:0.85,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.3,ease:'back.out(1.6)'},${A("পাসওয়ার্ড")});`);
      return { L };
    }
    case "toast": {
      const t = A("ব্যস");
      L.push(`tl.fromTo('#${id}-toast',{opacity:0,y:30},{opacity:1,y:0,duration:0.4,ease:'expo.out'},${t});`);
      return { L };
    }
    case "paid": {
      L.push(`tl.fromTo('#${id}-paid',{opacity:0,y:20},{opacity:1,y:0,duration:0.4,ease:'expo.out'},${A("এখন জমা")});`);
      L.push(`tl.fromTo('#${id}-due',{opacity:0},{opacity:1,duration:0.3},${A("বাকি টাকাটা")});`);
      return { L };
    }
    case "chat": {
      const t = A("WhatsApp") + 0.45;
      L.push(`tl.fromTo('#${id}-chat',{y:300,opacity:0},{y:0,opacity:1,duration:0.5,ease:'expo.out'},${t.toFixed(2)});`);
      return { L };
    }
    case "cancel": {
      const t = A("হিসাব বাতিল");
      L.push(`tl.fromTo('#${id}-dim',{opacity:0},{opacity:1,duration:0.25},${t});`);
      L.push(`tl.fromTo('#${id}-dlg',{opacity:0,scale:0.92,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.35,ease:'expo.out'},${t});`);
      L.push(`tl.fromTo('#${id}-cbtn',{color:'${C.red}'},{color:'#ffffff',backgroundColor:'${C.red}',duration:0.2},${A("উল্টো হিসাব")});`);
      return { L };
    }
    case "offline": {
      const t = A("নেট এলেই");
      L.push(`tl.fromTo('#${id}-banner',{y:260,opacity:0},{y:0,opacity:1,duration:0.45,ease:'expo.out'},0.3);`);
      L.push(`tl.to('#${id}-banner',{y:260,opacity:0,duration:0.3,ease:'power3.in'},${t});`);
      L.push(`tl.to('#${id}-chip2',{opacity:0,duration:0.25},${(t + 0.2).toFixed(2)});`);
      return { L };
    }
    default: return { L };
  }
}

// ------------------------------------------------------------------------------------------
const allSfx = [], allCaps = [];
for (let si = 0; si < placed.length; si++) {
  const sc = placed[si], prev = placed[si - 1], A = sc.at, id = sc.id;
  const L = [], extra = [];
  let pressN = 0, markN = 0, pressEnd = 0;

  // panel: chip, title, steps
  if (!sc.outro) {
    L.push(`tl.fromTo('#${id}-chip',{opacity:0,filter:'blur(6px)'},{opacity:1,filter:'blur(0px)',duration:0.16,ease:'power4.out'},0.08);`);
    L.push(`gsap.utils.toArray('#${id}-ttl .w').forEach((w,i)=>tl.fromTo(w,{opacity:0,filter:'blur(6px)'},{opacity:1,filter:'blur(0px)',duration:0.14,ease:'power4.out'},0.14+i*0.067));`);
    L.push(`tl.to(['#${id}-chip','#${id}-ttl','#${id}-steps'],{opacity:0,filter:'blur(6px)',duration:0.16,ease:'power2.in'},${(sc.dur - 0.2).toFixed(2)});`);
    sc.steps.forEach(([, kw], i) => {
      const t = Math.max(0.35, A(kw) - 0.1);
      L.push(`tl.fromTo('#${id}-st${i}',{opacity:0,x:-24},{opacity:1,x:0,duration:0.35,ease:'expo.out'},${t.toFixed(2)});`);
      L.push(`tl.to('#${id}-st${i} .n',{backgroundColor:'${C.amber}',borderColor:'${C.amber}',color:'${C.deep}',duration:0.2},${t.toFixed(2)});`);
      if (i > 0) L.push(`tl.to('#${id}-st${i - 1}',{opacity:0.62,duration:0.25},${t.toFixed(2)}); tl.to('#${id}-st${i - 1} .n',{backgroundColor:'rgba(255,255,255,.14)',borderColor:'rgba(255,255,255,.55)',color:'#ffffff',duration:0.2},${t.toFixed(2)});`);
    });
  }

  // screen entrance: continuous when the previous scene showed the same screen
  if (sc.intro) L.push(`tl.fromTo('#${id}-phone',{y:1150},{y:0,duration:0.9,ease:'expo.out'},0);`);
  else if (sc.outro) { /* handled below */ }
  else if (!prev || prev.screen !== sc.screen || prev.overlay === "offline") L.push(`tl.fromTo('#${id}-enter',{x:70,opacity:0},{x:0,opacity:1,duration:0.35,ease:'power3.out'},0);`);

  // overlay
  const ov = overlayJs(sc);
  L.push(...ov.L);

  // actions
  const acts = [...(sc.acts || [])];
  for (const t of ov.taps || []) acts.push({ k: "tap", at: t.t, x: t.x, y: t.y, rect: t.rect });
  const taps = acts.filter((a) => a.k === "tap").map((a) => ({ ...a, t: typeof a.at === "number" ? a.at : A(a.at) })).sort((a, b) => a.t - b.t);
  taps.forEach((a, i) => {
    const x = px(a.x) - 30, y = px(a.y) - 30, t = Math.max(0.6, a.t);
    if (i === 0) L.push(`tl.fromTo('#${id}-tap',{x:${x},y:${y},opacity:0,scale:0.6},{opacity:1,scale:1,duration:0.2,ease:'power3.out'},${(t - 0.45).toFixed(2)});`);
    else L.push(`tl.to('#${id}-tap',{x:${x},y:${y},duration:0.4,ease:'power2.inOut'},${Math.max(pressEnd + 0.02, t - 0.5).toFixed(2)});`);
    L.push(`tl.to('#${id}-tap',{scale:0.78,duration:0.08,ease:'power2.in'},${t.toFixed(2)}); tl.to('#${id}-tap',{scale:1,duration:0.2,ease:'power2.out'},${(t + 0.08).toFixed(2)});`);
    pressEnd = t + 0.29;
    if (a.rect) {
      const n = ++pressN, [rx, ry, rw, rh] = a.rect;
      extra.push(`<div id="${id}-pr${n}" class="press" style="left:${px(rx)}px;top:${px(ry)}px;width:${px(rw)}px;height:${px(rh)}px;border-radius:14px"></div>`);
      L.push(`tl.fromTo('#${id}-pr${n}',{opacity:0},{opacity:1,duration:0.06},${t.toFixed(2)}); tl.to('#${id}-pr${n}',{opacity:0,duration:0.3,ease:'power2.out'},${(t + 0.16).toFixed(2)});`);
    }
    allSfx.push({ t: +(sc.start + t + 0.03).toFixed(2), file: i % 2 ? "click-soft.mp3" : "click.mp3", vol: 0.28 });
  });
  if (taps.length) L.push(`tl.to('#${id}-tap',{opacity:0,scale:0.7,duration:0.15,ease:'power2.in'},${Math.min(sc.dur - 0.3, pressEnd + 0.6).toFixed(2)});`);

  for (const a of acts) {
    const t = (typeof a.at === "number" ? a.at : A(a.at)) + (a.delay || 0);
    if (a.k === "zoom") {
      const s = a.s, cx = SW / 2, cy = SH / 2;
      let x = cx - px(a.px) * s, y = cy - px(a.py) * s;
      x = Math.min(0, Math.max(SW - SW * s, x)); y = Math.min(0, Math.max(SH - SH * s, y));
      L.push(`tl.to('#${id}-zoom',{scale:${s},x:${x.toFixed(1)},y:${y.toFixed(1)},transformOrigin:'0 0',duration:0.65,ease:'power2.inOut'},${Math.max(0, t - 0.2).toFixed(2)});`);
    } else if (a.k === "unzoom") {
      L.push(`tl.to('#${id}-zoom',{scale:1,x:0,y:0,transformOrigin:'0 0',duration:0.55,ease:'power2.inOut'},${Math.max(0, t - 0.2).toFixed(2)});`);
    } else if (a.k === "mark") {
      const n = ++markN, [rx, ry, rw, rh] = a.rect;
      extra.push(`<div id="${id}-mk${n}" class="mark" style="left:${px(rx)}px;top:${px(ry)}px;width:${px(rw)}px;height:${px(rh)}px"></div>`);
      L.push(`tl.fromTo('#${id}-mk${n}',{opacity:0,scale:1.1,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.28,ease:'expo.out'},${t.toFixed(2)}); tl.to('#${id}-mk${n}',{opacity:0,duration:0.18,ease:'power2.in'},${Math.min(sc.dur - 0.25, t + (a.hold || 1.4)).toFixed(2)});`);
    } else if (a.k === "img") {
      extra.unshift(`<img id="${id}-alt" class="scr" src="assets/screens/${a.src}.png" alt="" style="opacity:0">`);
      L.push(`tl.fromTo('#${id}-alt',{opacity:0,x:60},{opacity:1,x:0,duration:0.35,ease:'power3.out'},${t.toFixed(2)});`);
    }
  }

  // captions for this scene: break at punctuation, 2-7 words a group, timed by characters
  const chunks = [];
  sc.text.split(/(?<=[।?!,—])\s+/).forEach((part) => {
    const ws = part.split(" ").filter(Boolean);
    const size = Math.ceil(ws.length / Math.ceil(ws.length / 7));
    for (let i = 0; i < ws.length; i += size) chunks.push(ws.slice(i, i + size));
  });
  const merged = [];
  for (const c of chunks) { if (merged.length && c.length <= 2 && merged[merged.length - 1].length <= 5) merged[merged.length - 1].push(...c); else merged.push([...c]); }
  let pos = 0;
  for (const g of merged) {
    const s = g.join(" ");
    const i = sc.text.indexOf(s, pos); pos = i + s.length;
    const a = sc.start + LEAD + (i / sc.text.length) * sc.speech, b = sc.start + LEAD + ((i + s.length) / sc.text.length) * sc.speech;
    allCaps.push({ start: +(a - 0.05).toFixed(2), end: +(b + 0.12).toFixed(2), words: g });
  }

  // scene file
  const steps = sc.steps.map(([txt], i) => `<div id="${id}-st${i}" class="step" style="top:${440 + i * 80}px"><div class="n">${bnNum(i + 1)}</div><div class="t">${txt}</div></div>`).join("\n");
  const screenImg = sc.screen ? `<img id="${id}-img" class="scr" src="assets/screens/${sc.screen}.png" alt="">` : "";
  let body;
  if (sc.outro) {
    L.push(`tl.fromTo('#${id}-phone',{scale:1,opacity:1,filter:'blur(0px)',transformOrigin:'50% 40%'},{scale:0.35,opacity:0,filter:'blur(8px)',duration:0.55,ease:'power3.in'},0);`);
    L.push(`tl.fromTo('#${id}-logo',{opacity:0,scale:0.5,transformOrigin:'50% 50%'},{opacity:1,scale:1,duration:0.6,ease:'back.out(1.5)'},0.35);`);
    L.push(`gsap.utils.toArray('#${id}-name .w').forEach((w,i)=>tl.fromTo(w,{opacity:0,filter:'blur(8px)'},{opacity:1,filter:'blur(0px)',duration:0.18,ease:'power4.out'},0.75+i*0.08));`);
    L.push(`tl.fromTo('#${id}-tag',{opacity:0,filter:'blur(6px)'},{opacity:1,filter:'blur(0px)',duration:0.2,ease:'power4.out'},1.1);`);
    L.push(`tl.fromTo('#${id}-btn',{opacity:0,y:30},{opacity:1,y:0,duration:0.45,ease:'expo.out'},1.5);`);
    allSfx.push({ t: +(sc.start + 0.37).toFixed(2), file: "impact-bass-1.mp3", vol: 0.35 });
    body = `<div id="${id}-phone" class="phone"><div class="screen"><div class="enter" data-layout-allow-overflow><div class="zoom" data-layout-allow-overflow>${screenImg}</div></div></div></div>
<div style="position:absolute;left:0;width:${W}px;top:170px;text-align:center">
  <img id="${id}-logo" src="assets/icon.png" alt="" style="width:200px;height:200px;border-radius:48px;box-shadow:0 18px 50px rgba(0,0,0,.35)">
  <div id="${id}-name" style="font-size:110px;font-weight:700;line-height:1.1;margin-top:20px">${words("হিসাববন্ধু")}</div>
  <div id="${id}-tag" style="font-size:44px;font-weight:600;margin-top:6px;opacity:.95">দোকানের হিসাব, সবসময় হাতের কাছে</div>
  <div id="${id}-btn" style="margin:36px auto 0;width:720px;height:96px;border-radius:48px;background:#fff;color:${C.green};font-size:42px;font-weight:700;display:flex;align-items:center;justify-content:center;box-shadow:0 14px 40px rgba(0,0,0,.3)">Play Store-এ ফ্রি ডাউনলোড করুন</div>
</div>`;
  } else {
    body = `<div class="brand"><img src="assets/icon.png" alt=""><div>হিসাববন্ধু · ইউজার গাইড</div></div>
${sc.chip ? `<div id="${id}-chip" class="chip">${sc.chip}</div>` : `<div id="${id}-chip"></div>`}
<div id="${id}-ttl" class="ttl">${words(sc.title)}</div>
<div id="${id}-steps">${steps}</div>
<div id="${id}-phone" class="phone"><div class="screen"><div id="${id}-enter" class="enter" data-layout-allow-overflow><div id="${id}-zoom" class="zoom" data-layout-allow-overflow>
  ${screenImg}
  ${overlayHtml(sc)}
  ${extra.join("\n  ")}
  <div id="${id}-tap" class="tap"></div>
</div></div></div></div>`;
  }
  writeFileSync(join(ROOT, "compositions", `${id}.html`), `<template>
<style>${BASE_CSS}</style>
<div id="root" data-composition-id="${id}" data-width="${W}" data-height="${H}">
${body}
</div>
<script>
  const tl = gsap.timeline({ paused: true });
  ${L.join("\n  ")}
  window.__timelines["${id}"] = tl;
</script>
</template>
`);
}

// ------------------------------------------------------------------------------------------
// captions: one host; words fade in, the spoken word turns amber
allCaps.sort((a, b) => a.start - b.start);
for (let i = 0; i < allCaps.length - 1; i++) allCaps[i].end = +Math.min(allCaps[i].end, allCaps[i + 1].start - 0.04).toFixed(2);
const capHtml = allCaps.map((c, i) => `<div id="cap${i}" class="cg">${c.words.map((w, j) => `<span id="cap${i}-w${j}" class="cw">${w}</span>`).join("")}</div>`).join("\n");
const capJs = allCaps.map((c, i) => {
  const n = c.words.length, span = c.end - c.start;
  const lens = c.words.map((w) => w.length + 1), tot = lens.reduce((a, b) => a + b, 0);
  const out = [`tl.set('#cap${i}',{opacity:1,visibility:'visible'},${c.start});`];
  let acc = 0;
  c.words.forEach((w, j) => {
    const th = +(c.start + 0.05 + (acc / tot) * (span - 0.2)).toFixed(2);
    acc += lens[j];
    out.push(`tl.fromTo('#cap${i}-w${j}',{opacity:0,y:10},{opacity:1,y:0,duration:0.14,ease:'power3.out'},${+(c.start + j * 0.04).toFixed(2)});`);
    out.push(`tl.to('#cap${i}-w${j}',{color:'${C.amber}',duration:0.1},${th});`);
    if (j < n - 1) {
      const nx = +(c.start + 0.05 + (acc / tot) * (span - 0.2)).toFixed(2);
      out.push(`tl.to('#cap${i}-w${j}',{color:'#ffffff',duration:0.12},${nx});`);
    }
  });
  out.push(`tl.to('#cap${i}',{opacity:0,duration:0.1,ease:'power2.in'},${+(c.end - 0.1).toFixed(2)});`);
  out.push(`tl.set('#cap${i}',{opacity:0,visibility:'hidden'},${c.end});`);
  return out.join("\n  ");
}).join("\n  ");
writeFileSync(join(ROOT, "compositions", "captions.html"), `<template>
<style>
${FONT_FACE}
#root{position:absolute;inset:0;font-family:'Hind Siliguri',sans-serif}
#root .cg{position:absolute;left:${PANEL.left}px;width:${PANEL.width}px;top:858px;height:112px;display:flex;flex-wrap:wrap;align-content:center;column-gap:12px;row-gap:0;opacity:0;visibility:hidden;overflow:visible}
#root .cw{display:inline-block;color:#fff;font-size:42px;font-weight:700;line-height:1.3;text-shadow:0 3px 12px rgba(0,0,0,.45)}
</style>
<div id="root" data-composition-id="captions" data-width="${W}" data-height="${H}">
${capHtml}
</div>
<script>
  const tl = gsap.timeline({ paused: true });
  ${capJs}
  window.__timelines["captions"] = tl;
</script>
</template>
`);

// ------------------------------------------------------------------------------------------
// root
const hosts = placed.map((sc) => `      <div id="${sc.id}" data-composition-id="${sc.id}" data-composition-src="compositions/${sc.id}.html" data-start="${sc.start}" data-duration="${sc.dur}" data-track-index="1" data-track-kind="graphics" data-width="${W}" data-height="${H}" style="z-index:2"></div>`).join("\n");
const voice = placed.filter((sc) => sc.hasVo).map((sc) => `      <audio id="vo-${sc.id}" class="clip" src="assets/vo/${sc.id}.mp3" data-start="${+(sc.start + LEAD).toFixed(2)}" data-duration="${+(sc.speech - 0.06).toFixed(2)}" data-volume="1" data-track-index="3"></audio>`).join("\n");
const sfxDur = { "click.mp3": 0.36, "click-soft.mp3": 0.36, "impact-bass-1.mp3": 2.1 };
const sfx = allSfx.sort((a, b) => a.t - b.t).map((e, i) => `      <audio id="sfx${i + 1}" class="clip" src="assets/sfx/${e.file}" data-start="${e.t}" data-duration="${sfxDur[e.file]}" data-volume="${e.vol}" data-track-index="4"></audio>`).join("\n");

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
          radial-gradient(rgba(255,255,255,.08) 1.4px, transparent 1.7px) 0 0 / 36px 36px,
          radial-gradient(1100px 700px at 88% 10%, rgba(25,113,194,.40), rgba(25,113,194,0) 70%),
          radial-gradient(900px 700px at 8% 95%, rgba(255,212,59,.14), rgba(255,212,59,0) 70%),
          linear-gradient(135deg, #0A6E50 0%, ${C.green} 40%, ${C.deep} 100%); }
    </style>
  </head>
  <body>
    <div id="root" data-composition-id="main" data-start="0" data-duration="${TOTAL}" data-width="${W}" data-height="${H}">
      <div id="bg" class="clip" data-start="0" data-duration="${TOTAL}" data-track-index="0"></div>
${hosts}
      <div id="captions" data-composition-id="captions" data-composition-src="compositions/captions.html" data-start="0" data-duration="${TOTAL}" data-track-index="2" data-track-kind="captions" data-width="${W}" data-height="${H}" style="z-index:5"></div>
${voice}
${sfx}
    </div>
    <script>
      window.__timelines = window.__timelines || {};
      window.__timelines["main"] = gsap.timeline({ paused: true });
    </script>
  </body>
</html>
`);

// SRT for YouTube / Facebook upload, and the narration list for the voice step
const srtTime = (s) => { const ms = Math.round(s * 1000); const h = Math.floor(ms / 3600000), m = Math.floor(ms / 60000) % 60, sec = Math.floor(ms / 1000) % 60; return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(sec).padStart(2, "0")},${String(ms % 1000).padStart(3, "0")}`; };
writeFileSync(join(ROOT, "subtitles.srt"), allCaps.map((c, i) => `${i + 1}\n${srtTime(c.start)} --> ${srtTime(c.end)}\n${c.words.join(" ")}\n`).join("\n"));
writeFileSync(join(ROOT, "tools", "narration.json"), JSON.stringify(placed.map((p) => ({ id: p.id, vo: p.vo, chars: p.text.length, hasVo: p.hasVo })), null, 2));

const withVo = placed.filter((p) => p.hasVo).length;
console.log(`scenes ${placed.length}, length ${TOTAL}s (${Math.floor(TOTAL / 60)}:${String(Math.round(TOTAL % 60)).padStart(2, "0")}), voice ${withVo}/${placed.length}, captions ${allCaps.length}, chars ${placed.reduce((a, p) => a + p.text.length, 0)}`);
