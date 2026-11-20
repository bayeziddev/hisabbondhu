// Builds Play Store screenshot images from real emulator captures.
// Usage: node brand/tools/compose-store.js <raw-screenshots-dir>
//   raw dir contains phone_bn_05_home.png, tablet7_bn_..., tablet10_bn_... (from the Screenshots workflow)
// Output: brand/play-store/screenshots/{phone,phone-en,tablet-7,tablet-10,chromebook}/NN-name.png
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const RAW = path.resolve(process.argv[2] || 'screenshots');
const BRAND = path.resolve(__dirname, '..');
const OUT = path.join(BRAND, 'play-store', 'screenshots');
const font = (w) => 'file://' + path.join(BRAND, 'fonts', `hind_siliguri_${w}.ttf`);

const CAPTIONS = {
  '05_home':            ['ইন্টারনেট ছাড়াই দোকানের সব হিসাব', 'Your whole shop — even offline'],
  '06_sale_entry':      ['১০ সেকেন্ডে বিক্রি লিখুন', 'Record a sale in 10 seconds'],
  '07_customers':       ['কার কাছে কত বাকি — সব পরিষ্কার', 'See exactly who owes what'],
  '08_customer_ledger': ['বাকির খাতা, WhatsApp মেসেজ ও PDF', 'Credit ledger, WhatsApp & PDF'],
  '09_stock':           ['স্টক নিজে থেকেই হিসাব হয়', 'Stock updates automatically'],
  '10_reports':         ['বিক্রি, খরচ ও লাভের রিপোর্ট', 'Sales, expense & profit reports'],
  '02_signin':          ['ইমেইল কোডে নিরাপদ অ্যাকাউন্ট', 'Secure sign-in with an email code'],
  '13_home_dark':       ['রাতেও আরামদায়ক ডার্ক মোড', 'Comfortable dark mode'],
};
const ORDER = Object.keys(CAPTIONS);

function html({ w, h, img, title, sub, landscape }) {
  const data = 'data:image/png;base64,' + fs.readFileSync(img).toString('base64');
  return `<!doctype html><html><head><meta charset="utf-8"><style>
  @font-face{font-family:HS;font-weight:700;src:url(${font('bold')})}
  @font-face{font-family:HS;font-weight:600;src:url(${font('semibold')})}
  html,body{margin:0;width:${w}px;height:${h}px;overflow:hidden}
  .c{width:${w}px;height:${h}px;box-sizing:border-box;font-family:HS,sans-serif;color:#fff;position:relative;
     background:linear-gradient(160deg,#0CA678 0%,#087F5B 50%,#065F46 100%);display:flex;
     ${landscape ? 'flex-direction:row;align-items:center;padding:60px 70px;gap:60px' : 'flex-direction:column;align-items:center;padding:90px 70px 0'}}
  .glow{position:absolute;right:-200px;top:-200px;width:700px;height:700px;border-radius:50%;background:radial-gradient(circle,rgba(255,255,255,.16),rgba(255,255,255,0) 70%)}
  .t{${landscape ? 'flex:0 0 520px' : 'text-align:center'};position:relative}
  h1{margin:0;font-weight:700;font-size:${landscape ? 66 : 74}px;line-height:1.25}
  p{margin:18px 0 0;font-weight:600;font-size:${landscape ? 30 : 34}px;opacity:.88}
  .brand{display:flex;align-items:center;gap:14px;margin-${landscape ? 'bottom' : 'top'}:28px;font-size:30px;font-weight:700;${landscape ? '' : 'justify-content:center'}}
  .brand i{display:inline-block;width:14px;height:14px;border-radius:50%;background:#fff;opacity:.85}
  .dev{position:relative;background:#111;border-radius:${landscape ? 36 : 56}px;padding:${landscape ? 18 : 22}px;box-shadow:0 40px 80px rgba(0,0,0,.35);
       ${landscape ? 'flex:1;' : `margin-top:60px;width:${Math.round(w * 0.74)}px;`}}
  .dev img{display:block;width:100%;border-radius:${landscape ? 20 : 36}px}
  </style></head><body><div class="c"><div class="glow"></div>
  <div class="t">${landscape ? '<div class="brand"><i></i>হিসাববন্ধু</div>' : ''}<h1>${title}</h1><p>${sub}</p></div>
  <div class="dev"><img src="${data}"></div></div></body></html>`;
}

async function render(page, opts, out) {
  await page.setViewportSize({ width: opts.w, height: opts.h });
  await page.setContent(html(opts));
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(150);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  await page.screenshot({ path: out, clip: { x: 0, y: 0, width: opts.w, height: opts.h } });
}

(async () => {
  const files = fs.readdirSync(RAW).filter((f) => f.endsWith('.png'));
  const find = (prefix, lang, key) => files.find((f) => f === `${prefix}_${lang}_${key}.png`);
  const browser = await chromium.launch();
  const page = await browser.newPage({ deviceScaleFactor: 1 });
  const sets = [
    { prefix: 'phone', lang: 'bn', dir: 'phone', w: 1080, h: 1920, landscape: false },
    { prefix: 'phone', lang: 'en', dir: 'phone-en', w: 1080, h: 1920, landscape: false },
    { prefix: 'tablet7', lang: 'bn', dir: 'tablet-7', w: 1080, h: 1920, landscape: false },
    { prefix: 'tablet10', lang: 'bn', dir: 'tablet-10', w: 1920, h: 1080, landscape: true },
    { prefix: 'tablet10', lang: 'bn', dir: 'chromebook', w: 1920, h: 1080, landscape: true },
  ];
  let n = 0;
  for (const s of sets) {
    let i = 0;
    for (const key of ORDER) {
      const f = find(s.prefix, s.lang, key);
      if (!f) continue;
      i++;
      const [bn, en] = CAPTIONS[key];
      const title = s.lang === 'bn' ? bn : en;
      const sub = s.lang === 'bn' ? 'হিসাববন্ধু · দোকানের হিসাব, সবসময় হাতের কাছে' : 'HisabBondhu · Your shop\'s accounts, always at hand';
      await render(page, { ...s, img: path.join(RAW, f), title, sub }, path.join(OUT, s.dir, `${String(i).padStart(2, '0')}-${key.slice(3)}.png`));
      n++;
    }
  }
  await browser.close();
  console.log(`composed ${n} store screenshots into ${OUT}`);
})();
