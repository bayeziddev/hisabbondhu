// node render.mjs → out/thumb-1-face.jpg, thumb-2-khata.jpg, thumb-3-pos.jpg (1280x720, YouTube size)
import { createRequire } from "node:module";
const { chromium } = createRequire(import.meta.url)("playwright"); // global install: run with NODE_PATH=$(npm root -g)
import { execFileSync } from "node:child_process";
import { mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
const here = dirname(fileURLToPath(import.meta.url));
mkdirSync(join(here, "out"), { recursive: true });
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 1 });
await page.goto("file://" + join(here, "thumbs.html"));
await page.evaluate(() => document.fonts.ready);
for (const [id, name] of [["t1", "thumb-1-face"], ["t2", "thumb-2-khata"], ["t3", "thumb-3-pos"]]) {
  const png = join(here, "out", name + ".png");
  await page.locator("#" + id).screenshot({ path: png });
  execFileSync("ffmpeg", ["-v", "error", "-y", "-i", png, "-q:v", "2", join(here, "out", name + ".jpg")]);
}
await browser.close();
