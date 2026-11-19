// Renders brand PNGs with headless Chromium. Usage: node brand/tools/render.js
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const brand = path.resolve(__dirname, '..');
const res = path.resolve(brand, '..', 'android', 'app', 'src', 'main', 'res');

async function svgToPng(page, svgFile, size, out, { round = false } = {}) {
  const url = 'data:image/svg+xml;base64,' + fs.readFileSync(path.join(brand, svgFile)).toString('base64');
  const radius = round ? '50%' : '0';
  await page.setViewportSize({ width: size, height: size });
  await page.setContent(`<html><body style="margin:0;background:transparent">
    <img src="${url}" style="width:${size}px;height:${size}px;display:block;border-radius:${radius}"></body></html>`);
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: out, omitBackground: true, clip: { x: 0, y: 0, width: size, height: size } });
}

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ deviceScaleFactor: 1 });

  // Legacy launcher icons for Android 7.x (adaptive icons are used on 8.0+).
  const densities = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
  for (const [d, px] of Object.entries(densities)) {
    const dir = path.join(res, `mipmap-${d}`);
    fs.mkdirSync(dir, { recursive: true });
    await svgToPng(page, 'logo-mark.svg', px, path.join(dir, 'ic_launcher.png'));
    await svgToPng(page, 'logo-mark-square.svg', px, path.join(dir, 'ic_launcher_round.png'), { round: true });
  }

  // Google Play assets
  await svgToPng(page, 'logo-mark-square.svg', 512, path.join(brand, 'play-store', 'icon-512.png'));
  await svgToPng(page, 'logo-mark.svg', 1024, path.join(brand, 'logo-mark-1024.png'));

  await page.setViewportSize({ width: 1024, height: 500 });
  await page.goto('file://' + path.join(brand, 'tools', 'feature-graphic.html'));
  await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.fonts.ready);
  await page.locator('#fg').screenshot({ path: path.join(brand, 'play-store', 'feature-graphic-1024x500.png') });

  // Horizontal lockup PNG
  await page.setViewportSize({ width: 1040, height: 240 });
  await page.goto('file://' + path.join(brand, 'logo-horizontal.svg'));
  await page.waitForTimeout(300);
  await page.screenshot({ path: path.join(brand, 'logo-horizontal.png'), omitBackground: true });

  await browser.close();
  console.log('rendered');
})();
