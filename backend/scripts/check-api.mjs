#!/usr/bin/env node
// API contract check — keeps the server, the Android app, the website and README.md in agreement.
//
//   node backend/scripts/check-api.mjs        (from the repo root, or `npm run check:api` in backend/)
//
// Fails (exit 1) when:
//   • the Android app (HisabApi.kt) calls an endpoint the server does not have (method + path)
//   • the website calls a path the server does not have
//   • README.md's API table is missing a server endpoint, or lists one the server no longer has
// No dependencies; it reads the source files with regular expressions.
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = (p) => readFileSync(join(ROOT, p), 'utf8');
// `/v1/businesses/:id/sync/push` and `/v1/businesses/{id}/sync/push` are the same route.
const norm = (path) => ('/' + path.replace(/^\/+/, '')).replace(/\/+$/, '').replace(/[:{][a-zA-Z_]+}?/g, ':param') || '/';
const key = (method, path) => `${method.toUpperCase()} ${norm(path)}`;

// ---- server: app.route(prefix, xRoutes) + xRoutes.get('/path') in each route file ----
const index = read('backend/src/index.ts');
const server = new Map(); // key -> source
for (const m of index.matchAll(/app\.(get|post|put|delete|patch)\('([^']+)'/g)) server.set(key(m[1], m[2]), 'backend/src/index.ts');
const imports = Object.fromEntries([...index.matchAll(/import \{ (\w+Routes) \} from '\.\/routes\/(\w+)'/g)].map((m) => [m[1], m[2]]));
for (const m of index.matchAll(/app\.route\('([^']+)', (\w+Routes)\)/g)) {
  const [, prefix, name] = m;
  const file = `backend/src/routes/${imports[name]}.ts`;
  if (!imports[name] || !existsSync(join(ROOT, file))) { console.error(`✗ cannot find the file for ${name}`); process.exit(1); }
  const src = read(file);
  for (const r of src.matchAll(new RegExp(`${name}\\.(get|post|put|delete|patch)\\('([^']*)'`, 'g'))) {
    server.set(key(r[1], prefix + (r[2] === '/' ? '' : r[2])), file);
  }
}

// ---- Android: Retrofit annotations ----
const apiFile = 'android/app/src/main/java/com/hisabbondhu/app/data/remote/HisabApi.kt';
const android = [...read(apiFile).matchAll(/@(GET|POST|PUT|DELETE|PATCH)\("([^"]+)"\)/g)].map((m) => key(m[1], m[2]));

// ---- website: any '/v1/…' string in pages and scripts (method not inferred) ----
const webFiles = [
  ...readdirSync(join(ROOT, 'website')).filter((f) => f.endsWith('.html')).map((f) => `website/${f}`),
  ...readdirSync(join(ROOT, 'website/assets')).filter((f) => f.endsWith('.js')).map((f) => `website/assets/${f}`),
];
const web = new Map();
for (const f of webFiles) for (const m of read(f).matchAll(/['"`](\/v1\/[a-z0-9/_:{}-]+)['"`?]/gi)) web.set(norm(m[1]), f);

// ---- README: rows of the API table start with | `METHOD` | `/path` | ----
const readme = [...read('README.md').matchAll(/^\| `(GET|POST|PUT|DELETE|PATCH)` \| `([^`]+)` \|/gm)].map((m) => key(m[1], m[2]));

let failures = 0;
const fail = (msg) => { console.log(`✗ ${msg}`); failures++; };
const serverPaths = new Set([...server.keys()].map((k) => k.split(' ')[1]));

for (const k of android) if (!server.has(k)) fail(`app calls ${k} (${apiFile}) — not on the server`);
for (const [p, f] of web) if (!serverPaths.has(p)) fail(`website calls ${p} (${f}) — not on the server`);
const readmeSet = new Set(readme);
for (const [k, f] of server) if (!readmeSet.has(k)) fail(`server has ${k} (${f}) — missing from README.md API table`);
for (const k of readmeSet) if (!server.has(k)) fail(`README.md lists ${k} — the server has no such endpoint`);
const dupes = readme.filter((k, i) => readme.indexOf(k) !== i);
for (const k of new Set(dupes)) fail(`README.md lists ${k} more than once`);

console.log(`server ${server.size} endpoints · app ${android.length} calls · website ${web.size} paths · README ${readme.length} rows`);
if (failures) { console.log(`\n${failures} problem(s) — fix the code or README.md (see "API reference")`); process.exit(1); }
console.log('✓ server, app, website and README.md agree');
